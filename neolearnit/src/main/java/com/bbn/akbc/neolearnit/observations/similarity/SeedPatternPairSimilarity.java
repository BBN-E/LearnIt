package com.bbn.akbc.neolearnit.observations.similarity;

import java.util.Map;

/**
 * Created by msrivast on 3/8/18.
 */
public class SeedPatternPairSimilarity extends ObservationSimilarity{

    private SeedPatternPairSimilarity(SeedPatternPair observedPair, Map<SeedPatternPair,Double> similarPairs){
        super(observedPair,similarPairs);
    }

    public static SeedPatternPairSimilarity create(SeedPatternPair observedPair, Map<SeedPatternPair,Double> similarPairs){
        return new SeedPatternPairSimilarity(observedPair, similarPairs);
    }

}
