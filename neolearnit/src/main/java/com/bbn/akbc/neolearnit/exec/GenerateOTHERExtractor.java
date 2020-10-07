package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.common.targets.TargetSlot;
import com.bbn.akbc.neolearnit.common.targets.constraints.impl.*;
import com.bbn.akbc.neolearnit.mappings.filters.FrequencyLimitFilter;
import com.bbn.akbc.neolearnit.mappings.filters.MappingsFilter;
import com.bbn.akbc.neolearnit.mappings.filters.PatternConstraintsFilter;
import com.bbn.akbc.neolearnit.mappings.filters.TargetFilter;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.scoring.scores.PatternScore;
import com.bbn.akbc.neolearnit.scoring.scores.SeedScore;
import com.bbn.akbc.neolearnit.scoring.tables.AbstractScoreTable;
import com.bbn.akbc.neolearnit.util.GeneralUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class GenerateOTHERExtractor{
    public static void main(String[] args) throws IOException {
        String paramsFile = args[0];
        LearnItConfig.loadParams(new File(paramsFile));
        String mappingsPath = args[1];
        String outputExtractorPath = args[2];
        Mappings mappings = Mappings.deserialize(new File(mappingsPath), true);


        Set<InstanceIdentifier.SpanningType> possibleSlot0SpanningType = new HashSet<>();
        Set<InstanceIdentifier.SpanningType> possibleSlot1SpanningType = new HashSet<>();
        ArrayList<String> slot0List = Lists.newArrayList("all");
        ArrayList<String> slot1List = Lists.newArrayList("all");
//        if (slot0SpanningType.contains("Empty")) {
//            possibleSlot0SpanningType.add(InstanceIdentifier.SpanningType.Empty);
//        } else {
//            String[] spanningTypes = slot0SpanningType.split(",");
//            for (String spanningType : spanningTypes) {
//                possibleSlot0SpanningType.add(InstanceIdentifier.SpanningType.valueOf(spanningType));
//            }
//        }
//
//        if (slot1SpanningType.contains("Empty")) {
//            possibleSlot1SpanningType.add(InstanceIdentifier.SpanningType.Empty);
//        } else {
//            String[] spanningTypes = slot1SpanningType.split(",");
//            for (String spanningType : spanningTypes) {
//                possibleSlot1SpanningType.add(InstanceIdentifier.SpanningType.valueOf(spanningType));
//            }
//        }
        Target newTarget;
        String name = "OTHER";
        if (possibleSlot0SpanningType.contains(InstanceIdentifier.SpanningType.EventMention)) {
            if (possibleSlot1SpanningType.contains(InstanceIdentifier.SpanningType.EventMention)) {
                newTarget = new Target.Builder(name)
                        .withTargetSlot(new TargetSlot.Builder(0, "all")
                                .build()).withTargetSlot(new TargetSlot.Builder(1, "all")
                                .build())
                        .withAddedConstraint(new SpanningTypeConstraint(0, Arrays.asList(InstanceIdentifier.SpanningType.EventMention)))
                        .withAddedConstraint(new SpanningTypeConstraint(1, Arrays.asList(InstanceIdentifier.SpanningType.EventMention)))
                        .build();

            } else if (possibleSlot1SpanningType.contains(InstanceIdentifier.SpanningType.Mention) || possibleSlot1SpanningType.contains(InstanceIdentifier.SpanningType.ValueMention)) {
                List<String> allTypes = ImmutableList.of("PER", "ORG", "GPE", "LOC", "FAC", "WEA", "VEH", "PRN", "TPN");
                newTarget = new Target.Builder(name)
                        .withTargetSlot(new TargetSlot.Builder(0, "all")
                                .build()).withTargetSlot(new TargetSlot.Builder(1, "all").build())
                        .withAddedConstraint(new SpanningTypeConstraint(0, Arrays.asList(InstanceIdentifier.SpanningType.EventMention)))
                        .withAddedConstraint(new AtomicMentionConstraint(1))
                        .withAddedConstraint(new EntityTypeOrValueConstraint(1, slot1List))
                        .withAddedConstraint(new MinEntityLevelConstraint("All", 1, "DESC"))
                        .build();
            } else if (possibleSlot1SpanningType.contains(InstanceIdentifier.SpanningType.Empty)) {
                newTarget = new Target.Builder(name).
                        withTargetSlot(new TargetSlot.Builder(0, "all").build()).
                        withTargetSlot(new TargetSlot.Builder(1, "all").build())
                        .withAddedConstraint(new SpanningTypeConstraint(0, Arrays.asList(InstanceIdentifier.SpanningType.EventMention)))
                        .withAddedConstraint(new SpanningTypeConstraint(1, Arrays.asList(InstanceIdentifier.SpanningType.Empty))).build();
            } else {
                throw new com.bbn.bue.common.exceptions.NotImplementedException();
            }
        } else if (possibleSlot0SpanningType.contains(InstanceIdentifier.SpanningType.Mention)) {
            if (possibleSlot1SpanningType.contains(InstanceIdentifier.SpanningType.Empty)) {
                newTarget = new Target.Builder(name)
                        .withTargetSlot(new TargetSlot.Builder(0, "all")
                                .build()).withTargetSlot(new TargetSlot.Builder(1, "all")
                                .build())
                        .withAddedConstraint(new AtomicMentionConstraint(0))
                        .withAddedConstraint(new EntityTypeOrValueOrEventConstraint(0, slot0List))
                        .withAddedConstraint(new MinEntityLevelConstraint("ALL", 0, "DESC"))
                        .withAddedConstraint(new SpanningTypeConstraint(1, Arrays.asList(InstanceIdentifier.SpanningType.Empty)))
                        .build();
            } else if (possibleSlot1SpanningType.contains(InstanceIdentifier.SpanningType.Mention)) {
                newTarget = new Target.Builder(name)
                        .withTargetSlot(new TargetSlot.Builder(0, "all")
                                .build()).withTargetSlot(new TargetSlot.Builder(1, "all")
                                .build())
                        .withAddedConstraint(new AtomicMentionConstraint(0))
                        .withAddedConstraint(new AtomicMentionConstraint(1))
                        .withAddedConstraint(new EntityTypeOrValueOrEventConstraint(0, slot0List))
                        .withAddedConstraint(new EntityTypeOrValueOrEventConstraint(1, slot1List))
                        .withAddedConstraint(new MinEntityLevelConstraint("ALL", 0, "DESC"))
                        .withAddedConstraint(new MinEntityLevelConstraint("ALL", 1, "DESC"))
                        .build();
            } else {
                throw new com.bbn.bue.common.exceptions.NotImplementedException();
            }
        } else {
            throw new com.bbn.bue.common.exceptions.NotImplementedException();
        }

        newTarget.serialize(String.format("%s/%s", LearnItConfig.get("learnit_root"), String.format("inputs/targets/json/%s.json", "OTHER")));

        // This is a must because downstream also filter mappings in the same way.
        TargetFilter targetFilter = new TargetFilter(newTarget);
        mappings = targetFilter.makeFiltered(mappings);


        // Maybe filtering mappings here?
        List<MappingsFilter> filterQueueInObject = new ArrayList<>();
        filterQueueInObject.add(new FrequencyLimitFilter(2, 99999999, 2, 99999999));
//        filterQueueInObject.add(new LabelPatternFilter(mappings));
        PatternConstraintsFilter patternConstraintsFilter = new PatternConstraintsFilter(new HashSet<>(), new HashSet<>(), true);
        patternConstraintsFilter.set_prop_pattern_max_depth(4);
        filterQueueInObject.add(patternConstraintsFilter);
        for (MappingsFilter mappingsFilter : filterQueueInObject) {
            mappings = mappingsFilter.makeFiltered(mappings);
        }




        Map<String, TargetAndScoreTables> latestExtractors = GeneralUtils.loadLatestExtractors();
        Set<LearnitPattern> existingPatternInOtherExtractor = new HashSet<>();//This Other has different meaning than the rest;
        Set<Seed> existingSeedInOtherExtractor = new HashSet<>(); //This Other has different meaning than the rest;
        for(Map.Entry<String, TargetAndScoreTables> extractorEntry : latestExtractors.entrySet()){
            String label = extractorEntry.getKey();
            if(label.toLowerCase().compareTo("OTHER".toLowerCase()) == 0)continue;
            TargetAndScoreTables currentScoreTable = extractorEntry.getValue();
            for (AbstractScoreTable.ObjectWithScore<LearnitPattern, PatternScore> pattern : currentScoreTable.getPatternScores().getObjectsWithScores()) {
                if (pattern.getScore().isFrozen() && pattern.getScore().isGood()) {
                    existingPatternInOtherExtractor.add(pattern.getObject());
                }
            }
            for(AbstractScoreTable.ObjectWithScore<Seed,SeedScore> seed:currentScoreTable.getSeedScores().getObjectsWithScores()){
                if (seed.getScore().isFrozen() && seed.getScore().isGood()) {
                    existingSeedInOtherExtractor.add(seed.getObject());
                }
            }
        }
        throw new NotImplementedException();
        // Need to correct paths
//        Date date = new Date();
////        String timeString = (new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(date)).replace("-", "");
//        String timeString = "2018101973234";
//
//        TargetAndScoreTables newScoreTable = new TargetAndScoreTables(String.format("%s/%s",LearnItConfig.get("learnit_root"),String.format("inputs/targets/json/%s.json","OTHER")));
//
//
//        for(LearnitPattern learnitPattern: mappings.getAllPatterns().elementSet()){
//            if(!existingPatternInOtherExtractor.contains(learnitPattern)){
//                newScoreTable.getPatternScores().addDefault(learnitPattern);
//                PatternScore score = newScoreTable.getPatternScores().getScore(learnitPattern);
////                score.setPrecision((double)mappings.getInstancesForPattern(learnitPattern).size());
//                score.setPrecision(0.5);
//                score.setFrequency(mappings.getInstancesForPattern(learnitPattern).size());
//                score.setConfidence(1.0);
//            }
//        }
//
//        for(Seed seed : mappings.getAllSeeds().elementSet()){
//            if(!existingSeedInOtherExtractor.contains(seed)){
//                newScoreTable.getSeedScores().addDefault(seed);
//                SeedScore score = newScoreTable.getSeedScores().getScore(seed);
//                score.setFrequency(mappings.getInstancesForSeed(seed).size());
//                score.setConfidence(1.0);
//            }
//        }
//
//        System.out.println();
//
//        String strTargetPathDir = String.format("%s/%s/",outputExtractorPath, newScoreTable.getTarget().getName());
//        File dir = new File(strTargetPathDir);
//        if(!dir.exists()) {
//            dir.mkdir();
//        }
//        String strPathJson = strTargetPathDir + newScoreTable.getTarget().getName() + "_" +
//                timeString + ".json";
//        System.out.println("\t serializing extractor for "+newScoreTable+"...");
//        newScoreTable.serialize(new File(strPathJson));
//        System.out.println("\t\t...done.");
    }
}
