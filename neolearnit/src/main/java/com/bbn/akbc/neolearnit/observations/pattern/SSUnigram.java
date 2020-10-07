package com.bbn.akbc.neolearnit.observations.pattern;

import com.bbn.bue.common.symbols.Symbol;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashSet;
import java.util.Set;

public class SSUnigram extends LearnitPattern {

    @JsonProperty
    final Symbol SSUnigramToken;


    @JsonCreator
    public SSUnigram(@JsonProperty("SSUnigramToken") Symbol token) {
        this.SSUnigramToken = token;
    }

    @Override
    @JsonProperty
    public String toPrettyString() {
        return toIDString();
    }

    @Override
    public int hashCode() {
        return this.toIDString().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SSUnigram) {
            return this.SSUnigramToken.equals((((SSUnigram) obj).SSUnigramToken));
        }
        return false;
    }

    @Override
    public String toString() {
        return null;
    }

    @Override
    public String toIDString() {
        return "{" + this.SSUnigramToken + "}";
    }

    @Override
    public boolean isInCanonicalSymmetryOrder() {
        return false;
    }

    @Override
    public Set<Symbol> getLexicalItems() {
        Set<Symbol> ret = new HashSet<>();
        ret.add(this.SSUnigramToken);
        return ret;
    }

    @Override
    public boolean matchesPattern(LearnitPattern p) {
        return this.equals(p);
    }
}
