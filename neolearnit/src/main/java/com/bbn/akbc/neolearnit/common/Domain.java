package com.bbn.akbc.neolearnit.common;

import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.google.common.collect.ImmutableMap;

import java.io.File;
import java.util.Map;

public class Domain {
    public static String getExtractorsPath() {
        return new File(LearnItConfig.get("domain_root") + File.separator + "extractors").getAbsolutePath();
    }

    public static String getOntologyPath() {
        return new File(LearnItConfig.get("domain_root") + File.separator + "ontology").getAbsolutePath();
    }

    public static String getHumanLabeledMappingsMainPath() {
        return new File(LearnItConfig.get("domain_root") + File.separator + "labeled_mappings" + File.separator + LearnItConfig.get("corpus_name") + ".sjson").getAbsolutePath();
    }

    public static String getHumanLabeledMappingsOtherPath() {
        return new File(LearnItConfig.get("domain_root") + File.separator + "labeled_mappings" + File.separator + LearnItConfig.get("corpus_name") + "_other.sjson").getAbsolutePath();
    }

    public static Map<String, String> getOntologyNameToPathMap() {
        return ImmutableMap.<String, String>builder()
                .put("unaryEvent", new File(getOntologyPath() + File.separator + "unary_event_ontology.yaml").getAbsolutePath())
                .put("binaryEvent", new File(getOntologyPath() + File.separator + "binary_event_ontology.yaml").getAbsolutePath())
                .put("binaryEntity", new File(getOntologyPath() + File.separator + "binary_entity_ontology.yaml").getAbsolutePath())
                .put("binaryEventEntityOrValueMention", new File(getOntologyPath() + File.separator + "binary_event_entity_or_value_mention.yaml").getAbsolutePath())
                .put("unaryEntity", new File(getOntologyPath() + File.separator + "unary_entity_ontology.yaml").getAbsolutePath())
                .build();
    }

    public static String getConvertedTargetName(String originalTargetName){
        switch (originalTargetName) {
            case "unaryEvent":
                return "unary_event";
            case "binaryEvent":
                return "binary_event_event";
            case "binaryEntity":
                return "binary_entity_entity";
            case "binaryEventEntityOrValueMention":
                return "binary_event_entity_or_value";
            case "unaryEntity":
                return "unary_entity";
        }
        throw new IllegalArgumentException();
    }

    public static String getOriginalTargetName(String convertedTargetName){
        switch (convertedTargetName){
            case "unary_event":
                return "unaryEvent";
            case "binary_event_event":
                return "binaryEvent";
            case "binary_entity_entity":
                return "binaryEntity";
            case "binary_event_entity_or_value":
                return "binaryEventEntityOrValueMention";
            case "unary_entity":
                return "unaryEntity";
        }
        throw new IllegalArgumentException();
    }

    public static String getTargetNameToOntologyRootName(String targetName){
        switch (targetName) {
            case "binary_entity_entity":
                return "Binary-Entity";
            case "binary_event_entity_or_value":
                return "Binary-Event-Entity-ValueMention";
            case "binary_event_event":
                return "Binary-Event";
            case "unary_entity":
                return "Unary-Entity";
            case "unary_event":
                return "Event";
        }
        throw new IllegalArgumentException();
    }

    public static String getUIRuntimePath(){
        return new File(LearnItConfig.get("domain_root")+File.separator+"runtime").getAbsolutePath();
    }

    public static class ExtractorEntry{
        TargetAndScoreTables targetAndScoreTables;
        BBNInternalOntology.BBNInternalOntologyNode ontologyNode;

        public ExtractorEntry(TargetAndScoreTables targetAndScoreTables, BBNInternalOntology.BBNInternalOntologyNode ontologyNode){
            this.targetAndScoreTables = targetAndScoreTables;
            this.ontologyNode = ontologyNode;
        }
        public TargetAndScoreTables getTargetAndScoreTables(){
            return this.targetAndScoreTables;
        }

        public BBNInternalOntology.BBNInternalOntologyNode getOntologyNode(){
            return this.ontologyNode;
        }
    }
}
