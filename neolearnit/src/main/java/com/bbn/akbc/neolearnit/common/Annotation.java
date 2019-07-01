package com.bbn.akbc.neolearnit.common;

import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.storage.MapStorage;
import com.bbn.akbc.neolearnit.storage.impl.HashMapStorage;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Annotation {
    public enum FrozenState {
        FROZEN_GOOD,
        NO_FROZEN,
        FROZEN_BAD
    }

    public static class InMemoryAnnotationStorage {
        final Map<InstanceIdentifier, Multiset<LabelPattern>> annotationStorage;

        public InMemoryAnnotationStorage() {
            this.annotationStorage = new ConcurrentHashMap<>();
        }

        public InMemoryAnnotationStorage(Mappings mappings) {
            this.annotationStorage = new ConcurrentHashMap<>();
            for (InstanceIdentifier instanceIdentifier : mappings.getPatternInstances()) {
                for (LearnitPattern learnitPattern : mappings.getPatternsForInstance(instanceIdentifier)) {
                    Multiset<LabelPattern> buf = this.annotationStorage.getOrDefault(instanceIdentifier, HashMultiset.create());
                    buf.add((LabelPattern) learnitPattern);
                    this.annotationStorage.put(instanceIdentifier, buf);
                }
            }
        }

        public Mappings convertToMappings() {
            MapStorage<InstanceIdentifier, Seed> instance2Seed = new HashMapStorage.Builder<InstanceIdentifier, Seed>().build();
            MapStorage.Builder<InstanceIdentifier, LearnitPattern> instance2PatternTableBuilder = new HashMapStorage.Builder<>();
            for (InstanceIdentifier instanceIdentifier : this.annotationStorage.keySet()) {
                for (LabelPattern labelPattern : this.annotationStorage.get(instanceIdentifier)) {
                    instance2PatternTableBuilder.put(instanceIdentifier, labelPattern);
                }
            }
            Mappings mappings = new Mappings(instance2Seed, instance2PatternTableBuilder.build());
            return mappings;
        }

        public Multiset<LabelPattern> lookupInstanceIdentifierAnnotation(InstanceIdentifier instanceIdentifier) {
            return this.annotationStorage.getOrDefault(instanceIdentifier, HashMultiset.create());
        }

        public void addAnnotation(InstanceIdentifier instanceIdentifier, LabelPattern labelPattern) {
            // ATTENTION: Due to what's implemented in HashMapStorage. If you add the exact the same
            // instanceIdentifier,labelPattern pair several times, it will not be stored into Mappings.
            // But in memory, it's allowed to do so.
            Multiset<LabelPattern> buf = this.annotationStorage.getOrDefault(instanceIdentifier, HashMultiset.create());
            buf.add(labelPattern);
            this.annotationStorage.put(instanceIdentifier, buf);
        }

        public boolean isParticularAnnotationExists(InstanceIdentifier instanceIdentifier, LabelPattern labelPattern) {
            Collection<LabelPattern> labelPatterns = this.lookupInstanceIdentifierAnnotation(instanceIdentifier);
            if (labelPatterns == null) return false;
            return labelPatterns.contains(labelPattern);
        }

        public void deleteAnnotationUnderLabelPattern(InstanceIdentifier instanceIdentifier, LabelPattern labelPattern, int count) {
            if (!this.isParticularAnnotationExists(instanceIdentifier, labelPattern)) return;
            this.annotationStorage.get(instanceIdentifier).remove(labelPattern, count);
            if (this.annotationStorage.get(instanceIdentifier).size() < 1)
                this.annotationStorage.remove(instanceIdentifier);
        }

        public void deleteAllAnnotation(InstanceIdentifier instanceIdentifier) {
            this.annotationStorage.remove(instanceIdentifier);
        }

        public void deleteAnnotationUnderLabelPattern(InstanceIdentifier instanceIdentifier, LabelPattern labelPattern) {
            if (!this.isParticularAnnotationExists(instanceIdentifier, labelPattern)) return;
            this.annotationStorage.get(instanceIdentifier).remove(labelPattern);
            if (this.annotationStorage.get(instanceIdentifier).size() < 1)
                this.annotationStorage.remove(instanceIdentifier);
        }

        public int countOfAppears(InstanceIdentifier instanceIdentifier, LabelPattern labelPattern) {
            if (!this.isParticularAnnotationExists(instanceIdentifier, labelPattern)) return 0;
            return this.annotationStorage.get(instanceIdentifier).count(labelPattern);
        }

        public void clearAllAnnotation() {
            this.annotationStorage.clear();
        }

        public void MergeOther(InMemoryAnnotationStorage inMemoryAnnotationStorage) {
            this.annotationStorage.putAll(inMemoryAnnotationStorage.annotationStorage);
        }

        public Set<InstanceIdentifier> getAllInstanceIdentifier() {
            return this.annotationStorage.keySet();
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            InMemoryAnnotationStorage that = (InMemoryAnnotationStorage) o;
            return that.annotationStorage.equals(this.annotationStorage);
        }

        @Override
        public int hashCode() {
            return this.annotationStorage.hashCode();
        }
    }

    public static InMemoryAnnotationStorage mergeAnnotation(List<InMemoryAnnotationStorage> annotationStorages) {
        InMemoryAnnotationStorage ret = new InMemoryAnnotationStorage();
        for (InMemoryAnnotationStorage inMemoryAnnotationStorage : annotationStorages) {
            for (InstanceIdentifier instanceIdentifier : inMemoryAnnotationStorage.getAllInstanceIdentifier()) {
                Map<String, LabelPattern> shouldRemoveAnnotation = new HashMap<>();
                for (LabelPattern labelPatternInCurrent : ret.lookupInstanceIdentifierAnnotation(instanceIdentifier)) {
                    shouldRemoveAnnotation.put(labelPatternInCurrent.getLabel(), labelPatternInCurrent);
                }
                for (LabelPattern labelPattern : inMemoryAnnotationStorage.lookupInstanceIdentifierAnnotation(instanceIdentifier)) {
                    if (shouldRemoveAnnotation.containsKey(labelPattern.getLabel())) {
                        ret.deleteAnnotationUnderLabelPattern(instanceIdentifier, shouldRemoveAnnotation.get(labelPattern.getLabel()));
                    }
                    ret.addAnnotation(instanceIdentifier, labelPattern);
                }
            }
        }
        return ret;
    }
}
