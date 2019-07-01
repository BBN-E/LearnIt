package com.bbn.akbc.neolearnit.common.targets.properties;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SlotProperty extends TargetProperty {

	public static final String USE_HEAD_TEXT = "use_head_text";
	public static final String ALLOW_DESC_TRAINING = "allow_desc_training";

	@JsonProperty
	private final int slot;

	@JsonCreator
	public SlotProperty(
			@JsonProperty("slot") int slotNum,
			@JsonProperty("propertyName") String name) {
		super(name);
		this.slot = slotNum;
	}
}
