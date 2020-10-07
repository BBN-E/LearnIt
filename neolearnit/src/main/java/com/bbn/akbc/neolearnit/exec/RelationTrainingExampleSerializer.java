package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.common.Pair;
import com.bbn.akbc.neolearnit.common.Annotation.FrozenState;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.common.targets.TargetFactory;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observers.Observer;
import com.bbn.akbc.neolearnit.serializers.binary_event.CausalRelationCNNObserver;
import com.bbn.akbc.neolearnit.util.InstanceIdentifierFilterForAnnotation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class RelationTrainingExampleSerializer {
    final static String NEGATIVE_TYPE = "NO_RELATION";

    public static Pair<Set<Pair<InstanceIdentifier,LabelPattern>>,Set<Pair<InstanceIdentifier,LabelPattern>>> TrainingExampleGenerator(Mappings mappings,double NEGATIVE_SAMPLING_RATIO){
        Set<Pair<InstanceIdentifier,LabelPattern>> positivePatternInstance = new HashSet<>();
        Set<InstanceIdentifier> positiveSet = new HashSet<>();
        Set<LearnitPattern> patternSet = new HashSet<>(mappings.getAllPatterns());
        Set<Pair<InstanceIdentifier,LabelPattern>> negativePatternInstance = new HashSet<>();
        Set<InstanceIdentifier> negativeSet = new HashSet<>();
        Map<Pair<InstanceIdentifier,LabelPattern>,Integer> countMap = new HashMap<>();
        Set<InstanceIdentifier> nuturalInstance = new HashSet<>();
        //Dealing with positive examples
        for(LearnitPattern learnitPattern : patternSet){
            if(LabelPattern.class.isInstance(learnitPattern)){
                // This filter is for de-duplicate
                Set<InstanceIdentifier> instanceIdentifierSet = new HashSet<>(InstanceIdentifierFilterForAnnotation.makeFiltered(mappings.getInstancesForPattern(learnitPattern)));
                for(InstanceIdentifier instanceIdentifier: instanceIdentifierSet){
                    Pair<InstanceIdentifier,LabelPattern> key = new Pair<>(instanceIdentifier,(LabelPattern)learnitPattern);
                    if(key.getSecond().getFrozenState().equals(FrozenState.FROZEN_GOOD)){
                        positivePatternInstance.add(key);
                        positiveSet.add(instanceIdentifier);
                        countMap.put(key,countMap.getOrDefault(key,0) + 1);
                    }
                    else if(key.getSecond().getFrozenState().equals(FrozenState.FROZEN_BAD)){
                        negativePatternInstance.add(key);
                        negativeSet.add(instanceIdentifier);
                    }
                    else{
                        nuturalInstance.add(key.getFirst());
                    }
                }
            }
            else{
                nuturalInstance.addAll(mappings.getInstancesForPattern(learnitPattern));
            }

        }
        //Dealing with negative examples
        int numNegativeExamples = (int)(NEGATIVE_SAMPLING_RATIO * positivePatternInstance.size());
//        @hqiu: NOW, no matter the instanceidentifiers captured by LabelPattern(negative) or not, I'll treat it as
//        "General", no labeled by specific pattern, negative pattern.
//        If you want to leverage the capability of negative pattern for per instanceidentifier basis, you should change
//        below logic
        List<InstanceIdentifier> negativeCandidateList = new ArrayList<>();
        for(Pair<InstanceIdentifier,LabelPattern> instanceIdentifierLabelPatternPair : negativePatternInstance){
            InstanceIdentifier instanceIdentifier = instanceIdentifierLabelPatternPair.getFirst();
            if(!positiveSet.contains(instanceIdentifier) || !negativeSet.contains(instanceIdentifier)){
                negativeCandidateList.add(instanceIdentifier);
            }
        }
        for(InstanceIdentifier instanceIdentifier : nuturalInstance){
            if(!positiveSet.contains(instanceIdentifier) || !negativeSet.contains(instanceIdentifier)){
                negativeCandidateList.add(instanceIdentifier);
            }
        }
        numNegativeExamples = Math.min(negativeCandidateList.size(),numNegativeExamples);
        List<InstanceIdentifier> reservoir = new ArrayList<>();
        int i;
        for(i = 0 ; i < numNegativeExamples;++i){
            reservoir.add(negativeCandidateList.get(i));
        }
        Random r = new Random();
        for(; i < numNegativeExamples;++i){
            int j = r.nextInt(i + 1);
            if(j < numNegativeExamples){
                reservoir.add(j,negativeCandidateList.get(i));
            }
        }

        Set<Pair<InstanceIdentifier,LabelPattern>> positiveExamples = new HashSet<>(positivePatternInstance);
        Set<Pair<InstanceIdentifier,LabelPattern>> negativeExamples = new HashSet<>();
        for(InstanceIdentifier negativeExample : reservoir){
            negativeExamples.add(new Pair<>(negativeExample,new LabelPattern(NEGATIVE_TYPE,FrozenState.FROZEN_BAD)));
        }
        return new Pair<>(positiveExamples,negativeExamples);
    }

    public static void main(String args[]) throws IOException{
        String strFileParam = args[0];
        String strFileListMappings = args[1];
        String strOutFilePrefix = args[2];
        double NEGATIVE_SAMPLING_RATIO = Double.parseDouble(args[3]);
        LearnItConfig.loadParams(new File(strFileParam));
        Target target = TargetFactory.makeBinaryEventEventTarget();
        String jsonOutputPath = strOutFilePrefix + ".jsonl";
        String strSentenceFile = strOutFilePrefix + ".sentence";
        String labelFile = strOutFilePrefix + ".label";
        String distanceToArg1File = strOutFilePrefix + ".distanceToArg1";
        String distanceToArg2File = strOutFilePrefix + ".distanceToArg2";
        String typeFile = strOutFilePrefix + ".entityType";
        String mentionLevelFile = strOutFilePrefix + ".mentionLevel";

        BufferedWriter writerSentence = new BufferedWriter(new FileWriter(
                new File(strSentenceFile)));
        BufferedWriter writerLabel = new BufferedWriter(new FileWriter(
                new File(labelFile)));
        BufferedWriter writerDistanceToArg1 = new BufferedWriter(new FileWriter(
                new File(distanceToArg1File)));
        BufferedWriter writerDistanceToArg2 = new BufferedWriter(new FileWriter(
                new File(distanceToArg2File)));// for typeJson
        BufferedWriter writerType = new BufferedWriter(new FileWriter(
                new File(typeFile)));
        BufferedWriter writerMentionLevel = new BufferedWriter(new FileWriter(
                new File(mentionLevelFile)));
        BufferedWriter writerJson = new BufferedWriter(new FileWriter(
                new File(jsonOutputPath)));

        CausalRelationCNNObserver causalRelationCNNObserver = new CausalRelationCNNObserver(writerSentence,writerLabel,writerDistanceToArg1,writerDistanceToArg2,writerType,writerMentionLevel,writerJson);

        List<Observer<Pair<InstanceIdentifier,LabelPattern>>> observers = new ArrayList<>();
        observers.add(causalRelationCNNObserver);

        try {
            // deserialize mappings to search for instances ID
            List<String> filePaths =
                    com.bbn.akbc.utility.FileUtil.readLinesIntoList(strFileListMappings);
            for (String filePath : filePaths) {
                System.out.println("Load mapping file " + filePath);
                Mappings mappings = Mappings.deserialize(new File(filePath), true);
                Pair<Set<Pair<InstanceIdentifier,LabelPattern>>,Set<Pair<InstanceIdentifier,LabelPattern>>> ret = TrainingExampleGenerator(mappings,NEGATIVE_SAMPLING_RATIO);
                Set<Pair<InstanceIdentifier,LabelPattern>> positiveExamples = ret.getFirst();
                Set<Pair<InstanceIdentifier,LabelPattern>> negativeExamples = ret.getSecond();

                for(Pair<InstanceIdentifier,LabelPattern> observation: positiveExamples){
                    for(Observer<Pair<InstanceIdentifier,LabelPattern>> observer : observers){
                        observer.observe(observation);
                    }
                }
                for(Pair<InstanceIdentifier,LabelPattern> observation: negativeExamples){
                    causalRelationCNNObserver.observe(observation);
                    // ATTENTION SHOULD CLOSE LATER
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        writerSentence.close();
        writerLabel.close();
        writerDistanceToArg1.close();
        writerDistanceToArg2.close();
        writerType.close();
        writerMentionLevel.close();
        writerJson.close();
    }
}
