package com.bbn.akbc.neolearnit.util;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.pattern.SerifPattern;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.scoring.scores.PatternScore;
import com.bbn.akbc.neolearnit.scoring.tables.AbstractScoreTable;
import com.bbn.akbc.neolearnit.scoring.tables.PatternScoreTable;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import static com.bbn.akbc.neolearnit.util.AlignOldPatternToNewPattern.getNodeNamesFromOntology;

public class LintSerifPattern {

    public static void addPatternToTable(LearnitPattern learnitPattern, PatternScoreTable patternScoreTable, boolean isGood) {
        PatternScore patternScore = patternScoreTable.getScoreOrDefault(learnitPattern);
        if (patternScore.isFrozen()) patternScore.unfreeze();
        patternScore.setPrecision(isGood ? 0.95 : 0.05);
        patternScore.setConfidence(1.0);
        patternScore.freezeScore(0);
    }

    public static String getSerifPatternString(SerifPattern serifPattern) {
        return serifPattern.toIDString();
    }

    public static void main(String[] args) throws Exception {
        LearnItConfig.loadParams(new File(args[0]));

        Set<String> unaryExtractorNameSet = new HashSet<>();
        Set<String> binaryExtractorNameSet = new HashSet<>();
        unaryExtractorNameSet.addAll(getNodeNamesFromOntology(String.format("%s/inputs/ontologies/cx/bae_icm/internal_ontology/unary_event_ontology_hume.yaml", LearnItConfig.get("learnit_root"))));
        unaryExtractorNameSet.addAll(getNodeNamesFromOntology(String.format("%s/inputs/ontologies/wm/hume/internal_ontology/unary_event_ontology_hume.yaml", LearnItConfig.get("learnit_root"))));
        binaryExtractorNameSet.addAll(getNodeNamesFromOntology(String.format("%s/inputs/ontologies/cx/bae_icm/internal_ontology/binary_event_ontology.yaml", LearnItConfig.get("learnit_root"))));
        binaryExtractorNameSet.addAll(getNodeNamesFromOntology(String.format("%s/inputs/ontologies/cx/bae_icm/internal_ontology/binary_event_entity_or_value_mention.yaml", LearnItConfig.get("learnit_root"))));

        File dir = new File(String.format("%s/inputs/extractors/", LearnItConfig.get("learnit_root")));

        for (File file : dir.listFiles()) {
            String extractorName = file.getName().replace(".json", "");
            if (unaryExtractorNameSet.contains(extractorName) || binaryExtractorNameSet.contains(extractorName)) {
                TargetAndScoreTables targetAndScoreTables = TargetAndScoreTables.deserialize(file);
                PatternScoreTable patternScoreTable = new PatternScoreTable();
                for (AbstractScoreTable.ObjectWithScore<LearnitPattern, PatternScore> patternScoreTableEntry : targetAndScoreTables.getPatternScores().getObjectsWithScores()) {
                    if(patternScoreTableEntry.getScore().isFrozen()){
                        if (patternScoreTableEntry.getObject() instanceof SerifPattern) {
                            SerifPattern serifPattern = new SerifPattern(getSerifPatternString((SerifPattern) patternScoreTableEntry.getObject()));
                            addPatternToTable(serifPattern, patternScoreTable, patternScoreTableEntry.getScore().isGood());
                        } else {
                            addPatternToTable(patternScoreTableEntry.getObject(), patternScoreTable, patternScoreTableEntry.getScore().isGood());
                        }
                    }
                }
                TargetAndScoreTables newTargetAndScoreTables = targetAndScoreTables.copyWithPatternScoreTable(patternScoreTable);
                newTargetAndScoreTables.serialize(file);
            }
        }
    }

}
