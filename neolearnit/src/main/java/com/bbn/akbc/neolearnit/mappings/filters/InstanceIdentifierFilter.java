package com.bbn.akbc.neolearnit.mappings.filters;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToSeedMapping;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.storage.MapStorage;
import com.bbn.akbc.neolearnit.util.InstanceIdentifierFilterForAnnotation;

import java.util.HashSet;
import java.util.Set;

public class InstanceIdentifierFilter implements MappingsFilter{
    //@hqiu: This class exist because, sometimes, we'll create "duplicated" Instanceidentifier due to upstream handling,
    // Say, we add a word with different event type. But the tokenspan is exact the same.

    @Override
    public Mappings makeFiltered(Mappings input) {
        MapStorage.Builder<InstanceIdentifier, Seed> instanceToSeedMapping = input.getInstance2Seed().getStorage().newBuilder();
        MapStorage.Builder<InstanceIdentifier, LearnitPattern> instanceToPatternMapping = input.getInstance2Pattern().getStorage().newBuilder();
        Set<InstanceIdentifier> instanceIdentifierSet = new HashSet<>(input.getPatternInstances());
        instanceIdentifierSet.addAll(input.getSeedInstances());
        instanceIdentifierSet = new HashSet<>(InstanceIdentifierFilterForAnnotation.makeFiltered(instanceIdentifierSet));
        for(InstanceIdentifier instanceIdentifier:input.getPatternInstances()){
            if(instanceIdentifierSet.contains(instanceIdentifier)){
                for(LearnitPattern learnitPattern:input.getPatternsForInstance(instanceIdentifier)){
                    instanceToPatternMapping.put(instanceIdentifier,learnitPattern);
                }
            }
        }
        for(InstanceIdentifier instanceIdentifier:input.getSeedInstances()){
            if(instanceIdentifierSet.contains(instanceIdentifier)){
                for(Seed seed:input.getSeedsForInstance(instanceIdentifier)){
                    instanceToSeedMapping.put(instanceIdentifier,seed);
                }
            }
        }
        Mappings newMappings =  new Mappings(instanceToSeedMapping.build(),instanceToPatternMapping.build());
        System.out.println(newMappings.getSeedInstances().size());
        return newMappings;
    }

}
