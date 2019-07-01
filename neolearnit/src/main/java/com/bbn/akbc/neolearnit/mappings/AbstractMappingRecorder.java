package com.bbn.akbc.neolearnit.mappings;

import com.bbn.akbc.neolearnit.storage.MapStorage;

public abstract class AbstractMappingRecorder<T,U> implements Recorder<T,U> {

	private final MapStorage.Builder<T,U> storage;

	public AbstractMappingRecorder(MapStorage.Builder<T,U> storage) {
		this.storage = storage;
	}

	@Override
	public void record(T item, U property) {
		storage.put(item, property);
	}

	protected MapStorage<T,U> buildMapping() {
		return storage.build();
	}

}
