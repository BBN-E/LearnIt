package com.bbn.akbc.neolearnit.bootstrapping;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;
import com.bbn.akbc.neolearnit.observations.pattern.BetweenSlotsPattern;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.pattern.PropPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.google.common.base.Optional;

import java.io.File;
import java.io.PrintWriter;


public class GenerateTrainingDataForGraphEmbeddings {
    public static Optional<String> get_tokenized(LearnitPattern learnitPattern) {
        if(learnitPattern instanceof LabelPattern) {
            LabelPattern labelPattern = (LabelPattern) learnitPattern;
            return Optional.of(labelPattern.getLabel());
        } else if (learnitPattern instanceof BetweenSlotsPattern) {
            BetweenSlotsPattern betweenSlotsPattern = (BetweenSlotsPattern) learnitPattern;
            return Optional.of(betweenSlotsPattern.toIDString());
        } else if (learnitPattern instanceof PropPattern) {
            PropPattern propPattern = (PropPattern) learnitPattern;
            return Optional.of(propPattern.toDepString());
        } else {
            System.out.print("Skip pattern: " + learnitPattern.toIDString());
            return Optional.absent();
        }
    }

    public static void main(String[] args) throws Exception{

        String strFileParam = args[0];
        String strFileMappings = args[1];
        String strOutFile = args[2];

        PrintWriter printWriter = new PrintWriter(strOutFile);

        LearnItConfig.loadParams(new File(strFileParam));

        Mappings mappings = Mappings.deserialize(new File(strFileMappings), true);

        for(LearnitPattern learnitPattern : mappings.getAllPatterns().elementSet()) {
            Optional<String> patternStringOptional = get_tokenized(learnitPattern);

            if(patternStringOptional.isPresent()) {
                for (Seed seed : mappings.getSeedsForPattern(learnitPattern).elementSet()) {

                    printWriter.write(String.format("%s||%s||%s\n",
                            seed.getSlot(0).asString(),
                            patternStringOptional.get(),
                            seed.getSlot(1).asString()));
                }
            }
        }

        /*
        RNNTransEObserver rnnTransEObserver = new RNNTransEObserver(new PrintWriter(strOutFilePrefix));

        for(InstanceIdentifier instanceIdentifier : mappings.getPatternInstances()) {
            for(LearnitPattern learnitPattern : mappings.getPatternsForInstance(instanceIdentifier)) {
                rnnTransEObserver.observe(new Pair<InstanceIdentifier, LearnitPattern>(instanceIdentifier, learnitPattern));
            }
        }
        */
    }
}
