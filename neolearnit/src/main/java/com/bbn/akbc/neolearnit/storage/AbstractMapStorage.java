package com.bbn.akbc.neolearnit.storage;

import com.google.common.collect.Multimap;

public abstract class AbstractMapStorage<T,U> implements MapStorage<T,U> {

	@Override
	public boolean hasLeft(T item) {
		return this.getLefts().contains(item);
	}

	@Override
	public boolean hasRight(U item) {
		return this.getRights().contains(item);
	}

	public abstract static class Builder<T,U> implements MapStorage.Builder<T, U> {

		@Override
		public synchronized void putAll(Multimap<T,U> multimap) {
			for (T item : multimap.keySet()) {
				for (U subitem : multimap.get(item)) {
					put(item, subitem);
				}
			}
		}

		@Override
		public synchronized void putAll(MapStorage<T,U> storage) {
			for (T item : storage.getLefts()) {
				for (U subitem : storage.getRight(item)) {
					put(item, subitem);
				}
			}
		}

	}

}
