package com.bbn.akbc.neolearnit.observations.similarity;

import com.bbn.akbc.neolearnit.observations.LearnItObservation;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by msrivast on 3/9/18.
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.MINIMAL_CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public class SeedPatternPair extends LearnItObservation{

    @JsonProperty
    Seed seed;
    @JsonProperty
    PatternID patternID;

    private SeedPatternPair(Seed seed, PatternID patternID){
        checkNotNull(seed);
        checkNotNull(patternID);
        this.seed = seed;
        this.patternID = patternID;
    }

    public static SeedPatternPair create(Seed seed, PatternID pattern){
        return new SeedPatternPair(seed, pattern);
    }

    public Seed seed(){
        return this.seed;
    }

    public PatternID pattern(){
        return this.patternID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SeedPatternPair that = (SeedPatternPair) o;

        if (!seed.equals(that.seed)) return false;
        return patternID.equals(that.patternID);
    }

    @Override
    public int hashCode() {
        int result = seed.hashCode();
        result = 31 * result + patternID.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "{seed: "+this.seed.toIDString()+", patternID: "+this.patternID.toIDString()+"}";
    }

    @Override
    public String toIDString(){
        return this.toString();
    }

    @Override
    public String toPrettyString(){
        return this.toString();
    }

}
