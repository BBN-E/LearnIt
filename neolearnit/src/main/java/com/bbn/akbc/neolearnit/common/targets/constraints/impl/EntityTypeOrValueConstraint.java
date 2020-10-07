package com.bbn.akbc.neolearnit.common.targets.constraints.impl;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.serif.theories.Spanning;
import com.bbn.serif.theories.ValueMention;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class EntityTypeOrValueConstraint extends EntityTypeConstraint {

	@JsonCreator
	public EntityTypeOrValueConstraint(
			@JsonProperty("slot") int slot,
			@JsonProperty("entityTypes") Iterable<String> entityTypes) {
		super(slot, entityTypes);
	}

	@Override
	public boolean offForEvaluation() {
		return false;
	}

	public EntityTypeOrValueConstraint(int slot, String entityType) {
		super(slot, entityType);
	}

	@Override
	public boolean valid(Spanning mention) {
		return mention instanceof ValueMention || super.valid(mention);
	}

	@Override
	public boolean valid(InstanceIdentifier instanceId, Seed seed) {
        return super.valid(instanceId, seed) || instanceId.isSlotMention(slot) || instanceId.isSlotValueMention(slot);
	}

	@Override
	public String toString() {
		return "ValueOr"+super.toString();
	}

}
