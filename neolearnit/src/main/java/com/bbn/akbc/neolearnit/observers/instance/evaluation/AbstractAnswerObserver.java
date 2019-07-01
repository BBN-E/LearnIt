package com.bbn.akbc.neolearnit.observers.instance.evaluation;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.mappings.Recorder;
import com.bbn.akbc.neolearnit.observations.evaluation.EvalAnswer;
import com.bbn.akbc.neolearnit.observers.instance.AbstractInstanceIdObserver;

public abstract class AbstractAnswerObserver<T extends EvalAnswer> extends AbstractInstanceIdObserver<T> {

	protected AbstractAnswerObserver(Recorder<InstanceIdentifier, T> recorder) {
		super(recorder);
	}


}
