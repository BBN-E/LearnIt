package com.bbn.akbc.neolearnit.mappings;

public interface Recorder<T,U> {

	public void record(T item, U property);

}
