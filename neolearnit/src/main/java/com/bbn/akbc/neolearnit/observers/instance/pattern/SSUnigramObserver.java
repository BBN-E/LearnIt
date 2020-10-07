package com.bbn.akbc.neolearnit.observers.instance.pattern;

import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;
import com.bbn.akbc.neolearnit.observations.pattern.SSUnigram;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.Token;
import com.google.inject.Inject;

public class SSUnigramObserver extends MonolingualPatternObserver {

    @Inject
    public SSUnigramObserver(InstanceToPatternMapping.Builder recorder, String language) {
        super(recorder, language);
    }

    @Override
    public void observe(MatchInfo match, MatchInfo.LanguageMatchInfo languageMatch) {
        SentenceTheory sentenceTheory = languageMatch.getSentTheory();
        for (Token token : sentenceTheory.tokenSpan().tokens(languageMatch.docTheory())) {
            this.record(match, new SSUnigram(token.symbol()));
        }
    }
}
