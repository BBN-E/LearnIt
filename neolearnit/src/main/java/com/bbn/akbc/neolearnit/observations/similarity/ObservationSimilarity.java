package com.bbn.akbc.neolearnit.observations.similarity;

import com.bbn.akbc.neolearnit.observations.LearnItObservation;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.pattern.SerifPattern;
import com.bbn.akbc.utility.Pair;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class ObservationSimilarity {

    protected LearnItObservation observedElement;
    protected Map<? extends LearnItObservation, Double> similarObservations;


    protected ObservationSimilarity(LearnItObservation observedElement, Map<? extends LearnItObservation, Double> similarObservations) {
        checkNotNull(observedElement);
        checkNotNull(similarObservations);
        this.observedElement = observedElement;
        this.similarObservations = similarObservations;
    }



    public static List<Pair<LearnItObservation, Double>> mergeMultipleSimilarities(
            Collection<Optional<? extends ObservationSimilarity>> multipleSimilarities,
            double similarityScoreThreshold, Optional<Integer> cutOff,
            Optional<Collection<? extends LearnItObservation>> artifactsToFilter) {

        Multimap<LearnItObservation, Double> similarObservationsMultimap = HashMultimap.create();
        for (Optional<? extends ObservationSimilarity> optSimilarity : multipleSimilarities) {
            if (optSimilarity.isPresent()) {
                for (Map.Entry<? extends LearnItObservation, Double> entry : optSimilarity.get().similarObservations().entrySet()) {
                    if (!artifactsToFilter.isPresent() || !artifactsToFilter.get().contains(entry.getKey())) {
                        similarObservationsMultimap.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
        Map<LearnItObservation, Double> mergedSimilarityMap = new HashMap<>();
        for (LearnItObservation observation : similarObservationsMultimap.keySet()) {
            Collection<Double> scores = similarObservationsMultimap.get(observation);
            double mergedScore = 0.0;
            for (double score : scores) {
                mergedScore += score;
            }
            mergedScore /= (scores.size());
            mergedSimilarityMap.put(observation, mergedScore);
        }

        List<Pair<LearnItObservation, Double>> finalList = FluentIterable.from(mergedSimilarityMap.entrySet()).transform(new Function<Map.Entry<LearnItObservation, Double>, Pair<LearnItObservation, Double>>() {
            @Override
            public Pair<LearnItObservation, Double> apply(Map.Entry<LearnItObservation, Double> entry) {
                return Pair.fromEntry(entry);
            }
        }).toSortedList(new Comparator<Pair<LearnItObservation, Double>>() {
            @Override
            public int compare(Pair<LearnItObservation, Double> o1, Pair<LearnItObservation, Double> o2) {
                return o2.value.compareTo(o1.value); //reverse sorting
            }
        });
        return finalList.subList(0, cutOff.isPresent() && cutOff.get() < finalList.size() ? cutOff.get() : finalList.size());
    }

    public LearnItObservation observedElement() {
        return this.observedElement;
    }

    public Map<? extends LearnItObservation, Double> similarObservations() {
        return this.similarObservations;
    }

    public List<Pair<? extends LearnItObservation, Double>> getSimilarObservationsAsSortedList(double similarityScoreThreshold) {
        List<Pair<? extends LearnItObservation, Double>> list = getSimilarObservationsAsSortedList();
        return FluentIterable.from(list).filter(new Predicate<Pair<? extends LearnItObservation, Double>>() {
            @Override
            public boolean apply(Pair<? extends LearnItObservation, Double> pair) {
                return pair.value >= similarityScoreThreshold;
            }
        }).toList();
    }

    public List<Pair<? extends LearnItObservation, Double>> getSimilarObservationsAsSortedList(double similarityScoreThreshold, int sizeLimit) {
        List<Pair<? extends LearnItObservation, Double>> list = this.getSimilarObservationsAsSortedList(similarityScoreThreshold);
        if (list.size() > sizeLimit) {
            list = list.subList(0, sizeLimit);
        }
        return list;
    }

    public List<Pair<? extends LearnItObservation, Double>> getSimilarObservationsAsSortedList(int sizeLimit) {
        List<Pair<? extends LearnItObservation, Double>> list = this.getSimilarObservationsAsSortedList();
        if (list.size() > sizeLimit) {
            list = list.subList(0, sizeLimit);
        }
        return list;
    }

    public List<Pair<? extends LearnItObservation, Double>> getSimilarObservationsAsSortedList() {
        return FluentIterable.from(similarObservations.entrySet()).transform(new Function<Map.Entry<? extends LearnItObservation, Double>, Pair<? extends LearnItObservation, Double>>() {
            @Override
            public Pair<? extends LearnItObservation, Double> apply(Map.Entry<? extends LearnItObservation, Double> entry) {
                return Pair.fromEntry(entry);
            }
        }).toSortedList(new Comparator<Pair<? extends LearnItObservation, Double>>() {
            @Override
            public int compare(Pair<? extends LearnItObservation, Double> o1, Pair<? extends LearnItObservation, Double> o2) {
                return o2.value.compareTo(o1.value); //reverse sorting
            }
        });
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ObservationSimilarity that = (ObservationSimilarity) o;

        if (!observedElement.equals(that.observedElement)) return false;
        return similarObservations.equals(that.similarObservations);
    }

    @Override
    public int hashCode() {
        int result = observedElement.hashCode();
        result = 31 * result + similarObservations.hashCode();
        return result;
    }

    @Override
    public String toString() {
        String str = "{observedElement: " + observedElement.toString() + ", similarObservations: [";
        str += Joiner.on(" ").join(FluentIterable.from(similarObservations.entrySet()).transform(
                (Map.Entry<? extends LearnItObservation, Double> entry) -> Pair.fromEntry(entry)).toList());
        return str.trim() + "]}";
    }

    public String toIDString() {
        return this.toString();
    }

    public String toPrettyString() {
        return this.toIDString();
    }
}
