package com.bbn.akbc.neolearnit.evaluation;

import java.util.Set;

import com.bbn.akbc.neolearnit.observations.evaluation.EvalAnswer;
import com.google.common.collect.Sets;

public class PrecisionRecallScore {

	private final double precision;
	private final double recall;

	private PrecisionRecallScore(double precision, double recall) {
		this.precision = precision;
		this.recall = recall;
	}

	public static PrecisionRecallScore from(Set<EvalAnswer> system, Set<EvalAnswer> gold) {
		int correct = Sets.intersection(system, gold).size();
		double p = (double)correct/system.size();
		double r = (double)correct/gold.size();
		return new PrecisionRecallScore(p,r);
	}


	public double getPrecision() {
		return precision;
	}

	public double getRecall() {
		return recall;
	}

	public double getF1() {
		if (precision == 0 || recall == 0) return 0;
		return 2.0*precision*recall/(precision+recall);
	}

	@Override
	public String toString() {
		return "[precision=" + precision + ", recall="+ recall + ", f1=" + getF1() + "]";
	}

}
