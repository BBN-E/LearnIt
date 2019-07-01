package com.bbn.akbc.neolearnit.scoring.scorers;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.resources.SeedGroups;
import com.bbn.akbc.neolearnit.common.resources.StopWords;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.processing.seedpruning.SeedPruningInformation;
import com.bbn.akbc.neolearnit.processing.seedpruning.SeedPruningInformation.SeedPartialInfo;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.scoring.scores.PatternScore;
import com.bbn.akbc.neolearnit.scoring.scores.SeedScore;
import com.bbn.akbc.neolearnit.scoring.tables.AbstractScoreTable;
import com.bbn.akbc.neolearnit.scoring.tables.AbstractScoreTable.ObjectWithScore;
import com.google.common.collect.ImmutableSet;

import java.util.Collection;

public class SeedScorer extends AbstractScorer<Seed,SeedScore> {

	private final TargetAndScoreTables scores;
	private final SeedPruningInformation pruningInfo;
	private final Double goodSeedPrior;
    private final StopWords stopwords;

	public SeedScorer(
			TargetAndScoreTables scores,
			SeedPruningInformation pruningInfo) {

		this.pruningInfo = pruningInfo;
		this.scores = scores;
		this.goodSeedPrior = scores.getGoodSeedPrior();
        this.stopwords = StopWords.getFromParamsWithBackoff("overlap_seed_match_filter_word_list");
	}

	protected boolean hasOverlap(Collection<InstanceIdentifier> seedInstances,
			Collection<InstanceIdentifier> featureInstances) {

		for (InstanceIdentifier id : featureInstances) {
			if (seedInstances.contains(id)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Estimate the score of the given seed.  We basically use a
        Naive Bayes approach, with each pattern as a feature.  In
        particular, the score of a seed is given by::

                                P(good,insts)
          score(seed) = ------------------------------
                         P(good,insts) + P(bad,insts)
                                    __                  __
          P(good,insts) = P(good) * || P(+match|good) * || P(-match|good)
                                   matching          nonmatching
                                   patterns           patterns
                                  __                 __
          P(bad,insts) = P(bad) * || P(+match|bad) * || P(-match|bad)
                                 matching          nonmatching
                                 patterns           patterns

	 * @return
	 */
	protected Double calculateSeedScore(Seed seed) {

		double p_good = Math.log(goodSeedPrior);
		double p_bad = Math.log(1.0 - goodSeedPrior);

		for (ObjectWithScore<LearnitPattern, PatternScore> scoredFeature :
					scores.getPatternScores().getFrozenObjectsWithScores()) {

			//if(scoredFeature.getScore().getPrecision()<0.01 && scoredFeature.getScore().getRecall()<0.01)
			//	continue;

			double tp = scoredFeature.getScore().getTP();
			double tn = scoredFeature.getScore().getTN();
			double fp = scoredFeature.getScore().getFP();
			double fn = scoredFeature.getScore().getFN();

			double p_match_given_good = Math.max(tp/(tp+fn),0.00001);
			double p_nomatch_given_good = Math.max(fn/(tp+fn),0.00001);
			double p_match_given_bad = Math.max(fp/(fp+tn),0.00001);

			// good seed prior is currently too high, which will penalize p_nomatch_given_bad (with a smaller tn).
			// this penalization will slowly accumulated to a really high level if we have lots of patterns, e.g., use all patterns for pruning seeds
			double p_nomatch_given_bad = Math.max(tn/(fp+tn),0.00001);

			boolean captured = false;
            if (LearnItConfig.optionalParamTrue("use_seed_groups")) {
                Collection<Seed> group = SeedGroups.getGroupOrSingleton(seed.getReducedForm(stopwords), seed);
                for (Seed groupSeed : group) {
                    if (pruningInfo.getPartialScore(groupSeed).sources.contains(scores.getPatternScores().getItemIndex(scoredFeature.getObject()))) {
                        captured = true;
                        break;
                    }
                }
            } else {
                captured = pruningInfo.getPartialScore(seed).sources.contains(scores.getPatternScores().getItemIndex(scoredFeature.getObject()));
            }

			if (captured) {
				p_good += Math.log(p_match_given_good);
				p_bad += Math.log(p_match_given_bad);
			} else {
				p_good += Math.log(p_nomatch_given_good);
				p_bad += Math.log(p_nomatch_given_bad);
			}

			if (Double.isNaN(p_good) || Double.isNaN(p_bad)) {
				throw new RuntimeException("Seed "+seed+" has NaN score. pgood="+p_good+", pbad="+p_bad+" "+scoredFeature.toString()+" "+tp+","+tn+","+fp+","+fn);
			}

		}
		Double result = Math.pow(Math.E, p_good - addLogs(p_good,p_bad));

		//round it so we don't have floating point inconsistencies
		result = round(result);

		if (result.isNaN()) {
			throw new RuntimeException("Seed "+seed+" has NaN score. pgood="+p_good+", pbad="+p_bad);
		}

		return result;
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

        Collection<Seed> group;
        if (LearnItConfig.optionalParamTrue("use_seed_groups")) {
            group = SeedGroups.getGroupOrSingleton(seed.getReducedForm(StopWords.getFromParamsWithBackoff("overlap_seed_match_filter_word_list")), seed);
        } else {
            group = ImmutableSet.of(seed);
        }

        double score = calculateSeedScore(seed);

        for (Seed s : group) {
            SeedScore currentScore = table.getScoreOrDefault(s);
            SeedPartialInfo info = pruningInfo.getPartialScore(s);

            currentScore.setFrequency(info.totalInstanceCount);
            currentScore.setKnownFrequency(info.knownInstanceCount);
            currentScore.setSources(scores.getPatternScores().translateIndexMultiset(info.sources));
            currentScore.setConfidence(getConfidence(info));
            currentScore.setConfidenceNumerator(info.confidenceNumerator);
            currentScore.setConfidenceDenominator((info.confidenceDenominator == 0 ? 1.0D : info.confidenceDenominator));
            currentScore.setScore(score);
        }

		//System.out.println(currentScore);
	}

	private static final double ADD_LOGS_MAX_DIFF = Math.log(1e-30)/Math.log(2.0);

	/**
	 * Given two numbers logx=log(x) and logy=log(y), return log(x+y).
	 * Conceptually, this is the same as returning
	 * log(2**(logx)+2**(logy)), but the actual implementation avoids
	 * overflow errors that could result from direct computation.
	 * @param logx
	 * @param logy
	 * @return
	 */
	private static double addLogs(double logx, double logy) {
		if (logx < logy + ADD_LOGS_MAX_DIFF)
	        return logy;

	    if (logy < logx + ADD_LOGS_MAX_DIFF)
	        return logx;

	    double base = Math.min(logx, logy);
	    // return base + Math.log(Math.pow(2.0,logx-base) + Math.pow(2.0,logy-base))/Math.log(2.0);
	    return base + Math.log(Math.pow(Math.E,logx-base) + Math.pow(Math.E,logy-base));
	}

}
