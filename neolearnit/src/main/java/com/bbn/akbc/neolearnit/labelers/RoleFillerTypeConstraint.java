package com.bbn.akbc.neolearnit.labelers;

import com.bbn.akbc.neolearnit.common.Annotation;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;
import com.google.common.collect.ImmutableSet;

import java.util.HashSet;
import java.util.Set;

public class RoleFillerTypeConstraint implements MappingsLabeler {
    final ImmutableSet<String> slot0AllowedEntityType;
    final ImmutableSet<String> slot1AllowedEntityType;
    final boolean enforceOnSlot0;
    final boolean enforceOnSlot1;

    public RoleFillerTypeConstraint(String slot0AllowedEntityType, String slot1AllowedEntityType, boolean enforceSlot0, boolean enforceSlot1) {
        this.enforceOnSlot0 = enforceSlot0;
        this.enforceOnSlot1 = enforceSlot1;
        Set<String> s0EntityTypes = new HashSet<>();
        if (slot0AllowedEntityType.equals("all")) {
            s0EntityTypes.add("all");
        } else {
            for (String entityTypeStr : slot0AllowedEntityType.split(",")) {
                s0EntityTypes.add(entityTypeStr);
            }
        }
        this.slot0AllowedEntityType = ImmutableSet.copyOf(s0EntityTypes);
        Set<String> s1EntityTypes = new HashSet<>();
        if (slot1AllowedEntityType.equals("all")) {
            s1EntityTypes.add("all");
        } else {
            for (String entityTypeStr : slot1AllowedEntityType.split(",")) {
                s1EntityTypes.add(entityTypeStr);
            }
        }
        this.slot1AllowedEntityType = ImmutableSet.copyOf(s1EntityTypes);
    }

    public boolean passLeft(InstanceIdentifier instanceIdentifier) {
        if (!enforceOnSlot0) return true;
        else {
            if (!instanceIdentifier.getSlot0SpanningType().equals(InstanceIdentifier.SpanningType.Mention) && !instanceIdentifier.getSlot0SpanningType().equals(InstanceIdentifier.SpanningType.ValueMention))
                return false;
            if (slot0AllowedEntityType.contains("all")) return true;
            return slot0AllowedEntityType.contains(instanceIdentifier.getSlotEntityType(0));
        }
    }

    public boolean passRight(InstanceIdentifier instanceIdentifier) {
        if (!enforceOnSlot1) return true;
        else {
            if (!instanceIdentifier.getSlot1SpanningType().equals(InstanceIdentifier.SpanningType.Mention) && !instanceIdentifier.getSlot1SpanningType().equals(InstanceIdentifier.SpanningType.ValueMention))
                return false;
            if (slot1AllowedEntityType.contains("all")) return true;
            return slot1AllowedEntityType.contains(instanceIdentifier.getSlotEntityType(1));
        }
    }

    @Override
    public Annotation.InMemoryAnnotationStorage LabelMappings(Mappings original, Annotation.InMemoryAnnotationStorage labeledMappings) throws Exception {
        Annotation.InMemoryAnnotationStorage outLabelMappings = new Annotation.InMemoryAnnotationStorage();
        for (InstanceIdentifier instanceIdentifier : labeledMappings.getAllInstanceIdentifier()) {
            if (passLeft(instanceIdentifier) && passRight(instanceIdentifier)) {
                for (LabelPattern labelPattern : labeledMappings.lookupInstanceIdentifierAnnotation(instanceIdentifier)) {
                    outLabelMappings.addAnnotation(instanceIdentifier, labelPattern);
                }
            }
        }
        return outLabelMappings;
    }
}
