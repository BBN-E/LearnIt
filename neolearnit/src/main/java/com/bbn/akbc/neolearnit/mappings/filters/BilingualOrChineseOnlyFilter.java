package com.bbn.akbc.neolearnit.mappings.filters;

import java.util.HashSet;
import java.util.Set;

import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;

public class BilingualOrChineseOnlyFilter implements MappingsFilter {

	/*
	boolean isBilingualOrChineseMonolingualString(String str) {
		for(int i=0; i<str.length(); i++) {
			char c = str.charAt(i);
			if((c>=0x4E00 && c<=0x9FFF) || (c>=0x3400 && c<=0x4DFF) ||
					(c>=0x20000 && c<=0x2A6DF) || (c>=0xF900 && c<=0xFAFF) ||
					(c>=0x2F800 && c<=0x2FA1F))
				return true;
		}

		return false;
	}
	*/

	public BilingualOrChineseOnlyFilter() {}


	@Override
	public Mappings makeFiltered(Mappings input) {

		Set<Seed> seeds = new HashSet<Seed>();
		Set<LearnitPattern> patterns = new HashSet<LearnitPattern>();

		System.out.println("=STATS: before: " + input.getInstance2Pattern().getAllPatterns().elementSet().size() + " patterns, " +
				input.getInstance2Seed().getAllSeeds().elementSet().size() + " seeds.");

		for (LearnitPattern pattern : input.getInstance2Pattern().getAllPatterns().elementSet()) {
			if(ChineseStrUtil.isBilingualOrChineseMonolingual(pattern))
				patterns.add(pattern);
			else
				System.out.println("=FILTERED: " + pattern.toIDString());
		}

		for(Seed seed : input.getInstance2Seed().getAllSeeds().elementSet()) {
			if(ChineseStrUtil.isBilingualOrChineseMonolingual(seed))
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
