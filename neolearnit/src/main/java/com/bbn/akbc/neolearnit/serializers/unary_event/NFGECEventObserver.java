package com.bbn.akbc.neolearnit.serializers.unary_event;


import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.util.SourceListsReader;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;
import com.bbn.akbc.neolearnit.serializers.ExternalAnnotationBuilder;
import com.bbn.akbc.neolearnit.serializers.observations.NFGECEvent;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.EventMention;
import com.bbn.serif.theories.SentenceTheory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class NFGECEventObserver extends ExternalAnnotationBuilder {
    final String outputPrefix;

    public NFGECEventObserver(String outputPrefix) {
        super();
        this.outputPrefix = outputPrefix;
    }


    @Override
    public void build() throws Exception {
        Map<InstanceIdentifier, SentenceTheory> instanceIdentifierSentenceTheoryMap = this.resolveSentenceTheory();
        List<NFGECEvent.NFGECLineEntry> nfgecLineEntries = new ArrayList<>();
        List<NFGECEvent.NFGECReconstructionEntry> nfgecReconstructionEntries = new ArrayList<>();
        for (InstanceIdentifier instanceIdentifier : this.inMemoryAnnotationStorage.getAllInstanceIdentifier()) {
            final SentenceTheory sentenceTheory = instanceIdentifierSentenceTheoryMap.get(instanceIdentifier);
            final EventMention eventMention = (EventMention) InstanceIdentifier.getSpanning(sentenceTheory, instanceIdentifier.getSlot0Start(), instanceIdentifier.getSlot0End(), instanceIdentifier.getSlotEntityType(0)).get();
            final Symbol sentenceText = Symbol.from(sentenceTheory.span().tokenizedText().utf16CodeUnits());
            final Symbol triggerHeadText = Symbol.from(eventMention.anchorNode().head().span().tokenizedText().utf16CodeUnits());
            List<String> eventTypes = new ArrayList<>();
            for (LabelPattern labelPattern : this.inMemoryAnnotationStorage.lookupInstanceIdentifierAnnotation(instanceIdentifier)) {
                nfgecLineEntries.add(new NFGECEvent.NFGECLineEntry(instanceIdentifier.getSlot0Start(), instanceIdentifier.getSlot0End(), sentenceText, Symbol
                        .from(labelPattern.getLabel()), triggerHeadText));
                eventTypes.add(labelPattern.getLabel());
            }
            nfgecReconstructionEntries.add(new NFGECEvent.NFGECReconstructionEntry((Symbol.from(new File(SourceListsReader.getFullPath(instanceIdentifier.getDocid())).getAbsolutePath())), instanceIdentifier.getSentid(), instanceIdentifier.getSlot0Start(), instanceIdentifier.getSlot0End(), eventTypes));
        }
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File(this.outputPrefix + "all.txt")));
        for (NFGECEvent.NFGECLineEntry nfgecLineEntry : nfgecLineEntries) {
            bufferedWriter.write(nfgecLineEntry.toNFGECLineEntryStr());
            bufferedWriter.newLine();
        }
        bufferedWriter.close();
        bufferedWriter = new BufferedWriter(new FileWriter(new File(this.outputPrefix + "reconstruct.txt")));
        for (NFGECEvent.NFGECReconstructionEntry nfgecReconstructionEntry : nfgecReconstructionEntries) {
            bufferedWriter.write(nfgecReconstructionEntry.toNFGECConstructEntryStr());
            bufferedWriter.newLine();
        }
        bufferedWriter.close();
    }
}
