package com.bbn.akbc.neolearnit.observers.instance.label;

import com.bbn.akbc.common.Pair;
import com.bbn.akbc.neolearnit.common.Annotation.FrozenState;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;
import com.bbn.akbc.neolearnit.observations.pattern.LearnItPatternFactory;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.scoring.scores.PatternScore;
import com.bbn.akbc.neolearnit.scoring.tables.AbstractScoreTable;
import com.bbn.akbc.neolearnit.util.GeneralUtils;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.bbn.akbc.neolearnit.util.GeneralUtils.getLemmatizedPattern;

public class LearnItExtractorLabelObserver extends AbstractWithMappingsObserver {

    private static final Multimap<LearnitPattern, String> pattern2relation = HashMultimap.create();

    public void loadExtractors() {
        try {
            ArrayList<Pair<LearnitPattern, String>> patternIDStringAndLabels =
                    new ArrayList<>();
            Map<String, TargetAndScoreTables> latestExtractors = GeneralUtils.loadLatestExtractors();
            for (Map.Entry<String, TargetAndScoreTables> entry : latestExtractors.entrySet()) {
                String label = entry.getKey();
                TargetAndScoreTables ex = latestExtractors.get(label);
                for (AbstractScoreTable.ObjectWithScore<LearnitPattern, PatternScore> pattern : ex
                        .getPatternScores().getObjectsWithScores()) {
                    if(pattern.getScore().isFrozen() && pattern.getScore().isGood()){
                        patternIDStringAndLabels.add(new Pair<>(pattern.getObject(), label));
                    }
                }
            }
            for (Pair<LearnitPattern, String> patternAndLabel : patternIDStringAndLabels) {
                LearnitPattern pattern = patternAndLabel.getFirst();
                String label = patternAndLabel.getSecond();
                pattern2relation.put(pattern, label);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void observe(MatchInfo match, MatchInfo.LanguageMatchInfo languageMatch, Mappings mappings) {
        InstanceIdentifier instanceIdentifier = InstanceIdentifier.from(match);

        for(LearnitPattern learnitPattern : mappings.getPatternsForInstance(instanceIdentifier)) {
            if(pattern2relation.containsKey(learnitPattern)) {
                for(String relationType : pattern2relation.get(learnitPattern))
                    this.record(match,new LabelPattern(relationType,FrozenState.FROZEN_GOOD));
            }
        }
    }
    public LearnItExtractorLabelObserver(InstanceToPatternMapping.Builder recorder, String language, Mappings mappings) {
        super(recorder, language,mappings);
        loadExtractors();
    }
}
