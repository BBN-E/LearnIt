package com.bbn.akbc.neolearnit.mappings.callables;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;

import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.ComboPattern;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

public class SeedMatchMappingExtractor extends AbstractMappingExtractorCallable<Seed> {

	private final LearnitPattern pattern;

	public SeedMatchMappingExtractor(File file, TargetAndScoreTables data, LearnitPattern pattern)
			throws FileNotFoundException {

		super(file, data);
		this.pattern = pattern;
	}

	@Override
	public Multiset<Seed> getInstances(Mappings m) {
		return m.getSeedsForPattern(pattern);
	}

	@Override
	public Set<LearnitPattern> getPatternsToRestrict(Mappings m) {
		if (pattern instanceof ComboPattern) {
			ComboPattern cp = (ComboPattern)pattern;
			return Sets.newHashSet(cp.getRootPattern());
		} else {
			return new HashSet<LearnitPattern>();
		}
	}

}
