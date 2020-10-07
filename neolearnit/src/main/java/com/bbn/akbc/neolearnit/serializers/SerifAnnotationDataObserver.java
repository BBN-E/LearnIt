package com.bbn.akbc.neolearnit.serializers;

import com.bbn.akbc.common.Pair;
import com.bbn.akbc.neolearnit.SerifAnnotationDataTemplate.SerifAnnotationData;
import com.bbn.akbc.neolearnit.common.Annotation;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;
import com.bbn.bue.common.exceptions.NotImplementedException;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.*;

import java.io.File;
import java.util.*;

public class SerifAnnotationDataObserver extends ExternalAnnotationBuilder {

    final static String source = "LearnIt";

    final File outputDir;

    public SerifAnnotationDataObserver(String outputDir) {
        this.outputDir = new File(outputDir);
        this.outputDir.mkdirs();
    }

    public static void main(String[] args) throws Exception {
        String paramPath = args[0];
        LearnItConfig.loadParams(new File(paramPath));
        String labelledMappingsPath = args[1];
        String outputPrefix = args[2];

        Mappings labeledMappings = Mappings.deserialize(new File(labelledMappingsPath), true);
        SerifAnnotationDataObserver serifAnnotationDataObserver = new SerifAnnotationDataObserver(outputPrefix);
        serifAnnotationDataObserver.observe(labeledMappings);
        serifAnnotationDataObserver.build();
    }

    public static SerifAnnotationData.Span eventMentionSpanHandler(EventMention eventMention, SentenceTheory sentenceTheory) {
        return new SerifAnnotationData.Span(
                eventMention.anchorNode().span().startCharOffset().asInt(),
                eventMention.anchorNode().span().endCharOffset().asInt(),
                sentenceTheory.span().startCharOffset().asInt(),
                sentenceTheory.span().endCharOffset().asInt()
        );
    }

    public static SerifAnnotationData.Span mentionSpanHandler(Mention mention, SentenceTheory sentenceTheory) {
        return new SerifAnnotationData.Span(
                mention.span().startCharOffset().asInt(),
                mention.span().endCharOffset().asInt(),
                sentenceTheory.span().startCharOffset().asInt(),
                sentenceTheory.span().endCharOffset().asInt()
        );
    }

    public static SerifAnnotationData.Span valueMentionSpanHandler(ValueMention valueMention, SentenceTheory sentenceTheory) {
        return new SerifAnnotationData.Span(
                valueMention.span().startCharOffset().asInt(),
                valueMention.span().endCharOffset().asInt(),
                sentenceTheory.span().startCharOffset().asInt(),
                sentenceTheory.span().endCharOffset().asInt()
        );
    }

    public static SerifAnnotationData.Span getSpanning(InstanceIdentifier instanceIdentifier, int slot, SentenceTheory sentenceTheory) {
        Spanning serifSpan;
        InstanceIdentifier.SpanningType spanningType;
        if (slot == 0) {
            serifSpan = InstanceIdentifier.getSpanning(sentenceTheory, instanceIdentifier.getSlot0Start(), instanceIdentifier.getSlot0End(), instanceIdentifier.getSlot0SpanningType()).get();
            spanningType = instanceIdentifier.getSlot0SpanningType();
        } else if (slot == 1) {
            serifSpan = InstanceIdentifier.getSpanning(sentenceTheory, instanceIdentifier.getSlot1Start(), instanceIdentifier.getSlot1End(), instanceIdentifier.getSlot1SpanningType()).get();
            spanningType = instanceIdentifier.getSlot1SpanningType();
        } else {
            throw new NotImplementedException();
        }
        if (spanningType.equals(InstanceIdentifier.SpanningType.EventMention)) {
            EventMention eventMention = (EventMention) serifSpan;
            return eventMentionSpanHandler(eventMention, sentenceTheory);
        } else if (spanningType.equals(InstanceIdentifier.SpanningType.Mention)) {
            Mention mention = (Mention) serifSpan;
            return mentionSpanHandler(mention, sentenceTheory);
        } else if (spanningType.equals(InstanceIdentifier.SpanningType.ValueMention)) {
            ValueMention valueMention = (ValueMention) serifSpan;
            return valueMentionSpanHandler(valueMention, sentenceTheory);
        } else {
            throw new NotImplementedException();
        }
    }

    public static void edgeMentionAnnotationHandler(Map<String, Map<Pair<SerifAnnotationData.Span, SerifAnnotationData.Span>, SerifAnnotationData.EdgeMentionAnnotation>> bookingMap, InstanceIdentifier instanceIdentifier, SentenceTheory sentenceTheory, LabelPattern label) {
        SerifAnnotationData.Span leftSpan = getSpanning(instanceIdentifier, 0, sentenceTheory);
        SerifAnnotationData.Span rightSpan = getSpanning(instanceIdentifier, 1, sentenceTheory);
        String docId = instanceIdentifier.getDocid();
        Map<Pair<SerifAnnotationData.Span, SerifAnnotationData.Span>, SerifAnnotationData.EdgeMentionAnnotation> currentDocBuf = bookingMap.getOrDefault(docId, new HashMap<>());
        SerifAnnotationData.EdgeMentionAnnotation serifEdgeMentionAnnotation = currentDocBuf.getOrDefault(new Pair<>(leftSpan, rightSpan), new SerifAnnotationData.EdgeMentionAnnotation(leftSpan, rightSpan));
        SerifAnnotationData.AnnotationEntry potentialLearnItEn = null;
        for (SerifAnnotationData.AnnotationEntry annotationEntry : serifEdgeMentionAnnotation.annotationEntries) {
            if (annotationEntry.source.equals(source)) {
                potentialLearnItEn = annotationEntry;
                break;
            }
        }
        if (potentialLearnItEn == null) {
            potentialLearnItEn = new SerifAnnotationData.AnnotationEntry(source);
            serifEdgeMentionAnnotation.annotationEntries.add(potentialLearnItEn);
        }
        potentialLearnItEn.markings.add(new SerifAnnotationData.Marking(label.getLabel(), label.getFrozenState()));
        currentDocBuf.put(new Pair<>(leftSpan, rightSpan), serifEdgeMentionAnnotation);
        bookingMap.put(docId, currentDocBuf);
    }

    public static void nodeMentionAnnotationHandler(Map<String, Map<SerifAnnotationData.Span, SerifAnnotationData.NodeMentionAnnotation>> bookingMap, InstanceIdentifier instanceIdentifier, SentenceTheory sentenceTheory, int slot, LabelPattern label) {
        SerifAnnotationData.Span span = getSpanning(instanceIdentifier, slot, sentenceTheory);
        String docId = instanceIdentifier.getDocid();
        Map<SerifAnnotationData.Span, SerifAnnotationData.NodeMentionAnnotation> currentDocBuf = bookingMap.getOrDefault(docId, new HashMap<>());
        SerifAnnotationData.NodeMentionAnnotation serifNodeMentionAnnotation = currentDocBuf.getOrDefault(span, new SerifAnnotationData.NodeMentionAnnotation(span));
        SerifAnnotationData.AnnotationEntry potentialLearnItEn = null;
        for (SerifAnnotationData.AnnotationEntry annotationEntry : serifNodeMentionAnnotation.annotationEntries) {
            if (annotationEntry.source.equals(source)) {
                potentialLearnItEn = annotationEntry;
                break;
            }
        }
        if (potentialLearnItEn == null) {
            potentialLearnItEn = new SerifAnnotationData.AnnotationEntry(source);
            serifNodeMentionAnnotation.annotationEntries.add(potentialLearnItEn);
        }
        potentialLearnItEn.markings.add(new SerifAnnotationData.Marking(label.getLabel(), label.getFrozenState()));
        currentDocBuf.put(span, serifNodeMentionAnnotation);
        bookingMap.put(docId, currentDocBuf);
    }

    @Override
    public void build() throws Exception {
        Map<InstanceIdentifier, SentenceTheory> instanceIdentifierSentenceTheoryMap = this.resolveSentenceTheory();
        Map<String, Map<Pair<SerifAnnotationData.Span, SerifAnnotationData.Span>, SerifAnnotationData.EdgeMentionAnnotation>> docIdToEventEventRelationMentions = new HashMap<>();
        Map<String, Map<Pair<SerifAnnotationData.Span, SerifAnnotationData.Span>, SerifAnnotationData.EdgeMentionAnnotation>> docIdToEventEventArgumentRelationMentions = new HashMap<>();
        Map<String, Map<SerifAnnotationData.Span, SerifAnnotationData.NodeMentionAnnotation>> docIdToEventMentions = new HashMap<>();
        Set<String> allDocId = new HashSet<>();
        for (InstanceIdentifier instanceIdentifier : this.inMemoryAnnotationStorage.getAllInstanceIdentifier()) {
            final SentenceTheory sentenceTheory = instanceIdentifierSentenceTheoryMap.get(instanceIdentifier);

            boolean NALabelAppears = false;
            boolean onlyNALabelAppears = true;

            for (LabelPattern labelPattern : this.inMemoryAnnotationStorage.lookupInstanceIdentifierAnnotation(instanceIdentifier)) {
                if (labelPattern.getFrozenState().equals(Annotation.FrozenState.FROZEN_BAD)) NALabelAppears = true;
                if (labelPattern.getFrozenState().equals(Annotation.FrozenState.FROZEN_GOOD))
                    onlyNALabelAppears = false;
            }

            allDocId.add(instanceIdentifier.getDocid());
            if (NALabelAppears && onlyNALabelAppears) {
                LabelPattern labelPattern = new LabelPattern(Symbol.from("NA"), Annotation.FrozenState.FROZEN_BAD);
                if (instanceIdentifier.getSlot1SpanningType().equals(InstanceIdentifier.SpanningType.Empty)) {
                    // Event trigger Task
                    nodeMentionAnnotationHandler(docIdToEventMentions, instanceIdentifier, sentenceTheory, 0, labelPattern);
                } else {
                    //Event-Menion/ValueMention or Event-Event task
                    if (instanceIdentifier.getSlot0SpanningType().equals(InstanceIdentifier.SpanningType.EventMention) && instanceIdentifier.getSlot1SpanningType().equals(InstanceIdentifier.SpanningType.EventMention)) {
                        //Event Event case
                        edgeMentionAnnotationHandler(docIdToEventEventRelationMentions, instanceIdentifier, sentenceTheory, labelPattern);
                    } else if (instanceIdentifier.getSlot0SpanningType().equals(InstanceIdentifier.SpanningType.EventMention) && (instanceIdentifier.getSlot1SpanningType().equals(InstanceIdentifier.SpanningType.Mention) || instanceIdentifier.getSlot1SpanningType().equals(InstanceIdentifier.SpanningType.ValueMention))) {
                        // Event Event Argument Case
                        edgeMentionAnnotationHandler(docIdToEventEventArgumentRelationMentions, instanceIdentifier, sentenceTheory, labelPattern);
                    } else {
                        throw new NotImplementedException();
                    }
                }
            } else {
                for (LabelPattern labelPattern : this.inMemoryAnnotationStorage.lookupInstanceIdentifierAnnotation(instanceIdentifier)) {
                    if (labelPattern.getFrozenState().equals(Annotation.FrozenState.FROZEN_BAD)) continue;
                    if (instanceIdentifier.getSlot1SpanningType().equals(InstanceIdentifier.SpanningType.Empty)) {
                        // Event trigger Task
                        nodeMentionAnnotationHandler(docIdToEventMentions, instanceIdentifier, sentenceTheory, 0, labelPattern);
                    } else {
                        //Event-Menion/ValueMention or Event-Event task
                        if (instanceIdentifier.getSlot0SpanningType().equals(InstanceIdentifier.SpanningType.EventMention) && instanceIdentifier.getSlot1SpanningType().equals(InstanceIdentifier.SpanningType.EventMention)) {
                            //Event Event case
                            edgeMentionAnnotationHandler(docIdToEventEventRelationMentions, instanceIdentifier, sentenceTheory, labelPattern);
                        } else if (instanceIdentifier.getSlot0SpanningType().equals(InstanceIdentifier.SpanningType.EventMention) && (instanceIdentifier.getSlot1SpanningType().equals(InstanceIdentifier.SpanningType.Mention) || instanceIdentifier.getSlot1SpanningType().equals(InstanceIdentifier.SpanningType.ValueMention))) {
                            // Event Event Argument Case
                            edgeMentionAnnotationHandler(docIdToEventEventArgumentRelationMentions, instanceIdentifier, sentenceTheory, labelPattern);
                        } else {
                            throw new NotImplementedException();
                        }
                    }
                }
            }
        }
        for (String docId : allDocId) {
            File path = new File(this.outputDir + File.separator + docId + ".json");
            if (path.isFile()) {
                SerifAnnotationData serifAnnotationData = SerifAnnotationData.readSerifAnnotationData(path);
                Map<SerifAnnotationData.Span, SerifAnnotationData.NodeMentionAnnotation> eventMentionAnnotations = docIdToEventMentions.getOrDefault(docId, new HashMap<>());
                for (SerifAnnotationData.Span span : eventMentionAnnotations.keySet()) {
                    List<SerifAnnotationData.AnnotationEntry> newAnnotationEntries = eventMentionAnnotations.get(span).annotationEntries;
                    SerifAnnotationData.AnnotationEntry newAnnotationEntry = SerifAnnotationData.getAnnotationEntry(newAnnotationEntries, source);
                    SerifAnnotationData.NodeMentionAnnotation eventAnnotation = serifAnnotationData.getOrCreateEventMentionAnnotation(span);
                    SerifAnnotationData.AnnotationEntry annotationEntry = SerifAnnotationData.getOrCreateAnnotationEntry(eventAnnotation.annotationEntries, source);
                    annotationEntry.markings.clear();
                    if (newAnnotationEntry != null) {
                        annotationEntry.markings.addAll(newAnnotationEntry.markings);
                    }
                }
                Map<Pair<SerifAnnotationData.Span, SerifAnnotationData.Span>, SerifAnnotationData.EdgeMentionAnnotation> eventeventRelationAnnotations = docIdToEventEventRelationMentions.getOrDefault(docId, new HashMap<>());
                for (Pair<SerifAnnotationData.Span, SerifAnnotationData.Span> spanPair : eventeventRelationAnnotations.keySet()) {
                    List<SerifAnnotationData.AnnotationEntry> newAnnotationEntries = eventeventRelationAnnotations.get(spanPair).annotationEntries;
                    SerifAnnotationData.AnnotationEntry newAnnotationEntry = SerifAnnotationData.getAnnotationEntry(newAnnotationEntries, source);
                    SerifAnnotationData.EdgeMentionAnnotation eventeventRelationAnnotation = serifAnnotationData.getOrCreateEventEventRelationMentionAnnotation(spanPair.getFirst(), spanPair.getSecond());
                    SerifAnnotationData.AnnotationEntry annotationEntry = SerifAnnotationData.getOrCreateAnnotationEntry(eventeventRelationAnnotation.annotationEntries, source);
                    annotationEntry.markings.clear();
                    if (newAnnotationEntry != null) {
                        annotationEntry.markings.addAll(newAnnotationEntry.markings);
                    }
                }
                Map<Pair<SerifAnnotationData.Span, SerifAnnotationData.Span>, SerifAnnotationData.EdgeMentionAnnotation> eventeventArgumentRelationAnnotations = docIdToEventEventArgumentRelationMentions.getOrDefault(docId, new HashMap<>());
                for (Pair<SerifAnnotationData.Span, SerifAnnotationData.Span> spanPair : eventeventArgumentRelationAnnotations.keySet()) {
                    List<SerifAnnotationData.AnnotationEntry> newAnnotationEntries = eventeventArgumentRelationAnnotations.get(spanPair).annotationEntries;
                    SerifAnnotationData.AnnotationEntry newAnnotationEntry = SerifAnnotationData.getAnnotationEntry(newAnnotationEntries, source);
                    SerifAnnotationData.EdgeMentionAnnotation eventEventArgumentAnnotation = serifAnnotationData.getOrCreateEventEventArgumentRelationMentionAnnotation(spanPair.getFirst(), spanPair.getSecond());
                    SerifAnnotationData.AnnotationEntry annotationEntry = SerifAnnotationData.getOrCreateAnnotationEntry(eventEventArgumentAnnotation.annotationEntries, source);
                    annotationEntry.markings.clear();
                    if (newAnnotationEntry != null) {
                        annotationEntry.markings.addAll(newAnnotationEntry.markings);
                    }
                }
                serifAnnotationData.writeSerifAnnotationData(path);
            } else {
                SerifAnnotationData serifAnnotationData = new SerifAnnotationData(docId);
                // Event
                serifAnnotationData.eventMentionAnnotations.addAll(docIdToEventMentions.getOrDefault(docId, new HashMap<>()).values());
                // Event Event Relation
                serifAnnotationData.eventEventRelationMentionAnnotations.addAll(docIdToEventEventRelationMentions.getOrDefault(docId, new HashMap<>()).values());
                // Event Event Argument Relation
                serifAnnotationData.eventEventArgumentRelationMentionAnnotations.addAll(docIdToEventEventArgumentRelationMentions.getOrDefault(docId, new HashMap<>()).values());
                serifAnnotationData.writeSerifAnnotationData(path);
            }

        }

    }
}
