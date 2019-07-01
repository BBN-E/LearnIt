package com.bbn.akbc.neolearnit.processing.seedpruning;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.resources.SeedSimilarity;
import com.bbn.akbc.neolearnit.logging.Glogger;
import com.bbn.akbc.neolearnit.mappings.filters.SeedMatchFilter;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.processing.AbstractStage;
import com.bbn.akbc.neolearnit.processing.seedpruning.SeedPruningInformation.SeedPartialInfo;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.scoring.scorers.NaiveSeedScorer;
import com.bbn.akbc.neolearnit.scoring.selectors.SeedSelector;

import java.util.Collection;
import java.util.Set;

public class SeedPruner extends AbstractStage<SeedPruningInformation> {

	protected int numberToAccept;

	public SeedPruner(TargetAndScoreTables data) {
		super(data);
		this.numberToAccept = LearnItConfig.getInt("max_num_seeds_to_freeze");
	}

	public int getNumberToAccept() {
		return numberToAccept;
	}

	public void setNumberToAccept(int amount) {
		this.numberToAccept = amount;
	}

	@Override
	public Mappings applyStageMappingsFilter(Mappings mappings) {
		return new SeedMatchFilter(data.getSeedScores().getNonFrozen()).makeFiltered(mappings);
	}

	@Override
	public SeedPruningInformation processFilteredMappings(Mappings mappings) {
//		SeedSimilarity.load(data);

		SeedPruningInformation result = new SeedPruningInformation();
		for (Seed s : mappings.getAllSeeds().elementSet()) {
			result.recordPartialInfo(s, SeedPartialInfo.calculateInfo(s,mappings,data));
		}

		/*
        // set good seed prior & tp/tn/fp/fn
		double totalConfidence=0, totalConfidenceDenominator=0;
		for (InstanceIdentifier id : mappings.getInstance2Seed().getAllInstances().elementSet()) {
			totalConfidence += PatternPruningInformation.PatternPartialInfo.instanceScore(id, mappings, data).value*id.getConfidence();
			totalConfidenceDenominator += id.getConfidence();
		}
		// recalculate all TPFN stats
		for (ObjectWithScore<LearnitPattern,PatternScore> scored : data.getPatternScores().getFrozenObjectsWithScores()) {
			scored.getScore().calculateTPFNStats(data.getGoodSeedPrior(), totalConfidenceDenominator);
//			System.out.println("pattern: " + scored.getObject().toIDString() + ", "
//					+ scored.getScore().toString());
		}
		//
		*/

		return result;
	}

	@Override
	public SeedPruningInformation reduceInformation(Collection<SeedPruningInformation> inputs) {

		SeedPruningInformation result = new SeedPruningInformation();
		for (SeedPruningInformation input : inputs) {
			for (Seed seed : input.getSeeds()) {
				result.getPartialScore(seed).mergeIn(input.getPartialScore(seed));
			}
		}
		return result;
	}

	public void score(SeedPruningInformation info) {
//		SeedScorer scorer = new SeedScorer(data, info);
		NaiveSeedScorer scorer = new NaiveSeedScorer(data, info);
		scorer.score(info.getSeedsToScore(), data.getSeedScores());
	}

	public Set<Seed> select(int amount) {
		SeedSelector selector = new SeedSelector(amount);
		if(data.getIteration() <= 1)
			selector.setDoNotCheckForNewFact(true);

		return selector.freezeScores(data.getSeedScores());
	}

	@Override
	public void runStage(SeedPruningInformation input) {
		score(input);
		data.getSeedScores().removeProposed();
		select(this.numberToAccept);

		SeedSimilarity.updateKMeans(data);

		Glogger.logger().debug(data.getSeedScores().toFrozenString());
	}

	@Override
	public Class<SeedPruningInformation> getInfoClass() {
		return SeedPruningInformation.class;
	}

}
