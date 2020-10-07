package com.bbn.akbc.neolearnit.scoring.tables;

import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.scoring.scores.SeedScore;
import com.bbn.akbc.neolearnit.storage.EfficientMapDataStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SeedScoreTable extends AbstractScoreTable<Seed,SeedScore> {

	public SeedScoreTable() {
		super();
	}


	@JsonCreator
	public SeedScoreTable(@JsonProperty("data") EfficientMapDataStore<Seed,SeedScore> data,
			@JsonProperty("iteration") int iteration) {
		super(data,iteration);
	}

	@Override
	public SeedScore makeDefaultScore(Seed obj) {
		SeedScore score = new SeedScore(iteration);
		score.setScore(0.01D);
		score.setConfidence(0.0);
		return score;
	}

	/**
	 * Remove nonessential info to reduce extractor size
	 */
	@Override
	public void reduceSize() {
		for (ObjectWithScore<Seed, SeedScore> scoredObj : this.getNonFrozenObjectsWithScores()) {
			scoredObj.getScore().clearSources();
		}
	}

    public void updateScoreTableBasedOnMappings(Mappings mappings) {
        for (AbstractScoreTable.ObjectWithScore<Seed, SeedScore> seed : getObjectsWithScores()) {
            seed.getScore().setFrequency(mappings.getInstancesForSeed(seed.getObject()).size());
        }
    }
}
