package com.bbn.akbc.neolearnit.util;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.observations.similarity.SeedPatternPair;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.scoring.scores.PatternScore;
import com.bbn.akbc.neolearnit.scoring.scores.SeedScore;
import com.bbn.akbc.neolearnit.scoring.tables.AbstractScoreTable;
import com.google.common.collect.Sets;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MergeExtractors {

    public static Map<String,TargetAndScoreTables> mergeTargetAndScoreTables(Map<String,TargetAndScoreTables> master,Map<String,TargetAndScoreTables> aux){
        Map<String,TargetAndScoreTables> ret = new HashMap<>();

//        for(String targetName:Sets.difference(master.keySet(),aux.keySet())){
//            ret.put(targetName,master.get(targetName));
//        }
        
        for(String targetName:Sets.difference(aux.keySet(),master.keySet())){
            ret.put(targetName,aux.get(targetName));
        }

        for(String targetName: Sets.intersection(master.keySet(),aux.keySet())){
            TargetAndScoreTables mainTargetAndScoreTables = master.get(targetName);
            TargetAndScoreTables auxTargetAndScoreTables = aux.get(targetName);
            Sets.SetView<LearnitPattern> diffPattern = Sets.difference(mainTargetAndScoreTables.getPatternScores().keySet(),auxTargetAndScoreTables.getPatternScores().keySet());
            for(LearnitPattern learnitPattern: diffPattern){
                mainTargetAndScoreTables.getPatternScores().removeItem(learnitPattern);
            }
            for(AbstractScoreTable.ObjectWithScore<LearnitPattern, PatternScore> patternScoreTableEntry : auxTargetAndScoreTables.getPatternScores().getObjectsWithScores()){

                PatternScore patternScore = mainTargetAndScoreTables.getPatternScores().getScoreOrDefault(patternScoreTableEntry.getObject());
                boolean isGood =patternScore.isGood();
                if (patternScore.isFrozen()) patternScore.unfreeze();
                patternScore.setPrecision(isGood?0.95:0.05);
                patternScore.setConfidence(1.0);
                patternScore.freezeScore(0);
            }

            Sets.SetView<Seed> diffSeed = Sets.difference(mainTargetAndScoreTables.getSeedScores().keySet(),auxTargetAndScoreTables.getSeedScores().keySet());
            for(Seed seed: diffSeed){
                mainTargetAndScoreTables.getSeedScores().removeItem(seed);
            }
            for(AbstractScoreTable.ObjectWithScore<Seed, SeedScore> seedScoreTableEntry : auxTargetAndScoreTables.getSeedScores().getObjectsWithScores()){
                SeedScore seedScore = mainTargetAndScoreTables.getSeedScores().getScoreOrDefault(seedScoreTableEntry.getObject());
                boolean isGood = seedScore.isGood();
                if(seedScore.isFrozen()) seedScore.unfreeze();
                seedScore.setScore(isGood?0.95:0.05);
                seedScore.setConfidence(1.0);
                seedScore.freezeScore(0);
            }
            ret.put(targetName,mainTargetAndScoreTables);
        }

        return ret;
    }

    public static void main(String [] args) throws Exception {
        String params = args[0];
        String strFileExtractorMaster = args[1];
        String strFileExtractorAux = args[2];
        String strFileExtractorMerged = args[3];

        LearnItConfig.loadParams(new File(params));

        Map<String,TargetAndScoreTables> master = GeneralUtils.loadExtractors(strFileExtractorMaster);
        Map<String,TargetAndScoreTables> aux = GeneralUtils.loadExtractors(strFileExtractorAux);

        Map<String,TargetAndScoreTables> resolved = mergeTargetAndScoreTables(master,aux);

        for(TargetAndScoreTables targetAndScoreTables:resolved.values()){
            targetAndScoreTables.serialize(new File(strFileExtractorMerged+File.separator+targetAndScoreTables.getTarget().getName()+".json"));
        }
    }
}
