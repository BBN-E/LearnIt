package com.bbn.akbc.neolearnit.bootstrapping;

import com.bbn.akbc.neolearnit.common.BBNInternalOntology;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Stack;

public class GenerateTrainingDataForEventGrounding {
    static Multimap<String,String> ParseDataExample(String jsonFile) throws Exception{
        Multimap<String,String> token2eventType = HashMultimap.create();
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(new FileReader(jsonFile));
        JSONObject jsonObject = (JSONObject) obj;
        for (Object eventObject : jsonObject.keySet()) {
            String eventType = (String) eventObject;
            JSONObject map1 = (JSONObject) jsonObject.get(eventObject);
            if(map1.containsKey("exemplars")) {
                ArrayList<Object> triggers = (ArrayList<Object>) map1.get("exemplars");
                for (final Object trigger : triggers) {
                    JSONObject triggerJson = (JSONObject) trigger;
                    if(triggerJson.containsKey("trigger")) {
                        JSONObject map = (JSONObject) triggerJson.get("trigger");
                        if (map.containsKey("text")) {
                            String text = (String) map.get("text");
                            String[] tokens = text.trim().split(" ");
                            if (tokens.length == 1) {
                                String token = tokens[0].trim().toLowerCase();
                                token2eventType.put(token, eventType);
                            }

                            System.out.println("event\t" + eventType + "\t" + text);
                        }
                    }
                }
            }
        }
        return token2eventType;
    }

    public static void main(String[] args) throws Exception{

        String strFileParam = args[0];
        String strFileJsonExamplars = args[1];
        String yamlOntologyFile = args[2];
        String strFileMappings = args[3];
        String strOutFilePrefix = args[4];

        LearnItConfig.loadParams(new File(strFileParam));
        Multimap<String,String> keywordToOntologyTypeIdMapping = ParseDataExample(strFileJsonExamplars);

        BBNInternalOntology.BBNInternalOntologyNode ontologyRoot = BBNInternalOntology.BBNInternalOntologyNode.fromInternalOntologyFile(new File(yamlOntologyFile));
        Multimap<String,String> concept2path = HashMultimap.create();
        BBNInternalOntology.DFSNodeNameToSlashJointNodePathMapParsing(ontologyRoot, new Stack<>(), concept2path);

        System.out.println("Load mapping file " + strFileMappings);
        Mappings mappings = Mappings.deserialize(new File(strFileMappings), true);




        for(InstanceIdentifier instanceIdentifier:mappings.getSeedInstances()){

        }

    }


}
