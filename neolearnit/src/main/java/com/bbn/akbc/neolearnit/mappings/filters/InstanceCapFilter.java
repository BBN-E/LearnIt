package com.bbn.akbc.neolearnit.mappings.filters;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.mappings.filters.storage.CappedStorageFilter;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToSeedMapping;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;

public class InstanceCapFilter implements MappingsFilter {

	private final int cap;

	public InstanceCapFilter(int cap) {
		this.cap = cap;
	}

	@Override
	public Mappings makeFiltered(Mappings input) {
		if (cap > -1)
			System.out.println("Capping instances at "+cap+" for all seeds");
		InstanceToSeedMapping newSeedMapping =
				new InstanceToSeedMapping(
					new CappedStorageFilter<InstanceIdentifier,Seed>(-1,cap)
						.filter(input.getInstance2Seed().getStorage()));

		if (cap > -1)
			System.out.println("Capping instances at "+cap+" for all patterns");
		InstanceToPatternMapping newPatternMapping =
				new InstanceToPatternMapping(
					new CappedStorageFilter<InstanceIdentifier,LearnitPattern>(-1,cap)
						.filter(input.getInstance2Pattern().getStorage()));

		return new RelevantInstancesFilter().makeFiltered(new Mappings(newSeedMapping,newPatternMapping));
	}

}
