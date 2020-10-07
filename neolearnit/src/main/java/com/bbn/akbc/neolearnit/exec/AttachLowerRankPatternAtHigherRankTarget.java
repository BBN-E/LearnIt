package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToSeedMapping;
import com.bbn.akbc.neolearnit.observations.pattern.ComboPattern;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.pattern.SSUnigram;
import com.bbn.akbc.neolearnit.observations.pattern.UnaryPattern.HeadWordPOSTagPattern;
import com.bbn.akbc.neolearnit.observations.pattern.UnaryPattern.TypePattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.storage.impl.HashMapStorage;
import com.google.common.base.Optional;

import java.io.File;

public class AttachLowerRankPatternAtHigherRankTarget {
    public static void main(String[] args) throws Exception {
        String learnitConfigPath = args[0];
        LearnItConfig.loadParams(new File(learnitConfigPath));

        Mappings inputMappings = Mappings.deserialize(new File(args[1]), true);

        InstanceToPatternMapping.Builder instanceToPatternMapping = new InstanceToPatternMapping.Builder(new HashMapStorage.Builder<>());
        InstanceToSeedMapping.Builder instanceToSeedMapping = new InstanceToSeedMapping.Builder(new HashMapStorage.Builder<>());

        for (InstanceIdentifier binaryInstanceIdentifier : inputMappings.getPatternInstances()) {
            if (!binaryInstanceIdentifier.getSlot0SpanningType().equals(InstanceIdentifier.SpanningType.Empty) && !binaryInstanceIdentifier.getSlot1SpanningType().equals(InstanceIdentifier.SpanningType.Empty)) {
                InstanceIdentifier myFakeLeftInstanceIdentifier = binaryInstanceIdentifier.getLowerRankInstanceIdentifierLeft();
                InstanceIdentifier myFakeRightInstanceIdentifier = binaryInstanceIdentifier.getLowerRankInstanceIdentifierRight();
                for (LearnitPattern binaryLearnitPattern : inputMappings.getPatternsForInstance(binaryInstanceIdentifier)) {
                    if (binaryLearnitPattern instanceof SSUnigram) continue;
                    for (LearnitPattern unaryLeftPattern : inputMappings.getPatternsForInstance(myFakeLeftInstanceIdentifier)) {
                        if (!(unaryLeftPattern instanceof HeadWordPOSTagPattern) && !(unaryLeftPattern instanceof TypePattern))
                            continue;
                        instanceToPatternMapping.record(binaryInstanceIdentifier, new ComboPattern(binaryLearnitPattern, unaryLeftPattern));
                        for (LearnitPattern unaryRightPattern : inputMappings.getPatternsForInstance(myFakeRightInstanceIdentifier)) {
                            if (!(unaryRightPattern instanceof HeadWordPOSTagPattern) && !(unaryRightPattern instanceof TypePattern))
                                continue;
                            instanceToPatternMapping.record(binaryInstanceIdentifier, new ComboPattern(binaryLearnitPattern, new ComboPattern(unaryLeftPattern, unaryRightPattern)));
                        }
                    }
                    for (LearnitPattern unaryRightPattern : inputMappings.getPatternsForInstance(myFakeRightInstanceIdentifier)) {
                        if (!(unaryRightPattern instanceof HeadWordPOSTagPattern) && !(unaryRightPattern instanceof TypePattern))
                            continue;
                        instanceToPatternMapping.record(binaryInstanceIdentifier, new ComboPattern(binaryLearnitPattern, unaryRightPattern));
                    }
                }
            }
        }
        for (InstanceIdentifier instanceIdentifier : inputMappings.getPatternInstances()) {
            for (LearnitPattern learnitPattern : inputMappings.getPatternsForInstance(instanceIdentifier)) {
                instanceToPatternMapping.record(instanceIdentifier, learnitPattern);
            }
        }
        for (InstanceIdentifier instanceIdentifier : inputMappings.getSeedInstances()) {
            for (Seed seed : inputMappings.getSeedsForInstance(instanceIdentifier)) {
                instanceToSeedMapping.record(instanceIdentifier, seed);
            }
        }
        Mappings outputMappings = new Mappings(instanceToSeedMapping.build(), instanceToPatternMapping.build());
        outputMappings.serialize(new File(args[2]), true);
    }

}
