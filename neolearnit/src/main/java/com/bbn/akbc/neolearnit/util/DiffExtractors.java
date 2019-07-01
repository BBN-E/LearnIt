package com.bbn.akbc.neolearnit.util;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.scoring.scores.PatternScore;
import com.bbn.akbc.neolearnit.scoring.tables.AbstractScoreTable;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DiffExtractors {
    public static void main(String[] args) throws IOException {
        String paramsFile = args[0];
        LearnItConfig.loadParams(new File(paramsFile));
        String extractorsPath1 = args[1];
        String extractorsPath2 = args[2];
        Map<String, TargetAndScoreTables> extractorsGroup1 =
                GeneralUtils.loadExtractors(extractorsPath1);
        Map<String, TargetAndScoreTables> extractorsGroup2 =
                GeneralUtils.loadExtractors(extractorsPath2);
        Set<LearnitPattern> biggerPatternSet = new HashSet<>();
        for(String relationName : extractorsGroup2.keySet()){
            if(relationName.trim().toLowerCase().equals("OTHER".trim().toLowerCase()))continue;
            TargetAndScoreTables targetAndScoreTables = extractorsGroup2.get(relationName);
            for (AbstractScoreTable.ObjectWithScore<LearnitPattern, PatternScore> pattern : targetAndScoreTables.getPatternScores().getObjectsWithScores()) {
                biggerPatternSet.add(pattern.getObject());
            }
        }
        for(String relationName : extractorsGroup1.keySet()){
            if(relationName.trim().toLowerCase().equals("OTHER".trim().toLowerCase()))continue;
            TargetAndScoreTables targetAndScoreTables = extractorsGroup1.get(relationName);
            for(AbstractScoreTable.ObjectWithScore<LearnitPattern, PatternScore> pattern : targetAndScoreTables.getPatternScores().getObjectsWithScores()){
                if(!biggerPatternSet.contains(pattern.getObject())){
                    System.out.println(pattern.getObject().toIDString());
                }
            }
        }
    }
}
