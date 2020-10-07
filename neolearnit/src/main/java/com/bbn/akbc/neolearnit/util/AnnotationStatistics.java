package com.bbn.akbc.neolearnit.util;

import com.bbn.akbc.neolearnit.common.Annotation;
import com.bbn.akbc.neolearnit.common.BBNInternalOntology;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.scoring.scores.PatternScore;
import com.bbn.akbc.neolearnit.scoring.tables.AbstractScoreTable;
import com.bbn.akbc.neolearnit.storage.impl.HashMapStorage;
import com.google.common.collect.Multimap;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AnnotationStatistics {
    public static void main(String[] args) throws Exception{

        Map<String, Set<LearnitPattern>> extractorToGoodPatternCount = new HashMap<>();
        Map<String,Set<InstanceIdentifier>> extractorToPotentialGoodInstanceCount = new HashMap<>();
        Map<String,Set<InstanceIdentifier>> UIGoodInstanceCount = new HashMap<>();

        String paramPath = args[0];
        LearnItConfig.loadParams(new File(paramPath));
        String yamlPath = args[1];
        String mappingsPath = args[2];
        Mappings mappings=null;
        if(new File(mappingsPath).exists()){
            mappings = Mappings.deserialize(new File(mappingsPath),true);
        }
        else{
            System.out.println("Mappings "+mappingsPath+" not found!");
        }

        final File ontologyFile = new File(yamlPath);
        BBNInternalOntology.BBNInternalOntologyNode root =  BBNInternalOntology.BBNInternalOntologyNode.fromInternalOntologyFile(ontologyFile);
        Multimap<String, BBNInternalOntology.BBNInternalOntologyNode> childrenNodeMap = root.getPropToNodeMultiMap(new BBNInternalOntology.BBNInternalOntologyNode.PropStringGetter("originalKey"));
        String targetPathDir = String.format("%s/inputs/extractors/", LearnItConfig.get("learnit_root"));
        for(String targetName:childrenNodeMap.keySet()){
            if(new File(targetPathDir+File.separator+targetName).exists()){
                String latestFileTimestamp = GeneralUtils.getLatestExtractor(targetName, new File(targetPathDir+File.separator+targetName)).orNull();
                String fileName = String.format("%s/%s_%s.json", targetPathDir+File.separator+targetName,
                        targetName, latestFileTimestamp);
                TargetAndScoreTables targetAndScoreTables = TargetAndScoreTables.deserialize(new File(fileName));

                Set<LearnitPattern> goodPatternBuffer = extractorToGoodPatternCount.getOrDefault(targetName,new HashSet<>());
                Set<InstanceIdentifier> goodInstancesBuffer = extractorToPotentialGoodInstanceCount.getOrDefault(targetName,new HashSet<>());
                for (AbstractScoreTable.ObjectWithScore<LearnitPattern, PatternScore> pattern : targetAndScoreTables.getPatternScores().getObjectsWithScores()) {
                    if (pattern.getScore().isFrozen() && pattern.getScore().isGood()) {
                        goodPatternBuffer.add(pattern.getObject());
                        if(mappings != null){
                            goodInstancesBuffer.addAll(mappings.getInstancesForPattern(pattern.getObject()));
                        }
                    }
                }
                extractorToGoodPatternCount.put(targetName,goodPatternBuffer);
                extractorToPotentialGoodInstanceCount.put(targetName,goodInstancesBuffer);
            }
        }

        String labeledMappingsPath = String.format("%s.sjson", LearnItConfig.get("human_label_mappings_prefix"));
        if(new File(labeledMappingsPath).exists()){
            Annotation.InMemoryAnnotationStorage inMemoryAnnotationStorage = new Annotation.InMemoryAnnotationStorage(Mappings.deserialize(new File(labeledMappingsPath),true));
            for(InstanceIdentifier instanceIdentifier: inMemoryAnnotationStorage.getAllInstanceIdentifier()){
                for(LabelPattern labelPattern: inMemoryAnnotationStorage.lookupInstanceIdentifierAnnotation(instanceIdentifier)){
                    if(labelPattern.getFrozenState().equals(Annotation.FrozenState.FROZEN_GOOD)){
                        Set<InstanceIdentifier> buf = UIGoodInstanceCount.getOrDefault(labelPattern.getLabel(),new HashSet<>());
                        buf.add(instanceIdentifier);
                        UIGoodInstanceCount.put(labelPattern.getLabel(),buf);
                    }
                }
            }
        }
        else{
            System.out.println("UI instance annotation "+labeledMappingsPath+" not found!");
        }
        System.out.println("Report for yaml file "+yamlPath+", under corpus "+LearnItConfig.get("corpus_name"));
        for(String targetName:extractorToGoodPatternCount.keySet()){
            System.out.println("Under target "+targetName);
            System.out.println("We have "+extractorToGoodPatternCount.getOrDefault(targetName,new HashSet<>()).size() + " good patterns.");
            System.out.println("We will have  "+extractorToPotentialGoodInstanceCount.getOrDefault(targetName,new HashSet<>()).size() + " good instances when directly using extractor to label instances.");
            System.out.println("We have  "+UIGoodInstanceCount.getOrDefault(targetName,new HashSet<>()).size() + " UI marked instances.");
        }

    }
}
