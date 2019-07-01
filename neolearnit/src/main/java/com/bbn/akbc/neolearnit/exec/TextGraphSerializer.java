package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.common.Pair;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;
import com.bbn.akbc.neolearnit.observations.pattern.BetweenSlotsPattern;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.pattern.PropPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.google.common.base.Optional;

import java.io.*;
import java.util.List;

public class TextGraphSerializer {

    static Optional<Pair<String, String>> get_tokenized(Seed seed) {
        return Optional.of(new Pair(seed.getSlot(0).asString(), seed.getSlot(1).asString()));
    }

    static Optional<String> get_tokenized(LearnitPattern learnitPattern) {
        if (learnitPattern instanceof LabelPattern) {
            LabelPattern labelPattern = (LabelPattern) learnitPattern;
            return Optional.of(labelPattern.getLabel());
        } else if (learnitPattern instanceof BetweenSlotsPattern) {
            BetweenSlotsPattern betweenSlotsPattern = (BetweenSlotsPattern) learnitPattern;
            return Optional.of(betweenSlotsPattern.toIDString().replace('|',' '));
        } else if (learnitPattern instanceof PropPattern) {
            PropPattern propPattern = (PropPattern) learnitPattern;
            return Optional.of(propPattern.toDepString().replace('|',' '));
        } else {
            System.out.print("Skip pattern: " + learnitPattern.toIDString());
            return Optional.absent();
        }
    }

    public static void main(String[] args) {
        String strFileParam = args[0];
        String strFileListMappings = args[1];
        String strOutFile = args[2];

        try {
            BufferedWriter rnnTransE = new BufferedWriter(new FileWriter(
                    new File(strOutFile)));
            LearnItConfig.loadParams(new File(strFileParam));

            // deserialize mappings to search for instances ID
            List<String> filePaths =
                    com.bbn.akbc.utility.FileUtil.readLinesIntoList(strFileListMappings);
            for (String filePath : filePaths) {
                System.out.println("Load mapping file " + filePath);
                Mappings mappings = Mappings.deserialize(new File(filePath), true);

                for (LearnitPattern learnitPattern : mappings.getAllPatterns().elementSet()) {
                    Optional<String> patternString = get_tokenized(learnitPattern);
                    for (Seed seed : mappings.getSeedsForPattern(learnitPattern).elementSet()) {
                        Optional<Pair<String, String>> seedString = get_tokenized(seed);

                        if (patternString.isPresent() && seedString.isPresent()) {
                            rnnTransE.write(String.format("%s||%s||%s\n",
                                    seedString.get().getFirst(),
                                    patternString.get(),
                                    seedString.get().getSecond()
                            ));
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

    }
}
