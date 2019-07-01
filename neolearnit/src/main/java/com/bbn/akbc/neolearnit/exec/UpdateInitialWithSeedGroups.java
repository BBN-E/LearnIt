package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.resources.SeedGroups;
import com.bbn.akbc.neolearnit.common.resources.StopWords;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.scoring.scores.SeedScore;
import com.bbn.akbc.neolearnit.storage.StorageUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by mcrivaro on 8/7/2014.
 */
public class UpdateInitialWithSeedGroups {

    public static void main(String[] args) throws IOException {
        LearnItConfig.loadParams(new File(args[0]));
        String target = args[1];
        TargetAndScoreTables data = TargetAndScoreTables.deserialize(new File(target));

        if (!LearnItConfig.optionalParamTrue("use_seed_groups"))
            throw new RuntimeException("Why is this being run if you aren't using seed groups?");

        SeedGroups.load(data);

        //Take care of things in groups with initial seeds at iteration 1
        System.out.println("Freezing initial seed groups...");
        for (Seed frozen : data.getSeedScores().getFrozen()) {
            System.out.println("For " + frozen.toSimpleString() + "...");
            SeedScore frozenScore = data.getSeedScores().getScore(frozen);
            for (Seed groupSeed : SeedGroups.getGroupOrSingleton(frozen.getReducedForm(StopWords.getFromParamsWithBackoff("overlap_seed_match_filter_word_list")), frozen)) {
                if (data.getSeedScores().isKnownFrozen(groupSeed)) continue;
                SeedScore groupSeedScore = data.getSeedScores().getScoreOrDefault(groupSeed);
                groupSeedScore.setIdenticalTo(frozenScore);
                groupSeedScore.freezeScore(0);
                System.out.println("\tFroze " + groupSeed.toSimpleString());
            }
        }

        StorageUtils.serialize(new File(target), data, false);

    }
}
