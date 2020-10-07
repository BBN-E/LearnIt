package com.bbn.akbc.neolearnit.labelers;

import com.bbn.akbc.neolearnit.common.Annotation;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;
import com.bbn.akbc.neolearnit.serializers.observations.OpenNREJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;

import java.io.File;
import java.util.List;

public class OpenNREJSONLabeler implements MappingsLabeler {


    final List<File> openNREJsonPaths;


    public OpenNREJSONLabeler(List<File> openNreJsonPaths) {
        this.openNREJsonPaths = openNreJsonPaths;
    }

    @Override
    public Annotation.InMemoryAnnotationStorage LabelMappings(Mappings original, Annotation.InMemoryAnnotationStorage labeledMappings) throws Exception {
        for (File file : openNREJsonPaths) {
            ObjectMapper mapper = new ObjectMapper();
            OpenNREJson[] openNREJsons = mapper.readValue(file, OpenNREJson[].class);
            for (OpenNREJson openNREJson : openNREJsons) {
                InstanceIdentifier instanceIdentifier = new InstanceIdentifier(
                        openNREJson.docid,
                        openNREJson.sentid,
                        openNREJson.slot0StartIdx,
                        openNREJson.slot0EndIdx,
                        InstanceIdentifier.SpanningType.EventMention,
                        Optional.absent(),
                        openNREJson.slot0EntityType,
                        false,
                        openNREJson.slot1StartIdx,
                        openNREJson.slot1EndIdx,
                        InstanceIdentifier.SpanningType.EventMention,
                        Optional.absent(),
                        openNREJson.slot1EntityType,
                        false
                );
                String relationType = openNREJson.semantic_class;
                labeledMappings.addOrChangeAnnotation(instanceIdentifier, new LabelPattern(relationType, relationType.equals("NA") ? Annotation.FrozenState.FROZEN_BAD : Annotation.FrozenState.FROZEN_GOOD));
            }
        }
        return labeledMappings;
    }

}
