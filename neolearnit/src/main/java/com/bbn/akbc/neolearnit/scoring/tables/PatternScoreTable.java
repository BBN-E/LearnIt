package com.bbn.akbc.neolearnit.scoring.tables;

import java.util.*;

import com.bbn.akbc.neolearnit.observations.pattern.ComboPattern;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.scoring.scores.PatternScore;
import com.bbn.akbc.neolearnit.storage.EfficientMapDataStore;
import com.fasterxml.jackson.annotation.*;

public class PatternScoreTable extends AbstractScoreTable<LearnitPattern,PatternScore> {

	public PatternScoreTable() {
		super();
	}


	@JsonProperty(access= JsonProperty.Access.READ_ONLY)
	public List<String> FrontendSortableKeys(){
		Set<String> possibleItems = new HashSet<>();
		for(AbstractScoreTable.ObjectWithScore<LearnitPattern, PatternScore> pattern :this.getObjectsWithScores()){
			possibleItems.addAll(pattern.getScore().scoreForFrontendRanking.keySet());
		}
		return new ArrayList<>(possibleItems);
	}

	@JsonCreator
	public PatternScoreTable(@JsonProperty("data") EfficientMapDataStore<LearnitPattern,PatternScore> data,
			@JsonProperty("iteration") int iteration) {
		super(data,iteration);
	}

	@Override
	public PatternScore makeDefaultScore(LearnitPattern obj) {
		PatternScore score = new PatternScore(iteration);
		score.setPrecision(0.01D);
		score.setRecall(0.01D);
		score.setConfidence(0.0);
		return score;
	}

	public Collection<ComboPattern> getComboPatterns(boolean includeNonFrozen) {
		Collection<ComboPattern> comboPatterns = new ArrayList<ComboPattern>();
		for (LearnitPattern pattern : this.keySet()) {
			if (includeNonFrozen || this.getScore(pattern).isFrozen()) {
				if (pattern instanceof ComboPattern) {
					comboPatterns.add((ComboPattern)pattern);
				}
			}
		}
		return comboPatterns;
	}

	/**
	 * Remove nonessential info to reduce extractor size
	 */
	@Override
	public void reduceSize() {
		for (ObjectWithScore<LearnitPattern, PatternScore> scoredObj : this.getNonFrozenObjectsWithScores()) {
			scoredObj.getScore().clearSources();
		}
	}

}
