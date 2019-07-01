package com.bbn.akbc.neolearnit.exec;

import java.io.File;
import java.io.IOException;

import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class MappingDiffer {


	public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException {
		Mappings map1 = Mappings.deserialize(new File(args[0]), true);
		Mappings map2 = Mappings.deserialize(new File(args[1]), true);

		boolean same = true;

		System.out.println("Comparing patterns...");
		for (LearnitPattern p1 : map1.getAllPatterns().elementSet()) {
			if (!map2.getAllPatterns().contains(p1)) {
				System.out.println("mapping 2 is missing pattern "+p1.toIDString());
				same = false;
			}
		}
		for (LearnitPattern p2 : map2.getAllPatterns().elementSet()) {
			if (!map1.getAllPatterns().contains(p2)) {
				System.out.println("mapping 1 is missing pattern "+p2.toIDString());
				same = false;
			}
		}

		if (map1.getInstance2Pattern().getStorage().equals(map2.getInstance2Pattern().getStorage())) {
			System.out.println("Instance to Pattern maps are identical.");
		} else {
			System.out.println("Instance to Pattern maps are NOT identical.");
			same = false;
		}



		System.out.println("Comparing seeds...");
		for (Seed s1 : map1.getAllSeeds().elementSet()) {
			if (!map2.getAllSeeds().contains(s1)) {
				System.out.println("mapping 2 is missing seed "+s1.toString());
				same = false;
			}
		}
		for (Seed s2 : map2.getAllSeeds().elementSet()) {
			if (!map1.getAllSeeds().contains(s2)) {
				System.out.println("mapping 1 is missing seed "+s2.toString());
				same = false;
			}
		}

		if (map1.getInstance2Seed().getStorage().equals(map2.getInstance2Seed().getStorage())) {
			System.out.println("Instance to Seed maps are identical.");
		} else {
			System.out.println("Instance to Seed maps are NOT identical.");
			same = false;
		}

		if (same) {
			System.out.println("SUCCESS: Mappings are identical!");
		} else {
			System.out.println("FAILURE: Mappings differ!");
		}
	}

}
