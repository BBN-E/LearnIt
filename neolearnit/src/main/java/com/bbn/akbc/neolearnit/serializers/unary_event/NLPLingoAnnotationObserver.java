package com.bbn.akbc.neolearnit.serializers.unary_event;

import com.bbn.akbc.neolearnit.common.Annotation.FrozenState;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;
import com.bbn.akbc.neolearnit.serializers.ExternalAnnotationBuilder;
import com.bbn.akbc.neolearnit.serializers.observations.NLPLingoAnnotation;
import com.bbn.bue.common.files.FileUtils;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.EventMention;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.Spanning;
import com.google.common.base.Optional;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;


public class NLPLingoAnnotationObserver extends ExternalAnnotationBuilder {
    final String entityTypes;
    final String outputPath;
    final boolean shouldOutputNegative;
    public NLPLingoAnnotationObserver(final String outputPath, boolean shouldOutputNegative) {
        super();
        this.outputPath = outputPath;
        this.shouldOutputNegative = shouldOutputNegative;
        InputStream inputStream = NLPLingoAnnotationObserver.class.getClassLoader().getResourceAsStream("nlplingo_entity_type_in_domain_ontology.txt");
        BufferedReader br = new BufferedReader(new InputStreamReader(
                inputStream));
        entityTypes = new Scanner(br).useDelimiter("\\Z").next();
    }

    public void build() throws Exception {
        Map<InstanceIdentifier, SentenceTheory> instanceIdentifierSentenceTheoryMap = this.resolveSentenceTheory();


        List<String> spanSerif = new ArrayList<>();
        Map<Symbol,Set<Symbol>> eventTypeToKeywordsMapping = new HashMap<>();
        File outputPathObj = new File(this.outputPath);
        FileUtils.recursivelyDeleteDirectory(outputPathObj);
        outputPathObj.mkdirs();

        Set<String> converedEventTypes = new HashSet<>();

        Set<NLPLingoAnnotation.SentSpan> sentSpanSet = new HashSet<>();
        Map<Symbol, Set<NLPLingoAnnotation.NlplingoEventMention>> docIdToNlplingoSpan = new HashMap<>();
        Map<Symbol, Symbol> docIdToDocPath = new HashMap<>();
        for (InstanceIdentifier instanceIdentifier : this.inMemoryAnnotationStorage.getAllInstanceIdentifier()) {
            Symbol docId = Symbol.from(instanceIdentifier.getDocid());
            Symbol docPath = Symbol.from(ExternalAnnotationBuilder.getDocPath(docId.asString()));
            boolean shouldOutputSpan = false;
            for (LabelPattern labelPattern : this.inMemoryAnnotationStorage.lookupInstanceIdentifierAnnotation(instanceIdentifier)) {
                final SentenceTheory sentenceTheory = instanceIdentifierSentenceTheoryMap.get(instanceIdentifier);
                final int sentStart = sentenceTheory.span().startCharOffset().asInt();
                final int sentEnd = sentenceTheory.span().endCharOffset().asInt();
                final EventMention eventMention = (EventMention) InstanceIdentifier.getSpanning(sentenceTheory, instanceIdentifier.getSlot0Start(), instanceIdentifier.getSlot0End(), instanceIdentifier.getSlot0SpanningType()).get();
                final Optional<Spanning> slot1Spanning = InstanceIdentifier.getSpanning(sentenceTheory, instanceIdentifier.getSlot1Start(), instanceIdentifier.getSlot1End(), instanceIdentifier.getSlot1SpanningType());
                if (slot1Spanning.isPresent()) {
                    System.out.println("[WARNING] We're handling unary InstanceIdentifier, you happen to send binary one, which slot1 gets dropped.");
                }
                final int triggerStart = eventMention.anchorNode().span().startCharOffset().asInt();
                final int triggerEnd = eventMention.anchorNode().span().endCharOffset().asInt();
                final NLPLingoAnnotation.TriggerMarkingSpan triggerMarkingSpan = new NLPLingoAnnotation.TriggerMarkingSpan(
                        docId,
                        docPath,
                        sentStart,
                        sentEnd,
                        triggerStart,
                        triggerEnd,
                        Symbol.from(eventMention.anchorNode().span().tokenizedText().utf16CodeUnits())
                );
                if (labelPattern.getFrozenState() == FrozenState.FROZEN_GOOD) {
                    shouldOutputSpan = true;
                    sentSpanSet.add(new NLPLingoAnnotation.SentSpan(docId, triggerMarkingSpan.sentStartCharOff, triggerMarkingSpan.sentEndCharOff));
                    Set<NLPLingoAnnotation.NlplingoEventMention> buf = docIdToNlplingoSpan.getOrDefault(docId, new HashSet<>());
                    buf.add(new NLPLingoAnnotation.NlplingoEventMention(triggerMarkingSpan, Symbol.from(labelPattern.getLabel())));
                    docIdToNlplingoSpan.put(docId, buf);
                    Set<Symbol> triggerWordBuf = eventTypeToKeywordsMapping.getOrDefault(Symbol.from(labelPattern.getLabel()), new HashSet<>());
                    triggerWordBuf.add(triggerMarkingSpan.triggerWord);
                    eventTypeToKeywordsMapping.put(Symbol.from(labelPattern.getLabel()), triggerWordBuf);
                    converedEventTypes.add(labelPattern.getLabel());
                } else if (labelPattern.getFrozenState() == FrozenState.FROZEN_BAD && this.shouldOutputNegative) {
                    shouldOutputSpan = true;
                    sentSpanSet.add(new NLPLingoAnnotation.SentSpan(docId, triggerMarkingSpan.sentStartCharOff, triggerMarkingSpan.sentEndCharOff));
                    converedEventTypes.add(labelPattern.getLabel());
                }
            }
            if (shouldOutputSpan && !docIdToDocPath.containsKey(docId)) {
                docIdToDocPath.put(docId, docPath);
            }

        }

        for (Symbol docId : docIdToDocPath.keySet()) {
            Symbol docPath = docIdToDocPath.get(docId);
            String spanPath = Paths.get(this.outputPath, docId + ".span").toString();
            spanSerif.add(String.format("SPAN:%s SERIF:%s\n", spanPath, docPath));
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File(spanPath)));
            for (NLPLingoAnnotation.NlplingoEventMention nlplingoEventMention : docIdToNlplingoSpan.getOrDefault(docId, new HashSet<>())) {
                bufferedWriter.write(nlplingoEventMention.toNlplingoSpan());
            }
            bufferedWriter.close();
        }

        String sentSpanOutputPath = Paths.get(this.outputPath, "argument.sent_spans").toString();
        String spanSerifOutputPath = Paths.get(this.outputPath, "argument.span_serif_list").toString();
        String sentSpanListOutputPath = Paths.get(this.outputPath, "argument.sent_spans.list").toString();
        String domainOntologyOutputPath = Paths.get(this.outputPath, "domain_ontology.txt").toString();
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File(sentSpanOutputPath)));
        for(NLPLingoAnnotation.SentSpan sentSpan: sentSpanSet){
            bufferedWriter.write(sentSpan.toNlplingoSentSpan());
        }
        bufferedWriter.close();
        bufferedWriter = new BufferedWriter(new FileWriter(new File(spanSerifOutputPath)));
        for(String spanSerifStr:spanSerif){
            bufferedWriter.write(spanSerifStr);
        }
        bufferedWriter.close();
        bufferedWriter = new BufferedWriter(new FileWriter(new File(sentSpanListOutputPath)));
        bufferedWriter.write(sentSpanOutputPath);
        bufferedWriter.close();
        bufferedWriter = new BufferedWriter(new FileWriter(new File(domainOntologyOutputPath)));
        for(String coveredEventType:converedEventTypes){
            bufferedWriter.write(String.format("<Event type=\"%s\">\n",coveredEventType));
            bufferedWriter.write(String.format("<Role>%s</Role>\n","Time"));
            bufferedWriter.write(String.format("<Role>%s</Role>\n","Place"));
            bufferedWriter.write(String.format("<Role>%s</Role>\n","Active"));
            bufferedWriter.write(String.format("<Role>%s</Role>\n","Affected"));
            bufferedWriter.write(String.format("<Role>%s</Role>\n","Artifact"));
            bufferedWriter.write("</Event>\n");
        }
        bufferedWriter.write(entityTypes);
        bufferedWriter.close();
    }
}
