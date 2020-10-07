package com.bbn.akbc.neolearnit.common.targets.constraints.impl;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.targets.constraints.AbstractSlotMatchConstraint;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.serif.patterns.Pattern.Builder;
import com.bbn.serif.theories.EventMention;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.Mention.Type;
import com.bbn.serif.theories.Spanning;
import com.bbn.serif.theories.ValueMention;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Ensures that the slot's mention is has a non-NONE type.
 * These are the mentions that can appear in Propositions, so you probably always want this
 * turned on.
 **/
public class AtomicMentionConstraint extends AbstractSlotMatchConstraint {

	@JsonCreator
	public AtomicMentionConstraint(@JsonProperty("slot") int slot) {
		super(slot);
	}

	public AtomicMentionConstraint(int slot, boolean canBeEmptySlot) {
		super(slot, canBeEmptySlot);
	}

	@Override
	public boolean offForEvaluation() {
		return false;
	}

	@Override
	public boolean valid(Spanning spanning) {
		if (spanning instanceof Mention) {
			Mention m = (Mention)spanning;
			return !(m.mentionType() == Type.NONE);
		} else if (spanning instanceof ValueMention) {
			return true;
		} else if (spanning instanceof EventMention) { // An EventMention should always be atomic
			return true;
		} else {
			throw new RuntimeException("Unrecognized spanning: "+spanning);
		}
	}

	@Override
	public boolean valid(InstanceIdentifier instanceId, Seed seed) {
		return true;
	}

	@Override
	public Builder setBrandyConstraints(Builder builder) {
		return builder;
	}

	@Override
	public String toString() {
		return "AtomicMentionConstraint [slot=" + slot + "]";
	}

}
