package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.mappings.filters.FrequencyLimitFilter;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.storage.StorageUtils;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import sun.security.jca.GetInstance;

import java.io.File;
import java.io.IOException;

public class PrintInterventions {

    public static void main(String[] argv) throws JsonParseException, JsonMappingException, IOException {

        String inputMappingsFile = argv[0];

        System.out.println("Loading mappings...");
        Mappings mappings = Mappings.deserialize(new File(inputMappingsFile), true);

        for (Seed seed : mappings.getAllSeeds().elementSet()) {
            String arg1text = seed.getSlotHeadText(0).asString();
            String arg2text = seed.getSlotHeadText(1).asString();
            for(LearnitPattern learnitPattern : mappings.getPatternsForSeed(seed)) {
                String patternStr = learnitPattern.toIDString();
                if(patternStr.contains("sub") && patternStr.contains("obj")) {
                    patternStr = patternStr.replace("<sub>", "").replace("<obj>", "");
                    System.out.println(arg1text + "\t" + patternStr + "\t" + arg2text);
                }
            }
        }
    }
}
