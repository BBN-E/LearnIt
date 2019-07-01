package com.bbn.akbc.neolearnit.common.resources;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.storage.structs.EfficientMapDataStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


public class KMeans<T> {

	public interface SimilarityFunction {
		public double calculateSimilarity(Multiset<Integer> vector1, Multiset<Integer> vector2);
	}

	public interface VectorLookupFunction<T> {
		public Multiset<Integer> lookupVector(T item);
	}

	public static class AssignmentAndScore {
		private final int assignment;
		private final double score;
		private AssignmentAndScore(int assignment, double score) {
			this.assignment = assignment;
			this.score = score;
		}
		public int getAssignment() {
			return assignment;
		}
		public double getScore() {
			return score;
		}
	}

	private static final int RANDOM_SEED = 123;

	@JsonProperty
	private int k;
	@JsonProperty
	private final VectorLookupFunction<T> lookupFunction;
	@JsonProperty
	private final SimilarityFunction similarityFunction;

	private final Map<T,Integer> clusterAssignments;
	@JsonProperty
	private EfficientMapDataStore<T,Integer> clusterAssignments() {
		return EfficientMapDataStore.fromMap(clusterAssignments);
	}

	private final Map<Integer,Multiset<Integer>> centroids;
	@JsonProperty
	private Map<Integer,List<Integer>> centroids() {
		Map<Integer,List<Integer>> result = new HashMap<Integer,List<Integer>>();
		for (Integer i : centroids.keySet()) {
			result.put(i, Lists.newArrayList(centroids.get(i)));
		}
		return result;
	}

	private final List<T> dataPoints;
	private final Random r;

	@JsonCreator
	private KMeans(
			@JsonProperty("k") int k,
			@JsonProperty("centroids") Map<Integer,List<Integer>> centroids,
			@JsonProperty("clusterAssignments") EfficientMapDataStore<T,Integer> clusterAssignments,
			@JsonProperty("lookupFunction") VectorLookupFunction<T> lookupFunction,
			@JsonProperty("similarityFunction") SimilarityFunction similarityFunction) {

		this.k = k;
		this.centroids = new HashMap<Integer,Multiset<Integer>>();
		for (Integer i : centroids.keySet()) {
			this.centroids.put(i, HashMultiset.create(centroids.get(i)));
		}
		this.clusterAssignments = clusterAssignments.makeMap();
		this.dataPoints = Lists.newArrayList(this.clusterAssignments.keySet());
		this.lookupFunction = lookupFunction;
		this.similarityFunction = similarityFunction;

		r = new Random(RANDOM_SEED);
	}



	private KMeans(List<T> dataPoints, int k,
			VectorLookupFunction<T> lookupFunction, SimilarityFunction similarityFunction) {

		this.k = k;
		this.dataPoints = dataPoints;
		this.lookupFunction = lookupFunction;
		this.similarityFunction = similarityFunction;

		if (k > dataPoints.size()) k = dataPoints.size();
		this.clusterAssignments = new HashMap<T,Integer>();
		this.centroids = new HashMap<Integer,Multiset<Integer>>();
		r = new Random(RANDOM_SEED);
	}


	/**
	 * Assigns the given item to the given cluster
	 * @param item
	 * @param cluster
	 * @return  true if the item's assignment changed as a result of this call
	 */
	public boolean assignCluster(T item, Integer cluster) {
		if (!clusterAssignments.containsKey(item) || !(clusterAssignments.get(item).equals(cluster))) {
			clusterAssignments.put(item, cluster);
			return true;
		} else {
			return false;
		}
	}

	public boolean hasDataPoint(T item) {
		return clusterAssignments.containsKey(item);
	}

	public int getCluster(T item) {
		return clusterAssignments.get(item);
	}

	public List<T> getItems(int cluster) {
		List<T> items = new ArrayList<T>();
		for (T key : clusterAssignments.keySet()) {
			if (this.clusterAssignments.get(key) == cluster) {
				items.add(key);
			}
		}
		return items;
	}

	public void assignRandomly() {
		Collections.shuffle(dataPoints, r);
		if (dataPoints.size() < k) {
		    k = dataPoints.size();
		}

		//assign at least one item to each cluster
		for (int i=0;i<k;i++) {
			assignCluster(dataPoints.get(i), i);
		}

		//randomly assign the rest
		for (int i=k;i<dataPoints.size(); i++) {
			assignCluster(dataPoints.get(i),r.nextInt(k));
		}
	}

	public void computeCentroid(int i) {
		Multiset<Integer> centroid = HashMultiset.create();
		for (T key : clusterAssignments.keySet()) {
			if (clusterAssignments.get(key) == i) {
				centroid.addAll(lookupFunction.lookupVector(key));
			}
		}

		centroids.put(i, centroid);
	}

	public void computeCentroids() {
		centroids.clear();
		for (int i=0;i<k;i++) {
			computeCentroid(i);
		}
	}

    private Multiset<Integer> centroidWithout(int i, T toIgnore) {
        if (!clusterAssignments.containsKey(toIgnore) || getCluster(toIgnore) != i)
            return centroids.get(i);

        Multiset<Integer> centroid = HashMultiset.create();
        for (T key : clusterAssignments.keySet()) {
            if (!key.equals(toIgnore) && clusterAssignments.get(key) == i) {
                centroid.addAll(lookupFunction.lookupVector(key));
            }
        }

        return centroid;
    }

	public AssignmentAndScore bestCentroidAndScore(T item) {

		double max = 0;
		int maxI = 0;
		Multiset<Integer> itemVector = lookupFunction.lookupVector(item);
		for (int i=0;i<k;i++) {
			double sim = similarityFunction.calculateSimilarity(itemVector, centroidWithout(i,item));
			if (sim > max) {
				max = sim;
				maxI = i;
			}
		}
		return new AssignmentAndScore(maxI,max);
	}

	public void addNewItem(T item) {
		dataPoints.add(item);
		int cluster = bestCentroidAndScore(item).getAssignment();
		assignCluster(item, cluster);
		computeCentroid(cluster);
	}

    private T lowestCorrelatedItem() {
        Collections.shuffle(dataPoints,r);
        double min = 1;
        T minItem = null;

        for (T item : dataPoints) {
            Multiset<Integer> itemVector = lookupFunction.lookupVector(item);
            int i = getCluster(item);
            if (getItems(i).size() > 1) {
                double sim = similarityFunction.calculateSimilarity(itemVector, centroidWithout(i, item));
                if (sim < min || minItem == null) {
                    min = sim;
                    minItem = item;
                }
            }
        }

        return minItem;
    }

    private boolean hasIsomorphicAssignments(Map<T,Integer> oldAssignments, Map<T,Integer> currAssignments) {
        //try to build a cluster-to-cluster translation. If successful, these assignments are isomorphic
        Map<Integer,Integer> isomorphism = new HashMap<Integer, Integer>();
        for (T item : currAssignments.keySet()) {
            int currI = currAssignments.get(item);
            int oldI  = oldAssignments.get(item);
            //If there's an inconsistency, then these aren't the same
            if (isomorphism.containsKey(currI) && !isomorphism.get(currI).equals(oldI))
                return false;
            isomorphism.put(currI,oldI);
        }
        return true;
    }

	public boolean recomputeAssignments() {

        Map<T,Integer> oldAssignments = ImmutableMap.copyOf(clusterAssignments);

//		boolean assignmentChanged = false;
		for (T item : dataPoints) {
			int assignment = bestCentroidAndScore(item).getAssignment();
			/*if (*/assignCluster(item, assignment);/*)*/
//				assignmentChanged = true;
		}
		// if any cluster now has no elements, randomly reassign things to it
//		Collections.shuffle(dataPoints,r);
//		int reassignmentIdx = 0;
//		for (int i=0;i<k;i++) {
//			List<T> items = getItems(i);
//			if (items.isEmpty()) {
//				assignCluster(dataPoints.get(reassignmentIdx), i);
//				reassignmentIdx++;
//				assignmentChanged = true;
//			}
//		}

        // if any cluster now has no elements, randomly reassign things to it
		for (int i=0;i<k;i++) {
			List<T> items = getItems(i);
			if (items.isEmpty()) {
                T lowestCorrelated = lowestCorrelatedItem();
                if (lowestCorrelated != null) {
                    assignCluster(lowestCorrelated, i);
//                    assignmentChanged = true;
                } else {
                    throw new RuntimeException(String.format("Have %d clusters but only %d data points!",k,dataPoints.size()));
                }
			}
		}

		return !hasIsomorphicAssignments(oldAssignments, clusterAssignments);
	}

    public int nMeans() {
        return this.centroids.keySet().size();
    }

	public boolean runKMeansIteration() {
		this.computeCentroids();
        if (LearnItConfig.optionalParamTrue("use_arbitrary_seed_clusters"))
            return false;
		return this.recomputeAssignments();
	}

	public int runKMeans(int iterMax) {
		int cur = 0;
		while (this.runKMeansIteration()) {
			cur++;
			if (cur > iterMax) {
				break;
			}
		}
		return cur;
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		for (int i=0;i<k;i++) {
			result.append(i+": "+getItems(i)+"\n");
		}
		return result.toString();
	}

	public static <T> KMeans<T> runKMeans(int k, List<T> dataPoints,
			VectorLookupFunction<T> lookupFunction, SimilarityFunction similarityFunction) {

		System.out.println("Running k-means to make "+k+" clusters...");

		KMeans<T> result = new KMeans<T>(dataPoints,k,lookupFunction,similarityFunction);
		result.assignRandomly();
		//iterate until convergence but not more than N iterations
		int cur = result.runKMeans(100);
		System.out.println("Stopping after "+cur+" iterations...");

		System.out.println(result.toString());
		return result;
	}
}
