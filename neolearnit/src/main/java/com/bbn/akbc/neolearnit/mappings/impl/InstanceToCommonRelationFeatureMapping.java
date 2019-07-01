package com.bbn.akbc.neolearnit.mappings.impl;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.mappings.AbstractMapping;
import com.bbn.akbc.neolearnit.mappings.AbstractMappingRecorder;
import com.bbn.akbc.neolearnit.observations.feature.AbstractRelationFeature;
import com.bbn.akbc.neolearnit.storage.MapStorage;

import com.google.common.collect.Multiset;
import com.google.inject.Inject;

import java.util.Collection;


public class InstanceToCommonRelationFeatureMapping extends
    AbstractMapping<InstanceIdentifier, AbstractRelationFeature> {

  public InstanceToCommonRelationFeatureMapping(MapStorage<InstanceIdentifier, AbstractRelationFeature> storage) {
    super(storage);
  }

  public Collection<AbstractRelationFeature> getRelationFeatures(InstanceIdentifier id) {
    return storage.getRight(id);
  }

  public Collection<InstanceIdentifier> getInstances(AbstractRelationFeature relationFeature) {
    return storage.getLeft(relationFeature);
  }

  public Multiset<InstanceIdentifier> getAllInstances() {
    return storage.getLefts();
  }

  public Multiset<AbstractRelationFeature> getAllRelationFeatures() {
    return storage.getRights();
  }

  public static class Builder extends
      AbstractMappingRecorder<InstanceIdentifier, AbstractRelationFeature> {

    @Inject
    public Builder(MapStorage.Builder<InstanceIdentifier, AbstractRelationFeature> storage) {
      super(storage);
    }

    @Override
    public void record(InstanceIdentifier item, AbstractRelationFeature property) {
      super.record(item, property);
    }

    public InstanceToCommonRelationFeatureMapping build() {
      return new InstanceToCommonRelationFeatureMapping(this.buildMapping());
    }
  }

}
