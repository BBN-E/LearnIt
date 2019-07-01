package com.bbn.akbc.neolearnit.bootstrapping;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.scoring.scores.PatternScore;
import com.bbn.akbc.neolearnit.scoring.scores.SeedScore;
import com.bbn.akbc.neolearnit.scoring.tables.AbstractScoreTable;
import com.google.common.collect.Multiset;

import java.io.File;
import java.util.*;

public class GeneratePairsFromPatterns {

    public static void main(String[] args) throws Exception{
        String strFileParam = args[0];
        String strFileInputExtractor = args[1];
        String strFileOutputExtractor = args[2];
        String strFileMappings = args[3];
        int iteration = Integer.parseInt(args[4]);

        LearnItConfig.loadParams(new File(strFileParam));
        TargetAndScoreTables targetAndScoreTables = TargetAndScoreTables.deserialize(new File(strFileInputExtractor));

        System.out.println("Load mapping file " + strFileMappings);
        Mappings mappings = Mappings.deserialize(new File(strFileMappings), true);

        Set<LearnitPattern> goodPatternSet = new HashSet<>();
        for(AbstractScoreTable.ObjectWithScore<LearnitPattern, PatternScore> pattern : targetAndScoreTables.getPatternScores().getObjectsWithScores()) {
            if(pattern.getScore().isFrozen() && pattern.getScore().isGood()) {
                goodPatternSet.add(pattern.getObject());
            }
        }

        Multiset<Seed> seeds = mappings.getSeedsForPatterns(goodPatternSet);
        for(Seed seed : seeds) {
            if(!targetAndScoreTables.getSeedScores().keySet().contains(seed))
                targetAndScoreTables.getSeedScores().addDefault(seed);

            // clear all frozen seeds
            targetAndScoreTables.getSeedScores().getScore(seed).unfreeze();

            SeedScore seedScore = targetAndScoreTables.getSeedScores().getScore(seed);
            seedScore.setFrequency(mappings.getInstancesForSeed(seed).size());
            seedScore.freezeScore(iteration);
        }

        targetAndScoreTables.serialize(new File(strFileOutputExtractor));
    }
}
