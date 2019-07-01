package com.bbn.akbc.neolearnit.tmp;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.common.targets.TargetSlot;
import com.bbn.akbc.neolearnit.common.targets.constraints.impl.EventMentionOnlyConstraint;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.util.GeneralUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class ConvertOldExtractorIntoCurrent {
    public static void main(String[] args) throws Exception {
        // Assert old extractors are all Event-event relations
        String params = args[0];
        LearnItConfig.loadParams(new File(params));
        String strFileListExtractor = args[1];
        String outputExtractorPath = args[2];
        Map<String, TargetAndScoreTables> targetAndScoreTablesMap = GeneralUtils.loadExtractorsFromFileList(strFileListExtractor);

        Date date = new Date();
        String timeString = (new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(date)).replace("-", "");

        for (String targetName : targetAndScoreTablesMap.keySet()) {
            TargetAndScoreTables oldScoreTable = targetAndScoreTablesMap.get(targetName);
            Target.Builder newTargetBuilder;
            newTargetBuilder = new Target.Builder(targetName)
                    .setDescription("")
                    .withTargetSlot(new TargetSlot.Builder(0, "all")
                            .build()).withTargetSlot(new TargetSlot.Builder(1, "all")
                            .build());
            newTargetBuilder.withAddedConstraint(new EventMentionOnlyConstraint(0, false));
            newTargetBuilder.withAddedConstraint(new EventMentionOnlyConstraint(1, false));
            Target newTarget = newTargetBuilder.build();
            newTarget.serialize(String.format("%s/%s", LearnItConfig.get("learnit_root"), String.format("inputs/targets/json/%s.json", targetName)));
            TargetAndScoreTables newScoreTable = new TargetAndScoreTables(String.format("%s/%s", LearnItConfig.get("learnit_root"), String.format("inputs/targets/json/%s.json", targetName)));
            newScoreTable = newScoreTable.copyWithPatternAndSeedScoreTable(oldScoreTable.getPatternScores(), oldScoreTable.getSeedScores());
            String strTargetPathDir = String.format("%s/%s/", outputExtractorPath, newScoreTable.getTarget().getName());
            File dir = new File(strTargetPathDir);
            if (!dir.exists()) {
                dir.mkdir();
            }
            String strPathJson = strTargetPathDir + newScoreTable.getTarget().getName() + "_" +
                    timeString + ".json";
            System.out.println("\t serializing extractor for " + newScoreTable + "...");
            newScoreTable.serialize(new File(strPathJson));
            System.out.println("\t\t...done.");
        }

    }
}
