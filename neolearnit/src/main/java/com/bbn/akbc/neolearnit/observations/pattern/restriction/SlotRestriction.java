package com.bbn.akbc.neolearnit.observations.pattern.restriction;

import com.bbn.serif.patterns.MapPatternReturn;
import com.bbn.serif.patterns.Pattern;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.MINIMAL_CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public abstract class SlotRestriction extends Restriction {

	@JsonProperty
	protected final int slot;

	protected SlotRestriction(int slot) {
		this.slot = slot;
	}

	@Override
	public boolean isInCanonicalSymmetryOrder() {
		return true;
	}

	public int getSlot() {
		return slot;
	}

	public static int getBrandyPatternSlot(Pattern pattern) {
		if (pattern.getPatternReturn() != null && pattern.getPatternReturn() instanceof MapPatternReturn) {

			MapPatternReturn ret = (MapPatternReturn)pattern.getPatternReturn();
			return ret.keySet().contains("ff_fact_type") ? 0 : 1;

		} else {
			return -1;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * slot;
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
		SlotRestriction other = (SlotRestriction) obj;
		if (slot != other.slot)
			return false;
		return true;
	}

}
