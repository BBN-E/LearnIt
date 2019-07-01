package com.bbn.akbc.neolearnit.util;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.mappings.filters.InstanceIdentifierFilter;
import com.google.common.collect.ImmutableList;

import java.util.*;

public class InstanceIdentifierFilterForAnnotation {
    // So sometimes, we only care about "basic" InstanceIdentifiers, by the word "basic" means, we don't strictly follow
    // equals method in InstanceIdentifiers, but care about some fields. An obvious usage is, different permutation of event
    // types can generate different InstanceIdentifier, but with same docId, sentId, slot0 spans, slot1 spans. When doing
    // annotation, we only care about higher level(relation catagory correctness) precision, instead of lower level percision
    // (strictly equal)

    public static final class BasicInstanceIdentifier{
        final String docId;
        final int sentId;
        final int slot0Start;
        final int slot0End;
        final int slot1Start;
        final int slot1End;
        public BasicInstanceIdentifier(InstanceIdentifier instanceIdentifier){
            this.docId = instanceIdentifier.getDocid();
            this.sentId = instanceIdentifier.getSentid();
            this.slot0Start = instanceIdentifier.getSlot0Start();
            this.slot0End = instanceIdentifier.getSlot0End();
            this.slot1Start = instanceIdentifier.getSlot1Start();
            this.slot1End = instanceIdentifier.getSlot1End();
        }
        public BasicInstanceIdentifier(final String docId,final int sentId,final int slot0Start,final int slot0End,final int slot1Start,final int slot1End){
            this.docId = docId;
            this.sentId = sentId;
            this.slot0Start = slot0Start;
            this.slot0End = slot0End;
            this.slot1Start = slot1Start;
            this.slot1End = slot1End;
        }
        public String getDocId(){return this.docId;}
        public int getSentId(){return this.sentId;}
        public int getSlot0Start(){return this.slot0Start;}
        public int getSlot0End(){return this.slot0End;}
        public int getSlot1Start(){return this.slot1Start;}
        public int getSlot1End(){return this.slot1End;}
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
                    (this.slot1End == that.slot1End)
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
            return result;
        }



    }

    public static BasicInstanceIdentifier GenerateKeyForInstanceIdentifier(InstanceIdentifier instanceIdentifier){
        String docId = instanceIdentifier.getDocid();
        int sentId = instanceIdentifier.getSentid();
        int slot0Start = instanceIdentifier.getSlot0Start();
        int slot0End = instanceIdentifier.getSlot0End();
        int slot1Start = instanceIdentifier.getSlot1Start();
        int slot1End = instanceIdentifier.getSlot1End();
        return new BasicInstanceIdentifier(instanceIdentifier);
    }

    public static Set<InstanceIdentifier> makeFiltered(Collection<InstanceIdentifier> original){
        // WARNING: expect data reduction
        Map<BasicInstanceIdentifier,InstanceIdentifier> cacheTable = new HashMap<>();
        for(InstanceIdentifier instanceIdentifier:original){
            BasicInstanceIdentifier key = GenerateKeyForInstanceIdentifier(instanceIdentifier);
//            if(cacheTable.containsKey(key)){
//                System.out.println("We have duplicate InstanceIdentifier:\t"+
//                        key.docId + "\t" +
//                        key.sentId + "\t" +
//                        key.slot0Start + "\t" +
//                        key.slot0End + "\t" +
//                        key.slot1Start + "\t" +
//                        key.slot1End);
//            }
            cacheTable.put(key,instanceIdentifier);
        }
        Set<InstanceIdentifier> ret = new HashSet<>();
        for(BasicInstanceIdentifier key : cacheTable.keySet()){
            ret.add(cacheTable.get(key));
        }
        return ret;
    }

}
