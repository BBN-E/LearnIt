package com.bbn.akbc.neolearnit.util;

import com.bbn.akbc.common.Pair;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.EventMention;
import com.bbn.serif.theories.SentenceTheory;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PrintSerifDocAndMappings {

    public static void main(String[] args) throws Exception {
        String params = "/home/hqiu/ld100/learnit/params/learnit/runs/learnit_dev.params";
        LearnItConfig.loadParams(new File(params));

        String mappingPath = "/nfs/raid88/u10/users/hqiu/learnit/learnit_dev.111219.v2/source_mappings/unary_event-1/freq_1_1/mappings.master.sjson";

        Mappings mappings = Mappings.deserialize(new File(mappingPath), true);


        Map<Pair<String, Integer>, Set<InstanceIdentifier>> docSentToLearnItPattern = new HashMap<>();

        for (InstanceIdentifier instanceIdentifier : mappings.getPatternInstances()) {
            String docId = instanceIdentifier.getDocid();
            int sentId = instanceIdentifier.getSentid();
            Set<InstanceIdentifier> buffer = docSentToLearnItPattern.getOrDefault(new Pair<>(docId, sentId), new HashSet<>());
            buffer.add(instanceIdentifier);
            docSentToLearnItPattern.put(new Pair<>(docId, sentId), buffer);
        }

        InstanceIdentifier.preLoadDocThoery(mappings.getPatternInstances());

        for (Pair<String, Integer> docIdSentIdPair : docSentToLearnItPattern.keySet()) {
            String docId = docIdSentIdPair.getFirst();
            int sentId = docIdSentIdPair.getSecond();
            for (InstanceIdentifier instanceIdentifier : docSentToLearnItPattern.get(docIdSentIdPair)) {

                DocTheory docTheory = InstanceIdentifier.getDocTheoryFromDocID(docId).get();
                SentenceTheory sentenceTheory = docTheory.sentenceTheory(sentId);
                EventMention eventMention = (EventMention) (InstanceIdentifier.getSpanning(sentenceTheory, instanceIdentifier.getSlot0Start(), instanceIdentifier.getSlot0End(), instanceIdentifier.getSlot0SpanningType()).get());
                for (LearnitPattern learnitPattern : mappings.getPatternsForInstance(instanceIdentifier)) {
                    System.out.println(String.format("[HQIUDEBUG]\t%s\t%s\t%s\t%s", docId, eventMention.anchorNode().span().tokenizedText().utf16CodeUnits(), sentenceTheory.span().tokenizedText().utf16CodeUnits(), learnitPattern.toIDString()));
                }
            }
        }


    }
}
