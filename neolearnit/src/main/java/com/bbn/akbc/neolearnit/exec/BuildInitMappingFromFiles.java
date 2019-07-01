package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.mappings.filters.FrequencyLimitFilter;
import com.bbn.akbc.neolearnit.mappings.filters.MappingsFilter;
import com.bbn.akbc.neolearnit.mappings.filters.RelevantInstancesFilter;
import com.bbn.akbc.neolearnit.mappings.filters.storage.CappedStorageFilter;
import com.bbn.akbc.neolearnit.mappings.filters.storage.FrequencyLimitStorageFilter;
import com.bbn.akbc.neolearnit.mappings.filters.storage.MemberStorageFilter;
import com.bbn.akbc.neolearnit.mappings.filters.storage.StorageFilter;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToSeedMapping;
import com.bbn.akbc.neolearnit.observations.pattern.LearnItPatternFactory;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.pattern.initialization.InitializationPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.storage.MapStorage;
import com.bbn.akbc.neolearnit.storage.StorageUtils;
import com.bbn.akbc.neolearnit.storage.impl.HashMapStorage;
import com.bbn.bue.common.files.FileUtils;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class BuildInitMappingFromFiles {

	public static <T,U> MapStorage.Builder<T,U> updateBuilder(MapStorage.Builder<T,U> builder, StorageFilter<T,U> filter) {
		MapStorage<T,U> result = filter.filter(builder.build());
		MapStorage.Builder<T,U> resultBuilder = result.newBuilder();
		resultBuilder.putAll(result);
		return resultBuilder;
	}

	public static <T> MapStorage.Builder<InstanceIdentifier,T> filterInstances(MapStorage.Builder<InstanceIdentifier,T> builder, Set<InstanceIdentifier> ids) {
		return updateBuilder(builder,new MemberStorageFilter<InstanceIdentifier,T>(ids));
	}

	public static Map<InstanceIdentifier,String> filterInstances(Map<InstanceIdentifier,String> detailMap, Set<InstanceIdentifier> ids) {
		Map<InstanceIdentifier,String> newMap = new HashMap<InstanceIdentifier,String>();
		for (InstanceIdentifier id : ids) {
			newMap.put(id,detailMap.get(id));
		}
		return newMap;
	}

    private static boolean matchesInitializationPattern(LearnitPattern p, Collection<InitializationPattern> initPatterns) {
        for (InitializationPattern ip : initPatterns) {
            if (ip.matchesPattern(p)) return true;
        }
        return false;
    }

    private static boolean matchesInitializationPatternInLearnItFormat(LearnitPattern p, Collection<LearnitPattern> initPatterns) {
        for (LearnitPattern ip : initPatterns) {
            if (ip.matchesPattern(p)) return true;
        }
        return false;
    }



	public static void main(String[] args) throws IOException {
		String params = args[0];
		String output = args[1];
		String target = args[2];

        List<File> files = FileUtils.loadFileList(new File(args[3]));

		String stageId = args[4];

        LearnItConfig.loadParams(new File(params));

        TargetAndScoreTables extractor = TargetAndScoreTables.deserialize(new File(target));

        boolean doSeeds = LearnItConfig.optionalParamTrue("use_seeds_to_initialize");
        boolean doPatterns = LearnItConfig.optionalParamTrue("use_patterns_to_initialize");

        if (!(doSeeds || doPatterns)) throw new RuntimeException("No valid initialization mode specified!");

        Set<InitializationPattern> initPatterns = ImmutableSet.of();
        if (doPatterns && args.length > 5 && new File(args[5]).exists()) {
            initPatterns = InitializationPattern.getFromFile(new File(args[5]), extractor.getTarget());
        }

        // read-in even more additional patterns in Sexp format
        Set<LearnitPattern> initPatternsInSexpFormat = ImmutableSet.of();
        if (doPatterns && args.length > 6 && new File(args[6]).exists()) {
        	initPatternsInSexpFormat = LearnItPatternFactory.fromFile(new File(args[6]), extractor.getTarget());
        }
        //

		int maxsize = LearnItConfig.getInt("initialization_size_stage_" + stageId);
		int cap = LearnItConfig.getInt("initialization_instance_cap");
		int maxCount = LearnItConfig.getInt("initialization_instance_max");
		int minCount = 2;

		MapStorage.Builder<InstanceIdentifier,Seed> instance2Seed = new HashMapStorage.Builder<InstanceIdentifier,Seed>();
		MapStorage.Builder<InstanceIdentifier,LearnitPattern> instance2Pattern = new HashMapStorage.Builder<InstanceIdentifier,LearnitPattern>();

		MappingsFilter atLeast2Filter = new FrequencyLimitFilter(minCount,maxCount,minCount,maxCount);
		RelevantInstancesFilter relevantFilter = new RelevantInstancesFilter();

		Multimap<Seed,InstanceIdentifier> initialSeedSet = HashMultimap.create();
		Multimap<LearnitPattern,InstanceIdentifier> initialPatternSet = HashMultimap.create();

		//shuffle up the files you use for initialization
		Collections.shuffle(files, new Random(123));

		int num = 0;
		for (File f : files) {

			num++;
			System.out.println("File number "+num);
			System.out.println("Reading in "+f.toString()+"...");

			//deserialize the source
			Mappings info = Mappings.deserialize(f,true).getVersionForInitializationStep();

			//save the known seed-pattern relationships and/or situations that match our keywords
            if (doSeeds) {
                for (Seed s : info.getAllSeeds().elementSet()) {
                    if (doSeeds && extractor.getSeedScores().isKnownFrozen(s)) {
                        System.out.println("Found known seed " + s);
                        for (InstanceIdentifier id : info.getInstancesForSeed(s)) {
                            initialSeedSet.put(s, id);
                            for (LearnitPattern pattern : info.getPatternsForInstance(id)) {
                                initialPatternSet.putAll(pattern, info.getInstancesForPattern(pattern));
                            }
                        }
                    }
                }
            }
            for (LearnitPattern p : info.getAllPatterns().elementSet()) {
//            	System.out.println("dbg1: " + p.toIDString());

                if (doPatterns && !initialPatternSet.containsKey(p)) {
                    if (matchesInitializationPattern(p,initPatterns)) {
                        initialPatternSet.putAll(p, info.getInstancesForPattern(p));
                    }
                }

                // add new patterns in Sexp format
                if (doPatterns && !initialPatternSet.containsKey(p)) {
                   if (initPatternsInSexpFormat.contains(p)) {
//                    if (matchesInitializationPatternInLearnItFormat(p, initPatternsInSexpFormat)) {
                	   System.out.println("dbg2: " + p.toIDString());
                        initialPatternSet.putAll(p, info.getInstancesForPattern(p));
                    }
                }

                //

                if (initialPatternSet.containsKey(p)) {
                    for (InstanceIdentifier id : info.getInstancesForPattern(p)) {
                        initialPatternSet.put(p, id);
                        for (Seed s : info.getSeedsForInstance(id)) {
                            initialSeedSet.put(s, id);
                        }
                    }
                }
            }


			//standard filtering
			info = atLeast2Filter.makeFiltered(info);

			//combine in the info
			System.out.println("Loading data...");
			instance2Seed.putAll(info.getInstance2Seed().getStorage());
			instance2Pattern.putAll(info.getInstance2Pattern().getStorage());

			//only-if-necessary filtering
			int size = instance2Seed.build().size()+instance2Pattern.build().size();
			System.out.println("size: "+size);

			if (size > maxsize) {
				// first we re-apply instance caps
				System.out.println("Applying instance cap of "+cap);

				instance2Seed = updateBuilder(instance2Seed, new CappedStorageFilter<InstanceIdentifier,Seed>(-1,cap));
				instance2Pattern = updateBuilder(instance2Pattern, new CappedStorageFilter<InstanceIdentifier,LearnitPattern>(-1,cap));

				Set<InstanceIdentifier> relevantInstances = relevantFilter.getRelevantInstances(
						instance2Seed.build(), instance2Pattern.build());

				instance2Seed = filterInstances(instance2Seed,relevantInstances);
				instance2Pattern = filterInstances(instance2Pattern,relevantInstances);

				size = instance2Seed.build().size()+instance2Pattern.build().size();
				System.out.println("size: "+size);
			}

			while (size > maxsize) {
				System.out.println("raising min limit to "+(minCount+1));
				minCount++;

				atLeast2Filter = new FrequencyLimitFilter(minCount,maxCount,minCount,maxCount);

				instance2Seed = updateBuilder(instance2Seed, new FrequencyLimitStorageFilter<InstanceIdentifier,Seed>(-1,-1,minCount,maxCount));
				instance2Pattern = updateBuilder(instance2Pattern, new FrequencyLimitStorageFilter<InstanceIdentifier,LearnitPattern>(-1,-1,minCount,maxCount));

				Set<InstanceIdentifier> relevantInstances = relevantFilter.getRelevantInstances(
						instance2Seed.build(), instance2Pattern.build());

				instance2Seed = filterInstances(instance2Seed,relevantInstances);
				instance2Pattern = filterInstances(instance2Pattern,relevantInstances);

				size = instance2Seed.build().size()+instance2Pattern.build().size();
				System.out.println("size: "+size);
			}
		}

		// apply the current filters for fairness
		System.out.println("Applying final filters...");
		instance2Seed = updateBuilder(instance2Seed, new FrequencyLimitStorageFilter<InstanceIdentifier,Seed>(-1,-1,minCount,maxCount));
		instance2Pattern = updateBuilder(instance2Pattern, new FrequencyLimitStorageFilter<InstanceIdentifier,LearnitPattern>(-1,-1,minCount,maxCount));

		Set<InstanceIdentifier> relevantInstances = relevantFilter.getRelevantInstances(
				instance2Seed.build(), instance2Pattern.build());

		instance2Seed = filterInstances(instance2Seed,relevantInstances);
		instance2Pattern = filterInstances(instance2Pattern,relevantInstances);

		//put back in all the initials (but not too much)
//        String source = doSeeds ? "initial seeds" + (doKeywords ? "/keywords" : "") : (doKeywords ? "keywords" : "NONE");
		System.out.println("Reinserting additional evidence for initialization sources...");
		int MAX_INIT_SIZE = 100000;
		int initAdded = 0;
		for (Seed s : initialSeedSet.keySet()) {
			for (InstanceIdentifier id : initialSeedSet.get(s)) {
				if (initAdded < MAX_INIT_SIZE) {
					instance2Seed.put(id, s);
					initAdded++;
				}
			}
		}
        System.out.printf("Added %d instances based on initial seeds.\n",initAdded);
		initAdded = 0;

        if (doPatterns && !initialPatternSet.keySet().isEmpty()) {
            int maxPerPattern = MAX_INIT_SIZE / initialPatternSet.keySet().size();
            for (LearnitPattern p : initialPatternSet.keySet()) {
                int perPattern = 0;
                for (InstanceIdentifier id : initialPatternSet.get(p)) {
                    if (perPattern < maxPerPattern) {
                        instance2Pattern.put(id, p);
                        perPattern++;
                        initAdded++;
                    } else break;
                }
            }
        }

		for (LearnitPattern p : initialPatternSet.keySet()) {
			for (InstanceIdentifier id : initialPatternSet.get(p)) {
				if (initAdded < MAX_INIT_SIZE) {
					instance2Pattern.put(id, p);
					initAdded++;
				} else break;
			}
		}
        System.out.printf("Added %d instances based on initial patterns.\n",initAdded);

		System.out.println("Writing final output...");
		Mappings finalMappings = new Mappings(
				new InstanceToSeedMapping(instance2Seed.build()),
				new InstanceToPatternMapping(instance2Pattern.build()));

		StorageUtils.serialize(new File(output), finalMappings, true);

	}

}
