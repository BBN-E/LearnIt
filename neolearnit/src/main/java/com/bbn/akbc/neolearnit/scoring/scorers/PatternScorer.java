package com.bbn.akbc.neolearnit.scoring.scorers;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.logging.Glogger;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.processing.PatternScoringInformation;
import com.bbn.akbc.neolearnit.processing.patternpruning.PatternPruningInformation.PatternPartialInfo;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.scoring.scores.PatternScore;
import com.bbn.akbc.neolearnit.scoring.tables.AbstractScoreTable;

public class PatternScorer extends AbstractScorer<LearnitPattern,PatternScore> {

	private final TargetAndScoreTables scores;
	private final PatternScoringInformation pruningInfo;

	public PatternScorer(TargetAndScoreTables scores, PatternScoringInformation info) {

		this.pruningInfo = info;
		this.scores = scores;
	}

  	/*
  	 * prec(pattern) = sum_of(scores(seeds))/count(seeds)
  	 */
	protected double calculatePrecision(PatternPartialInfo info) {
		return round(info.seedScoreSum(scores.getSeedScores())/info.sources.elementSet().size());
	}

  	/*
  	 * recall(pattern) = count(seeds)/count_of_all_frozen(seeds)
  	 */
	protected double calculateRecall(PatternPartialInfo info) {
		if (scores.getSeedScores().getFrozen().size() == 0) return 0.0;

		return round((double)info.sources.elementSet().size()/scores.getSeedScores().getFrozen().size());
	}

	/*
	 * confidence(pattern) = confidenceNumerator/confidenceDenominator
	 */
	protected double calculateConfidence(PatternPartialInfo info) {
		if (info.seedSample.size() < LearnItConfig.getInt("min_unique_seeds")) {
		  Glogger.logger().debug(
		      "throwing out because unique seeds " + info.seedSample.size());
			return 0.0;
		} else {
			if (info.totalInstanceCount == info.knownInstanceCount) {
				//provides nothing new, so we don't care if we're confident in it, but it's useless to accept
			  Glogger.logger().debug("throwing out because instance count");
				return 0.0;
			}

			if (info.confidenceDenominator == 0) return 0;

			//double seedConfidence = info.knownSeeds.size()/info.totalSeeds.size();
			//return (instanceConfidence+seedConfidence)/2.0;
			return round(info.confidenceNumerator/info.confidenceDenominator);
		}
	}

	@Override
	public void score(LearnitPattern feature, AbstractScoreTable<LearnitPattern,PatternScore> table) {
		PatternPartialInfo info = pruningInfo.getPartialInfo(feature);
        PatternScore currentScore = table.getScore(feature);

		currentScore.setSources(scores.getSeedScores().translateIndexMultiset(info.sources));

		currentScore.setFrozenPatternInstanceCount(info.frozenPatternInstanceCount);
		currentScore.setFrequency(info.totalInstanceCount);
		currentScore.setKnownFrequency(info.knownInstanceCount);
		//currentScore.setSeedFrequency(info.totalSeeds.size());
		currentScore.setKnownSeedFrequency(info.sources.elementSet().size());

		currentScore.setPrecision(calculatePrecision(info));
		currentScore.setRecall(calculateRecall(info));
		currentScore.setConfidenceNumerator(info.confidenceNumerator);
		currentScore.setConfidenceDenominator(
		    (info.confidenceDenominator == 0 ? 1.0D : info.confidenceDenominator));
		currentScore.setConfidenceFromSimilarity(info.confidenceFromSimilarity);
		currentScore.setConfidence(calculateConfidence(info));
		currentScore.calculateTPFNStats(scores.getGoodSeedPrior(),
		    scores.getTotalInstanceDenominator());

	  Glogger.logger().debug("PatternScorer: p=" + feature.toIDString() + ", "
	      + "setFrozenPatternInstanceCount=" + info.frozenPatternInstanceCount + ", "
	      + "setFrequency=" + info.totalInstanceCount + ", "
	      + "setKnownSeedFrequency=" + info.sources.elementSet().size() + ", "
	      + "setPrecision=" + calculatePrecision(info) + ", "
	      + "setRecall=" + calculateRecall(info) + ", "
	      + "setConfidenceNumerator=" + info.confidenceNumerator + ", "
	      + "setConfidenceFromSimilarity=" + info.confidenceFromSimilarity + ", "
	      + "setConfidence=" + calculateConfidence(info)
	  );

//        if (feature instanceof PropPattern && ((PropPattern)feature).getPredicateType() == Proposition.PredicateType.SET) {
//        if (feature.getLexicalItems().isEmpty()) {
//            System.out.println(feature.toIDString());
//            System.out.println("\t" + info.confidenceDenominator);
//            System.out.println("\t" + scores.getTotalInstanceDenominator());
//            System.out.println("\t" + info.totalInstanceCount);
//            System.out.println("\t" + info.knownInstanceCount);
//        }
	}

}
