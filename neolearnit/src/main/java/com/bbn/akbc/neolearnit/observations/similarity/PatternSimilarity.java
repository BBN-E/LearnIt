package com.bbn.akbc.neolearnit.observations.similarity;

import java.util.Map;

/**
 * Created by msrivast on 3/8/18.
 */
public class PatternSimilarity extends ObservationSimilarity{

    private PatternSimilarity(PatternID observedPattern, Map<PatternID,Double> similarPatterns){
        super(observedPattern,similarPatterns);
    }

    public static PatternSimilarity create(PatternID observedPattern, Map<PatternID,Double> similarPatterns){
        return new PatternSimilarity(observedPattern, similarPatterns);
    }

}
