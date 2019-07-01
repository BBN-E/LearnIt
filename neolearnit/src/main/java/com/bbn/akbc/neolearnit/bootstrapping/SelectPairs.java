package com.bbn.akbc.neolearnit.bootstrapping;

import com.bbn.akbc.common.FileUtil;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.targets.TargetFactory;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.scoring.scores.SeedScore;
import com.bbn.akbc.neolearnit.scoring.scores.PatternScore;
import com.bbn.akbc.neolearnit.scoring.tables.AbstractScoreTable;

import java.io.File;
import java.util.*;

public class SelectPairs {

    public static void main(String[] args) throws Exception {
        String strFileParam = args[0];
        String strFileListInputExtractor = args[1];
        String strFileOutputExtractor = args[2];
        int minFrequence = Integer.parseInt(args[3]);
        int iteration = Integer.parseInt(args[4]);

        LearnItConfig.loadParams(new File(strFileParam));

        TargetAndScoreTables combinedTargetAndScoreTables = new TargetAndScoreTables(TargetFactory.makeEverythingTarget());

        // aggregate frequencies
        List<String> listInputExtractors = FileUtil.readLinesIntoList(strFileListInputExtractor);
        for(String strInputExtractor : listInputExtractors) {
            TargetAndScoreTables targetAndScoreTables = TargetAndScoreTables.deserialize(new File(strInputExtractor));

            // merge pairs
            for(AbstractScoreTable.ObjectWithScore<Seed, SeedScore> scoreObjectWithScore : targetAndScoreTables.getSeedScores().getObjectsWithScores()) {
                Seed seed = scoreObjectWithScore.getObject();
                SeedScore seedScore = scoreObjectWithScore.getScore();

                if(seedScore.isFrozen()) {

                    if (!combinedTargetAndScoreTables.getSeedScores().keySet().contains(seed)) {
                        combinedTargetAndScoreTables.getSeedScores().addDefault(seed);
                        SeedScore seedScoreNew = combinedTargetAndScoreTables.getSeedScores().getScore(seed);
                        seedScoreNew.setFrequency(seedScore.getFrequency());
                    } else {
                        SeedScore seedScoreNew = combinedTargetAndScoreTables.getSeedScores().getScore(seed);
                        seedScoreNew.setFrequency(seedScoreNew.getFrequency() + seedScore.getFrequency());
                    }
                }
            }

            // merge patterns
            for(AbstractScoreTable.ObjectWithScore<LearnitPattern, PatternScore> scoreObjectWithScore : targetAndScoreTables.getPatternScores().getObjectsWithScores()) {
                LearnitPattern pattern = scoreObjectWithScore.getObject();
                PatternScore patternScore = scoreObjectWithScore.getScore();

                if(patternScore.isFrozen() && patternScore.isGood()) {
                   if (!combinedTargetAndScoreTables.getPatternScores().keySet().contains(pattern)) {
                       combinedTargetAndScoreTables.getPatternScores().addDefault(pattern);

                       PatternScore patternScoreNew = combinedTargetAndScoreTables.getPatternScores().getScore(pattern);
                       patternScoreNew.setPrecision(0.6); // set to good pattern
                       combinedTargetAndScoreTables.getPatternScores().getScore(pattern).freezeScore(iteration);
                   }
                }
            }
        }

        // prune away low frequency ones
        for(AbstractScoreTable.ObjectWithScore<Seed, SeedScore> scoreObjectWithScore : combinedTargetAndScoreTables.getSeedScores().getObjectsWithScores()) {
            Seed seed = scoreObjectWithScore.getObject();
            SeedScore seedScore = scoreObjectWithScore.getScore();

            if(seedScore.getFrequency()>=minFrequence)
                combinedTargetAndScoreTables.getSeedScores().getScore(seed).freezeScore(iteration);
        }

        combinedTargetAndScoreTables.serialize(new File(strFileOutputExtractor));
    }
}
