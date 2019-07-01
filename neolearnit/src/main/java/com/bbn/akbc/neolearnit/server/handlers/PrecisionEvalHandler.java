package com.bbn.akbc.neolearnit.server.handlers;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfoDisplay;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToSeedMapping;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.storage.MapStorage;
import com.bbn.akbc.neolearnit.storage.StorageUtils;
import com.bbn.akbc.neolearnit.storage.impl.HashMapStorage;
import com.google.common.collect.Sets;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class PrecisionEvalHandler extends SimpleJSONHandler {

    private static final Random RNG = new Random(525252);
    private static final int BATCH_SIZE = 50;

	private final Map<InstanceIdentifier,Boolean> instanceMap;
	private final Mappings mappings;
    private final Map<InstanceIdentifier,MatchInfoDisplay> displayMap;
    private final List<InstanceIdentifier> orderedInstances;
    private final String output;
    private final String target;

	public PrecisionEvalHandler(Mappings mappings, String target, Map<InstanceIdentifier,Boolean> initInstanceMap,
                                Map<InstanceIdentifier, MatchInfoDisplay> displayMap, String output) {
		this.instanceMap = initInstanceMap;
		this.mappings = mappings;
        this.target = target;
        this.displayMap = displayMap;
        this.output = output;

        this.orderedInstances = new ArrayList<InstanceIdentifier>(displayMap.keySet());
        //get everything into a consistent random order
        Collections.sort(orderedInstances, new Comparator<InstanceIdentifier>() {
            @Override
            public int compare(InstanceIdentifier id1, InstanceIdentifier id2) {
                return id1.toString().compareTo(id2.toShortString());
            }
        });
        Collections.shuffle(orderedInstances,RNG);
	}

	@JettyMethod("/precision/shutdown")
	public String shutdown() throws IOException {
        MapStorage.Builder<InstanceIdentifier,Seed> instance2SeedGood = new HashMapStorage.Builder<InstanceIdentifier,Seed>();
        MapStorage.Builder<InstanceIdentifier,LearnitPattern> instance2PatternGood = new HashMapStorage.Builder<InstanceIdentifier,LearnitPattern>();
        MapStorage.Builder<InstanceIdentifier,Seed> instance2SeedBad = new HashMapStorage.Builder<InstanceIdentifier,Seed>();
        MapStorage.Builder<InstanceIdentifier,LearnitPattern> instance2PatternBad = new HashMapStorage.Builder<InstanceIdentifier,LearnitPattern>();

        for (InstanceIdentifier id : instanceMap.keySet()) {
            if (instanceMap.get(id)) {
                for (LearnitPattern p : mappings.getInstance2Pattern().getPatterns(id))
                    instance2PatternGood.put(id,p);
            } else {
                for (LearnitPattern p : mappings.getInstance2Pattern().getPatterns(id))
                    instance2PatternBad.put(id,p);
            }
        }

        final Mappings goodMappings = new Mappings(
                new InstanceToSeedMapping(instance2SeedGood.build()), new InstanceToPatternMapping(instance2PatternGood.build()));
        final Mappings badMappings = new Mappings(
                new InstanceToSeedMapping(instance2SeedBad.build()), new InstanceToPatternMapping(instance2PatternBad.build()));

        System.out.println(goodMappings.getPatternInstances().size());
        System.out.println(badMappings.getPatternInstances().size());

        List<LearnitPattern> allPatterns = new ArrayList<LearnitPattern>(Sets.union(goodMappings.getAllPatterns().elementSet(), badMappings.getAllPatterns().elementSet()));
        Collections.sort(allPatterns, new Comparator<LearnitPattern>() {
            @Override
            public int compare(LearnitPattern p1, LearnitPattern p2) {
                int c1 = goodMappings.getInstancesForPattern(p1).size() + badMappings.getInstancesForPattern(p1).size();
                int c2 = goodMappings.getInstancesForPattern(p2).size() + badMappings.getInstancesForPattern(p2).size();
                return c2 - c1;
            }
        });

        StringBuilder precisionOutput = new StringBuilder();
        int numGood = goodMappings.getPatternInstances().size();
        int numAll  = goodMappings.getPatternInstances().size() + badMappings.getPatternInstances().size();
        precisionOutput.append(String.format("Precision: %.4f (%d/%d)\n\n",((double)numGood)/numAll, numGood, numAll));
        for (LearnitPattern p : allPatterns) {
            int pGood = goodMappings.getInstancesForPattern(p).size();
            int pAll  = pGood + badMappings.getInstancesForPattern(p).size();
            precisionOutput.append(String.format("%s: %d/%d, %.4f\n", p.toIDString(), pGood, pAll, ((double)pGood)/pAll));
        }

        System.out.println(precisionOutput.toString());

        FileWriter fw = new FileWriter(output+".txt");
        fw.write(precisionOutput.toString());
        fw.close();

        StorageUtils.serialize(new File(output+"_good.json"), goodMappings, false);
        StorageUtils.serialize(new File(output+"_bad.json"), badMappings, false);

        Runtime.getRuntime().exit(0);
		return "success";
	}

    @JettyMethod("/precision/get_target")
    public String getTarget() {
        return target;
    }

	/*------------------------------------------------------------------*
	 *                                                                  *
	 *                       INSTANCE ROUTINES                          *
	 *                                                                  *
	 *------------------------------------------------------------------*/

    @JettyMethod("/precision/get_instances")
    public List<MatchInfoDisplay> getInstances() {
        int index = 0;
        List<MatchInfoDisplay> displays = new ArrayList<MatchInfoDisplay>();
        while (index < orderedInstances.size() && displays.size() < BATCH_SIZE) {
            InstanceIdentifier id = orderedInstances.get(index);
            if (!instanceMap.containsKey(id)) {
                displays.add(displayMap.get(id));
            }
            ++index;
        }
        return displays;
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
    @JettyMethod("/precision/add_instances")
    public String addInstances(@JettyArg("instances") String[] instances, @JettyArg("quality") String quality) throws IOException {
        int count = 0;
        for (String instStr : instances) {
            String json = String.format("[\"InstanceIdentifier\",%s]",instStr);
            InstanceIdentifier inst = StorageUtils.getDefaultMapper().readValue(json,InstanceIdentifier.class);
            instanceMap.put(inst,quality.equals("good"));
        }

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
    @JettyMethod("/precision/add_instance")
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
    @JettyMethod("/precision/remove_instance")
    public String removeInstance(@JettyArg("instance") String instStr) throws IOException {
        String json = String.format("[\"InstanceIdentifier\",%s]",instStr);
        InstanceIdentifier inst = StorageUtils.getDefaultMapper().readValue(json,InstanceIdentifier.class);
        if (inst != null && instanceMap.containsKey(inst)) {
            instanceMap.remove(inst);
        }
        System.out.println(instanceMap);
        return "success";
    }
}
