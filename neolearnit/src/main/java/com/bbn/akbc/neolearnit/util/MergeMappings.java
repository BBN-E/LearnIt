package com.bbn.akbc.neolearnit.util;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.mappings.filters.FrequencyLimitFilter;
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
import java.util.*;

public class MergeMappings {

    public static void main(String[] args) throws IOException {
        String params = args[0];
        String listMappings = args[1]; // this can also be a directory of list files
        String finalMappingsFile = args[2];

//        int seedMin=Integer.parseInt(args[3]);
//        int seedMax=Integer.parseInt(args[4]);
//        int patternMin=Integer.parseInt(args[5]);
//        int patternMax=Integer.parseInt(args[6]);


        LearnItConfig.loadParams(new File(params));

        List<File> listOfListFiles = new ArrayList<>();
        if (new File(listMappings).isFile()) {
            listOfListFiles.add(new File(listMappings));
        } else {
            listOfListFiles = Arrays.asList(new File(listMappings).listFiles());
        }

        List<String> mappingsList = new ArrayList<>();
        for (File listFile : listOfListFiles) {
            mappingsList.addAll(FileUtil.readLinesIntoList(listFile));
        }
        MapStorage.Builder<InstanceIdentifier, Seed> i2SeedStorage = new HashMapStorage.Builder<>();
        MapStorage.Builder<InstanceIdentifier, LearnitPattern> i2PatternStorage = new HashMapStorage.Builder<>();


        // "aggregated" items in param file will being enforced. If per batch frequency limit is larger then aggregated limit, we're not going to enforcing the frequency filter again.
        int seedMin = -1;
        if (LearnItConfig.params().getOptionalInteger("min_seed_frequency_per_batch").or(-1) < LearnItConfig.params().getOptionalInteger("aggregated_frequency_filter_seed_min").or(-1)) {
            seedMin = LearnItConfig.params().getOptionalInteger("aggregated_frequency_filter_seed_min").or(-1);
        }
        int seedMax = LearnItConfig.params().getOptionalInteger("aggregated_frequency_filter_seed_max").or(-1);
        int patternMin = -1;
        if (LearnItConfig.params().getOptionalInteger("min_pattern_frequency_per_batch").or(-1) < LearnItConfig.params().getOptionalInteger("aggregated_frequency_filter_pattern_min").or(-1)) {
            patternMin = LearnItConfig.params().getOptionalInteger("aggregated_frequency_filter_pattern_min").or(-1);
        }
        int patternMax = LearnItConfig.params().getOptionalInteger("aggregated_frequency_filter_pattern_max").or(-1);

        FrequencyLimitFilter outputfrequencyLimitFilter = new FrequencyLimitFilter(seedMin, seedMax, patternMin, patternMax);

        int count = 0;

        Map<Seed,Integer> existingSeed = new HashMap<>();
        Map<LearnitPattern,Integer> existingLearnItPattern = new HashMap<>();
        for (String mappingPath : mappingsList) {
            File mappingFile = new File(mappingPath);
            System.out.println("Reading mappings " + mappingPath + " size: " + mappingFile.length() / (1024 * 1024) + "M" +
                    "...(" + (++count) + "/" + mappingsList.size() + ")");
            Mappings mappings = Mappings.deserialize(mappingFile, true);

            System.out.println("\tupdating instance2Seed map...");
            for(Seed seed : mappings.getAllSeeds().elementSet()){
//                if(existingSeed.getOrDefault(seed,0) < 500){
//                    int cnt = existingSeed.getOrDefault(seed,0);
                    for(InstanceIdentifier instanceIdentifier: mappings.getInstancesForSeed(seed)){
//                        if(cnt>500)break;
                        i2SeedStorage.put(instanceIdentifier,seed);
//                        cnt++;
                    }
//                    existingSeed.put(seed,cnt);
//                }
            }
            System.out.println("\tupdating instance2Pattern map...");
            for(LearnitPattern learnitPattern: mappings.getAllPatterns().elementSet()){
//                if(existingLearnItPattern.getOrDefault(learnitPattern,0) < 500){
//                    int cnt = existingLearnItPattern.getOrDefault(learnitPattern,0);
                    for(InstanceIdentifier instanceIdentifier: mappings.getInstancesForPattern(learnitPattern)){
//                        if(cnt > 500)break;
                        i2PatternStorage.put(instanceIdentifier,learnitPattern);
//                        cnt++;
                    }
//                    existingLearnItPattern.put(learnitPattern,cnt);
//                }
            }
        }
        InstanceToSeedMapping i2s = new InstanceToSeedMapping(i2SeedStorage.build());
        InstanceToPatternMapping i2p = new InstanceToPatternMapping(i2PatternStorage.build());

        Mappings finalMappings = new Mappings(i2s, i2p);
        System.out.println("Filtering final mappings...");
        finalMappings = outputfrequencyLimitFilter.makeFiltered(finalMappings);
        System.out.println("Serializing final mappings...");

        File outputFile = new File(finalMappingsFile);
        finalMappings.serialize(outputFile, true);

        System.out.println("Final mappings written to: " + finalMappingsFile + " size: " + outputFile.length() / (1024 * 1024) + "M");

    }
}
