package com.bbn.akbc.neolearnit.scoring.tables;

import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.scoring.scores.PatternScore;
import com.bbn.akbc.neolearnit.scoring.scores.SeedScore;
import com.bbn.akbc.neolearnit.storage.EfficientMapDataStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SeedScoreTable extends AbstractScoreTable<Seed,SeedScore> {

	public SeedScoreTable() {
		super();
	}

	@JsonProperty(access= JsonProperty.Access.READ_ONLY)
	public List<String> FrontendSortableKeys(){
		Set<String> possibleItems = new HashSet<>();
		for(AbstractScoreTable.ObjectWithScore<Seed, SeedScore> seed :this.getObjectsWithScores()){
			possibleItems.addAll(seed.getScore().scoreForFrontendRanking.keySet());
		}
		return new ArrayList<>(possibleItems);
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

}
