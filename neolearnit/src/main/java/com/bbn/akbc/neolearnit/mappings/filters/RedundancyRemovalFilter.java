package com.bbn.akbc.neolearnit.mappings.filters;

import java.util.*;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToSeedMapping;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.storage.MapStorage;
import com.bbn.akbc.neolearnit.storage.impl.HashMapStorage;
import com.bbn.akbc.utility.FileUtil;

public class RedundancyRemovalFilter implements MappingsFilter {

    public RedundancyRemovalFilter() {}

    @Override
    public Mappings makeFiltered(Mappings input) {
        System.out.println("Running the redundancy removal filter...");

        Set<LearnitPattern> learnitPatterns = input.getAllPatterns().elementSet();

        MapStorage.Builder<InstanceIdentifier,Seed> i2SeedStorage = new HashMapStorage.Builder();
        MapStorage.Builder<InstanceIdentifier,LearnitPattern> i2PatternStorage = new HashMapStorage.Builder();

        System.out.println("\tupdating instance2Seed map...");
        for(Seed seed : input.getAllSeeds().elementSet()) {
            Set<InstanceIdentifier> instanceIdentifiers = new HashSet<InstanceIdentifier>(input.getInstancesForSeed(seed));
            for(InstanceIdentifier instanceIdentifier : instanceIdentifiers) {
                i2SeedStorage.put(instanceIdentifier, seed);
            }
        }
        System.out.println("\tupdating instance2Pattern map...");
        for(LearnitPattern learnitPattern : input.getAllPatterns().elementSet()) {
            Set<InstanceIdentifier> instanceIdentifiers = new HashSet<InstanceIdentifier>(input.getInstancesForPattern(learnitPattern));
            for(InstanceIdentifier instanceIdentifier : instanceIdentifiers) {
                i2PatternStorage.put(instanceIdentifier, learnitPattern);
            }
        }

        InstanceToSeedMapping i2s = new InstanceToSeedMapping(i2SeedStorage.build());
        InstanceToPatternMapping i2p = new InstanceToPatternMapping(i2PatternStorage.build());

        Mappings finalMappings = new Mappings(i2s,i2p);

        return finalMappings;
    }

}
