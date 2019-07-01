package com.bbn.akbc.neolearnit.observations.label;

import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.common.Annotation.FrozenState;
import com.bbn.bue.common.symbols.Symbol;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.*;

@JsonTypeInfo(use=JsonTypeInfo.Id.MINIMAL_CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public class LabelPattern extends LearnitPattern {
    @JsonProperty
    final FrozenState frozenState;

    @JsonProperty
    final Symbol relationType;

    final private String patternIDString(){return this.toIDString();}
    @Override
    public String toPrettyString() {
        return this.toIDString();
    }

    public String getLabel() {
        return relationType.asString();
    }

    @Override
    @JsonProperty
    public String toIDString() {
//        return "[LabelPattern]\tlearnitPattern: " + learnitPattern.toString() + "\trelationType" + relationType.toString();
        return "[LabelPattern]\trelationType" + relationType.toString() + "\tfrozenstate:" + frozenState.toString();
    }


    public FrozenState getFrozenState() {return this.frozenState;}

    @Override
    public boolean isInCanonicalSymmetryOrder() {
        return true;
    }

    @Override
    public Set<Symbol> getLexicalItems() {
        return new HashSet<Symbol>();
    }

    @Override
    public boolean matchesPattern(LearnitPattern obj) {
        if(!(obj instanceof LabelPattern))return false;
        LabelPattern that = (LabelPattern)obj;
        return that.relationType.equals(this.relationType) && that.frozenState.equals(this.frozenState);
    }

    @Override
    public int hashCode() {
        return this.frozenState.hashCode() * 31 + this.relationType.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof LabelPattern))return false;
        LabelPattern that = (LabelPattern)obj;
        return this.matchesPattern(that);
    }

    @JsonCreator
    private static LabelPattern from(
            @JsonProperty("relationType") Symbol relationType,
            @JsonProperty("frozenState") FrozenState frozenState
    ) {
        return new LabelPattern(relationType,frozenState);
    }
    public LabelPattern(Symbol relationType,FrozenState frozenState){
        this.relationType = relationType;
        this.frozenState = frozenState;
    }
    public LabelPattern(String relationType,FrozenState frozenState){
        this.relationType = Symbol.from(relationType);
        this.frozenState = frozenState;
    }
}
