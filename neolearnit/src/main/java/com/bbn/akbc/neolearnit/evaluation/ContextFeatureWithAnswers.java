package com.bbn.akbc.neolearnit.evaluation;

import java.util.HashSet;
import java.util.Set;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToAnswerMapping;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;
import com.bbn.akbc.neolearnit.observations.evaluation.EvalAnswer;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;

public class ContextFeatureWithAnswers implements Comparable<ContextFeatureWithAnswers> {

	private final LearnitPattern cf;
	private final Set<EvalAnswer> answers;

	private ContextFeatureWithAnswers(LearnitPattern cf, Set<EvalAnswer> answers) {
		this.cf = cf;
		this.answers = answers;
	}

	public static ContextFeatureWithAnswers from(LearnitPattern cf,
			InstanceToAnswerMapping ansMap, InstanceToPatternMapping cfMap) {

		Set<EvalAnswer> answers = new HashSet<EvalAnswer>();
		for (InstanceIdentifier id : cfMap.getInstances(cf)) {
			answers.add(ansMap.getAnswer(id));
		}
		return new ContextFeatureWithAnswers(cf,answers);
	}

	public double calculatePrecision() {
		int correct = 0;
		for (EvalAnswer ans : answers) {
			if (ans.isCorrect()) correct++;
		}
		return (double)correct/answers.size();
	}

	@Override
	public int compareTo(ContextFeatureWithAnswers arg0) {
		if (this.calculatePrecision() == arg0.calculatePrecision()) {
			return arg0.getAnswers().size() - this.getAnswers().size();
		} else {
			return -Double.compare(this.calculatePrecision(), arg0.calculatePrecision());
		}
	}

	public LearnitPattern getCf() {
		return cf;
	}

	public Set<EvalAnswer> getAnswers() {
		return answers;
	}

}
