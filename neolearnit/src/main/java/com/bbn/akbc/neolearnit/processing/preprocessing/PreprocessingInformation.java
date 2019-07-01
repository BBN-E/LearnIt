package com.bbn.akbc.neolearnit.processing.preprocessing;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.resources.StopWords;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.processing.PartialInformation;
import com.bbn.akbc.neolearnit.storage.structs.EfficientMultimapDataStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.hash.Hashing;

import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;


public class PreprocessingInformation implements PartialInformation {

    private final Multimap<String,Integer> seedPatterns;
    private final Multimap<String,Seed> seedGroups;

	@JsonProperty
    private EfficientMultimapDataStore<String,Integer> seedPatterns() {
        return EfficientMultimapDataStore.fromMultimap(seedPatterns);
    }

    @JsonProperty
    private EfficientMultimapDataStore<String,Seed> seedGroups() {
        return EfficientMultimapDataStore.fromMultimap(seedGroups);
    }

    public EfficientMultimapDataStore<String,Integer> reducedSeedPatterns() {
        return seedPatterns();
    }

    public EfficientMultimapDataStore<String,Seed> getSeedGroups() {
        return seedGroups();
    }

	private PreprocessingInformation(Multimap<String,Integer> seedPatterns, Multimap<String,Seed> seedGroups) {
		this.seedPatterns = seedPatterns;
        this.seedGroups = seedGroups;
	}

	@JsonCreator
	private PreprocessingInformation(@JsonProperty("seedPatterns") EfficientMultimapDataStore<String,Integer> matches,
                                     @JsonProperty("seedGroups") EfficientMultimapDataStore<String,Seed> groups)
    {
		this.seedPatterns = matches.makeMultimap();
        this.seedGroups = groups.makeMultimap();
	}


	public static class Builder {

		private final Multimap<String,Integer> seedPatterns;
        private final Multimap<String,Seed> seedGroups;
        private final StopWords stopwords;

		public Builder() {
			seedPatterns = HashMultimap.create();
            seedGroups = HashMultimap.create();
            stopwords = StopWords.getFromParamsWithBackoff("overlap_seed_match_filter_word_list");
		}

		private int getHash(String objectString) {
			return Hashing.md5().hashString(objectString, Charset.defaultCharset()).asInt();
		}

        public void withSeed(Seed seed) {
            String reducedForm = seed.getReducedForm(stopwords);
            if (reducedForm.length() > 0 && !reducedForm.equals(seed.toSimpleString())) {
                seedGroups.put(reducedForm, seed);
            }
        }

		public void withSeedPattern(Seed seed, String patternId) {
            String key = LearnItConfig.optionalParamTrue("use_seed_groups") ? seed.getReducedForm(stopwords) : seed.toSimpleString();
			if (seedPatterns.containsKey(key)) {
				seedPatterns.get(key).add(getHash(patternId));
			} else {
                seedPatterns.put(key,getHash(patternId));
			}
		}

		public void withInfo(PreprocessingInformation info) {
            seedPatterns.putAll(info.seedPatterns);
            seedGroups.putAll(info.seedGroups);
		}

		public void removeLowCountSeeds(int threshold) {
			Set<String> toRemove = new HashSet<String>();
			for (String seed : seedPatterns.keySet()) {
				if (seedPatterns.get(seed).size() <= threshold) {
					toRemove.add(seed);
				}
			}
            System.out.println("REMOVING " + toRemove.size() + " SEEDS!");
			for (String seed : toRemove) {
                seedPatterns.removeAll(seed);
			}

            if (LearnItConfig.optionalParamTrue("use_seed_groups")) {
                toRemove = new HashSet<String>();
                for (String reducedForm : seedGroups.keySet()) {
                    if (seedGroups.get(reducedForm).size() == 1) {
                        toRemove.add(reducedForm);
                    }
                }
                System.out.println("Removing " + toRemove.size() + " seed groups!");
                for (String seed : toRemove) {
                    seedGroups.removeAll(seed);
                }
            }
		}

        public void keepNRandomSeeds(int numToTake) {
            List<String> seedList = ImmutableList.copyOf(seedPatterns.keySet());

            System.out.println("REMOVING " + (seedList.size()-numToTake) + " SEEDS!");

            Random rand = new Random(52525);
            int remaining = seedList.size();
            while (remaining > numToTake) {
                String seed = seedList.get(rand.nextInt(seedList.size()));
                if (seedPatterns.containsKey(seed)) {
                    seedPatterns.removeAll(seed);
                    --remaining;
                }
            }
        }

		public PreprocessingInformation build() {
			System.out.println("Building vectors of similarities for "+seedPatterns.keySet().size()+" seeds.");
            if (LearnItConfig.optionalParamTrue("use_seed_groups"))
                System.out.println("Constructing " + seedGroups.keySet().size() + " seed groups.");
			return new PreprocessingInformation(seedPatterns,seedGroups);
		}
	}

}
