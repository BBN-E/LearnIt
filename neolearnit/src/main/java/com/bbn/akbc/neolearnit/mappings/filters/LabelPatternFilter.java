package com.bbn.akbc.neolearnit.mappings.filters;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.mappings.filters.storage.MemberStorageFilter;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;

import java.util.HashSet;
import java.util.Set;

public class LabelPatternFilter implements MappingsFilter{
    public final Set<LearnitPattern> patternsToKeep;
    public LabelPatternFilter(Mappings mappings) {
        patternsToKeep = new HashSet<>();
        for(LearnitPattern learnitPattern : mappings.getAllPatterns().elementSet()){
            if(!(learnitPattern instanceof LabelPattern))patternsToKeep.add(learnitPattern);
        }
    }

    @Override
    public Mappings makeFiltered(Mappings input) {
        InstanceToPatternMapping newPatternMapping =
                new InstanceToPatternMapping(
                        MemberStorageFilter.<InstanceIdentifier,LearnitPattern>fromRightSet(patternsToKeep)
                                .filter(input.getInstance2Pattern().getStorage()));

        final Mappings relevantMapping = new RelevantInstancesFilter().makeFiltered( new Mappings(input.getInstance2Seed(), newPatternMapping) );
        return relevantMapping;
    }
}
