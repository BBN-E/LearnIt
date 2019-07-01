package com.bbn.akbc.neolearnit.serializers.binary_event;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;
import com.bbn.akbc.neolearnit.serializers.ExternalAnnotationBuilder;
import com.bbn.serif.theories.EventMention;
import com.bbn.serif.theories.SentenceTheory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.Map;

public class RelationalTripleObserver extends ExternalAnnotationBuilder {
    final String txtFilePath;

    public RelationalTripleObserver(String txtFilePath) {
        // Event Event plz
        this.txtFilePath = txtFilePath;
    }
    @Override
    public void build() throws Exception {
        Map<InstanceIdentifier, SentenceTheory> instanceIdentifierSentenceTheoryMap = this.resolveSentenceTheory();
        Writer txtWriter = new BufferedWriter(new FileWriter(new File(this.txtFilePath)));
        for (InstanceIdentifier instanceIdentifier : this.inMemoryAnnotationStorage.getAllInstanceIdentifier()) {
            final SentenceTheory sentenceTheory = instanceIdentifierSentenceTheoryMap.get(instanceIdentifier);
            final EventMention leftEventMention = (EventMention) InstanceIdentifier.getSpanning(sentenceTheory, instanceIdentifier.getSlot0Start(), instanceIdentifier.getSlot0End(), instanceIdentifier.getSlotEntityType(0)).get();
            final EventMention rightEventMention = (EventMention) InstanceIdentifier.getSpanning(sentenceTheory, instanceIdentifier.getSlot1Start(), instanceIdentifier.getSlot1End(), instanceIdentifier.getSlotEntityType(1)).get();
            String event0_tokenized = leftEventMention.anchorNode().head().span().tokenizedText().utf16CodeUnits();
            String event1_tokenized = rightEventMention.anchorNode().head().span().tokenizedText().utf16CodeUnits();
            for (LabelPattern labelPattern : this.inMemoryAnnotationStorage.lookupInstanceIdentifierAnnotation(instanceIdentifier)) {
                txtWriter.write(String.format("%s||%s||%s\n",
                        event0_tokenized,
                        labelPattern.getLabel(),
                        event1_tokenized
                ));
            }
        }
        txtWriter.close();
    }
}
