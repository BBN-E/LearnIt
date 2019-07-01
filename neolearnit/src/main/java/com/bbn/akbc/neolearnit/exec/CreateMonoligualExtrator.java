package com.bbn.akbc.neolearnit.exec;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.mappings.filters.ChineseStrUtil;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;

public class CreateMonoligualExtrator {
	public static void main(String [] argv) throws IOException {
		String params = argv[0];
		String strInputJsonFile = argv[1];

		LearnItConfig.loadParams(new File(params));

		String strOutputJsonFile = strInputJsonFile + ".english";
		TargetAndScoreTables inputExtractor = TargetAndScoreTables.deserialize(new File(strInputJsonFile));

		TargetAndScoreTables outputExtractor = getExtractorWithOnlyEnglishPatterns(inputExtractor);

		outputExtractor.serialize(new File(strOutputJsonFile));

	}

	private static TargetAndScoreTables getExtractorWithOnlyEnglishPatterns(TargetAndScoreTables extractor) {
		Set<LearnitPattern> patternsToRemove = new HashSet<LearnitPattern>();
		Set<Seed> SeedToRemove = new HashSet<Seed>();

		for(LearnitPattern pattern : extractor.getPatternScores().keySet())
			if(ChineseStrUtil.isBilingualOrChineseMonolingual(pattern)) {
				System.out.println("REMOVE pattern: " + pattern.toIDString());
				patternsToRemove.add(pattern);
			}

		for(Seed seed : extractor.getSeedScores().keySet())
			if(ChineseStrUtil.isBilingualOrChineseMonolingual(seed)) {
				System.out.println("REMOVE seed: " + seed.toIDString());
				SeedToRemove.add(seed);
			}

		for(LearnitPattern pattern : patternsToRemove) {
			extractor.getPatternScores().removeItem(pattern);
		}
		for(Seed seed : SeedToRemove) {
			extractor.getSeedScores().removeItem(seed);
		}

		return extractor;
	}
}
