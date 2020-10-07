package com.bbn.akbc.neolearnit.labelers;

import com.bbn.akbc.neolearnit.common.Annotation;
import com.bbn.akbc.neolearnit.common.Domain;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.serializers.binary_event.EERGraphObserver;
import com.bbn.serif.theories.Spanning;
import com.bbn.serif.theories.Token;
import com.bbn.serif.theories.TokenSequence;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class EERGraphLabeler{

    public static class LabelingResult{
        public Annotation.InMemoryAnnotationStorage unaryLabeledMappings;
        public LabelTrackingObserver unaryLabelTrackingObserver;
        public Annotation.InMemoryAnnotationStorage binaryLabeledMappings;
        public LabelTrackingObserver binaryLabelTrackingObserver;
    }




    public static LabelingResult generateGraph(Mappings mappings) throws Exception {
        TargetAndScoreTableLabeler unaryEventLabeler = TargetAndScoreTableLabeler.fromOntologyYamlAndLatestExtractors(Domain.getOntologyNameToPathMap().get("unaryEvent"), false, false);
        LabelTrackingObserver unaryLabelTrackingObserver = new LabelTrackingObserver(LabelTrackingObserver.LabelerTask.pattern);
        unaryEventLabeler.addLabelTrackingObserver(unaryLabelTrackingObserver);
        Annotation.InMemoryAnnotationStorage unaryLabeledMappings = unaryEventLabeler.LabelMappings(mappings, new Annotation.InMemoryAnnotationStorage());


        TargetAndScoreTableLabeler binaryEventLabeler = TargetAndScoreTableLabeler.fromOntologyYamlAndLatestExtractors(Domain.getOntologyNameToPathMap().get("binaryEvent"), false, false);
        LabelTrackingObserver binaryLabelTrackingObserver = new LabelTrackingObserver(LabelTrackingObserver.LabelerTask.pattern);
        binaryEventLabeler.addLabelTrackingObserver(binaryLabelTrackingObserver);
        Annotation.InMemoryAnnotationStorage binaryLabeledMappings = binaryEventLabeler.LabelMappings(mappings, new Annotation.InMemoryAnnotationStorage());

        LabelingResult labelingResult = new LabelingResult();
        labelingResult.unaryLabeledMappings = unaryLabeledMappings;
        labelingResult.unaryLabelTrackingObserver = unaryLabelTrackingObserver;
        labelingResult.binaryLabeledMappings = binaryLabeledMappings;
        labelingResult.binaryLabelTrackingObserver = binaryLabelTrackingObserver;
        return labelingResult;
    }

    public static void main(String[] args) throws Exception{
        String params = "/home/hqiu/ld100/learnit/params/learnit/runs/wm_dart_101519_bootstrap.params";
        LearnItConfig.loadParams(new File(params));
        String mappingsPath = "/nfs/raid88/u10/users/hqiu/learnit_data/wm_dart_101519_vexpt1/source_mappings/unary_event_and_binary_event-1/freq_1_2/mappings.master.sjson";
        String testEEr = "/home/hqiu/tmp/eer_graph.json";
        Mappings mappings = Mappings.deserialize(new File(mappingsPath),true);
        long startTime = System.nanoTime();
        LabelingResult labelingResult = generateGraph(mappings);
        EERGraphObserver eerGraphObserver = new EERGraphObserver();
        eerGraphObserver.serializeEERGraph(labelingResult,mappings,testEEr);
        System.out.println(System.nanoTime()-startTime);
    }


}
