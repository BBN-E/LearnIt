package com.bbn.akbc.neolearnit.mappings.filters;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;

public class SubsamplingFilter implements MappingsFilter {

	private TargetAndScoreTables ex;
	private double subsampling_ratio = 1.0;
    private Random randomGenerator;

    private int MAX_INT = 1000;


	public SubsamplingFilter(TargetAndScoreTables ex,
			double subsampling_ratio) {
		this.ex = ex;
		this.subsampling_ratio = subsampling_ratio;
		this.randomGenerator = new Random();
	}

	private boolean tossedToSelect() {
		int randomInt = randomGenerator.nextInt(MAX_INT);

		if(randomInt<MAX_INT*subsampling_ratio)
			return true;
		else
			return false;
	}

	private boolean selected(LearnitPattern pattern) {
		if(ex.getPatternScores().isKnownFrozen(pattern))
			return true;

		return tossedToSelect();
	}

	private boolean selected(Seed seed) {
		if(ex.getSeedScores().isKnownFrozen(seed))
			return true;

		return tossedToSelect();
	}

	@Override
	public Mappings makeFiltered(Mappings input) {

		Set<Seed> seeds = new HashSet<Seed>();
		Set<LearnitPattern> patterns = new HashSet<LearnitPattern>();

		System.out.println("=STATS: before: " + input.getInstance2Pattern().getAllPatterns().elementSet().size() + " patterns, " +
				input.getInstance2Seed().getAllSeeds().elementSet().size() + " seeds.");

		for (LearnitPattern pattern : input.getInstance2Pattern().getAllPatterns().elementSet()) {
			if(selected(pattern))
				patterns.add(pattern);
			else
				System.out.println("=FILTERED: " + pattern.toIDString());
		}

		for(Seed seed : input.getInstance2Seed().getAllSeeds().elementSet()) {
			if(selected(seed))
				seeds.add(seed);
			else
				System.out.println("=FILTERED: " + seed.toIDString());
		}

		final Mappings resultedMapping = new SeedAndPatternMatchFilter(seeds, patterns).makeFiltered(input);

		System.out.println("=STATS: after: " + resultedMapping.getInstance2Pattern().getAllPatterns().elementSet().size() + " patterns, " +
				resultedMapping.getInstance2Seed().getAllSeeds().elementSet().size() + " seeds.");

		return resultedMapping;
	}
}
