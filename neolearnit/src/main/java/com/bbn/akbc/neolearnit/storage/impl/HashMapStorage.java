package com.bbn.akbc.neolearnit.storage.impl;

import com.bbn.akbc.neolearnit.storage.AbstractMapStorage;
import com.bbn.akbc.neolearnit.storage.MapStorage;
import com.bbn.akbc.neolearnit.storage.impl.HashMapStorage.EfficientMultimapDataStore.MappedEntry;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HashMapStorage<T,U> extends AbstractMapStorage<T,U> {

	private final Multimap<T,U> map;
	private final Multimap<U,T> inverseMap;

	@JsonProperty
	private final EfficientMultimapDataStore<T,U> data() {
		return EfficientMultimapDataStore.fromMultimap(map,inverseMap.keySet());
	}

	/**
	 * NOTE: this hasn't been updated to use the Data Store in storage.structs
	 * because we already generated all the mappings and regenerating them would
	 * be a pain
	 *
	 * @param <T>
	 * @param <U>
	 */
	public static class EfficientMultimapDataStore<T,U> {
		@JsonProperty
		private final List<T> keyList;
		@JsonProperty
		private final List<U> valList;
		@JsonProperty
		private final List<MappedEntry> entries;

		@JsonCreator
		private EfficientMultimapDataStore(@JsonProperty("keyList") List<T> keyList,
				@JsonProperty("valList") List<U> valList,
				@JsonProperty("entries") List<MappedEntry> entries) {
			this.keyList = keyList;
			this.valList = valList;
			this.entries = entries;
		}

		public static <T,U> EfficientMultimapDataStore<T,U> fromMultimap(Multimap<T,U> multimap, Set<U> uniqueUs) {
			Map<Object,Integer> keyObjects = new HashMap<Object,Integer>();
			List<T> keyList = new ArrayList<T>();
			for (T o : multimap.keySet()) {
				keyObjects.put(o, keyList.size());
				keyList.add(o);
			}
			Map<Object,Integer> valObjects = new HashMap<Object,Integer>();
			List<U> valList = new ArrayList<U>();
			for (U o : uniqueUs) {
				valObjects.put(o, valList.size());
				valList.add(o);
			}
			List<MappedEntry> entries = new ArrayList<MappedEntry>();
			for (T key : multimap.keySet()) {
				entries.add(MappedEntry.fromEntry(key,multimap,keyObjects,valObjects));
			}
			return new EfficientMultimapDataStore<T,U>(keyList,valList,entries);
		}


		public static class MappedEntry {
			@JsonProperty
			public Integer key;
			@JsonProperty
			public List<Integer> values;
			@JsonCreator
			public MappedEntry(@JsonProperty("key") Integer key, @JsonProperty("values") List<Integer> values) {
				this.key = key;
				this.values = values;
			}
			public static <T,U> MappedEntry fromEntry(T key, Multimap<T,U> multimap,
					Map<Object,Integer> keyLookup, Map<Object,Integer> valLookup) {

				Integer intKey = keyLookup.get(key);
				List<Integer> values = new ArrayList<Integer>();
				for (U val : multimap.get(key)) {
					values.add(valLookup.get(val));
				}
				return new MappedEntry(intKey, values);
			}
		}
	}

	private HashMapStorage(Multimap<T,U> map, Multimap<U,T> inverseMap) {
		this.map = map;
		this.inverseMap = inverseMap;
	}

	@JsonCreator
	private static <T,U> HashMapStorage<T,U> makeMapStorage(
			@JsonProperty("data") EfficientMultimapDataStore<T,U> data) {

		HashMapStorage.Builder<T, U> builder = new HashMapStorage.Builder<T, U>();
		for (MappedEntry entry : data.entries) {
			for (Integer val : entry.values) {
				builder.put(data.keyList.get(entry.key), data.valList.get(val));
			}
		}
		return builder.build();
	}

	@Override
	public Multiset<T> getLefts() {
		return map.keys();
	}

	@Override
	public Multiset<U> getRights() {
		return inverseMap.keys();
	}

	@Override
	public Collection<U> getRight(T item) {
		return map.get(item);
	}

	@Override
	public Collection<T> getLeft(U item) {
		return inverseMap.get(item);
	}

	@Override
	public MapStorage.Builder<T, U> newBuilder() {
		return new Builder<T,U>();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((map == null) ? 0 : map.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		HashMapStorage other = (HashMapStorage) obj;
		if (map == null) {
			if (other.map != null)
				return false;
		} else if (!map.equals(other.map))
			return false;
		return true;
	}

	@Override
	public int size() {
		return this.map.entries().size()+this.inverseMap.entries().size();
	}

	public static class Builder<T,U> extends AbstractMapStorage.Builder<T, U> {

		private final HashMultimap<T,U> map;
		private final HashMultimap<U,T> inverseMap;

		@Inject
		public Builder() {
			map = HashMultimap.create();
			inverseMap = HashMultimap.create();
		}

		@Override
		public synchronized void put(T left, U right) {
			if (map.containsEntry(left, right)) return;
			map.put(left,right);
			inverseMap.put(right,left);
		}

		// for creating Indexer
		public synchronized Optional<Set<U>> containsLeft(T left) {
			if (map.containsKey(left)) return Optional.of(map.get(left));
			else
				return Optional.absent();
		}

		@Override
		public HashMapStorage<T, U> build() {
			return new HashMapStorage<T,U>(map, inverseMap);
		}
	}
}
