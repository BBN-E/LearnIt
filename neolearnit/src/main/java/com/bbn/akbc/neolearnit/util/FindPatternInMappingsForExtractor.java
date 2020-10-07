package com.bbn.akbc.neolearnit.util;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.scoring.scores.PatternScore;
import com.bbn.akbc.neolearnit.scoring.tables.AbstractScoreTable;

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


public class FindPatternInMappingsForExtractor {

    public static String getElementsFromOldPatterns(LearnitPattern pattern) {
        String strPattern = pattern.toIDString().replace("{", " ").replace("}", " ")
                .replace("[", " ").replace("]", " ")
                .replace("=", " ")
                .replace(":", " ");

        return strPattern;
    }

    public static String getElementsFromSexpPatterns(LearnitPattern pattern) {
        String strPattern = pattern.toPrettyString()
                .replace("[v]", " ").replace("[n]", " ")
                .replace("[OTH]", " ").replace("[PER]", " ").replace("[ORG]", " ").replace("[GPE]", " ").replace("[FAC]", " ").replace("[VEH]", " ").replace("[WEA]", " ").replace("[UNDET]", " ")
                .replace("[", " ").replace("]", " ");

        return strPattern;
    }

//    public boolean isGoodMatch(LearnitPattern oldPattern, LearnitPattern sexpPattern) {
//
//    }

    public static void main(String[] args) throws IOException {
        String paramsFile = args[0];
        LearnItConfig.loadParams(new File(paramsFile));

        String extractorFileList = args[1];
        String mappingsFile = args[2];

        Map<String, TargetAndScoreTables> extractorsGroup1 =
                GeneralUtils.loadExtractorsFromFileList(extractorFileList);

        for(String relationName : extractorsGroup1.keySet()){
            System.out.println();

            TargetAndScoreTables targetAndScoreTables = extractorsGroup1.get(relationName);
            for(AbstractScoreTable.ObjectWithScore<LearnitPattern, PatternScore> pattern : targetAndScoreTables.getPatternScores().getObjectsWithScores()){
                LearnitPattern p = pattern.getObject();

                System.out.println(relationName.trim() + "\t" + p.toIDString() + "\t" + getElementsFromOldPatterns(p));
            }
        }

//        Mappings mappings = Mappings.deserialize(new File(mappingsFile), true);
//        for (LearnitPattern learnitPattern : mappings.getAllPatterns().elementSet()) {
//            if(learnitPattern.toPrettyString().contains("[1]")) {
//                System.out.println("pattern:\t" + learnitPattern.toPrettyString() + "\t" + getElementsFromSexpPatterns(learnitPattern));
//            }
//        }
    }
}
