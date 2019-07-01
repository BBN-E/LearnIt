package com.bbn.akbc.neolearnit.scoring.scorers;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.resources.SeedGroups;
import com.bbn.akbc.neolearnit.common.resources.StopWords;
import com.bbn.akbc.neolearnit.logging.Glogger;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.processing.seedproposal.SeedProposalInformation;
import com.bbn.akbc.neolearnit.processing.seedpruning.SeedPruningInformation.SeedPartialInfo;
import com.bbn.akbc.neolearnit.scoring.scores.SeedScore;
import com.bbn.akbc.neolearnit.scoring.tables.AbstractScoreTable;
import com.google.common.collect.ImmutableSet;

import java.util.Collection;

public class SimplifiedSeedScorer extends AbstractScorer<Seed,SeedScore> {

  private final SeedProposalInformation proposalInfo;
  private final StopWords stopwords;

  public SimplifiedSeedScorer(SeedProposalInformation proposalInfo) {
    this.proposalInfo = proposalInfo;
    this.stopwords = StopWords.getFromParamsWithBackoff("overlap_seed_match_filter_word_list");
  }

  protected double getConfidence(SeedPartialInfo info) {
    if (info.totalInstanceCount == 0) return 0;
    if (info.knownInstanceCount == 0) return 0;

//		if (info.totalInstanceCount == info.knownInstanceCount) return 0;

    if (info.confidenceDenominator == 0) return 0;

    return round(info.confidenceNumerator/info.confidenceDenominator);
  }


  @Override
  public void score(Seed seed, AbstractScoreTable<Seed,SeedScore> table) {
    //System.out.println("Scoring "+seed);

    Collection<Seed> group = LearnItConfig.optionalParamTrue("use_seed_groups") ? SeedGroups.getGroupOrSingleton(seed.getReducedForm(stopwords),seed) : ImmutableSet.of(seed);

    for (Seed s : group) {
      SeedPartialInfo info = proposalInfo.getScore(s);
      SeedScore currentScore = table.getScoreOrDefault(s);

      currentScore.setFrequency(info.totalInstanceCount);
      currentScore.setKnownFrequency(info.knownInstanceCount);
      currentScore.setConfidence(getConfidence(info));
      currentScore.setConfidenceNumerator(info.confidenceNumerator);
      currentScore.setConfidenceDenominator(
	  (info.confidenceDenominator == 0 ? 1.0D : info.confidenceDenominator));

      Glogger.logger().debug("SimplifiedSeedScorer: seed=" + s.toIDString() + ", "
	      + "setFrequency" + info.totalInstanceCount + ", "
	      + "setKnownFrequency=" + info.knownInstanceCount + ", "
	      + "setConfidence=" + getConfidence(info) + ", "
	      + "setConfidenceNumerator=" + info.confidenceNumerator + ", "
	      + "setConfidenceDenominator=" + (info.confidenceDenominator == 0 ? 1.0D : info.confidenceDenominator)
      );
    }

    //System.out.println(currentScore);
  }

}
