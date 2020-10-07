package com.bbn.akbc.neolearnit.util;

import com.bbn.akbc.neolearnit.common.Annotation;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;

import java.io.File;

public class GenerateEmptyMappings {
    public static void main(String[] args)throws Exception{
        LearnItConfig.loadParams(new File(args[0]));
        Annotation.InMemoryAnnotationStorage inMemoryAnnotationStorage = new Annotation.InMemoryAnnotationStorage();
        inMemoryAnnotationStorage.convertToMappings().serialize(new File(args[1]),true);
    }
}
