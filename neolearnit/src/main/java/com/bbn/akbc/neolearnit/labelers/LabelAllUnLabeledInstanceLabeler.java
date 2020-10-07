package com.bbn.akbc.neolearnit.labelers;

import com.bbn.akbc.neolearnit.common.Annotation;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.google.common.collect.Sets;

import java.util.*;

public class LabelAllUnLabeledInstanceLabeler implements MappingsLabeler{
    @Override
    public Annotation.InMemoryAnnotationStorage LabelMappings(Mappings original, Annotation.InMemoryAnnotationStorage labeledMappings) throws Exception {
        Set<InstanceIdentifier> markedInstances = new HashSet<>(labeledMappings.getAllInstanceIdentifier());
        Set<InstanceIdentifier> allInstances = new HashSet<>(original.getPatternInstances());
        allInstances.addAll(original.getSeedInstances());
        Set<InstanceIdentifier> untouchedInstances = Sets.difference(allInstances,markedInstances);
        for(InstanceIdentifier instanceIdentifier: untouchedInstances){
            labeledMappings.addAnnotation(instanceIdentifier,new LabelPattern("incomplete", Annotation.FrozenState.FROZEN_BAD));
        }
        return labeledMappings;
    }
}
