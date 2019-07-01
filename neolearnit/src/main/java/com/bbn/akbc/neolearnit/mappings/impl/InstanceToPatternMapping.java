package com.bbn.akbc.neolearnit.mappings.impl;

import java.util.Collection;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.mappings.AbstractMapping;
import com.bbn.akbc.neolearnit.mappings.AbstractMappingRecorder;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.storage.MapStorage;
import com.google.common.collect.Multiset;
import com.google.inject.Inject;

public class InstanceToPatternMapping extends
		AbstractMapping<InstanceIdentifier, LearnitPattern>{

	public InstanceToPatternMapping(MapStorage<InstanceIdentifier, LearnitPattern> storage) {
		super(storage);
	}

	public Collection<LearnitPattern> getPatterns(InstanceIdentifier id) {
		return storage.getRight(id);
	}

	public Collection<InstanceIdentifier> getInstances(LearnitPattern observation) {
		return storage.getLeft(observation);
	}

	public Multiset<InstanceIdentifier> getAllInstances() {
		return storage.getLefts();
	}

	public Multiset<LearnitPattern> getAllPatterns() {
		return storage.getRights();
	}

	public static class Builder
			extends AbstractMappingRecorder<InstanceIdentifier, LearnitPattern> {

		@Inject
		public Builder(MapStorage.Builder<InstanceIdentifier, LearnitPattern> storage) {
			super(storage);
		}

		@Override
		public void record(InstanceIdentifier item, LearnitPattern property) {
			super.record(item, property);
		}

		public InstanceToPatternMapping build() {
			return new InstanceToPatternMapping(this.buildMapping());
		}
	}


}

