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
 * Ensures that the slot's mention is atomic.
 * For names , we take them as they come. For desctiptors, we ensure they have no child mentions.
 * You pretty much always want this on, except for value mentions where it's irrelevant.
 */
public class AtomicMentionConstraint extends AbstractSlotMatchConstraint {

	@JsonCreator
	public AtomicMentionConstraint(@JsonProperty("slot") int slot) {
		super(slot);
	}

	@Override
	public boolean offForEvaluation() {
		return false;
	}

	@Override
	public boolean valid(Spanning spanning) {
		if (spanning instanceof Mention) {
			Mention m = (Mention)spanning;

			if (m.mentionType() == Type.NAME) {
				return true;
			} else if (m.mentionType() == Type.NONE) {
				return false;
			} else {
				if (m.child().isPresent()) {
					return false;
				}
				return true;
			}
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
