package com.bbn.akbc.neolearnit.scoring.selectors;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.logging.Glogger;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.scoring.scores.PatternScore;
import com.bbn.akbc.neolearnit.scoring.tables.AbstractScoreTable.ObjectWithScore;
import com.bbn.akbc.neolearnit.scoring.tables.PatternScoreTable;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PatternSelector implements Selector<LearnitPattern, PatternScore, PatternScoreTable>{

	private final int numToFreeze;
	private final int minNumToFreeze;
	private final double confidenceThreshold;

	public PatternSelector(TargetAndScoreTables data, int numToFreeze) {
		this.numToFreeze = numToFreeze;
		this.minNumToFreeze = LearnItConfig.getInt("min_num_patterns_to_freeze");
		this.confidenceThreshold = data.getTarget().getPatternConfidenceCutoff();//LearnItConfig.getDouble("pattern_confidence_threshold");
	}

  	/*
  	 * select by sorting
  	 * - first by confidence
  	 * - then by precision
  	 * - then by recall
  	 */
	@Override
	public Set<LearnitPattern> freezeScores(PatternScoreTable scores) {

		List<ObjectWithScore<LearnitPattern,PatternScore>> tofreeze = scores.getNonFrozenObjectsWithScores();

		//just pick the highest confidence scores
		Collections.sort(tofreeze);

		int numBadFrozen = 0;
		int maxNumBadToFreeze = (int)Math.floor(LearnItConfig.getDouble("negative_rate")*this.numToFreeze);
		int numGoodFrozen = 0;
		int maxNumGoodToFreeze = this.numToFreeze-maxNumBadToFreeze;

		// freeze top numToFreeze seeds;
		Set<LearnitPattern> result = new HashSet<LearnitPattern>();
		int numFrozen = 0;
	  Glogger.logger().debug("Accepting patterns...");

        for (ObjectWithScore<LearnitPattern, PatternScore> scoredContext : tofreeze) {
			if ((scoredContext.getScore().getConfidence() >= confidenceThreshold || numFrozen < minNumToFreeze) &&
					scoredContext.getScore().getConfidence() > 0.0 &&
                    (LearnItConfig.optionalParamTrue("score_redundant_patterns") ||
                            scoredContext.getScore().getFrozenPatternInstanceCount() < scoredContext.getScore().getFrequency()))
            {

				if (!scoredContext.getScore().isGood()) {
					if (numBadFrozen >= maxNumBadToFreeze) continue;
					numBadFrozen++;
				  Glogger.logger().debug(
					    "ACCEPTING NEG: " + scoredContext.getObject()
						.toIDString() + " - " + scoredContext.getScore()
						.getConfidence());
				} else {
					if (numGoodFrozen >= maxNumGoodToFreeze) continue;
					numGoodFrozen++;
				  Glogger.logger().debug(
					    "ACCEPTING " + scoredContext.getObject().toIDString()
						+ " - " + scoredContext.getScore().getConfidence());
				}

				scoredContext.getScore().freezeScore(scores.getIteration());

				result.add(scoredContext.getObject());
                if (scoredContext.getScore().getFrozenPatternInstanceCount() < scoredContext.getScore().getFrequency())
				    numFrozen++; //Only increase count if this pattern isn't redundant

				if (numFrozen > numToFreeze) break;
			} else if (scoredContext.getScore().getFrozenPatternInstanceCount().equals(scoredContext.getScore().getFrequency())) {
			  Glogger.logger().debug(
				    "REJECTING (NO NEW) " + scoredContext.getObject().toIDString()
					+ " - " + scoredContext.getScore().getConfidence());

			} else if (scoredContext.getScore().getConfidence() < confidenceThreshold) {
			  Glogger.logger().debug(
				    "REJECTING " + scoredContext.getObject().toIDString() + " - "
					+ scoredContext.getScore().getConfidence());
			}
		}
	  Glogger.logger().debug("Froze " + numFrozen + " new contexts, with " + numGoodFrozen
		    + " being positive and " + numBadFrozen + " being negative");

		for (ObjectWithScore<LearnitPattern, PatternScore> scoredContext : scores.getFrozenObjectsWithScores()) {
			result.add(scoredContext.getObject());
		}

		//forget the sources of non-accepted things - to save space
		for (ObjectWithScore<LearnitPattern, PatternScore> scoredContext : scores.getNonFrozenObjectsWithScores()) {
			scoredContext.getScore().getSources().clear();
		}

		return result;
	}

}
