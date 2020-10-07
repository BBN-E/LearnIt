package com.bbn.akbc.neolearnit.util;

import com.bbn.akbc.neolearnit.common.Annotation;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class PruneLableMappingsForMixingNAAndOther {
    public static void main(String[] args) throws Exception{
        String params = args[0];
        LearnItConfig.loadParams(new File(params));

        String labeledMappingsPath = args[1];
        String outputLabeledMappingsPath = args[2];
        Annotation.InMemoryAnnotationStorage inMemoryAnnotationStorage = new Annotation.InMemoryAnnotationStorage(Mappings.deserialize(new File(labeledMappingsPath),true));
        Set<InstanceIdentifier> instanceIdentifierSet = new HashSet<>(inMemoryAnnotationStorage.getAllInstanceIdentifier());
        for(InstanceIdentifier instanceIdentifier:inMemoryAnnotationStorage.getAllInstanceIdentifier()){
            System.out.println(instanceIdentifier.toShortString());
            for(LabelPattern labelPattern: inMemoryAnnotationStorage.lookupInstanceIdentifierAnnotation(instanceIdentifier)){
                System.out.println(labelPattern);
            }
            System.out.println("######");
        }
        for(InstanceIdentifier instanceIdentifier: instanceIdentifierSet){
            if(inMemoryAnnotationStorage.lookupInstanceIdentifierAnnotation(instanceIdentifier).size() > 1){
                inMemoryAnnotationStorage.deleteAnnotationUnderLabelPattern(instanceIdentifier,new LabelPattern("NA", Annotation.FrozenState.NO_FROZEN));
                inMemoryAnnotationStorage.deleteAnnotationUnderLabelPattern(instanceIdentifier,new LabelPattern("NA", Annotation.FrozenState.FROZEN_BAD));
            }
        }
        System.out.println("####################################");
        for(InstanceIdentifier instanceIdentifier:inMemoryAnnotationStorage.getAllInstanceIdentifier()){
            System.out.println(instanceIdentifier.toShortString());
            for(LabelPattern labelPattern: inMemoryAnnotationStorage.lookupInstanceIdentifierAnnotation(instanceIdentifier)){
                System.out.println(labelPattern);
            }
            System.out.println("######");
        }
        inMemoryAnnotationStorage.convertToMappings().serialize(new File(outputLabeledMappingsPath),true);
    }
}
