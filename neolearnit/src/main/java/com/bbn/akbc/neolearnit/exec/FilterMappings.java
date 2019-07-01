package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.common.FileUtil;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.mappings.filters.MappingsFilter;
import com.bbn.akbc.neolearnit.mappings.filters.PatternConstraintsFilter;
import com.bbn.bue.common.exceptions.NotImplementedException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import java.io.File;
import java.io.IOException;

import com.bbn.akbc.neolearnit.mappings.filters.FrequencyLimitFilter;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.storage.StorageUtils;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.bbn.akbc.neolearnit.common.LearnItConfig.getOptionalList;


public class FilterMappings {

    public static void main(String[] argv) throws IOException {
        String paramsFile = argv[0];
        String operationMode = argv[1];
        String inputMappingsFile = argv[2];
        String outputMappingsFile = argv[3];

        LearnItConfig.loadParams(new File(paramsFile));
        String operationModeStr;
        switch (operationMode){
            case "normal":
                operationModeStr = "";
                break;
            case "aggregated":
                operationModeStr = "aggregated_";
                break;
            default:
                throw new NotImplementedException("Operation mode no supported");
        }
        List<String> filterQueueInString = LearnItConfig.getOptionalList(operationModeStr + "mapping_filters");
        List<MappingsFilter> filterQueueInObject = new ArrayList<>();
        for(String filerStage : filterQueueInString){
            filerStage = filerStage.toLowerCase();
            switch (filerStage){
                case "frequency_filter":
                    int seedMin = LearnItConfig.getInt(operationModeStr+"frequency_filter_seed_min");
                    int seedMax = LearnItConfig.getInt(operationModeStr+"frequency_filter_seed_max");;
                    int patternMin = LearnItConfig.getInt(operationModeStr+"frequency_filter_pattern_min");
                    int patternMax = LearnItConfig.getInt(operationModeStr+"frequency_filter_pattern_max");
                    filterQueueInObject.add(new FrequencyLimitFilter(seedMin,seedMax,patternMin,patternMax));
                    break;
                case "pattern_filter":
                    filterQueueInObject.add(new PatternConstraintsFilter(new HashSet<>(),new HashSet<>(),true));
                    break;
                default:
                    throw new NotImplementedException("The Filter is not there!!!");
            }
        }

        if(filterQueueInObject.size() < 1){
            Path from = Paths.get(inputMappingsFile); //convert from File to Path
            Path to = Paths.get(outputMappingsFile); //convert from String to Path
            Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
            return;
        }

        System.out.println("Loading mappings...");
        Mappings mappings = Mappings.deserialize(new File(inputMappingsFile), true);
        for(MappingsFilter mappingsFilter : filterQueueInObject){
            mappings = mappingsFilter.makeFiltered(mappings);
        }
        StorageUtils.serialize(new File(outputMappingsFile), mappings, true);
    }
}
