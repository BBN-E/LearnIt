package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.common.targets.TargetFactory;
import com.bbn.akbc.neolearnit.loaders.LoaderUtils;
import com.bbn.akbc.neolearnit.loaders.impl.BilingualDocTheoryInstanceLoader;
import com.bbn.akbc.neolearnit.loaders.impl.MonolingualDocTheoryInstanceLoader;
import com.bbn.akbc.neolearnit.mappings.filters.FrequencyLimitFilter;
import com.bbn.akbc.neolearnit.mappings.filters.RelevantInstancesFilter;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToMatchInfoDisplayMap;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToSeedMapping;
import com.bbn.akbc.neolearnit.modules.BilingualExtractionModule;
import com.bbn.akbc.neolearnit.modules.MonolingualExtractionModule;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.storage.StorageUtils;

import java.io.File;
import java.io.IOException;

public class InstanceExtractor {

	static boolean printMappingsForDebug = false;

	public static void main(String[] args){
		try{
			trueMain(args);
		}catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static void trueMain(String[] args) throws IOException {
		String paramsFile = args[0];
		String relation = args[1];
		String doclist = args[2];
		String output = args[3];
		boolean matchDisplay = (args.length > 4 && args[4].equals("--store-display"));

		LearnItConfig.loadParams(new File(paramsFile));

		Target target = TargetFactory.fromString(relation);
		//Target target = TargetFactory.makeEverythingTarget();

		Mappings info;
		InstanceToMatchInfoDisplayMap display;

		if (LearnItConfig.optionalParamTrue("bilingual")) {

			BilingualExtractionModule module = new BilingualExtractionModule(matchDisplay);
			BilingualDocTheoryInstanceLoader docTheoryLoader = module.getDocTheoryLoader(target);

			LoaderUtils.loadRegularBilingualFileList(new File(doclist), docTheoryLoader);
			//get our mappings out of the module (MAGIC!)
			info = module.getInformationForScoring();
			display = module.getMatchInfoDisplayMap();

		} else {

			MonolingualExtractionModule module = new MonolingualExtractionModule(matchDisplay);
			MonolingualDocTheoryInstanceLoader docTheoryLoader = module.getDocTheoryLoader(target);

			LoaderUtils.loadFileList(new File(doclist), docTheoryLoader);
			//get our mappings out of the module (MAGIC!)
			info = module.getInformationForScoring();
			display = module.getMatchInfoDisplayMap();
		}

        if (!LearnItConfig.optionalParamTrue("running_test_set"))
		    info = new RelevantInstancesFilter().makeFiltered(info);

		InstanceToPatternMapping i2p = info.getInstance2Pattern();
		InstanceToSeedMapping i2s = info.getInstance2Seed();

		if (LearnItConfig.optionalParamTrue("debug_extraction")) {
			for (InstanceIdentifier id : i2p.getAllInstances().elementSet()) {
				System.out.println(id.toShortString());
				for (LearnitPattern pat : i2p.getPatterns(id)) {
					System.out.println("\t" + pat.toPrettyString());
				}
				for (Seed seed : i2s.getSeeds(id)) {
					System.out.println("\t" + seed.toPrettyString());
					System.out.println("\t" + id.getSlotEntityType(1));
				}
				System.out.println();
			}
			for (Seed s : i2s.getAllSeeds().elementSet()) {
				System.out.println(s);
			}
		}

		Integer minSeedFrequency = LearnItConfig.params().
				getOptionalInteger("min_seed_frequency_per_batch").or(-1);
		Integer minPatternFrequency = LearnItConfig.params().
				getOptionalInteger("min_pattern_frequency_per_batch").or(-1);
		if(minSeedFrequency!=-1 || minPatternFrequency!=-1) {
			FrequencyLimitFilter frequencyLimitFilter = new FrequencyLimitFilter(minSeedFrequency, -1, minPatternFrequency, -1);
			info = frequencyLimitFilter.makeFiltered(info);
		}

		if (matchDisplay) {
			display = display.makeFitlered(info.getInstance2Pattern().getAllInstances().elementSet());
			StorageUtils.serialize(new File(output), display, true);
		} else {

			info.serialize(new File(output),true);
		}

		if(printMappingsForDebug) {
			for(Seed seed : info.getAllSeeds()) {
				System.out.println("SEED	:\t" + seed.toIDString());
				for(LearnitPattern learnitPattern : info.getPatternsForSeed(seed)) {
					System.out.println("SEED_PATTERN_PAIR	:\t" + seed.toIDString() + "\t" + learnitPattern.toIDString());
				}
			}
		}
	}

}
