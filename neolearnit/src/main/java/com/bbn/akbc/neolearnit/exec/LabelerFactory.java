package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.neolearnit.common.Annotation;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.labelers.*;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.bue.common.exceptions.NotImplementedException;

import java.io.File;

public class LabelerFactory {


    public static void main(String[] args) throws Exception{
        String paramPath = args[0];
        LearnItConfig.loadParams(new File(paramPath));
        String mappingsPath = args[1];
        String labeledMappingsPath = args[2];
        Annotation.InMemoryAnnotationStorage labeledMappings;
        if(labeledMappingsPath.equals("EMPTY_EXTRACTOR")){
            labeledMappings = new Annotation.InMemoryAnnotationStorage();
        }
        else{
            labeledMappings = new Annotation.InMemoryAnnotationStorage(Mappings.deserialize(new File(labeledMappingsPath),true));
        }
        String outputLabeledMappingsPath = args[3];
        String labelerClassName = args[4];
        MappingsLabeler newMappingsLabeler;
        if (labelerClassName.equals("TargetAndScoreTableLabeler")) {
            newMappingsLabeler = TargetAndScoreTableLabeler.fromSingleTargetAndScoreTable(args[5], false, false);
        }
        else if(labelerClassName.equals("DownSamplingLabelMappingsLabeler")){
            newMappingsLabeler = new DownSamplingLabelMappingsLabeler(Double.parseDouble(args[5]), Boolean.parseBoolean(args[6]));

        }
        else if (labelerClassName.equals("LabelEverythingLabeler")){
            newMappingsLabeler = new LabelEverythingLabeler(args[5], args[6]);
        } else if (labelerClassName.equals("RoleFillerTypeConstraint")) {
            newMappingsLabeler = new RoleFillerTypeConstraint(args[5], args[6], Boolean.parseBoolean(args[7]), Boolean.parseBoolean(args[8]));
        }
        else{
            throw new NotImplementedException();
        }
        Annotation.InMemoryAnnotationStorage outLabelMappings = newMappingsLabeler.LabelMappings(Mappings.deserialize(new File(mappingsPath), true), labeledMappings);
        outLabelMappings.convertToMappings().serialize(new File(outputLabeledMappingsPath), true);
    }
}
