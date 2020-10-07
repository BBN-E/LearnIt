package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.serializers.unary_event.SharableEventAnnotationObserver;

import java.io.File;

public class SerializeLabeledMappingsToSharableEventAnnotation {
    public static void main(String[] args) throws Exception {
        String paramPath = args[0];
        LearnItConfig.loadParams(new File(paramPath));
        String labelledMappingsPath = args[1];
        String metadataPath = args[2];
        String outputPrefix = args[3];

        Mappings labeledMappings = Mappings.deserialize(new File(labelledMappingsPath), true);

        SharableEventAnnotationObserver sharableEventAnnotationObserver = new SharableEventAnnotationObserver(metadataPath, outputPrefix);
        sharableEventAnnotationObserver.observe(labeledMappings);
        sharableEventAnnotationObserver.build();
    }
}
