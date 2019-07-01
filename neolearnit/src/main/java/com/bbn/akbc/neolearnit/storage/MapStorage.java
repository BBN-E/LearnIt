package com.bbn.akbc.neolearnit.storage;

import java.util.Collection;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;

/**
 * A bidirectional mapping that we plan to use throughout learnit
 *
 * this faciltates creation of different storage mechanisms
 *   - hash maps
 *   - bloom filters
 *   - database
 *
 * @author mshafir
 *
 */
public interface MapStorage<T,U> {

	public boolean hasLeft(T item);

	public boolean hasRight(U item);

	public Multiset<T> getLefts();

	public Multiset<U> getRights();

	public Collection<U> getRight(T item);

	public Collection<T> getLeft(U item);

	public int size();

	public Builder<T,U> newBuilder();

	public static interface Builder<T,U> {

		public void put(T left, U right);

		public void putAll(Multimap<T,U> multimap);

		public void putAll(MapStorage<T,U> storage);

		public MapStorage<T,U> build();

	}
}
