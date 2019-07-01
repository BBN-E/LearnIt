package com.bbn.akbc.neolearnit.common.targets;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.bbn.bue.common.xml.XMLUtils;
import com.bbn.akbc.neolearnit.common.targets.properties.TargetProperty;
import com.bbn.akbc.utility.Pair;
import com.google.common.base.Optional;

public class TargetSlotPairDefinition {

	public Integer A;
	public Integer B;
	public Boolean symmetric;
	public Integer max_as_per_b;
	public Boolean must_corefer;
	public Boolean must_not_corefer;
	public List<Pair<String, String>> allowed_type_pairs;
	public String no_overlap_setting;

	/*
	 * TargetSlotPairDefinition is a class adapted from learnit's TargetSlotPair class,
	 * and is used in neolearnit only to help parse xml target definition files
	 * formatted for learnit.
	 */
	public static TargetSlotPairDefinition fromElement(Element slotPair) {

		TargetSlotPairDefinition tsp = new TargetSlotPairDefinition();
		tsp.A = XMLUtils.requiredIntegerAttribute(slotPair, "a");
		tsp.B = XMLUtils.requiredIntegerAttribute(slotPair, "b");

		Optional<Integer> symmetric = XMLUtils.optionalIntegerAttribute(slotPair, TargetProperty.SYMMETRIC);
		if (symmetric.isPresent() && symmetric.get() == 1)
			tsp.symmetric = true;
		else
			tsp.symmetric = false;

		Optional<Integer> maxAsPerB = XMLUtils.optionalIntegerAttribute(slotPair, "max_as_per_b");
		if (maxAsPerB.isPresent())
			tsp.max_as_per_b = maxAsPerB.get();
		else
			tsp.max_as_per_b = null;

		Optional<Integer> mustCorefer = XMLUtils.optionalIntegerAttribute(slotPair, "must_corefer");
		if (mustCorefer.isPresent() && mustCorefer.get() == 1)
			tsp.must_corefer = true;
		else
			tsp.must_corefer = false;

		Optional<Integer> mustNotCorefer = XMLUtils.optionalIntegerAttribute(slotPair, "must_not_corefer");
		if (mustNotCorefer.isPresent() && mustNotCorefer.get() == 1)
			tsp.must_not_corefer = true;
		else
			tsp.must_not_corefer = false;

		Optional<String> noOverlapSetting = XMLUtils.optionalStringAttribute(slotPair, "no_overlap");
		if (noOverlapSetting.isPresent())
			tsp.no_overlap_setting = noOverlapSetting.get();
		else
			tsp.no_overlap_setting = "";

		if(tsp.must_corefer && tsp.must_not_corefer)
			throw new RuntimeException("Cannot have both must_corefer and must_not_corefer enabled! Impossible case!");

		tsp.allowed_type_pairs = null;

		for (Node child = slotPair.getFirstChild(); child!=null; child = child.getNextSibling()) {
			if (child instanceof Element) {
				String tag = ((Element)child).getTagName();
				if (tag.equals("type_pair")) {
					if (tsp.allowed_type_pairs == null)
						tsp.allowed_type_pairs = new ArrayList<Pair<String,String>>();

					Optional<String> atypes = XMLUtils.optionalStringAttribute((Element)child, "atypes");
					Optional<String> btypes = XMLUtils.optionalStringAttribute((Element)child, "btypes");
					if (!atypes.isPresent())
						atypes = Optional.of("*");
					if (!btypes.isPresent())
						btypes = Optional.of("*");

					for (String atype : atypes.get().split(" ")) {
						for (String btype : btypes.get().split(" ")) {
							tsp.allowed_type_pairs.add(new Pair<String, String>(atype, btype));
						}
					}
				}
			}
		}

		return tsp;
	}
}
