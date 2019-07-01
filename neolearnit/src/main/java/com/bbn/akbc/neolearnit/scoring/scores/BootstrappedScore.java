package com.bbn.akbc.neolearnit.scoring.scores;

import com.bbn.akbc.neolearnit.observations.LearnItObservation;

public interface BootstrappedScore<T extends LearnItObservation> extends Comparable<BootstrappedScore<T>> {

	/**
	 * unfreezes an object that was previously frozen, this is only done by humans during initialization
	 */
	public void unfreeze();

	/**
	 * freezes an object and saves the iteration at which it was frozen
	 * @param iteration   the iteration at which you are freezing it
	 */
	public void freezeScore(int iteration);

	/**
	 * whether or not the object is currently frozen
	 * @return
	 */
	public boolean isFrozen();

	/**
	 * whether the score/precision of the object is high enough for it to be considered "good"
	 * Only useful in the presence of negative data
	 * @return
	 */
	public boolean isGood();

	/**
	 * Unsets an object that was proposed, by default new objects are proposed,
	 * this clears that state
	 */
	public void unpropose();

	/**
	 * Sets an object as proposed, by default new objects are proposed.
	 */
	public void propose();

	/**
	 * Whether or not the object is currently set as proposed
	 * @return
	 */
	public boolean isProposed();

	/**
	 * Gets the recorded frequency of the object across the entire corpus
	 * @return
	 */
	public Integer getFrequency();

	/**
	 * Gets the iteration at which the object was created
	 * @return
	 */
	public Integer getIteration();

	/**
	 * Gets the iteration at which the object was frozen
	 * -1 if the object has not been frozen yet
	 * @return
	 */
	public Integer getFrozenIteration();

	/**
	 * Gets the confidence value of this object
	 * @return
	 */
	public Double getConfidence();

	/**
	 * Gets the score for seeds and precision for patterns
	 * @return
	 */
	public Double getScoreOrPrecision();
}
