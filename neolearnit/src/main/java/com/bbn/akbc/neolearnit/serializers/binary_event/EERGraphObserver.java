package com.bbn.akbc.neolearnit.serializers.binary_event;

import com.bbn.akbc.neolearnit.common.Annotation;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.Triple;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.labelers.EERGraphLabeler;
import com.bbn.akbc.neolearnit.labelers.LabelTrackingObserver;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.serializers.observations.EERGraph;
import com.bbn.akbc.neolearnit.storage.StorageUtils;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.Spanning;
import com.bbn.serif.theories.Token;
import com.bbn.serif.theories.TokenSequence;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class EERGraphObserver {

    Map<String, EERGraph.Node> nodeIdToNode;
    Map<String, EERGraph.Edge> edgeIdToEdge;
    int maxNumberOfExamples = 10;

    public EERGraphObserver() {
        this.nodeIdToNode = new HashMap<>();
        this.edgeIdToEdge = new HashMap<>();
    }

    public static String singleSlotMarkUp(TokenSequence tokens, Spanning span) {
        List<String> tokenMarked = new ArrayList<>();
        for (Token token : tokens) {
            StringBuilder sb = new StringBuilder();
            if (token == span.span().startToken()) {
                sb.append("<span class=\"slot0\">");
            }
            sb.append(token.tokenizedText().utf16CodeUnits());
            if (token == span.span().endToken()) {
                sb.append("</span>");
            }
            tokenMarked.add(sb.toString());
        }
        return String.join(" ", tokenMarked);
    }

    public static String dualSlotMarkUp(TokenSequence tokens, Spanning span0, Spanning span1) {
        List<String> tokenMarked = new ArrayList<>();
        for (Token token : tokens) {
            StringBuilder sb = new StringBuilder();
            if (token == span0.span().startToken()) {
                sb.append("<span class=\"slot0\">");
            }
            if (token == span1.span().startToken()) {
                sb.append("<span class=\"slot1\">");
            }
            sb.append(token.tokenizedText().utf16CodeUnits());
            if (token == span0.span().endToken()) {
                sb.append("</span>");
            }
            if (token == span1.span().endToken()) {
                sb.append("</span>");
            }
            tokenMarked.add(sb.toString());
        }
        return String.join(" ", tokenMarked);
    }

    public EERGraph.Node getOrCreateNode(String nodeId) {
        EERGraph.Node leftTypeNode = this.nodeIdToNode.getOrDefault(nodeId, new EERGraph.Node(nodeId));
        this.nodeIdToNode.putIfAbsent(nodeId, leftTypeNode);
        return leftTypeNode;
    }

    public EERGraph.Edge getOrCreateEdge(EERGraph.Node leftNode, EERGraph.Node rightNode, String edgeName, String edgeType) {
        String edgeKey = String.format("%s_%s_%s_%s", leftNode.nodeId, rightNode.nodeId, edgeName, edgeType);
        EERGraph.Edge edge = this.edgeIdToEdge.getOrDefault(edgeKey, new EERGraph.Edge(leftNode, rightNode, edgeName, edgeType));
        this.edgeIdToEdge.putIfAbsent(edgeKey, edge);
        return edge;
    }


    public void serializeEERGraph(EERGraphLabeler.LabelingResult labelingResult, Mappings mappings, String outputPath) throws InterruptedException, ExecutionException, IOException {
        Annotation.InMemoryAnnotationStorage binaryLabeledMappings = labelingResult.binaryLabeledMappings;
        Annotation.InMemoryAnnotationStorage unaryLabeledMappings = labelingResult.unaryLabeledMappings;
        LabelTrackingObserver unaryLabelTrackingObserver = labelingResult.unaryLabelTrackingObserver;
        LabelTrackingObserver binaryLabelTrackingObserver = labelingResult.binaryLabelTrackingObserver;

        Set<InstanceIdentifier> careInstances = new HashSet<>();

        Set<LearnitPattern> binaryPatternSet = new HashSet<>();
        Map<Symbol, Set<LearnitPattern>> symbolsToLearnitPattern = new HashMap<>();
        for (LearnitPattern learnitPattern : mappings.getAllPatterns().elementSet()) {
            Set<LearnitPattern> buf = symbolsToLearnitPattern.getOrDefault(Symbol.from(learnitPattern.toPrettyString()), new HashSet<>());
            buf.add(learnitPattern);
            symbolsToLearnitPattern.putIfAbsent(Symbol.from(learnitPattern.toPrettyString()), buf);
            for (InstanceIdentifier instanceIdentifier : mappings.getInstancesForPattern(learnitPattern)) {
                if (!instanceIdentifier.isUnaryInstanceIdentifier()) {
                    binaryPatternSet.add(learnitPattern);
                    careInstances.addAll(mappings.getInstancesForPattern(learnitPattern));
                }
                break;
            }
        }

        System.out.println("Caching " + careInstances.size() + " MatchInfo");
        InstanceIdentifier.preLoadDocThoery(careInstances);
        System.out.println("Caching completed.");

        Map<String, Set<String>> nodeIDMappings = new HashMap<>();
        // Nodes
        for (InstanceIdentifier instanceIdentifier : unaryLabeledMappings.getAllInstanceIdentifier()) {
            for (LabelPattern unaryLabelPattern : unaryLabeledMappings.lookupInstanceIdentifierAnnotation(instanceIdentifier)) {
                String typeNodeId = unaryLabelPattern.getLabel();
                for (Symbol patternPrettyString : unaryLabelTrackingObserver.getMarkingFromInstanceIdentifierLabelMappings(instanceIdentifier, unaryLabelPattern)) {
                    for (LearnitPattern learnitPattern : symbolsToLearnitPattern.getOrDefault(patternPrettyString, new HashSet<>())) {
                        String potentialTriggerId = learnitPattern.toPrettyString();
                        Set<String> buf = nodeIDMappings.getOrDefault(potentialTriggerId, new HashSet<>());
                        buf.add(typeNodeId);
                        nodeIDMappings.putIfAbsent(potentialTriggerId, buf);
                    }
                }
            }
        }
        for (InstanceIdentifier binaryInstanceIdentifier : binaryLabeledMappings.getAllInstanceIdentifier()) {
            for (LabelPattern binaryLabelPattern : binaryLabeledMappings.lookupInstanceIdentifierAnnotation(binaryInstanceIdentifier)) {
                String typeEdgeId = binaryLabelPattern.getLabel();
                for (Symbol patternPrettyString : binaryLabelTrackingObserver.getMarkingFromInstanceIdentifierLabelMappings(binaryInstanceIdentifier, binaryLabelPattern)) {
                    for (LearnitPattern learnitPattern : symbolsToLearnitPattern.getOrDefault(patternPrettyString, new HashSet<>())) {
                        String potentialTriggerId = learnitPattern.toPrettyString();
                        Set<String> buf = nodeIDMappings.getOrDefault(potentialTriggerId, new HashSet<>());
                        buf.add(typeEdgeId);
                        nodeIDMappings.putIfAbsent(potentialTriggerId, buf);
                    }
                }
            }
        }
        Map<LearnitPattern, Set<LearnitPattern>> binaryToUnaryLeft = new HashMap<>();
        Map<LearnitPattern, Set<LearnitPattern>> binaryToUnaryRight = new HashMap<>();
        Set<LearnitPattern> connectedUnaryPatterns = new HashSet<>();
        System.out.println("Start building pattern mapping");
        Map<Triple<LearnitPattern, LearnitPattern, LearnitPattern>, Set<InstanceIdentifier>> triplesToInstanceIdSet = new HashMap<>();
        for (LearnitPattern binaryLearnitPattern : binaryPatternSet) {
            for (InstanceIdentifier binaryInstanceIdentifier : mappings.getInstancesForPattern(binaryLearnitPattern)) {
                InstanceIdentifier leftUnaryInstanceIdentifier = binaryInstanceIdentifier.getLowerRankInstanceIdentifierLeft();
                InstanceIdentifier rightUnaryInstanceIdentifier = binaryInstanceIdentifier.getLowerRankInstanceIdentifierRight();
                Set<LearnitPattern> leftSet = new HashSet<>(mappings.getPatternsForInstance(leftUnaryInstanceIdentifier));
                Set<LearnitPattern> rightSet = new HashSet<>(mappings.getPatternsForInstance(rightUnaryInstanceIdentifier));
                binaryToUnaryLeft.put(binaryLearnitPattern, leftSet);
                binaryToUnaryRight.put(binaryLearnitPattern, rightSet);
                connectedUnaryPatterns.addAll(leftSet);
                connectedUnaryPatterns.addAll(rightSet);
                for (LearnitPattern unaryLeft : leftSet) {
                    for (LearnitPattern unaryRight : rightSet) {
                        Triple<LearnitPattern, LearnitPattern, LearnitPattern> triple = new Triple<>(unaryLeft, binaryLearnitPattern, unaryRight);
                        Set<InstanceIdentifier> buf = triplesToInstanceIdSet.getOrDefault(triple, new HashSet<>());
                        buf.add(binaryInstanceIdentifier);
                        triplesToInstanceIdSet.putIfAbsent(triple, buf);
                    }
                }

            }
        }
        System.out.println("End building pattern mapping");
        Map<LearnitPattern, Set<String>> learnitPatternToMappedNodeId = new HashMap<>();
        System.out.println("Serializing nodes");
        for (LearnitPattern unaryPattern : connectedUnaryPatterns) {
            Set<String> potentialMappedNodes = nodeIDMappings.getOrDefault(unaryPattern.toPrettyString(), new HashSet<>());
            List<String> sampledExamples = new LinkedList<>();
            for (InstanceIdentifier instanceIdentifier : mappings.getInstancesForPattern(unaryPattern)) {
                MatchInfo.LanguageMatchInfo languageMatchInfo = instanceIdentifier.reconstructMatchInfo(null).getPrimaryLanguageMatch();
                sampledExamples.add(singleSlotMarkUp(languageMatchInfo.getSentTheory().tokenSequence(), languageMatchInfo.firstSpanning()));
                if (sampledExamples.size() >= this.maxNumberOfExamples) {
                    break;
                }
            }
            Set<String> buf = learnitPatternToMappedNodeId.getOrDefault(unaryPattern, new HashSet<>());
            int capturedInstances = mappings.getInstancesForPattern(unaryPattern).size();
            if (potentialMappedNodes.size() > 0) {
                for (String mappedType : potentialMappedNodes) {
                    String nodeId = "event_grounding_" + mappedType;
                    EERGraph.Node leftTypeNode = this.getOrCreateNode(nodeId);
                    leftTypeNode.nodeName = mappedType;
                    leftTypeNode.nodeType = "event_grounding";
                    leftTypeNode.increaseCnt(capturedInstances);
                    for (String example : sampledExamples) {
                        leftTypeNode.putExample(example, this.maxNumberOfExamples);
                    }
                    buf.add(nodeId);
                }
            } else {
                String nodeId = "trigger_" + unaryPattern.toPrettyString();
                EERGraph.Node leftTypeNode = this.getOrCreateNode(nodeId);
                leftTypeNode.nodeName = unaryPattern.toPrettyString();
                leftTypeNode.nodeType = "trigger";
                leftTypeNode.increaseCnt(capturedInstances);
                for (String example : sampledExamples) {
                    leftTypeNode.putExample(example, this.maxNumberOfExamples);
                }
                buf.add(nodeId);
            }
            learnitPatternToMappedNodeId.putIfAbsent(unaryPattern, buf);
        }
        System.out.println("Serializing edges");
        for (LearnitPattern binaryPattern : binaryPatternSet) {
            Set<String> potentialMappedNodes = nodeIDMappings.getOrDefault(binaryPattern.toPrettyString(), new HashSet<>());
            int capturedInstances = mappings.getInstancesForPattern(binaryPattern).size();
            for (LearnitPattern unaryLeftPattern : binaryToUnaryLeft.get(binaryPattern)) {
                Set<String> leftNodeIds = learnitPatternToMappedNodeId.get(unaryLeftPattern);
                for (LearnitPattern unaryRightPattern : binaryToUnaryRight.get(binaryPattern)) {
                    Triple<LearnitPattern, LearnitPattern, LearnitPattern> triple = new Triple<>(unaryLeftPattern, binaryPattern, unaryRightPattern);
                    List<String> sampledExamples = new LinkedList<>();
                    for (InstanceIdentifier instanceIdentifier : triplesToInstanceIdSet.getOrDefault(triple, new HashSet<>())) {
                        MatchInfo.LanguageMatchInfo languageMatchInfo = instanceIdentifier.reconstructMatchInfo(null).getPrimaryLanguageMatch();
                        sampledExamples.add(dualSlotMarkUp(languageMatchInfo.getSentTheory().tokenSequence(), languageMatchInfo.firstSpanning(), languageMatchInfo.secondSpanning()));
                        if (sampledExamples.size() >= this.maxNumberOfExamples) {
                            break;
                        }
                    }
                    Set<String> rightNodeIds = learnitPatternToMappedNodeId.get(unaryRightPattern);
                    for (String leftNodeId : leftNodeIds) {
                        for (String rightNodeId : rightNodeIds) {
                            if (potentialMappedNodes.size() > 0) {
                                for (String mappedType : potentialMappedNodes) {
                                    EERGraph.Edge typeTypeEdge = this.getOrCreateEdge(this.nodeIdToNode.get(leftNodeId), this.nodeIdToNode.get(rightNodeId), mappedType, "event_grounding_event_grounding_relation");
                                    typeTypeEdge.increaseCnt(triplesToInstanceIdSet.getOrDefault(triple, new HashSet<>()).size());
                                    for (String example : sampledExamples) {
                                        typeTypeEdge.putExample(example, this.maxNumberOfExamples);
                                    }
                                }
                            } else {
                                EERGraph.Edge triggerTriggerEdge = this.getOrCreateEdge(this.nodeIdToNode.get(leftNodeId), this.nodeIdToNode.get(rightNodeId), binaryPattern.toPrettyString(), "trigger_trigger_relation");
                                triggerTriggerEdge.increaseCnt(triplesToInstanceIdSet.getOrDefault(triple, new HashSet<>()).size());
                                for (String example : sampledExamples) {
                                    triggerTriggerEdge.putExample(example, this.maxNumberOfExamples);
                                }
                            }
                        }
                    }
                }
            }
        }
        System.out.println("Serialized " + this.edgeIdToEdge.keySet().size() + " edges.");
        Map<String, List> jsonGraph = new HashMap<>();
        jsonGraph.put("nodes", this.nodeIdToNode.values().stream().map(EERGraph.Node::toDict).collect(Collectors.toList()));
        jsonGraph.put("edges", this.edgeIdToEdge.values().stream().map(EERGraph.Edge::toDict).collect(Collectors.toList()));
        ObjectMapper objectMapper = StorageUtils.getMapperWithoutTyping();
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(outputPath), jsonGraph);
    }

}
