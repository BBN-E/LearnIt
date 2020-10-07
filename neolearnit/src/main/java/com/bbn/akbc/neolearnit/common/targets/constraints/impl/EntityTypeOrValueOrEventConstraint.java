package com.bbn.akbc.neolearnit.common.targets.constraints.impl;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.bue.common.exceptions.NotImplementedException;
import com.bbn.serif.theories.Spanning;
import com.bbn.serif.theories.ValueMention;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class EntityTypeOrValueOrEventConstraint extends EntityTypeConstraint {

    @JsonCreator
    public EntityTypeOrValueOrEventConstraint(
            @JsonProperty("slot") int slot,
            @JsonProperty("entityTypes") Iterable<String> entityTypes) {
        super(slot, entityTypes);
    }

    @Override
    public boolean offForEvaluation() {
        return false;
    }

    public EntityTypeOrValueOrEventConstraint(int slot, String entityType) {
        super(slot, entityType);
    }

    @Override
    public boolean valid(Spanning mention) {
        return mention instanceof ValueMention || super.valid(mention);
    }

    @Override
    public boolean valid(InstanceIdentifier instanceId, Seed seed) {
        if (slot == 0) {
            return
                    (instanceId.getSlot0SpanningType().equals(InstanceIdentifier.SpanningType.Mention) && super.valid(instanceId, seed)) ||
                            instanceId.getSlot0SpanningType().equals(InstanceIdentifier.SpanningType.ValueMention);
        } else if (slot == 1) {
            return (instanceId.getSlot1SpanningType().equals(InstanceIdentifier.SpanningType.Mention) && super.valid(instanceId, seed)) ||
                    instanceId.getSlot1SpanningType().equals(InstanceIdentifier.SpanningType.ValueMention);
        } else {
            throw new NotImplementedException();
        }
    }

    @Override
    public String toString() {
        return "ValueOrEventOr"+super.toString();
    }

}
