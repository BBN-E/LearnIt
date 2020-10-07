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
        int result = 0;
		result = prime * result
				+ ((language == null) ? 0 : language.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		MonolingualPattern other = (MonolingualPattern) obj;
		if (language == null) {
            return other.language == null;
        } else return language.equals(other.language);
    }

	@Override
	public String toString() {
		return "MonolingualContextObservation [language=" + language + "]";
	}
}
