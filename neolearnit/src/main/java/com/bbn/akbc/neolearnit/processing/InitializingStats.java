package com.bbn.akbc.neolearnit.processing;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.processing.patternproposal.PatternProposalInformation;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.scoring.scores.PatternScore;
import com.bbn.akbc.neolearnit.scoring.tables.AbstractScoreTable.ObjectWithScore;

import java.util.Collection;

public class InitializingStats extends AbstractStage<PatternProposalInformation> {

//	protected int amount;
//	protected boolean initial;

	public InitializingStats(TargetAndScoreTables data) {
		super(data);
	}

	@Override
	public Mappings applyStageMappingsFilter(Mappings mappings) {
		return mappings;
	}

	@Override
	public PatternProposalInformation processFilteredMappings(Mappings mappings) {

		System.out.println("Summing instance confidences...");
		double mappingTotalInstConf = Mappings.getConfidenceSum(mappings.getPatternInstances());
		System.out.println("Total mapping instance confidence sum = "+mappingTotalInstConf);

		PatternProposalInformation.Builder builder = new PatternProposalInformation.Builder();
		for (LearnitPattern p : mappings.getAllPatterns().elementSet()) {
			double recallMax = Mappings.getConfidenceSum(mappings.getInstancesForPattern(p))/mappingTotalInstConf;
			if (p.isProposable(data.getTarget()) && !data.getPatternScores().isKnownFrozen(p) &&
                    recallMax >= LearnItConfig.getDouble("pattern_proposal_resolution"))
            {
				builder.withAddedPatternStats(p, mappings, data);
			}
		}
        //This is just so we can record frequency for seeds more accurately
		for (Seed s : mappings.getAllSeeds().elementSet()) {
			if (data.getSeedScores().isKnownFrozen(s)) {
				builder.withAddedSeedCount(s, mappings.getInstancesForSeed(s).size());
			}
		}
		builder.withCaclulatedTotalConfidence(mappings, data);
		return builder.build(LearnItConfig.getInt("num_patterns_to_propose")*10);
	}

	private void updateConfidenceDenominator(PatternProposalInformation info) {
		for (ObjectWithScore<LearnitPattern,PatternScore> scored : data.getPatternScores().getFrozenObjectsWithScores()) {
			if(info.hasPartialInfo(scored.getObject()))
				scored.getScore().addToConfidenceDenominator(info.getPartialInfo(scored.getObject()).confidenceDenominator);
		}
	}

	@Override
	public PatternProposalInformation reduceInformation(
			Collection<PatternProposalInformation> inputs) {

		PatternProposalInformation.Builder builder = new PatternProposalInformation.Builder();
		for (PatternProposalInformation info : inputs) {
			updateConfidenceDenominator(info);

			for (LearnitPattern pattern : info.getPatterns()) {
				if (info.getPartialInfo(pattern).confidenceEstimate() >= LearnItConfig.getDouble("pattern_proposal_confidence_threshold")) {
					if (info.getRecall(pattern) >= LearnItConfig.getDouble("pattern_proposal_resolution")) {
						builder.withMergedPatternStats(pattern, info.getPartialInfo(pattern));
					} else if (info.getRecall(pattern) >= LearnItConfig.getDouble("pattern_proposal_resolution")/2) {
						System.out.println("throwing out "+pattern.toIDString()+" because insufficient resolution of "+info.getRecall(pattern));
					}
				}
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
		data.setGoodSeedPrior(input.getTotalConfidence(), input.getTotalConfidenceDenominator());

//		for (ObjectWithScore<LearnitPattern,PatternScore> scored : data.getPatternScores().getFrozenObjectsWithScores()) {
//			scored.getScore().setDefaultTPFNStats();
//		}
	}

	@Override
	public Class<PatternProposalInformation> getInfoClass() {
		return PatternProposalInformation.class;
	}

}
