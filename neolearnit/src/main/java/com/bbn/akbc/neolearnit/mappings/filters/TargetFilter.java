package com.bbn.akbc.neolearnit.mappings.filters;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.mappings.filters.storage.MemberStorageFilter;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToSeedMapping;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.storage.MapStorage.Builder;
import com.google.common.collect.Sets;

import java.util.HashSet;
import java.util.Set;

public class TargetFilter implements MappingsFilter {

    private final Target target;

    public TargetFilter(Target t) {
        this.target = t;
    }

    @Override
    public Mappings makeFiltered(Mappings input) {
        InstanceToSeedMapping seedMapping = input.getInstance2Seed();
        InstanceToPatternMapping patternMapping = input.getInstance2Pattern();

        Set<InstanceIdentifier> relevantInstances;
        if (LearnItConfig.optionalParamTrue("running_test_set")) {
            relevantInstances = seedMapping.getAllInstances().elementSet();
        } else {
            System.out.println(
                    "Removing instances that are no longer relevant (no associated seed or pattern)...");
            relevantInstances = Sets.intersection(seedMapping.getAllInstances().elementSet(),
                    patternMapping.getAllInstances().elementSet());
        }
        relevantInstances = getValidInstances(relevantInstances, seedMapping);
        System.out.println(
                "Filtering by target constraints... " + relevantInstances.size() + " valid instances");

        seedMapping = new InstanceToSeedMapping(
                MemberStorageFilter.<InstanceIdentifier, Seed>fromLeftSet(relevantInstances)
                        .filter(seedMapping.getStorage()));
        patternMapping = new InstanceToPatternMapping(
                MemberStorageFilter.<InstanceIdentifier, LearnitPattern>fromLeftSet(relevantInstances)
                        .filter(patternMapping.getStorage()));

        //Symmetrize the patterns if neccessary before returning
        return new Mappings(seedMapping, withFormattedPatterns(patternMapping));
    }

    private Set<InstanceIdentifier> getValidInstances(Set<InstanceIdentifier> instances,
                                                      InstanceToSeedMapping seedMapping) {
        Set<InstanceIdentifier> targetMatchInstances = new HashSet<InstanceIdentifier>();
        for (InstanceIdentifier id : instances) {
            if (this.target.validInstance(id, seedMapping.getSeeds(id))) {
                targetMatchInstances.add(id);
            }
//		  	else
//			  System.out.println("Filtered by TargetFilter: " + id.toString());
        }
        return targetMatchInstances;
    }

    private InstanceToPatternMapping withFormattedPatterns(InstanceToPatternMapping patternMapping) {
        if (target.isSymmetric()) {
            System.out.println("Throwing out non canonically ordered patterns for symmetry...");
            Builder<InstanceIdentifier, LearnitPattern> newPatBuilder =
                    patternMapping.getStorage().newBuilder();
            for (LearnitPattern pattern : patternMapping.getAllPatterns().elementSet()) {
                if (pattern.isInCanonicalSymmetryOrder()) {
                    for (InstanceIdentifier id : patternMapping.getInstances(pattern)) {
                        newPatBuilder.put(id, pattern);
                    }
                }
            }
            System.out.println("Done!");

            return new InstanceToPatternMapping(newPatBuilder.build());
        } else {
            return patternMapping;
        }
    }
}
