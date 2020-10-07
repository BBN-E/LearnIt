package com.bbn.akbc.neolearnit.serializers.binary_event;

import com.bbn.akbc.neolearnit.common.Annotation;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.serializers.ExternalAnnotationBuilder;
import com.bbn.akbc.neolearnit.serializers.observations.OpenNREJson;
import com.bbn.akbc.neolearnit.storage.StorageUtils;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.EventMention;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.Spanning;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CausalJSONObserver extends ExternalAnnotationBuilder {
    final File outputPath;
    final List<OpenNREJson> outputBuffer;
    final List<Mappings> patternMappingsList;
    final Set<LearnitPattern> goodPatterns;

    public CausalJSONObserver(File outputPath, List<Mappings> patternMappingsList, Set<LearnitPattern> goodPatterns) throws IOException {
        super();
        this.outputPath = outputPath;
        // Reserve for word embedding
        this.outputBuffer = new ArrayList<>();
        this.patternMappingsList = patternMappingsList;
        this.goodPatterns = goodPatterns;
    }

    public void build() throws Exception {
        Map<InstanceIdentifier, SentenceTheory> instanceIdentifierSentenceTheoryMap = this.resolveSentenceTheory();
        ObjectMapper objectMapper = StorageUtils.getMapperWithoutTyping();

        for (InstanceIdentifier instanceIdentifier : this.inMemoryAnnotationStorage.getAllInstanceIdentifier()) {
            final SentenceTheory sentenceTheory = instanceIdentifierSentenceTheoryMap.get(instanceIdentifier);
            boolean NALabelAppears = false;
            boolean onlyNALabelAppears = true;
            final Spanning span1 = InstanceIdentifier.getSpanning(sentenceTheory, instanceIdentifier.getSlot0Start(), instanceIdentifier.getSlot0End(), instanceIdentifier.getSlot0SpanningType()).get();
            final Spanning span2 = InstanceIdentifier.getSpanning(sentenceTheory, instanceIdentifier.getSlot1Start(), instanceIdentifier.getSlot1End(), instanceIdentifier.getSlot1SpanningType()).get();
            final EventMention left = (EventMention) span1;
            final EventMention right = (EventMention) span2;
            for (LabelPattern labelPattern : this.inMemoryAnnotationStorage.lookupInstanceIdentifierAnnotation(instanceIdentifier)) {
                if (labelPattern.getFrozenState().equals(Annotation.FrozenState.FROZEN_BAD)) NALabelAppears = true;
                if (labelPattern.getFrozenState().equals(Annotation.FrozenState.FROZEN_GOOD))
                    onlyNALabelAppears = false;
            }
            if (NALabelAppears && onlyNALabelAppears) {
                LabelPattern labelPattern = new LabelPattern(Symbol.from("NA"), Annotation.FrozenState.FROZEN_BAD);
                outputBuffer.addAll(OpenNREJson.createOpenNREJsonFromEventMention(left, right, labelPattern, instanceIdentifier, sentenceTheory, patternMappingsList, goodPatterns));
            } else {
                for (LabelPattern labelPattern : this.inMemoryAnnotationStorage.lookupInstanceIdentifierAnnotation(instanceIdentifier)) {
                    if (labelPattern.getFrozenState().equals(Annotation.FrozenState.FROZEN_BAD)) continue;
                    outputBuffer.addAll(OpenNREJson.createOpenNREJsonFromEventMention(left, right, labelPattern, instanceIdentifier, sentenceTheory, patternMappingsList, goodPatterns));
                }
            }
        }
        Writer dataJsonWritter = new BufferedWriter(new FileWriter(this.outputPath));

        dataJsonWritter.write("[\n");
        for (int i = 0; i < this.outputBuffer.size(); ++i) {
            dataJsonWritter.write(objectMapper.writeValueAsString(this.outputBuffer.get(i)));
            if (i != this.outputBuffer.size() - 1) {
                dataJsonWritter.write(",\n");
            }
        }
        dataJsonWritter.write("\n]");
        dataJsonWritter.close();
    }
}
