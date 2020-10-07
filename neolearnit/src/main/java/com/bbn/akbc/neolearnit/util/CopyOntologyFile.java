package com.bbn.akbc.neolearnit.util;

import com.bbn.akbc.neolearnit.common.Domain;
import com.bbn.akbc.neolearnit.common.LearnItConfig;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class CopyOntologyFile {
    public static void main(String[] args) throws Exception{
        LearnItConfig.loadParams(new File(args[0]));
        String activatedTargets = args[1];
        String outputDir = args[2];
        String ontologyFilePath = Domain.getOntologyNameToPathMap().get(Domain.getOriginalTargetName(activatedTargets));
        Files.copy(new File(ontologyFilePath).toPath(),new File(outputDir+File.separator+new File(ontologyFilePath).getName()).toPath());
    }
}
