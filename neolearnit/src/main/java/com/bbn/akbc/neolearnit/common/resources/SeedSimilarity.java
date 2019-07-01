package com.bbn.akbc.neolearnit.common.resources;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.resources.KMeans.SimilarityFunction;
import com.bbn.akbc.neolearnit.common.resources.KMeans.VectorLookupFunction;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.processing.preprocessing.PreprocessingInformation;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.scoring.scores.SeedScore;
import com.bbn.akbc.neolearnit.scoring.tables.AbstractScoreTable.ObjectWithScore;
import com.bbn.akbc.neolearnit.storage.StorageUtils;
import com.bbn.akbc.neolearnit.storage.structs.EfficientMultimapDataStore;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SeedSimilarity {

	private static Multimap<String,Integer> seedMatrix = null;
	private static KMeans<String> kmeans = null;
    private static StopWords stopwords = null;

	private static File getPreprocessFile(TargetAndScoreTables data) {
		return new File(LearnItConfig.get("learnit_root")+"/expts/"+
				LearnItConfig.get("learnit_expt_suffix")+"/"+data.getRelationPathName(), "preprocessedInfo.sjson");
	}

	private static File getKMeansFile(TargetAndScoreTables data) {
		return new File(LearnItConfig.get("learnit_root")+"/expts/"+
				LearnItConfig.get("learnit_expt_suffix")+"/"+data.getRelationPathName(), "kmeans.json");
	}

	public static synchronized void load(TargetAndScoreTables data) {
		try {
            if (seedMatrix == null) loadSeedMatrix(data);
			if (kmeans == null) getSaveKMeans(data);
            if (stopwords == null) stopwords = StopWords.getFromParamsWithBackoff("overlap_seed_match_filter_word_list");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static synchronized void clear() {
		seedMatrix = null;
		kmeans = null;
	}

	public static boolean isNotValid() {
		return seedMatrix==null || kmeans==null;
	}

	@SuppressWarnings("unchecked")
	private static synchronized void loadSeedMatrix(TargetAndScoreTables data) throws IOException {

		System.out.println("Loading seed similarities...");
        seedMatrix = StorageUtils.deserialize(getPreprocessFile(data), EfficientMultimapDataStore.class, true).makeMultimap();
		System.out.println("Done!");
	}

    public static synchronized void save(TargetAndScoreTables data, PreprocessingInformation info) throws IOException {
        StorageUtils.serialize(getPreprocessFile(data), info.reducedSeedPatterns(), true);
	}

    public static synchronized void updateSeedSimilarity(TargetAndScoreTables data, Multimap<String,Integer> updateMatrix) throws IOException {
        seedMatrix.putAll(updateMatrix);
        StorageUtils.serialize(getPreprocessFile(data), EfficientMultimapDataStore.fromMultimap(seedMatrix), true);
    }

	@SuppressWarnings("unchecked")
	public static synchronized void getSaveKMeans(TargetAndScoreTables data) throws IOException {

		File kmeansFile = getKMeansFile(data);
		if (kmeansFile.exists() && !LearnItConfig.optionalParamTrue("update_kmeans")) {
            System.out.println("Loading k-means clusters...");
            KMeans<String> km = StorageUtils.deserialize(kmeansFile, KMeans.class, false);
            if (LearnItConfig.optionalParamTrue("update_kmeans_to_max_k") && km.nMeans() < LearnItConfig.getInt("kmeans_k")) {
                runSaveKMeans(data);
            } else {
                kmeans = km;
                System.out.println("Done!");
            }

		} else {
			runSaveKMeans(data);
		}
	}

	public static synchronized void runSaveKMeans(TargetAndScoreTables data) {
        if (stopwords == null) stopwords = StopWords.getFromParamsWithBackoff("overlap_seed_match_filter_word_list");
		try {
            System.out.println("Generating k-means clusters...");
			KMeans<String> result = runKMeans(data);

			if (result != null) {
				StorageUtils.serialize(getKMeansFile(data), result, false);
			}
            kmeans = result;
            System.out.println("Done!");
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException("runSaveKMeans failed");
		}
	}

	public static synchronized KMeans<String> runKMeans(TargetAndScoreTables data) throws IOException {
		if (seedMatrix == null) loadSeedMatrix(data);

		List<ObjectWithScore<Seed, SeedScore>> frozen = data.getSeedScores().getFrozenObjectsWithScores();
		Collections.sort(frozen);

		Set<String> toCluster = new HashSet<String>();
		for (ObjectWithScore<Seed, SeedScore> obj : frozen) {
			Seed seed = obj.getObject().withProperText(data.getTarget());
			String key = LearnItConfig.optionalParamTrue("use_seed_groups") ? seed.getReducedForm(stopwords) : seed.toSimpleString();
			if (seedMatrix.containsKey(key)) {
				toCluster.add(key);
			}
		}

		if (toCluster.isEmpty()) return null;

        return KMeans.runKMeans(LearnItConfig.getInt("kmeans_k"), new ArrayList<String>(toCluster), new SeedStringSimilarityLookup(), new CosineSimilarity());
	}

	public static synchronized void updateKMeans(TargetAndScoreTables data) {
		try {
			load(data);

			if (kmeans == null) return;

			System.out.println("Adding new seeds to kmeans clusters...");

			Set<Seed> newSeeds = new HashSet<Seed>();
			for (ObjectWithScore<Seed,SeedScore> obj : data.getSeedScores().getFrozenObjectsWithScores()) {
                String key = LearnItConfig.optionalParamTrue("use_seed_groups") ? obj.getObject().getReducedForm(stopwords) : obj.getObject().toSimpleString();
				if (!kmeans.hasDataPoint(key) && seedMatrix.containsKey(key)) {
					newSeeds.add(obj.getObject());
				}
			}

			for (Seed s : newSeeds) {
                String key = LearnItConfig.optionalParamTrue("use_seed_groups") ? s.getReducedForm(stopwords) : s.toSimpleString();
				kmeans.addNewItem(key);
			}

			kmeans.runKMeans(10);

			StorageUtils.serialize(getKMeansFile(data), kmeans, false);
		} catch (Exception ex) {

			ex.printStackTrace();
			throw new RuntimeException("Failed to update kmeans!");
		}
	}

	public static class CosineSimilarity implements SimilarityFunction {

		@Override
		public double calculateSimilarity(Multiset<Integer> a, Multiset<Integer> b) {

			double num = 0.0;
			double adist = 0.0;
			double bdist = 0.0;

			Set<Integer> union = Sets.union(a.elementSet(), b.elementSet());

			if (union.size() == a.size() + b.size()) return 0.0;

			for (Integer item : union) {
				num += a.count(item)*b.count(item);
				adist += Math.pow(a.count(item), 2.0);
				bdist += Math.pow(b.count(item), 2.0);
			}

			return num/(Math.sqrt(adist)*Math.sqrt(bdist));
		}

	}

//	public static class JaccardSimilarity implements SimilarityFunction {
//
//		@Override
//		public double calculateSimilarity(Multiset<Integer> a, Multiset<Integer> b) {
//
//			Multiset<Integer> intersection = Multisets.intersection(a,b);
//			int denom = a.size() + b.size() - intersection.size();
//			return (double)intersection.size()/(double)denom;
//		}
//
//	}

	public static class SeedStringSimilarityLookup implements VectorLookupFunction<String> {

		@Override
		public Multiset<Integer> lookupVector(String item) {
//			return seedMatrix.get(item);
            return ImmutableMultiset.copyOf(seedMatrix.get(item));
		}

	}

	private static Map<String,Double> scoreCache = new HashMap<String,Double>();

	public static synchronized boolean seedHasScore(String seed) {
		return seedMatrix.containsKey(seed);
	}

	public static synchronized boolean seedHasScore(Seed seed) {
        String key = LearnItConfig.optionalParamTrue("use_seed_groups") ? seed.getReducedForm(stopwords) : seed.toSimpleString();

        if(seedMatrix == null)
        	return key.length() > 0;
        else
        	return key.length() > 0 ? seedMatrix.containsKey(key) : seedMatrix.containsKey(seed.toSimpleString());
	}

	public static synchronized double getUnknownSeedScore(String seed) {
		if (seedMatrix == null) return 0.0;

		if (!seedMatrix.containsKey(seed)) return 0.0;
		if (kmeans == null) return 0.0;
		if (scoreCache.containsKey(seed)) return scoreCache.get(seed);
		//Multiset<Integer> s = seedMatrix.get(seed);
		//if (s.size() < 2) return 0.0;

		double result = kmeans.bestCentroidAndScore(seed).getScore();
		//System.out.println("Score for "+seed.toString()+" = "+result);
		scoreCache.put(seed, result);
		return result;
	}

	public static synchronized double getUnknownSeedScore(Seed seed) {
        String key = LearnItConfig.optionalParamTrue("use_seed_groups") ? seed.getReducedForm(stopwords) : seed.toSimpleString();
		return key.length() > 0 ? getUnknownSeedScore(key) : getUnknownSeedScore(seed.toSimpleString());
	}

}
