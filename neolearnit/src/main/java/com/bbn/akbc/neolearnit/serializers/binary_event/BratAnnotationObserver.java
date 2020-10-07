package com.bbn.akbc.neolearnit.serializers.binary_event;

import com.bbn.akbc.common.Pair;
import com.bbn.akbc.neolearnit.common.Annotation;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.common.targets.TargetFactory;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;
import com.bbn.akbc.neolearnit.serializers.ExternalAnnotationBuilder;
import com.bbn.bue.common.files.FileUtils;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.EventMention;
import com.bbn.serif.theories.SentenceTheory;
import com.google.common.io.Files;

import java.io.*;
import java.util.*;

public class BratAnnotationObserver extends ExternalAnnotationBuilder {
    // For why this is implemented in this way, please see:
    // http://e-gitlab.bbn.com/text-group/CauseEx/commit/64c294343af1ff4c8d1a0c297de8409577975c11

    final Set<DocTheory> sgmOutputCache;
    final File outputDir;
    final Target target;
    final Map<MentionIdentifier, String> eventMentionMapping;
    final Set<Pair<MentionIdentifier, MentionIdentifier>> relationMentionSet;
    final Map<String, Map<String, List<String>>> relationBuffer;

    public String unaryTypeStrGetterWithFallback(String docId, int sentId, int startIdx, int endIdx, UnaryInstanceTypeGetter unaryInstanceTypeGetter){
        String type = unaryInstanceTypeGetter.getLabel(docId,sentId,startIdx,endIdx);
        if(type == null){
            return "MISSING_IN_MAPPINGS";
        }
        else return type;
    }

    public BratAnnotationObserver(String outputDir) {
        this.outputDir = new File(outputDir);
        this.sgmOutputCache = new HashSet<>();
        this.target = TargetFactory.makeBinaryEventEventTarget();
        this.eventMentionMapping = new HashMap<>();
        this.relationMentionSet = new HashSet<>();
        this.relationBuffer = new HashMap<>();
        try {
            FileUtils.recursivelyDeleteDirectory(this.outputDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.outputDir.mkdirs();


        // TODO: Move this into resources.
        for (String confFile : new String[]{"annotation.conf", "kb_shortcuts.conf", "tools.conf", "visual.conf"}) {
            try {
                Files.copy(
                        new File("/home/hqiu/Public/brat_config_template/" + confFile),
                        new File(this.outputDir.getAbsolutePath() + "/" + confFile)
                );
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static File pathjoin(String path1, String path2) {
        File file1 = new File(path1);
        File file2 = new File(file1, path2);
        return file2;
    }

    private static String CleanSgmString(String txtFileString) {
        return txtFileString.replace('\n', ' ');
    }

    private void WriteSGM(DocTheory docTheory) {
        if (sgmOutputCache.contains(docTheory)) return;
        String docId = docTheory.docid().asString();
        String sgmContent = docTheory.document().originalText().content().utf16CodeUnits();
        sgmContent = CleanSgmString(sgmContent);
        File ret = pathjoin(this.outputDir.getPath(), docId + ".txt");
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(ret));
            bufferedWriter.write(sgmContent);
            sgmOutputCache.add(docTheory);
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }

//    public void WriteAllEventMention(Collection<DocTheory> docTheoryCollection) {
//        for (DocTheory docTheory : docTheoryCollection) {
//            for (SentenceTheory sentenceTheory : docTheory.sentenceTheories()) {
//                for (EventMention eventMention : sentenceTheory.eventMentions()) {
//                    WriteNodes(new MentionIdentifier(docTheory.docid().asString(),
//                            sentenceTheory.sentenceNumber(),
//                            eventMention.anchorNode().head().tokenSpan().startCharOffset().asInt(),
//                            eventMention.anchorNode().head().tokenSpan().endCharOffset().asInt(),
//                            "Event",
//                            eventMention.anchorNode().head().tokenSpan().originalText().content().utf16CodeUnits().replace('\n', ' ')), docTheory);
//                }
//            }
//        }
//    }

    private void WriteRelationMention(MentionIdentifier left, MentionIdentifier right) {
        if (this.relationMentionSet.contains(new Pair<>(left, right))) return;
        BufferedWriter annWriter;
        int relationNumber = relationMentionSet.size();
        assert left.getDocId().equals(right.getDocId());
        try {
            annWriter = new BufferedWriter(new FileWriter(pathjoin(this.outputDir.getPath(), left.getDocId() + ".ann"), true));
        } catch (FileNotFoundException e) {
            try {
                annWriter = new BufferedWriter(new FileWriter(pathjoin(this.outputDir.getPath(), left.getDocId() + ".ann")));
            } catch (IOException f) {
                f.printStackTrace();
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        try {
//            annWriter.write(String.format(
//                    "E%s\tEvent:T%s\tCause-Arg:E%s\n",
//                    relationNumber,
//                    this.eventMentionMapping.get(left),
//                    this.eventMentionMapping.get(left),
//                    this.eventMentionMapping.get(right)
//            ));
            this.relationBuffer.get(left.getDocId()).get(this.eventMentionMapping.get(left)).add(String.format("Cause-Arg:%s", this.eventMentionMapping.get(right)));
            relationMentionSet.add(new Pair<>(left, right));
            annWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }

    private void WriteNodes(MentionIdentifier mentionIdentifier, DocTheory docTheory) {
        if (eventMentionMapping.get(mentionIdentifier) != null) {
            return;
        }
        BufferedWriter annWriter;
        int triggerNumber = eventMentionMapping.keySet().size();
        try {
            annWriter = new BufferedWriter(new FileWriter(pathjoin(this.outputDir.getPath(), mentionIdentifier.getDocId() + ".ann"), true));
        } catch (FileNotFoundException e) {
            try {
                annWriter = new BufferedWriter(new FileWriter(pathjoin(this.outputDir.getPath(), mentionIdentifier.getDocId() + ".ann")));
            } catch (IOException f) {
                f.printStackTrace();
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        try {
            annWriter.write(String.format(
                    "T%d\tEvent %d %d\t%s\n",
                    triggerNumber,
                    mentionIdentifier.getStartCharOffset(),
                    mentionIdentifier.getEndCharOffset() + 1,
                    mentionIdentifier.getUtf16CodeUnits()
            ));
//            annWriter.write(String.format(
//                    "E%d\tEvent:T%d\n",
//                    triggerNumber,
//                    triggerNumber
//            ));
            Map<String, List<String>> localEvent = this.relationBuffer.getOrDefault(mentionIdentifier.getDocId(), new HashMap<>());
            List<String> newBuffer = localEvent.getOrDefault(String.format("E%d", triggerNumber), new ArrayList<>());
            newBuffer.add(String.format("Event:T%d", triggerNumber));
            localEvent.put(String.format("E%d", triggerNumber), newBuffer);
            eventMentionMapping.put(mentionIdentifier, String.format("E%d", triggerNumber));
            this.relationBuffer.put(mentionIdentifier.getDocId(), localEvent);
            annWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }

    public void flushEventBuffer() throws IOException {
        for (String docId : this.relationBuffer.keySet()) {
            BufferedWriter annWriter = new BufferedWriter(new FileWriter(pathjoin(this.outputDir.getPath(), docId + ".ann"), true));
            for (String eId : this.relationBuffer.get(docId).keySet()) {
                List<String> line = new ArrayList<>();
                line.add(eId);
//                line.addAll(this.relationBuffer.get(docId).get(eId));
                List<String> buf = this.relationBuffer.get(docId).get(eId);
                line.add(String.format("\t%s", buf.get(0)));
                for (int i = 1; i < buf.size(); ++i) {
                    line.add(String.format(" %s", buf.get(i)));
                }
                annWriter.write(String.format("%s\n", String.join("", line)));
            }
            annWriter.close();
        }
    }


    @Override
    public void build() throws Exception {
        Map<InstanceIdentifier, DocTheory> instanceIdentifierDocTheoryMapTheoryMap = this.resolveDocTheory();
        UnaryInstanceTypeGetter unaryInstanceTypeGetter = new UnaryInstanceTypeGetter();
        for(InstanceIdentifier instanceIdentifier:this.inMemoryAnnotationStorage.getAllInstanceIdentifier()){
            if(instanceIdentifier.getSlot1SpanningType().equals(InstanceIdentifier.SpanningType.Empty)){
                List<String> possibleTypes = new ArrayList<>();
                for(LabelPattern labelPattern: this.inMemoryAnnotationStorage.lookupInstanceIdentifierAnnotation(instanceIdentifier)){
                    String frozenString;
                    if (labelPattern.getFrozenState().equals(Annotation.FrozenState.FROZEN_GOOD)) {
                        frozenString = "+";
                    } else if (labelPattern.getFrozenState().equals(Annotation.FrozenState.FROZEN_BAD)) {
                        frozenString = "-";
                    } else {
                        frozenString = "N";
                    }
                    possibleTypes.add(labelPattern.getLabel()+"/"+frozenString);
                }
                if(possibleTypes.size()<1){
                    possibleTypes.add("NO_LABEL");
                }
                unaryInstanceTypeGetter.putLabel(instanceIdentifier.getDocid(),instanceIdentifier.getSentid(),instanceIdentifier.getSlot0Start(),instanceIdentifier.getSlot0End(),String.join(",",possibleTypes));
            }
        }

        for (InstanceIdentifier instanceIdentifier : this.inMemoryAnnotationStorage.getAllInstanceIdentifier()) {
            final DocTheory docTheory = instanceIdentifierDocTheoryMapTheoryMap.get(instanceIdentifier);
            final SentenceTheory sentenceTheory = instanceIdentifierDocTheoryMapTheoryMap.get(instanceIdentifier).sentenceTheory(instanceIdentifier.getSentid());

            String docId = instanceIdentifier.getDocid();
            WriteSGM(docTheory);
            EventMention leftEventMention = (EventMention) InstanceIdentifier.getSpanning(sentenceTheory, instanceIdentifier.getSlot0Start(), instanceIdentifier.getSlot0End(), instanceIdentifier.getSlot0SpanningType()).get();
            EventMention rightEventMention = (EventMention) InstanceIdentifier.getSpanning(sentenceTheory, instanceIdentifier.getSlot1Start(), instanceIdentifier.getSlot1End(), instanceIdentifier.getSlot1SpanningType()).get();
            MentionIdentifier left = new MentionIdentifier(
                    docTheory.docid().asString(),
                    sentenceTheory.sentenceNumber(),
                    leftEventMention.anchorNode().head().tokenSpan().startCharOffset().asInt(),
                    leftEventMention.anchorNode().head().tokenSpan().endCharOffset().asInt(),
                    unaryTypeStrGetterWithFallback(docTheory.docid().asString(), sentenceTheory.sentenceNumber(), instanceIdentifier.getSlot0Start(), instanceIdentifier.getSlot0End(), unaryInstanceTypeGetter),
                    leftEventMention.anchorNode().head().tokenSpan().originalText().content().utf16CodeUnits().replace('\n', ' '));
            MentionIdentifier right = new MentionIdentifier(
                    docTheory.docid().asString(),
                    sentenceTheory.sentenceNumber(),
                    rightEventMention.anchorNode().head().tokenSpan().startCharOffset().asInt(),
                    rightEventMention.anchorNode().head().tokenSpan().endCharOffset().asInt(),
                    unaryTypeStrGetterWithFallback(docTheory.docid().asString(), sentenceTheory.sentenceNumber(), instanceIdentifier.getSlot1Start(), instanceIdentifier.getSlot1End(), unaryInstanceTypeGetter),
                    rightEventMention.anchorNode().head().tokenSpan().originalText().content().utf16CodeUnits().replace('\n', ' '));
            WriteNodes(left, docTheory);
            WriteNodes(right, docTheory);
            WriteRelationMention(left, right);
        }


        flushEventBuffer();
    }

    private class UnaryInstanceTypeGetter{
        private class UnaryInstanceIdentifier{
            String docId;
            int sentId;
            int startIdx;
            int endIdx;
            UnaryInstanceIdentifier(String docId,int sentId,int startIdx,int endIdx){
                this.docId = docId;
                this.sentId = sentId;
                this.startIdx = startIdx;
                this.endIdx = endIdx;
            }
            @Override
            public boolean equals(Object o){
                if(!(o instanceof UnaryInstanceIdentifier))return false;
                UnaryInstanceIdentifier that = (UnaryInstanceIdentifier)o;
                return this.docId.equals(that.docId) && this.sentId == that.sentId && this.startIdx == that.startIdx && this.endIdx == that.endIdx;
            }

            @Override
            public int hashCode(){
                int prime = 31;
                int ret = docId.hashCode();
                ret = ret * prime + this.sentId;
                ret = ret * prime + this.startIdx;
                ret = ret * prime + this.endIdx;
                return ret;
            }
        }

        final Map<UnaryInstanceIdentifier,String> unaryInstanceIdToLabel = new HashMap<>();

        public UnaryInstanceTypeGetter(){
        }

        String getLabel(String docId,int sentId,int startIdx,int endIdx){
            return unaryInstanceIdToLabel.get(new UnaryInstanceIdentifier(docId,sentId,startIdx,endIdx));
        }

        void putLabel(String docId,int sentId,int startIdx,int endIdx,String label){
            unaryInstanceIdToLabel.put(new UnaryInstanceIdentifier(docId,sentId,startIdx,endIdx),label);
        }

    }

    private class MentionIdentifier {
        final String docId;
        final int sentId;
        final int startCharOffset;
        final int endCharOffset;
        final String eventType;
        final String utf16CodeUnits;

        MentionIdentifier(String docId, int sentId, int startCharOffset, int endCharOffset,String eventType, String utf16CodeUnits) {
            this.docId = docId;
            this.sentId = sentId;
            this.startCharOffset = startCharOffset;
            this.endCharOffset = endCharOffset;
            this.utf16CodeUnits = utf16CodeUnits;
            this.eventType = eventType;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof MentionIdentifier)) return false;
            MentionIdentifier that = (MentionIdentifier) o;
            return that.docId.equals(this.docId) &&
                    that.sentId == this.sentId &&
                    that.startCharOffset == this.startCharOffset &&
                    that.endCharOffset == this.endCharOffset &&
                    that.eventType.equals(this.eventType);
        }

        @Override
        public int hashCode() {
            int ret = this.docId.hashCode();
            ret = ret * 31 + this.sentId;
            ret = ret * 31 + this.startCharOffset;
            ret = ret * 31 + this.endCharOffset;
            return ret;
        }

        public String getDocId() {
            return this.docId;
        }

        public int getSentId() {
            return this.sentId;
        }

        public int getStartCharOffset() {
            return this.startCharOffset;
        }

        public int getEndCharOffset() {
            return this.endCharOffset;
        }

        public String getUtf16CodeUnits() {
            return this.utf16CodeUnits;
        }

        public String getEventType(){
            return this.eventType;
        }
    }
}