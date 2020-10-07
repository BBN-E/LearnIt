package com.bbn.akbc.neolearnit.util;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.pattern.SerifPattern;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.scoring.scores.PatternScore;
import com.bbn.akbc.neolearnit.scoring.tables.AbstractScoreTable;
import com.bbn.akbc.neolearnit.scoring.tables.PatternScoreTable;

import java.io.File;

public class SeperateExtractors {

    public static void addPatternToTable(LearnitPattern learnitPattern,PatternScoreTable patternScoreTable,boolean isGood){
        PatternScore patternScore = patternScoreTable.getScoreOrDefault(learnitPattern);
        if (patternScore.isFrozen()) patternScore.unfreeze();
        patternScore.setPrecision(isGood?0.95:0.05);
        patternScore.setConfidence(1.0);
        patternScore.freezeScore(0);
    }

    public static void main(String[] args) throws Exception {
        LearnItConfig.loadParams(new File(args[0]));
        String inputRoot = args[1];
        String newOutputRoot = args[2];
        String legacyOutputRoot = args[3];
        File dir = new File(inputRoot);
        for (File file : dir.listFiles()) {
            TargetAndScoreTables targetAndScoreTables = TargetAndScoreTables.deserialize(file);
            PatternScoreTable serifPatternScoreTable = new PatternScoreTable();
            PatternScoreTable oldPatternScoreTable = new PatternScoreTable();
            for (AbstractScoreTable.ObjectWithScore<LearnitPattern, PatternScore> patternScoreTableEntry : targetAndScoreTables.getPatternScores().getObjectsWithScores()) {
                if(patternScoreTableEntry.getScore().isFrozen()){
                    LearnitPattern srcLearnitPattern = patternScoreTableEntry.getObject();
                    if(!(srcLearnitPattern instanceof SerifPattern)){
                        addPatternToTable(srcLearnitPattern,oldPatternScoreTable,patternScoreTableEntry.getScore().isGood());
                    }
                    else{
                        addPatternToTable(srcLearnitPattern,serifPatternScoreTable,patternScoreTableEntry.getScore().isGood());
                    }
                }
            }
            if(oldPatternScoreTable.getObjectsWithScores().size()> 0){
                TargetAndScoreTables newTargetAndScoreTables = targetAndScoreTables.copyWithPatternScoreTable(oldPatternScoreTable);
                newTargetAndScoreTables.serialize(new File(legacyOutputRoot+File.separator+targetAndScoreTables.getTarget().getName()+".json"));
            }
            TargetAndScoreTables newTargetAndScoreTables = targetAndScoreTables.copyWithPatternScoreTable(serifPatternScoreTable);
            newTargetAndScoreTables.serialize(new File(newOutputRoot+File.separator+targetAndScoreTables.getTarget().getName()+".json"));
        }
    }
}
