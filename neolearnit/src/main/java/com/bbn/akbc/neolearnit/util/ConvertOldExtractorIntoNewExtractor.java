package com.bbn.akbc.neolearnit.util;

import com.bbn.akbc.neolearnit.common.BBNInternalOntology;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.labelers.TargetAndScoreTableLabeler;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.pattern.SerifPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.scoring.scores.PatternScore;
import com.bbn.akbc.neolearnit.scoring.scores.SeedScore;
import com.bbn.akbc.neolearnit.scoring.tables.AbstractScoreTable;
import com.bbn.akbc.neolearnit.scoring.tables.PatternScoreTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.io.File;
import java.util.*;

public class ConvertOldExtractorIntoNewExtractor {
    public static Set<LearnitPattern> findSimilarPatterns(LearnitPattern srcLearnitPattern, Mappings mappings){
        Map<LearnitPattern,Double> patternToOverlapRatio = new HashMap<>();
        Set<InstanceIdentifier> sourceInstances = new HashSet<>(mappings.getInstancesForPattern(srcLearnitPattern));
        if(sourceInstances.size()<1)return new HashSet<>();
        Set<LearnitPattern> dstPatternCandidateSet = new HashSet<>(mappings.getPatternsForInstances(sourceInstances));
        for(LearnitPattern dstLearnitPattern:dstPatternCandidateSet){
            if(srcLearnitPattern == dstLearnitPattern || !(dstLearnitPattern instanceof SerifPattern)){
                continue;
            }
            Set<InstanceIdentifier> destinationInstances = new HashSet<>(mappings.getInstancesForPattern(dstLearnitPattern));
            Set<InstanceIdentifier> sharedInstances = Sets.intersection(destinationInstances,sourceInstances);
            Set<InstanceIdentifier> extraInstances = Sets.difference(destinationInstances,sourceInstances);
            patternToOverlapRatio.put(dstLearnitPattern, (double)sharedInstances.size()/sourceInstances.size() + ((double)sourceInstances.size()/(sourceInstances.size()+extraInstances.size())));
        }
        List<LearnitPattern> newPatternList = new ArrayList<>(patternToOverlapRatio.keySet());
        Collections.sort(newPatternList, new Comparator<LearnitPattern>() {
            @Override
            public int compare(LearnitPattern o1, LearnitPattern o2) {
                return (int)((patternToOverlapRatio.get(o2) - patternToOverlapRatio.get(o1))*10000000);
            }
        });
        Set<LearnitPattern> ret = new HashSet<>();
        if(newPatternList.size()>0 && patternToOverlapRatio.get(newPatternList.get(0)) > 1.8){
            ret.add(newPatternList.get(0));
        }
        return ret;
    }

    public static void addPatternToTable(LearnitPattern learnitPattern,PatternScoreTable patternScoreTable,boolean isGood){
        PatternScore patternScore = patternScoreTable.getScoreOrDefault(learnitPattern);
        if (patternScore.isFrozen()) patternScore.unfreeze();
        patternScore.setPrecision(isGood?0.95:0.05);
        patternScore.setConfidence(1.0);
        patternScore.freezeScore(0);
    }

    public static void main(String[] args) throws Exception{
        LearnItConfig.loadParams(new File(args[0]));
//        Map<String, TargetAndScoreTables> e = GeneralUtils.loadExtractors(args[1]);
        BBNInternalOntology.BBNInternalOntologyNode ontologyNode = BBNInternalOntology.BBNInternalOntologyNode.fromInternalOntologyFile(new File(args[1]));
        Set<String> currentOntologyNodeNames = new HashSet<>();
        Map<String, BBNInternalOntology.BBNInternalOntologyNode> nodeIdToNodeMap = ontologyNode.getPropToNodeMap(new BBNInternalOntology.BBNInternalOntologyNode.PropStringGetter("originalKey"));
        currentOntologyNodeNames.addAll(nodeIdToNodeMap.keySet());

        File dir = new File(String.format("%s/inputs/extractors/", LearnItConfig.get("learnit_root")));
        Map<String,TargetAndScoreTables> e = new HashMap<>();
        for (File file : dir.listFiles()){
            String extractorName = file.getName().replace(".json","");
            if(currentOntologyNodeNames.contains(extractorName)){
                TargetAndScoreTables ex = TargetAndScoreTables.deserialize(file);
                e.put(ex.getTarget().getName(), ex);
            }
        }
        Mappings mappings = Mappings.deserialize(new File(args[2]),true);
        String outputDir = String.format("%s/inputs/extractors/", LearnItConfig.get("learnit_root"));
        for(String tag:e.keySet()){
            TargetAndScoreTables targetAndScoreTables = e.get(tag);
            String outputPath = outputDir+File.separator+tag+".json";
            PatternScoreTable patternScoreTable = new PatternScoreTable();
            for(AbstractScoreTable.ObjectWithScore<LearnitPattern, PatternScore> patternScoreTableEntry: targetAndScoreTables.getPatternScores().getObjectsWithScores()){
                if(patternScoreTableEntry.getScore().isFrozen()){
                    LearnitPattern srcLearnitPattern = patternScoreTableEntry.getObject();
                    if(!(srcLearnitPattern instanceof SerifPattern)){
                        Set<LearnitPattern> similarPatterns = findSimilarPatterns(srcLearnitPattern,mappings);
                        if(similarPatterns.size()>0){
                            LearnitPattern dstLearnitPattern = similarPatterns.iterator().next();
                            addPatternToTable(dstLearnitPattern,patternScoreTable,patternScoreTableEntry.getScore().isGood());
                            System.out.println("[AA] "+ tag +" Mapping " + srcLearnitPattern.toPrettyString().replace("\n"," ").replace("\t"," ") +" to "+dstLearnitPattern.toPrettyString());
                        }
                        else{
                            addPatternToTable(srcLearnitPattern,patternScoreTable,patternScoreTableEntry.getScore().isGood());
                            System.out.println("[BB] " + tag +" Cannot handle "+srcLearnitPattern.toPrettyString().replace("\n"," ").replace("\t"," "));
                            int cnt = 0;
                            for(InstanceIdentifier instanceIdentifier:mappings.getInstancesForPattern(srcLearnitPattern)){
                                System.out.println("[CC]Examples for "+srcLearnitPattern.toPrettyString().replace("\n"," ").replace("\t"," ")+" "+instanceIdentifier.toShortString());
                                cnt++;
                                if(cnt >=10)break;
                            }
                        }
                    }
                    else{
                        addPatternToTable(srcLearnitPattern,patternScoreTable,patternScoreTableEntry.getScore().isGood());
                    }
                }
            }
            TargetAndScoreTables newTargetAndScoreTables = targetAndScoreTables.copyWithPatternScoreTable(patternScoreTable);
            newTargetAndScoreTables.serialize(new File(outputPath));
        }
    }
}
