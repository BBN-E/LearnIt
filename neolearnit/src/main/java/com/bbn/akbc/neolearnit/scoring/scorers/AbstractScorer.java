package com.bbn.akbc.neolearnit.scoring.scorers;

import com.bbn.akbc.neolearnit.logging.Glogger;
import com.bbn.akbc.neolearnit.observations.LearnItObservation;
import com.bbn.akbc.neolearnit.scoring.scores.BootstrappedScore;
import com.bbn.akbc.neolearnit.scoring.tables.AbstractScoreTable;
import com.google.common.collect.Iterables;

public abstract class AbstractScorer<T extends LearnItObservation, U extends BootstrappedScore<T>> implements Scorer<T,U> {

	public void score(Iterable<T> toScore, AbstractScoreTable<T,U> table) {
		Glogger.logger().debug("Scoring " + Iterables.size(toScore) + " objects...");
		int numScored = 0;
		for (T obj : toScore) {
			if (!table.getScoreOrDefault(obj).isFrozen()) {
				numScored++;
				score(obj,table);
				table.getScore(obj).unpropose();
			}
		}
	  Glogger.logger().debug("Scored " + numScored + " not frozen objects.");
	}

	protected static double round(double input) {
		final int DIGITS = 6;
		final int FACTOR = (int) Math.pow(10,DIGITS);
		return (double)Math.round(input*FACTOR)/FACTOR;
	}

	/**
	 * The core (online) scoring function
	 * @param object   the object to score
	 * @param currentScore  the object on which to set score information
	 */
	@Override
	public abstract void score(T object, AbstractScoreTable<T,U> table);

}
