package com.bbn.akbc.neolearnit.mappings.filters;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.mappings.filters.storage.MemberStorageFilter;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToSeedMapping;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.storage.MapStorage;
import com.google.common.collect.Sets;

import java.util.HashSet;
import java.util.Set;

public class RelevantInstancesFilter implements MappingsFilter {

	@Override
	public Mappings makeFiltered(Mappings input) {
//		System.out.println("Removing instances that are no longer relevant (no associated seed or pattern).");

		Set<InstanceIdentifier> relevantInstances = getRelevantInstances(input);
//				Sets.intersection(input.getInstance2Seed().getAllInstances().elementSet(),
//						input.getInstance2Pattern().getAllInstances().elementSet());
//
//		relevantInstances = withNonRestrictionPatternMatch(relevantInstances, input.getInstance2Pattern().getStorage());

//		System.out.println(relevantInstances.size()+" relevant instances");

		InstanceToSeedMapping instFiltSeedMapping =
				new InstanceToSeedMapping(
					MemberStorageFilter.<InstanceIdentifier,Seed>fromLeftSet(relevantInstances)
						.filter(input.getInstance2Seed().getStorage()));

//		System.out.println(instFiltSeedMapping.getAllSeeds().elementSet().size()+" relevant seeds");

		InstanceToPatternMapping instFiltPatternMapping =
				new InstanceToPatternMapping(
					MemberStorageFilter.<InstanceIdentifier,LearnitPattern>fromLeftSet(relevantInstances)
						.filter(input.getInstance2Pattern().getStorage()));

//		System.out.println(instFiltPatternMapping.getAllPatterns().elementSet().size()+" relevant patterns");

		return new Mappings(instFiltSeedMapping,instFiltPatternMapping);
	}

	public Set<InstanceIdentifier> getRelevantInstances(Mappings mappings) {
		return getRelevantInstances(mappings.getInstance2Seed().getStorage(), mappings.getInstance2Pattern().getStorage());
	}

	public Set<InstanceIdentifier> getRelevantInstances(MapStorage<InstanceIdentifier,Seed> i2s, MapStorage<InstanceIdentifier,LearnitPattern> i2p) {
		Set<InstanceIdentifier> relevantInstances = Sets.intersection(i2s.getLefts().elementSet(), i2p.getLefts().elementSet());
		return withNonRestrictionPatternMatch(relevantInstances, i2p);
	}

	private Set<InstanceIdentifier> withNonRestrictionPatternMatch(Set<InstanceIdentifier> instances, MapStorage<InstanceIdentifier,LearnitPattern> i2p) {

		Set<InstanceIdentifier> relevantInstances = new HashSet<InstanceIdentifier>();

		for (InstanceIdentifier id : instances) {
			for (LearnitPattern pattern : i2p.getRight(id)) {
				if (pattern.isCompletePattern()) {
					relevantInstances.add(id);
					break;
				}
			}
		}

		return relevantInstances;
	}


}
