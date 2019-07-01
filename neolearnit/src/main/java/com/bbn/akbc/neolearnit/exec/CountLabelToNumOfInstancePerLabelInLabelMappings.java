package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.neolearnit.common.Annotation;
import com.bbn.akbc.neolearnit.common.BBNInternalOntology;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;
import com.bbn.akbc.neolearnit.serializers.ExternalAnnotationBuilder;
import com.bbn.akbc.neolearnit.storage.StorageUtils;
import com.bbn.serif.theories.EventMention;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.Token;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.bbn.akbc.neolearnit.common.matchinfo.MatchInfoDisplay.sanitize;

public class CountLabelToNumOfInstancePerLabelInLabelMappings extends ExternalAnnotationBuilder {

    final String outputPath;

    public CountLabelToNumOfInstancePerLabelInLabelMappings(String outputPath){
        this.outputPath = outputPath;
    }

    public static void main(String[] args) throws Exception {
        String paramPath = args[0];
        LearnItConfig.loadParams(new File(paramPath));
        String labelledMappingsPath = args[1];
        String outputFile = args[2];
        Mappings labeledMappings = Mappings.deserialize(new File(labelledMappingsPath), true);
        CountLabelToNumOfInstancePerLabelInLabelMappings countLabelToNumOfInstancePerLabelInLabelMappings = new CountLabelToNumOfInstancePerLabelInLabelMappings(outputFile);
        countLabelToNumOfInstancePerLabelInLabelMappings.observe(labeledMappings);
        countLabelToNumOfInstancePerLabelInLabelMappings.build();
    }

    @Override
    public void build() throws Exception {
        Map<String,Map<String,List<String>>> eventTypeToTriggerToSentenceSpanMap = new HashMap<>();
        Map<InstanceIdentifier, SentenceTheory> instanceIdentifierSentenceTheoryMap = this.resolveSentenceTheory();
        for(InstanceIdentifier instanceIdentifier:this.inMemoryAnnotationStorage.getAllInstanceIdentifier()){
            for(LabelPattern labelPattern:this.inMemoryAnnotationStorage.lookupInstanceIdentifierAnnotation(instanceIdentifier)){
                if(labelPattern.getFrozenState().equals(Annotation.FrozenState.FROZEN_GOOD)){
                    final String eventType = labelPattern.getLabel();
                    final SentenceTheory sentenceTheory = instanceIdentifierSentenceTheoryMap.get(instanceIdentifier);
                    final EventMention eventMention = (EventMention) InstanceIdentifier.getSpanning(sentenceTheory, instanceIdentifier.getSlot0Start(), instanceIdentifier.getSlot0End(), instanceIdentifier.getSlotEntityType(0)).get();
                    final String trigger = eventMention.anchorNode().span().tokenizedText().utf16CodeUnits();


                    Map<String,List<String>> eventBuf =  eventTypeToTriggerToSentenceSpanMap.getOrDefault(eventType,new HashMap<>());
                    List<String> triggerBuf = eventBuf.getOrDefault(trigger,new ArrayList<>());
                    List<String> sentenceInToken = new ArrayList<>();

                    for (int i = 0;i < sentenceTheory.tokenSequence().size();++i) {
                        String baseStr = sanitize(sentenceTheory.tokenSequence().token(i).tokenizedText().utf16CodeUnits());
                        if(i == instanceIdentifier.getSlot0Start()){
                            baseStr = "["+baseStr;
                        }
                        if(i == instanceIdentifier.getSlot0End()){
                            baseStr = baseStr + "]";
                        }
                        sentenceInToken.add(baseStr);
                    }
                    triggerBuf.add(String.join(" ",sentenceInToken));
                    eventBuf.put(trigger,triggerBuf);
                    eventTypeToTriggerToSentenceSpanMap.put(eventType,eventBuf);
                }
            }
        }
        ObjectMapper objectMapper = StorageUtils.getMapperWithoutTyping();
        objectMapper.writeValue(new File(this.outputPath),eventTypeToTriggerToSentenceSpanMap);
    }
}
