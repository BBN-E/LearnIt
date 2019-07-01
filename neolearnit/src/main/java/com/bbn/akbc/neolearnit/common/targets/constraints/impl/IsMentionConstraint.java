package com.bbn.akbc.neolearnit.common.targets.constraints.impl;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.targets.constraints.AbstractSlotMatchConstraint;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.serif.patterns.Pattern.Builder;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.Spanning;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class IsMentionConstraint extends AbstractSlotMatchConstraint {

	@JsonCreator
	public IsMentionConstraint(@JsonProperty("slot") int slot,@JsonProperty("canBeEmptySlot") boolean canBeEmptySlot) {
		super(slot,canBeEmptySlot);
	}

	public IsMentionConstraint(int slot){
		super(slot);
	}


	@Override
	public boolean offForEvaluation() {
		return false;
	}

	@Override
	public boolean valid(Spanning mention) {
		return mention instanceof Mention;
	}

	@Override
	public boolean valid(InstanceIdentifier instanceId, Seed seed) {
		return instanceId.isSlotMention(slot);
	}

	@Override
	public Builder setBrandyConstraints(Builder builder) {
		return builder;
	}
}
