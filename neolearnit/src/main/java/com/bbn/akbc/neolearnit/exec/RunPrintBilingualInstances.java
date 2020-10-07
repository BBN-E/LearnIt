package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.common.targets.TargetFactory;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;

import java.io.File;
import java.io.IOException;

public class RunPrintBilingualInstances {

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
		String inputMappingsFile = args[2];
		Mappings mappings = Mappings.deserialize(new File(inputMappingsFile), true);

		LearnItConfig.loadParams(new File(paramsFile));

		Target target = TargetFactory.fromNamedString(relation);

		for(Seed seed : mappings.getAllSeeds().elementSet()) {
			System.out.println("=== Seed: ");
			System.out.println(seed.toString());
			for(InstanceIdentifier instanceIdentifier : mappings.getInstancesForSeed(seed)) {
				MatchInfo matchInfo = instanceIdentifier.reconstructMatchInfo(target);

				System.out.println("====== Instance:");
				System.out.println(matchInfo.toString());

				for(LearnitPattern learnitPattern : mappings.getPatternsForInstance(instanceIdentifier)) {
					System.out.println("========= Pattern:");
					System.out.println(learnitPattern.toString());
				}
			}
		}
	}
}
