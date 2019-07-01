package com.bbn.akbc.neolearnit.util;

import com.bbn.akbc.common.Pair;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.bue.common.files.FileUtils;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.io.SerifXMLLoader;
import com.bbn.serif.io.SerifXMLWriter;
import com.bbn.serif.theories.*;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class AddEventMentionFromInstanceIdentifier {

    public static class EventEntry {
        final public int sentIdx;
        final public int triggerStartTokenIdx;
        final public int triggerEndTokenIdx;
        final public String eventType;

        public EventEntry(int sentIdx, int triggerStartTokenIdx, int triggerEndTokenIdx,String eventType) {
            this.sentIdx = sentIdx;
            this.triggerStartTokenIdx = triggerStartTokenIdx;
            this.triggerEndTokenIdx = triggerEndTokenIdx;
            this.eventType = eventType;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof EventEntry)) return false;
            EventEntry that = (EventEntry) o;
            return this.eventType.equals(that.eventType) &&
                    this.sentIdx == that.sentIdx &&
                    this.triggerStartTokenIdx == that.triggerStartTokenIdx &&
                    this.triggerEndTokenIdx == that.triggerEndTokenIdx;
        }

        @Override
        public int hashCode() {
            int prime = 31;
            int ret = eventType.hashCode();
            ret = ret * prime + sentIdx;
            ret = ret * prime + triggerStartTokenIdx;
            ret = ret * prime + triggerEndTokenIdx;
            return ret;
        }

        @Override
        public String toString(){
            return this.eventType+"\t"+this.sentIdx+"\t"+this.triggerStartTokenIdx+"\t"+this.triggerEndTokenIdx;
        }


    }

    public static void main(String[] args) throws Exception {
        String inputSerifListOrDirectoryPath = args[0];
        String resolvedAnnotationExample = args[1];
        String outputDirPathStr = args[2];
        File inputSerifListOrDirectory = new File(inputSerifListOrDirectoryPath);
        Map<String, String> docIdToDocPath = new HashMap<>();
        if (inputSerifListOrDirectory.isDirectory()) {
            for (File file : inputSerifListOrDirectory.listFiles()) {
                String fileName = file.getName();
                fileName = fileName.substring(0, fileName.lastIndexOf("."));
                docIdToDocPath.put(fileName, file.getAbsolutePath());
            }
        } else {
            List<String> fileList = GeneralUtils.readLinesIntoList(inputSerifListOrDirectoryPath);
            for (String fileStr : fileList) {
                File file = new File(fileStr);
                String fileName = file.getName();
                fileName = fileName.substring(0, fileName.lastIndexOf("."));
                docIdToDocPath.put(fileName, file.getAbsolutePath());
            }
        }
        Map<String, Set<InstanceIdentifier>> docIdToInstanceIdentifierSet = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper(new JsonFactory());
        List<Map> annotationExamples = objectMapper.readValue(new File(resolvedAnnotationExample), List.class);
        for (Map annotationExample : annotationExamples) {
            InstanceIdentifier instanceIdentifier = objectMapper.convertValue(annotationExample.get("learnit_instanceidentifier"), InstanceIdentifier.class);
            Set<InstanceIdentifier> buf = docIdToInstanceIdentifierSet.getOrDefault(instanceIdentifier.getDocid(), new HashSet<>());
            buf.add(instanceIdentifier);
            docIdToInstanceIdentifierSet.put(instanceIdentifier.getDocid(), buf);
        }
        File outputDir = new File(outputDirPathStr);

        FileUtils.recursivelyDeleteDirectory(outputDir);
        outputDir.mkdirs();
        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (String docId : docIdToDocPath.keySet()) {
            if (!docIdToInstanceIdentifierSet.keySet().contains(docId)) {
                Path from = Paths.get(docIdToDocPath.get(docId)); //convert from File to Path
                Path to = Paths.get(outputDirPathStr + File.separator + docId + ".xml"); //convert from String to Path
                Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
            } else {
                tasks.add(new SingleDocumentWorker(docIdToDocPath.get(docId), docIdToInstanceIdentifierSet.get(docId), outputDirPathStr));
            }
        }
        int CONCURRENCY = 12;
        ExecutorService service = Executors.newFixedThreadPool(CONCURRENCY);
        Collection<Future<Boolean>> results = service.invokeAll(tasks);
        for (Future<Boolean> result : results) {
            result.get();
        }
        service.shutdown();
    }

    public static class SingleDocumentWorker implements Callable<Boolean> {
        final String docPath;
        final Set<InstanceIdentifier> instanceIdentifierSet;
        final String outputDir;
        public SingleDocumentWorker(String docPath, Set<InstanceIdentifier> instanceIdentifierSet,String outputDir){
            this.docPath = docPath;
            this.instanceIdentifierSet = instanceIdentifierSet;
            this.outputDir = outputDir;
        }

        @Override
        public Boolean call() throws Exception {
            final SerifXMLLoader serifxmlLoader = new SerifXMLLoader.Builder().allowSloppyOffsets().build();
            DocTheory docTheory = serifxmlLoader.loadFrom(new File(this.docPath));
            Map<Integer,Set<EventEntry>> pendingAddEventSentenceToEventMap = new HashMap<>();
            for(InstanceIdentifier instanceIdentifier:instanceIdentifierSet){
                if(instanceIdentifier.getSlot0SpanningType().equals(InstanceIdentifier.SpanningType.EventMention)){
                    Optional<Spanning> leftSpanning = InstanceIdentifier.getSpanning(docTheory.sentenceTheory(instanceIdentifier.getSentid()),instanceIdentifier.getSlot0Start(),instanceIdentifier.getSlot0End(),instanceIdentifier.getSlotEntityType(0));
                    if ((!leftSpanning.isPresent() || !(leftSpanning.get() instanceof EventMention)) && instanceIdentifier.getSlot0Start() >= 0 && instanceIdentifier.getSlot0End() >= 0) {
                        Set<EventEntry> buf = pendingAddEventSentenceToEventMap.getOrDefault(instanceIdentifier.getSentid(),new HashSet<>());
                        buf.add(new EventEntry(instanceIdentifier.getSentid(),instanceIdentifier.getSlot0Start(),instanceIdentifier.getSlot0End(),instanceIdentifier.getSlotEntityType(0)));
                        pendingAddEventSentenceToEventMap.put(instanceIdentifier.getSentid(),buf);
                    }
                }
                if(instanceIdentifier.getSlot1SpanningType().equals(InstanceIdentifier.SpanningType.EventMention)){
                    Optional<Spanning> rightSpanning = InstanceIdentifier.getSpanning(docTheory.sentenceTheory(instanceIdentifier.getSentid()),instanceIdentifier.getSlot1Start(),instanceIdentifier.getSlot1End(),instanceIdentifier.getSlotEntityType(1));
                    if ((!rightSpanning.isPresent() || !(rightSpanning.get() instanceof EventMention)) && instanceIdentifier.getSlot1Start() >= 0 && instanceIdentifier.getSlot1End() >= 0) {
                        Set<EventEntry> buf = pendingAddEventSentenceToEventMap.getOrDefault(instanceIdentifier.getSentid(),new HashSet<>());
                        buf.add(new EventEntry(instanceIdentifier.getSentid(),instanceIdentifier.getSlot1Start(),instanceIdentifier.getSlot1End(),instanceIdentifier.getSlotEntityType(1)));
                        pendingAddEventSentenceToEventMap.put(instanceIdentifier.getSentid(),buf);
                    }
                }
            }
            DocTheory.Builder docBuilder = docTheory.modifiedCopyBuilder();
            for(Integer sentId:pendingAddEventSentenceToEventMap.keySet()){
                SentenceTheory sentenceTheory = docTheory.sentenceTheory(sentId);
                final SentenceTheory.Builder sentBuilder = sentenceTheory.modifiedCopyBuilder();
                final ImmutableList.Builder<EventMention> newEventMentions =
                        ImmutableList.builder();
                // keep existing event mentions
                for(EventMention eventMention : sentenceTheory.eventMentions()) {
                    newEventMentions.add(eventMention);
                }
                for(EventEntry eventEntry:pendingAddEventSentenceToEventMap.get(sentId)){
                    MySynNodeVisitor mySynNodeVisitor = new MySynNodeVisitor(new Pair<>(eventEntry.triggerStartTokenIdx,eventEntry.triggerEndTokenIdx));
                    SynNode desiredAnchor = null;
                    sentenceTheory.parse().root().get().preorderTraversal(mySynNodeVisitor);
                    desiredAnchor = mySynNodeVisitor.resolvedSynNode;
                    if(desiredAnchor == null){
                        System.out.println("Cannot find SynNode for "+eventEntry.toString());
                        continue;
                    }
                    EventMention newEventMention = EventMention
                            .builder(Symbol.from(eventEntry.eventType))
                            .setAnchorNode(desiredAnchor)
                            .setScore(0.17)
                            .build();
                    newEventMentions.add(newEventMention);
                }
                sentBuilder.eventMentions(new EventMentions.Builder()
                        .eventMentions(newEventMentions.build())
                        .build());
                docBuilder.replacePrimarySentenceTheory(sentenceTheory, sentBuilder.build());
            }
            SerifXMLWriter serifXMLWriter = SerifXMLWriter.create();
            serifXMLWriter.saveTo(docBuilder.build(), new File(this.outputDir + File.separator + docTheory.docid().asString() + ".xml"));
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
//                System.out.println("[SynNode Span]: "+synNode.span().startTokenIndexInclusive()+", "+synNode.span().endTokenIndexInclusive());
                if (synNode.span().startToken().index() == this.tokenIdxSpan.getFirst() && synNode.span().endToken().index() == this.tokenIdxSpan.getSecond()) {
                    resolvedSynNode = synNode;
                    return false;
                }

                return true;
            }
        }
    }
}
