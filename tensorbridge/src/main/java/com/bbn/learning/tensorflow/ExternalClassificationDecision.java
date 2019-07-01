package com.bbn.learning.tensorflow;

import com.bbn.bue.common.TextGroupImmutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import org.immutables.value.Value;

@TextGroupImmutable
@Value.Immutable
@JsonSerialize
@JsonDeserialize(as=ImmutableExternalClassificationDecision.class)
public abstract class ExternalClassificationDecision {
  public abstract double[] labelToClassificationScore();

  public static class Builder extends ImmutableExternalClassificationDecision.Builder {}
}
