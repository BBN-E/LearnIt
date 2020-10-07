package com.bbn.akbc.neolearnit.observers.instance.pattern.Unary;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;
import com.bbn.akbc.neolearnit.observations.pattern.UnaryPattern.HeadWordPOSTagPattern;
import com.bbn.akbc.neolearnit.observers.instance.pattern.MonolingualPatternObserver;
import com.bbn.serif.theories.SynNode;

import java.util.List;

public class UnaryHeadWordPOSTagObsever extends MonolingualPatternObserver {
    public UnaryHeadWordPOSTagObsever(InstanceToPatternMapping.Builder recorder,
                                      String language) {
        super(recorder,language);
    }
    @Override
    public void observe(MatchInfo match, MatchInfo.LanguageMatchInfo languageMatch) {
        if(languageMatch.getSlot1().isPresent())return;

        final List<SynNode> nodes = InstanceIdentifier.getNode(languageMatch.getSlot0().get(), languageMatch.docTheory());
        for (SynNode node : nodes) {
            SynNode head = node.headPreterminal();
            final String POSTag = head.headPOS().asString();
            String trigger = head.span().originalText().content().utf16CodeUnits();
            trigger = trigger.toLowerCase();

            HeadWordPOSTagPattern headWordPOSTagPattern = new HeadWordPOSTagPattern(trigger, POSTag);
//            System.out.println("HeadWordPOSTagPattern:\t" + headWordPOSTagPattern.toIDString());
            // TODO: Maybe lemmalized?
            this.record(match, headWordPOSTagPattern);
        }

    }
}
