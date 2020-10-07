package com.bbn.akbc.neolearnit.labelers;

import com.bbn.akbc.neolearnit.common.Annotation;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;

import java.util.Random;

public class DownSamplingLabelMappingsLabeler implements MappingsLabeler {
    static Random rand = new Random();
    final double NEGATIVE_SAMPLING_RATIO;
    final boolean onlyDownsampleNegative;
    public DownSamplingLabelMappingsLabeler(double NEGATIVE_SAMPLING_RATIO, boolean onlyDownsampleNegative){
        this.NEGATIVE_SAMPLING_RATIO = NEGATIVE_SAMPLING_RATIO;
        this.onlyDownsampleNegative = onlyDownsampleNegative;
    }

    boolean PassSampling() {
        return rand.nextInt(1000) <= 1000 * this.NEGATIVE_SAMPLING_RATIO;
    }


    @Override
    public Annotation.InMemoryAnnotationStorage LabelMappings(Mappings original, Annotation.InMemoryAnnotationStorage labeledMappings) throws Exception {
        Annotation.InMemoryAnnotationStorage outputInMemoryAnnotationStorage = new Annotation.InMemoryAnnotationStorage();
        for(InstanceIdentifier instanceIdentifier: labeledMappings.getAllInstanceIdentifier()){
            boolean shouldAttendDownSample = true;
            for(LabelPattern labelPattern: labeledMappings.lookupInstanceIdentifierAnnotation(instanceIdentifier)){
                if(onlyDownsampleNegative && labelPattern.getFrozenState().equals(Annotation.FrozenState.FROZEN_GOOD)){
                    shouldAttendDownSample = false;
                }
            }
            if (!shouldAttendDownSample || PassSampling()) {
                for(LabelPattern labelPattern:labeledMappings.lookupInstanceIdentifierAnnotation(instanceIdentifier)){
                    outputInMemoryAnnotationStorage.addAnnotation(instanceIdentifier,labelPattern);
                }
            }

        }
        return outputInMemoryAnnotationStorage;
    }
}
