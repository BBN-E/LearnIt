package com.bbn.akbc.neolearnit.observations;

public abstract class LearnItObservation implements Comparable<LearnItObservation> {


	@Override
	public int hashCode() {
		throw new RuntimeException("Learnit Observations must implement" +
				" their own .hashCode and .equals and must not call super.hashCode()!");
	}

	@Override
	public boolean equals(Object obj) {
		throw new RuntimeException("Learnit Observations must implement" +
				" their own .hashCode and .equals and must not call super.equals()!");
	}

	@Override
	public String toString() {
		System.out.println("Warning: Learnit Observations does not have it's own" +
				" toString.");
		return super.toString();
	}

	public abstract String toIDString();

	public abstract String toPrettyString();

	@Override
	public int compareTo(LearnItObservation o) {
		return this.toIDString().compareTo(o.toIDString());
	}
}
