package com.bbn.akbc.neolearnit.observations.pattern;

import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class MonolingualPattern extends LearnitPattern {
	@JsonProperty
	protected final String language;

	public String getLanguage() {
		return language;
	}

	public MonolingualPattern(String language) {
		this.language = language;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((language == null) ? 0 : language.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		MonolingualPattern other = (MonolingualPattern) obj;
		if (language == null) {
			if (other.language != null)
				return false;
		} else if (!language.equals(other.language))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "MonolingualContextObservation [language=" + language + "]";
	}
}
