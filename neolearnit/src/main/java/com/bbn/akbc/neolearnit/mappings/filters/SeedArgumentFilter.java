package com.bbn.akbc.neolearnit.mappings.filters;

import com.bbn.akbc.common.Pair;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.relations.utility.Utility;

import java.util.HashSet;
import java.util.Set;


public class SeedArgumentFilter implements MappingsFilter {

	private final Set<String> argDictionary;

	public SeedArgumentFilter(Set<String> argDictionary) {
		this.argDictionary = argDictionary;
	}


	@Override
	public Mappings makeFiltered(Mappings input) {
		Set<Seed> seeds = new HashSet<Seed>();
		Set<LearnitPattern> patterns = new HashSet<LearnitPattern>();

		for (LearnitPattern pattern : input.getInstance2Pattern().getAllPatterns().elementSet()) {
			patterns.add(pattern);
		}

		for(Seed seed : input.getInstance2Seed().getAllSeeds().elementSet()) {
			Pair<String, String> pairOfStr = Utility.getEntityPairString(seed);
			if(argDictionary.contains(pairOfStr.getFirst()) || argDictionary.contains(pairOfStr.getSecond()))
				seeds.add(seed);
		}

		Mappings resultedMapping = new SeedAndPatternMatchFilter(seeds, patterns).makeFiltered(input);

		return resultedMapping;
	}

}
