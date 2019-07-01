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
import com.google.common.base.Charsets;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class BuildQueryInstanceMappingFromFiles {

    public static Multimap<String,Integer> getQueriesFromFile(File queryFile) throws IOException {
        Multimap<String,Integer> queries = HashMultimap.create();
        for (String line : Files.readLines(queryFile, Charsets.UTF_8)) {
            String[] items = line.split("\t");
            if (items.length == 2) {
                queries.put(items[1].toLowerCase(),Integer.parseInt(items[0]));
            }
        }
        return queries;
    }

    private static final Multimap<Integer,String> queryMatches = HashMultimap.create();
    public static boolean matchesQuery(Seed seed, Multimap<String,Integer> queries) {
        if (queryMatches.get(0).contains(seed.getStringSlots().get(0)) || queryMatches.get(1).contains(seed.getStringSlots().get(1)))
            return true;
        for (int i = 0; i < seed.getStringSlots().size(); ++i) {
            String slot = seed.getStringSlots().get(i);
            for (String query : queries.keySet()) {
                if (queries.get(query).contains(i) && Arrays.asList(slot.split(" ")).containsAll(Arrays.asList(query.split(" ")))) {
                    System.out.println("Inserting " + seed.toIDString() + " as match for " + i + " " + query);
                    queryMatches.put(i,slot);
                    return true;
                }
            }
        }
        return false;
    }

	public static void main(String[] args) throws IOException {
        LearnItConfig.loadParams(new File(args[0]));
		String output = args[1]+".sjson";
        String displayOutput = args[1]+".display.sjson";

        TargetAndScoreTables extractor = TargetAndScoreTables.deserialize(new File(args[2]));
        Target target = extractor.getTarget();

        List<File> files = Lists.newArrayList(FileUtils.loadFileList(new File(args[3])));

        boolean merge = args.length > 5 && args[5].equals("--merge");

        Multimap<String,Integer> queries;
        List<File> displayFiles;
        if (!merge) {
            queries = getQueriesFromFile(new File(args[4]));
            displayFiles = ImmutableList.of();
        } else {
            queries = HashMultimap.create();
            displayFiles = Lists.newArrayList(FileUtils.loadFileList(new File(args[4])));
        }

		MapStorage.Builder<InstanceIdentifier,Seed> instance2Seed = new HashMapStorage.Builder<InstanceIdentifier,Seed>();
		MapStorage.Builder<InstanceIdentifier,LearnitPattern> instance2Pattern = new HashMapStorage.Builder<InstanceIdentifier,LearnitPattern>();

        Map<InstanceIdentifier,MatchInfoDisplay> displayMap = new HashMap<InstanceIdentifier, MatchInfoDisplay>();

		int num = 0;
		for (File f : files) {
			num++;
			System.out.println("File number "+num);
			System.out.println("Reading in "+f.toString()+"...");

			//deserialize the source
			Mappings info = Mappings.deserialize(f, true);//.getVersionForInitializationStep();

                    try {
                        if (merge) {
                            instance2Seed.putAll(info.getInstance2Seed().getStorage());
                            instance2Pattern.putAll(info.getInstance2Pattern().getStorage());
                        } else {
                            Set<LearnitPattern> frozen = extractor.getPatternScores().getFrozen();
                            //save the known instances that match our queries
                            for (Seed s : info.getAllSeeds().elementSet()) {
                                //Take all instances with seeds that possibly match queries and that have an associated frozen pattern
                                if (matchesQuery(s, queries)) {
                                    for (InstanceIdentifier id : info.getInstancesForSeed(s)) {
                                        for (LearnitPattern pattern : info
                                            .getPatternsForInstance(id)) {
                                            if (frozen.contains(pattern)) {
                                                instance2Seed.put(id, s.withProperText(target));
                                                instance2Pattern.put(id, pattern);
                                                displayMap.put(id,
                                                    id.reconstructMatchInfoDisplay(target));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
		}

        if (merge) {
            for (File d : displayFiles) {
                System.out.println("Reading in "+d.toString()+"...");
                displayMap.putAll(StorageUtils.deserialize(d,EfficientMapDataStore.class,true).makeMap());
            }
        }

        System.out.printf("Added %d instances based on queries.\n",instance2Seed.build().getLefts().size());

		System.out.println("Writing final output...");
		Mappings finalMappings = new Mappings(
				new InstanceToSeedMapping(instance2Seed.build()),
				new InstanceToPatternMapping(instance2Pattern.build()));

		StorageUtils.serialize(new File(output), finalMappings, true);
		StorageUtils.serialize(new File(displayOutput), EfficientMapDataStore.fromMap(displayMap), true);
	}

}
