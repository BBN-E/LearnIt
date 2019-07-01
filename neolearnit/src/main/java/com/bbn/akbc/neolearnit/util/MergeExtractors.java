package com.bbn.akbc.neolearnit.util;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.observations.similarity.SeedPatternPair;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;

import java.io.File;
import java.io.IOException;

public class MergeExtractors {

    public static TargetAndScoreTables loadExtractor(String strFile) throws IOException {

        System.out.println("load extractor: " + strFile);

        TargetAndScoreTables extractor =
             TargetAndScoreTables.deserialize(new File(strFile));

        return extractor;
    }

    public static void writeExtractor(TargetAndScoreTables targetAndScoreTables, String strPathJson) {
        try {
            targetAndScoreTables.serialize(new File(strPathJson));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void mergeExtractor(TargetAndScoreTables targetAndScoreTablesMaster, TargetAndScoreTables targetAndScoreTablesAux) {
        for(LearnitPattern p : targetAndScoreTablesAux.getPatternScores().getFrozen()) {
            if(!targetAndScoreTablesMaster.getPatternScores().getFrozen().contains(p)) {
                targetAndScoreTablesMaster.getPatternScores().addDefault(p);
                targetAndScoreTablesMaster.getPatternScores().getScore(p).setConfidence(1.0);
                targetAndScoreTablesMaster.getPatternScores().getScore(p).setPrecision(1.0);
                targetAndScoreTablesMaster.getPatternScores().getScore(p).setRecall(0.9);
                targetAndScoreTablesMaster.getPatternScores().getScore(p).freezeScore(0);
            }
        }

        for(Seed s : targetAndScoreTablesAux.getSeedScores().getFrozen()) {
            if(!targetAndScoreTablesMaster.getSeedScores().getFrozen().contains(s)) {
                targetAndScoreTablesMaster.getSeedScores().addDefault(s);;
                targetAndScoreTablesMaster.getSeedScores().getScore(s).setConfidence(1.0);
                targetAndScoreTablesMaster.getSeedScores().getScore(s).setScore(0.9);
                targetAndScoreTablesMaster.getSeedScores().getScore(s).freezeScore(0);
            }
        }

        for(SeedPatternPair sp : targetAndScoreTablesAux.getTripleScores().getFrozen()) {
            if(!targetAndScoreTablesMaster.getTripleScores().getFrozen().contains(sp)) {
                targetAndScoreTablesMaster.getTripleScores().addDefault(sp);;
                targetAndScoreTablesMaster.getTripleScores().getScore(sp).setConfidence(1.0);
                targetAndScoreTablesMaster.getTripleScores().getScore(sp).setPrecision(0.9);
                targetAndScoreTablesMaster.getTripleScores().getScore(sp).freezeScore(0);
            }
        }
    }

    public static void main(String [] args) throws IOException {
        String params = args[0];
        String strFileExtractorMaster = args[1];
        String strFileExtractorAux = args[2];
        String strFileExtractorMerged = args[3];

        LearnItConfig.loadParams(new File(params));

        TargetAndScoreTables extractorMaster = loadExtractor(strFileExtractorMaster);
        TargetAndScoreTables extractorAux = loadExtractor(strFileExtractorAux);

        mergeExtractor(extractorMaster, extractorAux);

        writeExtractor(extractorMaster, strFileExtractorMerged);
    }
}
