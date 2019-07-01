package com.bbn.akbc.neolearnit.mappings.filters;

import com.bbn.akbc.common.Pair;
import com.bbn.akbc.common.SlotConverter;
import com.bbn.akbc.common.StructuredKnowledgeBase;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.relations.utility.Utility;

import com.google.common.base.Optional;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;


public class KnowledgeBaseSamplingFilter implements MappingsFilter {

  	private final double subsampling_ratio_pos;
	private final double subsampling_ratio_neg;
	private Random randomGenerator;

	public KnowledgeBaseSamplingFilter(double subsampling_ratio_pos, double subsampling_ratio_neg) {
	  this.subsampling_ratio_pos = subsampling_ratio_pos;
	  this.subsampling_ratio_neg = subsampling_ratio_neg;
	  this.randomGenerator = new Random();
	}

	public boolean passSampling(double subsampling_ratio) {
		int randomInt = randomGenerator.nextInt(1000);
		if((double)randomInt/1000 > subsampling_ratio)
			return false;
		return true;
	}


	@Override
	public Mappings makeFiltered(Mappings input) {
		Set<Seed> seeds = new HashSet<Seed>();

		/*
		Set<LearnitPattern> patterns = new HashSet<LearnitPattern>();

		for (LearnitPattern pattern : input.getInstance2Pattern().getAllPatterns().elementSet()) {
			patterns.add(pattern);
		}
		*/

	  	System.out.println("== total #seeds: " + input.getAllSeeds().elementSet().size());
	  	int nSeed=0;
		for(Seed seed : input.getInstance2Seed().getAllSeeds().elementSet()) {
		  	nSeed++;
		  	if(nSeed%10000==0) {
			  System.out.println("== nSeed: " + nSeed);
			}

			Pair<String, String> pairOfStr = Utility.getEntityPairString(seed);

	    		Optional<Set<String>> matchedSlots = StructuredKnowledgeBase.matchPair(pairOfStr);
			if(matchedSlots.isPresent()) {// pass if the pair is in KB
			  if (passSampling(subsampling_ratio_pos))
			    seeds.add(seed);
			}
			else {
				if(StructuredKnowledgeBase.isInDictionary(pairOfStr.getFirst()) || StructuredKnowledgeBase.isInDictionary(pairOfStr.getSecond())) {
					if(passSampling(subsampling_ratio_neg))
						seeds.add(seed);
				}
			}


			if(!matchedSlots.isPresent()) {
			}

			Set<String> posLabels = new HashSet<String>();
			Set<String> negLabels = new HashSet<String>();
			negLabels = new HashSet<String>(SlotConverter.getAllSlots());
			if(matchedSlots.isPresent()) {
			  posLabels = matchedSlots.get();
			  negLabels.add("NR"); // not a relation
			  negLabels.removeAll(posLabels);
			}
			else {
			  posLabels = new HashSet<String>();
			  posLabels.add("NR");
			}
			System.out.println("====: " + pairOfStr.toString());
			System.out.println(Utility.list2string(posLabels, "\n", "++: "));
			System.out.println(Utility.list2string(negLabels, "\n", "--: "));


		  int nInst=0;
		  for(InstanceIdentifier inst : input.getInstancesForSeed(seed)) {
		    nInst++;

		    String slot0MentionType = "NA";
		    if(inst.getSlotMentionType(0).isPresent())
		      slot0MentionType = inst.getSlotMentionType(0).get().toString();

		    String slot1MentionType = "NA";
		    if(inst.getSlotMentionType(1).isPresent())
		      slot1MentionType = inst.getSlotMentionType(1).get().toString();


		    /*
		     * temporarily commented out

		    List<String> features = Extractor.fromInstIdToRichFeatures(inst, input);

		    // generate features with 1) positive labels, and 2) bag ID + inst ID
		    System.out.println(Extractor.list2string(features, "\n",
			"=+\t" + nSeed + "\t" + nInst + "\t" +
			    inst.getSlotEntityType(0) + "\t" + inst.getSlotEntityType(1) + "\t" +
			    slot0MentionType + "\t" + slot1MentionType + "\t" +
			    Extractor.list2string(posLabels, " ") + "\t"));
			*/
		  }
		}

		Mappings resultedMapping = new SeedMatchFilter(seeds).makeFiltered(input);

		return resultedMapping;
	}

}
