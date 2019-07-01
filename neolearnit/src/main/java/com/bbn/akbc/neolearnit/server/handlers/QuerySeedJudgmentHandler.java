package com.bbn.akbc.neolearnit.server.handlers;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfoDisplay;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.storage.StorageUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class QuerySeedJudgmentHandler extends SimpleJSONHandler {

    private static final Random RNG = new Random(525252);

	private final Mappings mappings;
    private final Map<Seed,Boolean> seedMap;
    private final Multimap<String,Seed> humanSeeds;
    private final Map<Seed,Seed> corefMap;
    private final Map<InstanceIdentifier,MatchInfoDisplay> displayMap;
    private final Multimap<String,Seed> querySeedMap;
    private final Target target;
    private final String output;

	public QuerySeedJudgmentHandler(Mappings mappings, Multimap<String,Seed> humanSeeds, Map<InstanceIdentifier, MatchInfoDisplay> displayMap,
                                    Multimap<String,Seed> querySeedMap, Target target, String output)
    {
		this.mappings = mappings;
        this.seedMap = new HashMap<Seed,Boolean>();
        this.corefMap = new HashMap<Seed, Seed>();
        this.humanSeeds = humanSeeds;
        this.displayMap = displayMap;
        this.querySeedMap = querySeedMap;
        this.target = target;
        this.output = output;
	}

	@JettyMethod("/query/shutdown")
	public String shutdown() throws IOException {
        Set<Seed> systemGood = new HashSet<Seed>();
        Set<Seed> systemBad  = new HashSet<Seed>();
        Set<Seed> humanGold  = new HashSet<Seed>(humanSeeds.values());

        for (Seed system : seedMap.keySet()) {
            if (seedMap.get(system)) {
                if (corefMap.containsKey(system)) {
                    systemGood.add(corefMap.get(system));
                } else {
                    systemGood.add(system);
                }
            } else {
                systemBad.add(system);
            }
        }

        StorageUtils.serialize(new File(output+"_good.json"), systemGood, false);
        StorageUtils.serialize(new File(output+"_bad.json"), systemBad, false);
        StorageUtils.serialize(new File(output+"_human.json"), humanGold, false);

        Runtime.getRuntime().exit(0);
		return "success";
	}

    @JettyMethod("/query/get_queries")
    public List<String> getQueries() {
        List<String> queries = new ArrayList<String>(Sets.union(querySeedMap.keySet(), humanSeeds.keySet()));
        Collections.sort(queries);
        return queries;
    }

	/*------------------------------------------------------------------*
	 *                                                                  *
	 *                       SEED ROUTINES                              *
	 *                                                                  *
	 *------------------------------------------------------------------*/

    private static final ObjectMapper mapper = StorageUtils.getMapperWithoutTyping();

    private Seed getSeedFromJson(String seedStr) throws IOException {
        return mapper.readValue(seedStr, Seed.class);
    }

    @JettyMethod("/query/get_seed_instances")
	public List<MatchInfoDisplay> getInstancesForSeed(@JettyArg("seed") String seedStr) throws IOException {
        List<MatchInfoDisplay> toReturn = new ArrayList<MatchInfoDisplay>();
		Seed seed = getSeedFromJson(seedStr);
        Collection<InstanceIdentifier> insts = mappings.getInstancesForSeed(seed);
        for (InstanceIdentifier id : insts) {
            toReturn.add(displayMap.get(id));
        }
        return toReturn;
	}

    @JettyMethod("/query/get_all_seeds")
    public List<Seed> getAllSeeds() {
        seedMap.clear();
        return ImmutableList.copyOf(mappings.getAllSeeds().elementSet());
    }

    @JettyMethod("/query/get_all_human_seeds")
    public List<Seed> getAllHumanSeeds() {
        seedMap.clear();
        return ImmutableList.copyOf(humanSeeds.values());
    }

	@JettyMethod("/query/get_query_seeds")
	public List<Seed> getQuerySeeds(@JettyArg("query") String queryString) throws IOException {
		List<Seed> seeds = new ArrayList<Seed>();
//        String queryString = String.format("%d %s",slot,entity);

        if (queryString.equals("Show All"))
            return ImmutableList.copyOf(mappings.getAllSeeds().elementSet());

        for (Seed seed : mappings.getAllSeeds().elementSet()) {
            if (querySeedMap.get(queryString).contains(seed)) {
                seeds.add(seed);
            }
        }

		return seeds;
	}

    @JettyMethod("/query/get_human_query_seeds")
    public List<Seed> getHumanQuerySeeds(@JettyArg("query") String queryString) throws IOException {
        if (queryString.equals("Show All"))
            return ImmutableList.copyOf(humanSeeds.values());
        return new ArrayList<Seed>(humanSeeds.get(queryString));
    }

    /**
     * ACCEPT SEEDS
     * ------------------------------------
     * Adds the given set of seeds to the set of judged seeds.
     *
     * @param seeds
     * @param quality
     * @return
     * @throws com.fasterxml.jackson.core.JsonParseException
     * @throws com.fasterxml.jackson.databind.JsonMappingException
     * @throws java.io.IOException
     */
    @JettyMethod("/query/add_seeds")
    public String addInstances(@JettyArg("seeds") String[] seeds, @JettyArg("quality") String quality) throws IOException {
        int count = 0;
        for (String seedStr : seeds) {;
            Seed seed = getSeedFromJson(seedStr);
            seedMap.put(seed,quality.equals("good"));
        }
        return "Added "+count+" seeds.";
    }

    /**
     * ACCEPT SEED
     * ------------------------------------
     * Adds the given set of seed to the set of judged seeds.
     *
     * @param seed
     * @param quality
     * @return
     * @throws com.fasterxml.jackson.core.JsonParseException
     * @throws com.fasterxml.jackson.databind.JsonMappingException
     * @throws java.io.IOException
     */
    @JettyMethod("/query/add_seed")
    public String addSeed(@JettyArg("seed") String seedStr, @JettyArg("quality") String quality) throws IOException {
        Seed seed = getSeedFromJson(seedStr);
        seedMap.put(seed, quality.equals("good"));
        return "Added seed";
    }

    /**
     * COREF SEED
     * ------------------------------------
     * Adds the given set of seed to the set of judged seeds.
     *
     * @param seed
     * @param quality
     * @return
     * @throws com.fasterxml.jackson.core.JsonParseException
     * @throws com.fasterxml.jackson.databind.JsonMappingException
     * @throws java.io.IOException
     */
    @JettyMethod("/query/coref_seed")
    public String corefSeed(@JettyArg("humanSeed") String humanSeedStr, @JettyArg("systemSeed") String systemSeedStr) throws IOException {
        Seed humanSeed = getSeedFromJson(humanSeedStr);
        Seed systemSeed = getSeedFromJson(systemSeedStr);
        corefMap.put(systemSeed,humanSeed);
        seedMap.put(systemSeed,true);
        return "Coreffed seeds";
    }

    /**
     * UNACCEPT SEED
     * -----------------------------------
     * Unsets the specified seed
     *
     * @param target
     * @param seedStr
     * @return
     * @throws com.fasterxml.jackson.core.JsonParseException
     * @throws com.fasterxml.jackson.databind.JsonMappingException
     * @throws java.io.IOException
     */
    @JettyMethod("/query/remove_seed")
    public String removeInstance(@JettyArg("seed") String seedStr) throws IOException {
        Seed seed = getSeedFromJson(seedStr);
        if (seed != null) {
            if (seedMap.containsKey(seed)) seedMap.remove(seed);
            if (corefMap.containsKey(seed)) corefMap.remove(seed);
        }
        return "success";
    }
}
