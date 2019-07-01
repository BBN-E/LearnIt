package com.bbn.akbc.neolearnit.labelers;

import com.bbn.akbc.neolearnit.common.Annotation;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.targets.Target;
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

public class LearnItRelationPatternLabeler implements MappingLabeler {
    static Random rand = new Random();
    final MODE mode;
    final TargetAndScoreTables targetAndScoreTables;
    final Map<LearnitPattern, List<String>> learnitPatternToRelationName;
    final String relationName;
    final int MAX_INSTANCES_PER_SEED;
    final boolean USE_PROP_PATTERN_ONLY;
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
        this.USE_PROP_PATTERN_ONLY = USE_PROP_PATTERN_ONLY;
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

    public static void main(String[] args) throws Exception {
        // @hqiu. It's upstream's responsibility to merge mappings.
        String paramPath = args[0];
        LearnItConfig.loadParams(new File(paramPath));
        String strFileExtractor = args[1];
        String strFileMappings = args[2];
        String strOutFilePrefix = args[3];
        String strRelationType = args[4];
        int MAX_INSTANCE_PER_SEED = Integer.parseInt(args[5]);
        double NEGATICE_SAMPLING_RATIO = Double.parseDouble(args[6]);
        final MODE mode = MODE.valueOf(args[7]);
        final boolean USE_PROP_PATTERN_ONLY = false;
        LearnItRelationPatternLabeler learnItRelationPatternLabeler = new LearnItRelationPatternLabeler(mode, strFileExtractor, strRelationType, MAX_INSTANCE_PER_SEED, USE_PROP_PATTERN_ONLY, NEGATICE_SAMPLING_RATIO);

        Mappings labeledMappings = learnItRelationPatternLabeler.LabelMappings(Mappings.deserialize(new File(strFileMappings), true));
        labeledMappings.serialize(new File(strOutFilePrefix), true);
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
    public Mappings LabelMappings(Mappings original) {
        Annotation.InMemoryAnnotationStorage inMemoryAnnotationStorage = new Annotation.InMemoryAnnotationStorage();
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
                    inMemoryAnnotationStorage.addAnnotation(instanceIdentifierCollection.get(i), new LabelPattern(relationName, !relationName.equals("NA") ? Annotation.FrozenState.FROZEN_GOOD : Annotation.FrozenState.FROZEN_BAD));
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
                            inMemoryAnnotationStorage.addAnnotation(instanceIdentifierCollection.get(i), new LabelPattern(instanceRelationName, Annotation.FrozenState.FROZEN_GOOD));
                    } else {
                        inMemoryAnnotationStorage.addAnnotation(instanceIdentifierCollection.get(i), new LabelPattern("NA", Annotation.FrozenState.FROZEN_BAD));
                    }
                }
            } else if (mode == MODE.DECODING) {
                for (InstanceIdentifier instanceIdentifier : original.getInstancesForSeed(seed)) {
                    inMemoryAnnotationStorage.addAnnotation(instanceIdentifier, new LabelPattern("NA", Annotation.FrozenState.FROZEN_BAD));
                }
            }
        }
        return inMemoryAnnotationStorage.convertToMappings();
    }

    public enum MODE {
        LABELING_SEED,
        LABELING_INSTANCE,
        DECODING
    }
}
