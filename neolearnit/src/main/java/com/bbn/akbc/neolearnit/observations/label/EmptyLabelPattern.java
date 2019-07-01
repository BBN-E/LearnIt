package com.bbn.akbc.neolearnit.observations.label;

import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.bue.common.symbols.Symbol;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Set;

@JsonTypeInfo(use=JsonTypeInfo.Id.MINIMAL_CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public class EmptyLabelPattern extends LearnitPattern {

    @JsonProperty
    final Symbol comment;

    final private String patternIDString(){return this.toIDString();}
    @Override
    public String toPrettyString() {
        return this.toIDString();
    }

    @Override
    @JsonProperty
    public String toIDString() {
        return "[EmptyLabelPattern]:" + this.comment.asString();
    }

    public LearnitPattern getOriginallearnitPattern(){
        return this;
    }

    @Override
    public boolean isInCanonicalSymmetryOrder() {
        return true;
    }

    @Override
    public Set<Symbol> getLexicalItems() {
        return null;
    }

    @Override
    public boolean matchesPattern(LearnitPattern obj) {
        if(!(obj instanceof EmptyLabelPattern))return false;
        EmptyLabelPattern that = (EmptyLabelPattern)obj;
        return that.comment.equalTo(this.comment);
    }

    @Override
    public int hashCode() {
        return this.comment.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof EmptyLabelPattern))return false;
        EmptyLabelPattern that = (EmptyLabelPattern)obj;
        return this.matchesPattern(that);
    }

    @JsonCreator
    private static EmptyLabelPattern from(
            @JsonProperty("comment") Symbol comment) {
        return new EmptyLabelPattern(comment);
    }
    public EmptyLabelPattern(String comment){
        this.comment = Symbol.from(comment);
    }
    public EmptyLabelPattern(Symbol comment){
        this.comment = comment;
    }
}