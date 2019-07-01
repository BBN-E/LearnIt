package com.bbn.learning.tensorflow;

import com.bbn.bue.common.TextGroupImmutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import org.immutables.value.Value;

import java.util.Map;

@TextGroupImmutable
@Value.Immutable
@JsonSerialize
@JsonDeserialize(as=ImmutableExternalClassificationDecisionWithLabel.class)
public abstract class ExternalClassificationDecisionWithLabel {
    public abstract String label();
    public abstract double confidence();

    public static class Builder extends ImmutableExternalClassificationDecisionWithLabel.Builder {}
}
