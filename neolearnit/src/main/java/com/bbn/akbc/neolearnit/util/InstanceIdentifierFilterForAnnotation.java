package com.bbn.akbc.neolearnit.util;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.mappings.filters.InstanceIdentifierFilter;
import com.bbn.serif.theories.Spanning;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.collect.ImmutableList;

import java.util.*;

public class InstanceIdentifierFilterForAnnotation {
    // So sometimes, we only care about "basic" InstanceIdentifiers, by the word "basic" means, we don't strictly follow
    // equals method in InstanceIdentifiers, but care about some fields. An obvious usage is, different permutation of event
    // types can generate different InstanceIdentifier, but with same docId, sentId, slot0 spans, slot1 spans. When doing
    // annotation, we only care about higher level(relation catagory correctness) precision, instead of lower level percision
    // (strictly equal)

    public static final class BasicInstanceIdentifier {
        @JsonProperty("docId")
        final String docId;
        @JsonProperty("sentId")
        final int sentId;
        @JsonProperty("slot0Start")
        final int slot0Start;
        @JsonProperty("slot0End")
        final int slot0End;
        @JsonProperty("slot1Start")
        final int slot1Start;
        @JsonProperty("slot1End")
        final int slot1End;
        @JsonProperty("slot0Type")
        final InstanceIdentifier.SpanningType slot0Type;
        @JsonProperty("slot1Type")
        final InstanceIdentifier.SpanningType slot1Type;

        public BasicInstanceIdentifier(InstanceIdentifier instanceIdentifier) {
            this.docId = instanceIdentifier.getDocid();
            this.sentId = instanceIdentifier.getSentid();
            this.slot0Start = instanceIdentifier.getSlot0Start();
            this.slot0End = instanceIdentifier.getSlot0End();
            this.slot1Start = instanceIdentifier.getSlot1Start();
            this.slot1End = instanceIdentifier.getSlot1End();
            this.slot0Type = instanceIdentifier.getSlot0SpanningType();
            this.slot1Type = instanceIdentifier.getSlot1SpanningType();
        }

        @JsonCreator
        public BasicInstanceIdentifier(@JsonProperty("docId") final String docId, @JsonProperty("sentId") final int sentId, @JsonProperty("slot0Start") final int slot0Start, @JsonProperty("slot0End") final int slot0End, @JsonProperty("slot1Start") final int slot1Start, @JsonProperty("slot1End") final int slot1End, @JsonProperty("slot0Type") InstanceIdentifier.SpanningType slot0Type, @JsonProperty("slot1Type") InstanceIdentifier.SpanningType slot1Type) {
            this.docId = docId;
            this.sentId = sentId;
            this.slot0Start = slot0Start;
            this.slot0End = slot0End;
            this.slot1Start = slot1Start;
            this.slot1End = slot1End;
            this.slot0Type = slot0Type;
            this.slot1Type = slot1Type;
        }

        public String getDocId() {
            return this.docId;
        }

        public int getSentId() {
            return this.sentId;
        }

        public int getSlot0Start() {
            return this.slot0Start;
        }

        public int getSlot0End() {
            return this.slot0End;
        }

        public int getSlot1Start() {
            return this.slot1Start;
        }

        public int getSlot1End() {
            return this.slot1End;
        }

        public InstanceIdentifier.SpanningType getSlot0Type() {
            return this.slot0Type;
        }

        public InstanceIdentifier.SpanningType getSlot1Type() {
            return this.slot1Type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BasicInstanceIdentifier that = (BasicInstanceIdentifier) o;
            return ((this.docId.equals(that.docId)) &&
                    (this.sentId == that.sentId) &&
                    (this.slot0Start == that.slot0Start) &&
                    (this.slot0End == that.slot0End) &&
                    (this.slot1Start == that.slot1Start) &&
                    (this.slot1End == that.slot1End) &&
                    (this.slot0Type == that.slot0Type) &&
                    (this.slot1Type == that.slot1Type)
            );
        }

        @Override
        public int hashCode() {
            int result = this.docId.hashCode();
            result = 31 * result + this.sentId;
            result = 31 * result + this.slot0Start;
            result = 31 * result + this.slot0End;
            result = 31 * result + this.slot1Start;
            result = 31 * result + this.slot1End;
            result = 31 * result + this.slot0Type.hashCode();
            result = 31 * result + this.slot1Type.hashCode();
            return result;
        }


    }

    public static BasicInstanceIdentifier GenerateKeyForInstanceIdentifier(InstanceIdentifier instanceIdentifier) {
        return new BasicInstanceIdentifier(instanceIdentifier);
    }

    public static List<InstanceIdentifier> makeFiltered(Collection<InstanceIdentifier> original) {
        // WARNING: expect data reduction
        Map<BasicInstanceIdentifier, InstanceIdentifier> cacheTable = new HashMap<>();
        for (InstanceIdentifier instanceIdentifier : original) {
            BasicInstanceIdentifier key = GenerateKeyForInstanceIdentifier(instanceIdentifier);
            cacheTable.put(key, instanceIdentifier);
        }
        List<InstanceIdentifier> ret = new ArrayList<>();
        for (BasicInstanceIdentifier key : cacheTable.keySet()) {
            ret.add(cacheTable.get(key));
        }
        return ret;
    }

}
