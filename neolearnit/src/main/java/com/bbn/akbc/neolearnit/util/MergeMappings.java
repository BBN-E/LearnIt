package com.bbn.akbc.neolearnit.util;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.mappings.filters.PatternConstraintsFilter;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToSeedMapping;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.storage.MapStorage;
import com.bbn.akbc.neolearnit.storage.impl.HashMapStorage;
import com.bbn.akbc.utility.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class MergeMappings {

    public static void main(String [] args) throws IOException {
        String params = args[0];
        String listMappings = args[1]; // this can also be a directory of list files
        String finalMappingsFile = args[2];

//        int seedMin=Integer.parseInt(args[3]);
//        int seedMax=Integer.parseInt(args[4]);
//        int patternMin=Integer.parseInt(args[5]);
//        int patternMax=Integer.parseInt(args[6]);


        LearnItConfig.loadParams(new File(params));

        List<File> listOfListFiles = new ArrayList<>();
        if (new File(listMappings).isFile()){
            listOfListFiles.add(new File(listMappings));
        }else{
            listOfListFiles = Arrays.asList(new File(listMappings).listFiles());
        }

        List<String> mappingsList = new ArrayList<>();
        for(File listFile : listOfListFiles) {
            mappingsList.addAll(FileUtil.readLinesIntoList(listFile));
        }
        MapStorage.Builder<InstanceIdentifier, Seed> i2SeedStorage = new HashMapStorage.Builder<>();
        MapStorage.Builder<InstanceIdentifier, LearnitPattern> i2PatternStorage = new HashMapStorage.Builder<>();

//        FrequencyLimitFilter frequencyLimitFilter = new FrequencyLimitFilter(seedMin,seedMax,patternMin,patternMax);
        // Too dangerous!!!
//        FrequencyLimitFilter inputFrequencyLimitFilter = new FrequencyLimitFilter(5,-1,-1,-1);

        int count = 0;
//        MappingsFilter patternConstraintsFilter =
        new PatternConstraintsFilter(new HashSet<>(),
                new HashSet<>(), true);
        for(String mappingPath : mappingsList){
            File mappingFile = new File(mappingPath);
            System.out.println("Reading mappings "+mappingPath+" size: "+mappingFile.length()/(1024*1024)+"M" +
                    "...("+(++count)+"/"+mappingsList.size()+")");
            Mappings mappings = Mappings.deserialize(mappingFile,true);
//            mappings = patternConstraintsFilter.makeFiltered(mappings);


            System.out.println("\tapplying filter...");
//            mappings = inputFrequencyLimitFilter.makeFiltered(mappings);
            System.out.println("\tupdating instance2Seed map...");
            InstanceToSeedMapping i2s = mappings.getInstance2Seed();
            for(InstanceIdentifier iid : i2s.getAllInstances()){
                for(Seed s : i2s.getSeeds(iid)) {
                    i2SeedStorage.put(iid,s);
                }
            }
            System.out.println("\tupdating instance2Pattern map...");
            InstanceToPatternMapping i2p = mappings.getInstance2Pattern();
            for(InstanceIdentifier iid : i2p.getAllInstances()){
                for(LearnitPattern p : i2p.getPatterns(iid)) {
                    i2PatternStorage.put(iid,p);
                }
            }
        }
        InstanceToSeedMapping i2s = new InstanceToSeedMapping(i2SeedStorage.build());
        InstanceToPatternMapping i2p = new InstanceToPatternMapping(i2PatternStorage.build());

        Mappings finalMappings = new Mappings(i2s,i2p);
        System.out.println("Filtering final mappings...");
//        finalMappings = frequencyLimitFilter.makeFiltered(finalMappings);
//        finalMappings = inputFrequencyLimitFilter.makeFiltered(finalMappings);
        System.out.println("Serializing final mappings...");

        File outputFile = new File(finalMappingsFile);
        finalMappings.serialize(outputFile,true);

        System.out.println("Final mappings written to: "+finalMappingsFile+" size: "+outputFile.length()/(1024*1024)+"M");

    }
}
