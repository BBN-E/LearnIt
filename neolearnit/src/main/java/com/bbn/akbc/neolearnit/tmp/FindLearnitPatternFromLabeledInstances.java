package com.bbn.akbc.neolearnit.tmp;

import com.bbn.akbc.neolearnit.common.Annotation;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfoDisplay;
import com.bbn.akbc.neolearnit.common.targets.TargetFactory;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.util.GeneralUtils;
import com.bbn.serif.theories.EventMention;
import com.google.common.base.Optional;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FindLearnitPatternFromLabeledInstances {
    public static void main(String[] args) throws Exception{
        String paramsFile = args[0];
        LearnItConfig.loadParams(new File(paramsFile));
        String mappingsListStr = args[1];
        List<String> mappingsList = GeneralUtils.readLinesIntoList(mappingsListStr);
        String labeledMappingListStr = args[2];
        List<String> labeledMappingsList = GeneralUtils.readLinesIntoList(labeledMappingListStr);
        for(String mappingsPath:mappingsList){
            Mappings mappings = Mappings.deserialize(new File(mappingsPath),true);
            for(String labeledMappingsPath : labeledMappingsList){
                Mappings labeledMappings = Mappings.deserialize(new File(labeledMappingsPath),true);

                for(LearnitPattern learnitPattern : mappings.getAllPatterns().elementSet()) {
                    for(InstanceIdentifier instanceIdentifier : mappings.getInstancesForPattern(learnitPattern)) {
                        // types from the labeled mapping
                        Set<LearnitPattern> labeledPatternSet = new HashSet<>(labeledMappings.getPatternsForInstance(instanceIdentifier));

                        if(labeledPatternSet.isEmpty()) {
                            String type = "Other";
                            System.out.println("[Pattern]\t"+type+"\t"+learnitPattern.toPrettyString());
                        }
                        else {
                            for(LearnitPattern labeledPatternPatternOriginal: labeledPatternSet){
                                LabelPattern labelPattern = (LabelPattern)labeledPatternPatternOriginal;
                                String type = labelPattern.getLabel();
                                System.out.println("[Pattern]\t"+type+"\t"+learnitPattern.toPrettyString());
                            }
                        }
                    }
                }
            }
        }

    }

    /*
        public static void main(String[] args) throws Exception{
        String paramsFile = args[0];
        LearnItConfig.loadParams(new File(paramsFile));
        String mappingsListStr = args[1];
        List<String> mappingsList = GeneralUtils.readLinesIntoList(mappingsListStr);
        String labeledMappingListStr = args[2];
        List<String> labeledMappingsList = GeneralUtils.readLinesIntoList(labeledMappingListStr);
        for(String mappingsPath:mappingsList){
            Mappings mappings = Mappings.deserialize(new File(mappingsPath),true);
            for(String labeledMappingsPath : labeledMappingsList){
                Mappings labeledMappings = Mappings.deserialize(new File(labeledMappingsPath),true);
                for(LearnitPattern labeledPatternPatternOriginal: labeledMappings.getAllPatterns().elementSet()){
                    LabelPattern labelPattern = (LabelPattern)labeledPatternPatternOriginal;
                    for(InstanceIdentifier nonGenericEventInstanceIdentifier: labeledMappings.getInstancesForPattern(labelPattern)){
                        MatchInfo matchInfo = nonGenericEventInstanceIdentifier.reconstructMatchInfo(TargetFactory.makeUnaryEventTarget());
                        MatchInfoDisplay matchInfoDisplay = MatchInfoDisplay.fromMatchInfo(matchInfo, Optional.absent());
                        if(matchInfoDisplay.getPrimaryLanguageMatch() != null){
                            System.out.println("#########");
                            EventMention em = (EventMention)matchInfo.getPrimaryLanguageMatch().getSlot0().get();
                            System.out.println("[EventType "+em.type().asString()+"]" + matchInfoDisplay.getPrimaryLanguageMatch().getSentenceWithSlotBoundaryMarkups());

                            for(LearnitPattern learnitPattern1:mappings.getPatternsForInstance(nonGenericEventInstanceIdentifier)){
                                System.out.println("[Pattern]\t"+labelPattern.getLabel()+"\t"+learnitPattern1.toPrettyString());
                            }
                            System.out.println("!!!!!!!!");
                        }

                    }
                }
            }
        }

    }
    */
}
