package com.bbn.akbc.neolearnit.mappings.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfoDisplay;
import com.bbn.akbc.neolearnit.mappings.Recorder;
import com.bbn.akbc.neolearnit.storage.EfficientMapDataStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

/**
 * This one is a little different from the others so I called it just a map
 * it's a one-way map from InstanceId to MatchInfo. MatchInfo isn't hashable
 * and we don't really need to store it every which way
 * @author mshafir
 *
 */
public class InstanceToMatchInfoDisplayMap {

	private final Map<InstanceIdentifier,MatchInfoDisplay> map;

	@JsonProperty
	private EfficientMapDataStore<InstanceIdentifier,MatchInfoDisplay> map() {
		return EfficientMapDataStore.fromMap(map);
	}

	@JsonCreator
	private InstanceToMatchInfoDisplayMap(@JsonProperty("map") EfficientMapDataStore<InstanceIdentifier,MatchInfoDisplay> map) {
		this.map = map.makeMap();
	}

	public InstanceToMatchInfoDisplayMap(Map<InstanceIdentifier,MatchInfoDisplay> map) {
		this.map = ImmutableMap.copyOf(map);
	}

	public MatchInfoDisplay getMatchInfoDisplay(InstanceIdentifier instanceId) {
		return map.get(instanceId);
	}

	public Collection<InstanceIdentifier> instances() {
		return map.keySet();
	}

	public Collection<MatchInfoDisplay> values() {
		return map.values();
	}

	public InstanceToMatchInfoDisplayMap makeFitlered(Set<InstanceIdentifier> ids) {
		Builder result = new Builder();
		for (InstanceIdentifier id : map.keySet()) {
			if (ids.contains(id)) {
				result.record(id, map.get(id));
			}
		}
		return result.build();
	}

	public static class Builder implements Recorder<InstanceIdentifier, MatchInfoDisplay> {

		private final Map<InstanceIdentifier, MatchInfoDisplay> map;

		@Inject
		public Builder() {
			map = new HashMap<InstanceIdentifier, MatchInfoDisplay>();
		}

		public int size() {
			return map.size();
		}

		@Override
		public synchronized void record(InstanceIdentifier item, MatchInfoDisplay property) {
			map.put(item,property);
		}

		public void putAll(InstanceToMatchInfoDisplayMap otherMap) {
			map.putAll(otherMap.map);

		}

		public InstanceToMatchInfoDisplayMap build() {
			return new InstanceToMatchInfoDisplayMap(map);
		}
	}

}
