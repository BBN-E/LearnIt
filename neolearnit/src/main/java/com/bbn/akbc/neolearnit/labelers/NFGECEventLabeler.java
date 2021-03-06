package com.bbn.akbc.neolearnit.labelers;

import com.bbn.akbc.neolearnit.common.Annotation;
import com.bbn.akbc.neolearnit.common.BBNInternalOntology;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.common.targets.TargetFactory;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;
import com.bbn.serif.theories.EventMention;
import com.bbn.serif.theories.SynNode;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class NFGECEventLabeler implements MappingsLabeler {
    final BBNInternalOntology.BBNInternalOntologyNode ontologyRoot;
    final Multimap<String, String> ontologyNodeNameToOntologySlashJointHierachyMap;
    final Multimap<String, String> dataExampleMap;

    public NFGECEventLabeler(String yamlOntologyPath, String dataExamplePath) throws Exception {
        this.ontologyRoot = BBNInternalOntology.BBNInternalOntologyNode.fromInternalOntologyFile(new File(yamlOntologyPath));
        ontologyNodeNameToOntologySlashJointHierachyMap = HashMultimap.create();
        BBNInternalOntology.DFSNodeNameToSlashJointNodePathMapParsing(ontologyRoot, new Stack<>(), ontologyNodeNameToOntologySlashJointHierachyMap);
        dataExampleMap = BBNInternalOntology.buildTriggerExampleToOntologyIdMapping(dataExamplePath);
    }

    public static String EventMentionUnifier(EventMention eventMention) {
        SynNode synNode = eventMention.anchorNode().head();
        return synNode.span().tokenizedText().utf16CodeUnits().toLowerCase();
    }

    public static void main(String[] args) throws Exception {
        String paramPath = args[0];
        LearnItConfig.loadParams(new File(paramPath));
        String autogeneratedMappingsPath = args[1];
        String yamlOntologyPath = args[2];
        String dataExamplePath = args[3];
        String outputLabelledMappingsPath = args[4];
        NFGECEventLabeler nfgecEventLabeler = new NFGECEventLabeler(yamlOntologyPath, dataExamplePath);
        Mappings autogeneratedMappings = Mappings.deserialize(new File(autogeneratedMappingsPath), true);
        Mappings labeledMappings = nfgecEventLabeler.LabelMappings(autogeneratedMappings, new Annotation.InMemoryAnnotationStorage()).convertToMappings();
        labeledMappings.serialize(new File(outputLabelledMappingsPath), true);
    }

    @Override
    public Annotation.InMemoryAnnotationStorage LabelMappings(Mappings original, Annotation.InMemoryAnnotationStorage labeledMappings) throws Exception {

        Set<InstanceIdentifier> instanceIdentifierSet = new HashSet<>();
        for (InstanceIdentifier instanceIdentifier : original.getPatternInstances()) {
            if (instanceIdentifier.getSlot0SpanningType() == InstanceIdentifier.SpanningType.EventMention && instanceIdentifier.getSlot1SpanningType() == InstanceIdentifier.SpanningType.Empty) {
                instanceIdentifierSet.add(instanceIdentifier);
            }
        }
        for (InstanceIdentifier instanceIdentifier : original.getSeedInstances()) {
            if (instanceIdentifier.getSlot0SpanningType() == InstanceIdentifier.SpanningType.EventMention && instanceIdentifier.getSlot1SpanningType() == InstanceIdentifier.SpanningType.Empty) {
                instanceIdentifierSet.add(instanceIdentifier);
            }
        }
        // @hqiu: multithread here maybe?
        for (InstanceIdentifier instanceIdentifier : instanceIdentifierSet) {
            MatchInfo.LanguageMatchInfo languageMatchInfo = instanceIdentifier.reconstructMatchInfo(TargetFactory.makeUnaryEventTarget()).getPrimaryLanguageMatch();
            EventMention eventMention = (EventMention) languageMatchInfo.getSlot0().get();
            String lemmaizedTrigger = EventMentionUnifier(eventMention);
            Collection<String> candidateEventTypeNames = dataExampleMap.get(lemmaizedTrigger);
            if (candidateEventTypeNames == null) continue;
            for (String candidateEventTypeName : candidateEventTypeNames) {
                for (String SlashJointOntologyHierachyPath : ontologyNodeNameToOntologySlashJointHierachyMap.get(candidateEventTypeName)) {
                    // Duplication allowed?
                    LabelPattern newLabelPattern = new LabelPattern(SlashJointOntologyHierachyPath, Annotation.FrozenState.FROZEN_GOOD);
                    if (labeledMappings.isParticularAnnotationExists(instanceIdentifier, newLabelPattern))
                        continue;
                    labeledMappings.addAnnotation(instanceIdentifier, newLabelPattern);
                }
            }
        }
        return labeledMappings;
    }
}
