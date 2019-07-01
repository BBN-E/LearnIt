package com.bbn.akbc.neolearnit.common.targets.constraints.impl;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.targets.constraints.SlotPairMatchConstraint;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.serif.theories.Spanning;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;

public class NoOverlapConstraint extends SlotPairMatchConstraint {

	private enum Setting {TOKEN, WHOLE, EQUAL};
	private static final BiMap<String, Setting> SETTING_MAP =
			ImmutableBiMap.of("token",Setting.TOKEN, "whole",Setting.WHOLE, "equal",Setting.EQUAL);

	private final Setting setting;

	@JsonProperty
	private String setting() {
		return SETTING_MAP.inverse().get(setting);
	}

	@JsonCreator
	public NoOverlapConstraint(
			@JsonProperty("setting") String setting)
	{
		super();
		if (SETTING_MAP.containsKey(setting))
			this.setting = SETTING_MAP.get(setting);
		else
			throw new RuntimeException(String.format("Unknown \"no_overlap\" setting '%s'!",setting));
	}

	private boolean hasRelevantOverlap(String s1, String s2) {
		if (setting == Setting.TOKEN) {
			String[] a1 = s1.split(" ");
			String[] a2 = s2.split(" ");
			for (String t1 : a1) {
				for (String t2 : a2) {
					if (t1.equals(t2)) {
						return true;
					}
				}
			}
			return false;
		} else if (setting == Setting.WHOLE) {
			return s1.contains(s2) || s2.contains(s1);
		} else { //(setting == Setting.EQUAL)
			return s1.equals(s2);
		}
	}

	@Override
	public boolean valid(InstanceIdentifier instanceId, Seed seed) {
		return !hasRelevantOverlap(seed.getSlot(0).toString(), seed.getSlot(1).toString());
	}

	@Override
	public boolean offForEvaluation() {
		return true;
	}

	@Override
	public boolean valid(Spanning slot0, Spanning slot1) {
		return true; //Not capable of knowing the proper text to consider at this point
	}

}
