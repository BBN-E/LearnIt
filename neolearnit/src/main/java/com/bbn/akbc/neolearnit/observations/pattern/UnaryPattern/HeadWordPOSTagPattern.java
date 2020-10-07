package com.bbn.akbc.neolearnit.observations.pattern.UnaryPattern;

import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.bue.common.symbols.Symbol;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.HashSet;
import java.util.Set;

@JsonTypeInfo(use=JsonTypeInfo.Id.MINIMAL_CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public class HeadWordPOSTagPattern extends LearnitPattern {

    @JsonProperty
    String headWord;
    @JsonProperty
    String POSTag;

    @JsonCreator
    public HeadWordPOSTagPattern(@JsonProperty("headWord") String headWord, @JsonProperty("POSTag") String POSTag) {

        this.headWord = headWord;
        this.POSTag = POSTag;
    }

    @Override
    public String toString(){
        return "[Headword=" + this.headWord + ", POSTag="
                + this.POSTag + "]";
    }

    @Override
    @JsonProperty
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
        Set<Symbol> ret = new HashSet<>();
        ret.add(Symbol.from(this.headWord));
        return ret;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj.getClass() != this.getClass())return false;
        HeadWordPOSTagPattern that = (HeadWordPOSTagPattern) obj;
        return that.headWord.equals(this.headWord) && that.POSTag.equals(this.POSTag);
    }

    @Override
    public int hashCode(){
        return 31 * headWord.hashCode() + POSTag.hashCode();
    }

    @Override
    public boolean matchesPattern(LearnitPattern p) {
        return this.equals(p);
    }

    @JsonProperty
    public String patternIDString(){
        return this.toIDString();
    }
}
