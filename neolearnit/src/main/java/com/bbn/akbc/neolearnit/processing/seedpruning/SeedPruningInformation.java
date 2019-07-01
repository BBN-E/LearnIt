package com.bbn.akbc.neolearnit.processing.seedpruning;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.resources.SeedGroups;
import com.bbn.akbc.neolearnit.common.resources.SeedSimilarity;
import com.bbn.akbc.neolearnit.common.resources.StopWords;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.processing.PartialInformation;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.storage.EfficientMapDataStore;
import com.bbn.akbc.neolearnit.storage.EfficientMultisetDataStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;

import java.util.*;

public class SeedPruningInformation implements PartialInformation {

	private final Map<Seed,SeedPartialInfo> partialInfos;

	@JsonProperty
	private EfficientMapDataStore<Seed,SeedPartialInfo> partialInfos() {
		return EfficientMapDataStore.fromMap(partialInfos);
	}

	@JsonCreator
	public SeedPruningInformation(@JsonProperty("partialInfos") EfficientMapDataStore<Seed,SeedPartialInfo> partialInfos) {
		this.partialInfos = partialInfos.makeMap();
	}

	public SeedPruningInformation() {
		this.partialInfos = new HashMap<Seed,SeedPartialInfo>();
	}

	public SeedPartialInfo getPartialScore(Seed seed) {
		if (!partialInfos.containsKey(seed)) {
			partialInfos.put(seed, new SeedPartialInfo());
		}
		return partialInfos.get(seed);
	}

	public void recordPartialInfo(Seed seed, SeedPartialInfo info) {
		partialInfos.put(seed, info);
	}

	public Set<Seed> getSeeds() {
		return partialInfos.keySet();
	}


	public Set<Seed> getSeedsToScore() {
		Set<Seed> toScore = new HashSet<Seed>();
		for (Seed s : getSeeds()) {
			if (this.getPartialScore(s).knownInstanceCount > 0)
				toScore.add(s);
		}
		return toScore;
	}

	public static class SeedPartialInfo {
		@JsonProperty
		public int knownInstanceCount; // init: 0; set to # instances that has "known" pattern matches
		@JsonProperty
		public int totalInstanceCount; // init: 0; set to # instances that 1) has "known" pattern matches, or 2) has some seed similarity
		@JsonProperty
		public double confidenceNumerator; // confidence(instance) * max(confidences(pattern_matched))
		@JsonProperty
		public double confidenceDenominator; // confidence(instance)
		public Multiset<Integer> sources;
		@JsonProperty
		public EfficientMultisetDataStore<Integer> sources() {
			return EfficientMultisetDataStore.fromMultiset(sources);
		}

        private static StopWords stopwords;

		@JsonCreator
		private SeedPartialInfo(
				@JsonProperty("knownPatterns") Set<String> knownPatterns,
				@JsonProperty("totalPatterns") Set<String> totalPatterns,
				@JsonProperty("knownInstanceCount") int knownInstanceCount,
				@JsonProperty("totalInstanceCount") int totalInstanceCount,
				@JsonProperty("confidenceNumerator") double confidenceNumerator,
				@JsonProperty("confidenceDenominator") double confidenceDenominator,
				@JsonProperty("sources") EfficientMultisetDataStore<Integer> sources) {
			this.knownInstanceCount = knownInstanceCount;
			this.totalInstanceCount = totalInstanceCount;
			this.confidenceNumerator = confidenceNumerator;
			this.confidenceDenominator = confidenceDenominator;
			this.sources = sources.makeMultiset();
		}

		public SeedPartialInfo() {
			this.knownInstanceCount = 0;
			this.totalInstanceCount = 0;
			this.confidenceNumerator = 0.0;
			this.confidenceDenominator = 0.0;
			this.sources = HashMultiset.create();
            if (stopwords == null) stopwords = StopWords.getFromParamsWithBackoff("overlap_seed_match_filter_word_list");
		}

		public static SeedPartialInfo calculateInfo(Seed seed, Mappings mappings, TargetAndScoreTables data) {
			SeedPartialInfo result = new SeedPartialInfo();

			double seedSim = 0.0;
			if(!SeedSimilarity.isNotValid())
				seedSim = SeedSimilarity.getUnknownSeedScore(seed)*LearnItConfig.getDouble("seed_pruning_similarity_penalty");

            Collection<Seed> seedGroup;
            if (LearnItConfig.optionalParamTrue("use_seed_groups"))
                seedGroup = SeedGroups.getGroupOrSingleton(seed.getReducedForm(stopwords), seed);
            else
                seedGroup = ImmutableSet.of(seed);

            for (Seed s : seedGroup) {
                for (InstanceIdentifier id : mappings.getInstance2Seed().getInstances(s)) {

                    boolean hasKnown = false;
                    double maxConf = 0;
                    for (LearnitPattern p : mappings.getPatternsForInstance(id)) {
                        if (data.getPatternScores().hasScore(p)) {
                            hasKnown = true;
                            double patternConf = data.getPatternScores().getScore(p).getConfidence();
                            if (patternConf > maxConf) maxConf = patternConf;
                            result.sources.add(data.getPatternScores().getItemIndex(p)); // only care to record the known sources (for tracking + size reasons)
                        }
                    }

                    if (maxConf < seedSim) maxConf = seedSim;

                    if (hasKnown || maxConf > 0) {

                        if (hasKnown) result.knownInstanceCount++;
                        result.totalInstanceCount++;
                        result.confidenceNumerator += id.getConfidence() * maxConf;
                        result.confidenceDenominator += id.getConfidence();
                    }
                }
            }

			return result;
		}

		public void mergeIn(SeedPartialInfo other) {
			this.knownInstanceCount += other.knownInstanceCount;
			this.totalInstanceCount += other.totalInstanceCount;
			this.confidenceNumerator += other.confidenceNumerator;
			this.confidenceDenominator += other.confidenceDenominator;
			this.sources.addAll(other.sources);
		}

	}

}
