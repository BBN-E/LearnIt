package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.storage.StorageUtils;
import com.google.common.collect.Sets;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by mcrivaro on 9/11/2014.
 */
public class EvaluateQAAssessment {

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws IOException {
        String paramFile = args[0];
        String jsonBase = args[1];
        String output = args[2];

        LearnItConfig.loadParams(new File(paramFile));

        Set<Seed> systemGood =  StorageUtils.deserialize(new File(jsonBase + "_good.json"), HashSet.class, false);
        Set<Seed> systemBad  =  StorageUtils.deserialize(new File(jsonBase + "_bad.json"), HashSet.class, false);
        Set<Seed> humanGold  =  StorageUtils.deserialize(new File(jsonBase + "_human.json"), HashSet.class, false);

        Set<Seed> allGood = Sets.union(systemGood,humanGold);

        double precision = systemGood.isEmpty() ? 0.0 : ((double)systemGood.size()) / (systemGood.size() + systemBad.size());
        double recall    = systemGood.isEmpty() ? 0.0 : ((double)systemGood.size()) / allGood.size();

        double fscore    = (2*precision*recall)/(precision+recall);

        System.out.println("System correct: " + systemGood.size());
        System.out.println("System incorrect: " + systemBad.size());
        System.out.println("Total Gold: " + allGood.size());

        System.out.printf("P: %.4f,\tR: %.4f,\tF: %.4f\n",precision,recall,fscore);
        System.out.printf("Human recall: %.4f\n",((double)humanGold.size()) / allGood.size());

        File out = new File(output);
        if (!out.getParentFile().exists())
            out.getParentFile().mkdirs();

        FileWriter fw = new FileWriter(out);
        fw.write("System correct: " + systemGood.size() + "\n");
        fw.write("System incorrect: " + systemBad.size() + "\n");
        fw.write("Total Gold: " + allGood.size() + "\n\n");

        fw.write("System scores:\n");
        fw.write(String.format("P: %.4f,\tR: %.4f,\tF: %.4f\n\n",precision,recall,fscore));

        fw.write(String.format("Human recall: %.4f\n",((double)humanGold.size()) / allGood.size()));

        fw.close();
    }
}
