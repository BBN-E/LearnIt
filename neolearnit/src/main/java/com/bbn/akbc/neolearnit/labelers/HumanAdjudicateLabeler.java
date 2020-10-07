package com.bbn.akbc.neolearnit.labelers;

import com.bbn.akbc.neolearnit.common.Annotation;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.bue.common.symbols.Symbol;

import java.util.*;

public class HumanAdjudicateLabeler implements MappingsLabeler {

    final Annotation.InMemoryAnnotationStorage adjudicated;
    final List<LabelTrackingObserver> labelTrackingObserverList;

    public HumanAdjudicateLabeler(Annotation.InMemoryAnnotationStorage adjudicated){
        this.adjudicated = adjudicated;
        this.labelTrackingObserverList = new ArrayList<>();
    }

    public void addLabelTrackingObserver(LabelTrackingObserver labelTrackingObserver){
        this.labelTrackingObserverList.add(labelTrackingObserver);
    }

    @Override
    public Annotation.InMemoryAnnotationStorage LabelMappings(Mappings original, Annotation.InMemoryAnnotationStorage labeledMappings) throws Exception {
        for(InstanceIdentifier instanceIdentifier: this.adjudicated.getAllInstanceIdentifier()){
            for(LabelPattern labelPattern: this.adjudicated.lookupInstanceIdentifierAnnotation(instanceIdentifier)){
                if(labeledMappings.lookupInstanceIdentifierAnnotation(instanceIdentifier).size() < 1){
                    labeledMappings.addAnnotation(instanceIdentifier,labelPattern);
                    for(LabelTrackingObserver labelTrackingObserver: labelTrackingObserverList){
                        if(labelTrackingObserver.getLabelerTask().equals(LabelTrackingObserver.LabelerTask.pattern)){
                            labelTrackingObserver.addMarking(instanceIdentifier,labelPattern,Symbol.from("HumanLabeled"));
                        }
                    }
                }
                else{
                    Set<LabelPattern> pendingDeletePatterns = new HashSet<>();
                    for(LabelPattern labelPattern1: labeledMappings.lookupInstanceIdentifierAnnotation(instanceIdentifier)){
                        if(labelPattern1.getLabel().equals(labelPattern.getLabel()) && !labelPattern1.getFrozenState().equals(labelPattern.getFrozenState())){
                            pendingDeletePatterns.add(labelPattern1);
                        }
                    }
                    for(LabelPattern labelPattern1: pendingDeletePatterns){
                        int count = labeledMappings.countOfAppears(instanceIdentifier,labelPattern1);
                        labeledMappings.deleteAnnotationUnderLabelPattern(instanceIdentifier,labelPattern1,count);
                        for(LabelTrackingObserver labelTrackingObserver:labelTrackingObserverList){
                            if(labelTrackingObserver.getLabelerTask().equals(LabelTrackingObserver.LabelerTask.pattern)){
                                labelTrackingObserver.removeAllMarking(instanceIdentifier,labelPattern1);
                            }
                        }
                    }
                    int count = labeledMappings.countOfAppears(instanceIdentifier,labelPattern);
                    if(count < 1){
                        labeledMappings.addAnnotation(instanceIdentifier,labelPattern);
                        for(LabelTrackingObserver labelTrackingObserver: labelTrackingObserverList){
                            if(labelTrackingObserver.getLabelerTask().equals(LabelTrackingObserver.LabelerTask.pattern)){
                                labelTrackingObserver.addMarking(instanceIdentifier,labelPattern,Symbol.from("HumanLabeled"));
                            }
                        }
                    }
                }
            }
        }
        return labeledMappings;
    }

}
