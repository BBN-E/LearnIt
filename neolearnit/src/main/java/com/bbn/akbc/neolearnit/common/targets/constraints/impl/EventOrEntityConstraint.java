package com.bbn.akbc.neolearnit.common.targets.constraints.impl;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.targets.constraints.AbstractSlotMatchConstraint;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.serif.patterns.Pattern;
import com.bbn.serif.theories.EventMention;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.Spanning;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;

public class EventOrEntityConstraint extends AbstractSlotMatchConstraint {

    final Set<String> entityTypes;

    @JsonCreator
    public EventOrEntityConstraint(@JsonProperty("slot") int slot,@JsonProperty("entityTypes") Collection<String> entityTypes){
        super(slot);
        this.entityTypes = new HashSet<>(entityTypes);
    }

    @Override
    public boolean valid(Spanning mention) {
        if (mention instanceof EventMention || mention instanceof Mention) {
            if(mention instanceof Mention){
                Mention m = (Mention) mention;
                return this.entityTypes.contains(m.entityType().toString());
            }
            else{
                return true;
            }
        } else {
            return false;
        }
    }

    @Override
    public Pattern.Builder setBrandyConstraints(Pattern.Builder builder) {
        return builder;
    }

    @Override
    public boolean valid(InstanceIdentifier instanceId, Seed seed) {
        return false;
    }

    @Override
    public boolean offForEvaluation() {
        return false;
    }
}
