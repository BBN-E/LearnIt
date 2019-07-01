package com.bbn.akbc.neolearnit.observations.pattern.restriction;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.BeforeAfterSlotsPattern.BeforeAfterSlotsRestrictionFactory;
import com.bbn.akbc.neolearnit.observations.pattern.restriction.Restriction.RestrictionFactory;
import com.bbn.akbc.neolearnit.observations.pattern.restriction.SlotContainsWordRestriction.SlotContainsWordRestrictionFactory;
import com.bbn.akbc.neolearnit.observations.pattern.restriction.SlotEntityTypeRestriction.SlotEntityTypeRestrictionFactory;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.google.common.base.Optional;

/**
 * Centralized utility class to apply restrictions to patterns.
 * @author mcrivaro
 *
 */
public class RestrictionSuite {

	public static Collection<RestrictionFactory> getRestrictionFactories(TargetAndScoreTables data, Mappings mappings) {

		Set<RestrictionFactory> toReturn = new HashSet<RestrictionFactory>();

		if (LearnItConfig.optionalParamTrue("use_slot_etype_restrictions")) {
			toReturn.add(new SlotEntityTypeRestrictionFactory(data,mappings));
		}

		if (LearnItConfig.optionalParamTrue("use_slot_contains_word_restrictions")) {
			toReturn.add(new SlotContainsWordRestrictionFactory(data,mappings));
		}

		toReturn.add(new BeforeAfterSlotsRestrictionFactory(data,mappings));

		return toReturn;
	}

	public static Restriction fromIDString(String idString) {
		Optional<? extends Restriction> etypeRes = SlotEntityTypeRestriction.fromIDString(idString);
		if (etypeRes.isPresent()) {
			return etypeRes.get();
		}

		Optional<? extends Restriction> conWordRes = SlotContainsWordRestriction.fromIDString(idString);
		if (conWordRes.isPresent()) {
			return conWordRes.get();
		}

		throw new RuntimeException("Unknown ID String " + idString + "!");
	}
}
