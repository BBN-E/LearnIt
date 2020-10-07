package com.bbn.akbc.neolearnit.util;

import com.bbn.bue.common.files.FileUtils;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.io.SerifXMLLoader;
import com.bbn.serif.io.SerifXMLWriter;
import com.bbn.serif.theories.*;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import java.io.File;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class GenericEventTimeDetector {
    public static float ARG_SCORE = 0.8f;
    public static Set<ValueMention> findValidValueMentionCandidateSet(ValueMentions valueMentions){
        Set<ValueMention> ret = new HashSet<>();
        for(ValueMention valueMention:valueMentions){
            Optional<Value> potentialValueObj = valueMention.documentValue();
            if(potentialValueObj.isPresent()){
                Value potentialTimeValObj = potentialValueObj.get();
                if (potentialTimeValObj.asTimex2().isPresent()) {
                    ret.add(valueMention);
                }
            }
        }
        return ret;
    }
    public static class SingleDocumentWorker implements Callable<Boolean> {
        final String docPath;
        final String outputFolder;
        public SingleDocumentWorker(String docPath,String outputFolder){
            this.docPath = docPath;
            this.outputFolder = outputFolder;
        }
        @Override
        public Boolean call() throws Exception {
            final SerifXMLLoader serifxmlLoader = new SerifXMLLoader.Builder().build();
            DocTheory docTheory = serifxmlLoader.loadFrom(new File(this.docPath));
            DocTheory.Builder docBuilder = docTheory.modifiedCopyBuilder();
            for(SentenceTheory sentenceTheory:docTheory.nonEmptySentenceTheories()){
                if(sentenceTheory.eventMentions().size()>0){
                    Set<ValueMention> potentialValueMention = findValidValueMentionCandidateSet(sentenceTheory.valueMentions());
                    if(potentialValueMention.size()>0){
                        final SentenceTheory.Builder sentBuilder = sentenceTheory.modifiedCopyBuilder();
                        final ImmutableList.Builder<EventMention> newEventMentions =
                                ImmutableList.builder();
                        for(EventMention eventMention:sentenceTheory.eventMentions()){
                            boolean shouldAddEventTime = true;
                            for(EventMention.Argument argument:eventMention.arguments()){
                                if(argument.role().asString().toLowerCase().contains("time")){
                                    shouldAddEventTime = false;
                                    break;
                                }
                            }
                            if(shouldAddEventTime){
                                EventMention.Builder modifiedEventMention = eventMention.modifiedCopyBuilder();
                                List<EventMention.Argument> currentArgs = new ArrayList<>(eventMention.arguments());
                                for(ValueMention valueMention:potentialValueMention){
                                    currentArgs.add(EventMention.ValueMentionArgument.from(Symbol.from("has_time"),valueMention,ARG_SCORE));
                                }
                                modifiedEventMention.setArguments(currentArgs);
                            }
                            newEventMentions.add(eventMention);
                        }
                        sentBuilder.eventMentions(new EventMentions.Builder()
                                .eventMentions(newEventMentions.build())
                                .build());
                        docBuilder.replacePrimarySentenceTheory(sentenceTheory,sentBuilder.build());
                    }
                }
            }
            SerifXMLWriter serifXMLWriter = SerifXMLWriter.create();
            serifXMLWriter.saveTo(docBuilder.build(), new File(this.outputFolder + File.separator + docTheory.docid().asString() + ".xml"));
            return true;
        }
    }
    public static void main(String[] args) throws Exception{
        String inputSerifListOrDirectoryPath = args[0];
        String outputDirPathStr = args[1];
        File inputSerifListOrDirectory = new File(inputSerifListOrDirectoryPath);
        List<String> docPaths = new ArrayList<>();
        if (inputSerifListOrDirectory.isDirectory()) {
            for (File file : inputSerifListOrDirectory.listFiles()) {
                docPaths.add(file.getAbsolutePath());
            }
        } else {
            List<String> fileList = GeneralUtils.readLinesIntoList(inputSerifListOrDirectoryPath);
            for (String fileStr : fileList) {
                File file = new File(fileStr);
                docPaths.add(file.getAbsolutePath());
            }
        }
        File outputDir = new File(outputDirPathStr);
        FileUtils.recursivelyDeleteDirectory(outputDir);
        outputDir.mkdirs();
        List<Callable<Boolean>> tasks = new ArrayList<>();
        for(String docPath:docPaths){
            tasks.add(new SingleDocumentWorker(docPath,outputDirPathStr));
        }
        int CONCURRENCY = 12;
        ExecutorService service = Executors.newFixedThreadPool(CONCURRENCY);
        Collection<Future<Boolean>> results = service.invokeAll(tasks);
        for (Future<Boolean> result : results) {
            result.get();
        }
        service.shutdown();
    }
}
