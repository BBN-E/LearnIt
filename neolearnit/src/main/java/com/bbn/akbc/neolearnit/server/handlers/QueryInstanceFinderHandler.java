package com.bbn.akbc.neolearnit.server.handlers;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.matchinfo.DocQueryDisplay;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfoDisplay;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToSeedMapping;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.storage.MapStorage;
import com.bbn.akbc.neolearnit.storage.StorageUtils;
import com.bbn.akbc.neolearnit.storage.impl.HashMapStorage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class QueryInstanceFinderHandler extends SimpleJSONHandler {

    private static final Random RNG = new Random(525252);

	private final Map<InstanceIdentifier,Boolean> instanceMap;
	private final Mappings mappings;
    private final Map<InstanceIdentifier,MatchInfoDisplay> displayMap;
    private final Target target;
    private final String output;

	public QueryInstanceFinderHandler(Mappings mappings, Map<InstanceIdentifier, MatchInfoDisplay> displayMap, Target target, String output) {
		this.instanceMap = new HashMap<InstanceIdentifier,Boolean>();
		this.mappings = mappings;
        this.displayMap = displayMap;
        this.target = target;
        this.output = output;
	}

	@JettyMethod("/query/shutdown")
	public String shutdown() throws IOException {
        MapStorage.Builder<InstanceIdentifier,Seed> instance2SeedGood = new HashMapStorage.Builder<InstanceIdentifier,Seed>();
        MapStorage.Builder<InstanceIdentifier,LearnitPattern> instance2PatternGood = new HashMapStorage.Builder<InstanceIdentifier,LearnitPattern>();
        MapStorage.Builder<InstanceIdentifier,Seed> instance2SeedBad = new HashMapStorage.Builder<InstanceIdentifier,Seed>();
        MapStorage.Builder<InstanceIdentifier,LearnitPattern> instance2PatternBad = new HashMapStorage.Builder<InstanceIdentifier,LearnitPattern>();

        for (InstanceIdentifier id : instanceMap.keySet()) {
            if (instanceMap.get(id)) {
                for (Seed s : mappings.getInstance2Seed().getSeeds(id))
                    instance2SeedGood.put(id,s);
                for (LearnitPattern p : mappings.getInstance2Pattern().getPatterns(id))
                    instance2PatternGood.put(id,p);
            } else {
                for (Seed s : mappings.getInstance2Seed().getSeeds(id))
                    instance2SeedBad.put(id,s);
                for (LearnitPattern p : mappings.getInstance2Pattern().getPatterns(id))
                    instance2PatternBad.put(id,p);
            }
        }

        Mappings goodMappings = new Mappings(
                new InstanceToSeedMapping(instance2SeedGood.build()), new InstanceToPatternMapping(instance2PatternGood.build()));
        Mappings badMappings = new Mappings(
                new InstanceToSeedMapping(instance2SeedBad.build()), new InstanceToPatternMapping(instance2PatternBad.build()));

        StorageUtils.serialize(new File(output+"_good.json"), goodMappings, false);
        StorageUtils.serialize(new File(output+"_bad.json"), badMappings, false);

        Runtime.getRuntime().exit(0);
		return "success";
	}

	/*------------------------------------------------------------------*
	 *                                                                  *
	 *                       INSTANCE ROUTINES                          *
	 *                                                                  *
	 *------------------------------------------------------------------*/

	public List<String> getInstanceContexts(Collection<InstanceIdentifier> insts) throws IOException {
		List<String> result = new ArrayList<String>();
		List<InstanceIdentifier> instList = new ArrayList<InstanceIdentifier>(insts);
        Collections.shuffle(instList,RNG);
		for (InstanceIdentifier id : instList) {
			result.add(displayMap.get(id).html());
		}
		return result;
	}

    public static boolean matchesQuery(Seed seed, String entity, int slot) {
        String seedSlot = seed.getStringSlots().get(slot);
        return Arrays.asList(seedSlot.split(" ")).containsAll(Arrays.asList(entity.split(" ")));
    }

    @JettyMethod("/query/get_all_instances")
    public List<MatchInfoDisplay> getAllInstances() {
        instanceMap.clear();
        List<MatchInfoDisplay> displays = new ArrayList<MatchInfoDisplay>();
        for (InstanceIdentifier id : new ArrayList<InstanceIdentifier>(mappings.getSeedInstances()).subList(0,100))
            displays.add(displayMap.get(id));
        return displays;
    }

	@JettyMethod("/query/get_query_instances")
	public List<InstanceIdentifier> getQueryInstances(@JettyArg("entity") String entity, @JettyArg("slot") int slot) throws IOException {
		List<InstanceIdentifier> insts = new ArrayList<InstanceIdentifier>();

        for (Seed seed : mappings.getAllSeeds().elementSet()) {
            if (matchesQuery(seed, entity, slot)) {
                for (InstanceIdentifier id : mappings.getInstancesForSeed(seed)) {
                    insts.add(id);
                }
            }
        }

        Collections.shuffle(insts,RNG);
		return insts;//getInstanceContexts(insts);
	}

    /**
     * SEARCH INSTANCES BY KEYWORD
     * -----------------------------------------------------
     * Searches for viable instances with the given keyword.
     * An empty slot denotes a wildcard.
     *
     * @param keyword
     * @return
     */
    public List<InstanceIdentifier> getInstancesByKeyword(String keyword) {

        List<InstanceIdentifier> results = new ArrayList<InstanceIdentifier>();
        for (InstanceIdentifier id : mappings.getInstance2Seed().getAllInstances().elementSet()) {
            if (displayMap.get(id).getPrimaryLanguageMatch().getSentence().toLowerCase().contains(keyword.toLowerCase())) {
                results.add(id);
            }
        }

        //sort by frequency
        Collections.shuffle(results,RNG);

        return results;
    }

    @JettyMethod("/query/get_instances_by_keyword")
    public List<InstanceIdentifier> getInstancesByKeyword(@JettyArg("keywords") String[] keywords) {
        System.out.println(Arrays.asList(keywords));
        if (keywords.length == 0)
            return new ArrayList<InstanceIdentifier>();

        Set<InstanceIdentifier> matching = new HashSet<InstanceIdentifier>();

        for (String keyword : keywords)
            matching.addAll(getInstancesByKeyword(keyword));

        List<InstanceIdentifier> matchList = new ArrayList<InstanceIdentifier>();
        for (InstanceIdentifier id : mappings.getInstance2Seed().getAllInstances().elementSet()) {
            if (matching.contains(id))
                matchList.add(id);
        }

        return matchList;
    }

    /**
     * ACCEPT INSTANCES
     * ------------------------------------
     * Adds the given set of instances to the set of judged instances.
     *
     * @param instances
     * @param quality
     * @return
     * @throws com.fasterxml.jackson.core.JsonParseException
     * @throws com.fasterxml.jackson.databind.JsonMappingException
     * @throws java.io.IOException
     */
    @JettyMethod("/query/add_instances")
    public String addInstances(@JettyArg("instances") String[] instances, @JettyArg("quality") String quality) throws IOException {
        int count = 0;
        for (String instStr : instances) {
            String json = String.format("[\"InstanceIdentifier\",%s]",instStr);
            InstanceIdentifier inst = StorageUtils.getDefaultMapper().readValue(json,InstanceIdentifier.class);
            instanceMap.put(inst,quality.equals("good"));
        }
        System.out.println(instanceMap);
        return "Added "+count+" instances";
    }

    /**
     * ACCEPT INSTANCE
     * ------------------------------------
     * Adds the given set of instances to the set of judged instances.
     *
     * @param instance
     * @param quality
     * @return
     * @throws com.fasterxml.jackson.core.JsonParseException
     * @throws com.fasterxml.jackson.databind.JsonMappingException
     * @throws java.io.IOException
     */
    @JettyMethod("/query/add_instance")
    public String addInstance(@JettyArg("instance") String instStr, @JettyArg("quality") String quality) throws IOException {
        int count = 0;
        String json = String.format("[\"InstanceIdentifier\",%s]",instStr);
        InstanceIdentifier inst = StorageUtils.getDefaultMapper().readValue(json,InstanceIdentifier.class);
        instanceMap.put(inst,quality.equals("good"));
        System.out.println(instanceMap);
        return "Added "+count+" instances";
    }

    /**
     * UNACCEPT AN INSTANCE
     * -----------------------------------
     * Unsets the specified instance
     *
     * @param target
     * @param seedStr
     * @return
     * @throws com.fasterxml.jackson.core.JsonParseException
     * @throws com.fasterxml.jackson.databind.JsonMappingException
     * @throws java.io.IOException
     */
    @JettyMethod("/query/remove_instance")
    public String removeInstance(@JettyArg("instance") String instStr) throws IOException {
        String json = String.format("[\"InstanceIdentifier\",%s]",instStr);
        InstanceIdentifier inst = StorageUtils.getDefaultMapper().readValue(json,InstanceIdentifier.class);
        if (inst != null && instanceMap.containsKey(inst)) {
            instanceMap.remove(inst);
        }
        System.out.println(instanceMap);
        return "success";
    }

    @JettyMethod("/query/try_get_doc")
    public List<DocQueryDisplay> tryGetDoc() throws IOException {
        String path = "/nfs/mercury-04/u24/mcrivaro/source/trunk/Active/Projects/neolearnit/expts/sec_test/sec_providesTo/query-mappings.1-molex.doc.json";
        return StorageUtils.deserialize(new File(path), ArrayList.class, false);
    }

}
