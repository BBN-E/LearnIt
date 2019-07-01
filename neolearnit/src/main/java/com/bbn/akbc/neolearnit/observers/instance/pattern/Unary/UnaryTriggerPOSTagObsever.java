package com.bbn.akbc.neolearnit.observers.instance.pattern.Unary;

import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;
import com.bbn.akbc.neolearnit.observations.pattern.UnaryPattern.TriggerPOSTagPattern;
import com.bbn.akbc.neolearnit.observers.instance.pattern.MonolingualPatternObserver;
import com.bbn.serif.theories.EventMention;
import com.bbn.serif.theories.SynNode;
import com.google.common.base.Optional;

public class UnaryTriggerPOSTagObsever extends MonolingualPatternObserver {
    public UnaryTriggerPOSTagObsever(InstanceToPatternMapping.Builder recorder,
                                     String language){
        super(recorder,language);
    }
    @Override
    public void observe(MatchInfo match, MatchInfo.LanguageMatchInfo languageMatch) {
        if(languageMatch.getSlot1().isPresent())return;

        final Optional<SynNode> anchor = UnaryPropObserver.getNode(languageMatch.getSlot0().get(),languageMatch.docTheory());
        if(anchor.isPresent() && (languageMatch.getSlot0().get() instanceof EventMention)){
            SynNode head = anchor.get().head();
            final String POSTag = head.headPOS().asString();
            String trigger = head.span().originalText().content().utf16CodeUnits();
            trigger = trigger.toLowerCase();
            // TODO: Maybe lemmalized?
            this.record(match,new TriggerPOSTagPattern(trigger,POSTag));
        }

    }
}
