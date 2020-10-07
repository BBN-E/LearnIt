package com.bbn.akbc.neolearnit.tmp;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.util.SourceListsReader;
import com.bbn.serif.io.SerifXMLLoader;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.Spanning;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.io.Files;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class AddLegacyEventAndArgumentAnnotationInSerifXML {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LegacyArgumentEntry{
        @JsonProperty
        public String doc_id;
        @JsonProperty
        public int sent_idx;
        @JsonProperty
        public int trig_start;
        @JsonProperty
        public int trig_end;
        @JsonProperty
        public String event_type;
        @JsonProperty
        public int arg_start;
        @JsonProperty
        public int arg_end;
        @JsonProperty
        public String arg_type;

        @JsonCreator
        public LegacyArgumentEntry(@JsonProperty("doc_id") String doc_id,@JsonProperty("sent_idx") int sent_idx,@JsonProperty("trig_start") int trig_start,@JsonProperty("trig_end") int trig_end,@JsonProperty("event_type") String event_type,@JsonProperty("arg_start") int arg_start,@JsonProperty("arg_end") int arg_end,@JsonProperty("arg_type") String arg_type){
            this.doc_id = doc_id;
            this.sent_idx = sent_idx;
            this.trig_start = trig_start;
            this.trig_end = trig_end;
            this.event_type = event_type;
            this.arg_start = arg_start;
            this.arg_end = arg_end;
            this.arg_type = arg_type;
        }
    }

    public static void main(String[] args) throws Exception {
        LearnItConfig.loadParams(new File("/home/hqiu/ld100/learnit/params/learnit/runs/cx_m5_wm_m6_wm_m6_isi_cx_m9_cx_m12_wm_m12_all_verbs_and_nouns.params"));
        ObjectMapper objectMapper = new ObjectMapper(new JsonFactory());
        Map<String, List<LegacyArgumentEntry>> docIdToEntries = new HashMap<>();
        for (String line : Files.readLines(new File("/nfs/raid88/u10/users/hqiu/annotation/nlplingo/argument/before_053019/aligned.ljson"), StandardCharsets.UTF_8)) {
            LegacyArgumentEntry legacyArgumentEntry = objectMapper.readValue(line, LegacyArgumentEntry.class);
            List<LegacyArgumentEntry> buf = docIdToEntries.getOrDefault(legacyArgumentEntry.doc_id, new ArrayList<>());
            buf.add(legacyArgumentEntry);
            docIdToEntries.put(legacyArgumentEntry.doc_id, buf);
        }
        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (String docId : docIdToEntries.keySet()) {
            String docPath = new File(SourceListsReader.getFullPath(docId)).getAbsolutePath();
            tasks.add(new SingleDocumentWorker(docPath, docIdToEntries.get(docId)));
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
        String docPath;
        List<LegacyArgumentEntry> legacyArgumentEntryList;
        public SingleDocumentWorker(String docPath,List<LegacyArgumentEntry> legacyArgumentEntryList){
            this.docPath = docPath;
            this.legacyArgumentEntryList = legacyArgumentEntryList;
        }
        @Override
        public Boolean call() throws Exception {
            final SerifXMLLoader serifxmlLoader = new SerifXMLLoader.Builder().allowSloppyOffsets().build();
            DocTheory docTheory = serifxmlLoader.loadFrom(new File(this.docPath));
            for(LegacyArgumentEntry legacyArgumentEntry: this.legacyArgumentEntryList){
                // Left is event and right is argument
                // @hqiu: Although the rest system is Event instead of Generic, we have a huge amount of legacy annotation data which highly rely on Generic
                String docId = legacyArgumentEntry.doc_id;
                int sentId = legacyArgumentEntry.sent_idx;
                int triggerStart = legacyArgumentEntry.trig_start;
                int triggerEnd = legacyArgumentEntry.trig_end;
                int argumentStart = legacyArgumentEntry.arg_start;
                int argumentEnd = legacyArgumentEntry.arg_end;
                String argType = legacyArgumentEntry.arg_type;
                SentenceTheory sentenceTheory = docTheory.sentenceTheory(sentId);
                Optional<Spanning> leftSpan = InstanceIdentifier.getSpanning(sentenceTheory, triggerStart, triggerEnd, InstanceIdentifier.SpanningType.EventMention);
                Optional<Spanning> rightSpan = InstanceIdentifier.getSpanning(sentenceTheory, argumentStart, argumentEnd, InstanceIdentifier.SpanningType.Empty);
                if(!rightSpan.isPresent()){
                    System.out.println("[NO]We cannot find Mention/ValueMention for "+docId+", "+sentId+", "+argumentStart+", "+argumentEnd);
                }
                else{
                    System.out.println("[Yes]");
                }
            }
            return true;
        }
    }
}
