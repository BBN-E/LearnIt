package com.bbn.akbc.neolearnit.labelers;

import com.bbn.akbc.neolearnit.common.Annotation;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.targets.TargetFactory;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.pattern.PropPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.scoring.scores.PatternScore;
import com.bbn.akbc.neolearnit.scoring.tables.AbstractScoreTable;

import java.io.File;
import java.util.*;

public class BinaryEventInstanceLabeler implements MappingsLabeler {
    final TargetAndScoreTables targetAndScoreTables;
    final Set<LearnitPattern> goodPatternSet;
    final int MAX_INSTANCES_PER_SEED;
    public BinaryEventInstanceLabeler(String extractorPath,int MAX_INSTANCES_PER_SEED,boolean USE_PROP_PATTERN_ONLY) throws Exception{
        if(extractorPath.equals("EMPTY_EXTRACTOR")) {
            this.targetAndScoreTables = new TargetAndScoreTables(TargetFactory.makeBinaryEventEventTarget());
        }
        else {
            this.targetAndScoreTables = TargetAndScoreTables.deserialize(new File(extractorPath));
        }
        this.goodPatternSet = new HashSet<>();
        this.MAX_INSTANCES_PER_SEED = MAX_INSTANCES_PER_SEED;
        for (AbstractScoreTable.ObjectWithScore<LearnitPattern, PatternScore> pattern : this.targetAndScoreTables
                .getPatternScores().getObjectsWithScores()) {
            if (pattern.getScore().isFrozen() && pattern.getScore().isGood()) {
                if (USE_PROP_PATTERN_ONLY && !(pattern.getObject() instanceof PropPattern)) continue;
                goodPatternSet.add(pattern.getObject());
            }
        }
    }
    @Override
    public Annotation.InMemoryAnnotationStorage LabelMappings(Mappings original, Annotation.InMemoryAnnotationStorage labeledMappings) throws Exception {
        for(Seed seed:original.getAllSeeds().elementSet()){
            List<InstanceIdentifier> instanceIdentifierCollection = new ArrayList<>(original.getInstancesForSeed(seed));
            Collections.shuffle(instanceIdentifierCollection);
            for (int i = 0; i < Math.min(instanceIdentifierCollection.size(), MAX_INSTANCES_PER_SEED); i++) {
                // output to OpenNRE format
                boolean capturedByGoodPattern = false;
                for(LearnitPattern learnitPattern: original.getPatternsForInstance(instanceIdentifierCollection.get(i))){
                    if(this.goodPatternSet.contains(learnitPattern)){
                        capturedByGoodPattern = true;
                        break;
                    }
                }
                if (capturedByGoodPattern) {
                    labeledMappings.addAnnotation(instanceIdentifierCollection.get(i), new LabelPattern(this.targetAndScoreTables.getTarget().getName(), Annotation.FrozenState.FROZEN_GOOD));
                } else {
                    labeledMappings.addAnnotation(instanceIdentifierCollection.get(i), new LabelPattern("NA", Annotation.FrozenState.FROZEN_BAD));
                }
            }
        }
        return labeledMappings;
    }
}
