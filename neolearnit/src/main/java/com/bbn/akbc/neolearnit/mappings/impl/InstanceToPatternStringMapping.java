package com.bbn.akbc.neolearnit.mappings.impl;

import java.util.Collection;
import java.util.Set;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.mappings.AbstractMapping;
import com.bbn.akbc.neolearnit.mappings.AbstractMappingRecorder;
import com.bbn.akbc.neolearnit.storage.MapStorage;
import com.google.common.collect.Multiset;
import com.google.inject.Inject;

public class InstanceToPatternStringMapping extends AbstractMapping<InstanceIdentifier, String> {

		public InstanceToPatternStringMapping(MapStorage<InstanceIdentifier, String> storage) {
			super(storage);
		}

		public Collection<String> getPatterns(InstanceIdentifier id) {
			return storage.getRight(id);
		}

		public Collection<InstanceIdentifier> getInstances(String pattern) {
			return storage.getLeft(pattern);
		}

		public Multiset<InstanceIdentifier> getAllInstances() {
			return storage.getLefts();
		}

		public Set<String> getAllPatterns() {
			return storage.getRights().elementSet();
		}

		public static class Builder extends AbstractMappingRecorder<InstanceIdentifier, String> {

			@Inject
			public Builder(MapStorage.Builder<InstanceIdentifier, String> storage) {
				super(storage);
			}

			public InstanceToPatternStringMapping build() {
				return new InstanceToPatternStringMapping(this.buildMapping());
			}
		}

	}
