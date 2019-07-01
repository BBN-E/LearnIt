package com.bbn.akbc.neolearnit.scoring.tables;

import com.bbn.akbc.neolearnit.observations.similarity.SeedPatternPair;
import com.bbn.akbc.neolearnit.scoring.scores.TripleScore;
import com.bbn.akbc.neolearnit.storage.EfficientMapDataStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TripleScoreTable extends AbstractScoreTable<SeedPatternPair,TripleScore> {

	public TripleScoreTable() {
		super();
	}

	@JsonCreator
	public TripleScoreTable(@JsonProperty("data") EfficientMapDataStore<SeedPatternPair,TripleScore> data,
                            @JsonProperty("iteration") int iteration) {
		super(data,iteration);
	}

	@Override
	public TripleScore makeDefaultScore(SeedPatternPair obj) {
		TripleScore score = new TripleScore(iteration);
		score.setPrecision(0.01D);
		score.setRecall(0.01D);
		score.setConfidence(0.0);
		return score;
	}

	/**
	 * Remove nonessential info to reduce extractor size
	 */
	@Override
	public void reduceSize() {
		for (ObjectWithScore<SeedPatternPair, TripleScore> scoredObj : this.getNonFrozenObjectsWithScores()) {
			scoredObj.getScore().clearSources();
		}
	}

}
