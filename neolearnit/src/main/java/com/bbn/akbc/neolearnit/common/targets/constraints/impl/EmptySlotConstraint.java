package com.bbn.akbc.neolearnit.common.targets.constraints.impl;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.common.targets.constraints.MatchConstraint;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.NotImplementedException;

import java.util.Collection;


public class EmptySlotConstraint implements MatchConstraint {

    @JsonProperty
    final int slot;

    @JsonCreator
    public EmptySlotConstraint(@JsonProperty("slot") int slot){
        this.slot = slot;
    }

    @Override
    public boolean valid(MatchInfo match) {
        if(slot == 0){
            return !match.getPrimaryLanguageMatch().getSlot0().isPresent();
        }
        else if (slot == 1){
            return !match.getPrimaryLanguageMatch().getSlot1().isPresent();
        }
        else{
            throw new NotImplementedException("You have to update it");
        }
    }

    @Override
    public boolean valid(InstanceIdentifier instanceId, Collection<Seed> seeds, Target t) {
        if(this.slot == 0){
            return instanceId.getSlot0SpanningType() == InstanceIdentifier.SpanningType.Empty;
        }
        else if(slot == 1){
            return instanceId.getSlot1SpanningType() == InstanceIdentifier.SpanningType.Empty;
        }
        else{
            throw new NotImplementedException("You have to update it");
        }
    }

    @Override
    public boolean offForEvaluation() {
        return false;
    }
}
