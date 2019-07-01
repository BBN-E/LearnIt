package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.serializers.binary_event.OpenNREObserver;

import java.io.File;

public class SerializeLabeledMappingsToOpenNRE {
    public static void main(String[] args) throws Exception {
        String paramPath = args[0];
        LearnItConfig.loadParams(new File(paramPath));
        String labelledMappingsPath = args[1];
        String outputPrefix = args[2];

        Mappings labeledMappings = Mappings.deserialize(new File(labelledMappingsPath), true);
        OpenNREObserver openNREObserver = new OpenNREObserver(new File(outputPrefix));
        openNREObserver.observe(labeledMappings);
        openNREObserver.build();
    }
}
