package com.bbn.akbc.neolearnit.util;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.util.SourceListsReader;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Set;

public class GenerateDocListForMappings {
    public static void main(String[] args) throws Exception{
        String params = args[0];
        LearnItConfig.loadParams(new File(params));
        String mappingsPath = args[1];
        String outputPath = args[2];


        Mappings mappings = Mappings.deserialize(new File(mappingsPath),true);
        Set<InstanceIdentifier> mentionedInstanceIds = new HashSet<>();

        mentionedInstanceIds.addAll(mappings.getPatternInstances());
        mentionedInstanceIds.addAll(mappings.getSeedInstances());

        Set<String> resultPaths = new HashSet<>();

        for(InstanceIdentifier instanceIdentifier: mentionedInstanceIds){
            String docPath = new File(SourceListsReader.getFullPath(instanceIdentifier.getDocid())).getAbsolutePath();
            resultPaths.add(docPath);
        }
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File(outputPath)));
        for(String p : resultPaths){
            bufferedWriter.write(p);
            bufferedWriter.newLine();
        }
        bufferedWriter.close();
    }
}
