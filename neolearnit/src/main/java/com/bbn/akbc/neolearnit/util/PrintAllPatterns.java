package com.bbn.akbc.neolearnit.util;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;

import java.io.File;

public class PrintAllPatterns {
    public static void main(String[]args)throws Exception{
        String learnitConfigPath = args[0];
        LearnItConfig.loadParams(new File(learnitConfigPath));

        Mappings mappings = Mappings.deserialize(new File(args[1]),true);
        for(LearnitPattern learnitPattern:mappings.getAllPatterns()){
            System.out.println(learnitPattern.toPrettyString());
        }
    }
}
