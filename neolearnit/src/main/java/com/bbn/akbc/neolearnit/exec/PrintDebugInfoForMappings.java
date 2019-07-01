package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.common.targets.TargetFactory;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.utility.FileUtil;
import com.bbn.akbc.utility.Pair;
import com.bbn.serif.theories.*;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;


import java.io.*;
import java.util.*;

public class PrintDebugInfoForMappings {

    static Table<String,Pair<String,String>,Set<String>> debugSections = HashBasedTable.create();
    static Table<String,Pair<String,String>,String> sectionIdToSlotPairToSentence = HashBasedTable.create();

    public static void main(String [] args) throws IOException {
        String params = args[0];
        String strListJsonFiles = args[1];
        String outputFile = args[2];

        PrintWriter output = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outputFile)));

        Target target = TargetFactory.makeEverythingTarget();

        LearnItConfig.loadParams(new File(params));
        List<String> listJsonFiles = FileUtil.readLinesIntoList(strListJsonFiles);

//        List<String> debugLines = new ArrayList<String>();


        for (String strJsonFile : listJsonFiles) {
            System.out.println("Load mapping file " + strJsonFile);

            Mappings mappings = Mappings.deserialize(new File(strJsonFile), true);


            for (InstanceIdentifier iid : mappings.getInstance2Pattern().getAllInstances()) {

                addDebugSectionToMap(iid, mappings.getInstance2Pattern().getPatterns(iid),
                        target);
            }
        }
        List<String> sectionIds = new ArrayList<String>(debugSections.rowKeySet());
        Collections.sort(sectionIds);

        for(String sectionId : sectionIds){
            List<String> section = new ArrayList<String>();
            section.add(sectionId);
            for(Map.Entry<Pair<String,String>,Set<String>> map : debugSections.row(sectionId).entrySet()){
                Pair<String,String> slotPair = map.getKey();
                String sentence = sectionIdToSlotPairToSentence.get(sectionId,slotPair);
                section.add(sentence);
                for(String pattern : map.getValue()){
                    section.add(pattern);
                }
                section.add("");
            }
            section.add(section.size()-1,"-----------------");
            output.println(Joiner.on("\n").join(section));
        }
        output.close();
    }

    static void addDebugSectionToMap(InstanceIdentifier instanceIdentifier,
                                     Collection<LearnitPattern> learnitPatterns,
                                     Target target) {
        MatchInfo matchInfo = instanceIdentifier.reconstructMatchInfo(target);

        Spanning spanning0 = matchInfo.getPrimaryLanguageMatch().getSlot0().get();
        Spanning spanning1 = matchInfo.getPrimaryLanguageMatch().getSlot1().get();

        if(spanning0 instanceof EventMention && spanning1 instanceof EventMention) {
            EventMention e0 = (EventMention) spanning0;
            EventMention e1 = (EventMention) spanning1;

//            debugOutput = instanceIdentifier.getDocid()+"\n";
            String verb0 = e0.anchorNode().span().originalText().content().utf16CodeUnits().replace("\n", " ");
            String verb1 = e1.anchorNode().span().originalText().content().utf16CodeUnits().replace("\n", " ");
            String matchInfoStr = matchInfo.getPrimaryLanguageMatch().toString();
            String sectionId = matchInfoStr.split("\n")[0];
            String sectionIdVar1 = sectionId+" verb0="+verb0+"\tverb1="+verb1;
            String sectionIdVar2 = sectionId+" verb0="+verb1+"\tverb1="+verb0;
            String slotPairStr = matchInfoStr.split("\n")[1];
            String slot0 = slotPairStr.substring(slotPairStr.indexOf("<SLOT0>")+"<SLOT0>".length(),slotPairStr.lastIndexOf("</SLOT0>")).replace("<SLOT1>","").replace("</SLOT1>","");
            String slot1 = slotPairStr.substring(slotPairStr.indexOf("<SLOT1>")+"<SLOT1>".length(),slotPairStr.lastIndexOf("</SLOT1>")).replace("<SLOT0>","").replace("</SLOT0>","");
            Pair<String,String> slotPair = new Pair(slot0,slot1);
            sectionIdToSlotPairToSentence.put(sectionIdVar1,slotPair,slotPairStr); //map the original sentence to original section id and slot pair
            if(debugSections.containsRow(sectionIdVar2)){
                sectionId = sectionIdVar2;
                slotPair = new Pair<String, String>(slot1,slot0);
            }else{
                sectionId = sectionIdVar1;
            }
            Set<String> existingPatterns = debugSections.get(sectionId,slotPair);
            if(existingPatterns==null){
                existingPatterns = new HashSet<String>();
            }
//            Set<String> normalizedExistingPatterns =  FluentIterable.from(existingPatterns).transform(new Function<String, String>() {
//                @Override
//                public String apply(String pattern){
//                    return pattern.replace("{0}","LEARNIT_SLOT").replace("{1}","LEARNIT_SLOT");
//                }
//            }).toSet();
            for(LearnitPattern learnitPattern : learnitPatterns) {
                if(learnitPattern.isCompletePattern()) {
                    String patternToAdd = learnitPattern.getClass().getSimpleName()+"\t"+
                            learnitPattern.toIDString();
//                    String normalizedPattern = patternToAdd.replace("{0}","LEARNIT_SLOT").replace("{1}","LEARNIT_SLOT");
//                    if(!normalizedExistingPatterns.contains(normalizedPattern)){
                        existingPatterns.add(patternToAdd);
//                    }
                }
            }
            debugSections.put(sectionId,slotPair,existingPatterns);
        }
    }
}
