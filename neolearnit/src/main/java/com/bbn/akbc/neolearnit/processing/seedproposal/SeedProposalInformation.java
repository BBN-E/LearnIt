package com.bbn.akbc.neolearnit.processing.seedproposal;

import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.processing.PartialInformation;
import com.bbn.akbc.neolearnit.processing.seedpruning.SeedPruningInformation.SeedPartialInfo;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.storage.structs.EfficientMapDataStore;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SeedProposalInformation implements PartialInformation {

	private final Map<Seed,SeedPartialInfo> scores;

	@JsonProperty
	private EfficientMapDataStore<Seed,SeedPartialInfo> scores() {
		return EfficientMapDataStore.fromMap(scores);
	}


	@JsonCreator
	public SeedProposalInformation(@JsonProperty("scores") EfficientMapDataStore<Seed,SeedPartialInfo> scores) {
		this.scores = scores.makeMap();
	}

	public SeedProposalInformation(Map<Seed,SeedPartialInfo> scores) {
		this.scores = scores;
	}

	public double getPartialConfidence(Seed obj) {
		return scores.get(obj).confidenceNumerator/scores.get(obj).confidenceDenominator;
	}

	public double getRecall(Seed seed,TargetAndScoreTables data) {
		return this.scores.get(seed).confidenceNumerator/data.getTotalInstanceDenominator();
	}

	public SeedPartialInfo 	getScore(Seed obj) {
		return scores.get(obj);
	}

	public Set<Seed> getSeeds() {
		return scores.keySet();
	}

	public static class Builder {
		private final Map<Seed,SeedPartialInfo> scores;

		public Builder() {
			this.scores = new HashMap<Seed,SeedPartialInfo>();
		}

		public Builder withScore(Seed seed, SeedPartialInfo score) {
			scores.put(seed, score);
			return this;
		}

		public Builder withMergedScore(Seed seed, SeedPartialInfo info) {
			if (!scores.containsKey(seed)) {
				scores.put(seed, new SeedPartialInfo());
			}
			scores.get(seed).mergeIn(info);
			return this;
		}

		public SeedProposalInformation build() {
			return new SeedProposalInformation(this.scores);
		}

	}

}
