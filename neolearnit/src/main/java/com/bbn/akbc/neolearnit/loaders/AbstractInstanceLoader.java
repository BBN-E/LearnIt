package com.bbn.akbc.neolearnit.loaders;

import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.observers.InstanceObservers;
import com.bbn.akbc.neolearnit.observers.Observer;
import com.bbn.serif.theories.DocTheory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractInstanceLoader<T> implements Loader<T> {

	protected final InstanceObservers binaryObservers;
	protected final InstanceObservers unaryObservers;
	protected final boolean evaluating;
	protected final Map<String,DocTheory> docTheoryCache;

	public AbstractInstanceLoader(InstanceObservers binaryObservers, InstanceObservers unaryObservers,boolean evaluating) {
		this.binaryObservers = binaryObservers;
		this.unaryObservers = unaryObservers;
		this.evaluating = evaluating;
		this.docTheoryCache = new ConcurrentHashMap<String,DocTheory>();
	}

	public boolean isEvaluating() {
		return evaluating;
	}

	public void handleBinaryMatch(MatchInfo obj) {
		for (Observer<MatchInfo> o : binaryObservers) {
			o.observe(obj);
		}
	}
	public void handleUnaryMatch(MatchInfo obj) {
		for (Observer<MatchInfo> o : unaryObservers) {
			o.observe(obj);
		}
	}
	/**
	 * ONLY IF EVALUATING
	 * @return
	 */
	public Map<String,DocTheory> getLoadedDocTheories() {
		return docTheoryCache;
	}

}
