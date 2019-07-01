package com.bbn.akbc.neolearnit.common.targets;

import com.bbn.bue.common.xml.XMLUtils;
import com.bbn.akbc.neolearnit.common.targets.properties.SlotProperty;
import com.google.common.base.Optional;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

/*
 * TargetSlotDefinition is a class adapted from learnit's TargetSlot class,
 * and is used in neolearnit only to help parse xml target definition files
 * formatted for learnit.
 */
public class TargetSlotDefinition {

	public Integer slotnum;
	public String description;
	public String type;
	public String mention_constraints;
	public String seed_type;
	public Boolean use_best_name;
	public String brandy_constraints;
	public Boolean allow_desc_training;

	public List<String> getTypes() {
		List<String> types = new ArrayList<String>();
		String mcons = this.mention_constraints;
		int typeIndex = mcons.indexOf("(acetype") + "(acetype".length();
		if (!mcons.contains("(acetype")) {
			typeIndex = mcons.indexOf("(type") + "(type".length();
			if (!mcons.contains("(type")) {
				return types;
			}
		}
		int endIndex = mcons.indexOf(')', typeIndex);
		String[] acetypes = mcons.substring(typeIndex,endIndex).split(" ");
		for (String type : acetypes)
			if (type.trim().length() > 0)
				types.add(type);
		return types;
	}

	public String getMinEntityLevel() {
		String mcons = this.mention_constraints;
		if (!mcons.contains("(min-entitylevel"))
			return "";
		int minEntIndex = mcons.indexOf("(min-entitylevel") + "(min-entitylevel".length();
		if (!mcons.contains("(acetype"))
			return "";
		int endIndex = mcons.indexOf(')', minEntIndex);
		return mcons.substring(minEntIndex,endIndex).trim();
	}

	public List<String> getMentionTypes() {
		List<String> types = new ArrayList<String>();
		String mcons = this.mention_constraints;
		if (!mcons.contains("(mentiontype"))
			return types;
		int mTypeIndex = mcons.indexOf("(mentiontype") + "(mentiontype".length();
		int endIndex = mcons.indexOf(')', mTypeIndex);
		String[] mTypes = mcons.substring(mTypeIndex,endIndex).split(" ");
		for (String type : mTypes)
			if (type.trim().length() > 0)
				types.add(type.toLowerCase());
		return types;
	}

//	public List<String> getAllowedMentionTypes() {
//		if (getMinEntityLevel().equals("NAME"))
//			return ImmutableList.of("NAME");
//		else if (type.equals("value"))
//			return ImmutableList.of("");
//		else
//			return ImmutableList.of("NAME", "DESC");
//	}

	public static TargetSlotDefinition fromElement(Element slot) {
		TargetSlotDefinition ts = new TargetSlotDefinition();
		ts.slotnum = XMLUtils.requiredIntegerAttribute(slot, "slotnum");
		ts.description = XMLUtils.requiredAttribute(slot, "description");
		ts.type = XMLUtils.requiredAttribute(slot, "type");
		ts.mention_constraints = XMLUtils.requiredAttribute(slot, "mention_constraints");
		if (slot.hasAttribute("seed_type"))
			ts.seed_type = slot.getAttribute("seed_type");
		else
			ts.seed_type = null;
		if (slot.hasAttribute("brandy_constraints"))
			ts.brandy_constraints = slot.getAttribute("brandy_constraints");
		else
			ts.brandy_constraints = null;
		Optional<Integer> useHeadText = XMLUtils.optionalIntegerAttribute(slot, SlotProperty.USE_HEAD_TEXT);
        ts.use_best_name = !(useHeadText.isPresent() && useHeadText.get() == 1);
		Optional<Integer> allowDesc = XMLUtils.optionalIntegerAttribute(slot, SlotProperty.ALLOW_DESC_TRAINING);
        ts.allow_desc_training = allowDesc.isPresent() && allowDesc.get() == 1;

		return ts;
	}
}
