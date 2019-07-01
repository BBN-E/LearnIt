package com.bbn.akbc.neolearnit.mappings.impl;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.mappings.AbstractMapping;
import com.bbn.akbc.neolearnit.mappings.AbstractMappingRecorder;
import com.bbn.akbc.neolearnit.relations.Features.FeatureSet;
import com.bbn.akbc.neolearnit.storage.MapStorage;

import com.google.common.collect.Multiset;
import com.google.inject.Inject;

import java.util.Collection;

public class InstanceToLexFeatureSetMapping extends AbstractMapping<InstanceIdentifier, FeatureSet> {

	public InstanceToLexFeatureSetMapping(MapStorage<InstanceIdentifier, FeatureSet> storage) {
		super(storage);
	}

	public Collection<FeatureSet> getLexFeatureSet(InstanceIdentifier id) {
		return storage.getRight(id);
	}

	public Collection<InstanceIdentifier> getInstances(FeatureSet FeatureSet) {
		return storage.getLeft(FeatureSet);
	}

	public Multiset<InstanceIdentifier> getAllInstances() {
		return storage.getLefts();
	}

	public Multiset<FeatureSet> getAllLexFeatureSets() {
		return storage.getRights();
	}

	public static class Builder extends AbstractMappingRecorder<InstanceIdentifier, FeatureSet> {

		@Inject
		public Builder(MapStorage.Builder<InstanceIdentifier, FeatureSet> storage) {
			super(storage);
		}

		public InstanceToLexFeatureSetMapping build() {
			return new InstanceToLexFeatureSetMapping(this.buildMapping());
		}
	}

}
