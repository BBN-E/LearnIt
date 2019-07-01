package com.bbn.akbc.neolearnit.common.targets.properties;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/*
 * TargetProperty is the base property class,
 * and is used to represent boolean target
 * properties such as "isSymmetric"
 */
public class TargetProperty {

    public static final String PATTERN_CONFIDENCE = "pattern_confidence";

	public static final String SYMMETRIC = "symmetric";
    public static final String EMPTY_SETS = "allow_empty_set_props";
    public static final String STOPWORD_PATS = "accept_stopword_patterns";
    public static final String LEX_EXPANSION = "lexical_expansion";
    public static final String SIMPLE_PROPS = "use_simple_props";

	public static TargetProperty makeSymmetric() {
		return new TargetProperty(SYMMETRIC);
	}

	public static TargetProperty makeAllowDescTraining(int slot) {
		return new SlotProperty(slot, SlotProperty.ALLOW_DESC_TRAINING);
	}

	public static TargetProperty makeUseHeadText(int slot) {
		return new SlotProperty(slot, SlotProperty.USE_HEAD_TEXT);
	}

	/**
	 * All target properties must have a unique
	 * name for lookup in the target properties
	 * hash table.
	 **/
	@JsonProperty
	protected final String propertyName;

	@JsonCreator
	public TargetProperty(@JsonProperty("propertyName") String propertyName) {
		this.propertyName = propertyName;
	}

	public String getName() {
		return this.propertyName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((propertyName == null) ? 0 : propertyName.hashCode());
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
		TargetProperty other = (TargetProperty) obj;
		if (propertyName == null) {
			if (other.propertyName != null)
				return false;
		} else if (!propertyName.equals(other.propertyName))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "TargetProperty [propertyName=" + propertyName + "]";
	}

}
