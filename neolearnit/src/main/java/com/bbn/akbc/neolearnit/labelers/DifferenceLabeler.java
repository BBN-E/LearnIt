package com.bbn.akbc.neolearnit.labelers;

import com.bbn.akbc.neolearnit.common.Annotation;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class DifferenceLabeler implements MappingLabeler {
    final Mappings smallMappings;


    public DifferenceLabeler(Mappings smallMappings) {
        this.smallMappings = smallMappings;
    }

    // The purpose of this Labeler is to to set difference. The input should be a big mappings(either auto populated or a big labeled mappings, as S, plus a labeled mappings as T.the output will be a small labeled mappings which is S-T
    public static void main(String[] args) throws Exception {
        String paramPath = args[0];
        LearnItConfig.loadParams(new File(paramPath));
        String setSMappingsPath = args[1];
        String setTMappingsPath = args[2];
        String outputMappingPath = args[3];
        DifferenceLabeler differenceLabeler = new DifferenceLabeler(Mappings.deserialize(new File(setTMappingsPath), true));
        Mappings result = differenceLabeler.LabelMappings(Mappings.deserialize(new File(setSMappingsPath), true));
        result.serialize(new File(outputMappingPath), true);
    }

    @Override
    public Mappings LabelMappings(Mappings original) {
        Set<InstanceIdentifier> shouldIgnoreDueToInSmallMappings = new HashSet<>();
        shouldIgnoreDueToInSmallMappings.addAll(this.smallMappings.getSeedInstances());
        shouldIgnoreDueToInSmallMappings.addAll(this.smallMappings.getPatternInstances());
        Annotation.InMemoryAnnotationStorage inMemoryAnnotationStorage = new Annotation.InMemoryAnnotationStorage();
        Set<InstanceIdentifier> instanceIdentifierinOriginal = new HashSet<>();
        instanceIdentifierinOriginal.addAll(original.getPatternInstances());
        instanceIdentifierinOriginal.addAll(original.getSeedInstances());
        int count = 0;
        for (InstanceIdentifier instanceIdentifier : instanceIdentifierinOriginal) {
            if (shouldIgnoreDueToInSmallMappings.contains(instanceIdentifier)) continue;
            inMemoryAnnotationStorage.addAnnotation(instanceIdentifier, new LabelPattern("NO_RELATION", Annotation.FrozenState.NO_FROZEN));
            count++;
        }

        System.out.println("Originally we have " + instanceIdentifierinOriginal.size() + " instanceIdentifiers, now it filtered down to " + count);
        return inMemoryAnnotationStorage.convertToMappings();
    }
}
