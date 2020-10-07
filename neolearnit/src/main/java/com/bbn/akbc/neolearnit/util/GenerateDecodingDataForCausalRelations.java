package com.bbn.akbc.neolearnit.util;

import com.bbn.akbc.common.Pair;
import com.bbn.akbc.neolearnit.common.Annotation.FrozenState;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;
import com.bbn.akbc.neolearnit.serializers.binary_event.CausalRelationCNNObserver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GenerateDecodingDataForCausalRelations{
    public final static String NETURAL_TYPE = "NETURAL_TYPE";

    public static File pathjoin(String path1, String path2)
    {
        File file1 = new File(path1);
        File file2 = new File(file1, path2);
        return file2;
    }

    public static void CreateEmptyFile(String strOutFilePrefix) throws Exception{
        String strSentenceFile = strOutFilePrefix + ".empty.sentence";
        String labelFile = strOutFilePrefix + ".empty.label";
        String distanceToArg1File = strOutFilePrefix + ".empty.distanceToArg1";
        String distanceToArg2File = strOutFilePrefix + ".empty.distanceToArg2";
        String typeFile = strOutFilePrefix + ".empty.entityType";
        String mentionLevelFile = strOutFilePrefix + ".empty.mentionLevel";
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
        writerSentence.close();
        writerLabel.close();
        writerDistanceToArg1.close();
        writerDistanceToArg2.close();
        writerType.close();
        writerMentionLevel.close();
    }


    public static void main(String[] args) throws Exception{
        String strFileParam = args[0];
        String strFileListMappings = args[1];
        String strOutFilePrefix = args[2];
        LearnItConfig.loadParams(new File(strFileParam));

        List<String> filePaths =
                com.bbn.akbc.utility.FileUtil.readLinesIntoList(strFileListMappings);
        Set<InstanceIdentifier> allSet = new HashSet<>();

        String strSentenceFile = strOutFilePrefix + ".sentence";
        String labelFile = strOutFilePrefix + ".label";
        String distanceToArg1File = strOutFilePrefix + ".distanceToArg1";
        String distanceToArg2File = strOutFilePrefix + ".distanceToArg2";
        String typeFile = strOutFilePrefix + ".entityType";
        String mentionLevelFile = strOutFilePrefix + ".mentionLevel";
        String jsonOutputPath = strOutFilePrefix + ".jsonl";
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

        CausalRelationCNNObserver causalRelationCNNObserver = new CausalRelationCNNObserver(writerSentence,
                writerLabel,
                writerDistanceToArg1,
                writerDistanceToArg2,
                writerType,
                writerMentionLevel,
                writerJson);
        for (String filePath : filePaths) {
            System.out.println("Load mapping file " + filePath);
            Mappings mappings = Mappings.deserialize(new File(filePath), true);
            allSet.addAll(mappings.getSeedInstances());
            allSet.addAll(mappings.getPatternInstances());
            allSet = new HashSet<>(InstanceIdentifierFilterForAnnotation.makeFiltered(allSet));
        }
        for(InstanceIdentifier instanceIdentifier : allSet){
            causalRelationCNNObserver.observe(new Pair<>(instanceIdentifier,new LabelPattern(NETURAL_TYPE,FrozenState.NO_FROZEN)));
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
