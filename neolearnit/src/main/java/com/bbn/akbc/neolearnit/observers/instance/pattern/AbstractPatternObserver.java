package com.bbn.akbc.neolearnit.observers.instance.pattern;

import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observers.instance.AbstractInstanceIdObserver;

public abstract class AbstractPatternObserver
		extends AbstractInstanceIdObserver<LearnitPattern> {

	protected AbstractPatternObserver(InstanceToPatternMapping.Builder recorder) {
		super(recorder);
	}

	@Override
	protected void record(MatchInfo instance, LearnitPattern property) {
		super.record(instance, property);
	}
}
