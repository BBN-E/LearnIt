package com.bbn.akbc.neolearnit.util;

import com.bbn.akbc.common.Pair;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.Annotation.FrozenState;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.common.targets.TargetFactory;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.scoring.scores.PatternScore;
import com.bbn.akbc.neolearnit.scoring.tables.AbstractScoreTable;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.SentenceTheory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.eclipse.jetty.util.IO;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static com.bbn.akbc.neolearnit.util.GeneralUtils.loadExtractorsFromFileList;

public class GenerateTrainingDataForCausalRelations {
    static int MAX_INST_PER_PATTERN = 3;
    static String NEGATIVE_TYPE = "NO_RELATION";

    public static Set<Pair<InstanceIdentifier,String>> GenerateExamplesFromMapping(Mappings mappings, Map<String,TargetAndScoreTables> extractors,double NEGATIVE_SAMPLING_RATIO){
        Set<Pair<InstanceIdentifier,String>> positivePatternInstance = new HashSet<>();
        Set<InstanceIdentifier> positiveSet = new HashSet<>();

        Map<Pair<InstanceIdentifier,String>,Integer> countMap = new HashMap<>();
        //Dealing with positive examples
        for(String relationTypeName : extractors.keySet()){
            for (AbstractScoreTable.ObjectWithScore<LearnitPattern, PatternScore> pattern : extractors.get(relationTypeName)
                    .getPatternScores().getObjectsWithScores()) {
                if(pattern.getScore().isFrozen() && pattern.getScore().isGood()){
                    for(InstanceIdentifier instanceIdentifier: mappings.getInstancesForPattern(pattern.getObject())){
                        Pair<InstanceIdentifier,String> key = new Pair<>(instanceIdentifier,relationTypeName);
                        positivePatternInstance.add(key);
                        positiveSet.add(instanceIdentifier);
                        countMap.put(key,countMap.getOrDefault(key,0) + 1);
                    }
                }
            }
        }
        Set<InstanceIdentifier> negativePatternInstance = new HashSet<>(mappings.getSeedInstances());
        negativePatternInstance.addAll(mappings.getPatternInstances());

        //Dealing with negative examples
        int numNegativeExamples = (int)(NEGATIVE_SAMPLING_RATIO * positivePatternInstance.size());
        List<InstanceIdentifier> negativeCandidateList = new ArrayList<>();
        for(InstanceIdentifier instanceIdentifier : negativePatternInstance){
            if(!positiveSet.contains(instanceIdentifier)){
                negativeCandidateList.add(instanceIdentifier);
            }
        }
        List<InstanceIdentifier> reservoir = new ArrayList<>();
        int i;
        for(i = 0 ; i < Math.min(negativeCandidateList.size(),numNegativeExamples);++i){
            reservoir.add(negativeCandidateList.get(i));
        }
        Random r = new Random();
        for(; i < numNegativeExamples;++i){
            int j = r.nextInt(i + 1);
            if(j < numNegativeExamples){
                reservoir.add(j,negativeCandidateList.get(i));
            }
        }
        System.out.println("We intend to get " + numNegativeExamples +" negative examples, but we got "+reservoir.size());
        Set<Pair<InstanceIdentifier,String>> ret = new HashSet<>(positivePatternInstance);
        for(InstanceIdentifier negativeExample : reservoir){
            ret.add(new Pair<>(negativeExample,NEGATIVE_TYPE));
        }
        return ret;

    }

    public static void main(String[] args) throws IOException {
        // deserialize extractors
        String strFileParam = args[0];
        String strFileListExtractor = args[1];
        String strFileListMappings = args[2];
        String strOutFilePrefix = args[3];
        double NEGATIVE_SAMPLING_RATIO = Double.parseDouble(args[4]);

        LearnItConfig.loadParams(new File(strFileParam));
        // instId2patterns = HashMultimap.create();
        Set<Pair<InstanceIdentifier,String>> trainingExamples = new HashSet<>();

        // deserialize mappings to search for instances ID
        List<String> filePaths =
                com.bbn.akbc.utility.FileUtil.readLinesIntoList(strFileListMappings);
        Map<String,TargetAndScoreTables> extractors = loadExtractorsFromFileList(strFileListExtractor);
        for (String filePath : filePaths) {
            System.out.println("Load mapping file " + filePath);
            Mappings mappings = Mappings.deserialize(new File(filePath), true);
            trainingExamples.addAll(GenerateExamplesFromMapping(mappings,extractors,NEGATIVE_SAMPLING_RATIO));
        }

        System.out.println("======== Serialize instances:");
        String outputPath = strOutFilePrefix;
        BufferedWriter outputJsonWritter = new BufferedWriter(new FileWriter(
                new File(outputPath+".ljson")));

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

        for (Pair<InstanceIdentifier,String> trainingExample : trainingExamples) {
            Optional<RelationMentionInfo> relationMentionInfo = fromInstanceIdentifier(trainingExample.getSecond(), trainingExample.getFirst());
            if (relationMentionInfo.isPresent()) {
                RelationMentionInfo.SerializationFactory.StringSerializer relationMentionPrinter = new RelationMentionInfo.SerializationFactory.StringSerializer(relationMentionInfo.get());
                writerSentence.write(relationMentionPrinter.getSentenceInOneLine() + "\n");
                writerLabel.write(relationMentionPrinter.getLabel() + "\n");
                writerDistanceToArg1.write(relationMentionPrinter.distanceToArg1InOneLine() + "\n");
                writerDistanceToArg2.write(relationMentionPrinter.distanceToArg2InOneLine() + "\n");
                writerType.write(relationMentionPrinter.entityTypeLabelsCoveringArgSpanInOneLine() + "\n");
                writerMentionLevel.write(relationMentionPrinter.mentionLevelLabelsCoveringArgSpanInOneLine() + "\n");
                RelationMentionInfo.SerializationFactory.JSONStringSerializer jsonStringSerializer = new RelationMentionInfo.SerializationFactory.JSONStringSerializer(relationMentionInfo.get());
                outputJsonWritter.write(jsonStringSerializer.getJSON() + "\n");
            }
        }
        writerSentence.close();
        writerLabel.close();
        writerDistanceToArg1.close();
        writerDistanceToArg2.close();
        writerType.close();
        writerMentionLevel.close();
        outputJsonWritter.close();
    }
    public static Optional<RelationMentionInfo> fromInstanceIdentifier(String type, InstanceIdentifier instanceIdentifier) throws IOException {


        MatchInfo.LanguageMatchInfo matchInfo =
                instanceIdentifier.reconstructMatchInfo(TargetFactory.makeEverythingTarget()).getPrimaryLanguageMatch();
        RelationMentionInfo.Builder builder = new RelationMentionInfo.Builder(matchInfo);
        String arg1type = "NA";
        String arg2type = "NA";
        String arg1MentionLevel = "NA";
        String arg2MentionLevel = "NA";
        int arg1index = instanceIdentifier.getSlot0End();
        int arg2index = instanceIdentifier.getSlot1End();

//        builder.withArg1entityType(arg1type);
        builder.withArg1mentionLevel(arg1MentionLevel);
//        builder.withArg2entityType(arg2type);
        builder.withArg2mentionLevel(arg2MentionLevel);
        builder.withArg1index(arg1index);
        builder.withArg2index(arg2index);
        builder.withLabel(type);
        builder.withArg1Span(instanceIdentifier.getSlot0Start(), instanceIdentifier.getSlot0End());
        builder.withArg2Span(instanceIdentifier.getSlot1Start(), instanceIdentifier.getSlot1End());
        builder.withmarkedSentenceString(matchInfo.markedUpTokenString());

        return Optional.of(builder.build());
    }
}
