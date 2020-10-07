package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.neolearnit.common.Annotation;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.bilingual.BilingualDocTheory;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.common.targets.TargetFactory;
import com.bbn.akbc.neolearnit.common.util.TabularPathListsConverter;
import com.bbn.akbc.neolearnit.labelers.LabelTrackingObserver;
import com.bbn.akbc.neolearnit.labelers.TargetAndScoreTableLabeler;
import com.bbn.akbc.neolearnit.loaders.LoaderUtils;
import com.bbn.akbc.neolearnit.mappings.AutoPopulatedMappingsGenerator;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;
import com.bbn.akbc.neolearnit.serializers.SerifXMLSerializer;
import com.bbn.akbc.neolearnit.serializers.binary_event.OpenNREObserverSilent;
import com.bbn.akbc.neolearnit.util.GeneralUtils;
import com.bbn.akbc.neolearnit.util.GenericEventDetector;
import com.bbn.bue.common.parameters.exceptions.MissingRequiredParameter;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.io.SerifXMLWriter;
import com.bbn.serif.theories.DocTheory;
import com.bbn.akbc.neolearnit.labelers.LearnItRelationPatternNALabeler;

import java.io.File;
import java.util.*;

public class EERNASampler {


    public static Mappings genMappings(List<DocTheory> docTheories,String targetName) throws Exception {
        Target target = TargetFactory.fromNamedString(targetName);
        Mappings mappings = AutoPopulatedMappingsGenerator.generateMappings(target, docTheories, new ArrayList<>());
        return mappings;
    }

    public static void main(String[] args) throws Exception{

        long startTime = System.nanoTime();

        LearnItConfig.loadParams(new File(args[0]));
        String doclist = args[1];
        String outputFolder = args[2];
        int max_instances_per_seed = Integer.parseInt(args[3]);

        List<DocTheory> docTheoryList = new ArrayList<>();


        // Load document
        if (LearnItConfig.optionalParamTrue("bilingual")) {
            List<BilingualDocTheory> bilingualDocTheoryList = new ArrayList<>();
            Map<String, Map<String, String>> docIdToEntries = TabularPathListsConverter.parseSingleTabularList(doclist);
            Collection<Map<String, String>> biEntries = docIdToEntries.values();
            bilingualDocTheoryList.addAll(LoaderUtils.resolveBilingualDocTheoryFromBiEntries(biEntries));
            for(BilingualDocTheory bilingualDocTheory:bilingualDocTheoryList){
                docTheoryList.add(bilingualDocTheory.getSourceDoc());
            }
        } else {
            List<String> fileList = GeneralUtils.readLinesIntoList(doclist);
            docTheoryList.addAll(LoaderUtils.resolvedDocTheoryFromPathList(fileList));

        }
        // End load document

        Set<String> stagesToRun = new HashSet<>(LearnItConfig.getList("stages_to_run"));
        if(stagesToRun.contains("binary_event_event_decoding")){

            List<String> extractorPaths = LearnItConfig.getList("binary_event_event_extractors");
            String targetName = "binary_event_event";
            LearnItRelationPatternNALabeler learnItRelationPatternLabeler = new LearnItRelationPatternNALabeler(extractorPaths, max_instances_per_seed);
            Mappings originalMappings = genMappings(docTheoryList, targetName);
            Mappings labeledMappings = learnItRelationPatternLabeler.LabelMappings(originalMappings, new Annotation.InMemoryAnnotationStorage()).convertToMappings();
            OpenNREObserverSilent openNREObserver = new OpenNREObserverSilent(new File(outputFolder));
            openNREObserver.observe(labeledMappings);
            openNREObserver.build();
        }
        long endTime = System.nanoTime();
        System.out.println("Milliseconds: " + ((endTime - startTime) / 1000000));
    }
}
