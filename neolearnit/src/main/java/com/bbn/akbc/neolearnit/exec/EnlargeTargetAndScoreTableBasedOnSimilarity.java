package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.common.Pair;
import com.bbn.akbc.neolearnit.common.Annotation;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.MinimumInstanceIdentifier;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.LearnItObservation;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.observations.similarity.ObservationSimilarity;
import com.bbn.akbc.neolearnit.observations.similarity.PatternID;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.scoring.scores.PatternScore;
import com.bbn.akbc.neolearnit.scoring.tables.AbstractScoreTable;
import com.bbn.akbc.neolearnit.similarity.ObservationSimilarityModule;
import com.bbn.akbc.neolearnit.util.GeneralUtils;
import com.google.common.base.Optional;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.*;
import java.util.stream.Collectors;

public class EnlargeTargetAndScoreTableBasedOnSimilarity {
    // @hqiu: Should rename it as Scorer or something

    public static Map<String, RecallMultiplierInfo> generateRecallCaluculatorObjBasedOnOTHER(TargetAndScoreTables OTHERTable, int freqCutoff, Mappings autoPopulatedMappings, Annotation.InMemoryAnnotationStorage annotationStorageOTHER) {
        Set<MinimumInstanceIdentifier> unSeenInstances = new HashSet<>();
        Set<MinimumInstanceIdentifier> seenInstances = new HashSet<>();
        for (AbstractScoreTable.ObjectWithScore<LearnitPattern, PatternScore> pattern : OTHERTable.getPatternScores().getObjectsWithScores()) {
            final int freq = pattern.getScore().getFrequency();
            for (InstanceIdentifier instanceIdentifier : autoPopulatedMappings.getInstancesForPattern(pattern.getObject())) {
                if (freq >= freqCutoff) {
                    seenInstances.add(new MinimumInstanceIdentifier(instanceIdentifier));
                } else {
                    unSeenInstances.add(new MinimumInstanceIdentifier(instanceIdentifier));
                }
            }
        }
        Map<String, Set<InstanceIdentifier>> undetectedInstancePerType = new HashMap<>();
        for (InstanceIdentifier instanceIdentifier : annotationStorageOTHER.getAllInstanceIdentifier()) {
            for (LabelPattern labelPattern : annotationStorageOTHER.lookupInstanceIdentifierAnnotation(instanceIdentifier)) {
                if (labelPattern.getLabel().toLowerCase().equals("other")) continue;
                final String label = labelPattern.getLabel();
                Set<InstanceIdentifier> undetectedInstanceSet = undetectedInstancePerType.getOrDefault(label, new HashSet<>());
                undetectedInstanceSet.add(instanceIdentifier);
                undetectedInstancePerType.put(label, undetectedInstanceSet);
            }
        }
        Map<String, RecallMultiplierInfo> ret = new HashMap<>();
        final double multiplier = (double) (seenInstances.size() + unSeenInstances.size()) / seenInstances.size();
        for (String targetName : undetectedInstancePerType.keySet()) {
            Set<InstanceIdentifier> undetectedInstanceSet = undetectedInstancePerType.get(targetName);
            ret.put(targetName, new RecallMultiplierInfo(undetectedInstanceSet, multiplier, (int) Math.ceil(undetectedInstanceSet.size() * multiplier)));
        }
        return ret;
    }


    private static LearnitPattern findPattern(Mappings info, String pattern) {
        for (LearnitPattern p : info.getAllPatterns().elementSet()) {
            if (p.toIDString().equals(pattern)) {
                return p;
            }
        }
        return null;
    }

    public static Map<String, Double> scoringNormal(Set<InstanceIdentifier> positiveByLearnIt, Map<InstanceIdentifier, Annotation.FrozenState> humanLabelMerged) {
        Map<String, Double> ret = new HashMap<>();
        int counterCorrect = 0;
        int counterWrong = 0;

        for (InstanceIdentifier instanceIdentifier : positiveByLearnIt) {
            if (!humanLabelMerged.containsKey(instanceIdentifier)) {
//                counterWrong++;
                continue;
            }

            Annotation.FrozenState humanAnnotationState = humanLabelMerged.get(instanceIdentifier);
            if (humanAnnotationState.equals(Annotation.FrozenState.FROZEN_BAD)) counterWrong++;
            if (humanAnnotationState.equals(Annotation.FrozenState.FROZEN_GOOD)) counterCorrect++;

        }

        int undetected = 0;
        for (InstanceIdentifier instanceIdentifier : humanLabelMerged.keySet()) {
            Annotation.FrozenState humanAnnotationState = humanLabelMerged.get(instanceIdentifier);
            if (humanAnnotationState.equals(Annotation.FrozenState.FROZEN_GOOD)) {
                if (!positiveByLearnIt.contains(instanceIdentifier)) undetected++;
            }
        }

        double precision = (double) counterCorrect / (counterCorrect + counterWrong);
        double recall = (double) counterCorrect / (counterCorrect + undetected);


        ret.put("precision", precision);
        ret.put("recall", recall);
        ret.put("F1", (double) 2 * precision * recall / (precision + recall));
        return ret;
    }

    public static Set<LearnitPattern> similarPatterns(ObservationSimilarityModule observationSimilarityModule, Mappings mappings, Set<LearnitPattern> currentPatternSet, Set<LearnitPattern> shouldMaskSet, double threshold, int SIZE_LIMIT) throws Exception {
        List<Optional<? extends ObservationSimilarity>> patternSimilarityRows = new ArrayList<>();

        for (LearnitPattern learnitPattern : currentPatternSet) {
            Optional<? extends ObservationSimilarity> learnitPatternSimilarity = observationSimilarityModule.getPatternSimilarity(PatternID.from(learnitPattern));
            patternSimilarityRows.add(learnitPatternSimilarity);
        }

        List<com.bbn.akbc.utility.Pair<LearnItObservation, Double>> similarLearnitPatterns =
                ObservationSimilarity.mergeMultipleSimilarities(patternSimilarityRows,
                        threshold,
                        Optional.of(SIZE_LIMIT),
                        Optional.of(shouldMaskSet));
        Set<LearnitPattern> ret = new HashSet<>();
        for (com.bbn.akbc.utility.Pair<LearnItObservation, Double> learnItObservationDoublePair : similarLearnitPatterns) {
            LearnitPattern learnitPattern = findPattern(mappings, learnItObservationDoublePair.key.toIDString());
            if (learnitPattern == null) {
                System.out.println("We should find something. This is a bug.");
                continue;
            }

            ret.add(learnitPattern);
        }
        return ret;
    }

    public static Set<Seed> similarSeeds(ObservationSimilarityModule observationSimilarityModule, Set<Seed> currentSeedSet, double threshold, int SIZE_LIMIT) throws Exception {
        List<Optional<? extends ObservationSimilarity>> seedSimilarityRows = new ArrayList<>();

        for (Seed seed : currentSeedSet) {
            Optional<? extends ObservationSimilarity> seedSimilarity = observationSimilarityModule.getSeedSimilarity(seed);
            seedSimilarityRows.add(seedSimilarity);
        }

        List<com.bbn.akbc.utility.Pair<LearnItObservation, Double>> similarSeeds =
                ObservationSimilarity.mergeMultipleSimilarities(seedSimilarityRows,
                        threshold,
                        Optional.of(SIZE_LIMIT),
                        Optional.of(currentSeedSet));
        Set<Seed> ret = new HashSet<>();
        for (com.bbn.akbc.utility.Pair<LearnItObservation, Double> learnItObservationDoublePair : similarSeeds) {
            ret.add((Seed) learnItObservationDoublePair.key);
        }
        return ret;
    }



    public static Map<String, Double> scoringUsingOther(Set<InstanceIdentifier> positiveByLearnIt, Map<InstanceIdentifier, Annotation.FrozenState> humanLabelFromNormal, RecallMultiplierInfo otherEntry) {
        Map<String, Double> ret = new HashMap<>();
        int counterCorrect = 0;
        int counterWrong = 0;

        Set<InstanceIdentifier> allPositiveInstanceFromOther = otherEntry.undetectedSet;
        final double multiplier = otherEntry.multiplier;

        for (InstanceIdentifier instanceIdentifier : positiveByLearnIt) {
            if (!humanLabelFromNormal.containsKey(instanceIdentifier)) {
                if (allPositiveInstanceFromOther.contains(instanceIdentifier)) counterCorrect++;
                continue;
            }
            Annotation.FrozenState humanAnnotationState = humanLabelFromNormal.get(instanceIdentifier);
            if (humanAnnotationState.equals(Annotation.FrozenState.FROZEN_BAD)) counterWrong++;
            if (humanAnnotationState.equals(Annotation.FrozenState.FROZEN_GOOD)) counterCorrect++;

        }

        int undetectedFromOther = 0;
//        Set<InstanceIdentifier> allPositiveInstanceFromNormal = new HashSet<>();
//        for (InstanceIdentifier instanceIdentifier : humanLabelFromNormal.keySet()) {
//            Annotation.FrozenState humanAnnotationState = humanLabelFromNormal.get(instanceIdentifier);
//            if (humanAnnotationState.equals(Annotation.FrozenState.FROZEN_GOOD)) {
//                allPositiveInstanceFromNormal.add(instanceIdentifier);
//            }
//        }
        for (InstanceIdentifier instanceIdentifier : allPositiveInstanceFromOther) {
            if (!positiveByLearnIt.contains(instanceIdentifier)) {
                undetectedFromOther++;
            }
        }


        double precision = (double) counterCorrect / (counterCorrect + counterWrong);
        double recall = (double) counterCorrect / (counterCorrect + undetectedFromOther * multiplier);


        ret.put("precision", precision);
        ret.put("recall", recall);
        ret.put("F1", (double) 2 * precision * recall / (precision + recall));
        return ret;
    }

    public static void printScoring(String methodName, Set<LearnitPattern> chosenPattern, Set<InstanceIdentifier> positiveByLearnIt, Map<InstanceIdentifier, Annotation.FrozenState> humanLabelMerged, Map<InstanceIdentifier, Annotation.FrozenState> humanLabelFromNormal, RecallMultiplierInfo otherEntry, CSVPrinter csvPrinter, LearnitPattern newestAddedPattern) throws Exception {
        Map<String, Double> fromNormal = scoringNormal(positiveByLearnIt, humanLabelMerged);
        Map<String, Double> fromOther = null;
        if (otherEntry != null) {
            fromOther = scoringUsingOther(positiveByLearnIt, humanLabelFromNormal, otherEntry);
        }

        final double precision = fromNormal.get("precision");
        final double recall = fromNormal.get("recall");
        final double F1 = fromNormal.get("F1");
        final double recallEst = (fromOther == null) ? (2.0 % 0) : fromOther.get("recall");
        final double F1Est = (fromOther == null) ? (2.0 % 0) : fromOther.get("F1");

//        System.out.println("Precision from normal is:" + fromNormal.get("precision"));
//        System.out.println("Recall from normal is:" + fromNormal.get("recall"));
//        System.out.println("F1 from normal is:" + fromNormal.get("F1"));
//        if (otherEntry != null) {
//            System.out.println("Precision using OTHER is:" + fromOther.get("precision"));
//            System.out.println("Recall using OTHER normal is:" + fromOther.get("recall"));
//            System.out.println("F1 using OTHER normal is:" + fromOther.get("F1"));
//        }
        csvPrinter.printRecord(methodName, chosenPattern.size(), precision, recall, F1, recallEst, F1Est, chosenPattern.stream().map(LearnitPattern::toIDString).collect(Collectors.joining(" || ")), newestAddedPattern.toIDString());
    }

    public static void printAnnotatedMap(Annotation.InMemoryAnnotationStorage inMemoryAnnotationStorage) {
        Map<String, Integer> positiveCntMap = new HashMap<>();
        Map<String, Integer> negativeCntMap = new HashMap<>();
        for (InstanceIdentifier instanceIdentifier : inMemoryAnnotationStorage.getAllInstanceIdentifier()) {
            for (LabelPattern labelPattern : inMemoryAnnotationStorage.lookupInstanceIdentifierAnnotation(instanceIdentifier)) {
                if (labelPattern.getFrozenState().equals(Annotation.FrozenState.FROZEN_GOOD)) {
                    positiveCntMap.put(labelPattern.getLabel(), positiveCntMap.getOrDefault(labelPattern.getLabel(), 0) + 1);
                }
                if (labelPattern.getFrozenState().equals(Annotation.FrozenState.FROZEN_BAD)) {
                    negativeCntMap.put(labelPattern.getLabel(), negativeCntMap.getOrDefault(labelPattern.getLabel(), 0) + 1);
                }
            }
        }
        System.out.println("===============");

        for (String targetName : positiveCntMap.keySet()) {
            System.out.println("For type " + targetName + ", we have " + positiveCntMap.get(targetName) + " positive examples.");
        }
        for (String targetName : negativeCntMap.keySet()) {
            System.out.println("For type " + targetName + ", we have " + negativeCntMap.get(targetName) + " negative examples.");
        }
        System.out.println("===============");
    }

    public static void main(String[] args) throws Exception {
        String paramsPath = args[0];
        LearnItConfig.loadParams(new File(paramsPath));
        String mappingsPath = args[1];
        Mappings autopopulatedMappings = Mappings.deserialize(new File(mappingsPath), true);
        String dirSuffixForSimilarityMatrices = args[2];
        ObservationSimilarityModule observationSimilarityModule = ObservationSimilarityModule.create(autopopulatedMappings, dirSuffixForSimilarityMatrices);
        String extractorsInputDirectory = args[3];
        String extractorOther = args[4];
        String outputFolder = args[5];
        new File(outputFolder).mkdirs();
        Map<String, TargetAndScoreTables> latestExtractors = GeneralUtils.loadExtractors(extractorsInputDirectory);
        TargetAndScoreTables otherExtractor = TargetAndScoreTables.deserialize(new File(extractorOther));


        // This section is for Adding annotated instances in
        // There are two set of annotations. Normal and OTHER
        final String instanceIdentifierAnnotationFilePathNormal = String.format("%s/inputs/relation_annotation_by_learnit_ui/%s.sjson", LearnItConfig.get("learnit_root"), LearnItConfig.get("corpus_name"));
        final String instanceIdentifierAnnotationFilePathOther = String.format("%s/inputs/relation_annotation_by_learnit_ui/%s_other.sjson", LearnItConfig.get("learnit_root"), LearnItConfig.get("corpus_name"));
        Annotation.InMemoryAnnotationStorage annotationNormal = new Annotation.InMemoryAnnotationStorage(Mappings.deserialize(new File(instanceIdentifierAnnotationFilePathNormal), true));
        Annotation.InMemoryAnnotationStorage annotationOther = new Annotation.InMemoryAnnotationStorage(Mappings.deserialize(new File(instanceIdentifierAnnotationFilePathOther), true));
        List<Annotation.InMemoryAnnotationStorage> annotationStorageList = new ArrayList<>();
        annotationStorageList.add(annotationNormal);
        annotationStorageList.add(annotationOther);
        Annotation.InMemoryAnnotationStorage mergedAnnotation = Annotation.mergeAnnotation(annotationStorageList);
        Map<String, RecallMultiplierInfo> recallInfoMap = generateRecallCaluculatorObjBasedOnOTHER(otherExtractor, 14, autopopulatedMappings, annotationOther);

        System.out.println("For normal annotation");
        printAnnotatedMap(annotationNormal);
        System.out.println("For other annotation");
        printAnnotatedMap(annotationOther);

        // Here's what you can play with current extractors. So for now, we suppose that we add all positive patterns, and labeled All instances that captured by that pattern as positive.

        Map<String, List<Pair<LearnitPattern, PatternScore>>> positivePatternMap = new HashMap<>();
        Map<String, Map<InstanceIdentifier, Annotation.FrozenState>> normalAnnotationConvertedMap = new HashMap<>();
        Map<String, Map<InstanceIdentifier, Annotation.FrozenState>> mergedAnnotationConvertedMap = new HashMap<>();

        for (String relationType : latestExtractors.keySet()) {
            if (relationType.toLowerCase().equals("other")) continue;
            TargetAndScoreTables targetAndScoreTables = latestExtractors.get(relationType);

            List<Pair<LearnitPattern, PatternScore>> positivePatterns = new ArrayList<>();
            for (AbstractScoreTable.ObjectWithScore<LearnitPattern, PatternScore> pattern : targetAndScoreTables.getPatternScores().getObjectsWithScores()) {
                if (pattern.getScore().isFrozen() && pattern.getScore().isGood()) {
                    pattern.getScore().setFrequency(autopopulatedMappings.getInstancesForPattern(pattern.getObject()).size());
                    if (pattern.getScore().getFrequency() > 0) {
                        positivePatterns.add(new Pair<>(pattern.getObject(), pattern.getScore()));
                    }

                }
            }
            positivePatternMap.put(relationType, positivePatterns);

            Map<InstanceIdentifier, Annotation.FrozenState> mergedAnnotatedInstanceMap = new HashMap<>();
            for (InstanceIdentifier instanceIdentifier : mergedAnnotation.getAllInstanceIdentifier()) {
                for (LabelPattern labelPattern : mergedAnnotation.lookupInstanceIdentifierAnnotation(instanceIdentifier)) {
                    if (labelPattern.getLabel().equals(relationType)) {
                        mergedAnnotatedInstanceMap.put(instanceIdentifier, labelPattern.getFrozenState());
                    }
                }
            }
            mergedAnnotationConvertedMap.put(relationType, mergedAnnotatedInstanceMap);
            Map<InstanceIdentifier, Annotation.FrozenState> normalAnotatedInstanceMap = new HashMap<>();
            for (InstanceIdentifier instanceIdentifier : annotationNormal.getAllInstanceIdentifier()) {
                for (LabelPattern labelPattern : mergedAnnotation.lookupInstanceIdentifierAnnotation(instanceIdentifier)) {
                    if (labelPattern.getLabel().equals(relationType)) {
                        normalAnotatedInstanceMap.put(instanceIdentifier, labelPattern.getFrozenState());
                    }
                }
            }
            normalAnnotationConvertedMap.put(relationType, normalAnotatedInstanceMap);
        }

        // Start downsampling

        Random random = new Random();
        for (String targetName : positivePatternMap.keySet()) {
            List<Pair<LearnitPattern, PatternScore>> positivePatterns = positivePatternMap.get(targetName);
            Map<InstanceIdentifier, Annotation.FrozenState> humanLabeledNormal = normalAnnotationConvertedMap.get(targetName);
            Map<InstanceIdentifier, Annotation.FrozenState> humanLabeledMerged = mergedAnnotationConvertedMap.get(targetName);
            RecallMultiplierInfo recallMultiplierInfo = recallInfoMap.get(targetName);
            CSVPrinter scorePrinter = new CSVPrinter(new BufferedWriter(new FileWriter(new File(outputFolder + File.separator + targetName + ".csv"))), CSVFormat.EXCEL);
            scorePrinter.printRecord("methodName", "numOfPatterns", "precision", "recall", "F1", "recallEst", "F1Est", "chosenPattern", "newestAddedPattern");

            List<Pair<LearnitPattern, PatternScore>> sortedPositivePatterns = new ArrayList<>(positivePatterns);
            List<Pair<LearnitPattern, PatternScore>> shuffledPositivePatterns = new ArrayList<>(positivePatterns);


            Collections.sort(sortedPositivePatterns, new Comparator<Pair<LearnitPattern, PatternScore>>() {
                @Override
                public int compare(Pair<LearnitPattern, PatternScore> o1, Pair<LearnitPattern, PatternScore> o2) {
                    return o2.getSecond().getFrequency() - o1.getSecond().getFrequency();
                }
            });

            Collections.shuffle(shuffledPositivePatterns);


            for (int sizeOfSubset = 1; sizeOfSubset <= positivePatterns.size(); sizeOfSubset += 1) {
                // Method1: random
                Set<LearnitPattern> sampledPatterns = new HashSet<>();
                Iterator<Pair<LearnitPattern, PatternScore>> shuffedIter = shuffledPositivePatterns.iterator();
                LearnitPattern newestAddedPattern = null;
                while (sampledPatterns.size() < sizeOfSubset) {
                    newestAddedPattern = shuffedIter.next().getFirst();
                    sampledPatterns.add(newestAddedPattern);
                }

                Set<InstanceIdentifier> capturedInstances = new HashSet<>();
                for (LearnitPattern learnitPattern : sampledPatterns) {
                    capturedInstances.addAll(autopopulatedMappings.getInstancesForPattern(learnitPattern));
                }

                printScoring("Random", sampledPatterns, capturedInstances, humanLabeledMerged, humanLabeledNormal, recallMultiplierInfo, scorePrinter, newestAddedPattern);

                // Method2: From the most frequent one
                capturedInstances.clear();
                sampledPatterns.clear();
                Iterator<Pair<LearnitPattern, PatternScore>> freqDescIter = sortedPositivePatterns.iterator();
                while (sampledPatterns.size() < sizeOfSubset) {
                    newestAddedPattern = freqDescIter.next().getFirst();
                    sampledPatterns.add(newestAddedPattern);
                }
                for (LearnitPattern learnitPattern : sampledPatterns) {
                    capturedInstances.addAll(autopopulatedMappings.getInstancesForPattern(learnitPattern));
                }
                printScoring("ByFreqDesc", sampledPatterns, capturedInstances, humanLabeledMerged, humanLabeledNormal, recallMultiplierInfo, scorePrinter, newestAddedPattern);
            }
            scorePrinter.close();
        }

    }

    public static class RecallMultiplierInfo {
        final public Set<InstanceIdentifier> undetectedSet;
        final public double multiplier;
        final public int estimatedUndetected;

        public RecallMultiplierInfo(Set<InstanceIdentifier> undetectedSet, double multiplier, int estimatedUndetected) {
            this.undetectedSet = undetectedSet;
            this.multiplier = multiplier;
            this.estimatedUndetected = estimatedUndetected;
        }
    }
}
