package com.bbn.akbc.neolearnit.exec;

import com.bbn.bue.common.StringUtils;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.matchinfo.DocQueryDisplay;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfoDisplay;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.common.targets.TargetFactory;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.server.SimpleServer;
import com.bbn.akbc.neolearnit.server.handlers.QuerySeedJudgmentHandler;
import com.bbn.akbc.neolearnit.storage.StorageUtils;
import com.bbn.akbc.neolearnit.storage.structs.EfficientMapDataStore;
import com.bbn.serif.theories.Mention;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.Files;

import java.io.File;
import java.util.Arrays;
import java.util.Map;

public class QueryInstanceFinderServerMain {

	public static void main(String[] args) throws Exception {
		String params = args[0];
		String mappingsFile = args[1]+".sjson";
		String displayMapFile = args[1]+".display.sjson";
        String targetName = args[2];
        String queryFile = args[3];
        String responseFileDir = args[4];
        String output = args[5];
		int port = Integer.parseInt(args[6]);

		LearnItConfig.loadParams(new File(params));

		System.out.println("loading mappings...");

		Mappings mappings = Mappings.deserialize(new File(mappingsFile), true);
        Map<InstanceIdentifier,MatchInfoDisplay> displayMap = StorageUtils.deserialize(new File(displayMapFile), EfficientMapDataStore.class, true).makeMap();
//        Map<InstanceIdentifier,MatchInfoDisplay> displayMap = new HashMap<InstanceIdentifier, MatchInfoDisplay>();
        Target target = TargetFactory.fromString(targetName);

        //Fetch the queries
        Multimap<String,Integer> queries = BuildQueryInstanceMappingFromFiles.getQueriesFromFile(new File(queryFile));

        Multimap<String,Seed> humanSeeds = HashMultimap.create();
        for (File rf : new File(responseFileDir).listFiles()) {
            String fname = rf.getName();
            int fSlot = Integer.parseInt(fname.substring(0,1));
            String queryTerm = StringUtils.SpaceJoin.apply(Arrays.asList(fname.substring(2, fname.lastIndexOf('.')).split("_")));
            if (queries.containsKey(queryTerm) && queries.get(queryTerm).contains(fSlot)) {
                String queryString = String.format("%d %s",fSlot,queryTerm);
                for (String line : Files.readLines(rf, Charsets.UTF_8)) {
                    if (line.trim().length() == 0) continue;
                    Seed seed;
                    if (fSlot == 0)
                        seed = Seed.from("_", queryTerm, line.trim());
                    else
                        seed = Seed.from("_", line.trim(), queryTerm);
                    humanSeeds.put(queryString, seed);
                }
            }
        }

        //Associate each seed with the query it potentially is a match for
        Multimap<String,Seed> querySeedMap = HashMultimap.create();
        for (InstanceIdentifier id : displayMap.keySet()) {
            Seed seed = mappings.getSeedsForInstance(id).iterator().next();
            for (String queryTerm : queries.keySet()) {
                for (int slot : queries.get(queryTerm)) {
                    Optional<Mention.Type> slotType = id.getSlotMentionType(slot);
                    boolean useBestName = slotType.isPresent() && slotType.get() == Mention.Type.PRON && DocQueryDisplay.FP_PRONOUNS.contains(seed.getSlotHeadText(slot).toString());
                    if (matchesSpecificQuery(seed, queryTerm, slot, useBestName)) {
                        querySeedMap.put(String.format("%d %s",slot,queryTerm),seed);
                    } else if (target.isSymmetric() && matchesSpecificQuery(seed.reversed(), queryTerm, slot, useBestName)) {
                        querySeedMap.put(String.format("%d %s", slot, queryTerm), seed.reversed());
                    }
                }
            }
        }

		System.out.println("starting server...");
        QuerySeedJudgmentHandler handler = new QuerySeedJudgmentHandler(mappings, humanSeeds, displayMap, querySeedMap, target, output);

		new SimpleServer(handler, "html/queries2.html", port)
			.withIntroMessage("Running relation "+targetName+" on port "+port)
			.run();
	}

    public static boolean matchesSpecificQuery(Seed seed, String queryTerm, int slot, boolean useBestName) {
        if (useBestName)
            return Arrays.asList(seed.getStringSlots().get(slot).split(" ")).containsAll(Arrays.asList(queryTerm.split(" ")));
        else
            return Arrays.asList(seed.getSlotHeadText(slot).toString().split(" ")).containsAll(Arrays.asList(queryTerm.split(" ")));
    }
}
