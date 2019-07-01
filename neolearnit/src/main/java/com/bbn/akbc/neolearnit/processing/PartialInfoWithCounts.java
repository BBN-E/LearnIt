package com.bbn.akbc.neolearnit.processing;

import com.bbn.akbc.neolearnit.observations.LearnItObservation;

public interface PartialInfoWithCounts<T extends LearnItObservation> extends PartialInformation {

	public int getCount(T obj);

}
