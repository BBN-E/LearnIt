package com.bbn.akbc.neolearnit.common.targets.constraints.impl;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.common.targets.constraints.MatchConstraint;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.bue.common.exceptions.NotImplementedException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSet;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class SpanningTypeConstraint implements MatchConstraint {

    @JsonProperty("allowedSpanningType")
    final Set<InstanceIdentifier.SpanningType> allowedSpanningType;
    @JsonProperty("slot")
    final int slot;

    @JsonCreator
    public SpanningTypeConstraint(@JsonProperty("slot") int slot, @JsonProperty("allowedSpanningType") Collection<InstanceIdentifier.SpanningType> allowedSpanningType) {
        this.slot = slot;
        this.allowedSpanningType = new HashSet<>(allowedSpanningType);
    }

    @Override
    public boolean valid(MatchInfo match) {
        return this.valid(InstanceIdentifier.from(match), null, null);
    }

    @Override
    public boolean valid(InstanceIdentifier instanceId, Collection<Seed> seeds, Target t) {
        if (slot == 0) {
            boolean isValid = this.allowedSpanningType.contains(instanceId.getSlot0SpanningType());
//            System.out.println("[AAAAA] "+ isValid +" "+slot+ " " +this.allowedSpanningType.stream().map(InstanceIdentifier.SpanningType::toString).collect(Collectors.joining(" ")) +" " +isValid+instanceId.toString());
            return isValid;
        } else if (slot == 1) {
            boolean isValid = this.allowedSpanningType.contains(instanceId.getSlot1SpanningType());
//            System.out.println("[AAAAA] "+ isValid +" "+slot+ " " +this.allowedSpanningType.stream().map(InstanceIdentifier.SpanningType::toString).collect(Collectors.joining(" ")) +" " +isValid+instanceId.toString());
            return isValid;
        } else {
            throw new NotImplementedException();
        }
    }

    @Override
    public boolean offForEvaluation() {
        return false;
    }

    public boolean containsEmptySpanning() {
        return this.allowedSpanningType.contains(InstanceIdentifier.SpanningType.Empty);
    }

    public int getSlot() {
        return this.slot;
    }

    public Set<InstanceIdentifier.SpanningType> getAllowedSpanningType() {
        return ImmutableSet.copyOf(this.allowedSpanningType);
    }

}
