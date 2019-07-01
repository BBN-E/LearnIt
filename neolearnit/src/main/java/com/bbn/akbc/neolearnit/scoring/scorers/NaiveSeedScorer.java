package com.bbn.akbc.neolearnit.scoring.scorers;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.resources.SeedGroups;
import com.bbn.akbc.neolearnit.common.resources.StopWords;
import com.bbn.akbc.neolearnit.logging.Glogger;
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

public class NaiveSeedScorer extends AbstractScorer<Seed,SeedScore> {

	private final TargetAndScoreTables scores;
	private final SeedPruningInformation pruningInfo;
	private final StopWords stopwords;

	private static int MIN_NUM_CAPTURED = 2;

	public NaiveSeedScorer(
			TargetAndScoreTables scores,
			SeedPruningInformation pruningInfo) {

		this.pruningInfo = pruningInfo;
		this.scores = scores;
        this.stopwords = StopWords.getFromParamsWithBackoff("overlap_seed_match_filter_word_list");
	}

        /*
         * set score to
         * - 1.0 if matched >= MIN_NUM_CAPTURED(currently 2) known frozen patterns
         * - 0.0 otherwise
         */
	protected Double calculateSeedScore(Seed seed) {
		int numCaptured=0;
		for (ObjectWithScore<LearnitPattern, PatternScore> scoredFeature :
					scores.getPatternScores().getFrozenObjectsWithScores()) {

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

			if (captured) numCaptured++;
		}

		if(numCaptured>=MIN_NUM_CAPTURED)
			return 1.0;
		else
			return 0.0;
	}

        /*
         * same as set_confidence in SimpleSeedScorer
         */
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
        Glogger.logger().debug("dbg: new seed score: " + seed.toIDString() + "\t" + score);

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
}
