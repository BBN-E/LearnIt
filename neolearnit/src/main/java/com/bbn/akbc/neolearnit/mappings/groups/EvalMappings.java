package com.bbn.akbc.neolearnit.mappings.groups;

import com.bbn.akbc.neolearnit.mappings.impl.InstanceToAnswerMapping;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToMatchInfoMap;

/**
 * This is the collection of mappings available for evaluation.
 *
 * @author mshafir
 *
 */
public class EvalMappings {

	private final InstanceToAnswerMapping instance2Answer;
	private final InstanceToPatternMapping instance2Pattern;
	private final InstanceToMatchInfoMap instance2MatchInfo;

	public EvalMappings(InstanceToAnswerMapping instance2Answer,
			InstanceToPatternMapping instance2Pattern,
			InstanceToMatchInfoMap instance2Match) {
		this.instance2Answer = instance2Answer;
		this.instance2Pattern = instance2Pattern;
		this.instance2MatchInfo = instance2Match;
	}

	public InstanceToAnswerMapping getInstance2Answer() {
		return instance2Answer;
	}

	public InstanceToPatternMapping getInstance2Pattern() {
		return instance2Pattern;
	}

	public InstanceToMatchInfoMap getInstance2MatchInfo() {
		return instance2MatchInfo;
	}



}
