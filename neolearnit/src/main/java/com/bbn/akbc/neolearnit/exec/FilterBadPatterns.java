package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.neolearnit.mappings.filters.FrequencyLimitFilter;
import com.bbn.akbc.neolearnit.mappings.filters.MappingsFilter;
import com.bbn.akbc.neolearnit.mappings.filters.PatternConstraintsFilter;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;

public class FilterBadPatterns {

	public static void main(String[] args){
		try{
			trueMain(args);
		}catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static void trueMain(String[] args) throws IOException {
		String inputMappings = args[0];
		String outputMappings = args[1];


		System.out.println("Filter incomplete patterns...");
		MappingsFilter patternConstraintsFilter =
				new PatternConstraintsFilter(new HashSet<LearnitPattern>(),
						new HashSet<LearnitPattern>(),true);


		File mappingsJsonFile =  new File(inputMappings);
		System.out.println("Reading input mappings file...");
		Mappings mappings = Mappings.deserialize(mappingsJsonFile, true);

		System.out.println("Applying filter...");
		mappings = patternConstraintsFilter.makeFiltered(mappings);

		for(Seed seed : mappings.getAllSeeds()) {
			System.out.println("Seed:\t" + mappings.getInstancesForSeed(seed).size() + "\t" + seed.getSlot(0).asString() + "\t" + seed.getSlot(1).asString());
		}

		System.out.println("Writing output mappings file...");
		mappings.serialize(new File(outputMappings)	, true);
		System.out.println("Done! Output written to "+outputMappings);
	}

}
