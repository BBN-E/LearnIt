package com.bbn.akbc.neolearnit.mappings;

import com.bbn.akbc.neolearnit.storage.MapStorage;


public abstract class AbstractMapping<T,U> {

	protected final MapStorage<T,U> storage;

	public AbstractMapping(MapStorage<T,U> storage) {
		this.storage = storage;
	}

	public MapStorage<T,U> getStorage() {
		return storage;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		AbstractMapping<?, ?> that = (AbstractMapping<?, ?>) o;

		return storage != null ? storage.equals(that.storage) : that.storage == null;
	}

	@Override
	public int hashCode() {
		return storage != null ? storage.hashCode() : 0;
	}
}
