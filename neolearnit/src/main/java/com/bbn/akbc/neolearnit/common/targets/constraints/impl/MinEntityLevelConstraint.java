package com.bbn.akbc.neolearnit.common.targets.constraints.impl;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo.LanguageMatchInfo;
import com.bbn.akbc.neolearnit.common.targets.constraints.LanguageMatchConstraint;
import com.bbn.akbc.neolearnit.common.targets.constraints.SlotMatchConstraint;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.serif.patterns.MentionPattern;
import com.bbn.serif.patterns.Pattern.Builder;
import com.bbn.serif.theories.*;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class MinEntityLevelConstraint extends LanguageMatchConstraint implements SlotMatchConstraint {

	@JsonProperty
	private final String entityLevel;
	@JsonProperty
	private final int slot;

	@JsonCreator
	public MinEntityLevelConstraint(
			@JsonProperty("language") String language,
			@JsonProperty("slot") int slot,
			@JsonProperty("entityLevel") String entityLevel) {
		super(language);
		this.slot = slot;
		this.entityLevel = entityLevel;
	}

	@Override
	public boolean offForEvaluation() {
		return true;
	}

	@Override
	public boolean valid(LanguageMatchInfo match) {
		DocTheory dt = match.getDocTheory();
		Optional<Spanning> span = (slot == 0) ? match.getSlot0() : match.getSlot1();
		if(!span.isPresent()) {
			return false;
		}
		if (span.get() instanceof Mention) {
			Mention m = (Mention)span.get();
			Optional<Entity> e = m.entity(dt);
			if (entityLevel.equals("NAME")) {
				return m.isName() || (e.isPresent() && e.get().hasNameMention());

			} else if (entityLevel.equals("DESC")) {
				return m.isName() || m.mentionType().equals(Mention.Type.DESC) ||
						(e.isPresent() && e.get().hasNameOrDescMention());

			} else if (entityLevel.equals("NONE")) {
				return true;

			} else {
				throw new RuntimeException("Unrecognized Entity Level: Must be one of NAME, DESC, or NONE");
			}

		} else if (span.get() instanceof ValueMention) {
			return true;
		} else if (span.get() instanceof EventMention) {
			return true;
		}
 		return false;
	}

	@Override
	public boolean valid(InstanceIdentifier instanceId, Seed seed) {
		return true;
	}

	@Override
	public String toString() {
		return "MinEntityLevelConstraint [minEntityLevel=" + entityLevel +
				", slot=" + slot + "]";
	}

	@Override
	public Builder setBrandyConstraints(Builder builder) {
		if (builder instanceof MentionPattern.Builder) {
			MentionPattern.Builder mbuilder = (MentionPattern.Builder)builder;
			if (entityLevel.equals("NAME")) {
				mbuilder.withRequiresName(true);
			} else if (entityLevel.equals("DESC")) {
				mbuilder.withRequiresNameOrDesc(true);
			}
			return mbuilder;

		} else {
			throw new RuntimeException("EntityTypeConstraint: Invalid builder "+builder);
		}
	}

	@Override
	public int getSlot() {
		return slot;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((entityLevel == null) ? 0 : entityLevel.hashCode());
		result = prime * result + slot;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		MinEntityLevelConstraint other = (MinEntityLevelConstraint) obj;
		if (entityLevel == null) {
			if (other.entityLevel != null)
				return false;
		} else if (!entityLevel.equals(other.entityLevel))
			return false;
		if (slot != other.slot)
			return false;
		return true;
	}

}
