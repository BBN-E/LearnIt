package com.bbn.akbc.neolearnit.modules;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.loaders.impl.MonolingualDocTheoryInstanceLoader;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToSeedMapping;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observers.InstanceObservers;
import com.bbn.akbc.neolearnit.observers.instance.label.LearnItExtractorLabelObserver;
import com.bbn.akbc.neolearnit.storage.MapStorage;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class MappingExtractionModule {
    private final InstanceObservers binaryObservers;
    private final InstanceObservers unaryObservers;
    private final InstanceToPatternMapping.Builder observationRecorder;
    private final Mappings oldMappings;
    public MappingExtractionModule(Mappings mappings) {
        this.oldMappings = mappings;
        final Injector recorderInjector = Guice.createInjector(new RecorderModule());
        this.observationRecorder = recorderInjector.getInstance(InstanceToPatternMapping.Builder.class);
        String primaryLanguage = LearnItConfig.getList("languages").get(0);
        this.binaryObservers = new InstanceObservers.Builder().withObserver(new LearnItExtractorLabelObserver(this.observationRecorder,primaryLanguage,mappings))
                .build();
        this.unaryObservers = new InstanceObservers.Builder().withObserver(new LearnItExtractorLabelObserver(this.observationRecorder,primaryLanguage,mappings)).build();
    }
    public MonolingualDocTheoryInstanceLoader getDocTheoryLoader(Target target) {
        return new MonolingualDocTheoryInstanceLoader(target, this.binaryObservers,this.unaryObservers);
    }
    public Mappings getInformationForScoring() {
        final InstanceToSeedMapping oldinstance2Seed = this.oldMappings.getInstance2Seed();
        final InstanceToPatternMapping oldinstance2Pattern = this.oldMappings.getInstance2Pattern();
        final InstanceToPatternMapping labelledinstance2Pattern = this.observationRecorder.build();
        final MapStorage.Builder<InstanceIdentifier,LearnitPattern> newinstance2Pattern = oldinstance2Pattern.getStorage().newBuilder();
        newinstance2Pattern.putAll(oldinstance2Pattern.getStorage());
        newinstance2Pattern.putAll(labelledinstance2Pattern.getStorage());
        return new Mappings(oldinstance2Seed, new InstanceToPatternMapping(newinstance2Pattern.build()));
    }
}
