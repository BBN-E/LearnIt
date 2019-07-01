package com.bbn.akbc.neolearnit.observations.pattern.restriction;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.targets.constraints.SlotMatchConstraint;
import com.bbn.akbc.neolearnit.common.targets.constraints.ValidTypeConstraint;
import com.bbn.akbc.neolearnit.common.targets.constraints.impl.EntityTypeConstraint;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.pattern.restriction.Restriction.MentionPatternBrandyRestrictor;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.serif.patterns.MentionPattern;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@JsonTypeInfo(use=JsonTypeInfo.Id.MINIMAL_CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public class SlotEntityTypeRestriction extends SlotRestriction
		implements MentionPatternBrandyRestrictor {

	@JsonProperty
	private final String eType;

	private static final Pattern ID_STRING_FORMAT = Pattern.compile("\\[slot=(\\d), etype=(\\w\\w\\w)\\]");

	@JsonCreator
	public SlotEntityTypeRestriction(@JsonProperty("slot") int slot, @JsonProperty("eType") String eType) {
		super(slot);
		this.eType = eType;
	}

	public String getEType() {
		return eType;
	}

	@Override
	public boolean appliesTo(InstanceIdentifier instance, Mappings mappings) {
		return instance.getSlotEntityType(slot).equals(eType);
	}

	@Override
	public String toString() {
		return String.format("SlotETypeRestriction[slot=%d, etype=%s]",slot,eType);
	}

	@Override
	public String toPrettyString() {
		return toString();
	}

	@Override
	@JsonProperty
	public String toIDString() {
		return String.format("[slot=%d, etype=%s]",slot,eType);
	}


	public static Optional<? extends Restriction> fromIDString(String idString) {
		Matcher idMatch = ID_STRING_FORMAT.matcher(idString);
		if (idMatch.matches()) {
			int foundSlot = Integer.parseInt(idMatch.group(1));
			String foundEtype = idMatch.group(2);
			return Optional.of(new SlotEntityTypeRestriction(foundSlot, foundEtype));
		} else {
			return Optional.<Restriction>absent();
		}
	}

//	public com.bbn.serif.patterns.Pattern modifyBrandy(com.bbn.serif.patterns.Pattern pattern) {
//		com.bbn.serif.patterns.Pattern.Builder builder = pattern.;
//		builder.
//	}

	@Override
	public Set<Symbol> getLexicalItems() {
		return ImmutableSet.of();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((eType == null) ? 0 : eType.hashCode());
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
		SlotEntityTypeRestriction other = (SlotEntityTypeRestriction) obj;
		if (eType == null) {
			if (other.eType != null)
				return false;
		} else if (!eType.equals(other.eType))
			return false;
		return true;
	}

	@Override
	public MentionPattern restrictBrandyPattern(MentionPattern pattern) {
		return pattern.modifiedCopyBuilder()
				.withAceTypes(Lists.newArrayList(EntityTypeConstraint.getTypeFromString(eType)))
				.build();
	}

	public static class SlotEntityTypeRestrictionFactory extends RestrictionFactory {

		public SlotEntityTypeRestrictionFactory(TargetAndScoreTables data, Mappings mappings) {
			super(data, mappings);
		}

		private boolean restrictSlot(int slot) {
			for (SlotMatchConstraint c : data.getTarget().getSlot(slot).getSlotConstraints()) {
				if (c instanceof ValidTypeConstraint) {
					ValidTypeConstraint vc = (ValidTypeConstraint)c;
					if (vc.getValidTypeSet().size() > 1) return true;
				}
			}
			return false;
		}

		@Override
		public Collection<Restriction> getRestrictions(LearnitPattern pattern) {

			Set<Restriction> toReturn = new HashSet<Restriction>();
			for (InstanceIdentifier instance : mappings.getInstance2Pattern().getInstances(pattern)) {
				if (restrictSlot(0))
					toReturn.add(new SlotEntityTypeRestriction(0, instance.getSlotEntityType(0)));

				if (restrictSlot(1))
					toReturn.add(new SlotEntityTypeRestriction(1, instance.getSlotEntityType(1)));
			}
			return toReturn;
		}

	}

	@Override
	public boolean matchesPattern(LearnitPattern p) {
		// TODO Auto-generated method stub
		return false;
	}

}
