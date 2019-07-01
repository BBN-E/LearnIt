package com.bbn.akbc.neolearnit.observations.pattern.UnaryPattern;


import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.bue.common.symbols.Symbol;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@JsonTypeInfo(use=JsonTypeInfo.Id.MINIMAL_CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public class UnaryLexicalPattern extends LearnitPattern {

    @JsonProperty("tokens")
    List<Symbol> tokens;

    @JsonCreator
    public UnaryLexicalPattern(@JsonProperty("tokens") List<Symbol> setOfTokens){
        this.tokens = new ArrayList<>(setOfTokens);
    }

    @Override
    public String toPrettyString() {
        return this.toString();
    }

    @Override
    @JsonProperty
    public String toIDString() {
        return this.toString();
    }

    @Override
    public boolean isInCanonicalSymmetryOrder() {
        return false;
    }

    @Override
    public Set<Symbol> getLexicalItems() {
        return new HashSet<>(this.tokens);
    }

    @Override
    public boolean matchesPattern(LearnitPattern p) {
        return this.equals(p);
    }

    @Override
    public boolean equals(Object obj){
        if(obj.getClass() != this.getClass())return false;
        UnaryLexicalPattern that = (UnaryLexicalPattern) obj;
        return that.tokens.equals(this.tokens);
    }
    @Override
    public int hashCode(){
        return this.tokens.hashCode();
    }

    @JsonProperty
    public String patternIDString(){
        return this.toIDString();
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        sb.append(this.tokens.stream().map(Object::toString).collect(Collectors.joining(",")));
        sb.append("]");
        return sb.toString();
    }
}
