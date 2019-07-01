package com.bbn.akbc.neolearnit.scoring.proposers;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.logging.Glogger;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.scoring.scores.SeedScore;
import com.bbn.akbc.neolearnit.scoring.tables.AbstractScoreTable.ObjectWithScore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SeedScoreProposer implements Proposer<Seed> {

  private final TargetAndScoreTables data;
  private final double confidenceThreshold;

  public SeedScoreProposer(TargetAndScoreTables data) {
    this.data = data;
    this.confidenceThreshold = LearnItConfig.getDouble("seed_proposal_confidence_threshold");
  }

  /*
   * Propose {amount} of seeds
   * - sort by confidence, take up to {amount} if exists
   * - sort by frequency, take up to {amount} if exists
   */
  @Override
  public Iterable<Seed> propose(Iterable<Seed> potentials, int amount) {

    boolean initial = data.getIteration() == 0;

    List<ObjectWithScore<Seed,SeedScore>> toPropose = data.getSeedScores().getNonFrozenObjectsWithScores();
    Glogger.logger().debug(toPropose.size() + " seeds up for proposal.");
    //just pick the highest confidence scores
    Collections.sort(toPropose);

    // freeze top numToFreeze seeds;
    Set<Seed> result = new HashSet<Seed>();
    int numProposed = 0;
    List<ObjectWithScore<Seed, SeedScore>> secondChance = new ArrayList<ObjectWithScore<Seed, SeedScore>>();

    for (ObjectWithScore<Seed, SeedScore> scoredSeed : toPropose) {
      if (scoredSeed.getScore().getConfidence() >= confidenceThreshold) {
	result.add(scoredSeed.getObject());
	Glogger.logger().debug(
	    "Proposing seeds: " + scoredSeed.getObject().toIDString() + " - " + scoredSeed.getScore());
	numProposed++;

	if (numProposed >= amount) break;
      } else if (initial) {
	secondChance.add(scoredSeed);
      } else if (scoredSeed.getScore().getConfidence() > 0.0) {
	Glogger.logger().debug(
	    "REJECTED: " + scoredSeed.getObject().toIDString() + " - " + scoredSeed.getScore());
      }
    }

    if (initial && numProposed < amount) {
      Collections.sort(secondChance, new Comparator<ObjectWithScore<Seed, SeedScore>>() {
	public int compare(ObjectWithScore<Seed, SeedScore> o, ObjectWithScore<Seed, SeedScore> o2) {
	  return o2.getScore().getFrequency() - o.getScore().getFrequency();
	}
      });

      for (int i=0; i < secondChance.size() && result.size() < amount; ++i)
	result.add(secondChance.get(i).getObject());
    }

    Glogger.logger().debug("Proposed " + numProposed + " new seeds.");

    //remove seeds that are not frozen and not proposed
    for (ObjectWithScore<Seed, SeedScore> scoredSeed : toPropose) {
      if (!result.contains(scoredSeed.getObject())) {
	data.getSeedScores().removeItem(scoredSeed.getObject());
      }
    }

    return result;
  }

}
