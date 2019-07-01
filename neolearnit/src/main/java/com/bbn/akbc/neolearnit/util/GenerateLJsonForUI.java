package com.bbn.akbc.neolearnit.util;

import com.bbn.bue.common.files.FileUtils;
import com.bbn.serif.io.SerifXMLLoader;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.EventMention;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.Token;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class GenerateLJsonForUI {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Pair<K,V>{
        @JsonProperty
        public final K key;
        @JsonProperty
        public final V value;
        public Pair(K k,V v){
            this.key = k;
            this.value = v;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class TriggerInfoOutputEntry {
        @JsonProperty
        private final String docId;
        @JsonProperty
        private final Pair<Integer, Integer> sentenceId;
        @JsonProperty
        private final String trigger;
        @JsonProperty
        private final String triggerPosTag;
        @JsonProperty
        private final int triggerSentenceTokenizedPosition;
        @JsonProperty
        private final int triggerSentenceTokenizedEndPosition;
        @JsonProperty
        private final int timeSentenceTokenizedPosition;
        @JsonProperty
        private final int timeSentenceTokenizedEndPosition;
        @JsonProperty
        private final int locationSentenceTokenizedPosition;
        @JsonProperty
        private final int locationSentenceTokenizedEndPosition;

        private TriggerInfoOutputEntry(final String docId, final Pair<Integer, Integer> sentenceId, final String trigger,
                                       final String triggerPosTag, final int triggerSentenceTokenizedPosition,
                                       final int triggerSentenceTokenizedEndPosition, final int timeSentenceTokenizedPosition,
                                       final int timeSentenceTokenizedEndPosition, final int locationSentenceTokenizedPosition,
                                       final int locationSentenceTokenizedEndPosition) {
            this.docId = docId;
            this.sentenceId = sentenceId;
            this.trigger = trigger;
            this.triggerPosTag = triggerPosTag;
            this.triggerSentenceTokenizedPosition = triggerSentenceTokenizedPosition;
            this.triggerSentenceTokenizedEndPosition = triggerSentenceTokenizedEndPosition;
            this.timeSentenceTokenizedPosition = timeSentenceTokenizedPosition;
            this.timeSentenceTokenizedEndPosition = timeSentenceTokenizedEndPosition;
            this.locationSentenceTokenizedPosition = locationSentenceTokenizedPosition;
            this.locationSentenceTokenizedEndPosition = locationSentenceTokenizedEndPosition;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class SentenceInfo {
        @JsonProperty
        private final List<String> token;
        @JsonProperty
        private final List<Pair<Integer, Integer>> tokenSpan;

        public SentenceInfo(final List<String> token, final List<Pair<Integer, Integer>> tokenSpan) {
            this.token = token;
            this.tokenSpan = tokenSpan;

        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class SentenceInfoOutputEntry {
        @JsonProperty
        private final String docId;
        @JsonProperty
        private final String docPath;
        @JsonProperty
        private final Pair<Integer, Integer> sentenceId;
        @JsonProperty
        private final SentenceInfo sentenceInfo;
        @JsonProperty
        private final String fullSentenceText;

        public SentenceInfoOutputEntry(final String docId, final String docPath, final Pair<Integer, Integer> sentenceId, final SentenceInfo sentenceInfo,final String fullSentenceText) {
            this.docId = docId;
            this.docPath = docPath;
            this.sentenceId = sentenceId;
            this.sentenceInfo = sentenceInfo;
            this.fullSentenceText = fullSentenceText;
        }
    }

    public static void main(String[] args)throws Exception{
        String inputListFilePath = args[0];
        String outputPrefix = args[1];
        String outputTriggerlJsonPath = outputPrefix+"_trigger.ljson";
        String outputSentencelJsonPath = outputPrefix+"_sentence.ljson";
        final SerifXMLLoader serifXMLLoader =
                SerifXMLLoader.builderFromStandardACETypes().allowSloppyOffsets().makeAllTypesDynamic()
                        .build();
        final ImmutableList<File> serifxmls = FileUtils.loadFileList(Files.asCharSource(new File(inputListFilePath), Charsets.UTF_8));
        final List<TriggerInfoOutputEntry> triggerInfoOutputEntries = new ArrayList<>();
        final List<SentenceInfoOutputEntry> sentenceInfoOutputEntries = new ArrayList<>();

        for (File serifXMLFile : serifxmls) {
            DocTheory dt = serifXMLLoader.loadFrom(serifXMLFile);
            final String docId = dt.docid().asString();
            final String docPath = serifXMLFile.getPath();
            for (final SentenceTheory st : dt.nonEmptySentenceTheories()) {
                boolean shouldAddSentence = false;
                for(EventMention eventMention: st.eventMentions()){
                    if(!eventMention.type().asString().equals("Event")){
                        continue;
                    }
                    String text = eventMention.anchorNode().span().tokenizedText().utf16CodeUnits();
                    text = text.replaceAll("[\\t\\n\\r.,_]+", "");
                    text = Joiner.on("_").join(Splitter.on(" ").trimResults().omitEmptyStrings().split(text));
                    text = text.toLowerCase();
                    final String posTag = eventMention.anchorNode().headPOS().asString();
                    String posTagChar;
                    if (posTag.startsWith("VB")) {
                        posTagChar = "v";
                    } else if (posTag.startsWith("NN")) {
                        posTagChar = "n";
                    }
                    else if (posTag.startsWith("JJ")){
                        posTagChar = "j";
                    }
                    else {
                        // ignore non-verbs and non-nouns
                        System.out.println(posTag);
                        continue;
                    }
                    if (text.length() > 512) {
                        continue;
                    }
                    shouldAddSentence = true;
                    triggerInfoOutputEntries.add(new TriggerInfoOutputEntry(
                            docId,
                            new Pair<Integer, Integer>(st.span().startCharOffset().asInt(), st.span().endCharOffset().asInt()),
                            text,
                            posTagChar,
                            eventMention.anchorNode().span().startTokenIndexInclusive(),
                            eventMention.anchorNode().span().endTokenIndexInclusive(),
                            -1,
                            -1,
                            -1,
                            -1
                    ));
                }
                if(shouldAddSentence){
                    List<String> tokenSentence = new ArrayList<>();
                    List<Pair<Integer, Integer>> spanTokenSentence = new ArrayList<>();
                    for (Token token : st.tokenSequence()) {
                        spanTokenSentence.add(new Pair<>(token.startCharOffset().asInt(), token.endCharOffset().asInt()));
                        tokenSentence.add(token.tokenizedText().utf16CodeUnits());
                    }
                    SentenceInfo sentenceInfo = new SentenceInfo(tokenSentence, spanTokenSentence);
                    SentenceInfoOutputEntry sentenceInfoOutputEntry = new SentenceInfoOutputEntry(
                            docId,
                            docPath,
                            new Pair<>(st.span().startCharOffset().asInt(), st.span().endCharOffset().asInt()),
                            sentenceInfo,
                            st.tokenSequence().span().tokenizedText().utf16CodeUnits()
                    );
                    sentenceInfoOutputEntries.add(sentenceInfoOutputEntry);
                }
            }
        }
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibilityChecker(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.NONE)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE));
//		mapper.registerModule(new BUECommonModule());
        mapper.findAndRegisterModules();
        BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputTriggerlJsonPath)));
        for(TriggerInfoOutputEntry triggerInfoOutputEntry:triggerInfoOutputEntries){
            bw.write(mapper.writeValueAsString(triggerInfoOutputEntry)+"\n");
        }
        bw.close();
        bw = new BufferedWriter(new FileWriter(new File(outputSentencelJsonPath)));
        for(SentenceInfoOutputEntry sentenceInfoOutputEntry:sentenceInfoOutputEntries){
            bw.write(mapper.writeValueAsString(sentenceInfoOutputEntry)+"\n");
        }
        bw.close();
    }
}
