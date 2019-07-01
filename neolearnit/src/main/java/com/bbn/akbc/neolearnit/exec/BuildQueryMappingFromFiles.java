package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.matchinfo.DocQueryDisplay;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.common.targets.TargetFactory;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToSeedMapping;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.storage.MapStorage;
import com.bbn.akbc.neolearnit.storage.StorageUtils;
import com.bbn.akbc.neolearnit.storage.impl.HashMapStorage;
import com.bbn.bue.common.files.FileUtils;
import com.bbn.serif.theories.Mention;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class BuildQueryMappingFromFiles {

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

    public static boolean matchesSpecificQuery(Seed seed, String queryTerm, int slot, boolean useBestName) {
        if (useBestName)
            return Arrays.asList(seed.getStringSlots().get(slot).split(" ")).containsAll(Arrays.asList(queryTerm.split(" ")));
        else
            return Arrays.asList(seed.getSlotHeadText(slot).toString().split(" ")).containsAll(Arrays.asList(queryTerm.split(" ")));
    }

	public static void main(String[] args) throws IOException {
        LearnItConfig.loadParams(new File(args[0]));
		String output = args[1]+".sjson";

        Target target = TargetFactory.fromString(args[2]);

        ImmutableList<File> files = FileUtils.loadFileList(new File(args[3]));

        Multimap<String,Integer> queries = getQueriesFromFile(new File(args[4]));

        boolean merge = args.length > 5 && args[5].equals("--merge");

		MapStorage.Builder<InstanceIdentifier,Seed> instance2Seed = new HashMapStorage.Builder<InstanceIdentifier,Seed>();
		MapStorage.Builder<InstanceIdentifier,LearnitPattern> instance2Pattern = new HashMapStorage.Builder<InstanceIdentifier,LearnitPattern>();

        Multimap<String,InstanceIdentifier> instancesPerDoc = HashMultimap.create();

		int num = 0;
		for (File f : files) {
			num++;
			System.out.println("File number "+num);
			System.out.println("Reading in "+f.toString()+"...");

			//deserialize the source
			Mappings info = Mappings.deserialize(f, true);//.getVersionForInitializationStep();

            if (merge) {
                for (InstanceIdentifier id : info.getSeedInstances()) {
                    instancesPerDoc.put(id.getDocid(),id);
                    for (Seed seed : info.getSeedsForInstance(id)) {
                        instance2Seed.put(id, seed);
                    }
                }
            } else {
                //save the known instances that match our queries
                for (Seed s : info.getAllSeeds().elementSet()) {
                    if (matchesQuery(s, queries)) {
                        System.out.println("(" + info.getInstancesForSeed(s).size() + " instances)");
                        for (InstanceIdentifier id : info.getInstancesForSeed(s)) {
                            instance2Seed.put(id, s);
                        }
                    }
                }
            }
		}

        System.out.printf("Added %d instances based on queries.\n",instance2Seed.build().getLefts().size());

		System.out.println("Writing final output...");
		Mappings finalMappings = new Mappings(
				new InstanceToSeedMapping(instance2Seed.build()),
				new InstanceToPatternMapping(instance2Pattern.build()));

		StorageUtils.serialize(new File(output), finalMappings, true);

        if (merge) {
            for (String queryTerm : queries.keySet()) {
                for (int slot : queries.get(queryTerm)) {
                    String queryString = String.format("%d-%s",slot, Joiner.on('_').join(queryTerm.split(" ")));
                    System.out.println(queryString);

                    //Fetch all docs that have an instance that matches this query
                    List<DocQueryDisplay> queryDocs = new ArrayList<DocQueryDisplay>();
                    for (String docid : instancesPerDoc.keySet()) {
                        Map<InstanceIdentifier,Seed> instanceSeedMap = new HashMap<InstanceIdentifier, Seed>();
                        for (InstanceIdentifier id : instancesPerDoc.get(docid)) {
                            Seed seed = finalMappings.getSeedsForInstance(id).iterator().next();
                            Optional<Mention.Type> slotType = id.getSlotMentionType(slot);
                            boolean useBestName = slotType.isPresent() && slotType.get() == Mention.Type.PRON && DocQueryDisplay.FP_PRONOUNS.contains(seed.getSlotHeadText(slot).toString());
                            if (matchesSpecificQuery(seed,queryTerm,slot,useBestName)) {
                                instanceSeedMap.put(id,seed);
                            } else if (target.isSymmetric() && matchesSpecificQuery(seed.reversed(),queryTerm,slot,useBestName)) {
                                instanceSeedMap.put(id.reversed(),seed.reversed());
                            }
                        }
                        if (!instanceSeedMap.isEmpty())
                            queryDocs.add(new DocQueryDisplay(target, docid, instanceSeedMap, queryTerm, slot));
                    }
                    File out = new File(args[1]+String.format("_docs/%s.json",queryString));
                    if (!out.getParentFile().exists())
                        out.getParentFile().mkdirs();
                    StorageUtils.serialize(out,queryDocs, false);
                }
            }
        }
	}

}
