package com.bbn.akbc.neolearnit.scoring.selectors;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.resources.SeedGroups;
import com.bbn.akbc.neolearnit.common.resources.StopWords;
import com.bbn.akbc.neolearnit.logging.Glogger;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.scoring.scores.SeedScore;
import com.bbn.akbc.neolearnit.scoring.tables.AbstractScoreTable.ObjectWithScore;
import com.bbn.akbc.neolearnit.scoring.tables.SeedScoreTable;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SeedSelector implements Selector<Seed,SeedScore,SeedScoreTable> {

  private final int numToFreeze;
  //private final int minNumToFreeze;
  private final boolean ignoreFrequency;
  private final double confidenceThreshold;
  private final StopWords stopwords;

  private boolean doNotCheckForNewFact = false;

  public SeedSelector(int numToFreeze) {
    this(numToFreeze,false);
  }

  public SeedSelector(int numToFreeze, boolean ignoreFrequency) {
    this.numToFreeze = numToFreeze;
    this.ignoreFrequency = ignoreFrequency;
    //this.minNumToFreeze = LearnItConfig.getInt("min_num_seeds_to_freeze");
    this.confidenceThreshold = LearnItConfig.getDouble("seed_confidence_threshold");
    this.stopwords = StopWords.getFromParamsWithBackoff("overlap_seed_match_filter_word_list");
  }

  public void setDoNotCheckForNewFact(boolean doNotCheckForNewFact) {
    this.doNotCheckForNewFact = doNotCheckForNewFact;
  }

  /*
   * freeze up to {numToFreeze} seeds
   * - based on the list sorted by score
   * - confidence must be greater than a confidenceThreshold ("seed_confidence_threshold")
   */
  @Override
  public Set<Seed> freezeScores(SeedScoreTable scores) {

    List<ObjectWithScore<Seed, SeedScore>> tofreeze = scores.getNonFrozenObjectsWithScores();

    //just pick the highest confidence scores
    Collections.sort(tofreeze);

    int numBadFrozen = 0;
    int maxNumBadToFreeze = (int)Math.floor(LearnItConfig.getDouble("negative_rate")*this.numToFreeze);
    int numGoodFrozen = 0;
    int maxNumGoodToFreeze = this.numToFreeze-maxNumBadToFreeze;

    // freeze top numToFreeze seeds;
    Set<Seed> result = new HashSet<Seed>();
    int numFrozen = 0;
    Glogger.logger().debug("Accepting seeds...");
    Glogger.logger().debug("Confidence threshold: " + confidenceThreshold);
    for (ObjectWithScore<Seed, SeedScore> scoredSeed : tofreeze) {


      if (ignoreFrequency || (doNotCheckForNewFact||scoredSeed.getScore().getKnownFrequency() < scoredSeed.getScore().getFrequency())) { //cases where you already know all the patterns aren't interesting

	if (!result.contains(scoredSeed.getObject()) && scoredSeed.getScore().getConfidence() > confidenceThreshold && scoredSeed.getScore().getConfidence() > 0.0) {
	  Glogger.logger().debug(
	      "SeedSelector: seed=" + scoredSeed.getObject().toIDString()
		  + ", maxNumGoodToFreeze=" + maxNumGoodToFreeze
		  + ", maxNumBadToFreeze=" + maxNumBadToFreeze
		  + ", isGood=" + scoredSeed.getScore().isGood()
		  + ", getScore=" + scoredSeed.getScore()
		  + ", doNotCheckForNewFact=" + doNotCheckForNewFact
		  + ", ignoreFrequency=" + ignoreFrequency
		  + ", getKnownFrequency=" + scoredSeed.getScore().getKnownFrequency()
		  + ", getFrequency=" + scoredSeed.getScore()
		  .getFrequency()
		  + ", getConfidence=" + scoredSeed.getScore()
		  .getConfidence());
	  if (!scoredSeed.getScore().isGood()) {
	    if (numBadFrozen >= maxNumBadToFreeze) continue;
	    numBadFrozen++;
	    Glogger.logger().debug(
		"NEG: " + scoredSeed.getObject().toIDString()
		    + " - " + scoredSeed.getScore()
		    .getConfidence());
	  } else {
	    if (numGoodFrozen >= maxNumGoodToFreeze) continue;
	    numGoodFrozen++;
	    Glogger.logger().debug(
		scoredSeed.getObject().toIDString() + " - "
		    + scoredSeed.getScore().getConfidence());
	  }

	  if (LearnItConfig.optionalParamTrue("use_seed_groups")) {
	    for (Seed seed : SeedGroups.getGroupOrSingleton(scoredSeed.getObject().getReducedForm(stopwords), scoredSeed.getObject())) {
	      if (!scores.hasScore(seed)) {
		SeedScore seedScore = scores.getScoreOrDefault(seed);
		seedScore.setIdenticalTo(scoredSeed.getScore());
	      }
	      scores.getScore(seed).freezeScore(scores.getIteration());
	      result.add(seed);
	    }
	  } else {
	    scoredSeed.getScore().freezeScore(scores.getIteration());
	    result.add(scoredSeed.getObject());
	  }

	  numFrozen++;
	  if (numFrozen > numToFreeze) break;
	}
      }
    }
    Glogger.logger().debug("Froze " + numFrozen + " new contexts, with " + numGoodFrozen
	+ " being positive and " + numBadFrozen + " being negative");

    //remember to stick old frozen things in the result
    for (ObjectWithScore<Seed, SeedScore> scoredSeed : scores.getFrozenObjectsWithScores()) {
      result.add(scoredSeed.getObject());
    }

    //forget the sources of non-accepted things - to save space
    for (ObjectWithScore<Seed, SeedScore> scoredSeed : scores.getNonFrozenObjectsWithScores()) {
      scoredSeed.getScore().getSources().clear();
    }

    return result;
  }

}
