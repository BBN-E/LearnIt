package com.bbn.akbc.neolearnit.observations.similarity;

import com.bbn.akbc.neolearnit.observations.seed.Seed;

import java.util.Map;

/**
 * Created by msrivast on 3/8/18.
 */
public class SeedSimilarity extends ObservationSimilarity{

    private SeedSimilarity(Seed observedSeed, Map<Seed,Double> similarSeeds){
        super(observedSeed,similarSeeds);
    }

    public static SeedSimilarity create(Seed observedSeed, Map<Seed,Double> similarSeeds){
        return new SeedSimilarity(observedSeed, similarSeeds);
    }

}
