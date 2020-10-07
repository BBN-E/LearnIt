package com.bbn.akbc.neolearnit.serializers.unary_event;

import com.bbn.akbc.neolearnit.common.Annotation;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;
import com.bbn.akbc.neolearnit.serializers.ExternalAnnotationBuilder;
import com.bbn.akbc.neolearnit.serializers.observations.SharableEventAnnotation;
import com.bbn.akbc.neolearnit.storage.StorageUtils;
import com.bbn.akbc.neolearnit.util.GeneralUtils;
import com.bbn.serif.theories.EventMention;
import com.bbn.serif.theories.SentenceTheory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SharableEventAnnotationObserver extends ExternalAnnotationBuilder {

    final String metadataPath;
    final String outputPath;

    public SharableEventAnnotationObserver(String metadataPath, String outputPath) {
        this.metadataPath = metadataPath;
        this.outputPath = outputPath;
    }

    @Override
    public void build() throws Exception {
        Map<InstanceIdentifier, SentenceTheory> instanceIdentifierSentenceTheoryMap = this.resolveSentenceTheory();
        Map<String, MetadataEntry> docIdToMetadata = new HashMap<>();
        List<String> metadataEntries = GeneralUtils.readLinesIntoList(this.metadataPath);
        for (String line : metadataEntries) {
            String[] split = line.split("\t");
            String docId = split[0];
            String uuid = split[6];
            int offset = Integer.parseInt(split[7]);
            docIdToMetadata.put(docId, new MetadataEntry(uuid, offset));
        }
        List<SharableEventAnnotation> ret = new ArrayList<>();
        for (InstanceIdentifier instanceIdentifier : this.inMemoryAnnotationStorage.getAllInstanceIdentifier()) {
            final SentenceTheory sentenceTheory = instanceIdentifierSentenceTheoryMap.get(instanceIdentifier);
            final int sentStart = sentenceTheory.span().startCharOffset().asInt();
            final int sentEnd = sentenceTheory.span().endCharOffset().asInt();
            final EventMention eventMention = (EventMention) InstanceIdentifier.getSpanning(sentenceTheory, instanceIdentifier.getSlot0Start(), instanceIdentifier.getSlot0End(), instanceIdentifier.getSlot0SpanningType()).get();
            final int triggerStart = eventMention.anchorNode().span().startCharOffset().asInt();
            final int triggerEnd = eventMention.anchorNode().span().endCharOffset().asInt();
            final String triggerString = eventMention.span().originalText().content().utf16CodeUnits();
            final String sentenceString = sentenceTheory.span().originalText().content().utf16CodeUnits();
            final String docUUID = docIdToMetadata.get(instanceIdentifier.getDocid()).uuid;
            final int offSet = docIdToMetadata.get(instanceIdentifier.getDocid()).offSet;
            for (LabelPattern labelPattern : this.inMemoryAnnotationStorage.lookupInstanceIdentifierAnnotation(instanceIdentifier)) {
                ret.add(new SharableEventAnnotation(docUUID, ImmutableList.of(ImmutableList.of(sentStart + offSet, sentEnd + offSet)), ImmutableList.of(ImmutableList.of(triggerStart + offSet, triggerEnd + offSet)), sentenceString, triggerString, labelPattern.getLabel(), labelPattern.getFrozenState().equals(Annotation.FrozenState.FROZEN_GOOD)));
            }
        }

        ObjectMapper objectMapper = StorageUtils.getMapperWithoutTyping();
        objectMapper.writeValue(new File(this.outputPath), ret);
    }

    public static class MetadataEntry {
        String uuid;
        int offSet;

        public MetadataEntry(String uuid, int offSet) {
            this.uuid = uuid;
            this.offSet = offSet;
        }
    }
}
