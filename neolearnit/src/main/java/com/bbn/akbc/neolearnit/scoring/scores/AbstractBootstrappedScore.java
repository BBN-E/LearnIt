package com.bbn.akbc.neolearnit.scoring.scores;

import com.bbn.akbc.neolearnit.observations.LearnItObservation;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public abstract class AbstractBootstrappedScore<T extends LearnItObservation> implements BootstrappedScore<T>{

	@JsonProperty
	private boolean frozen;
	@JsonProperty
	private boolean proposed;
	@JsonProperty
	private int iteration;
	@JsonProperty
	private int frozenIteration;

	protected AbstractBootstrappedScore(boolean frozen, boolean proposed, int iteration, int frozenIteration) {
		this.frozen = frozen;
		this.proposed = proposed;
		this.iteration = iteration;
		this.frozenIteration = frozenIteration;
	}

	public AbstractBootstrappedScore() {
		frozen = false;
	}

	@Override
	public Integer getIteration() {
		return iteration;
	}

	@Override
	public Integer getFrozenIteration() {
		return frozenIteration;
	}

	@Override
	public void freezeScore(int iteration) {
		this.frozen = true;
		this.frozenIteration = iteration;
		this.proposed = false;
	}

	@Override
	public void unfreeze() {
//		System.out.println("Warning: only a human should be unfreezing seeds");
		this.frozen = false;
	}

	@Override
	public boolean isFrozen() {
		return frozen;
	}

	@Override
	public void unpropose() {
		this.proposed = false;
	}

	@Override
	public void propose() {
		this.proposed = true;
	}

	@Override
	public boolean isProposed() {
		return proposed;
	}

	protected void checkNotFrozen() {
		if (frozen) {
			throw new RuntimeException("Error: tried to modify score for a frozen element.");
		}
	}

	@Override
	public String toString() {
		String freezeIt = frozen ? ":"+frozenIteration : ":";
		return "[frozen=" + frozen + ", proposed="
				+ proposed + ", iterations=" + iteration + freezeIt + "]";
	}
}
