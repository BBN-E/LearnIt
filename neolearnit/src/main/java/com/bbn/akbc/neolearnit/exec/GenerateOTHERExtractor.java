package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.common.targets.TargetSlot;
import com.bbn.akbc.neolearnit.common.targets.constraints.impl.EmptySlotConstraint;
import com.bbn.akbc.neolearnit.common.targets.constraints.impl.EventMentionOnlyConstraint;
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
import org.apache.commons.lang3.NotImplementedException;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class GenerateOTHERExtractor{
    public static void main(String args[]) throws IOException {
        String paramsFile = args[0];
        LearnItConfig.loadParams(new File(paramsFile));
        String mappingsPath = args[1];
        String outputExtractorPath = args[2];
        Mappings mappings = Mappings.deserialize(new File(mappingsPath), true);


        boolean isEvent = true;
        boolean isUnary = false;
        Target.Builder newTargetBuilder;
        if (isEvent) {
            newTargetBuilder = new Target.Builder("OTHER")
                    .setDescription("")
                    .withTargetSlot(new TargetSlot.Builder(0, "all")
                            .build()).withTargetSlot(new TargetSlot.Builder(1, "all")
                            .build());
            newTargetBuilder.withAddedConstraint(new EventMentionOnlyConstraint(0, false));
            if (!isUnary) newTargetBuilder.withAddedConstraint(new EventMentionOnlyConstraint(1, false));
            else newTargetBuilder.withAddedConstraint(new EmptySlotConstraint(1));
        } else {
            throw new NotImplementedException("");
        }

        Target newTarget = newTargetBuilder.build();
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

        Date date = new Date();
//        String timeString = (new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(date)).replace("-", "");
        String timeString = "2018101973234";

        TargetAndScoreTables newScoreTable = new TargetAndScoreTables(String.format("%s/%s",LearnItConfig.get("learnit_root"),String.format("inputs/targets/json/%s.json","OTHER")));


        for(LearnitPattern learnitPattern: mappings.getAllPatterns().elementSet()){
            if(!existingPatternInOtherExtractor.contains(learnitPattern)){
                newScoreTable.getPatternScores().addDefault(learnitPattern);
                PatternScore score = newScoreTable.getPatternScores().getScore(learnitPattern);
//                score.setPrecision((double)mappings.getInstancesForPattern(learnitPattern).size());
                score.setPrecision(0.5);
                score.setFrequency(mappings.getInstancesForPattern(learnitPattern).size());
                score.setConfidence(1.0);
            }
        }

        for(Seed seed : mappings.getAllSeeds().elementSet()){
            if(!existingSeedInOtherExtractor.contains(seed)){
                newScoreTable.getSeedScores().addDefault(seed);
                SeedScore score = newScoreTable.getSeedScores().getScore(seed);
                score.setFrequency(mappings.getInstancesForSeed(seed).size());
                score.setConfidence(1.0);
            }
        }

        System.out.println();

        String strTargetPathDir = String.format("%s/%s/",outputExtractorPath, newScoreTable.getTarget().getName());
        File dir = new File(strTargetPathDir);
        if(!dir.exists()) {
            dir.mkdir();
        }
        String strPathJson = strTargetPathDir + newScoreTable.getTarget().getName() + "_" +
                timeString + ".json";
        System.out.println("\t serializing extractor for "+newScoreTable+"...");
        newScoreTable.serialize(new File(strPathJson));
        System.out.println("\t\t...done.");
    }
}
