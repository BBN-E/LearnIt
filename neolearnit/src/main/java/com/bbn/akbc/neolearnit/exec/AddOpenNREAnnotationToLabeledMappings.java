package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.neolearnit.common.Annotation;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.labelers.OpenNREJSONLabeler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


@Deprecated
public class AddOpenNREAnnotationToLabeledMappings {

    public static void main(String[] args) throws Exception {
        String paramsFile = args[0];
        LearnItConfig.loadParams(new File(paramsFile));
        String openNREJSONPath = args[1];
        String outputLabeledMappingsPath = args[2];

        List<File> openNreJsonFileList = new ArrayList<>();
        openNreJsonFileList.add(new File(openNREJSONPath));

        OpenNREJSONLabeler openNREJSONLabeler = new OpenNREJSONLabeler(openNreJsonFileList);

        Annotation.InMemoryAnnotationStorage inMemoryAnnotationStorage = new Annotation.InMemoryAnnotationStorage();

        inMemoryAnnotationStorage = openNREJSONLabeler.LabelMappings(null, inMemoryAnnotationStorage);

        inMemoryAnnotationStorage.convertToMappings().serialize(new File(outputLabeledMappingsPath), true);
    }
}
