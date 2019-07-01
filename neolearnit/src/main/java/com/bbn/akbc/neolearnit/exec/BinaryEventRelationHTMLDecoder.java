package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.serializers.binary_event.HTMLVisualizationObserver;

import java.io.File;

public class BinaryEventRelationHTMLDecoder {
    public static void main(String[] args) throws Exception {
        String paramPath = args[0];
        LearnItConfig.loadParams(new File(paramPath));
        String labelledMappingsPath = args[1];
        String outputHTMLFilePath = args[2];

        Mappings labeledMappings = Mappings.deserialize(new File(labelledMappingsPath), true);
        HTMLVisualizationObserver htmlVisualizationObserver = new HTMLVisualizationObserver(outputHTMLFilePath);
        htmlVisualizationObserver.observe(labeledMappings);
        htmlVisualizationObserver.build();
    }
}
