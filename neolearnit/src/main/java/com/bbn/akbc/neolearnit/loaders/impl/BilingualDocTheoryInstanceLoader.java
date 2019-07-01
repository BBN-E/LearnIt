package com.bbn.akbc.neolearnit.loaders.impl;

import com.bbn.akbc.neolearnit.common.bilingual.BilingualDocTheory;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.loaders.AbstractInstanceLoader;
import com.bbn.akbc.neolearnit.observers.InstanceObservers;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.Spanning;
import com.bbn.serif.theories.ValueMention;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import sun.security.jca.GetInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BilingualDocTheoryInstanceLoader extends AbstractInstanceLoader<BilingualDocTheory> {

    private final Target target;
    private final Map<String, BilingualDocTheory> bidocTheoryCache;

    @Inject
    public BilingualDocTheoryInstanceLoader(Target target, InstanceObservers binaryObservers, InstanceObservers unaryObservers, boolean evaluating) {
        super(binaryObservers, unaryObservers, evaluating);
        Preconditions.checkNotNull(target);
        this.target = target;
        bidocTheoryCache = new ConcurrentHashMap<String, BilingualDocTheory>();
    }

    @Inject
    public BilingualDocTheoryInstanceLoader(Target target, InstanceObservers binaryObservers, InstanceObservers unaryObservers) {
        super(binaryObservers, unaryObservers, false);
        Preconditions.checkNotNull(target);
        this.target = target;
        bidocTheoryCache = new ConcurrentHashMap<String, BilingualDocTheory>();
    }

    public Map<String, BilingualDocTheory> getLoadedBilingualDocTheories() {
        return this.bidocTheoryCache;
    }

    @Override
    public void load(final BilingualDocTheory bidoc) {
        final DocTheory sourceDoc = bidoc.getSourceDoc();

        if (evaluating) {
            docTheoryCache.put(sourceDoc.docid().toString().replace(".segment", ""), sourceDoc);
            bidocTheoryCache.put(sourceDoc.docid().toString().replace(".segment", ""), bidoc);
        }

        for (final SentenceTheory sourceSt : sourceDoc.sentenceTheories()) {
            // TODO : this needs to be better handled, perhaps by adding a boolean variable to Target

            for (final Spanning slot : sourceSt.mentions()) {
                MatchInfo match = MatchInfo.from(target, bidoc, sourceSt, slot);
                if (target.validMatch(match, this.isEvaluating())) {
                    this.handleUnaryMatch(match);
                }
            }


            List<Spanning> sourceAllSpans = new ArrayList<Spanning>();
            for (Mention m : sourceSt.mentions().asList())
                sourceAllSpans.add(m);
            for (ValueMention m : sourceSt.valueMentions().asList())
                sourceAllSpans.add(m);
            // Iterable<Spanning> sourceAllSpans = Iterables.concat(sourceSt.mentions(), sourceSt.valueMentions());
            for (Spanning sourceSlot0 : sourceAllSpans) {
                for (Spanning sourceSlot1 : sourceAllSpans) {
                    MatchInfo match = MatchInfo.from(target, bidoc, sourceSt, sourceSlot0, sourceSlot1);
                    if (target.validMatch(match, this.isEvaluating())) {
                        this.handleBinaryMatch(match);
                    }
                }

            }
        }

    }

    public void load(final ImmutableList<MatchInfo> relationMentions) {
        for (final MatchInfo relationMention : relationMentions) {
            final MatchInfo newEg = relationMention.copyWithTarget(target);
            if (target.validMatch(newEg, this.isEvaluating())) {
                this.handleBinaryMatch(newEg);
            }
        }
    }

}
