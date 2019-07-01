package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.neolearnit.mappings.filters.FrequencyLimitFilter;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;


import java.io.*;

public class FilteredMappingCreatorByFreq {

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
		int minSeedFrequency = Integer.parseInt(args[1]);
		int minPatternFrequency = Integer.parseInt(args[2]);
		int maxSeedFrequency = Integer.parseInt(args[3]);
		int maxPatternFrequency = Integer.parseInt(args[4]);
		String outputMappings = args[5];


		FrequencyLimitFilter frequencyLimitFilter = new FrequencyLimitFilter(minSeedFrequency,maxSeedFrequency,
				minPatternFrequency,maxPatternFrequency);
		File mappingsJsonFile =  new File(inputMappings);
		System.out.println("Reading input mappings file...");
		Mappings mappings = Mappings.deserialize(mappingsJsonFile, true);
		System.out.println("Applying filter...");
		mappings = frequencyLimitFilter.makeFiltered(mappings);
		System.out.println("Writing output mappings file...");
		mappings.serialize(new File(outputMappings)	, true);
		System.out.println("Done! Output written to "+outputMappings);
	}

}
