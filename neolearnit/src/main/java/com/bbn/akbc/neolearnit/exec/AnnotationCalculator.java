package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.neolearnit.common.Annotation;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AnnotationCalculator {

    //    public static Map<String,Integer> RecallFNMultiplier (TargetAndScoreTables targetAndScoreTables,Mappings mappings,int cutOff,String instanceIdentifierAnnotationDir) throws IOException{
//        List<AbstractScoreTable.ObjectWithScore<LearnitPattern, PatternScore>> patternScoreTable = targetAndScoreTables.getPatternScores().getObjectsWithScores();
//        int patternScoreTableSize = patternScoreTable.size();
//        int touchedPart = patternScoreTableSize - cutOff;
//        System.out.println("We annotated "+ touchedPart + " patterns.");
//        Map<String,Integer> countMap = new HashMap<>();
//        int seenInstances = 0;
//        int totalInstances = 0;
//        for(int i = 0;i < patternScoreTableSize;++i){
//            LearnitPattern learnitPattern = patternScoreTable.get(i).getObject();
//            Set<InstanceIdentifier> insts = new HashSet<>(mappings.getInstancesForPattern(learnitPattern));
//            insts = InstanceIdentifierFilterForAnnotation.makeFiltered(insts);
//            if(i < touchedPart){
//                for(InstanceIdentifier inst:insts){
//                    String relationType = OnDemandReHandler.LookupInstanceIdentifierAnnotationFromOther(inst,instanceIdentifierAnnotationDir);
//                    countMap.put(relationType,countMap.getOrDefault(relationType,0) + 1);
//                    totalInstances++;
//                    seenInstances++;
//                }
//            }
//            else{
//                boolean shouldCountTotal = false;
//                int localTotalInstanceCounter = 0;
//                for(InstanceIdentifier inst:insts){
//                    String relationType = OnDemandReHandler.LookupInstanceIdentifierAnnotationFromOther(inst,instanceIdentifierAnnotationDir);
//                    if(relationType.toLowerCase().compareTo("NO_RELATION".toLowerCase()) != 0 && relationType.toLowerCase().compareTo("OTHER".toLowerCase()) != 0){
//                        shouldCountTotal = true;
//                    }
//                    countMap.put(relationType,countMap.getOrDefault(relationType,0) + 1);
//                    localTotalInstanceCounter++;
//                    totalInstances++;
//                }
//                if(shouldCountTotal)seenInstances+=localTotalInstanceCounter;
//            }
//        }
//        double multiplier = (double)totalInstances / seenInstances;
//        System.out.println("Our seen sentences is: "+seenInstances);
//        System.out.println("Our multiplier is: "+multiplier);
//        for(String relationType : countMap.keySet()){
//            countMap.put(relationType,countMap.get(relationType) * (int)multiplier);
//        }
//        return countMap;
//    }
    public static void main(String args[]) throws Exception {
        String paramsFile = args[0];
        LearnItConfig.loadParams(new File(paramsFile));
//        String mappingsPath = args[1];
//        Mappings mappings = Mappings.deserialize(new File(mappingsPath), true);
//        Map<String, TargetAndScoreTables> latestExtractors = GeneralUtils.loadLatestExtractors();
        final String instanceIdentifierAnnotationFilePathNormal = String.format("%s/inputs/relation_annotation_by_learnit_ui/%s.sjson", LearnItConfig.get("learnit_root"), LearnItConfig.get("corpus_name"));
        final String instanceIdentifierAnnotationFilePathOther = String.format("%s/inputs/relation_annotation_by_learnit_ui/%s_other.sjson", LearnItConfig.get("learnit_root"), LearnItConfig.get("corpus_name"));
        final Annotation.InMemoryAnnotationStorage inMemoryAnnotationStorageNormal = new Annotation.InMemoryAnnotationStorage(Mappings.deserialize(new File(instanceIdentifierAnnotationFilePathNormal), true));
        final Annotation.InMemoryAnnotationStorage inMemoryAnnotationStorageOther = new Annotation.InMemoryAnnotationStorage(Mappings.deserialize(new File(instanceIdentifierAnnotationFilePathOther), true));
        Map<String, Map<InstanceIdentifier, Annotation.FrozenState>> staticMap = new HashMap<>();
        Map<String, Map<String, Integer>> resultMap = new HashMap<>();
        Set<InstanceIdentifier> existedInstance = new HashSet<>(inMemoryAnnotationStorageNormal.getAllInstanceIdentifier());
        for (InstanceIdentifier instanceIdentifier : inMemoryAnnotationStorageNormal.getAllInstanceIdentifier()) {
            for (LabelPattern labelPattern : inMemoryAnnotationStorageNormal.lookupInstanceIdentifierAnnotation(instanceIdentifier)) {
                Annotation.FrozenState frozenState = labelPattern.getFrozenState();
                String relationType = labelPattern.getLabel();
                if (relationType.toLowerCase().equals("other")) continue;
                if (frozenState.equals(Annotation.FrozenState.NO_FROZEN)) continue;
                Map<InstanceIdentifier, Annotation.FrozenState> annotationEntry = staticMap.getOrDefault(relationType, new HashMap<>());
                annotationEntry.put(instanceIdentifier, frozenState);
                staticMap.put(relationType, annotationEntry);
            }
        }
        for (String relationType : staticMap.keySet()) {
            int numberOfObservations = 0;
            int numberOfMarkGood = 0;
            for (InstanceIdentifier instanceIdentifier : staticMap.get(relationType).keySet()) {
                Annotation.FrozenState frozenState = staticMap.get(relationType).get(instanceIdentifier);
                if (frozenState == Annotation.FrozenState.FROZEN_GOOD) {
                    numberOfObservations++;
                    numberOfMarkGood++;
                } else {
                    assert frozenState == Annotation.FrozenState.FROZEN_BAD;
                    numberOfObservations++;
                }
            }
            Map<String, Integer> recordEntry = resultMap.getOrDefault(relationType, new HashMap<>());
            recordEntry.put("TP", numberOfMarkGood);
            recordEntry.put("FP", numberOfObservations - numberOfMarkGood);
            resultMap.put(relationType, recordEntry);
            System.out.println("Precision for relationType " + relationType + " is: " + (double) numberOfMarkGood / numberOfObservations);
        }
        // Handle Other
        Map<String, Set<InstanceIdentifier>> staticMap2 = new HashMap<>();
        for (InstanceIdentifier instanceIdentifier : inMemoryAnnotationStorageOther.getAllInstanceIdentifier()) {
            for (LabelPattern labelPattern : inMemoryAnnotationStorageOther.lookupInstanceIdentifierAnnotation(instanceIdentifier)) {
                String relationType = labelPattern.getLabel();
                if (relationType.toLowerCase().equals("other")) continue;
                if (existedInstance.contains(instanceIdentifier)) {
                    boolean shouldCountItAsCaptured = false;
                    for (LabelPattern labelPattern1 : inMemoryAnnotationStorageNormal.lookupInstanceIdentifierAnnotation(instanceIdentifier)) {
                        if (labelPattern1.getLabel().toLowerCase().equals(relationType.toLowerCase())) {
                            shouldCountItAsCaptured = true;
                        }
                    }
                    if (shouldCountItAsCaptured) continue;
                }
                Set<InstanceIdentifier> currentSet = staticMap2.getOrDefault(relationType, new HashSet<>());
                currentSet.add(instanceIdentifier);
                staticMap2.put(relationType, currentSet);
            }
        }
        for (String relationType : staticMap2.keySet()) {
            Map<String, Integer> recordEntry = resultMap.getOrDefault(relationType, new HashMap<>());
            recordEntry.put("FN", staticMap2.getOrDefault(relationType, new HashSet<>()).size());
            resultMap.put(relationType, recordEntry);
            System.out.println("Recall for relationType " + relationType + " is: " + (double) recordEntry.getOrDefault("TP", 0) / (recordEntry.getOrDefault("TP", 0) + staticMap2.getOrDefault(relationType, new HashSet<>()).size()));
        }
        System.out.println("relationType\tprecision\trecall\tF1");
        for (String relationType : resultMap.keySet()) {
            Map<String, Integer> recordEntry = resultMap.getOrDefault(relationType, new HashMap<>());
            Double precision = (double) recordEntry.getOrDefault("TP", 0) / (recordEntry.getOrDefault("TP", 0) + recordEntry.getOrDefault("FP", 0));
            Double recall = (double) recordEntry.getOrDefault("TP", 0) / (recordEntry.getOrDefault("TP", 0) + recordEntry.getOrDefault("FN", 0));
            Double F1 = 2 * precision * recall / (precision + recall);
            System.out.println(relationType + "\t" + precision + "\t" + recall + "\t" + F1);
        }
//        Map<String,Integer> FNAdditionMap = RecallFNMultiplier(latestExtractors.get("OTHER"),mappings,1412,instanceIdentifierAnnotationDir);
//        System.out.println("relationType\tprecision\trecall\tF1");
//        for(String relationType: resultMap.keySet()){
//            Map<String,Integer> recordEntry = resultMap.getOrDefault(relationType,new HashMap<>());
//            Double precision = (double)recordEntry.get("TP")/(recordEntry.get("TP") + recordEntry.get("FP"));
//            Double recall = (double)recordEntry.get("TP")/(recordEntry.get("TP") + recordEntry.getOrDefault("FN",0) + FNAdditionMap.getOrDefault(relationType,0));
//            Double F1 = 2 * precision * recall / (precision + recall);
//            System.out.println(relationType+"\t"+precision+"\t"+recall+"\t"+F1);
//        }
        for (String relationType : resultMap.keySet()) {
            Map<String, Integer> recordEntry = resultMap.getOrDefault(relationType, new HashMap<>());
            System.out.println("Under relationType " + relationType + " , we got " + recordEntry.getOrDefault("TP", 0) + " positives, " + recordEntry.getOrDefault("FP", 0) + " negative");
            System.out.println("Under relationType " + relationType + " ,we got " + recordEntry.getOrDefault("FN", 0) + " undetected samples.");
        }
    }
}