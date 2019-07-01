package com.bbn.akbc.neolearnit.scoring.scorers;

import com.bbn.akbc.neolearnit.observations.LearnItObservation;
import com.bbn.akbc.neolearnit.scoring.scores.BootstrappedScore;
import com.bbn.akbc.neolearnit.scoring.tables.AbstractScoreTable;

public interface Scorer<T extends LearnItObservation, U extends BootstrappedScore<T>> {

	/**
	 * The core scoring function
	 * @param object   the object to score
	 * @param table    a table of objects on which to set score information
	 */
	public void score(T object, AbstractScoreTable<T,U> table);





}
