package com.bbn.akbc.neolearnit.serializers.binary_event;

import com.bbn.akbc.neolearnit.common.Annotation;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;
import com.bbn.akbc.neolearnit.serializers.ExternalAnnotationBuilder;
import com.bbn.akbc.neolearnit.serializers.observations.OpenNREJson;
import com.bbn.akbc.neolearnit.storage.StorageUtils;
import com.bbn.bue.common.files.FileUtils;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.EventMention;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.Spanning;
import com.bbn.serif.theories.Token;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class OpenNREObserver extends ExternalAnnotationBuilder {


    final Map<InstanceIdentifier, SentenceTheory> instanceIdentifierSentenceTheoryMap;

    final File outputDir;
    final List<OpenNREJson> outputBuffer;
    final Set<Symbol> wordSet;


    public OpenNREObserver(File outputDir) throws IOException{
        super();
        this.outputDir = outputDir;
        FileUtils.recursivelyDeleteDirectory(outputDir);
        outputDir.mkdirs();
        // Reserve for word embedding

        this.outputBuffer = new ArrayList<>();
        this.wordSet = new HashSet<>();
        this.instanceIdentifierSentenceTheoryMap = new ConcurrentHashMap<>();
    }

    public void build() throws Exception {
        Map<InstanceIdentifier, SentenceTheory> instanceIdentifierSentenceTheoryMap = this.resolveSentenceTheory();
        ObjectMapper objectMapper = StorageUtils.getMapperWithoutTyping();
        for (InstanceIdentifier instanceIdentifier : this.inMemoryAnnotationStorage.getAllInstanceIdentifier()) {
            final SentenceTheory sentenceTheory = instanceIdentifierSentenceTheoryMap.get(instanceIdentifier);
            boolean NALabelAppears = false;
            boolean onlyNALabelAppears = true;
            final Spanning span1 = InstanceIdentifier.getSpanning(sentenceTheory, instanceIdentifier.getSlot0Start(), instanceIdentifier.getSlot0End(), instanceIdentifier.getSlotEntityType(0)).get();
            final Spanning span2 = InstanceIdentifier.getSpanning(sentenceTheory, instanceIdentifier.getSlot1Start(), instanceIdentifier.getSlot1End(), instanceIdentifier.getSlotEntityType(1)).get();
            final EventMention left = (EventMention) span1;
            final EventMention right = (EventMention) span2;
            for (LabelPattern labelPattern : this.inMemoryAnnotationStorage.lookupInstanceIdentifierAnnotation(instanceIdentifier)) {
                if (labelPattern.getFrozenState().equals(Annotation.FrozenState.FROZEN_BAD)) NALabelAppears = true;
                if (labelPattern.getFrozenState().equals(Annotation.FrozenState.FROZEN_GOOD))
                    onlyNALabelAppears = false;
            }
            if (NALabelAppears && onlyNALabelAppears) {
                LabelPattern labelPattern = new LabelPattern(Symbol.from("NA"), Annotation.FrozenState.FROZEN_BAD);

                OpenNREJson openNREJson = new OpenNREJson(left, right, labelPattern, instanceIdentifier, sentenceTheory);
                outputBuffer.add(openNREJson);
                for (Token token : sentenceTheory.tokenSequence()) {
                    this.wordSet.add(Symbol.from(token.tokenizedText().utf16CodeUnits()));
                }
            } else {
                for (LabelPattern labelPattern : this.inMemoryAnnotationStorage.lookupInstanceIdentifierAnnotation(instanceIdentifier)) {
                    if (labelPattern.getFrozenState().equals(Annotation.FrozenState.FROZEN_BAD)) continue;
                    OpenNREJson openNREJson = new OpenNREJson(left, right, labelPattern, instanceIdentifier, sentenceTheory);
                    outputBuffer.add(openNREJson);
                    for (Token token : sentenceTheory.tokenSequence()) {
                        this.wordSet.add(Symbol.from(token.tokenizedText().utf16CodeUnits()));
                    }
                }
            }
        }
        Writer dataJsonWritter = new BufferedWriter(new FileWriter(new File(this.outputDir, "data.json")));

        dataJsonWritter.write("[\n");
        for (int i = 0; i < this.outputBuffer.size(); ++i) {
            dataJsonWritter.write(objectMapper.writeValueAsString(this.outputBuffer.get(i)));
            if (i != this.outputBuffer.size() - 1) {
                dataJsonWritter.write(",\n");
            }
        }
        dataJsonWritter.write("\n]");
        dataJsonWritter.close();

        Writer writer = new BufferedWriter(new FileWriter(new File(this.outputDir, "wordlist.txt")));
        writer.write(String.join("\n", this.wordSet.stream().map(s -> s.asString()).collect(Collectors.toList())));
        writer.close();
    }
}
