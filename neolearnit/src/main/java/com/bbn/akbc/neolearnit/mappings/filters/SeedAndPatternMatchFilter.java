package com.bbn.akbc.neolearnit.mappings.filters;

import java.util.Set;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.mappings.filters.storage.MemberStorageFilter;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToSeedMapping;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;

public class SeedAndPatternMatchFilter implements MappingsFilter {

	private final Set<Seed> seeds;
	private final Set<LearnitPattern> patterns;

	public SeedAndPatternMatchFilter(Set<Seed> seeds, Set<LearnitPattern> patterns) {
		this.seeds = seeds;
		this.patterns = patterns;
	}

	@Override
	public Mappings makeFiltered(Mappings input) {
		System.out.println("Running seed pruning filters over "+seeds.size()+" seeds and "+patterns.size()+" patterns...");

		InstanceToSeedMapping newSeedMapping =
				new InstanceToSeedMapping(
					MemberStorageFilter.<InstanceIdentifier,Seed>fromRightSet(seeds)
						.filter(input.getInstance2Seed().getStorage()));

		InstanceToPatternMapping newCFMapping =
				new InstanceToPatternMapping(
					MemberStorageFilter.<InstanceIdentifier,LearnitPattern>fromRightSet(patterns)
						.filter(input.getInstance2Pattern().getStorage()));

		System.out.println(newSeedMapping.getStorage().size()+" seed entries.");
		System.out.println(newCFMapping.getStorage().size()+" pattern entries.");

		return new RelevantInstancesFilter().makeFiltered(new Mappings(newSeedMapping,newCFMapping));
	}

}
