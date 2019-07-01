package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.neolearnit.common.Annotation;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MergeLabeledMappings {
    public static void main(String[] args) throws Exception {
        if (args.length < 2)
            throw new ArrayIndexOutOfBoundsException("You should at least point 1 input mappings and 1 output mappings");

        List<Annotation.InMemoryAnnotationStorage> pendingMergeList = new ArrayList<>();
        for (int i = 0; i < args.length - 1; ++i) {
            String inputMappingsPath = args[i];
            Annotation.InMemoryAnnotationStorage currentLabeledAnnotationStorage = new Annotation.InMemoryAnnotationStorage(Mappings.deserialize(new File(inputMappingsPath), true));
            pendingMergeList.add(currentLabeledAnnotationStorage);
        }
        Annotation.InMemoryAnnotationStorage resolvedLabeledMappings = Annotation.mergeAnnotation(pendingMergeList);
        resolvedLabeledMappings.convertToMappings().serialize(new File(args[args.length - 1]), true);
    }
}
