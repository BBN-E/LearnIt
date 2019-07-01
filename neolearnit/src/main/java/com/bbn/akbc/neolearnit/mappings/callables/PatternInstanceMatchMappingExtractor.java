package com.bbn.akbc.neolearnit.mappings.callables;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.ComboPattern;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;

public class PatternInstanceMatchMappingExtractor extends AbstractMappingExtractorCallable<String> {

	private final LearnitPattern pattern;

	public PatternInstanceMatchMappingExtractor(File file, TargetAndScoreTables data, LearnitPattern pattern)
			throws FileNotFoundException {

		super(file, data);
		this.pattern = pattern;
	}

	@Override
	public Multiset<String> getInstances(Mappings m) {
		Multiset<String> result = HashMultiset.<String>create();
		for (InstanceIdentifier id : m.getInstancesForPattern(pattern)) {
			// try {
				result.add(id.reconstructMatchInfoDisplay(data.getTarget()).html());
/*			} catch (IOException e) {
				e.printStackTrace();
			}
			*/
		}
		return result;
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
