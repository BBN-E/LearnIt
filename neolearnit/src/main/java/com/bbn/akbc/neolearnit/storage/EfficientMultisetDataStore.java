package com.bbn.akbc.neolearnit.storage;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Deprecated Use EfficientMultisetDataStore instead
 */

@Deprecated
public class EfficientMultisetDataStore<T> {
	@JsonProperty
	private final List<T> keyList;
	@JsonProperty
	private final List<MappedEntry> entries;

	@JsonCreator
	private EfficientMultisetDataStore(@JsonProperty("keyList") List<T> keyList,
			@JsonProperty("entries") List<MappedEntry> entries) {
		this.keyList = keyList;
		this.entries = entries;
	}

	public static <T> EfficientMultisetDataStore<T> fromMultiset(Multiset<T> multiset) {
		Map<Object,Integer> keyObjects = new HashMap<Object,Integer>();
		List<T> keyList = new ArrayList<T>();
		for (T o : multiset.elementSet()) {
			keyObjects.put(o, keyList.size());
			keyList.add(o);
		}
		List<MappedEntry> entries = new ArrayList<MappedEntry>();
		for (T key : multiset.elementSet()) {
			entries.add(MappedEntry.fromEntry(key,multiset,keyObjects));
		}
		return new EfficientMultisetDataStore<T>(keyList,entries);
	}

	public Multiset<T> makeMultiset() {
		Multiset<T> storage = HashMultiset.<T>create();
		for (MappedEntry entry : entries) {
			storage.add(keyList.get(entry.key), entry.count);
		}
		return storage;
	}

    @Deprecated
	public static class MappedEntry {
		@JsonProperty
		public Integer key;
		@JsonProperty
		public Integer count;
		@JsonCreator
		public MappedEntry(@JsonProperty("key") Integer key, @JsonProperty("count") Integer count) {
			this.key = key;
			this.count = count;
		}
		public static <T,U> MappedEntry fromEntry(T key, Multiset<T> multiset,
				Map<Object,Integer> keyLookup) {

			Integer intKey = keyLookup.get(key);
			return new MappedEntry(intKey, multiset.count(key));
		}
	}
}


