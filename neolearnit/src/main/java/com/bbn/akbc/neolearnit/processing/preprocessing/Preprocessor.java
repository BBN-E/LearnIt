package com.bbn.akbc.neolearnit.processing.preprocessing;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.resources.SeedGroups;
import com.bbn.akbc.neolearnit.common.resources.SeedSimilarity;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.processing.AbstractStage;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.google.common.collect.Multiset;

import java.util.Collection;

public class Preprocessor extends AbstractStage<PreprocessingInformation> {

	public Preprocessor(TargetAndScoreTables data) {
		super(data);
	}

	@Override
	public PreprocessingInformation reduceInformation(Collection<PreprocessingInformation> inputs) {
		PreprocessingInformation.Builder builder = new PreprocessingInformation.Builder();
		for (PreprocessingInformation info : inputs) {
			builder.withInfo(info);
		}

		return builder.build();
	}

	@Override
	public void runStage(PreprocessingInformation input) {
		PreprocessingInformation.Builder builder = new PreprocessingInformation.Builder();
		builder.withInfo(input);

        builder.keepNRandomSeeds(200000);
        builder.removeLowCountSeeds(1);

		try {
			SeedSimilarity.save(data, builder.build());
            if (LearnItConfig.optionalParamTrue("use_seed_groups"))
                SeedGroups.save(data, builder.build());
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to save preprocessing output.");
		}
	}

	@Override
	public PreprocessingInformation processFilteredMappings(Mappings mappings) {
		PreprocessingInformation.Builder builder = new PreprocessingInformation.Builder();

        System.out.println("Adding pattern info for " + mappings.getAllSeeds().elementSet().size() + " seeds...");
		for (Seed seed : mappings.getAllSeeds().elementSet()) {

            if (LearnItConfig.optionalParamTrue("use_seed_groups"))
                builder.withSeed(seed);

			Multiset<LearnitPattern> seedPatterns = mappings.getPatternsForSeed(seed);
			for (LearnitPattern p : seedPatterns) {
				builder.withSeedPattern(seed, p.toIDString());
			}
		}

        builder.keepNRandomSeeds(2000);
//        builder.removeLowCountSeeds(1);

		return builder.build();
	}

	@Override
	public Mappings applyStageMappingsFilter(Mappings mappings) {
		return mappings;
	}

	@Override
	public Class<PreprocessingInformation> getInfoClass() {
		return PreprocessingInformation.class;
	}

}
