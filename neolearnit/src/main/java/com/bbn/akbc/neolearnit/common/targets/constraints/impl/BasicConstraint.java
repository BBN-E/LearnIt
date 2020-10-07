package com.bbn.akbc.neolearnit.common.targets.constraints.impl;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.common.targets.constraints.MatchConstraint;
import com.bbn.akbc.neolearnit.observations.seed.Seed;

import java.util.Collection;

public class BasicConstraint implements MatchConstraint {
    @Override
    public boolean valid(MatchInfo match) {
        InstanceIdentifier instanceIdentifier = InstanceIdentifier.from(match);
        return this.valid(instanceIdentifier, null, null);
    }

    @Override
    public boolean valid(InstanceIdentifier instanceId, Collection<Seed> seeds, Target t) {
        if (instanceId.getSlot1SpanningType().equals(InstanceIdentifier.SpanningType.Empty)) {
            return true;
        } else {
            if (instanceId.getSlot0SpanningType().equals(InstanceIdentifier.SpanningType.EventMention)) {
                return instanceId.getSlot1SpanningType().equals(InstanceIdentifier.SpanningType.EventMention) || instanceId.getSlot1SpanningType().equals(InstanceIdentifier.SpanningType.Mention) || instanceId.getSlot1SpanningType().equals(InstanceIdentifier.SpanningType.ValueMention);
            } else if (instanceId.getSlot0SpanningType().equals(InstanceIdentifier.SpanningType.Mention)) {
                return instanceId.getSlot1SpanningType().equals(InstanceIdentifier.SpanningType.Mention) || instanceId.getSlot1SpanningType().equals(InstanceIdentifier.SpanningType.ValueMention);
            } else if (instanceId.getSlot0SpanningType().equals(InstanceIdentifier.SpanningType.ValueMention)) {
                return instanceId.getSlot1SpanningType().equals(InstanceIdentifier.SpanningType.ValueMention);
            } else {
                return false;
            }
        }
    }

    @Override
    public boolean offForEvaluation() {
        return false;
    }
}
