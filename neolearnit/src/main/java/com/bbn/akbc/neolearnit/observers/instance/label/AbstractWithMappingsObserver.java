package com.bbn.akbc.neolearnit.observers.instance.label;

import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;
import com.bbn.akbc.neolearnit.observers.instance.pattern.MonolingualPatternObserver;


public abstract class AbstractWithMappingsObserver extends MonolingualPatternObserver {

    // protected final Recorder<InstanceIdentifier, Property> recorder;
    protected final Mappings mappings;

    protected AbstractWithMappingsObserver(InstanceToPatternMapping.Builder recorder, String language, Mappings mappings) {
        super(recorder, language);
        this.mappings = mappings;
    }
    public void observe(MatchInfo match, MatchInfo.LanguageMatchInfo languageMatch){
        this.observe(match,languageMatch,this.mappings);
    }
    public abstract void observe(MatchInfo match, MatchInfo.LanguageMatchInfo languageMatch,Mappings mappings);
}