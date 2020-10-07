package com.bbn.akbc.neolearnit.util;

import com.bbn.akbc.common.FileUtil;
import com.bbn.akbc.neolearnit.common.Annotation;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.labelers.TargetAndScoreTableLabeler;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.serializers.binary_event.CausalJSONObserver;
import com.google.common.collect.Lists;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class EventEventRelationPatternDecoder {

    final static boolean changePatternTokensToLemma = true;

    public static void main(String[] args) throws Exception {
        String paramFile = args[0];
        String strJsonFileList = args[1]; // could also be a single json file
        String strOutputRelationJson = args[2];
        final boolean useOnlyPropPatterns = args[3].trim().toLowerCase().equals("props"); // "props" or "all"
        final int MIN_FREQ_EVENT_PAIRS = Integer.parseInt(args[4]);
        final boolean USE_TRIPLE_WHITE_LIST = args[5].trim().toLowerCase().equals("use_triple_whitelist"); // "use_triple_whitelist" or "na"
        if (USE_TRIPLE_WHITE_LIST) {
            String strFileTripleRelationEventPairs = args[6];
        }
        String extractorDirectory = args[7];

        System.out.println("=== Loading params...");
        LearnItConfig.loadParams(new File(paramFile));
        List<String> jsonFiles = new ArrayList<>();
        if (!strJsonFileList.endsWith("json")) {
            jsonFiles = FileUtil.readLinesIntoList(new File(strJsonFileList));
        } else {
            jsonFiles.add(strJsonFileList);
        }
        Annotation.InMemoryAnnotationStorage inMemoryAnnotationStorage = new Annotation.InMemoryAnnotationStorage();
        TargetAndScoreTableLabeler targetAndScoreTableLabeler = TargetAndScoreTableLabeler.fromExtractorDirectoryList(Lists.newArrayList(extractorDirectory), false, changePatternTokensToLemma);

        List<Mappings> patternMappingsList = new ArrayList<>();
        for(String jsonFile : jsonFiles){
            System.out.println("=== Process json file: " + jsonFile);
            Mappings mappings = Mappings.deserialize(new File(jsonFile), true);
            inMemoryAnnotationStorage = targetAndScoreTableLabeler.LabelMappings(mappings, inMemoryAnnotationStorage);
            patternMappingsList.add(mappings);
        }
        CausalJSONObserver causalJSONObserver = new CausalJSONObserver(new File(strOutputRelationJson), patternMappingsList, targetAndScoreTableLabeler.goodPatterns);
        causalJSONObserver.observe(inMemoryAnnotationStorage.convertToMappings());
        causalJSONObserver.build();
    }

}

