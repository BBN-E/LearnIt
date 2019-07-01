package com.bbn.akbc.neolearnit.tmp;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.serializers.binary_event.BratAnnotationObserver;

import java.io.File;

public class SerializeBrat {
    public static void main(String[] args) throws Exception {
        String paramPath = args[0];
        LearnItConfig.loadParams(new File(paramPath));
        String labelledMappingsPath = args[1];
        String outputDirectory = args[2];

        Mappings labeledMappings = Mappings.deserialize(new File(labelledMappingsPath), true);
        BratAnnotationObserver bratAnnotationObserver = new BratAnnotationObserver(outputDirectory);
        bratAnnotationObserver.observe(labeledMappings);
        bratAnnotationObserver.build();
    }
}
