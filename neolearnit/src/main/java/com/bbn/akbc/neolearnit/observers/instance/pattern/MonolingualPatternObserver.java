package com.bbn.akbc.neolearnit.observers.instance.pattern;

import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo.LanguageMatchInfo;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;

public abstract class MonolingualPatternObserver extends AbstractPatternObserver {

	protected final String language;

	public String getLanguage() {
		return language;
	}

	protected MonolingualPatternObserver(InstanceToPatternMapping.Builder recorder,
			String language) {
		super(recorder);
		this.language = language;
	}

	@Override
	public void observe(MatchInfo match) {
		if (match.getAvailableLanguages().contains(language)) {
			observe(match, match.getLanguageMatch(language));
		}
	}

	public abstract void observe(MatchInfo match, LanguageMatchInfo languageMatch);

}
