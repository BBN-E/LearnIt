package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfoDisplay;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToSeedMapping;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.storage.MapStorage;
import com.bbn.akbc.neolearnit.storage.StorageUtils;
import com.bbn.akbc.neolearnit.storage.impl.HashMapStorage;
import com.bbn.akbc.neolearnit.storage.structs.EfficientMapDataStore;
import com.bbn.bue.common.files.FileUtils;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class BuildPrecisionEvalMappingFromFiles {

    private static final int MAX_PER_PATTERN = 10;
    private static final int MAX_PER_PATTERN_MERGE = 100;
    private static final int PER_PATTERN_MIN = 2;
    private static final Random RNG = new Random(52525);

	public static void main(String[] args) throws IOException {
        LearnItConfig.loadParams(new File(args[0]));
		String output = args[1]+".sjson";
        String displayOutput = args[1]+".display.sjson";

        TargetAndScoreTables extractor = TargetAndScoreTables.deserialize(new File(args[2]));
        Target target = extractor.getTarget();

        List<File> files = FileUtils.loadFileList(new File(args[3]));

        boolean merge = args.length > 4;
        List<File> displayFiles = merge ? Lists.newArrayList(FileUtils.loadFileList(new File(args[4]))) : ImmutableList.of();

		MapStorage.Builder<InstanceIdentifier,Seed> instance2Seed = new HashMapStorage.Builder<InstanceIdentifier,Seed>();
		MapStorage.Builder<InstanceIdentifier,LearnitPattern> instance2Pattern = new HashMapStorage.Builder<InstanceIdentifier,LearnitPattern>();

        Map<InstanceIdentifier,MatchInfoDisplay> displayMap = new HashMap<InstanceIdentifier, MatchInfoDisplay>();

		int num = 0;
        Multimap<LearnitPattern,InstanceIdentifier> patInstMap = HashMultimap.create();
		for (File f : files) {
			num++;
			System.out.println("File number "+num);
			System.out.println("Reading in "+f.toString()+"...");

			//deserialize the source
			Mappings info = Mappings.deserialize(f, true);//.getVersionForInitializationStep();

            if (merge) {
                for (LearnitPattern pattern : info.getAllPatterns()) {
                    patInstMap.putAll(pattern, info.getInstancesForPattern(pattern));
                }

//                instance2Seed.putAll(info.getInstance2Seed().getStorage());
//                instance2Pattern.putAll(info.getInstance2Pattern().getStorage());
            } else {
                //save the known instances that match frozen patterns
                for (LearnitPattern pattern : extractor.getPatternScores().getFrozen()) {
                    //Take all instances with seeds that possibly match queries and that have an associated frozen pattern
                    patInstMap.putAll(pattern, info.getInstancesForPattern(pattern));
                }
            }
		}

        int perPatternMin = LearnItConfig.defined("precision_eval_pattern_min") ? LearnItConfig.getInt("precision_eval_pattern_min") : PER_PATTERN_MIN;
        int maxPerPattern = merge ? (LearnItConfig.defined("precision_eval_merge_max") ? LearnItConfig.getInt("precision_eval_merge_max") : MAX_PER_PATTERN_MERGE) :
                                    (LearnItConfig.defined("precision_eval_pattern_max") ? LearnItConfig.getInt("precision_eval_pattern_max") : MAX_PER_PATTERN);

        for (LearnitPattern pattern : patInstMap.keySet()) {
            List<InstanceIdentifier> insts = new ArrayList<InstanceIdentifier>(patInstMap.get(pattern));
            if (insts.size() >= perPatternMin) {
                System.out.println("Processing pattern: " + pattern.toIDString());
                Collections.shuffle(insts, RNG);
                for (InstanceIdentifier id : insts.subList(0, Math.min(insts.size(), maxPerPattern))) {
                    instance2Pattern.put(id, pattern);
                    if (!merge)
                        displayMap.put(id, id.reconstructMatchInfoDisplay(target));
                }
            }
        }

        System.out.printf("Added %d instances based on patterns.\n",instance2Pattern.build().getLefts().size());

        System.out.println("Writing final output...");
        Mappings finalMappings = new Mappings(
                new InstanceToSeedMapping(instance2Seed.build()),
                new InstanceToPatternMapping(instance2Pattern.build()));


        if (merge) {
            for (File d : displayFiles) {
                System.out.println("Reading in "+d.toString()+"...");
                Map<InstanceIdentifier,MatchInfoDisplay> displays = StorageUtils.deserialize(d,EfficientMapDataStore.class,true).makeMap();
                for (InstanceIdentifier id : displays.keySet()) {
                    if (finalMappings.getPatternInstances().contains(id)) {
                        displayMap.put(id,displays.get(id));
                    }
                }
            }
        }

		StorageUtils.serialize(new File(output), finalMappings, true);
		StorageUtils.serialize(new File(displayOutput), EfficientMapDataStore.fromMap(displayMap), true);
	}

}
