package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.neolearnit.common.Annotation;
import com.bbn.akbc.neolearnit.common.BBNInternalOntology;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;
import com.bbn.akbc.neolearnit.serializers.unary_event.NLPLingoAnnotationObserver;
import com.bbn.akbc.neolearnit.util.GeneralUtils;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SerializeLabeledMappingsToNLPLingo {
    public static Map<String, Mappings> partitioner(File careListFile, BBNInternalOntology.BBNInternalOntologyNode ontologyRoot, Annotation.InMemoryAnnotationStorage annotationStorage) throws IllegalAccessException, IOException {

        //BucketName:oldType->newType
        Map<String, Map<String, String>> modelNameToTypeMap = new HashMap<>();
        Multimap<String, String> typeToModelName = HashMultimap.create();
        Multimap<String, BBNInternalOntology.BBNInternalOntologyNode> nodeNameToNode = ontologyRoot.getPropToNodeMultiMap(new BBNInternalOntology.BBNInternalOntologyNode.PropStringGetter("originalKey"));
        for (String line : GeneralUtils.readLinesIntoList(careListFile.toString())) {
            if (line.startsWith("#")) continue;
            String[] careTypes = line.split(",");
            String modelName = careTypes[0];
            Map<String, String> typeMap = new HashMap<>();
            for (int i = 1; i < careTypes.length; ++i) {
                String careType = careTypes[i];
                careType = careType.trim();

                if (careType.startsWith("*") && careType.endsWith("*")) {
                    careType = careType.substring(1, careType.length() - 1);
                    if (!nodeNameToNode.containsKey(careType)) {
                        System.out.println("Cannot find node " + careType + ". Skipping!");
                        continue;
                    }
                    Set<String> possibleChildrenNames = new HashSet<>();
                    for (BBNInternalOntology.BBNInternalOntologyNode ontologyNode : nodeNameToNode.get(careType)) {
                        Multimap<String, BBNInternalOntology.BBNInternalOntologyNode> childrenNodeMap = ontologyNode.getPropToNodeMultiMap(new BBNInternalOntology.BBNInternalOntologyNode.PropStringGetter("originalKey"));
                        possibleChildrenNames.addAll(childrenNodeMap.keySet());
                    }
                    for (String unResolvedType : possibleChildrenNames) {
                        typeMap.put(unResolvedType, careType);
                    }
                } else {
                    if (!nodeNameToNode.containsKey(careType)) {
                        System.out.println("Cannot find node " + careType + ". Skipping!");
                        continue;
                    }
                    typeMap.put(careType, careType);
                }
            }
            for (String unResolvedType : typeMap.keySet()) {
                typeToModelName.put(unResolvedType, modelName);
            }
            modelNameToTypeMap.put(modelName, typeMap);
        }
        Map<String, Annotation.InMemoryAnnotationStorage> resolvedModelBuckets = new HashMap<>();
        Set<String> labeledBeingDropped = new HashSet<>();
        for (InstanceIdentifier instanceIdentifier : annotationStorage.getAllInstanceIdentifier()) {
            for (LabelPattern labelPattern : annotationStorage.lookupInstanceIdentifierAnnotation(instanceIdentifier)) {
                if (typeToModelName.get(labelPattern.getLabel()).size() < 1) {
                    labeledBeingDropped.add(labelPattern.getLabel());
                    continue;
                }
                for (String modelName : typeToModelName.get(labelPattern.getLabel())) {
                    String resolvedTypeInCurrentModel = modelNameToTypeMap.get(modelName).get(labelPattern.getLabel());
                    Annotation.InMemoryAnnotationStorage buf = resolvedModelBuckets.getOrDefault(modelName, new Annotation.InMemoryAnnotationStorage());
                    buf.addAnnotation(instanceIdentifier, new LabelPattern(resolvedTypeInCurrentModel, labelPattern.getFrozenState()));
                    resolvedModelBuckets.put(modelName, buf);
                }
            }
        }
        for (String droppedLabel : labeledBeingDropped) {
            System.out.println(droppedLabel + " event examples are being dropped because it isn't on the carelist.");
        }
        Map<String, Mappings> ret = new HashMap<>();
        for (String modelName : resolvedModelBuckets.keySet()) {
            Annotation.InMemoryAnnotationStorage inMemoryAnnotationStorage = resolvedModelBuckets.get(modelName);
            ret.put(modelName, inMemoryAnnotationStorage.convertToMappings());
        }
        return ret;
    }


    public static void main(String[] args) throws Exception {
        String paramPath = args[0];
        LearnItConfig.loadParams(new File(paramPath));
        String labelledMappingsPath = args[1];
        String careListPath = args[2];
        String yamlOntologyPath = args[3];
        String outputPrefix = args[4];

        Mappings labeledMappings = Mappings.deserialize(new File(labelledMappingsPath), true);
        BBNInternalOntology.BBNInternalOntologyNode ontologyRoot = BBNInternalOntology.BBNInternalOntologyNode.fromInternalOntologyFile(new File(yamlOntologyPath));
        Map<String, Mappings> partitionInBucket = partitioner(new File(careListPath), ontologyRoot, new Annotation.InMemoryAnnotationStorage(labeledMappings));
        for (String modelName : partitionInBucket.keySet()) {
            Mappings labeledMappingInBucket = partitionInBucket.get(modelName);
            NLPLingoAnnotationObserver nlpLingoAnnotationObserver = new NLPLingoAnnotationObserver(outputPrefix + File.separator + modelName, true);
            nlpLingoAnnotationObserver.observe(labeledMappingInBucket);
            nlpLingoAnnotationObserver.build();
        }

    }
}
