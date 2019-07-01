package com.bbn.akbc.neolearnit.serializers.observations;

import com.bbn.akbc.common.Pair;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.serif.theories.EventMention;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.Token;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CausalRelationCNN{
    @JsonProperty
    private final List<String> tokens;
    @JsonProperty
    private final List<Integer> distancesToArg1;
    @JsonProperty
    private final List<Integer> distancesToArg2;
    @JsonProperty
    private final String label;
    @JsonProperty
    private final String arg1entityType;
    @JsonProperty
    private final String arg1mentionLevel;
    @JsonProperty
    private final String arg2entityType;
    @JsonProperty
    private final String arg2mentionLevel;
    @JsonProperty
    private final List<String> entityTypeLabels;
    @JsonProperty
    private final List<String> mentionLevelLabels;
    @JsonProperty
    private final String docId;
    @JsonProperty
    private final int sentId;
    @JsonProperty
    private String markedSentenceString;
    @JsonProperty
    private Pair<Integer, Integer> arg1span;
    @JsonProperty
    private Pair<Integer, Integer> arg2span;
    @JsonProperty
    private Pair<Integer,Integer> arg1CharSpan;
    @JsonProperty
    private Pair<Integer,Integer> arg2CharSpan;
    @JsonCreator
    public CausalRelationCNN(
            @JsonProperty("tokens") List<String> tokens,
            @JsonProperty("distancesToArg1") List<Integer> distancesToArg1,
            @JsonProperty("distancesToArg2") List<Integer> distancesToArg2,
            @JsonProperty("label") String label,

            @JsonProperty("arg1entityType") String arg1entityType,
            @JsonProperty("arg1mentionLevel") String arg1mentionLevel,
            @JsonProperty("arg2entityType") String arg2entityType,
            @JsonProperty("arg2mentionLevel") String arg2mentionLevel,

            @JsonProperty("entityTypeLabels") List<String> entityTypeLabels,
            @JsonProperty("mentionLevelLabels") List<String> mentionLevelLabels,
            @JsonProperty("docId") String docId,
            @JsonProperty("sentId") int sentId,
            @JsonProperty("markedSentenceString") String markedSentenceString,
            @JsonProperty("arg1span") Pair<Integer, Integer> arg1span,
            @JsonProperty("arg2span") Pair<Integer, Integer> arg2span,
            @JsonProperty("arg1CharSpan") Pair<Integer,Integer> arg1CharSpan,
            @JsonProperty("arg2CharSpan") Pair<Integer,Integer> arg2CharSpan
    ) {
        this.tokens = tokens;
        this.distancesToArg1 = distancesToArg1;
        this.distancesToArg2 = distancesToArg2;
        this.label = label;
        this.arg1entityType = arg1entityType;
        this.arg1mentionLevel = arg1mentionLevel;
        this.arg2entityType = arg2entityType;
        this.arg2mentionLevel = arg2mentionLevel;
        this.entityTypeLabels = entityTypeLabels;
        this.mentionLevelLabels = mentionLevelLabels;
        this.markedSentenceString = markedSentenceString;
        this.arg1span = arg1span;
        this.arg2span = arg2span;
        this.docId = docId;
        this.sentId = sentId;
        this.arg1CharSpan = arg1CharSpan;
        this.arg2CharSpan = arg2CharSpan;
    }

    public static class Builder {

        final List<String> tokens;
        final Pair<Integer, Integer> arg1span;
        final Pair<Integer, Integer> arg2span;
        final String defaultEntityTypeLabels;
        final String defaultmentionLevelLabels;
        List<Integer> distancesToArg1;
        List<Integer> distancesToArg2;
        String label;
        String arg1entityType;
        String arg1mentionLevel;
        String arg2entityType;
        String arg2mentionLevel;
        List<String> entityTypeLabels;
        List<String> mentionLevelLabels;
        String markedSentenceString;
        MatchInfo.LanguageMatchInfo languageMatchInfo;
        MentionOrganization mentionOrganization;
        final Pair<Integer, Integer> arg1Charspan;
        final Pair<Integer, Integer> arg2Charspan;
        public Builder(MatchInfo.LanguageMatchInfo languageMatchInfo) {
            this.tokens = new ArrayList<String>();
            SentenceTheory sentenceTheory = languageMatchInfo.getSentTheory();
            for (Token token : sentenceTheory.tokenSequence()) {
                tokens.add(token.originalText().content().utf16CodeUnits());
            }
            this.arg1span = new Pair<>(languageMatchInfo.getSlot0().get().span().startTokenIndexInclusive(), languageMatchInfo.getSlot0().get().span().endTokenIndexInclusive());
            this.arg2span = new Pair<>(languageMatchInfo.getSlot1().get().span().startTokenIndexInclusive(), languageMatchInfo.getSlot1().get().span().endTokenIndexInclusive());
            this.arg1Charspan = new Pair<>(languageMatchInfo.getSlot0().get().span().startCharOffset().asInt(),languageMatchInfo.getSlot0().get().span().endCharOffset().asInt());
            this.arg2Charspan = new Pair<>(languageMatchInfo.getSlot1().get().span().startCharOffset().asInt(),languageMatchInfo.getSlot1().get().span().endCharOffset().asInt());
            this.defaultEntityTypeLabels = "NA";
            this.defaultmentionLevelLabels = "NA";
            this.arg1entityType = "NA";
            this.arg1mentionLevel = "NA";
            this.arg2entityType = "NA";
            this.arg2mentionLevel = "NA";
            this.languageMatchInfo = languageMatchInfo;
            mentionOrganization = MentionOrganization.HEAD_WORD;
            this.markedSentenceString = "";
        }

        public CausalRelationCNN.Builder withLabel(String label) {
            this.label = label;
            return this;
        }

        public CausalRelationCNN.Builder withArg1entityType(String arg1entityType) {
            this.arg1entityType = arg1entityType;
            return this;
        }

        public CausalRelationCNN.Builder withArg1mentionLevel(String arg1mentionLevel) {
            this.arg1mentionLevel = arg1mentionLevel;
            return this;
        }

        public CausalRelationCNN.Builder withArg2entityType(String arg2entityType) {
            this.arg2entityType = arg2entityType;
            return this;
        }

        public CausalRelationCNN.Builder withArg2mentionLevel(String arg2mentionLevel) {
            this.arg2mentionLevel = arg2mentionLevel;
            return this;
        }

        public CausalRelationCNN.Builder withmarkedSentenceString(String markedSentenceString) {
            this.markedSentenceString = markedSentenceString;
            return this;
        }

        public CausalRelationCNN.Builder withMentionOrganizationHeadWord() {
            this.mentionOrganization = MentionOrganization.HEAD_WORD;
            return this;
        }

        public CausalRelationCNN.Builder withMentionOrganizationLastToken() {
            this.mentionOrganization = MentionOrganization.LAST_TOKEN;
            return this;
        }

        public CausalRelationCNN build() {
            entityTypeLabels = new ArrayList<String>();
            mentionLevelLabels = new ArrayList<String>();
            distancesToArg1 = new ArrayList<Integer>();
            distancesToArg2 = new ArrayList<Integer>();
            EventMention arg1 = (EventMention) languageMatchInfo.firstSpanning();
            EventMention arg2 = (EventMention) languageMatchInfo.secondSpanning();
            Token arg1Head = this.languageMatchInfo.getSentTheory().tokenSequence().token(arg1.anchorNode().head().tokenSpan().endTokenIndexInclusive());
            Token arg2Head = this.languageMatchInfo.getSentTheory().tokenSequence().token(arg2.anchorNode().head().tokenSpan().endTokenIndexInclusive());
            for (int i = 0; i < tokens.size(); i++) {
                // arg1
                if (i >= arg1span.getFirst() && i <= arg1span.getSecond()) {
                    //entityTypeLabels.add("B-" + arg1entityType);
                    entityTypeLabels.add(arg1entityType);
                    mentionLevelLabels.add(arg1mentionLevel);
                } else if (i >= arg2span.getFirst() && i <= arg2span.getSecond()) {
                    //entityTypeLabels.add("B-" + arg2entityType);
                    entityTypeLabels.add(arg2entityType);
                    mentionLevelLabels.add(arg2mentionLevel);
                } else {
                    entityTypeLabels.add(defaultEntityTypeLabels);
                    mentionLevelLabels.add(defaultmentionLevelLabels);
                }
                if (this.mentionOrganization == MentionOrganization.HEAD_WORD) {
                    distancesToArg1.add(i - arg1Head.index());
                    distancesToArg2.add(i - arg2Head.index());
                } else {
                    assert this.mentionOrganization == MentionOrganization.LAST_TOKEN;
                    distancesToArg1.add(i - this.languageMatchInfo.getSentTheory().tokenSequence().token(this.languageMatchInfo.getSlot0().get().span().endTokenIndexInclusive()).index());
                    distancesToArg2.add(i - this.languageMatchInfo.getSentTheory().tokenSequence().token(this.languageMatchInfo.getSlot1().get().span().endTokenIndexInclusive()).index());
                }
            }
            return new CausalRelationCNN(
                    tokens,
                    distancesToArg1,
                    distancesToArg2,
                    label,
                    arg1entityType,
                    arg1mentionLevel,
                    arg2entityType,
                    arg2mentionLevel,
                    entityTypeLabels,
                    mentionLevelLabels,
                    this.languageMatchInfo.getDocTheory().docid().asString(),
                    this.languageMatchInfo.getSentTheory().sentenceNumber(),
                    markedSentenceString,
                    arg1span,
                    arg2span,
                    arg1Charspan,
                    arg2Charspan
            );
        }
        enum MentionOrganization {
            LAST_TOKEN,
            HEAD_WORD
        }
    }
    public static class SerializationFactory {
        public static class StringSerializer {
            CausalRelationCNN causalRelationCNN;
            public StringSerializer(CausalRelationCNN causalRelationCNN) {
                this.causalRelationCNN = causalRelationCNN;
            }

            public String getSentenceInOneLine() {
                StringBuilder sb = new StringBuilder();
                for (String token : this.causalRelationCNN.tokens)
                    sb.append(token.replace("\n", "").replace("\t", "").replace(" ", "").trim() + " ");
                return sb.toString().trim();
            }

            public String getLabel() {
                return this.causalRelationCNN.label;
            }

            public String distanceToArg1InOneLine() {
                StringBuilder sb = new StringBuilder();
                for (int dis : this.causalRelationCNN.distancesToArg1)
                    sb.append(dis + " ");

                return sb.toString().trim();
            }

            public String distanceToArg2InOneLine() {
                StringBuilder sb = new StringBuilder();
                for (int dis : this.causalRelationCNN.distancesToArg2)
                    sb.append(dis + " ");

                return sb.toString().trim();
            }

            public String entityTypeLabelsCoveringArgSpanInOneLine() {
                StringBuilder sb = new StringBuilder();
                for (String s : this.causalRelationCNN.entityTypeLabels) {
                    sb.append(s + " ");
                }
                return sb.toString().trim();
            }

            public String mentionLevelLabelsCoveringArgSpanInOneLine() {
                StringBuilder sb = new StringBuilder();
                for (String s : this.causalRelationCNN.mentionLevelLabels) {
                    sb.append(s + " ");
                }
                return sb.toString().trim();
            }
        }

        public static class JSONStringSerializer {
            CausalRelationCNN causalRelationCNN;

            public JSONStringSerializer(CausalRelationCNN causalRelationCNN) {
                this.causalRelationCNN = causalRelationCNN;
            }

            public String getJSON() {
                ObjectMapper mapper = new ObjectMapper();
                try {
                    return mapper.writeValueAsString(this.causalRelationCNN);
                } catch (JsonProcessingException j) {
                    j.printStackTrace();
                    return null;
                }
            }
        }
    }
}
