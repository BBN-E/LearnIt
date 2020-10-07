package com.bbn.akbc.neolearnit.util;

import com.bbn.akbc.neolearnit.common.Annotation;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.labelers.TargetAndScoreTableLabeler;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;

import java.io.File;

public class UseYAMLAndLatestExtractorForLabelingMappings {

    public static void main(String[] args) throws Exception{
        String learnitConfigPath = args[0];
        LearnItConfig.loadParams(new File(learnitConfigPath));

        String yamlPath = args[1];
        String autoPopulatedMappingsPath = args[2];
        String outputPath = args[3];
        TargetAndScoreTableLabeler targetAndScoreTableLabeler = TargetAndScoreTableLabeler.fromOntologyYamlAndLatestExtractors(yamlPath,false,false);
        Mappings mappings = Mappings.deserialize(new File(autoPopulatedMappingsPath),true);
        Annotation.InMemoryAnnotationStorage labeledMappings = new Annotation.InMemoryAnnotationStorage();
        labeledMappings = targetAndScoreTableLabeler.LabelMappings(mappings,labeledMappings);
        labeledMappings.convertToMappings().serialize(new File(outputPath),true);
    }
}
