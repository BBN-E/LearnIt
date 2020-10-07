package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.neolearnit.common.BBNInternalOntology;
import com.bbn.akbc.neolearnit.common.Domain;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.targets.TargetFactory;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.util.GeneralUtils;
import com.google.common.collect.Multimap;

import java.io.File;

public class ExportLatestTargetAndScoreTableBasedOnYAML {
    static boolean shouldCreateExtraFolder = false;

    public static void exportExtractors(String paramPath,String yamlPath,String outputFolder) throws Exception {
        LearnItConfig.loadParams(new File(paramPath));
        new File(outputFolder).mkdirs();
        new File(outputFolder+".target").mkdirs();
        final File ontologyFile = new File(yamlPath);
        String targetPathDir = Domain.getExtractorsPath();
        BBNInternalOntology.BBNInternalOntologyNode root =  BBNInternalOntology.BBNInternalOntologyNode.fromInternalOntologyFile(ontologyFile);
        Multimap<String, BBNInternalOntology.BBNInternalOntologyNode> childrenNodeMap = root.getPropToNodeMultiMap(new BBNInternalOntology.BBNInternalOntologyNode.PropStringGetter("originalKey"));

//        String targetPathDir = String.format("%s/inputs/extractors/", LearnItConfig.get("learnit_root"));

        for(String targetName:childrenNodeMap.keySet()){
            if(new File(targetPathDir + File.separator + targetName+".json").exists()){
                TargetAndScoreTables targetAndScoreTables = TargetAndScoreTables.deserialize(new File(targetPathDir + File.separator + targetName+".json"));
                String strPathJson = outputFolder + File.separator + targetName + ".json";
                if(shouldCreateExtraFolder){
                    new File(outputFolder + File.separator + targetName).mkdirs();
                    strPathJson = outputFolder + File.separator + targetName + File.separator + targetName + ".json";
                }
                targetAndScoreTables.serialize(new File(strPathJson));
                targetAndScoreTables.getTarget().serialize(outputFolder+".target" + File.separator + targetName + ".json");
            }
        }
    }

    public static void main(String[] args) throws Exception{
        String paramPath = args[0];
        String yamlPath = args[1];
        String outputFolder = args[2];
        exportExtractors(paramPath,yamlPath,outputFolder);
    }
}
