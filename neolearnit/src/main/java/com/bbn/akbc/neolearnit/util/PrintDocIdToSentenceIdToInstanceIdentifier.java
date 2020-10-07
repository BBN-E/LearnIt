package com.bbn.akbc.neolearnit.util;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.LearnItObservation;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.bue.common.files.FileUtils;
import com.bbn.serif.io.SerifXMLLoader;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.EventMention;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PrintDocIdToSentenceIdToInstanceIdentifier {
    public static void main(String[] args) {
    }

    public static void run(Mappings mappings, String strFileList, Target target) throws IOException {
        Map<String, DocTheory> docId2docTheory = readSerifXml(strFileList);

        // InstanceIdentifier instanceIdentifier;
        // MatchInfo.LanguageMatchInfo languageMatchInfo;
        // languageMatchInfo.markedUpTokenString()

        final Map<String,Map<Integer,Multimap<InstanceIdentifier,LearnItObservation>>> map = new TreeMap();

        for(InstanceIdentifier instanceIdentifier: mappings.getInstance2Seed().getAllInstances()){
            Collection<Seed> seedCollection = mappings.getSeedsForInstance(instanceIdentifier);
            for(Seed seed : seedCollection){

                map.computeIfAbsent(instanceIdentifier.getDocid(),k -> new TreeMap<>()).computeIfAbsent(instanceIdentifier.getSentid(),k -> ArrayListMultimap.create()).put(instanceIdentifier,seed);
            }
        }
        for(InstanceIdentifier instanceIdentifier: mappings.getInstance2Pattern().getAllInstances()){
            Collection<LearnitPattern> learnitPatternCollection = mappings.getPatternsForInstance(instanceIdentifier);
            for(LearnitPattern learnitPattern : learnitPatternCollection){
                map.computeIfAbsent(instanceIdentifier.getDocid(),k-> new TreeMap<>()).computeIfAbsent(instanceIdentifier.getSentid(),k -> ArrayListMultimap.create()).put(instanceIdentifier,learnitPattern);
            }
        }

        for(String docId: map.keySet()){
            if(docId2docTheory.containsKey(docId)) {
                DocTheory docTheory = docId2docTheory.get(docId);
                for (int sentId=0; sentId<docTheory.numSentences(); sentId++) {
                    try{
                        System.out.println("[PrintDocIdToSentenceIdMapping] DocId: " + docId + "\tsentId: " + sentId + "\tText: " + docTheory.sentenceTheory(sentId).tokenSpan().originalText().content().utf16CodeUnits().replace("\n", " "));

                    }
                    catch (UnsupportedOperationException e){
                        System.out.println("[PrintDocIdToSentenceIdMapping] Empty TokenSpan");
                    }
                    // print event mentions
                    for(EventMention eventMention : docTheory.sentenceTheory(sentId).eventMentions()) {
                        System.out.println("[PrintDocIdToSentenceIdMapping] DocId: " + docId + "\tsentId: " + sentId + "\tNodeMentionAnnotation: " + eventMention.anchorNode().tokenSpan().tokenizedText(docTheory));
                    }

                    boolean foundRelationMention = map.get(docId).containsKey(sentId);
                    if(foundRelationMention){
                        for(InstanceIdentifier instanceIdentifier : map.get(docId).get(sentId).keySet()){
                            MatchInfo.LanguageMatchInfo languageMatchInfo = instanceIdentifier.reconstructMatchInfo(target).getPrimaryLanguageMatch();
                            System.out.println("[PrintDocIdToSentenceIdMapping] DocId: " + docId + "\tsentId: " + sentId + "\tEdgeMentionAnnotation: " + languageMatchInfo.markedUpTokenString());

                            // print pattern
                            for (LearnItObservation record : map.get(docId).get(sentId).get(instanceIdentifier)) {
                                System.out.println("[PrintDocIdToSentenceIdMapping] DocId: " + docId + "\tsentId: " + sentId + "\tLearnItObversation: " + record.toIDString());
                            }
                        }
                    }
                    else{
                        System.out.println("[PrintDocIdToSentenceIdMapping] DocId: " + docId + "\tsentId: " + sentId + "\tLearnIt cannot find any EdgeMentionAnnotation in this sentence.");
                    }
                }
            }
        }
    }

    static Map<String, DocTheory> readSerifXml(String strFileList) throws IOException {
        Map<String, DocTheory> docId2docTheory = new HashMap<String, DocTheory>();

        List<File> fileList = FileUtils.loadFileList(new File(strFileList));
        final SerifXMLLoader serifxmlLoader =
                LearnItConfig.params().getOptionalBoolean("load_serifxml_with_sloppy_offsets").or(false)?
                        new SerifXMLLoader.Builder().allowSloppyOffsets().build():
                        SerifXMLLoader.createFrom(LearnItConfig.params());
        for (final File serifxml : fileList) {
            DocTheory docTheory = serifxmlLoader.loadFrom(serifxml);
            docId2docTheory.put(docTheory.docid().asString(), docTheory);
        }

        return docId2docTheory;
    }
}
