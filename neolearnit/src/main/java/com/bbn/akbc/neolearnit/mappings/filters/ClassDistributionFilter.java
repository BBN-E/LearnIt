package com.bbn.akbc.neolearnit.mappings.filters;

import com.bbn.akbc.common.Pair;
import com.bbn.akbc.common.StructuredKnowledgeBase;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.mappings.filters.storage.MemberStorageFilter;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToSeedMapping;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.relations.utility.Utility;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multiset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by bmin on 4/29/15.
 */
public class ClassDistributionFilter implements MappingsFilter {

//  private final Set<Seed> seeds;

  public ClassDistributionFilter() {
  }

  @Override
  public Mappings makeFiltered(Mappings input) {
    Set<Seed> relevantSeeds = new HashSet<Seed>();

    Multiset<String> slotCounter = HashMultiset.create();
    Map<String, List<Seed>> slot2seeds = new HashMap<String, List<Seed>>();

    for(Seed seed : input.getInstance2Seed().getAllSeeds().elementSet()) {
      Pair<String, String> pairOfStr = Utility.getEntityPairString(seed);

      if(StructuredKnowledgeBase.matchPair(pairOfStr).isPresent()) { // pass if the pair is in KB
        for (String slot : StructuredKnowledgeBase.matchPair(pairOfStr).get()) {

          slotCounter.add(slot);
          if (!slot2seeds.containsKey(slot))
            slot2seeds.put(slot, new ArrayList<Seed>());
          slot2seeds.get(slot).add(seed);
        }
      }
      else {
        String slot = "NR";

        slotCounter.add(slot);
        if (!slot2seeds.containsKey(slot))
          slot2seeds.put(slot, new ArrayList<Seed>());
        slot2seeds.get(slot).add(seed);
      }
    }

    for(String slot : slotCounter.elementSet()) {
      Collections.shuffle(slot2seeds.get(slot));
      int maxNumSeedsKept = getMaxNumSeedsKeptForSlot(slot);
      if(maxNumSeedsKept>=slot2seeds.get(slot).size())
        relevantSeeds.addAll(slot2seeds.get(slot));
      else {
        relevantSeeds.addAll(slot2seeds.get(slot).subList(0, maxNumSeedsKept));
      }

      System.out.println("[COUNT]\t" + slot + "\t" + slotCounter.count(slot) + "\t->\t" + maxNumSeedsKept);
    }

    InstanceToSeedMapping newSeedMapping =
        new InstanceToSeedMapping(
            MemberStorageFilter.<InstanceIdentifier,Seed>fromRightSet(relevantSeeds)
                .filter(input.getInstance2Seed().getStorage()));

    System.out.println("Resulting mapping size: "+newSeedMapping.getStorage().size());

    return new RelevantInstancesFilter().makeFiltered(new Mappings(newSeedMapping,input.getInstance2Pattern()));
  }


  public int getMaxNumSeedsKeptForSlot(String slot) {
    if(!slot2maxNumSeedsKept.containsKey(slot))
      return Integer.MAX_VALUE;
    else
      return slot2maxNumSeedsKept.get(slot);
  }

  static ImmutableMap<String, Integer> slot2maxNumSeedsKept =
      (new ImmutableMap.Builder<String, Integer>())
          .put("PER_Cause_of_Death", 1000000)
          .build();
}


