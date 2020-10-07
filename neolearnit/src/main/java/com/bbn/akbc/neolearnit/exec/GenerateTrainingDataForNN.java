package com.bbn.akbc.neolearnit.exec;


import com.bbn.akbc.neolearnit.common.Domain;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.bilingual.BilingualDocTheory;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.common.util.TabularPathListsConverter;
import com.bbn.akbc.neolearnit.labelers.TargetAndScoreTableLabeler;
import com.bbn.akbc.neolearnit.loaders.LoaderUtils;
import com.bbn.akbc.neolearnit.mappings.AutoPopulatedMappingsGenerator;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.util.GeneralUtils;
import com.bbn.akbc.neolearnit.util.GenericEventDetector;
import com.bbn.bue.common.exceptions.NotImplementedException;
import com.bbn.bue.common.parameters.exceptions.MissingRequiredParameter;
import com.bbn.serif.io.SerifXMLWriter;
import com.bbn.serif.theories.DocTheory;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.bbn.akbc.neolearnit.exec.LearnItDecoder.realLearnItDecoder;

public class GenerateTrainingDataForNN {

    public static List<DocTheory> decodeFromYamlAndLatestExtractors(String stageToRunInStr, List<DocTheory> docTheoryList, boolean shouldOutputInComplete) throws Exception {

        Set<String> stagesToRun = new HashSet<>();
        for (String stageName : stageToRunInStr.split(",")) {
            stagesToRun.add(stageName.trim());
        }

        if (stagesToRun.contains("unary_entity")) {
            TargetAndScoreTableLabeler targetAndScoreTableLabeler;
            targetAndScoreTableLabeler = TargetAndScoreTableLabeler.fromOntologyYamlAndLatestExtractors(Domain.getOntologyNameToPathMap().get("unaryEntity"), false, false);
            String targetName = "unary_entity";
            docTheoryList = realLearnItDecoder(null,docTheoryList, targetName, targetAndScoreTableLabeler,shouldOutputInComplete);
        }
        if (stagesToRun.contains("binary_entity_entity")) {
            TargetAndScoreTableLabeler targetAndScoreTableLabeler;
            targetAndScoreTableLabeler = TargetAndScoreTableLabeler.fromOntologyYamlAndLatestExtractors(Domain.getOntologyNameToPathMap().get("binaryEntity"), false, false);

            String targetName = "binary_entity_entity";
            docTheoryList = realLearnItDecoder(null,docTheoryList, targetName, targetAndScoreTableLabeler,shouldOutputInComplete);
        }
        if (stagesToRun.contains("unary_event")) {
            String targetName = "unary_event";
            TargetAndScoreTableLabeler targetAndScoreTableLabeler = TargetAndScoreTableLabeler.fromOntologyYamlAndLatestExtractors(Domain.getOntologyNameToPathMap().get("unaryEvent"), false, false);
            docTheoryList = realLearnItDecoder(null,docTheoryList, targetName, targetAndScoreTableLabeler,shouldOutputInComplete);
        }
        if (stagesToRun.contains("binary_event_entity_or_value")) {
            String targetName = "binary_event_entity_or_value";
            TargetAndScoreTableLabeler targetAndScoreTableLabeler = TargetAndScoreTableLabeler.fromOntologyYamlAndLatestExtractors(Domain.getOntologyNameToPathMap().get("binaryEventEntityOrValueMention"), false, false);
            docTheoryList = realLearnItDecoder(null,docTheoryList, targetName, targetAndScoreTableLabeler,shouldOutputInComplete);
        }
        if (stagesToRun.contains("binary_event_event")) {
            TargetAndScoreTableLabeler targetAndScoreTableLabeler;
            targetAndScoreTableLabeler = TargetAndScoreTableLabeler.fromOntologyYamlAndLatestExtractors(Domain.getOntologyNameToPathMap().get("binaryEvent"), false, false);

            String targetName = "binary_event_event";
            docTheoryList = realLearnItDecoder(null,docTheoryList, targetName, targetAndScoreTableLabeler,shouldOutputInComplete);
        }
        return docTheoryList;
    }



    public static void main(String[] args) throws Exception {

        LearnItConfig.loadParams(new File(args[0]));
        String doclist = args[1];
        String serifOutputFolder = args[2];
        String activatedTargets = args[3];
        boolean shouldOutputInComplete = Boolean.parseBoolean(args[4]);

        List<DocTheory> docTheoryList = new ArrayList<>();
        // Load document
        if (LearnItConfig.optionalParamTrue("bilingual")) {
            List<BilingualDocTheory> bilingualDocTheoryList = new ArrayList<>();
            Map<String, Map<String, String>> docIdToEntries = TabularPathListsConverter.parseSingleTabularList(doclist);
            Collection<Map<String, String>> biEntries = docIdToEntries.values();
            bilingualDocTheoryList.addAll(LoaderUtils.resolveBilingualDocTheoryFromBiEntries(biEntries));
            for (BilingualDocTheory bilingualDocTheory : bilingualDocTheoryList) {
                docTheoryList.add(bilingualDocTheory.getSourceDoc());
            }
        } else {
            List<String> fileList = GeneralUtils.readLinesIntoList(doclist);
            docTheoryList.addAll(LoaderUtils.resolvedDocTheoryFromPathList(fileList));

        }
        // End load document

        docTheoryList = decodeFromYamlAndLatestExtractors(activatedTargets,docTheoryList,shouldOutputInComplete);

        SerifXMLWriter serifXMLWriter = SerifXMLWriter.create();
        for (DocTheory docTheory : docTheoryList) {
            serifXMLWriter.saveTo(docTheory, serifOutputFolder + File.separator + docTheory.docid().asString() + ".xml");
        }

    }
}
