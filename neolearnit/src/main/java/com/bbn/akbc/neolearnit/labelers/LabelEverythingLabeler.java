package com.bbn.akbc.neolearnit.labelers;

import com.bbn.akbc.neolearnit.common.Annotation;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;

import java.util.HashSet;
import java.util.Set;

public class LabelEverythingLabeler implements MappingsLabeler {
    final String label;
    final Annotation.FrozenState frozenState;

    public LabelEverythingLabeler(String label, String frozenState) {
        this.label = label;
        this.frozenState = Annotation.FrozenState.valueOf(frozenState);
    }
    @Override
    public Annotation.InMemoryAnnotationStorage LabelMappings(Mappings original, Annotation.InMemoryAnnotationStorage labeledMappings) {
        Set<InstanceIdentifier> instanceIdentifierSet = new HashSet<>();
        instanceIdentifierSet.addAll(original.getPatternInstances());
        instanceIdentifierSet.addAll(original.getSeedInstances());

        for (InstanceIdentifier instanceIdentifier : instanceIdentifierSet) {
            labeledMappings.addAnnotation(instanceIdentifier, new LabelPattern(this.label, this.frozenState));
        }
        return labeledMappings;
    }

}
