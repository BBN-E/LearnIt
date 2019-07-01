package com.bbn.akbc.neolearnit.common.targets;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.common.targets.constraints.MatchConstraint;
import com.bbn.akbc.neolearnit.common.targets.constraints.SlotMatchConstraint;
import com.bbn.akbc.neolearnit.common.targets.constraints.impl.EntityTypeConstraint;
import com.bbn.akbc.neolearnit.common.targets.constraints.impl.ValueTypeConstraint;
import com.bbn.akbc.neolearnit.common.targets.properties.SlotProperty;
import com.bbn.akbc.neolearnit.common.targets.properties.TargetProperty;
import com.bbn.akbc.neolearnit.observations.pattern.restriction.Restriction;
import com.bbn.akbc.neolearnit.observations.pattern.restriction.Restriction.MentionPatternBrandyRestrictor;
import com.bbn.akbc.neolearnit.observations.pattern.restriction.Restriction.ValueMentionPatternBrandyRestrictor;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.serif.patterns.MapPatternReturn;
import com.bbn.serif.patterns.MentionPattern;
import com.bbn.serif.patterns.Pattern;
import com.bbn.serif.patterns.ValueMentionPattern;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TargetSlot implements Comparable<TargetSlot>{
	@JsonProperty
	private final int slotNum;
	@JsonProperty
	private final String type;

	private final List<SlotMatchConstraint> constraints;
	private final Map<String, TargetProperty> properties;

	@JsonProperty
	private List<SlotMatchConstraint> constraints() {
		return new ArrayList<SlotMatchConstraint>(constraints);
	}

	@JsonProperty
	private Map<String, TargetProperty> properties() {
		return new HashMap<String, TargetProperty>(properties);
	}

	@JsonCreator
	private TargetSlot(
			@JsonProperty("slotNum") int slotNum,
			@JsonProperty("type") String type,
			@JsonProperty("constraints") List<SlotMatchConstraint> constraints,
			@JsonProperty("properties") Map<String, TargetProperty> properties){
		this.slotNum = slotNum;
		this.type = type;
		this.constraints = new ArrayList<SlotMatchConstraint>(constraints);
		this.properties = new HashMap<String, TargetProperty>(properties);
	}

	public static class Builder {
		private final int slotNum;
		private final String type;
		private final ImmutableList.Builder<SlotMatchConstraint> constraintBuilder;
		private final ImmutableMap.Builder<String, TargetProperty> propertiesBuilder;

		public Builder(int slotNum, String type) {
			this.slotNum = slotNum;
			this.type = type;
			this.constraintBuilder = new ImmutableList.Builder<SlotMatchConstraint>();
			this.propertiesBuilder = new ImmutableMap.Builder<String, TargetProperty>();
		}

		public Builder withAddedConstraint(SlotMatchConstraint constraint) {
			this.constraintBuilder.add(constraint);
			return this;
		}

		public Builder withAddedProperty(TargetProperty property) {
			this.propertiesBuilder.put(property.getName(), property);
			return this;
		}

		public TargetSlot build() {
			return new TargetSlot(slotNum,type,
					constraintBuilder.build(),
					propertiesBuilder.build());
		}
	}

	public List<SlotMatchConstraint> getSlotConstraints() {
		return constraints;
	}

	public boolean isMention() {
		return type.equals("mention");
	}

	public boolean useBestName() {
		return !this.hasBooleanProperty(SlotProperty.USE_HEAD_TEXT);
	}

	public boolean allowDescTraining() {
		return this.hasBooleanProperty(SlotProperty.ALLOW_DESC_TRAINING);
	}

	public boolean hasBooleanProperty(String propertyName) {
		return this.properties.containsKey(propertyName);
	}

	public boolean validMatch(MatchInfo match, boolean evaluating) {
		for (SlotMatchConstraint c : constraints) {
			if (evaluating && c.offForEvaluation()) continue;

			if (!c.valid(match)) {
				return false;
			}
		}
		return true;
	}

	private boolean validMentionTypeInInstance(InstanceIdentifier id) {
		if (this.allowDescTraining() || !id.isSlotMention(slotNum)) {
			return true;
		} else if (this.useBestName()) {
			return id.isSlotBestNameTypeName(slotNum);
		} else {
			return id.isSlotName(slotNum);
		}
	}

	public boolean validInstance(InstanceIdentifier id, Collection<Seed> seeds) {

		if (!validMentionTypeInInstance(id)) {
			return false;
		}

		for (MatchConstraint c : constraints) {
			if (!c.valid(id, seeds, null)) {
				return false;
			}
		}
		return true;
	}

	private Pattern.Builder getSlotBuilder() {

		for (SlotMatchConstraint constraint : this.constraints) {

			if (constraint instanceof EntityTypeConstraint) {
				return new MentionPattern.Builder();
			}

			if (constraint instanceof ValueTypeConstraint) {
				return new ValueMentionPattern.Builder();
			}

		}
		return new MentionPattern.Builder();
	}

	public Pattern applyRestrictions(Pattern pattern, Iterable<Restriction> restrictions) {
		if (pattern instanceof MentionPattern) {
			for (Restriction r : restrictions) {
				if (r instanceof MentionPatternBrandyRestrictor) {
					pattern = ((MentionPatternBrandyRestrictor)r).restrictBrandyPattern((MentionPattern)pattern);
				}
			}
		}

		if (pattern instanceof ValueMentionPattern) {
			for (Restriction r : restrictions) {
				if (r instanceof ValueMentionPatternBrandyRestrictor) {
					pattern = ((ValueMentionPatternBrandyRestrictor)r).restrictBrandyPattern((ValueMentionPattern)pattern);
				}
			}
		}

		return pattern;
	}

	public Pattern makeBrandyPattern(String factType, String targetName, boolean reflexive, Iterable<Restriction> restrictions) {

		Pattern.Builder builder = getSlotBuilder();

		for (SlotMatchConstraint constraint : this.constraints) {
			constraint.setBrandyConstraints(builder);
		}

		if (slotNum == 0) {
			builder.withPatternReturn(
					new MapPatternReturn.Builder()
						.withReturnAdd("ff_role", "AGENT1")
						.withReturnAdd("ff_fact_type", factType)
						.build());

			if (!reflexive) {
				if (builder instanceof MentionPattern.Builder) {
					MentionPattern.Builder mbuilder = (MentionPattern.Builder)builder;
					mbuilder.withEntityLabels(ImmutableList.of(Symbol.from("AGENT1")));
				}
			}

		} else {
			builder.withPatternReturn(
					new MapPatternReturn.Builder()
						.withReturnAdd("ff_role", targetName)
						.build());

			if (!reflexive) {

				if (builder instanceof MentionPattern.Builder) {
					MentionPattern.Builder mbuilder = (MentionPattern.Builder)builder;
					mbuilder.withBlockingEntityLabels(ImmutableList.of(Symbol.from("AGENT1")));
				}
			} else if (!(builder instanceof MentionPattern.Builder)) {
				throw new RuntimeException("Reflexivity doesn't make sense for non mention-mention relations.");
			}

		}

		return applyRestrictions(builder.build(), restrictions);

	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((constraints == null) ? 0 : constraints.hashCode());
		result = prime * result
				+ ((properties == null) ? 0 : properties.hashCode());
		result = prime * result + slotNum;
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TargetSlot other = (TargetSlot) obj;
		if (constraints == null) {
			if (other.constraints != null)
				return false;
		} else if (!constraints.equals(other.constraints))
			return false;
		if (properties == null) {
			if (other.properties != null)
				return false;
		} else if (!properties.equals(other.properties))
			return false;
		if (slotNum != other.slotNum)
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "TargetSlot [slotNum=" + slotNum + ", type=" + type
				+ ", constraints=" + constraints + ", properties=" + properties
				+ "]";
	}

	@Override
	public int compareTo(TargetSlot o) {
		return this.slotNum - o.slotNum;
	}

}
