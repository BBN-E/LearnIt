package com.bbn.akbc.neolearnit.processing.patternproposal;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.processing.PatternScoringInformation;
import com.bbn.akbc.neolearnit.processing.patternpruning.PatternPruningInformation;
import com.bbn.akbc.neolearnit.processing.patternpruning.PatternPruningInformation.PatternPartialInfo;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.storage.EfficientMultisetDataStore;
import com.bbn.akbc.neolearnit.storage.structs.EfficientMapDataStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;

import java.util.Map;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;


public class PatternProposalInformation implements PatternScoringInformation {

	private final Map<LearnitPattern,PatternPartialInfo> stats;
	private final Multiset<Seed> knownSeedCounts;
	@JsonProperty
	private final double totalConfidence;
	@JsonProperty
	private final double totalConfidenceDenominator;

	@JsonProperty
	private EfficientMapDataStore<LearnitPattern,PatternPartialInfo> stats() {
		return EfficientMapDataStore.fromMap(stats);
	}

	@JsonProperty
	private EfficientMultisetDataStore<Seed> knownSeedCounts() {
		return EfficientMultisetDataStore.fromMultiset(knownSeedCounts);
	}

	@JsonCreator
	public PatternProposalInformation(
			@JsonProperty("stats") EfficientMapDataStore<LearnitPattern,PatternPartialInfo> stats,
			@JsonProperty("knownSeedCounts") EfficientMultisetDataStore<Seed> knownSeedCounts,
			@JsonProperty("totalConfidence") double totalConfidence,
			@JsonProperty("totalConfidenceDenominator") double totalConfidenceDenominator) {

		this.stats = stats.makeMap();
		this.totalConfidence = totalConfidence;
		this.totalConfidenceDenominator = totalConfidenceDenominator;
		this.knownSeedCounts = knownSeedCounts.makeMultiset();
	}

	public PatternProposalInformation(Map<LearnitPattern,PatternPartialInfo> stats,
			Multiset<Seed> knownSeedCounts, double totalConfidence, double totalConfidenceDenominator) {

		this.stats = stats;
		this.totalConfidence = totalConfidence;
		this.totalConfidenceDenominator = totalConfidenceDenominator;
		this.knownSeedCounts = knownSeedCounts;
	}

	@Override
	public PatternPartialInfo getPartialInfo(LearnitPattern pattern) {
		return this.stats.get(pattern);
	}

    @Override
    public boolean hasPartialInfo(LearnitPattern pattern) {
        return stats.containsKey(pattern);
    }

	public double getRecall(LearnitPattern pattern) {
		return this.stats.get(pattern).confidenceNumerator/this.totalConfidence;
	}

	public double getTotalConfidence() {
		return this.totalConfidence;
	}

	public double getTotalConfidenceDenominator() {
		return this.totalConfidenceDenominator;
	}

	public Set<LearnitPattern> getPatterns() {
		return stats.keySet();
	}

	public int getSeedCount(Seed s) {
		return knownSeedCounts.count(s);
	}

	public Set<Seed> getSeeds() {
		return knownSeedCounts.elementSet();
	}

    @Override
    public int getCount(LearnitPattern obj) {
        return getPartialInfo(obj).totalInstanceCount;
    }

    public static class Builder {
		private final Map<LearnitPattern,PatternPartialInfo> stats;
		private final Multiset<Seed> knownSeedCounts;
		private double totalConfidence;
		private double totalConfidenceDenominator;

		public Builder() {
			this.stats = new HashMap<LearnitPattern,PatternPartialInfo>();
			this.knownSeedCounts = HashMultiset.create();
			this.totalConfidenceDenominator = 0;
			this.totalConfidence = 0;
		}

		public Builder withAddedPatternStats(LearnitPattern pattern, Mappings mappings, TargetAndScoreTables data) {
			stats.put(pattern, PatternPartialInfo.calculateInfo(pattern, mappings, data));
			stats.get(pattern).sources.clear(); // we don't need this during proposal and it takes up space
			return this;
		}

		public Builder withCaclulatedTotalConfidence(Mappings mappings, TargetAndScoreTables data) {
			System.out.println("Calculating total confidence across all instances...");
			for (InstanceIdentifier id : mappings.getInstance2Seed().getAllInstances().elementSet()) {
				this.totalConfidence += PatternPruningInformation.PatternPartialInfo.instanceScore(id, mappings, data).value*id.getConfidence();
				this.totalConfidenceDenominator += id.getConfidence();
			}
			System.out.println("Total Confidence = "+this.totalConfidence+"/"+this.totalConfidenceDenominator);
			return this;
		}

		public Builder withAddedTotalConfidence(double amount) {
			this.totalConfidence += amount;
			return this;
		}

		public Builder withAddedTotalConfidenceDenominator(double amount) {
			this.totalConfidenceDenominator += amount;
			return this;
		}

		public Builder withMergedPatternStats(LearnitPattern pattern, PatternPartialInfo info) {
			if (!stats.containsKey(pattern)) {
				stats.put(pattern, new PatternPartialInfo());
			}
			stats.get(pattern).mergeIn(info);
			stats.get(pattern).sources.clear();
			return this;
		}

		public Builder withAddedSeedCount(Seed obj, int count) {
			knownSeedCounts.add(obj, count);
			return this;
		}

		public PatternProposalInformation build(int nBest) {
			//right before we build lets filters out the nBest
			List<LearnitPattern> patterns = Lists.newArrayList(stats.keySet());
			Collections.sort(patterns, new Comparator<LearnitPattern>() {

                @Override
                public int compare(LearnitPattern o1, LearnitPattern o2) {
                    return stats.get(o1).compareTo(stats.get(o2));
                }

            });

			patterns = patterns.subList(0, Math.min(patterns.size(), nBest));

			Map<LearnitPattern,PatternPartialInfo> finalMap = new HashMap<LearnitPattern,PatternPartialInfo>();
			for (LearnitPattern pattern : patterns) {
				finalMap.put(pattern, stats.get(pattern));
			}

			return new PatternProposalInformation(finalMap, this.knownSeedCounts,
					this.totalConfidence, this.totalConfidenceDenominator);
		}

	}

}
