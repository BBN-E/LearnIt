package com.bbn.akbc.neolearnit.mappings.filters;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.mappings.filters.storage.FrequencyLimitStorageFilter;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToSeedMapping;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;

public class FrequencyLimitFilter implements MappingsFilter {

	private final int seedMin;
	private final int seedMax;
	private final int patternMin;
	private final int patternMax;

	public FrequencyLimitFilter(int seedMin, int seedMax, int patternMin, int patternMax) {
		this.seedMin = seedMin;
		this.seedMax = seedMax;
		this.patternMin = patternMin;
		this.patternMax = patternMax;
	}


	@Override
	public Mappings makeFiltered(Mappings input) {
		if (seedMin > -1)
			System.out.println("Filtering out seeds that occur fewer than "+seedMin+" times.");
		if (seedMax > -1)
			System.out.println("Filtering out seeds that occur more than "+seedMax+" times.");
		InstanceToSeedMapping newSeedMapping =
				new InstanceToSeedMapping(
					new FrequencyLimitStorageFilter<InstanceIdentifier,Seed>(-1,-1,seedMin,seedMax)
						.filter(input.getInstance2Seed().getStorage()));

		if (patternMin > -1)
			System.out.println("Filtering out patterns that occur fewer than "+patternMin+" times.");
		if (patternMax > -1)
			System.out.println("Filtering out patterns that occur more than "+patternMax+" times.");
		InstanceToPatternMapping newPatternMapping =
				new InstanceToPatternMapping(
					new FrequencyLimitStorageFilter<InstanceIdentifier,LearnitPattern>(-1,-1,patternMin,patternMax)
						.filter(input.getInstance2Pattern().getStorage()));

		return new RelevantInstancesFilter().makeFiltered(new Mappings(newSeedMapping,newPatternMapping));
	}
}
