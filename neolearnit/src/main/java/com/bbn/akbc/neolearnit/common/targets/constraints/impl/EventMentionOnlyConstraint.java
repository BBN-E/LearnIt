package com.bbn.akbc.neolearnit.common.targets.constraints.impl;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.common.targets.constraints.AbstractSlotMatchConstraint;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.bue.common.exceptions.NotImplementedException;
import com.bbn.serif.patterns.Pattern;
import com.bbn.serif.theories.EventMention;
import com.bbn.serif.theories.Spanning;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;



public class EventMentionOnlyConstraint extends AbstractSlotMatchConstraint {

    @JsonCreator
    public EventMentionOnlyConstraint(@JsonProperty("slot") int slot,@JsonProperty("canBeEmptySlot") boolean canBeEmptySlot) {
        super(slot,canBeEmptySlot);
    }

    public EventMentionOnlyConstraint(int slot){
        super(slot);
    }

    @Override
    public boolean offForEvaluation() {
        return false;
    }

    @Override
    public boolean valid(Spanning spanning) {
        if (spanning instanceof EventMention) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean valid(InstanceIdentifier instanceId, Seed seed) {
        if(this.slot == 0){
            return (instanceId.getSlot0SpanningType() == InstanceIdentifier.SpanningType.EventMention) || (this.canBeEmptySlot && instanceId.getSlot0SpanningType() == InstanceIdentifier.SpanningType.Empty);
        }
        else if(this.slot == 1){
            return (instanceId.getSlot1SpanningType() == InstanceIdentifier.SpanningType.EventMention) || (this.canBeEmptySlot && instanceId.getSlot1SpanningType() == InstanceIdentifier.SpanningType.Empty);
        }
        else{
            throw new NotImplementedException("You have to take care of it");
        }
    }

    @Override
    public Pattern.Builder setBrandyConstraints(Pattern.Builder builder) {
        return builder;
    }

    @Override
    public String toString() {
        return "EventMentionOnlyConstraint [slot=" + slot + "]";
    }

}
