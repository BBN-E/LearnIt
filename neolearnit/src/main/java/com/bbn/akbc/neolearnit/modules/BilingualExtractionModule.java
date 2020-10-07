package com.bbn.akbc.neolearnit.modules;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.loaders.impl.BilingualDocTheoryInstanceLoader;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToMatchInfoDisplayMap;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToSeedMapping;
import com.bbn.akbc.neolearnit.observers.InstanceObservers;
import com.bbn.akbc.neolearnit.observers.instance.SeedObserver;
import com.bbn.akbc.neolearnit.observers.instance.matchinfo.MatchInfoDisplayObserver;
import com.bbn.akbc.neolearnit.observers.instance.pattern.Binary.PropObserver;
import com.bbn.akbc.neolearnit.observers.instance.pattern.Binary.TextBeforeAfterSlotsObserver;
import com.bbn.akbc.neolearnit.observers.instance.pattern.Binary.TextEntityTypeBetweenSlotsObserver;
import com.bbn.akbc.neolearnit.observers.instance.pattern.SerifPatternObserver;
import com.bbn.akbc.neolearnit.observers.instance.pattern.Unary.EventMentionNounPhraseObserver;
import com.bbn.akbc.neolearnit.observers.instance.pattern.Unary.UnaryHeadWordPOSTagObsever;
import com.bbn.akbc.neolearnit.observers.instance.pattern.Unary.UnaryPropObserver;
import com.google.inject.Guice;
import com.google.inject.Injector;


public class BilingualExtractionModule {

	private final Injector recorderInjector;

	private final InstanceToSeedMapping.Builder seedRecorder;
	private final InstanceToPatternMapping.Builder observationRecorder;
	private final InstanceToMatchInfoDisplayMap.Builder matchDisplayRecorder;
	private final InstanceObservers binaryObservers;
	private final InstanceObservers unaryObservers;

	public BilingualExtractionModule() {
		this(false);
	}

	public BilingualExtractionModule(boolean storeMatchInfoDisplay) {
		recorderInjector = Guice.createInjector(new RecorderModule());

		// mapping builders are recorders
		this.seedRecorder = recorderInjector.getInstance(InstanceToSeedMapping.Builder.class);
		this.observationRecorder = recorderInjector.getInstance(InstanceToPatternMapping.Builder.class);
		this.matchDisplayRecorder = recorderInjector.getInstance(InstanceToMatchInfoDisplayMap.Builder.class);

		String primaryLanguage = LearnItConfig.getList("languages").get(0);
		String secondaryLanguage = LearnItConfig.getList("languages").get(1);
		// observers require recorders of the correct types
		InstanceObservers.Builder binaryObsBuilder = new InstanceObservers.Builder();
		InstanceObservers.Builder UnaryObsBuilder = new InstanceObservers.Builder();

		if (storeMatchInfoDisplay) {
			binaryObsBuilder.withObserver(new MatchInfoDisplayObserver(matchDisplayRecorder));
			UnaryObsBuilder.withObserver(new MatchInfoDisplayObserver(matchDisplayRecorder));
		}

		binaryObservers = binaryObsBuilder.build();
		/*
		binaryObservers = binaryObsBuilder
			.withObserver(new SeedObserver(seedRecorder))
			.withObserver(new PropObserver(observationRecorder, primaryLanguage, LearnItConfig.getInt("max_prop_depth")))
			.withObserver(new TextEntityTypeBetweenSlotsObserver(observationRecorder, primaryLanguage, LearnItConfig.getInt("max_regexp_pattern_words")))
			.withObserver(new TextBeforeAfterSlotsObserver(observationRecorder, primaryLanguage, LearnItConfig.getInt("max_regexp_pattern_words")))

			.withObserver(new PropObserver(observationRecorder, secondaryLanguage, LearnItConfig.getInt("max_prop_depth")))
			.withObserver(new TextEntityTypeBetweenSlotsObserver(observationRecorder, secondaryLanguage, LearnItConfig.getInt("max_regexp_pattern_words")))
			.withObserver(new TextBeforeAfterSlotsObserver(observationRecorder, secondaryLanguage, LearnItConfig.getInt("max_regexp_pattern_words")))
			.build();
			*/

		unaryObservers = UnaryObsBuilder
				.withObserver(new SeedObserver(seedRecorder))
				.withObserver(new UnaryHeadWordPOSTagObsever(observationRecorder, primaryLanguage))
				//.withObserver(new UnaryPropObserver(observationRecorder, primaryLanguage))
				//.withObserver(new EventMentionNounPhraseObserver(observationRecorder,primaryLanguage))
				.withObserver(new UnaryHeadWordPOSTagObsever(observationRecorder, secondaryLanguage)) // for foreign lang
				//.withObserver(new UnaryPropObserver(observationRecorder, secondaryLanguage)) // for foreign lang
				//.withObserver(new EventMentionNounPhraseObserver(observationRecorder,secondaryLanguage)) // for foreign lang
				.withObserver(new SerifPatternObserver(observationRecorder,primaryLanguage,LearnItConfig.getInt("max_unary_prop_depth"),LearnItConfig.getInt("max_binary_prop_depth"),LearnItConfig.getInt("max_regexp_pattern_words")))
				.build();
	}

	public BilingualDocTheoryInstanceLoader getDocTheoryLoader(Target target) {
		return new BilingualDocTheoryInstanceLoader(target, binaryObservers,unaryObservers);
	}

	public InstanceToSeedMapping getSeedInstanceData() {
		return seedRecorder.build();
	}

	public InstanceToPatternMapping getPatternData() {
		return observationRecorder.build();
	}

	public Mappings getInformationForScoring() {
		return new Mappings(getSeedInstanceData(), getPatternData());
	}

	public InstanceToMatchInfoDisplayMap getMatchInfoDisplayMap() {
		return matchDisplayRecorder.build();
	}
}
