package com.bbn.akbc.neolearnit.mappings.filters.storage;

import com.bbn.akbc.neolearnit.storage.MapStorage;

public interface StorageFilter<T,U> {

	public MapStorage<T,U> filter(MapStorage<T,U> input);

}
