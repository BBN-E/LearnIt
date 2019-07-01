package com.bbn.akbc.neolearnit.storage.structs;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EfficientMultimapDataStore<T,U> {
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

	public static <T,U> EfficientMultimapDataStore<T,U> fromMultimap(Multimap<T,U> multimap) {
		Map<Object,Integer> keyObjects = new HashMap<Object,Integer>();
		List<T> keyList = new ArrayList<T>();
		for (T o : multimap.keySet()) {
			keyObjects.put(o, keyList.size());
			keyList.add(o);
		}
		Map<Object,Integer> valObjects = new HashMap<Object,Integer>();
		List<U> valList = new ArrayList<U>();
		for (U o : multimap.values()) {
			if (!valObjects.containsKey(o)) {
				valObjects.put(o, valList.size());
				valList.add(o);
			}
		}
		List<MappedEntry> entries = new ArrayList<MappedEntry>();
		for (T key : multimap.keySet()) {
			entries.add(MappedEntry.fromEntry(key,multimap,keyObjects,valObjects));
		}
		return new EfficientMultimapDataStore<T,U>(keyList,valList,entries);
	}

	public Multimap<T,U> makeMultimap() {
		Multimap<T, U> storage = HashMultimap.create();
		for (MappedEntry entry : entries) {
			T key = keyList.get(entry.key);
			for (Integer value : entry.values) {
				storage.put(key, valList.get(value));
			}
		}
		return storage;
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
