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
import com.bbn.akbc.neolearnit.util.GeneralUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class LearnItRelationPatternNALabeler implements MappingsLabeler {
    static Random rand = new Random();
    final Collection<TargetAndScoreTables> targetAndScoreTables;
    final Map<LearnitPattern, List<String>> learnitPatternToRelationName;
    final int MAX_INSTANCES_PER_SEED;

    public Collection<TargetAndScoreTables> fromExtractorList(List<String> strExtractorList) throws IOException {
        Map<String,TargetAndScoreTables> extractors = new HashMap<>();
        for(String strExtractorDirectory:strExtractorList){
            extractors.putAll(GeneralUtils.loadExtractors(strExtractorDirectory));
        }
        return extractors.values();
    }

    public LearnItRelationPatternNALabeler(List<String> extractorPaths, int MAX_INSTANCES_PER_SEED) throws Exception {
        this.targetAndScoreTables = fromExtractorList(extractorPaths);
        this.learnitPatternToRelationName = new HashMap<>();
        this.MAX_INSTANCES_PER_SEED = MAX_INSTANCES_PER_SEED;
        Set<LearnitPattern> goodPatternSet = new HashSet<>();
        for (TargetAndScoreTables table : this.targetAndScoreTables) {
            for (AbstractScoreTable.ObjectWithScore<LearnitPattern, PatternScore> pattern :
                    table.getPatternScores().getObjectsWithScores()) {
                if (pattern.getScore().isFrozen() && pattern.getScore().isGood()) {
                    if (!(pattern.getObject() instanceof PropPattern)) continue;
                    goodPatternSet.add(pattern.getObject());
                }
            }
            for (LearnitPattern learnitPattern : goodPatternSet) {
                List<String> buf = this.learnitPatternToRelationName.getOrDefault(learnitPattern, new ArrayList<>());
                buf.add("NON_NA");
                this.learnitPatternToRelationName.put(learnitPattern, buf);
            }
        }
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
            List<InstanceIdentifier> instanceIdentifierCollection = new ArrayList<>(original.getInstancesForSeed(seed));
            Collections.shuffle(instanceIdentifierCollection);

            for (int i = 0; i < Math.min(instanceIdentifierCollection.size(), MAX_INSTANCES_PER_SEED); i++) {
                // output to OpenNRE format

                Set<String> instanceRelationNames = getRelationNameByPatternMatching(original.getPatternsForInstance(instanceIdentifierCollection.get(i)),
                        learnitPatternToRelationName);

                if (!instanceRelationNames.isEmpty()) {
                    // skip
                    continue;
                } else {
                    // System.out.println("Added NA relation");
                    labeledMappings.addAnnotation(instanceIdentifierCollection.get(i), new LabelPattern("NA", Annotation.FrozenState.FROZEN_BAD));
                }
            }
        }
        return labeledMappings;
    }
}
