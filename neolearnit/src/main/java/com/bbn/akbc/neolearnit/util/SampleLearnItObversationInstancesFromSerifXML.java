package com.bbn.akbc.neolearnit.util;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.bilingual.BilingualDocTheory;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.common.targets.TargetFactory;
import com.bbn.akbc.neolearnit.common.util.TabularPathListsConverter;
import com.bbn.akbc.neolearnit.loaders.LoaderUtils;
import com.bbn.akbc.neolearnit.mappings.AutoPopulatedMappingsGenerator;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.LearnItObservation;
import com.bbn.serif.theories.DocTheory;

import java.io.File;
import java.util.*;

import static com.bbn.akbc.neolearnit.util.DumpLearnItObversationWithSelectedInstances.selectInstanceForLearnItObversation;
import static com.bbn.akbc.neolearnit.util.DumpLearnItObversationWithSelectedInstances.serializeSampledInstance;

public class SampleLearnItObversationInstancesFromSerifXML{
    public static void main(String[] args) throws Exception{
        String paramsFile = args[0];
        String relation = args[1];
        String doclist = args[2];
        DumpLearnItObversationWithSelectedInstances.WorkingMode workingMode = DumpLearnItObversationWithSelectedInstances.WorkingMode.valueOf(args[3]);
        int NUMBER_OF_INSTANCES_PER_LEARNIT_OBVERSATION_CAP = Integer.parseInt(args[4]);
        String outputPath = args[5];

        LearnItConfig.loadParams(new File(paramsFile));
        Target target = TargetFactory.fromNamedString(relation);

        List<DocTheory> docTheoryList = new ArrayList<>();
        List<BilingualDocTheory> bilingualDocTheoryList = new ArrayList<>();
        if (LearnItConfig.optionalParamTrue("bilingual")) {
            Map<String, Map<String, String>> docIdToEntries = TabularPathListsConverter.parseSingleTabularList(doclist);
            Collection<Map<String, String>> biEntries = docIdToEntries.values();
            bilingualDocTheoryList.addAll(LoaderUtils.resolveBilingualDocTheoryFromBiEntries(biEntries));
        } else {
            List<String> fileList = GeneralUtils.readLinesIntoList(doclist);
            docTheoryList.addAll(LoaderUtils.resolvedDocTheoryFromPathList(fileList));
        }
        Mappings mappings = AutoPopulatedMappingsGenerator.generateMappings(target, docTheoryList, bilingualDocTheoryList);
        List<Mappings> mappingsList = new ArrayList<>();
        mappingsList.add(mappings);

        Map<LearnItObservation, Set<InstanceIdentifierFilterForAnnotation.BasicInstanceIdentifier>> patternToSelectedInstancesIdentifier = selectInstanceForLearnItObversation(mappingsList,workingMode,NUMBER_OF_INSTANCES_PER_LEARNIT_OBVERSATION_CAP);

        serializeSampledInstance(patternToSelectedInstancesIdentifier,outputPath);
    }
}
