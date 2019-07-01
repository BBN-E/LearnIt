package com.bbn.akbc.neolearnit.mappings.filters;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.storage.MapStorage;
import com.bbn.akbc.neolearnit.util.InstanceIdentifierFilterForAnnotation;

import java.util.*;

public class SingleDirectionFilter implements MappingsFilter{
    public SingleDirectionFilter(){

    }

    public static class DirectionalInstanceIdentifier{
        final String docId;
        final int sentId;
        final int slot0Start;
        final int slot0End;
        final int slot1Start;
        final int slot1End;
        public DirectionalInstanceIdentifier(InstanceIdentifier instanceIdentifier){
            this.docId = instanceIdentifier.getDocid();
            this.sentId = instanceIdentifier.getSentid();
            boolean swap = (instanceIdentifier.getSlot0Start() >= instanceIdentifier.getSlot1Start());
            this.slot0Start = (swap)?instanceIdentifier.getSlot1Start():instanceIdentifier.getSlot0Start();
            this.slot0End = (swap)? instanceIdentifier.getSlot1End():instanceIdentifier.getSlot0End();
            this.slot1Start = (swap)?instanceIdentifier.getSlot0Start():instanceIdentifier.getSlot1Start();
            this.slot1End = (swap)?instanceIdentifier.getSlot0End():instanceIdentifier.getSlot1End();
        }
        public DirectionalInstanceIdentifier(final String docId,final int sentId,final int slot0Start,final int slot0End,final int slot1Start,final int slot1End){
            this.docId = docId;
            this.sentId = sentId;
            boolean swap = (slot0Start > slot1Start);
            this.slot0Start = (swap)?slot1Start:slot0Start;
            this.slot0End = (swap)? slot1End:slot0End;
            this.slot1Start = (swap)?slot0Start:slot1Start;
            this.slot1End = (swap)?slot0End:slot1End;
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DirectionalInstanceIdentifier that = (DirectionalInstanceIdentifier) o;
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

    public static Set<InstanceIdentifier> FilterOutBiDirectionalInstanceIdentidier(Collection<InstanceIdentifier> original){
        Map<DirectionalInstanceIdentifier,InstanceIdentifier> filterMap = new HashMap<>();
        for(InstanceIdentifier instanceIdentifier : original){
            filterMap.put(new DirectionalInstanceIdentifier(instanceIdentifier),instanceIdentifier);
        }
        Set<InstanceIdentifier> ret = new HashSet<>();
        for(DirectionalInstanceIdentifier key : filterMap.keySet()){
            ret.add(filterMap.get(key));
        }
        return ret;
    }

    @Override
    public Mappings makeFiltered(Mappings input) {
        MapStorage.Builder<InstanceIdentifier, Seed> instanceToSeedMapping = input.getInstance2Seed().getStorage().newBuilder();
        MapStorage.Builder<InstanceIdentifier, LearnitPattern> instanceToPatternMapping = input.getInstance2Pattern().getStorage().newBuilder();
        Set<InstanceIdentifier> instanceIdentifierSet = new HashSet<>(input.getPatternInstances());
        instanceIdentifierSet.addAll(input.getSeedInstances());
        instanceIdentifierSet = FilterOutBiDirectionalInstanceIdentidier(instanceIdentifierSet);
        for(InstanceIdentifier instanceIdentifier:input.getPatternInstances()){
            if(instanceIdentifierSet.contains(instanceIdentifier)){
                for(LearnitPattern learnitPattern:input.getPatternsForInstance(instanceIdentifier)){
                    instanceToPatternMapping.put(instanceIdentifier,learnitPattern);
                }
            }
        }
        for(InstanceIdentifier instanceIdentifier:input.getSeedInstances()){
            if(instanceIdentifierSet.contains(instanceIdentifier)){
                for(Seed seed:input.getSeedsForInstance(instanceIdentifier)){
                    instanceToSeedMapping.put(instanceIdentifier,seed);
                }
            }
        }
        Mappings newMappings =  new Mappings(instanceToSeedMapping.build(),instanceToPatternMapping.build());
        return newMappings;
    }
}
