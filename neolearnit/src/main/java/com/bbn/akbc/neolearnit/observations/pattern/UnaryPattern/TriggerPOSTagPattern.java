package com.bbn.akbc.neolearnit.observations.pattern.UnaryPattern;

import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.pattern.MonolingualPattern;
import com.bbn.bue.common.symbols.Symbol;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.HashSet;
import java.util.Set;

@JsonTypeInfo(use=JsonTypeInfo.Id.MINIMAL_CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public class TriggerPOSTagPattern extends LearnitPattern {

    @JsonProperty
    String trigger;
    @JsonProperty
    String POSTag;

    @JsonCreator
    public TriggerPOSTagPattern(@JsonProperty("trigger") String trigger,@JsonProperty("POSTag") String POSTag) {

        this.trigger = trigger;
        this.POSTag = POSTag;
    }

    @Override
    public String toString(){
        return "[trigger=" + this.trigger + ", POSTag="
                + this.POSTag + "]";
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
        Set<Symbol> ret = new HashSet<>();
        ret.add(Symbol.from(this.trigger));
        return ret;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj.getClass() != this.getClass())return false;
        TriggerPOSTagPattern that = (TriggerPOSTagPattern) obj;
        return that.trigger.equals(this.trigger) && that.POSTag.equals(this.POSTag);
    }

    @Override
    public int hashCode(){
        return 31 * trigger.hashCode() + POSTag.hashCode();
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
