package com.bbn.akbc.neolearnit.observations.pattern.UnaryPattern;


import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.bue.common.symbols.Symbol;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.HashSet;
import java.util.Set;

@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public class TypePattern extends LearnitPattern {

    @JsonProperty("type")
    final String type;

    @JsonCreator
    public TypePattern(@JsonProperty("type") String type) {
        this.type = type;
    }


    @Override
    @JsonProperty
    public String toPrettyString() {
        return this.toIDString();
    }

    @Override
    public int hashCode() {
        return this.type.hashCode() * 31 + "TypePattern".hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TypePattern)) return false;
        TypePattern that = (TypePattern) obj;
        return this.type.equals(that.type);
    }

    @Override
    public String toString() {
        return this.toIDString();
    }

    @Override
    public String toIDString() {
        return "Type: " + this.type;
    }

    @Override
    public boolean isInCanonicalSymmetryOrder() {
        return false;
    }

    @Override
    public Set<Symbol> getLexicalItems() {
        Set<Symbol> ret = new HashSet<>();
        ret.add(Symbol.from(this.type));
        return ret;
    }

    @Override
    public boolean matchesPattern(LearnitPattern p) {
        if (!(p instanceof TypePattern)) return false;
        TypePattern that = (TypePattern) p;
        return this.type.equals(that.type);
    }
}
