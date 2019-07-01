package com.bbn.akbc.neolearnit.scoring.selectors;

import java.util.Set;

import com.bbn.akbc.neolearnit.observations.LearnItObservation;
import com.bbn.akbc.neolearnit.scoring.scores.BootstrappedScore;
import com.bbn.akbc.neolearnit.scoring.tables.AbstractScoreTable;

public interface Selector<T extends LearnItObservation, U extends BootstrappedScore<T>, V extends AbstractScoreTable<T,U>> {

	/**
	 * Freezes some scores in the given score table
	 * @param scores
	 * @return  the objects frozen as a result of this call
	 */
	public Set<T> freezeScores(V scores);

}
