package com.bbn.akbc.neolearnit.mappings.callables;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Set;

import com.bbn.akbc.neolearnit.mappings.filters.NormalizeSeedsFilter;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

public class PatternMatchMappingExtractor extends AbstractMappingExtractorCallable<LearnitPattern> {

	private final Seed seed;

	public PatternMatchMappingExtractor(File file, TargetAndScoreTables data, Seed seed)
			throws FileNotFoundException {

		super(file, data);
		this.seed = seed;
	}

	private Multiset<LearnitPattern> getCompleteSeedPatterns(Mappings m) {
		Multiset<LearnitPattern> result = HashMultiset.<LearnitPattern>create();
		for (LearnitPattern pattern : m.getPatternsForSeed(seed)) {
			if (pattern.isCompletePattern()) {
				result.add(pattern);
			};
		}
		return result;
	}

	@Override
	public Multiset<LearnitPattern> getInstances(Mappings m) {
		return getCompleteSeedPatterns(m);
	}

	@Override
	public Set<LearnitPattern> getPatternsToRestrict(Mappings m) {
		Mappings normM = new NormalizeSeedsFilter(data.getTarget()).makeFiltered(m);
		return getCompleteSeedPatterns(normM).elementSet();
	}

}
