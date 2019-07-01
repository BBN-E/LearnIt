package com.bbn.akbc.neolearnit.storage;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Deprecated Use EfficientMapDataStore instead
 */

@Deprecated
public class EfficientMapDataStore<T,U> {
	@JsonProperty
	private final List<T> keyList;
	@JsonProperty
	private final List<U> valList;
	@JsonProperty
	private final List<MappedEntry> entries;

	@JsonCreator
	private EfficientMapDataStore(@JsonProperty("keyList") List<T> keyList,
			@JsonProperty("valList") List<U> valList,
			@JsonProperty("entries") List<MappedEntry> entries) {
		this.keyList = keyList;
		this.valList = valList;
		this.entries = entries;
	}

	public static <T,U> EfficientMapDataStore<T,U> fromMap(Map<T,U> map) {
		Map<Object,Integer> keyObjects = new HashMap<Object,Integer>();
		List<T> keyList = new ArrayList<T>();
		for (T o : map.keySet()) {
			keyObjects.put(o, keyList.size());
			keyList.add(o);
		}
		Map<Object,Integer> valObjects = new HashMap<Object,Integer>();
		List<U> valList = new ArrayList<U>();
		for (U o : map.values()) {
			valObjects.put(o, valList.size());
			valList.add(o);
		}
		List<MappedEntry> entries = new ArrayList<MappedEntry>();
		for (T key : map.keySet()) {
			entries.add(MappedEntry.fromEntry(key,map,keyObjects,valObjects));
		}
		return new EfficientMapDataStore<T,U>(keyList,valList,entries);
	}

	public Map<T,U> makeMap() {
		Map<T, U> storage = new HashMap<T, U>();
		for (MappedEntry entry : entries) {
			storage.put(keyList.get(entry.key), valList.get(entry.value));
		}
		return storage;
	}

    @Deprecated
	public static class MappedEntry {
		@JsonProperty
		public Integer key;
		@JsonProperty
		public Integer value;
		@JsonCreator
		public MappedEntry(@JsonProperty("key") Integer key, @JsonProperty("value") Integer value) {
			this.key = key;
			this.value = value;
		}
		public static <T,U> MappedEntry fromEntry(T key, Map<T,U> map,
				Map<Object,Integer> keyLookup, Map<Object,Integer> valLookup) {

			Integer intKey = keyLookup.get(key);
			return new MappedEntry(intKey, valLookup.get(map.get(key)));
		}
	}
}


