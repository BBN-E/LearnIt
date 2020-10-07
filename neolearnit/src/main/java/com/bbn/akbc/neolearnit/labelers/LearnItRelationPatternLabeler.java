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

public class LearnItRelationPatternLabeler implements MappingsLabeler {
    static Random rand = new Random();
    final MODE mode;
    final TargetAndScoreTables targetAndScoreTables;
    final Map<LearnitPattern, List<String>> learnitPatternToRelationName;
    final String relationName;
    final int MAX_INSTANCES_PER_SEED;
    final double NEGATIVE_SAMPLING_RATIO;

    public LearnItRelationPatternLabeler(MODE mode, String strFileExtractor, String relationName, int MAX_INSTANCES_PER_SEED, boolean USE_PROP_PATTERN_ONLY, double NEGATIVE_SAMPLING_RATIO) throws Exception {
        this.mode = mode;
        if(strFileExtractor.equals("EMPTY_EXTRACTOR")){
            this.targetAndScoreTables = new TargetAndScoreTables(TargetFactory.makeBinaryEventEventTarget());
        }
        else{
            this.targetAndScoreTables = TargetAndScoreTables.deserialize(new File(strFileExtractor));
        }
        this.learnitPatternToRelationName = new HashMap<>();
        this.relationName = relationName;
        this.MAX_INSTANCES_PER_SEED = MAX_INSTANCES_PER_SEED;
        this.NEGATIVE_SAMPLING_RATIO = NEGATIVE_SAMPLING_RATIO;
        Set<LearnitPattern> goodPatternSet = new HashSet<>();
        for (AbstractScoreTable.ObjectWithScore<LearnitPattern, PatternScore> pattern : this.targetAndScoreTables
                .getPatternScores().getObjectsWithScores()) {
            if (pattern.getScore().isFrozen() && pattern.getScore().isGood()) {
                if (USE_PROP_PATTERN_ONLY && !(pattern.getObject() instanceof PropPattern)) continue;
                goodPatternSet.add(pattern.getObject());
            }
        }
        for (LearnitPattern learnitPattern : goodPatternSet) {
            List<String> buf = this.learnitPatternToRelationName.getOrDefault(learnitPattern, new ArrayList<>());
            buf.add(relationName);
            this.learnitPatternToRelationName.put(learnitPattern, buf);
        }

    }

    static boolean PassSampling(double NEGATIVE_SAMPLING_RATIO) {
        return rand.nextInt(1000) <= 1000 * NEGATIVE_SAMPLING_RATIO;
    }

    static Set<String> getRelationNameByPatternMatching(Collection<LearnitPattern> learnitPatterns,
                                                        Map<LearnitPattern, List<String>> learnitPatternToRelationName) {
        Set<String> relationNames = new HashSet<>();
        for (LearnitPattern learnitPattern : learnitPatterns) {
            if (learnitPatternToRelationName.get(learnitPattern) != null) {
                relationNames.addAll(learnitPatternToRelationName.get(learnitPattern));
            }
        }

        return relationNames;
    }

    @Override
    public Annotation.InMemoryAnnotationStorage LabelMappings(Mappings original, Annotation.InMemoryAnnotationStorage labeledMappings) throws Exception {
        for (Seed seed : original.getAllSeeds().elementSet()) {
            if (this.mode == MODE.LABELING_SEED) {
                // determine if at-least-one pattern shows a relation
                String relationName = "NA";
                if (targetAndScoreTables.getSeedScores().keySet().contains(seed))
                    if (targetAndScoreTables.getSeedScores().getScore(seed).isFrozen() && targetAndScoreTables.getSeedScores().getScore(seed).isGood())
                        relationName = this.relationName;

                // sample negative instances
                if (relationName.equals("NA") && !PassSampling(this.NEGATIVE_SAMPLING_RATIO))
                    continue;

                // sample intances
                List<InstanceIdentifier> instanceIdentifierCollection = new ArrayList<>(original.getInstancesForSeed(seed));
                Collections.shuffle(instanceIdentifierCollection);

                for (int i = 0; i < Math.min(instanceIdentifierCollection.size(), MAX_INSTANCES_PER_SEED); i++) {
                    // output to OpenNRE format
                    labeledMappings.addAnnotation(instanceIdentifierCollection.get(i), new LabelPattern(relationName, !relationName.equals("NA") ? Annotation.FrozenState.FROZEN_GOOD : Annotation.FrozenState.FROZEN_BAD));
                }
            } else if (mode == MODE.LABELING_INSTANCE) {
                List<InstanceIdentifier> instanceIdentifierCollection = new ArrayList<>(original.getInstancesForSeed(seed));
                Collections.shuffle(instanceIdentifierCollection);

                for (int i = 0; i < Math.min(instanceIdentifierCollection.size(), MAX_INSTANCES_PER_SEED); i++) {
                    // output to OpenNRE format

                    Set<String> instanceRelationNames = getRelationNameByPatternMatching(original.getPatternsForInstance(instanceIdentifierCollection.get(i)),
                            learnitPatternToRelationName);

                    if (!instanceRelationNames.isEmpty()) {
                        for (String instanceRelationName : instanceRelationNames)
                            labeledMappings.addAnnotation(instanceIdentifierCollection.get(i), new LabelPattern(instanceRelationName, Annotation.FrozenState.FROZEN_GOOD));
                    } else {
                        labeledMappings.addAnnotation(instanceIdentifierCollection.get(i), new LabelPattern("NA", Annotation.FrozenState.FROZEN_BAD));
                    }
                }
            } else if (mode == MODE.DECODING) {
                for (InstanceIdentifier instanceIdentifier : original.getInstancesForSeed(seed)) {
                    labeledMappings.addAnnotation(instanceIdentifier, new LabelPattern("NA", Annotation.FrozenState.FROZEN_BAD));
                }
            }
        }
        return labeledMappings;
    }

    public enum MODE {
        LABELING_SEED,
        LABELING_INSTANCE,
        DECODING
    }
}
