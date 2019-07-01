package com.bbn.akbc.neolearnit.modules;

import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.evaluation.answers.AnswerCollection;
import com.bbn.akbc.neolearnit.loaders.impl.BilingualDocTheoryInstanceLoader;
import com.bbn.akbc.neolearnit.loaders.impl.MonolingualDocTheoryInstanceLoader;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToAnswerMapping;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToMatchInfoDisplayMap;
import com.bbn.akbc.neolearnit.observers.InstanceObservers;
import com.bbn.akbc.neolearnit.observers.instance.evaluation.SentenceEntityPairAnswerObserver;
import com.bbn.akbc.neolearnit.observers.instance.matchinfo.MatchInfoDisplayObserver;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class PrematchedEvaluationModule {

	private final Injector recorderInjectorSysAns;
	private final Injector recorderInjectorGoldAns;

	private final InstanceToAnswerMapping.Builder systemAnswerRecorder;
	private final InstanceToAnswerMapping.Builder goldAnswerRecorder;
	private final InstanceToMatchInfoDisplayMap.Builder matchInfoRecorder;
	private final InstanceObservers binaryObservers;
	private final InstanceObservers unaryObservers;

	public PrematchedEvaluationModule(AnswerCollection systemAnswers, AnswerCollection goldAnswers) {
		recorderInjectorSysAns = Guice.createInjector(new RecorderModule());
		recorderInjectorGoldAns = Guice.createInjector(new RecorderModule());

		// mapping builders are recorders
		this.systemAnswerRecorder = recorderInjectorSysAns.getInstance(InstanceToAnswerMapping.Builder.class);
		this.goldAnswerRecorder = recorderInjectorGoldAns.getInstance(InstanceToAnswerMapping.Builder.class);
		this.matchInfoRecorder = recorderInjectorSysAns.getInstance(InstanceToMatchInfoDisplayMap.Builder.class);

		// binaryObservers require recorders of the correct types
		InstanceObservers.Builder obsBuilder = new InstanceObservers.Builder()
			.withObserver(new SentenceEntityPairAnswerObserver(systemAnswerRecorder,systemAnswers,true))
			.withObserver(new SentenceEntityPairAnswerObserver(goldAnswerRecorder,goldAnswers))
			.withObserver(new MatchInfoDisplayObserver(matchInfoRecorder));

		this.binaryObservers = obsBuilder.build();
		this.unaryObservers = new InstanceObservers.Builder().build();
	}

	public MonolingualDocTheoryInstanceLoader getDocTheoryLoader(Target target) {
		return new MonolingualDocTheoryInstanceLoader(target, binaryObservers,unaryObservers);
	}

	public BilingualDocTheoryInstanceLoader getBilingualDocTheoryLoader(Target target) {
		return new BilingualDocTheoryInstanceLoader(target, binaryObservers,unaryObservers);
	}

	public InstanceToAnswerMapping getSystemAnswerMapping() {
		return systemAnswerRecorder.build();
	}

	public InstanceToAnswerMapping getGoldAnswerMapping() {
		return goldAnswerRecorder.build();
	}

	public InstanceToMatchInfoDisplayMap getMatchInfoMap() {
		return matchInfoRecorder.build();
	}

}
