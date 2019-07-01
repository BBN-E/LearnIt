package com.bbn.akbc.neolearnit.mappings.filters;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToSeedMapping;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.storage.MapStorage.Builder;

public class NormalizeSeedsFilter implements MappingsFilter {

	private final Target target;

	public NormalizeSeedsFilter(Target target) {
		this.target = target;
	}

	@Override
	public Mappings makeFiltered(Mappings input) {

		InstanceToSeedMapping seedMapping = input.getInstance2Seed();
		Builder<InstanceIdentifier, Seed> properTextSeedBuilder = seedMapping.getStorage().newBuilder();
		for (Seed seed : seedMapping.getAllSeeds().elementSet()) {
			Seed newSeed = seed.withProperText(target);
			if (target.isSymmetric()) newSeed = newSeed.makeSymmetric();
			for (InstanceIdentifier id : seedMapping.getInstances(seed)) {
				properTextSeedBuilder.put(id, newSeed);
			}
		}

		return new Mappings(new InstanceToSeedMapping(properTextSeedBuilder.build()), input.getInstance2Pattern());
	}


}
