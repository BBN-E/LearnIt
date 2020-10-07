package com.bbn.akbc.neolearnit.util;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.LearnItObservation;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.storage.StorageUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DumpLearnItObversationWithSelectedInstances {

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonTypeInfo(use=JsonTypeInfo.Id.MINIMAL_CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
    static class LearnitObversationInstanceEntry {
        @JsonProperty
        Map<String,String> aux;
        @JsonProperty
        LearnItObservation learnItObservation;
        @JsonProperty
        Set<InstanceIdentifierFilterForAnnotation.BasicInstanceIdentifier> chosenInstances;
        LearnitObversationInstanceEntry(LearnItObservation learnItObservation){
            this.learnItObservation = learnItObservation;
            this.chosenInstances = new HashSet<>();
            this.aux = new HashMap<>();
        }
    }

    enum WorkingMode{
        Seed,
        LearnitPattern
    }

    public static Map<LearnItObservation,Set<InstanceIdentifierFilterForAnnotation.BasicInstanceIdentifier>> selectInstanceForLearnItObversation(List<Mappings> mappingsList,WorkingMode workingMode,int NUMBER_OF_INSTANCES_PER_LEARNIT_OBVERSATION_CAP){
        Map<LearnItObservation,Set<InstanceIdentifierFilterForAnnotation.BasicInstanceIdentifier>> patternToSelectedInstancesIdentifier = new HashMap<>();
        for(Mappings mappings: mappingsList){
            if(workingMode.equals(WorkingMode.Seed)){
                for(Seed seed:mappings.getAllSeeds().elementSet()){
                    Set<InstanceIdentifierFilterForAnnotation.BasicInstanceIdentifier> buf = patternToSelectedInstancesIdentifier.getOrDefault(seed,new HashSet<>());
                    if(buf.size()< NUMBER_OF_INSTANCES_PER_LEARNIT_OBVERSATION_CAP){
                        for(InstanceIdentifier instanceIdentifier:mappings.getInstancesForSeed(seed)){
                            InstanceIdentifierFilterForAnnotation.BasicInstanceIdentifier basicInstanceIdentifier = new InstanceIdentifierFilterForAnnotation.BasicInstanceIdentifier(instanceIdentifier);
                            buf.add(basicInstanceIdentifier);
                            if(buf.size()>=NUMBER_OF_INSTANCES_PER_LEARNIT_OBVERSATION_CAP){
                                break;
                            }
                        }
                    }
                    patternToSelectedInstancesIdentifier.put(seed,buf);
                }
            }
            else{
                for(LearnitPattern learnitPattern:mappings.getAllPatterns().elementSet()){
                    Set<InstanceIdentifierFilterForAnnotation.BasicInstanceIdentifier> buf = patternToSelectedInstancesIdentifier.getOrDefault(learnitPattern,new HashSet<>());
                    if(buf.size()< NUMBER_OF_INSTANCES_PER_LEARNIT_OBVERSATION_CAP){
                        for(InstanceIdentifier instanceIdentifier:mappings.getInstancesForPattern(learnitPattern)){
                            InstanceIdentifierFilterForAnnotation.BasicInstanceIdentifier basicInstanceIdentifier = new InstanceIdentifierFilterForAnnotation.BasicInstanceIdentifier(instanceIdentifier);
                            buf.add(basicInstanceIdentifier);
                            if(buf.size()>=NUMBER_OF_INSTANCES_PER_LEARNIT_OBVERSATION_CAP){
                                break;
                            }
                        }
                    }
                    patternToSelectedInstancesIdentifier.put(learnitPattern,buf);
                }
            }
        }
        return patternToSelectedInstancesIdentifier;
    }

    public static void serializeSampledInstance(Map<LearnItObservation,Set<InstanceIdentifierFilterForAnnotation.BasicInstanceIdentifier>> patternToSelectedInstancesIdentifier, String outputPath) throws IOException {
        List<LearnitObversationInstanceEntry> output = new ArrayList<>();
        for(LearnItObservation learnitPattern: patternToSelectedInstancesIdentifier.keySet()){
            Set<InstanceIdentifierFilterForAnnotation.BasicInstanceIdentifier> buf = patternToSelectedInstancesIdentifier.get(learnitPattern);
            LearnitObversationInstanceEntry learnitObversationInstanceEntry = new LearnitObversationInstanceEntry(learnitPattern);
            learnitObversationInstanceEntry.chosenInstances.addAll(buf);
            output.add(learnitObversationInstanceEntry);
        }
        ObjectMapper objectMapper = StorageUtils.getMapperWithoutTyping();
        objectMapper.writeValue(new File(outputPath),output);
    }

    public static void main(String[] args) throws Exception{
        String paramPath = args[0];
        LearnItConfig.loadParams(new File(paramPath));
//        String mappingsPath = args[1];
        String mappingsPathListPath = args[1];
        String outputPath = args[2];
        WorkingMode workingMode = WorkingMode.valueOf(args[3]);
        int NUMBER_OF_INSTANCES_PER_LEARNIT_OBVERSATION_CAP = Integer.parseInt(args[4]);

        List<Mappings> mappingsList = new ArrayList<>();
        for(String mappingsPath:GeneralUtils.readLinesIntoList(mappingsPathListPath)){
            Mappings mappings = Mappings.deserialize(new File(mappingsPath),true);
            mappingsList.add(mappings);
        }

        Map<LearnItObservation,Set<InstanceIdentifierFilterForAnnotation.BasicInstanceIdentifier>> patternToSelectedInstancesIdentifier = selectInstanceForLearnItObversation(mappingsList,workingMode,NUMBER_OF_INSTANCES_PER_LEARNIT_OBVERSATION_CAP);

        serializeSampledInstance(patternToSelectedInstancesIdentifier,outputPath);

    }
}
