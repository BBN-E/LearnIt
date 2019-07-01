package com.bbn.akbc.neolearnit.processing.seedproposal;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.resources.SeedGroups;
import com.bbn.akbc.neolearnit.common.resources.SeedSimilarity;
import com.bbn.akbc.neolearnit.common.resources.StopWords;
import com.bbn.akbc.neolearnit.logging.Glogger;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.processing.AbstractStage;
import com.bbn.akbc.neolearnit.processing.seedpruning.SeedPruningInformation;
import com.bbn.akbc.neolearnit.processing.seedpruning.SeedPruningInformation.SeedPartialInfo;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.scoring.proposers.Proposer;
import com.bbn.akbc.neolearnit.scoring.proposers.SeedScoreProposer;
import com.bbn.akbc.neolearnit.scoring.scorers.SimplifiedSeedScorer;

import java.util.Collection;

public class SeedProposer extends AbstractStage<SeedProposalInformation> {

  protected int amount;
  private final StopWords stopwords;

  public SeedProposer(TargetAndScoreTables data) {
    super(data);
    this.amount = LearnItConfig.getInt("num_seeds_to_propose");
    this.stopwords = StopWords.getFromParamsWithBackoff("overlap_seed_match_filter_word_list");
  }

  public int getAmount() {
    return amount;
  }

  public void setAmount(int amount) {
    this.amount = amount;
  }

  @Override
  public Mappings applyStageMappingsFilter(Mappings mappings) {
    return mappings;
  }

  public boolean hasKnownPattern(Seed seed, Mappings mappings) {
    for (LearnitPattern pattern : mappings.getPatternsForSeed(seed)) {
      if (data.getPatternScores().isKnownFrozen(pattern)) {
	return true;
      }
    }
    return false;
  }

  @Override
  public SeedProposalInformation processFilteredMappings(Mappings mappings) {

    double mappingTotalInstConf = Mappings.getConfidenceSum(mappings.getSeedInstances());

    SeedProposalInformation.Builder builder = new SeedProposalInformation.Builder();
    for (Seed s : mappings.getAllSeeds().elementSet()) {
//			System.out.println("dbg:\t" + SeedSimilarity.seedHasScore(s.withProperText(data.getTarget())) + "\t" + s.toIDString());
      if (SeedSimilarity.seedHasScore(s.withProperText(data.getTarget()))) {

	double recallMax = Mappings.getConfidenceSum(mappings.getInstancesForSeed(s))/mappingTotalInstConf;
	if (!data.getSeedScores().isKnownFrozen(s) && hasKnownPattern(s,mappings)
	    && recallMax >= LearnItConfig.getDouble("seed_proposal_resolution"))
	{
	  SeedPartialInfo score = SeedPruningInformation.SeedPartialInfo.calculateInfo(s, mappings, data);

	  Glogger.logger().debug(
	      "SeedProposer:\t" + data.getSeedScores()
		  .isKnownFrozen(s) + "\t"
		  + hasKnownPattern(s, mappings) + "\t" + recallMax
		  + "\t"
		  + data.getIteration() + "\t"
		  + score.knownInstanceCount + "\t"
		  + score.totalInstanceCount);

//					if (score.knownInstanceCount > 0 && (data.getIteration() == 0 || score.knownInstanceCount != score.totalInstanceCount)) {
	  if (score.knownInstanceCount > 0 && (data.getIteration() <= 1 || score.knownInstanceCount != score.totalInstanceCount)) {

//					if (score.knownInstanceCount > 0 && (data.getIteration() == 0)) {
	    score.sources.clear();
	    if (LearnItConfig.optionalParamTrue("use_seed_groups")) {
	      for (Seed seed : SeedGroups.getGroupOrSingleton(s.getReducedForm(stopwords), s))
		builder.withScore(seed, score);
	    } else {
	      builder.withScore(s, score);
	    }
	  }
	}
	else {
	  Glogger.logger().debug("not include seed: " + s.toIDString());
	}
      }
    }
    Glogger.logger().debug(builder.build().getSeeds().size() + " possible seeds!");

    return builder.build();
  }

  @Override
  public SeedProposalInformation reduceInformation(Collection<SeedProposalInformation> inputs) {
    int count = 0;
    SeedProposalInformation.Builder builder = new SeedProposalInformation.Builder();
    for (SeedProposalInformation info : inputs) {
      for (Seed seed : info.getSeeds()) {
	if (info.getPartialConfidence(seed) >= LearnItConfig.getDouble("seed_proposal_confidence_threshold")) {
	  if (info.getRecall(seed,data) >= LearnItConfig.getDouble("seed_proposal_resolution")) {
	    count++;
	    builder.withMergedScore(seed, info.getScore(seed));
	  } else if (info.getRecall(seed,data) >= LearnItConfig.getDouble("seed_proposal_resolution")/2) {
	    Glogger.logger().debug(
		"throwing out " + seed.toIDString()
		    + " because insufficient resolution of "
		    + info.getRecall(seed, data));
	  }
	  else {
	    Glogger.logger().debug(
		"throwing out " + seed.toIDString()
		    + " because low resolution of "
		    + info.getRecall(seed, data));
	  }
	}
	else {
	  Glogger.logger().debug("lower than seed_proposal_confidence_threshold: " + seed
	      .toIDString());
	}
      }
    }
    Glogger.logger().debug(count + " possible seeds!");
    return builder.build();
  }

  @Override
  public void runStage(SeedProposalInformation input) {
    Glogger.logger().debug(
	"Proposing " + amount + " from potential set of " + input.getSeeds().size()
	    + " seeds...");

    SimplifiedSeedScorer scorer = new SimplifiedSeedScorer(input);
    scorer.score(input.getSeeds(), data.getSeedScores());

    Proposer<Seed> proposer = new SeedScoreProposer(data);

    for (Seed seed : proposer.propose(input.getSeeds(), amount)) {

      data.getSeedScores().getScore(seed).propose();
    }
  }

  @Override
  public Class<SeedProposalInformation> getInfoClass() {
    return SeedProposalInformation.class;
  }

}
