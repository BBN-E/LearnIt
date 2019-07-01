package com.bbn.akbc.neolearnit.mappings.filters;

import java.util.Set;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.mappings.filters.storage.MemberStorageFilter;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToSeedMapping;
import com.bbn.akbc.neolearnit.observations.seed.Seed;

public class SeedMatchFilter implements MappingsFilter {

	private final Set<Seed> seeds;

	public SeedMatchFilter(Set<Seed> seeds) {
		this.seeds = seeds;
	}

	@Override
	public Mappings makeFiltered(Mappings input) {
		System.out.println("Matching "+seeds.size()+" seeds.");
		InstanceToSeedMapping newSeedMapping =
				new InstanceToSeedMapping(
					MemberStorageFilter.<InstanceIdentifier,Seed>fromRightSet(seeds)
						.filter(input.getInstance2Seed().getStorage()));

		System.out.println("Resulting mapping size: "+newSeedMapping.getStorage().size());

		return new RelevantInstancesFilter().makeFiltered(new Mappings(newSeedMapping,input.getInstance2Pattern()));
	}

}
