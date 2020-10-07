package com.bbn.akbc.neolearnit.observations.pattern;

import com.bbn.bue.common.symbols.Symbol;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashSet;
import java.util.Set;

public class ContextWordsPattern extends LearnitPattern {

    @JsonProperty("token")
    final Symbol token;

    @JsonCreator
    public ContextWordsPattern(@JsonProperty("token") Symbol token) {
        this.token = token;
    }

    @Override
    @JsonProperty
    public String toPrettyString() {
        return "ContextWordsPattern: " + this.token.asString();
    }

    @Override
    @JsonProperty
    public String toIDString() {
        return this.toPrettyString();
    }

    @Override
    public boolean isInCanonicalSymmetryOrder() {
        return false;
    }

    @Override
    public Set<Symbol> getLexicalItems() {
        Set<Symbol> ret = new HashSet<>();
        ret.add(this.token);
        return ret;
    }

    @Override
    public boolean matchesPattern(LearnitPattern p) {
        if (!(p instanceof ContextWordsPattern)) return false;
        return this.token.equalTo(((ContextWordsPattern) p).token);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ContextWordsPattern)) return false;
        ContextWordsPattern that = (ContextWordsPattern) o;
        return this.token.equals(that.token);
    }

    @Override
    public String toString() {
        return this.toIDString();
    }

    @Override
    public int hashCode() {
        return this.token.hashCode() * 31 + "ContextWordsPattern".hashCode();
    }
}
