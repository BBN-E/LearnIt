package com.bbn.akbc.neolearnit.observers.instance.pattern.Unary;

import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;
import com.bbn.akbc.neolearnit.observations.pattern.UnaryPattern.TypePattern;
import com.bbn.akbc.neolearnit.observers.instance.pattern.MonolingualPatternObserver;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.Spanning;
import com.bbn.serif.theories.ValueMention;

public class UnaryTypeObserver extends MonolingualPatternObserver {

    public UnaryTypeObserver(InstanceToPatternMapping.Builder recorder,
                             String language) {
        super(recorder, language);
    }

    @Override
    public void observe(MatchInfo match, MatchInfo.LanguageMatchInfo languageMatch) {
        if (languageMatch.getSlot1().isPresent()) return;

        Spanning spanning = languageMatch.getSlot0().get();
        if (spanning instanceof Mention) {
            Mention mention = (Mention) spanning;
            this.record(match, new TypePattern(mention.entityType().name().asString()));
        } else if (spanning instanceof ValueMention) {
            ValueMention valueMention = (ValueMention) spanning;
            this.record(match, new TypePattern(valueMention.fullType().name().asString()));
        }
    }
}
