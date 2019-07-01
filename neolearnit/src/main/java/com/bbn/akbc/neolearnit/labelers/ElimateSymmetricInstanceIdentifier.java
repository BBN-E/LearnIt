package com.bbn.akbc.neolearnit.labelers;

import com.bbn.akbc.neolearnit.common.Annotation;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.MinimumInstanceIdentifier;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ElimateSymmetricInstanceIdentifier implements MappingLabeler {
    // Sometimes, we want to ignore directionality. This is why here it is.

    public static void main(String[] args) throws Exception {
        String paramPath = args[0];
        LearnItConfig.loadParams(new File(paramPath));
        String inputMappingsPath = args[1];
        String outputMappingsPath = args[2];

        Mappings original = Mappings.deserialize(new File(inputMappingsPath), true);

        ElimateSymmetricInstanceIdentifier elimateSymmetricInstanceIdentifier = new ElimateSymmetricInstanceIdentifier();

        Mappings newMappings = elimateSymmetricInstanceIdentifier.LabelMappings(original);
        newMappings.serialize(new File(outputMappingsPath), true);
    }


    @Override
    public Mappings LabelMappings(Mappings original) {
        Map<MinimumInstanceIdentifier, Set<InstanceIdentifier>> instanceIdentifierMap = new HashMap<>();
        Set<InstanceIdentifier> originalMappingsAllInstanceIdentifier = new HashSet<>();
        originalMappingsAllInstanceIdentifier.addAll(original.getPatternInstances());
        originalMappingsAllInstanceIdentifier.addAll(original.getSeedInstances());
        for (InstanceIdentifier instanceIdentifier : originalMappingsAllInstanceIdentifier) {
            int minSlotStart;
            int minSlotEnd;
            int maxSlotStart;
            int maxSlotEnd;
            if (instanceIdentifier.getSlot0Start() < instanceIdentifier.getSlot1Start()) {
                minSlotStart = instanceIdentifier.getSlot0Start();
                minSlotEnd = instanceIdentifier.getSlot0End();
                maxSlotStart = instanceIdentifier.getSlot1Start();
                maxSlotEnd = instanceIdentifier.getSlot1End();
            } else {
                minSlotStart = instanceIdentifier.getSlot1Start();
                minSlotEnd = instanceIdentifier.getSlot1End();
                maxSlotStart = instanceIdentifier.getSlot0Start();
                maxSlotEnd = instanceIdentifier.getSlot0End();
            }


            MinimumInstanceIdentifier minimumInstanceIdentifier = new MinimumInstanceIdentifier(instanceIdentifier.getDocid(), instanceIdentifier.getSentid(), minSlotStart, minSlotEnd, maxSlotStart, maxSlotEnd);
            Set<InstanceIdentifier> buf = instanceIdentifierMap.getOrDefault(minimumInstanceIdentifier, new HashSet<>());
            buf.add(instanceIdentifier);
            instanceIdentifierMap.put(minimumInstanceIdentifier, buf);
        }

        Annotation.InMemoryAnnotationStorage inMemoryAnnotationStorage = new Annotation.InMemoryAnnotationStorage();

        for (MinimumInstanceIdentifier minimumInstanceIdentifier : instanceIdentifierMap.keySet()) {
            InstanceIdentifier instanceIdentifier = instanceIdentifierMap.get(minimumInstanceIdentifier).iterator().next();
            inMemoryAnnotationStorage.addAnnotation(instanceIdentifier, new LabelPattern("NO_RELATION", Annotation.FrozenState.NO_FROZEN));
        }
        System.out.println("Originally we have " + originalMappingsAllInstanceIdentifier.size() + " instanceIdentifiers, now it's " + inMemoryAnnotationStorage.getAllInstanceIdentifier().size());
        return inMemoryAnnotationStorage.convertToMappings();
    }
}
