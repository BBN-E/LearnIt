package com.bbn.akbc.neolearnit.labelers;

import com.bbn.akbc.neolearnit.common.Annotation;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.targets.TargetFactory;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;
import com.bbn.akbc.neolearnit.observations.pattern.BetweenSlotsPattern;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.pattern.PropPattern;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.scoring.scores.PatternScore;
import com.bbn.akbc.neolearnit.scoring.tables.AbstractScoreTable;
import com.bbn.akbc.neolearnit.util.GeneralUtils;
import com.google.common.base.Optional;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PatternStrLabeler implements MappingLabeler {
    // This labeler is using pattern string to generate LabelPattern's label
    final Set<LearnitPattern> goodPatternSet;
    final boolean useAllPatternsFromMappings;

    public PatternStrLabeler(List<TargetAndScoreTables> targetAndScoreTablesList) {
        this.goodPatternSet = new HashSet<>();
        this.useAllPatternsFromMappings = false;
        for (TargetAndScoreTables targetAndScoreTables : targetAndScoreTablesList) {
            for (AbstractScoreTable.ObjectWithScore<LearnitPattern, PatternScore> pattern : targetAndScoreTables.getPatternScores().getObjectsWithScores()) {
                if (pattern.getScore().isFrozen() && pattern.getScore().isGood()) {
                    this.goodPatternSet.add(pattern.getObject());
                }
            }
        }
    }

    public PatternStrLabeler(boolean useAllPatternsFromMappings) throws Exception {
        this.useAllPatternsFromMappings = useAllPatternsFromMappings;
        if (useAllPatternsFromMappings) {
            this.goodPatternSet = new HashSet<>();
        } else {
            List<TargetAndScoreTables> targetAndScoreTablesList = new ArrayList<>();
            String targetPathDir = String.format("%s/inputs/extractors/", LearnItConfig.get("learnit_root"));
            File dir = new File(targetPathDir);
            if (dir.exists()) {
                for (File subDir : dir.listFiles()) {
                    if (subDir.isDirectory()) {
                        String targetName = subDir.getName(); // target name is the directory name

                        String latestFileTimestamp = GeneralUtils.getLatestExtractor(targetName, subDir).orNull();
                        TargetAndScoreTables ex = new TargetAndScoreTables(TargetFactory.fromString(targetName));
                        if (latestFileTimestamp != null) {
                            String fileName = String.format("%s/%s_%s.json", subDir.getAbsolutePath(),
                                    targetName, latestFileTimestamp);
                            System.out.println("Loading extractor " + targetName + " from: " + fileName);
                            ex = TargetAndScoreTables.deserialize(new File(fileName));
                        }
                        targetAndScoreTablesList.add(ex);
                    }
                }
            }
            this.goodPatternSet = new HashSet<>();
            for (TargetAndScoreTables targetAndScoreTables : targetAndScoreTablesList) {
                for (AbstractScoreTable.ObjectWithScore<LearnitPattern, PatternScore> pattern : targetAndScoreTables.getPatternScores().getObjectsWithScores()) {
                    if (pattern.getScore().isFrozen() && pattern.getScore().isGood()) {
                        this.goodPatternSet.add(pattern.getObject());
                    }
                }
            }
        }

    }

    static Optional<String> get_tokenized(LearnitPattern learnitPattern) {
        if (learnitPattern instanceof LabelPattern) {
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

    @Override
    public Mappings LabelMappings(Mappings original) {
        Annotation.InMemoryAnnotationStorage inMemoryAnnotationStorage = new Annotation.InMemoryAnnotationStorage();
        if (useAllPatternsFromMappings) {
            for (LearnitPattern learnitPattern : original.getAllPatterns().elementSet()) {
                for (InstanceIdentifier instanceIdentifier : original.getInstancesForPattern(learnitPattern)) {
                    Optional<String> patternStr = get_tokenized(learnitPattern);
                    if (patternStr.isPresent()) {
                        inMemoryAnnotationStorage.addAnnotation(instanceIdentifier, new LabelPattern(patternStr.get(), Annotation.FrozenState.FROZEN_GOOD));
                    }

                }
            }
        } else {
            for (LearnitPattern learnitPattern : goodPatternSet) {
                for (InstanceIdentifier instanceIdentifier : original.getInstancesForPattern(learnitPattern)) {
                    Optional<String> patternStr = get_tokenized(learnitPattern);
                    if (patternStr.isPresent()) {
                        inMemoryAnnotationStorage.addAnnotation(instanceIdentifier, new LabelPattern(patternStr.get(), Annotation.FrozenState.FROZEN_GOOD));
                    }
                }
            }
        }
        return inMemoryAnnotationStorage.convertToMappings();
    }
}
