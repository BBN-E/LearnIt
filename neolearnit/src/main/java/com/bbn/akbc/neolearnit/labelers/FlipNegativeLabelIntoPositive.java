package com.bbn.akbc.neolearnit.labelers;

import com.bbn.akbc.neolearnit.common.Annotation;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;
import com.bbn.bue.common.symbols.Symbol;

import java.util.*;

public class FlipNegativeLabelIntoPositive implements MappingsLabeler{
    final List<LabelTrackingObserver> labelTrackingObserverList;

    public FlipNegativeLabelIntoPositive(){
        this.labelTrackingObserverList = new ArrayList<>();
    }

    public void addLabelTrackingObserver(LabelTrackingObserver labelTrackingObserver){
        this.labelTrackingObserverList.add(labelTrackingObserver);
    }

    @Override
    public Annotation.InMemoryAnnotationStorage LabelMappings(Mappings original, Annotation.InMemoryAnnotationStorage labeledMappings) throws Exception {
        for(InstanceIdentifier instanceIdentifier:labeledMappings.getAllInstanceIdentifier()){
            Set<LabelPattern> pendingDeletePattern = new HashSet<>();
            for(LabelPattern labelPattern: labeledMappings.lookupInstanceIdentifierAnnotation(instanceIdentifier)){
                if(labelPattern.getFrozenState().equals(Annotation.FrozenState.FROZEN_BAD)){
                    pendingDeletePattern.add(labelPattern);
                }
            }
            for(LabelPattern labelPattern: pendingDeletePattern){
                int count = labeledMappings.countOfAppears(instanceIdentifier,labelPattern);
                labeledMappings.deleteAnnotationUnderLabelPattern(instanceIdentifier,labelPattern,count);
                LabelPattern newLabeledPattern = new LabelPattern("Negative_"+labelPattern.getLabel(), Annotation.FrozenState.FROZEN_GOOD);
                labeledMappings.addOrChangeAnnotation(instanceIdentifier,newLabeledPattern);
                for(LabelTrackingObserver labelTrackingObserver: labelTrackingObserverList){
                    if(labelTrackingObserver.getLabelerTask().equals(LabelTrackingObserver.LabelerTask.pattern)){
                        List<Symbol> oldSymbols = labelTrackingObserver.getMarkingFromInstanceIdentifierLabelMappings(instanceIdentifier,labelPattern);
                        labelTrackingObserver.removeAllMarking(instanceIdentifier,labelPattern);
                        for(Symbol symbol: oldSymbols){
                            labelTrackingObserver.addMarking(instanceIdentifier,newLabeledPattern,symbol);
                        }
                    }
                }
            }
        }
        return labeledMappings;
    }

}
