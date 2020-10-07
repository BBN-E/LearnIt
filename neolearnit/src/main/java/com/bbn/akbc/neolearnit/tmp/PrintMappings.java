package com.bbn.akbc.neolearnit.tmp;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;

import java.io.File;

public class PrintMappings {

    public static void main(String[] args) throws Exception{
        String paramPath = args[0];
        LearnItConfig.loadParams(new File(paramPath));
        String mappingsPath = args[1];
        Mappings mappings = Mappings.deserialize(new File(mappingsPath),true);
        for(LearnitPattern learnitPattern:mappings.getAllPatterns().elementSet()){
            System.out.println(learnitPattern.toPrettyString()+"\t"+mappings.getAllPatterns().count(learnitPattern));
        }
    }
}
