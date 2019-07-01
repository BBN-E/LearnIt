package com.bbn.akbc.neolearnit.observations.pattern;

import com.bbn.akbc.neolearnit.common.SlotPair;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.MINIMAL_CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public abstract class SlotPairPattern<T> extends MonolingualPattern {
	protected final Integer firstSlot;
	protected final Integer secondSlot;
	protected final T content;

	public SlotPairPattern(String language,
			Integer firstSlot, Integer secondSlot, T content) {
		super(language);
		this.firstSlot = firstSlot;
		this.secondSlot = secondSlot;
		this.content = content;
	}

	@JsonProperty(value="firstSlot")
	public Integer getFirstSlot() {
		return firstSlot;
	}

	@JsonProperty(value="secondSlot")
	public Integer getSecondSlot() {
		return secondSlot;
	}

	public SlotPair getSlotPair() {
		return new SlotPair(firstSlot,secondSlot);
	}

	@JsonProperty(value="content")
	public T getContent() {
		return content;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = prime;
		result = prime * result + ((content == null) ? 0 : content.hashCode());
		result = prime * result
				+ ((firstSlot == null) ? 0 : firstSlot.hashCode());
		result = prime * result
				+ ((secondSlot == null) ? 0 : secondSlot.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		@SuppressWarnings("rawtypes")
		SlotPairPattern other = (SlotPairPattern) obj;
		if (content == null) {
			if (other.content != null)
				return false;
		} else if (!content.equals(other.content))
			return false;
		if (firstSlot == null) {
			if (other.firstSlot != null)
				return false;
		} else if (!firstSlot.equals(other.firstSlot))
			return false;
		if (secondSlot == null) {
			if (other.secondSlot != null)
				return false;
		} else if (!secondSlot.equals(other.secondSlot))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SlotPairObservation [language=" + language + " firstSlot=" + firstSlot + ", secondSlot="
				+ secondSlot + ", content=" + content + "]";
	}



}
