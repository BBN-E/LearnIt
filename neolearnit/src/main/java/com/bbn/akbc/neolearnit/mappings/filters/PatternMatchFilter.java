package com.bbn.akbc.neolearnit.mappings.filters;

import java.util.HashSet;
import java.util.Set;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.mappings.filters.storage.MemberStorageFilter;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;

public class PatternMatchFilter implements MappingsFilter {

	private final Set<LearnitPattern> patterns;

	public PatternMatchFilter(Set<LearnitPattern> patterns) {
		this.patterns = patterns;
	}

	@Override
	public Mappings makeFiltered(Mappings input) {
		System.out.println("Matching against " + patterns.size() + " LearnitPattern.");

		Set<LearnitPattern> withPartials = new HashSet<LearnitPattern>(patterns);
		// Do not confuse the 'patterns' above (which are target or user defined patterns) with
		// input.getAllPatterns() which are instances/instantiated patterns
		for (LearnitPattern pattern : input.getAllPatterns()) {
			if (!pattern.isCompletePattern()) {
				withPartials.add(pattern);
			}
		}
		if (withPartials.size() - patterns.size() > 0) {
			System.out.println("Including " + (withPartials.size() - patterns.size()) + " incomplete pattern forms");
		}

		// now perform the filtering
		InstanceToPatternMapping newPatternMapping =
				new InstanceToPatternMapping(
					MemberStorageFilter.<InstanceIdentifier,LearnitPattern>fromRightSet(withPartials)
						.filter(input.getInstance2Pattern().getStorage()));

		//for(final InstanceIdentifier instance : newPatternMapping.getAllInstances()) {
		//	System.out.println(instance);
		//}
		//for(final LearnitPattern p : newPatternMapping.getAllPatterns()) {
		//	System.out.println(p);
		//}

		final Mappings relevantMapping = new RelevantInstancesFilter().makeFiltered( new Mappings(input.getInstance2Seed(), newPatternMapping) );
		//for(final LearnitPattern p : relevantMapping.getAllPatterns()) {
		//	System.out.println(p);
		//}

		return relevantMapping;
	}

}
