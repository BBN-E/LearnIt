package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.neolearnit.common.Annotation;
import com.bbn.akbc.neolearnit.common.Domain;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.bilingual.BilingualDocTheory;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.common.targets.TargetFactory;
import com.bbn.akbc.neolearnit.common.util.TabularPathListsConverter;
import com.bbn.akbc.neolearnit.labelers.*;
import com.bbn.akbc.neolearnit.loaders.LoaderUtils;
import com.bbn.akbc.neolearnit.mappings.AutoPopulatedMappingsGenerator;
import com.bbn.akbc.neolearnit.mappings.filters.TargetFilterWithCache;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;
import com.bbn.akbc.neolearnit.serializers.SerifXMLSerializer;
import com.bbn.akbc.neolearnit.util.GeneralUtils;
import com.bbn.akbc.neolearnit.util.GenericEventDetector;
import com.bbn.bue.common.parameters.exceptions.MissingRequiredParameter;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.io.SerifXMLWriter;
import com.bbn.serif.theories.DocTheory;

import java.io.File;
import java.util.*;

public class LearnItDecoder {

    public static List<DocTheory> realLearnItDecoder(Mappings mappings, List<DocTheory> docTheories, String targetName, TargetAndScoreTableLabeler targetAndScoreTableLabeler, boolean shouldOutputInComplete) throws Exception {
        Target target = TargetFactory.fromNamedString(targetName);
        if(mappings == null){
            mappings = AutoPopulatedMappingsGenerator.generateMappings(target, docTheories, new ArrayList<>());
        }

        LabelTrackingObserver labelTrackingObserverPattern = new LabelTrackingObserver(LabelTrackingObserver.LabelerTask.pattern);
        LabelTrackingObserver instanceTriggerObserver = new LabelTrackingObserver(LabelTrackingObserver.LabelerTask.triggerText);
        targetAndScoreTableLabeler.addLabelTrackingObserver(labelTrackingObserverPattern);
        targetAndScoreTableLabeler.addLabelTrackingObserver(instanceTriggerObserver);
        Annotation.InMemoryAnnotationStorage inMemoryAnnotationStorage = targetAndScoreTableLabeler.LabelMappings(mappings, new Annotation.InMemoryAnnotationStorage());

        if (LearnItConfig.optionalParamTrue("use_human_label")) {
            // Human adjudication
            File annotationFile = new File(Domain.getHumanLabeledMappingsMainPath());

            Mappings humanMappings = Mappings.deserialize(annotationFile, true);
            TargetFilterWithCache targetFilterWithCache = new TargetFilterWithCache(false);
            targetFilterWithCache.setFocusTarget(target);
            humanMappings = targetFilterWithCache.makeFiltered(humanMappings);
            Annotation.InMemoryAnnotationStorage humanAnnotation = new Annotation.InMemoryAnnotationStorage(humanMappings);
            HumanAdjudicateLabeler addLabelTrackingObserver = new HumanAdjudicateLabeler(humanAnnotation);
            addLabelTrackingObserver.addLabelTrackingObserver(labelTrackingObserverPattern);
            inMemoryAnnotationStorage = addLabelTrackingObserver.LabelMappings(null, inMemoryAnnotationStorage);
            // End human adjudication
        }

        if(shouldOutputInComplete){
            LabelAllUnLabeledInstanceLabeler labelAllUnLabeledInstanceLabeler = new LabelAllUnLabeledInstanceLabeler();
            inMemoryAnnotationStorage = labelAllUnLabeledInstanceLabeler.LabelMappings(mappings,inMemoryAnnotationStorage);
        }

        if(LearnItConfig.optionalParamTrue("use_human_label") || shouldOutputInComplete){
            // Flip annotation
            FlipNegativeLabelIntoPositive flipNegativeLabelIntoPositive = new FlipNegativeLabelIntoPositive();
            flipNegativeLabelIntoPositive.addLabelTrackingObserver(labelTrackingObserverPattern);
            inMemoryAnnotationStorage = flipNegativeLabelIntoPositive.LabelMappings(null, inMemoryAnnotationStorage);
            // End Flip annotation
        }

        if(LearnItConfig.optionalParamTrue("use_human_label")){
            DropNALabeler dropNALabeler = new DropNALabeler();
            inMemoryAnnotationStorage = dropNALabeler.LabelMappings(null,inMemoryAnnotationStorage);
        }

        Map<InstanceIdentifier, Map<LabelPattern, List<Symbol>>> intancePatternMap = labelTrackingObserverPattern.getInstanceIdentifierToLabeledPatternToLabel();
        Map<InstanceIdentifier, Map<LabelPattern, List<Symbol>>> intancetriggerTextMap = instanceTriggerObserver.getInstanceIdentifierToLabeledPatternToLabel();
        Map<String, DocTheory> docIdToDocTheory = new HashMap<>();
        for (DocTheory docTheory : docTheories) {
            docIdToDocTheory.put(docTheory.docid().asString(), docTheory);
        }
        SerifXMLSerializer serifXMLSerializer = new SerifXMLSerializer(null, docIdToDocTheory, SerifXMLSerializer.TaskType.valueOf(targetName));
        serifXMLSerializer.setInstanceIdentifierToLabeledPatternToPattern(intancePatternMap);
        serifXMLSerializer.setInstanceIdentifierToTriggerText(intancetriggerTextMap);
        serifXMLSerializer.observe(inMemoryAnnotationStorage.convertToMappings());
        return serifXMLSerializer.buildNewDocTheory();
    }

    public static void main(String[] args) throws Exception {

        LearnItConfig.loadParams(new File(args[0]));
        String doclist = args[1];
        String outputFolder = args[2];
        boolean shouldOutputInComplete = Boolean.parseBoolean(args[3]);

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


        Set<String> stagesToRun = new HashSet<>(LearnItConfig.getList("stages_to_run"));

        if (stagesToRun.contains("unary_entity")) {
            TargetAndScoreTableLabeler targetAndScoreTableLabeler;
            try {
                List<String> extractorPaths = LearnItConfig.getList("unary_entity_extractors");
                targetAndScoreTableLabeler = TargetAndScoreTableLabeler.fromExtractorDirectoryList(extractorPaths, false, false);
            } catch (MissingRequiredParameter e) {
                targetAndScoreTableLabeler = TargetAndScoreTableLabeler.fromOntologyYamlAndLatestExtractors(Domain.getOntologyNameToPathMap().get("unaryEntity"), false, false);
            }
            String targetName = "unary_entity";
            docTheoryList = realLearnItDecoder(null,docTheoryList, targetName, targetAndScoreTableLabeler,shouldOutputInComplete);
        }
        if (stagesToRun.contains("binary_entity_entity")) {
            TargetAndScoreTableLabeler targetAndScoreTableLabeler;
            try {
                List<String> extractorPaths = LearnItConfig.getList("binary_entity_entity_extractors");
                targetAndScoreTableLabeler = TargetAndScoreTableLabeler.fromExtractorDirectoryList(extractorPaths, false, false);
            } catch (MissingRequiredParameter e) {
                targetAndScoreTableLabeler = TargetAndScoreTableLabeler.fromOntologyYamlAndLatestExtractors(Domain.getOntologyNameToPathMap().get("binaryEntity"), false, false);
            }
            String targetName = "binary_entity_entity";
            docTheoryList = realLearnItDecoder(null,docTheoryList, targetName, targetAndScoreTableLabeler,shouldOutputInComplete);
        }
        if (stagesToRun.contains("generic_event")) {
            String nounWhiteList = LearnItConfig.get("generic_event_noun_whitelist");
            String blacklist = LearnItConfig.get("generic_event_blacklist");
            GenericEventDetector genericEventDetector = new GenericEventDetector(nounWhiteList, blacklist);
            List<DocTheory> resolvedDocTheories = new ArrayList<>();
            for (DocTheory docTheory : docTheoryList) {
                resolvedDocTheories.add(genericEventDetector.addEventMentions(docTheory));
            }

            docTheoryList = resolvedDocTheories;
        }
        if (stagesToRun.contains("unary_event_and_binary_event_argument_decoding") || stagesToRun.contains("unary_event_decoding")) {
            List<String> extractorPath = null;
            String targetName = null;
            TargetAndScoreTableLabeler targetAndScoreTableLabeler;
            try {
                extractorPath = LearnItConfig.getList("unary_event_and_binary_event_argument_extractors");
                targetName = "unary_event_and_binary_event_argument";
                targetAndScoreTableLabeler = TargetAndScoreTableLabeler.fromExtractorDirectoryList(extractorPath, false, false);
            } catch (MissingRequiredParameter e) {
                try {
                    extractorPath = LearnItConfig.getList("unary_event_extractors");
                    targetName = "unary_event";
                    targetAndScoreTableLabeler = TargetAndScoreTableLabeler.fromExtractorDirectoryList(extractorPath, false, false);
                } catch (MissingRequiredParameter ef) {
                    targetName = "unary_event";
                    targetAndScoreTableLabeler = TargetAndScoreTableLabeler.fromOntologyYamlAndLatestExtractors(Domain.getOntologyNameToPathMap().get("unaryEvent"), false, false);
                }
            }
            docTheoryList = realLearnItDecoder(null,docTheoryList, targetName, targetAndScoreTableLabeler,shouldOutputInComplete);
        }
        if (stagesToRun.contains("binary_event_event_decoding")) {
            TargetAndScoreTableLabeler targetAndScoreTableLabeler;
            try {
                List<String> extractorPaths = LearnItConfig.getList("binary_event_event_extractors");
                targetAndScoreTableLabeler = TargetAndScoreTableLabeler.fromExtractorDirectoryList(extractorPaths, false, false);
            } catch (MissingRequiredParameter ef) {
                targetAndScoreTableLabeler = TargetAndScoreTableLabeler.fromOntologyYamlAndLatestExtractors(Domain.getOntologyNameToPathMap().get("binaryEvent"), false, false);
            }
            String targetName = "binary_event_event";
            docTheoryList = realLearnItDecoder(null,docTheoryList, targetName, targetAndScoreTableLabeler,shouldOutputInComplete);
        }
        SerifXMLWriter serifXMLWriter = SerifXMLWriter.create();
        for (DocTheory docTheory : docTheoryList) {
            serifXMLWriter.saveTo(docTheory, outputFolder + File.separator + docTheory.docid().asString() + ".xml");
        }

    }
}
