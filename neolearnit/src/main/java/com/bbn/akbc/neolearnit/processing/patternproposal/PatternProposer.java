package com.bbn.akbc.neolearnit.processing.patternproposal;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.resources.SeedSimilarity;
import com.bbn.akbc.neolearnit.logging.Glogger;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.processing.AbstractStage;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.scoring.proposers.PatternScoreProposer;
import com.bbn.akbc.neolearnit.scoring.proposers.Proposer;
import com.bbn.akbc.neolearnit.scoring.scorers.SimplifiedPatternScorer;

import java.util.Collection;

public class PatternProposer extends AbstractStage<PatternProposalInformation> {

  protected int amount;
  protected boolean initial;

  public PatternProposer(TargetAndScoreTables data) {
    super(data);
    this.amount = LearnItConfig.getInt("num_patterns_to_propose");
//		this.initial = data.getIteration() <= 1;
    this.initial = data.getIteration() <= 2;
  }

  public PatternProposer(TargetAndScoreTables data, boolean initializeSeedScores) {
    super(data);
    this.amount = LearnItConfig.getInt("num_patterns_to_propose");
    this.initial = initializeSeedScores && data.getIteration() <= 1;
  }

  public int getAmount() {
    return amount;
  }

  public void setAmount(int amount) {
    this.amount = amount;
  }

  public boolean isInitial() {
    return initial;
  }

  public void setInitial(boolean initial) {
    this.initial = initial;
  }

  @Override
  public Mappings applyStageMappingsFilter(Mappings mappings) {
    return mappings;
  }

  @Override
  public PatternProposalInformation processFilteredMappings(Mappings mappings) {

    double mappingTotalInstConf = Mappings.getConfidenceSum(mappings.getPatternInstances());
    Glogger.logger().debug("Total mapping instance confidence sum = " + mappingTotalInstConf);

    PatternProposalInformation.Builder builder = new PatternProposalInformation.Builder();
    for (LearnitPattern p : mappings.getAllPatterns().elementSet()) {
      double recallMax = Mappings.getConfidenceSum(mappings.getInstancesForPattern(p))/mappingTotalInstConf;
      if (p.isProposable(data.getTarget()) && !data.getPatternScores().isKnownFrozen(p) &&
	  recallMax >= LearnItConfig.getDouble("pattern_proposal_resolution"))
      {
	builder.withAddedPatternStats(p, mappings, data);
      }
      else {
	Glogger.logger().debug("processFilteredMappings: " + "low recall: " + recallMax + ", " + p.toIDString());
      }
    }
    //This is just so we can record frequency for seeds more accurately
    for (Seed s : mappings.getAllSeeds().elementSet()) {
      if (data.getSeedScores().isKnownFrozen(s)) {
	builder.withAddedSeedCount(s, mappings.getInstancesForSeed(s).size());
      }
      else
	Glogger.logger().debug("processFilteredMappings: not knownFrozen: " + s.toIDString());
    }
    builder.withCaclulatedTotalConfidence(mappings, data);
    return builder.build(LearnItConfig.getInt("num_patterns_to_propose")*10);
  }

  @Override
  public PatternProposalInformation reduceInformation(
      Collection<PatternProposalInformation> inputs) {

    PatternProposalInformation.Builder builder = new PatternProposalInformation.Builder();
    for (PatternProposalInformation info : inputs) {
      for (LearnitPattern pattern : info.getPatterns()) {
	if (info.getPartialInfo(pattern).confidenceEstimate() >= LearnItConfig.getDouble("pattern_proposal_confidence_threshold")) {
	  if (info.getRecall(pattern) >= LearnItConfig.getDouble("pattern_proposal_resolution")) {
	    builder.withMergedPatternStats(pattern, info.getPartialInfo(pattern));
	  } else if (info.getRecall(pattern) >= LearnItConfig.getDouble("pattern_proposal_resolution")/2) {
	    Glogger.logger().debug("throwing out " + pattern.toIDString()
		+ " because insufficient resolution of " + info
		.getRecall(pattern));
	  }
	  else {
	    Glogger.logger().debug("throwing out " + pattern.toIDString()
		+ " because low resolution of " + info
		.getRecall(pattern));
	  }
	}
	else
	  Glogger.logger().debug(
	      "low confidence: " + info.getPartialInfo(pattern).confidenceEstimate() + ", "
		  + pattern.toIDString());

      }
      for (Seed seed : info.getSeeds()) {
	builder.withAddedSeedCount(seed, info.getSeedCount(seed));
      }
      builder.withAddedTotalConfidence(info.getTotalConfidence());
      builder.withAddedTotalConfidenceDenominator(info.getTotalConfidenceDenominator());
    }
    return builder.build(LearnItConfig.getInt("num_patterns_to_propose")*10);
  }

  @Override
  public void runStage(PatternProposalInformation input) {
    if (initial) {
      Glogger.logger().debug("Initializing seed scores...");
      data.initializeSeedScores(input);
      //get the similarity scores ready for pattern pruner
      SeedSimilarity.runSaveKMeans(data);
    }

    data.setGoodSeedPrior(input.getTotalConfidence(), input.getTotalConfidenceDenominator());

    Glogger.logger().debug(
	"Proposing " + amount + " from potential set of " + input.getPatterns().size()
	    + " patterns...");

    SimplifiedPatternScorer scorer = new SimplifiedPatternScorer(data,input);
    scorer.score(input.getPatterns(), data.getPatternScores());

    Proposer<LearnitPattern> proposer = new PatternScoreProposer(data);

    for (LearnitPattern pattern : proposer.propose(input.getPatterns(), amount)) {

      data.getPatternScores().getScore(pattern).propose();
    }
  }

  @Override
  public Class<PatternProposalInformation> getInfoClass() {
    return PatternProposalInformation.class;
  }

}
