package com.bbn.akbc.neolearnit.mappings.callables;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.mappings.filters.NormalizeSeedsFilter;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;

public class SeedInstanceMatchMappingExtractor extends AbstractMappingExtractorCallable<String> {

	private final Seed seed;

	public SeedInstanceMatchMappingExtractor(File file, TargetAndScoreTables data, Seed seed)
			throws FileNotFoundException {

		super(file, data);
		this.seed = seed;
	}

	@Override
	public Multiset<String> getInstances(Mappings m) throws IOException {
		Multiset<String> result = HashMultiset.<String>create();
		for (InstanceIdentifier id : m.getInstancesForSeed(seed)) {
//			try {
				result.add(id.reconstructMatchInfoDisplay(data.getTarget()).html());
		  /*
			} catch (IOException e) {
				e.printStackTrace();
			}
			*/
		}
		return result;
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
	public Set<LearnitPattern> getPatternsToRestrict(Mappings m) {
		Mappings normM = new NormalizeSeedsFilter(data.getTarget()).makeFiltered(m);
		return getCompleteSeedPatterns(normM).elementSet();
	}

}
