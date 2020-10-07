package com.bbn.akbc.neolearnit.labelers;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;
import com.bbn.bue.common.symbols.Symbol;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import java.util.*;

public class LabelTrackingObserver{
    final Map<InstanceIdentifier, Map<LabelPattern, List<Symbol>>> instanceIdentifierToLabeledPatternToLabel;

    public static enum LabelerTask{
        pattern,
        triggerText
    }

    final LabelerTask labelerTask;
    public LabelTrackingObserver(LabelerTask labelerTask){
        this.instanceIdentifierToLabeledPatternToLabel = new HashMap<>();
        this.labelerTask = labelerTask;
    }

    public void removeAllMarking(InstanceIdentifier instanceIdentifier, LabelPattern labelPattern) {
        if(this.instanceIdentifierToLabeledPatternToLabel.containsKey(instanceIdentifier)){
            this.instanceIdentifierToLabeledPatternToLabel.get(instanceIdentifier).remove(labelPattern);
        }
    }

    public void removeSpecificMarking(InstanceIdentifier instanceIdentifier, LabelPattern labelPattern, Symbol marking){
        if(this.instanceIdentifierToLabeledPatternToLabel.containsKey(instanceIdentifier)){
            if(this.instanceIdentifierToLabeledPatternToLabel.get(instanceIdentifier).containsKey(labelPattern)){
                this.instanceIdentifierToLabeledPatternToLabel.get(instanceIdentifier).get(labelPattern).removeAll(Lists.newArrayList(marking));
            }
        }
    }

    public void addMarking(InstanceIdentifier instanceIdentifier, LabelPattern labelPattern, Symbol marking) {
        Map<LabelPattern,List<Symbol>> buf1 = this.instanceIdentifierToLabeledPatternToLabel.getOrDefault(instanceIdentifier,new HashMap<>());
        List<Symbol> buf2 = buf1.getOrDefault(labelPattern,new ArrayList<>());
        buf2.add(marking);
        buf1.put(labelPattern,buf2);
        this.instanceIdentifierToLabeledPatternToLabel.put(instanceIdentifier,buf1);
    }

    public Map<LabelPattern,List<Symbol>> getMarkingFromInstanceIdentifier(InstanceIdentifier instanceIdentifier){
        return this.instanceIdentifierToLabeledPatternToLabel.getOrDefault(instanceIdentifier, new HashMap<>());
    }

    public List<Symbol> getMarkingFromInstanceIdentifierLabelMappings(InstanceIdentifier instanceIdentifier, LabelPattern labelPattern){
        return this.getMarkingFromInstanceIdentifier(instanceIdentifier).getOrDefault(labelPattern,new ArrayList<>());
    }

    public Map<InstanceIdentifier, Map<LabelPattern, List<Symbol>>> getInstanceIdentifierToLabeledPatternToLabel(){
        return this.instanceIdentifierToLabeledPatternToLabel;
    }

    public LabelerTask getLabelerTask(){
        return this.labelerTask;
    }
}
