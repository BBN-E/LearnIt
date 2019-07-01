package com.bbn.akbc.neolearnit.scoring.proposers;

import com.bbn.akbc.neolearnit.observations.LearnItObservation;

public interface Proposer<T extends LearnItObservation> {

	public Iterable<T> propose(Iterable<T> potentials, int amount);

}
