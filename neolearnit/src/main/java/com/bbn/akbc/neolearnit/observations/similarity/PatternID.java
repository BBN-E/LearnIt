package com.bbn.akbc.neolearnit.observations.similarity;

import com.bbn.akbc.neolearnit.observations.LearnItObservation;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by msrivast on 3/9/18.
 *
 * This class is merely a String representation of an existing {@link LearnitPattern}. The relevance of this class is that
 * for similarity module, one can use essentially this class to encapsulate a pattern as LearnItObservation. This class uses a LearnitPattern object's getIDString()
 * method to from a String representation for it.
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.MINIMAL_CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public class PatternID extends LearnItObservation{

    @JsonProperty
    String patternIDString;

    private PatternID(String patternIDString){
        checkNotNull(patternIDString);
        this.patternIDString = patternIDString;
    }

    public static PatternID from(LearnitPattern learnitPattern){
        checkNotNull(learnitPattern);
        return new PatternID(learnitPattern.toIDString());
    }

    public static PatternID from(String patternIDString){
        checkNotNull(patternIDString);
        return new PatternID(patternIDString);
    }

    public String getPatternIDString(){
        return this.patternIDString;
    }

    public String getNormalizedString(){
        return this.patternIDString.replaceAll(" = ","=").replaceAll("[\\[\\]:_]"," ").replaceAll("[\\s]+",
                " ").trim();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PatternID that = (PatternID) o;

        return patternIDString.equals(that.patternIDString);
    }

    @Override
    public int hashCode() {
        return patternIDString.hashCode();
    }

    @Override
    public String toString() {
        return this.toPrettyString();
    }

    @Override
    @JsonProperty
    public String toIDString(){
        return this.getPatternIDString();
    }

    @Override
    @JsonProperty
    public String toPrettyString(){
        return this.getNormalizedString();
    }

}
