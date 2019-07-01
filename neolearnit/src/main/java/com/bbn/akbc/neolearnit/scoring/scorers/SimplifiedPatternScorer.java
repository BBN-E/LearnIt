package com.bbn.akbc.neolearnit.scoring.scorers;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.logging.Glogger;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.processing.PatternScoringInformation;
import com.bbn.akbc.neolearnit.processing.patternpruning.PatternPruningInformation.PatternPartialInfo;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.scoring.scores.PatternScore;
import com.bbn.akbc.neolearnit.scoring.tables.AbstractScoreTable;

/**
 * This scorer is (slightly) simplified so that it can be cheap and be done at proposal
 * for all patterns in the corpus
 *
 * @author mshafir
 *
 */
public class SimplifiedPatternScorer extends AbstractScorer<LearnitPattern,PatternScore> {
  private final TargetAndScoreTables scores;
  private final PatternScoringInformation scoringInfo;

  public SimplifiedPatternScorer(TargetAndScoreTables scores, PatternScoringInformation info) {

    this.scoringInfo = info;
    this.scores = scores;
  }

  protected double calculateConfidence(PatternPartialInfo info) {
    if (info.seedSample.size() < LearnItConfig.getInt("min_unique_seeds")) {
      //System.out.println("throwing out because unique seeds "+info.seedSample.size());
      return 0.0;
    } else {
      if (!LearnItConfig.optionalParamTrue("score_redundant_patterns") && info.totalInstanceCount == info.knownInstanceCount) {
	//provides nothing new, so we don't care if we're confident in it, but it's useless to accept (not always, though...)
	//System.out.println("throwing out because instance count");
	return 0.0;
      }

      if (info.confidenceDenominator == 0) return 0;

      return round(info.confidenceNumerator/info.confidenceDenominator);
    }
  }

  /*
   * set confidence = confidenceNumerator/confidenceDenominator
   */
  @Override
  public void score(LearnitPattern feature, AbstractScoreTable<LearnitPattern,PatternScore> table) {
    StringBuilder sb = new StringBuilder();
    sb.append("score: " + feature.toIDString());

    PatternPartialInfo info = scoringInfo.getPartialInfo(feature);
    PatternScore currentScore = table.getScore(feature);

    sb.append(", setFrozenPatternInstanceCount: " + info.frozenPatternInstanceCount);
    sb.append(", setFrequency: " + info.totalInstanceCount);
    sb.append(", setKnownFrequency: " + info.knownInstanceCount);
    sb.append(", setConfidenceNumerator: " + info.confidenceNumerator);
    sb.append(", setConfidenceDenominator: " + (info.confidenceDenominator == 0 ? 1.0D : info.confidenceDenominator));
    sb.append(", setConfidenceFromSimilarity: " + info.confidenceFromSimilarity);
    sb.append(", setConfidence: " + calculateConfidence(info));

    currentScore.setFrozenPatternInstanceCount(info.frozenPatternInstanceCount);
    currentScore.setFrequency(info.totalInstanceCount);
    currentScore.setKnownFrequency(info.knownInstanceCount);

    currentScore.setConfidenceNumerator(info.confidenceNumerator);
    currentScore.setConfidenceDenominator((info.confidenceDenominator == 0 ? 1.0D : info.confidenceDenominator));
    currentScore.setConfidenceFromSimilarity(info.confidenceFromSimilarity);
    currentScore.setConfidence(calculateConfidence(info));

    Glogger.logger().debug(sb.toString());
  }
}
