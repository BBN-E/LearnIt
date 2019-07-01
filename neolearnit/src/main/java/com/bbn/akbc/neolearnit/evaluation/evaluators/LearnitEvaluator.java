package com.bbn.akbc.neolearnit.evaluation.evaluators;

import java.io.IOException;

import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;

public interface LearnitEvaluator {

	public void evaluate(TargetAndScoreTables data) throws IOException;

}
