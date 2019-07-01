package com.bbn.akbc.neolearnit.bootstrapping;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.targets.TargetFactory;
import com.bbn.akbc.neolearnit.labelers.LearnItRelationPatternLabeler;
import com.bbn.akbc.neolearnit.mappings.filters.TargetFilter;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.serializers.binary_event.OpenNREObserver;

import java.io.File;


public class GenerateTrainingDataFromSeedsForOpenNRE {
    static boolean USE_PROP_PATTERN_ONLY = false;
    public static void main(String[] args) throws Exception{

        String strFileParam = args[0];
        String strFileExtractor = args[1];
        String strFileMappings = args[2];
        String strOutFilePrefix = args[3];

        LearnItConfig.loadParams(new File(strFileParam));

        String relationType = args[4];
        int MAX_INSTANCES_PER_SEED = Integer.parseInt(args[5]);
        double NEGATIVE_SAMPLING_RATIO = Double.parseDouble(args[6]);

        String strMode = args[7];
        LearnItRelationPatternLabeler.MODE mode = LearnItRelationPatternLabeler.MODE.valueOf(strMode);


        Mappings mappings = Mappings.deserialize(new File(strFileMappings), true);
        TargetFilter targetFilter = new TargetFilter(TargetFactory.makeBinaryEventEventTarget());
        mappings = targetFilter.makeFiltered(mappings);


        LearnItRelationPatternLabeler learnItRelationPatternLabeler = new LearnItRelationPatternLabeler(mode, strFileExtractor, relationType, MAX_INSTANCES_PER_SEED, USE_PROP_PATTERN_ONLY, NEGATIVE_SAMPLING_RATIO);
        Mappings labeledMappings = learnItRelationPatternLabeler.LabelMappings(mappings);

        OpenNREObserver openNREObserver = new OpenNREObserver(new File(strOutFilePrefix));
        openNREObserver.observe(labeledMappings);
        openNREObserver.build();

    }

}
