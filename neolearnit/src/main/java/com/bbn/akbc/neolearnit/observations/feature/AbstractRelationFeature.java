package com.bbn.akbc.neolearnit.observations.feature;

import com.bbn.akbc.neolearnit.observations.LearnItObservation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by bmin on 6/6/15.
 */

@JsonIgnoreProperties(ignoreUnknown=true)
public abstract class AbstractRelationFeature extends LearnItObservation {
  @Override
  public abstract String toPrettyString();

  public abstract String toIDString();


}
