package com.bbn.akbc.neolearnit.common.targets.constraints.impl;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.targets.constraints.AbstractSlotMatchConstraint;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.serif.patterns.MentionPattern;
import com.bbn.serif.patterns.Pattern.Builder;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.Spanning;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

import java.util.HashSet;
import java.util.Set;

public class MentionTypeConstraint extends AbstractSlotMatchConstraint {

	private final Set<String> mentionTypes;

	@JsonProperty
	private Set<String> mentionTypes() {
		return new HashSet<String>(mentionTypes);
	}

	@JsonCreator
	public MentionTypeConstraint(
			@JsonProperty("slot") int slot,
			@JsonProperty("mentionTypes") Iterable<String> mentionTypes) {
		super(slot);
		this.mentionTypes = ImmutableSet.copyOf(mentionTypes);
	}

	@Override
	public boolean offForEvaluation() {
		return false;
	}

	public MentionTypeConstraint(int slot, String mentionType) {
		super(slot);
		this.mentionTypes = ImmutableSet.of(mentionType);
	}

	@Override
	public boolean valid(Spanning mention) {
		if (mention instanceof Mention) {
			Optional<Mention> m = Optional.of((Mention)mention);
			while (m.isPresent()) {
				if (mentionTypes.contains(Mention.symbolForType(m.get().mentionType()).toString())) {
					return true;
				}
				m = m.get().child();
			}
			return false;
		} else {
			return false;
		}
	}

	@Override
	public boolean valid(InstanceIdentifier instanceId, Seed seed) {
		if (instanceId.getSlotMentionType(slot).isPresent()) {
			return mentionTypes.contains(Mention.symbolForType(instanceId.getSlotMentionType(slot).get()).toString());
		}
		return false;
	}

	@Override
	public Builder setBrandyConstraints(Builder builder) {
		if (builder instanceof MentionPattern.Builder) {
			MentionPattern.Builder mbuilder = (MentionPattern.Builder)builder;
			Set<Mention.Type> types = new HashSet<Mention.Type>();
			for (String type : mentionTypes) {
				types.add(Mention.typeForSymbol(Symbol.from(type)));
			}

			mbuilder.withMentionTypes(types);
			return mbuilder;

		} else {
			throw new RuntimeException("MentionTypeConstraint: Invalid builder "+builder);
		}
	}

	@Override
	public String toString() {
		return "MentionTypeConstraint [mentionTypes=" + mentionTypes + ", slot="
				+ slot + "]";
	}

}
