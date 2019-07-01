package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.neolearnit.common.Annotation;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;
import com.bbn.akbc.neolearnit.storage.StorageUtils;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import java.io.File;
import java.util.Map;

public class CreateLabelMappingsFromSingleColumnCSV {
    public static void main(String[] args) throws Exception {
        String csvPath = args[0];
        String outputLabeledMappingsPath = args[1];

        CsvMapper csvMapper = new CsvMapper();
        CsvSchema csvSchema = CsvSchema.emptySchema().withHeader();
        MappingIterator<Map<String, String>> iterator = csvMapper.reader(Map.class).with(csvSchema).readValues(new File(csvPath));
        Annotation.InMemoryAnnotationStorage inMemoryAnnotationStorage = new Annotation.InMemoryAnnotationStorage();
        ObjectMapper objectMapper = StorageUtils.getMapperWithoutTyping();

        while (iterator.hasNext()) {
            Map<String, String> en = iterator.next();
//            InstanceIdentifier instanceIdentifier = objectMapper.readValue(en.get("instanceIdentifier"), InstanceIdentifier.class);
//            InstanceIdentifier instanceIdentifier = objectMapper.convertValue(en.get("learnit_instanceidentifier"), InstanceIdentifier.class);
            InstanceIdentifier instanceIdentifier = objectMapper.readValue(en.get("learnit_instanceidentifier"), InstanceIdentifier.class);
            String relationType = en.get("relationType");
            Boolean positive = Boolean.parseBoolean(en.get("positive"));
            Annotation.FrozenState frozenState = positive ? Annotation.FrozenState.FROZEN_GOOD : Annotation.FrozenState.FROZEN_BAD;
            inMemoryAnnotationStorage.addAnnotation(instanceIdentifier, new LabelPattern(relationType, frozenState));
        }
        inMemoryAnnotationStorage.convertToMappings().serialize(new File(outputLabeledMappingsPath), true);
    }
}
