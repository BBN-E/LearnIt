package com.bbn.akbc.neolearnit.modules;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.loaders.impl.MonolingualDocTheoryInstanceLoader;
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


public class MonolingualExtractionModule {

	private final Injector recorderInjector;

	private final InstanceToSeedMapping.Builder seedRecorder;
	private final InstanceToPatternMapping.Builder observationRecorder;
	private final InstanceToMatchInfoDisplayMap.Builder matchDisplayRecorder;

  	// relation feature extraction
//  	private final InstanceToCommonRelationFeatureMapping.Builder commonRelationFeatureRecorder;

	private final InstanceObservers binaryObservers;
	private final InstanceObservers unaryObservers;

	public MonolingualExtractionModule() {
		this(false);
	}

	public MonolingualExtractionModule(boolean storeMatchInfoDisplay) {
		recorderInjector = Guice.createInjector(new RecorderModule());

		// mapping builders are recorders
		this.seedRecorder = recorderInjector.getInstance(InstanceToSeedMapping.Builder.class);
		this.observationRecorder = recorderInjector.getInstance(InstanceToPatternMapping.Builder.class);
		this.matchDisplayRecorder = recorderInjector.getInstance(InstanceToMatchInfoDisplayMap.Builder.class);

//		this.commonRelationFeatureRecorder = recorderInjector.getInstance(InstanceToCommonRelationFeatureMapping.Builder.class);

	  String primaryLanguage = LearnItConfig.getList("languages").get(0);
		// observers require recorders of the correct types
		InstanceObservers.Builder binaryObsBuilder = new InstanceObservers.Builder();
		InstanceObservers.Builder unaryObsBuilder = new InstanceObservers.Builder();

		if (storeMatchInfoDisplay) {
			binaryObsBuilder.withObserver(new MatchInfoDisplayObserver(matchDisplayRecorder));
            unaryObsBuilder.withObserver(new MatchInfoDisplayObserver(matchDisplayRecorder));
		}

		// SerifPattern Observer will share Instances

		SerifPatternObserver serifPatternObserver = new SerifPatternObserver(observationRecorder,primaryLanguage,LearnItConfig.getInt("max_unary_prop_depth"),LearnItConfig.getInt("max_binary_prop_depth"),LearnItConfig.getInt("max_regexp_pattern_words"));

		binaryObservers = binaryObsBuilder
			.withObserver(new SeedObserver(seedRecorder))
//			.withObserver(new PropObserver(observationRecorder, primaryLanguage, LearnItConfig.getInt("max_binary_prop_depth")))
//			.withObserver(new TextEntityTypeBetweenSlotsObserver(observationRecorder, primaryLanguage, LearnItConfig.getInt("max_regexp_pattern_words")))
//			.withObserver(new TextBeforeAfterSlotsObserver(observationRecorder, primaryLanguage, LearnItConfig.getInt("max_regexp_pattern_words")))
                //.withObserver(new SSUnigramObserver(observationRecorder, primaryLanguage))
//		    	.withObserver(new CommonRelationFeatureObserver(commonRelationFeatureRecorder))
				.withObserver(serifPatternObserver)
				.build();


        unaryObservers = unaryObsBuilder
                .withObserver(new SeedObserver(seedRecorder))
//                .withObserver(new UnaryHeadWordPOSTagObsever(observationRecorder, primaryLanguage))
//                .withObserver(new UnaryPropObserver(observationRecorder, primaryLanguage))
                // .withObserver(new ContextWindowObserver(observationRecorder, primaryLanguage))

                //.withObserver(new UnaryTypeObserver(observationRecorder, primaryLanguage))
//				.withObserver(new UnigramInSameSentence())
                //.withObserver(new BigramInSameSentence());
                //		.withObserver(new NameInSameSentence());
//				.withObserver(new EventMentionNounPhraseObserver(observationRecorder,primaryLanguage))
				.withObserver(serifPatternObserver)
				//.withObserver(new ComboPattern(UnaryPropObserver(), ContextWordsPattern()))
                .build();


//        secrury + one of unigrfood, secury + in, securyi i+ supply secury + china
//			secury + [food supply in china]
	}

	public MonolingualDocTheoryInstanceLoader getDocTheoryLoader(Target target) {
		return new MonolingualDocTheoryInstanceLoader(target, binaryObservers,unaryObservers);
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
