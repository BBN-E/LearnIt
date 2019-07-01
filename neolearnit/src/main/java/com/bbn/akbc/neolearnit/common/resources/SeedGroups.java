package com.bbn.akbc.neolearnit.common.resources;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.processing.preprocessing.PreprocessingInformation;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.storage.StorageUtils;
import com.bbn.akbc.neolearnit.storage.structs.EfficientMultimapDataStore;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

/**
 * Created by mcrivaro on 7/14/2014.
 *
 * SeedGroups are structures that treat a collection of different seeds as if they were all the same.
 * Currently used as an ad hoc method of guessing at cross-document coref in cases where two best names
 * vary by only trivial words (e.g. "Raytheon" and "Raytheon Co.")
 */
public class SeedGroups {

    private static Multimap<String, Seed> seedGroups;

    private static File getPreprocessFile(TargetAndScoreTables data) {
        return new File(LearnItConfig.get("learnit_root")+"/expts/"+
                LearnItConfig.get("learnit_expt_suffix")+"/"+data.getRelationPathName(), "seedGroupData.sjson");
    }

    @SuppressWarnings("unchecked")
    private static synchronized Multimap<String, Seed> loadSeedGroups(TargetAndScoreTables data) throws IOException {

        if (seedGroups != null) return seedGroups;
        System.out.println("Loading seed groups...");
        seedGroups = StorageUtils.deserialize(getPreprocessFile(data), EfficientMultimapDataStore.class, true).makeMultimap();
        System.out.println("Done!");
        return seedGroups;
    }

    public static synchronized void save(TargetAndScoreTables data, PreprocessingInformation info) throws IOException {
        StorageUtils.serialize(getPreprocessFile(data), info.getSeedGroups(), true);
    }

    public static synchronized void load(TargetAndScoreTables data) {
        try {
            seedGroups = loadSeedGroups(data);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public static synchronized void clear() {
        seedGroups = null;
    }

    public static Optional<Collection<Seed>> getGroupSeeds(String key) {
        if (key.length() > 0 && seedGroups.containsKey(key))
            return Optional.of(seedGroups.get(key));
        else
            return Optional.absent();
    }

    public static Collection<Seed> getGroupOrSingleton(String key, Seed seed) {
        if (key.length() > 0 && seedGroups.containsKey(key))
            return seedGroups.get(key);
        else
            return ImmutableSet.of(seed);
    }
}
