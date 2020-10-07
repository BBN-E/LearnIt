package com.bbn.akbc.neolearnit.labelers;

import com.bbn.akbc.neolearnit.common.Annotation;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;

import java.util.HashSet;
import java.util.Set;

public class DropNALabeler implements MappingsLabeler{

    @Override
    public Annotation.InMemoryAnnotationStorage LabelMappings(Mappings original, Annotation.InMemoryAnnotationStorage labeledMappings) throws Exception {
        Set<InstanceIdentifier> pendingRemoveInstance = new HashSet<>();
        for(InstanceIdentifier instanceIdentifier :labeledMappings.getAllInstanceIdentifier()){
            for(LabelPattern labelPattern:labeledMappings.lookupInstanceIdentifierAnnotation(instanceIdentifier)){
                if(labelPattern.getLabel().equals("NA")){
                    pendingRemoveInstance.add(instanceIdentifier);
                    break;
                }
            }
        }
        for(InstanceIdentifier instanceIdentifier: pendingRemoveInstance){
            labeledMappings.deleteAllAnnotation(instanceIdentifier);
        }
        return labeledMappings;
    }
}
