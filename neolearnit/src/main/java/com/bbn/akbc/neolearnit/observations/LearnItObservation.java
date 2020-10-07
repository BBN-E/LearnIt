package com.bbn.akbc.neolearnit.observations;

import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class LearnItObservation implements Comparable<LearnItObservation> {


	@Override
    public abstract int hashCode();

	@Override
    public abstract boolean equals(Object obj);
	@Override
    public abstract String toString();

	public abstract String toIDString();

	public abstract String toPrettyString();

	@Override
	public int compareTo(LearnItObservation o) {
		return this.toIDString().compareTo(o.toIDString());
	}
}
