package com.bbn.akbc.neolearnit.common.targets.constraints;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo.LanguageMatchInfo;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collection;

public abstract class LanguageMatchConstraint implements MatchConstraint {

	@JsonProperty
	private final String language;

	/**
	 * Constructor specifying the language for which the constraint applies
	 * Empty for primary language
	 * "ALL" for constraints on all available languages
	 * @param language
	 */
	public LanguageMatchConstraint(String language) {
		this.language = language;
	}

	public LanguageMatchConstraint() {
		this.language = "";
	}

	@Override
	public final boolean valid(MatchInfo match) {
		if (this.language.equals("ALL")) {
			for (String lang : match.getAvailableLanguages()) {
				if (!valid(match.getLanguageMatch(lang))) {
					return false;
				}
			}
			return true;
		} else if (this.language.equals("")) {
			return valid(match.getPrimaryLanguageMatch());
		} else {
			return valid(match.getLanguageMatch(language));
		}
	}

	public abstract boolean valid(LanguageMatchInfo match);

	@Override
	public final boolean valid(InstanceIdentifier instanceId, Collection<Seed> seeds, Target t) {
		for (Seed seed : seeds) {
			if (seed.getLanguage().equalsIgnoreCase(language) || this.language.equals("")) {
                if (t != null)
                    return valid(instanceId, seed.withProperText(t.getSlot(0).useBestName(), t.getSlot(1).useBestName()));
                else
                    return valid(instanceId, seed);
            }
		}
		return true;
	}

	public abstract boolean valid(InstanceIdentifier instanceId, Seed seed);

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((language == null) ? 0 : language.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LanguageMatchConstraint other = (LanguageMatchConstraint) obj;
		if (language == null) {
			if (other.language != null)
				return false;
		} else if (!language.equals(other.language))
			return false;
		return true;
	}

}
