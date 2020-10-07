package com.bbn.akbc.neolearnit.labelers;

import com.bbn.akbc.neolearnit.common.Annotation;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.util.GeneralUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MergeLabeledMappingsFromList {
    public static void main(String[] args) throws Exception {
        if (args.length < 2)
            throw new ArrayIndexOutOfBoundsException("Usage: input-list output-labeled-mappings-path");

        String inputListPath = args[0];
        String outputLabelMappingsPath = args[1];
        List<String> labeledMappingsList = GeneralUtils.readLinesIntoList(inputListPath);
        List<Annotation.InMemoryAnnotationStorage> pendingMergeList = new ArrayList<>();
        for (String inputMappingsPath:labeledMappingsList) {
            Annotation.InMemoryAnnotationStorage currentLabeledAnnotationStorage = new Annotation.InMemoryAnnotationStorage(Mappings.deserialize(new File(inputMappingsPath), true));
            pendingMergeList.add(currentLabeledAnnotationStorage);
        }
        Annotation.InMemoryAnnotationStorage resolvedLabeledMappings = Annotation.mergeAnnotation(pendingMergeList);
        resolvedLabeledMappings.convertToMappings().serialize(new File(outputLabelMappingsPath), true);
    }
}
