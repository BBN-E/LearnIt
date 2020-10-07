package com.bbn.akbc.neolearnit.tmp;

import com.bbn.akbc.neolearnit.common.BBNInternalOntology;
import com.bbn.akbc.neolearnit.common.Domain;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.google.common.collect.Multimap;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class TurnOntologyAndExtractorsToUpperCase {
    public static void main(String[] args) throws Exception{
        LearnItConfig.loadParams(new File(args[0]));
        final File ontologyFile = new File(args[1]);
//        String targetPathDir = args[2];
        String targetPathDir = Domain.getExtractorsPath();
        BBNInternalOntology.BBNInternalOntologyNode root =  BBNInternalOntology.BBNInternalOntologyNode.fromInternalOntologyFile(ontologyFile);
        Multimap<String, BBNInternalOntology.BBNInternalOntologyNode> childrenNodeMap = root.getPropToNodeMultiMap(new BBNInternalOntology.BBNInternalOntologyNode.PropStringGetter("originalKey"));

//        String targetPathDir = String.format("%s/inputs/extractors/", LearnItConfig.get("learnit_root"));

        Map<String,TargetAndScoreTables> oldOntologyNameToExtractors = new HashMap<>();

        for(String targetName:childrenNodeMap.keySet()){
            if(new File(targetPathDir + File.separator + targetName+".json").exists()){
                TargetAndScoreTables targetAndScoreTables = TargetAndScoreTables.deserialize(new File(targetPathDir + File.separator+  targetName+".json"));
                oldOntologyNameToExtractors.put(targetName,targetAndScoreTables);
//                targetAndScoreTables.serialize(new File(strPathJson));
//                targetAndScoreTables.getTarget().serialize(outputFolder+".target" + File.separator + targetName + ".json");

            }
        }
        Map<String,String> oldOntologyNameToNewOntologyName = new HashMap<>();
        for(String ontologyName:childrenNodeMap.keySet()){
            String newOntologyName = ontologyName.toUpperCase();
            oldOntologyNameToNewOntologyName.put(ontologyName,newOntologyName);
        }
        // Step1 Ontology
        for(BBNInternalOntology.BBNInternalOntologyNode bbnInternalOntologyNode:childrenNodeMap.values()){
            bbnInternalOntologyNode.originalKey = oldOntologyNameToNewOntologyName.get(bbnInternalOntologyNode.originalKey);
        }
        // Step2 extractors
        for(TargetAndScoreTables targetAndScoreTables:oldOntologyNameToExtractors.values()){
            targetAndScoreTables.getTarget().setName(oldOntologyNameToNewOntologyName.get(targetAndScoreTables.getTarget().getName()));
        }
        // Step3 Remove old Targets
        for(String targetName:childrenNodeMap.keySet()){
            if(new File(targetPathDir + File.separator + targetName+".json").exists()){
                File taretAndScorePath = new File(targetPathDir + File.separator + targetName+".json");
                File outputDirForTarget = new File(new File(taretAndScorePath.getParent()).getParent() + File.separator + new File(taretAndScorePath.getParent()).getName() + ".target");
                String targetPath = outputDirForTarget.getAbsolutePath()+File.separator+targetName+".json";

                taretAndScorePath.delete();
                new File(targetPath).delete();
            }
        }
        // Step 4 serialize
        root.convertToInternalOntologyYamlFile(ontologyFile);
        for(TargetAndScoreTables targetAndScoreTables:oldOntologyNameToExtractors.values()){
            String strPathJson = targetPathDir + File.separator + targetAndScoreTables.getTarget().getName() + ".json";
            System.out.println("\t serializing extractor for " + targetAndScoreTables.getTarget().getName() + "...");
            targetAndScoreTables.serialize(new File(strPathJson));
            String strPathTargetJson = targetPathDir + ".target" + File.separator + targetAndScoreTables.getTarget().getName() + ".json";
            targetAndScoreTables.getTarget().serialize(strPathTargetJson);
        }

    }
}
