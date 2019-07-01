package com.bbn.akbc.neolearnit.storage.structs;

import com.bbn.bue.common.symbols.Symbol;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CondFreqDistDataStore<T> {
	@JsonProperty
	private final List<T> keyList; //seeds
	@JsonProperty
	private final List<String> valList; //patterns
	@JsonProperty
	private final List<MappedEntry> entries;

	@JsonCreator
	private CondFreqDistDataStore(
			@JsonProperty("keyList") List<T> keyList,
			@JsonProperty("valList") List<String> valList,
			@JsonProperty("entries") List<MappedEntry> entries) {
		this.keyList = keyList;
		this.valList = valList;
		this.entries = entries;
	}

	public static <T> CondFreqDistDataStore<T> fromMapOfMultiset(Map<T,Multiset<Symbol>> map) {
		Map<Object,Integer> keyObjects = new HashMap<Object,Integer>();
		List<T> keyList = new ArrayList<T>();
		for (T o : map.keySet()) {
			keyObjects.put(o, keyList.size());
			keyList.add(o);
		}
		Map<String,Integer> valObjects = new HashMap<String,Integer>();
		List<String> valList = new ArrayList<String>();
		for (Multiset<Symbol> ms : map.values()) {
			for (Symbol so : ms.elementSet()) {
				String o = so.toString();
				if (!valObjects.containsKey(o)) {
					valObjects.put(o, valList.size());
					valList.add(o);
				}
			}

		}
		List<MappedEntry> entries = new ArrayList<MappedEntry>();
		for (T key : map.keySet()) {
			for (Symbol val : map.get(key).elementSet()) {
				entries.add(MappedEntry.fromEntry(key,val,map,keyObjects,valObjects));
			}
		}
		return new CondFreqDistDataStore<T>(keyList,valList,entries);
	}

	public static <T> CondFreqDistDataStore<T> fromMapOfIntegerMultiset(Map<T,Multiset<Integer>> map) {
		Map<Object,Integer> keyObjects = new HashMap<Object,Integer>();
		List<T> keyList = new ArrayList<T>();
		for (T o : map.keySet()) {
			keyObjects.put(o, keyList.size());
			keyList.add(o);
		}
		List<MappedEntry> entries = new ArrayList<MappedEntry>();
		for (T key : map.keySet()) {
			for (Integer val : map.get(key).elementSet()) {
				entries.add(MappedEntry.fromEntry(key,val,map,keyObjects));
			}
		}
		return new CondFreqDistDataStore<T>(keyList,Lists.<String>newArrayList(),entries);
	}

	public Map<T,Multiset<Symbol>> makeMapMultiset() {
		Map<T,Multiset<Symbol>> storage = new HashMap<T,Multiset<Symbol>>();
		for (MappedEntry entry : entries) {
			T key = keyList.get(entry.key);
			Symbol val = Symbol.from(valList.get(entry.value));
			if (!storage.containsKey(key)) {
				storage.put(key, HashMultiset.<Symbol>create());
			}
			storage.get(key).add(val);
		}
		return storage;
	}

	public Map<T,Multiset<Integer>> makeMapIntegerMultiset() {
		Map<T,Multiset<Integer>> storage = new HashMap<T,Multiset<Integer>>();
		for (MappedEntry entry : entries) {
			T key = keyList.get(entry.key);
			if (!storage.containsKey(key)) {
				storage.put(key, HashMultiset.<Integer>create());
			}

			storage.get(key).add(entry.value, entry.count);
		}
		return storage;
	}


	public static class MappedEntry {
		@JsonProperty
		public Integer key; // seed
		@JsonProperty
		public Integer value; // pattern
		@JsonProperty
		public Integer count; // number of times pattern matches seed
		@JsonCreator
		public MappedEntry(@JsonProperty("key") Integer key,
						@JsonProperty("value") Integer value,
						@JsonProperty("count") Integer count) {
			this.key = key;
			this.value = value;
			this.count = count;
		}
		public static <T> MappedEntry fromEntry(T key, Symbol val, Map<T,Multiset<Symbol>> map,
				Map<Object,Integer> keyLookup, Map<String,Integer> valLookup) {

			Integer intKey = keyLookup.get(key);
			Integer intVal = valLookup.get(val.toString());
			return new MappedEntry(intKey, intVal, map.get(key).count(val));
		}
		public static <T> MappedEntry fromEntry(T key, Integer intVal, Map<T,Multiset<Integer>> map,
				Map<Object,Integer> keyLookup) {

			Integer intKey = keyLookup.get(key);
			return new MappedEntry(intKey, intVal, map.get(key).count(intVal));
		}
	}
}


