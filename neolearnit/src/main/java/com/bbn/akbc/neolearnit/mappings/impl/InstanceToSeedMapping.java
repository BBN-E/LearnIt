package com.bbn.akbc.neolearnit.mappings.impl;

import java.util.Collection;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.mappings.AbstractMapping;
import com.bbn.akbc.neolearnit.mappings.AbstractMappingRecorder;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.storage.MapStorage;
import com.google.common.collect.Multiset;
import com.google.inject.Inject;

public class InstanceToSeedMapping extends AbstractMapping<InstanceIdentifier, Seed> {

	public InstanceToSeedMapping(MapStorage<InstanceIdentifier, Seed> storage) {
		super(storage);
	}

	public Collection<Seed> getSeeds(InstanceIdentifier id) {
		return storage.getRight(id);
	}

	public Collection<InstanceIdentifier> getInstances(Seed seed) {
		return storage.getLeft(seed);
	}

	public Multiset<InstanceIdentifier> getAllInstances() {
		return storage.getLefts();
	}

	public Multiset<Seed> getAllSeeds() {
		return storage.getRights();
	}

	public static class Builder extends AbstractMappingRecorder<InstanceIdentifier, Seed> {

		@Inject
		public Builder(MapStorage.Builder<InstanceIdentifier, Seed> storage) {
			super(storage);
		}

		public InstanceToSeedMapping build() {
			return new InstanceToSeedMapping(this.buildMapping());
		}
	}

}
