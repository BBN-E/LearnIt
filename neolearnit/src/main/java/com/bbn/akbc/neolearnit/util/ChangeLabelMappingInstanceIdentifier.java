package com.bbn.akbc.neolearnit.util;

import com.bbn.akbc.neolearnit.common.Annotation;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;
import com.bbn.serif.theories.Mention;

import java.io.File;

public class ChangeLabelMappingInstanceIdentifier {
    public static void main(String[] args) throws Exception{
        String inputLabelMappings = "/home/hqiu/ld100/learnit/inputs/legacy_unary_event_annotation/unary_event_wm.sjson";
        String outputLabelMappings = "/home/hqiu/ld100/learnit/inputs/legacy_unary_event_annotation/unary_event_wm_modified.sjson";
        Mappings labeledMappings = Mappings.deserialize(new File(inputLabelMappings), true);
        Annotation.InMemoryAnnotationStorage oldAnnotationStorage = new Annotation.InMemoryAnnotationStorage(labeledMappings);
        Annotation.InMemoryAnnotationStorage newAnnotationStorage = new Annotation.InMemoryAnnotationStorage();
        for(InstanceIdentifier oldInstanceIdentifier:oldAnnotationStorage.getAllInstanceIdentifier()){
            InstanceIdentifier newInstanceIdentifier = new InstanceIdentifier(oldInstanceIdentifier.getDocid(), oldInstanceIdentifier.getSentid(),oldInstanceIdentifier.getSlot0Start(), oldInstanceIdentifier.getSlot0End(), oldInstanceIdentifier.getSlot0SpanningType(),oldInstanceIdentifier.getSlotMentionType(0),
                    "Event", oldInstanceIdentifier.isSlotBestNameTypeName(0),
            oldInstanceIdentifier.getSlot1Start(), oldInstanceIdentifier.getSlot1End(),oldInstanceIdentifier.getSlot1SpanningType(),oldInstanceIdentifier.getSlotMentionType(1),
                    oldInstanceIdentifier.getSlotEntityType(1),oldInstanceIdentifier.isSlotBestNameTypeName(1));
            for(LabelPattern labelPattern: oldAnnotationStorage.lookupInstanceIdentifierAnnotation(oldInstanceIdentifier)){
                newAnnotationStorage.addAnnotation(newInstanceIdentifier,labelPattern);
            }
        }
        newAnnotationStorage.convertToMappings().serialize(new File(outputLabelMappings),true);
    }
}
