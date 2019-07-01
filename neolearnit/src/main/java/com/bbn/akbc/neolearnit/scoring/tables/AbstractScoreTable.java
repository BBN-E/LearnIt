package com.bbn.akbc.neolearnit.scoring.tables;

import com.bbn.akbc.neolearnit.observations.LearnItObservation;
import com.bbn.akbc.neolearnit.scoring.scores.BootstrappedScore;
import com.bbn.akbc.neolearnit.storage.EfficientMapDataStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public abstract class AbstractScoreTable<T extends LearnItObservation, U extends BootstrappedScore<T>> {

	private final Map<T, U> table;
	@JsonProperty
	protected int iteration;

	@JsonProperty
	public EfficientMapDataStore<T,U> data() {
		return EfficientMapDataStore.fromMap(table);
	}

	protected List<T> orderedItemsCache;
	protected Map<T,Integer> indexMap;

	@JsonCreator
	public AbstractScoreTable(@JsonProperty("data") EfficientMapDataStore<T,U> data,
			@JsonProperty("iteration") int iteration) {
		table = data.makeMap();
		this.iteration = iteration;
		orderItems();
	}

	public AbstractScoreTable() {
		table = new HashMap<T, U>();
		iteration = 0;
		orderedItemsCache = Lists.newArrayList();
		indexMap = new HashMap<T,Integer>();
	}

	public synchronized void orderItems() {
		orderedItemsCache = Lists.newArrayList(table.keySet());
		Collections.sort(orderedItemsCache);
		indexMap = new HashMap<T,Integer>();
		for (int i=0;i<orderedItemsCache.size();i++) {
			indexMap.put(orderedItemsCache.get(i), i);
		}
	}


	/*
	 * THESE THREE FUNCTIONS
	 * allow for a compact way of representing and recovering
	 * known objects by integer index in a consistently sorted key set
	 * from this table
	 */

	public int getItemIndex(T item) {
		return indexMap.get(item);
	}

	public T getItemFromIndex(int index) {
		return orderedItemsCache.get(index);
	}

	public Multiset<T> translateIndexMultiset(Multiset<Integer> idMultiset) {
		Multiset<T> result = HashMultiset.create();
		for (Integer key : idMultiset.elementSet()) {
			result.add(getItemFromIndex(key), idMultiset.count(key));
		}
		return result;
	}

	/*
	 * Score table needs to know iteration because it doesn't know the
	 * TargetAndScoreTables that it is a parent of
	 */
	public void incrementIteration() {
		iteration++;
	}

	public int getIteration() {
		return iteration;
	}

	public void setIteration(int iteration) {
		this.iteration = iteration;
	}

	/**
	 * Unfreeze objects with scores below the given threshold of confidence
	 * Used during evaluation
	 * @param threshold
	 */
	public void setConfidenceThreshold(double threshold) {
		for (ObjectWithScore<T,U> scoredObj : getFrozenObjectsWithScores()) {
			if (scoredObj.getScore().getConfidence() < threshold) {
				scoredObj.getScore().unfreeze();
			}
		}
	}

	public abstract void reduceSize();

	public void removeItem(T item) {
		table.remove(item);
	}

	/**
	 * Get rid of the proposed items that weren't observed
	 */
	public void removeProposed() {
		List<T> elements = new ArrayList<T>(table.keySet());
		for (T item : elements) {
			if (table.get(item).isProposed()) table.remove(item);
		}
	}

	public void removeBadForHuman(boolean cutoffConfidence) {
		List<T> elements = new ArrayList<T>(table.keySet());
		for (T item : elements) {
			if (!table.get(item).isFrozen()) {
				if (cutoffConfidence && table.get(item).getConfidence() < 0.01) {
					table.remove(item);
				}
			}
		}
	}

	/**
	 * Get rid of the items that weren't frozen, for evaluation purposes and reducing the display
	 */
	public void removeNonFrozen() {
		List<T> elements = new ArrayList<T>(table.keySet());
		for (T item : elements) {
			if (!table.get(item).isFrozen()) table.remove(item);
		}
	}

	/**
	 * Get rid of the items that weren't frozen, for evaluation purposes and reducing the display
	 */
	public void clear() {
		table.clear();
	}

	/**
	 * Returns whether or not the object has a score and that score is frozen
	 * @param obj
	 * @return
	 */
	public boolean isKnownFrozen(T obj) {
		return hasScore(obj) && getScore(obj).isFrozen();
	}

	/**
	 * Returns whether or not the given object has a score
	 * @param obj
	 * @return
	 */
	public boolean hasScore(T obj) {
		return table.containsKey(obj);
	}

	/**
	 * Gets the score for the given object, throws an error if the object has no score
	 * @param obj
	 * @return
	 */
	public U getScore(T obj) {
		if (!table.containsKey(obj)) {
			throw new RuntimeException("No score set for object "+obj.toPrettyString());
		}
		return table.get(obj);
	}

	/**
	 * Gets the score for the given object, creates a new default score if that object has no score
	 * @param obj
	 * @return
	 */
	public U getScoreOrDefault(T obj) {
		if (!table.containsKey(obj)) {
			table.put(obj, makeDefaultScore(obj));
		}
		return table.get(obj);
	}

	/**
	 * Adds a default score for the given object if it has no score
	 * @param obj
	 */
	public void addDefault(T obj) {
		if (!table.containsKey(obj)) {
			table.put(obj, makeDefaultScore(obj));
		}
	}

	public abstract U makeDefaultScore(T obj);

	public Set<T> keySet() {
		return table.keySet();
	}

	/**
	 * Gets all objects with frozen scores
	 * @return
	 */
	public Set<T> getFrozen() {
		Set<T> result = new HashSet<T>();
		for (T object : table.keySet()) {
			if (table.get(object).isFrozen()) {
				result.add(object);
			}
		}
		return result;
	}

	/**
	 * Gets all objects without frozen scores
	 * @return
	 */
	public Set<T> getNonFrozen() {
		Set<T> result = new HashSet<T>();
		for (T object : table.keySet()) {
			if (!table.get(object).isFrozen()) {
				result.add(object);
			}
		}
		return result;
	}

	public List<ObjectWithScore<T,U>> getFrozenObjectsWithScores() {
		List<ObjectWithScore<T,U>> result = new ArrayList<ObjectWithScore<T,U>>();
		for (T object : table.keySet()) {
			if (table.get(object).isFrozen()) {
				result.add(new ObjectWithScore<T,U>(object, table.get(object)));
			}
		}
		return result;
	}

	public List<ObjectWithScore<T,U>> getNonFrozenObjectsWithScores() {
		List<ObjectWithScore<T,U>> result = new ArrayList<ObjectWithScore<T,U>>();
		for (T object : table.keySet()) {
			if (!table.get(object).isFrozen()) {
				result.add(new ObjectWithScore<T,U>(object, table.get(object)));
			}
		}
		return result;
	}

	public List<ObjectWithScore<T,U>> getObjectsWithScores() {
		List<ObjectWithScore<T,U>> result = new ArrayList<ObjectWithScore<T,U>>();
		for (T object : table.keySet()) {
			result.add(new ObjectWithScore<T,U>(object, table.get(object)));
		}
		return result;
	}

	public static <T extends LearnItObservation, U extends BootstrappedScore<T>> double calculateAverageFrequency(
			List<ObjectWithScore<T, U>> scoredObjs) {

		int frequencySum = 0;
		int numNonzeroScores = 0;
		for (ObjectWithScore<T, U> scoredObj : scoredObjs) {
			if (scoredObj.getScore().getFrequency() > 0) {
				numNonzeroScores++;
				frequencySum+=scoredObj.getScore().getFrequency();
			}
		}
		return (double)frequencySum/numNonzeroScores;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("-------------------------------\n");
		builder.append("   Frozen Objects: \n");
		builder.append("-------------------------------\n");
		List<ObjectWithScore<T,U>> frozen = getFrozenObjectsWithScores();
		Collections.sort(frozen);
		for (ObjectWithScore<T,U> scoreObj : frozen) {
			builder.append(scoreObj+"\n");
		}
		builder.append("\n");
		builder.append("-------------------------------\n");
		builder.append("   Unfrozen Objects: \n");
		builder.append("-------------------------------\n");
		List<ObjectWithScore<T,U>> unfrozen = getNonFrozenObjectsWithScores();
		Collections.sort(unfrozen);
		for (ObjectWithScore<T,U> scoreObj : unfrozen) {
			builder.append(scoreObj+"\n");
		}
		return builder.toString();
	}

	public String toFrozenString() {
		StringBuilder builder = new StringBuilder();
		builder.append("-------------------------------\n");
		builder.append("   Frozen Objects: \n");
		builder.append("-------------------------------\n");
		List<ObjectWithScore<T,U>> frozen = getFrozenObjectsWithScores();
		Collections.sort(frozen);
		for (ObjectWithScore<T,U> scoreObj : frozen) {
			builder.append(scoreObj+"\n");
		}
		return builder.toString();
	}

	public static class ObjectWithScore<T extends LearnItObservation, U extends BootstrappedScore<T>> implements Comparable<ObjectWithScore<T, U>>{
		private final T object;
		private final U score;

		public ObjectWithScore(T object, U score) {
			this.object = object;
			this.score = score;
		}

		public T getObject() {
			return object;
		}

		public U getScore() {
			return score;
		}

		@Override
		public String toString() {
			return object.toPrettyString()+": "+score.toString();
		}

		@Override
		public int compareTo(ObjectWithScore<T, U> arg0) {
			int comparison = this.getScore().compareTo(arg0.getScore());
			if (comparison == 0) {
				return object.toPrettyString().compareTo(arg0.object.toPrettyString());
			} else {
				return comparison;
			}
		}
	}

}
