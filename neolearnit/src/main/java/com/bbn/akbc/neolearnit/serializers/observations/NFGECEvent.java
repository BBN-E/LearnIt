package com.bbn.akbc.neolearnit.serializers.observations;


import com.bbn.bue.common.symbols.Symbol;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import java.util.List;

public class NFGECEvent {
    public static class NFGECLineEntry {
        final int triggerStartTokenIdx;
        final int triggerEndTokenIdx;
        final Symbol sentenceText;
        final Symbol eventType;
        final Symbol triggerText;

        public NFGECLineEntry(int triggerStartTokenIdx, int triggerEndTokenIdx, Symbol sentenceText, Symbol eventType, Symbol triggerText) {
            this.triggerStartTokenIdx = triggerStartTokenIdx;
            this.triggerEndTokenIdx = triggerEndTokenIdx;
            this.sentenceText = sentenceText;
            this.eventType = eventType;
            this.triggerText = triggerText;
        }

        public String toNFGECLineEntryStr() {
            return Joiner.on("\t").join(
                    Lists.newArrayList(triggerStartTokenIdx, triggerEndTokenIdx + 1,
                            sentenceText.asString(), eventType.asString(), "HEAD|" + triggerText.asString()));
        }
    }

    public static class NFGECReconstructionEntry {
        final Symbol docPath;
        final int sentenceId;
        final int triggerStartTokenIdx;
        final int triggerEndTokenIdx;
        final List<String> eventTypes;

        public NFGECReconstructionEntry(Symbol docPath, int sentenceId, int triggerStartTokenIdx, int triggerEndTokenIdx, List<String> eventTypes) {
            this.docPath = docPath;
            this.sentenceId = sentenceId;
            this.triggerStartTokenIdx = triggerStartTokenIdx;
            this.triggerEndTokenIdx = triggerEndTokenIdx;
            this.eventTypes = eventTypes;
        }

        public String toNFGECConstructEntryStr() {
            return Joiner.on("\t").join(
                    Lists.newArrayList(docPath.asString(), sentenceId,
                            triggerStartTokenIdx, triggerEndTokenIdx + 1, String.join(" ", eventTypes)));
        }

    }

}
