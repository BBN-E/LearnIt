package com.bbn.akbc.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use=JsonTypeInfo.Id.MINIMAL_CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public class Pair<A, B> {
	@JsonProperty("first")
    private A first;
	@JsonProperty("second")
    private B second;

	@JsonCreator
    public Pair(@JsonProperty("first") A first,@JsonProperty("second") B second) {
    	this.first = first;
    	this.second = second;
    }

    @Override
	public int hashCode() {
    	int hashFirst = first != null ? first.hashCode() : 0;
    	int hashSecond = second != null ? second.hashCode() : 0;

    	return (hashFirst + hashSecond) * hashSecond + hashFirst;
    }

    @Override
	@SuppressWarnings("rawtypes")
	public boolean equals(Object other) {
    	if (other instanceof Pair) {
    		Pair otherPair = (Pair) other;
            return this.first.equals(otherPair.first) && this.second.equals(otherPair.second);
//    		((  this.first == otherPair.first ||
//    			( this.first != null && otherPair.first != null &&
//    			  this.first.equals(otherPair.first))) &&
//    		 (	this.second == otherPair.second ||
//    			( this.second != null && otherPair.second != null &&
//    			  this.second.equals(otherPair.second))) );
    	}

    	return false;
    }

    @Override
	public String toString()
    {
           return "(" + first + ", " + second + ")";
    }

    public A getFirst() {
    	return first;
    }

    public void setFirst(A first) {
    	this.first = first;
    }

    public B getSecond() {
    	return second;
    }

    public void setSecond(B second) {
    	this.second = second;
    }
}


