package com.bbn.akbc.neolearnit.mappings.groups;

import java.util.Map;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToSeedMapping;
import com.bbn.akbc.neolearnit.storage.EfficientMapDataStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MappingsWithDisplay {

	@JsonProperty
	private final Mappings mappings;
	private final Map<InstanceIdentifier,String> displayMap;

	@JsonProperty
	private EfficientMapDataStore<InstanceIdentifier,String> displayMap() {
		return EfficientMapDataStore.fromMap(displayMap);
	}

	@JsonCreator
	public MappingsWithDisplay(@JsonProperty("mappings") Mappings mappings,
			@JsonProperty("displayMap") EfficientMapDataStore<InstanceIdentifier,String> displayMap) {
		this.mappings = mappings;
		this.displayMap = displayMap.makeMap();
	}

	public MappingsWithDisplay(InstanceToSeedMapping i2Seed, InstanceToPatternMapping i2Pattern, Map<InstanceIdentifier,String> i2Display) {

		mappings = new Mappings(i2Seed,i2Pattern);
		displayMap = i2Display;

	}

	public Mappings getMappings() {
		return mappings;
	}

	public Map<InstanceIdentifier,String> getDisplayMap() {
		return displayMap;
	}



}
