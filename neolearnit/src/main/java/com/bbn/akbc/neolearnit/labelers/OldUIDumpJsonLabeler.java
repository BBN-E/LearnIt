package com.bbn.akbc.neolearnit.labelers;

import com.bbn.akbc.common.Pair;
import com.bbn.akbc.neolearnit.common.Annotation;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.util.SourceListsReader;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;
import com.bbn.akbc.neolearnit.util.GeneralUtils;
import com.bbn.serif.io.SerifXMLLoader;
import com.bbn.serif.theories.*;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class OldUIDumpJsonLabeler {


    public static void main(String[] args) throws Exception {
        List<String> spanFiles = new ArrayList<>();
        LearnItConfig.loadParams(new File("/home/hqiu/ld100/learnit/params/learnit/runs/cx_m5_wm_m6_wm_m6_isi_cx_m9_cx_m12_wm_m12_all_verbs_and_nouns.params"));
//        spanFiles.add("/home/hqiu/tmp/alignment_cx2/causeex_m9_new_demo.align.json");
//        spanFiles.add("/home/hqiu/tmp/alignment_cx2/causeex_m9_old_partial_demo.align.json");
//        spanFiles.add("/home/hqiu/tmp/alignment_cx2/causeex_m5_demo.align.json");
//        spanFiles.add("/home/hqiu/tmp/alignment_cx2/causeex_m12_demo.align.json");
//        spanFiles.add("/home/hqiu/tmp/alignment_cx2/wm_m6_demo.align.json");
        spanFiles.add("/home/hqiu/tmp/alignment_wm2/wm_m6_demo.align.json");
        spanFiles.add("/home/hqiu/tmp/alignment_wm2/wm_intervention_demo.align.json");
        spanFiles.add("/home/hqiu/tmp/alignment_wm2/wm_m6_isi_demo.align.json");
        spanFiles.add("/home/hqiu/tmp/alignment_wm2/wm_m12_demo.align.json");

        File outputFile = new File("/home/hqiu/tmp/unary_event_wm.sjson");
        ObjectMapper objectMapper = new ObjectMapper(new JsonFactory());
        Map<String, List<UIAnnotationEntry>> UIAnnotations = new HashMap<>();
        for (String spanFile : spanFiles) {
            for (String jsonLine : GeneralUtils.readLinesIntoList(spanFile)) {
                Map<String, Object> parsedJson = objectMapper.readValue(jsonLine, Map.class);
                String docId = (String) parsedJson.get("docId");
                int sentCharStart = (Integer) parsedJson.get("sentCharStart");
                int sentCharEnd = (Integer) parsedJson.get("sentCharEnd");
                int triggerStartTokenIdx = (Integer) parsedJson.get("triggerStartTokenIdx");
                int triggerEndTokenIdx = (Integer) parsedJson.get("triggerEndTokenIdx");
                String eventType = (String) parsedJson.get("eventType");
                int positiveInt = (Integer) parsedJson.get("positive");
                Annotation.FrozenState positive = (positiveInt == 1) ? Annotation.FrozenState.FROZEN_GOOD : (positiveInt == 2) ? Annotation.FrozenState.FROZEN_BAD : Annotation.FrozenState.NO_FROZEN;
                List<UIAnnotationEntry> annotationPerDoc = UIAnnotations.getOrDefault(docId, new ArrayList<>());
                annotationPerDoc.add(new UIAnnotationEntry(docId, sentCharStart, sentCharEnd, triggerStartTokenIdx, triggerEndTokenIdx, eventType, positive));
                UIAnnotations.put(docId, annotationPerDoc);
            }
        }

        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (String docId : UIAnnotations.keySet()) {
//            File docFile = PathConverter.getFile(docId);
            File docFile = new File(SourceListsReader.getFullPath(docId));
            tasks.add(new ConvertSpanToInstanceIdentifier(docFile, UIAnnotations.get(docId)));
        }
        GeneralUtils.GeneralDocBasedWorkerScheduler(tasks);
        Annotation.InMemoryAnnotationStorage inMemoryAnnotationStorage = new Annotation.InMemoryAnnotationStorage();
        int aligned = 0;
        int partiallyAligned = 0;
        int unresolved = 0;
        for (Callable<Boolean> task : tasks) {
            ConvertSpanToInstanceIdentifier sentSpanObj = (ConvertSpanToInstanceIdentifier) task;
            for (UIAnnotationEntry uiAnnotationEntry : sentSpanObj.resolvedAnnotationEntries.keySet()) {
                InstanceIdentifier instanceIdentifier = sentSpanObj.resolvedAnnotationEntries.get(uiAnnotationEntry);
                if (instanceIdentifier != null) {
                    inMemoryAnnotationStorage.addAnnotation(instanceIdentifier, new LabelPattern(uiAnnotationEntry.eventType.substring(uiAnnotationEntry.eventType.lastIndexOf("/") + 1), uiAnnotationEntry.positive));
                    if (uiAnnotationEntry.triggerStartTokenIdx == instanceIdentifier.getSlot0Start() && uiAnnotationEntry.triggerEndTokenIdx == instanceIdentifier.getSlot0End()) {
                        System.out.println("[Fully resolved] " + uiAnnotationEntry);
                        aligned++;
                    } else {
                        System.out.println("[Partially resolved] " + uiAnnotationEntry + ". now is " + instanceIdentifier.getSlot0Start() + "\t" + instanceIdentifier.getSlot0End());
                        partiallyAligned++;
                    }
                } else {
                    System.out.println("[Unresolved] " + uiAnnotationEntry.toString());
                    unresolved++;
                }
            }
        }
        inMemoryAnnotationStorage.convertToMappings().serialize(outputFile, true);
        System.out.println("[Fully resolved]:" + aligned);
        System.out.println("[Partially resolved]:" + partiallyAligned);
        System.out.println("[Unresolved]:" + unresolved);
    }

    public static class ConvertSpanToInstanceIdentifier implements Callable<Boolean> {

        final File docFile;
        public String docId;
        public List<UIAnnotationEntry> unresolvedAnnotationEntries;
        public Map<UIAnnotationEntry, InstanceIdentifier> resolvedAnnotationEntries;

        public ConvertSpanToInstanceIdentifier(File docFile, List<UIAnnotationEntry> unresolvedAnnotationEntries) {

            this.docFile = docFile;
            this.unresolvedAnnotationEntries = unresolvedAnnotationEntries;
            this.resolvedAnnotationEntries = new HashMap<>();
        }

        @Override
        public Boolean call() throws Exception {
            final SerifXMLLoader serifxmlLoader =
                    LearnItConfig.params().getOptionalBoolean("load_serifxml_with_sloppy_offsets").or(false) ?
                            new SerifXMLLoader.Builder().allowSloppyOffsets().build() :
                            SerifXMLLoader.createFrom(LearnItConfig.params());
            DocTheory docTheory = serifxmlLoader.loadFrom(this.docFile);
            this.docId = docTheory.docid().asString();
            Map<Pair<Integer, Integer>, SentenceTheory> sentSpanToIdxMap = new HashMap<>();
            for (SentenceTheory sentenceTheory : docTheory.nonEmptySentenceTheories()) {
                final int startCharOff = sentenceTheory.span().startCharOffset().asInt();
                final int endCharOff = sentenceTheory.span().endCharOffset().asInt();
                sentSpanToIdxMap.put(new Pair<>(startCharOff, endCharOff), sentenceTheory);
            }

            for (UIAnnotationEntry annotationEntry : unresolvedAnnotationEntries) {
                SentenceTheory sentenceTheory = sentSpanToIdxMap.get(new Pair<>(annotationEntry.sentCharStart, annotationEntry.sentCharEnd));
                EventMentions eventMentions = sentenceTheory.eventMentions();
                InstanceIdentifier instanceIdentifier = null;
                for (EventMention eventMention : eventMentions) {
                    if (eventMention.anchorNode().span().startTokenIndexInclusive() == annotationEntry.triggerStartTokenIdx && eventMention.anchorNode().span().endTokenIndexInclusive() == annotationEntry.triggerEndTokenIdx) {
                        instanceIdentifier = new InstanceIdentifier(
                                docId,
                                sentenceTheory.sentenceNumber(),
                                eventMention.anchorNode().span().startTokenIndexInclusive(),
                                eventMention.anchorNode().span().endTokenIndexInclusive(),
                                InstanceIdentifier.SpanningType.EventMention,
                                com.google.common.base.Optional.absent(),
                                "Generic",
                                false,
                                -1, -1, InstanceIdentifier.SpanningType.Empty,
                                Optional.absent(),
                                "NA",
                                false
                        );
                    }
                }
//                if (instanceIdentifier == null) {
//                    SynNode synRoot = sentenceTheory.parse().root().get();
//                    MySynNodeVisitor mySynNodeVisitor = new MySynNodeVisitor(new Pair<>(annotationEntry.triggerStartTokenIdx, annotationEntry.triggerEndTokenIdx));
//                    synRoot.preorderTraversal(mySynNodeVisitor);
//                    for (EventMention eventMention : eventMentions) {
//                        if (eventMention.anchorNode().span().startTokenIndexInclusive() == mySynNodeVisitor.resolvedSynNode.head().span().startTokenIndexInclusive() && eventMention.anchorNode().span().endTokenIndexInclusive() == mySynNodeVisitor.resolvedSynNode.head().span().endTokenIndexInclusive()) {
//                            instanceIdentifier = new InstanceIdentifier(
//                                    docId,
//                                    sentenceTheory.sentenceNumber(),
//                                    eventMention.anchorNode().span().startTokenIndexInclusive(),
//                                    eventMention.anchorNode().span().endTokenIndexInclusive(),
//                                    InstanceIdentifier.SpanningType.EventMention,
//                                    com.google.common.base.Optional.absent(),
//                                    "generic",
//                                    false,
//                                    -1, -1, InstanceIdentifier.SpanningType.Empty,
//                                    Optional.absent(),
//                                    "NA",
//                                    false
//                            );
//                        }
//                    }
//                }
//                if (instanceIdentifier == null) {
//                    for (EventMention eventMention : eventMentions) {
//                        if (eventMention.anchorNode().span().startTokenIndexInclusive() <= annotationEntry.triggerStartTokenIdx && eventMention.anchorNode().span().endTokenIndexInclusive() >= annotationEntry.triggerEndTokenIdx) {
//                            instanceIdentifier = new InstanceIdentifier(
//                                    docId,
//                                    sentenceTheory.sentenceNumber(),
//                                    eventMention.anchorNode().span().startTokenIndexInclusive(),
//                                    eventMention.anchorNode().span().endTokenIndexInclusive(),
//                                    InstanceIdentifier.SpanningType.EventMention,
//                                    com.google.common.base.Optional.absent(),
//                                    "Generic",
//                                    false,
//                                    -1, -1, InstanceIdentifier.SpanningType.Empty,
//                                    Optional.absent(),
//                                    "NA",
//                                    false
//                            );
//                        }
//                    }
//                }
                if (instanceIdentifier == null) {
                    List<String> originalSpan = new ArrayList<>();
                    for (int i = annotationEntry.triggerStartTokenIdx; i <= annotationEntry.triggerEndTokenIdx; ++i) {
                        originalSpan.add(sentenceTheory.tokenSequence().token(i).originalText().content().utf16CodeUnits());
                    }
                    System.out.println("[DEBUG] for event " + annotationEntry.toString() + ", original token is: " + String.join(" ", originalSpan));
                    System.out.println(sentenceTheory.span().tokenizedText().utf16CodeUnits());
                    System.out.println("we have these eventmentions:");
                    for (EventMention eventMention : sentenceTheory.eventMentions()) {
                        System.out.println("StartIdx: " + eventMention.anchorNode().span().startTokenIndexInclusive() + " ,EndIdx: " + eventMention.anchorNode().span().endTokenIndexInclusive() + " , event span text: " + eventMention.anchorNode().span().tokenizedText().utf16CodeUnits());
                    }
                }
                resolvedAnnotationEntries.put(annotationEntry, instanceIdentifier);
            }
            return true;
        }

        public static class MySynNodeVisitor implements com.bbn.serif.theories.SynNode.PreorderVisitor {

            Pair<Integer, Integer> tokenIdxSpan;
            SynNode resolvedSynNode;

            public MySynNodeVisitor(Pair<Integer, Integer> tokenIdxSpan) {
                this.tokenIdxSpan = tokenIdxSpan;
                resolvedSynNode = null;
            }

            @Override
            public boolean visitChildren(SynNode synNode) {
                if (resolvedSynNode == null) {
                    resolvedSynNode = synNode;
                } else {
                    if (synNode.span().startTokenIndexInclusive() >= this.tokenIdxSpan.getFirst() && synNode.span().endTokenIndexInclusive() <= this.tokenIdxSpan.getSecond()) {
                        resolvedSynNode = synNode;
                    }
                }
                return true;
            }
        }


    }

    public static class UIAnnotationEntry {
        public String docId;
        public int sentCharStart;
        public int sentCharEnd;
        public int triggerStartTokenIdx;
        public int triggerEndTokenIdx;
        public String eventType;
        public Annotation.FrozenState positive;

        public UIAnnotationEntry(String docId, int sentCharStart, int sentCharEnd, int triggerStartTokenIdx, int triggerEndTokenIdx, String eventType, Annotation.FrozenState positive) {
            this.docId = docId;
            this.sentCharStart = sentCharStart;
            this.sentCharEnd = sentCharEnd;
            this.triggerStartTokenIdx = triggerStartTokenIdx;
            this.triggerEndTokenIdx = triggerEndTokenIdx;
            this.eventType = eventType;
            this.positive = positive;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof UIAnnotationEntry)) return false;
            UIAnnotationEntry that = (UIAnnotationEntry) o;
            return this.docId.equals(that.docId) &&
                    this.sentCharStart == that.sentCharStart &&
                    this.sentCharEnd == that.sentCharEnd &&
                    this.triggerStartTokenIdx == that.triggerStartTokenIdx &&
                    this.triggerEndTokenIdx == that.triggerEndTokenIdx &&
                    this.eventType.equals(that.eventType) &&
                    this.positive.equals(that.positive);
        }

        @Override
        public int hashCode() {
            int prime = 31;
            int ret = docId.hashCode();
            ret = ret * prime + sentCharStart;
            ret = ret * prime + sentCharEnd;
            ret = ret * prime + triggerStartTokenIdx;
            ret = ret * prime + triggerEndTokenIdx;
            ret = ret * prime + eventType.hashCode();
            ret = ret * prime + positive.hashCode();
            return ret;
        }

        @Override
        public String toString() {
            return this.docId + "\t" + this.sentCharStart + "\t" + this.sentCharEnd + "\t" + this.triggerStartTokenIdx + "\t" + this.triggerEndTokenIdx + "\t" + this.eventType;
        }


    }

}
