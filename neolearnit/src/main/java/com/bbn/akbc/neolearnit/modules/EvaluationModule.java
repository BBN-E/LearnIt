package com.bbn.akbc.neolearnit.modules;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.evaluation.answers.AnswerCollection;
import com.bbn.akbc.neolearnit.loaders.impl.BilingualDocTheoryInstanceLoader;
import com.bbn.akbc.neolearnit.loaders.impl.MonolingualDocTheoryInstanceLoader;
import com.bbn.akbc.neolearnit.mappings.groups.EvalReportMappings;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToAnswerMapping;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToCommonRelationFeatureMapping;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToMatchInfoDisplayMap;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToMatchInfoMap;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToSeedMapping;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observers.InstanceObservers;
import com.bbn.akbc.neolearnit.observers.instance.CommonRelationFeatureObserver;
import com.bbn.akbc.neolearnit.observers.instance.SeedObserver;
import com.bbn.akbc.neolearnit.observers.instance.evaluation.SentenceEntityPairAnswerObserver;
import com.bbn.akbc.neolearnit.observers.instance.matchinfo.MatchInfoDisplayObserver;
import com.bbn.akbc.neolearnit.observers.instance.matchinfo.MatchInfoObserver;
import com.bbn.akbc.neolearnit.observers.instance.pattern.Binary.PropObserver;
import com.bbn.akbc.neolearnit.observers.instance.pattern.Binary.TextBeforeAfterSlotsObserver;
import com.bbn.akbc.neolearnit.observers.instance.pattern.Binary.TextEntityTypeBetweenSlotsObserver;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.storage.MapStorage.Builder;

import com.google.inject.Guice;
import com.google.inject.Injector;


public class EvaluationModule {

	private final Injector recorderInjector;

	private final AnswerCollection answers;

	private final InstanceToSeedMapping.Builder seedRecorder;
	private final InstanceToAnswerMapping.Builder answerRecorder;
	private final InstanceToPatternMapping.Builder observationRecorder;
	private final InstanceToMatchInfoDisplayMap.Builder matchInfoDisplayRecorder;
	private final InstanceToMatchInfoMap.Builder matchInfoRecorder;

//	private final InstanceToLexFeatureSetMapping.Builder lexFeatureSetRecorder;
  	private InstanceToCommonRelationFeatureMapping.Builder commonRelationFeatureRecorder;

	private final InstanceObservers binaryObservers;
	private final InstanceObservers unaryObservers;

	public EvaluationModule(AnswerCollection answers, boolean bilingual) {
		recorderInjector = Guice.createInjector(new RecorderModule());

		this.answers = answers;
		// mapping builders are recorders
		this.seedRecorder = recorderInjector.getInstance(InstanceToSeedMapping.Builder.class);
		this.answerRecorder = recorderInjector.getInstance(InstanceToAnswerMapping.Builder.class);
		this.observationRecorder = recorderInjector.getInstance(InstanceToPatternMapping.Builder.class);
		this.matchInfoDisplayRecorder = recorderInjector.getInstance(InstanceToMatchInfoDisplayMap.Builder.class);
		this.matchInfoRecorder = recorderInjector.getInstance(InstanceToMatchInfoMap.Builder.class);

//		this.lexFeatureSetRecorder = recorderInjector.getInstance(InstanceToLexFeatureSetMapping.Builder.class);

//	  	if(LearnItConfig.optionalParamTrue("DoFeatureExtraction"))
//		  this.commonRelationFeatureRecorder = recorderInjector.getInstance(InstanceToCommonRelationFeatureMapping.Builder.class);

		String primaryLanguage = LearnItConfig.getList("languages").get(0);
		// observers require recorders of the correct types
        InstanceObservers.Builder unaryObsBuilder = new InstanceObservers.Builder();
		InstanceObservers.Builder binaryObsBuilder = new InstanceObservers.Builder()
			.withObserver(new SeedObserver(seedRecorder))
			.withObserver(new SentenceEntityPairAnswerObserver(answerRecorder, answers,
			    LearnItConfig.getInt("partial_match_allowance")))
			.withObserver(new PropObserver(observationRecorder, primaryLanguage, LearnItConfig.getInt("max_prop_depth")))
			.withObserver(new TextEntityTypeBetweenSlotsObserver(observationRecorder, primaryLanguage, LearnItConfig.getInt("max_regexp_pattern_words")))
			.withObserver(new TextBeforeAfterSlotsObserver(observationRecorder, primaryLanguage, LearnItConfig.getInt("max_regexp_pattern_words")))
			.withObserver(new MatchInfoDisplayObserver(matchInfoDisplayRecorder))
			.withObserver(new MatchInfoObserver(matchInfoRecorder))
			// .withObserver(new LexFeatureSetObserver(lexFeatureSetRecorder)
//		    	.withObserver(new CommonRelationFeatureObserver(commonRelationFeatureRecorder))
			;

	  if(LearnItConfig.optionalParamTrue("DoFeatureExtraction")) {
	    this.commonRelationFeatureRecorder = recorderInjector.getInstance(InstanceToCommonRelationFeatureMapping.Builder.class);
	    binaryObsBuilder.withObserver(new CommonRelationFeatureObserver(commonRelationFeatureRecorder));
	  }


	    if (bilingual) {
			String secondaryLanguage = LearnItConfig.getList("languages").get(1);

			binaryObsBuilder.withObserver(new PropObserver(observationRecorder, secondaryLanguage, LearnItConfig.getInt("max_prop_depth")))
					  .withObserver(new TextEntityTypeBetweenSlotsObserver(observationRecorder, secondaryLanguage, LearnItConfig.getInt("max_regexp_pattern_words")))
					  .withObserver(new TextBeforeAfterSlotsObserver(observationRecorder, secondaryLanguage, LearnItConfig.getInt("max_regexp_pattern_words")));
		}
		this.binaryObservers = binaryObsBuilder.build();
        this.unaryObservers = unaryObsBuilder.build();
	}

	public AnswerCollection getAnswers() {
		return answers;
	}

	public MonolingualDocTheoryInstanceLoader getDocTheoryLoader(Target target) {
		return new MonolingualDocTheoryInstanceLoader(target, binaryObservers, unaryObservers,true);
	}

	public BilingualDocTheoryInstanceLoader getBilingualDocTheoryLoader(Target target) {
		return new BilingualDocTheoryInstanceLoader(target, binaryObservers,unaryObservers, true);
	}

	public EvalReportMappings getEvalMappings(TargetAndScoreTables data) {
		InstanceToPatternMapping patternMapping = observationRecorder.build();

		//normalize for symmetry during evaluation
		if (data.getTarget().isSymmetric()) {
			Builder<InstanceIdentifier, LearnitPattern> newPatBuilder = patternMapping.getStorage().newBuilder();
			for (LearnitPattern pattern : patternMapping.getAllPatterns().elementSet()) {
				if (pattern.isInCanonicalSymmetryOrder()) {
					for (InstanceIdentifier id : patternMapping.getInstances(pattern)) {
						newPatBuilder.put(id, pattern);
					}
				}
			}
			patternMapping = new InstanceToPatternMapping(newPatBuilder.build());
		}

		//we have to add in restrictions at this point
		InstanceToSeedMapping seedMapping = seedRecorder.build();

	  if(LearnItConfig.optionalParamTrue("DoFeatureExtraction")) {
	    return new EvalReportMappings(data,
		new Mappings(seedMapping.getStorage(), patternMapping.getStorage())
		    .getAllPatternUpdatedMappings(data)
		    .makeWithoutIncompletePatterns(),
		answerRecorder.build(), matchInfoDisplayRecorder.build(),
		// lexFeatureSetRecorder.build()
		commonRelationFeatureRecorder.build()
	    );
	  }
	  else {
	    return new EvalReportMappings(data,
		new Mappings(seedMapping.getStorage(), patternMapping.getStorage())
		    .getAllPatternUpdatedMappings(data)
		    .makeWithoutIncompletePatterns(),
		answerRecorder.build(), matchInfoDisplayRecorder.build()
	    );
	  }
	}

	public InstanceToMatchInfoMap getMatchInfoMap() {
		return matchInfoRecorder.build();
	}

}
