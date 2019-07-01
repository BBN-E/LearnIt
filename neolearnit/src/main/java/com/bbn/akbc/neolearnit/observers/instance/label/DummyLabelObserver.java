package com.bbn.akbc.neolearnit.observers.instance.label;

import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;

public class DummyLabelObserver extends AbstractWithMappingsObserver {
    @Override
    public void observe(MatchInfo match, MatchInfo.LanguageMatchInfo languageMatch, Mappings mappings) {
//        this.record(match,new LabelPattern("Cause"));
    }
    public DummyLabelObserver(InstanceToPatternMapping.Builder recorder, String language, Mappings mappings) {
        super(recorder, language,mappings);
    }
}
