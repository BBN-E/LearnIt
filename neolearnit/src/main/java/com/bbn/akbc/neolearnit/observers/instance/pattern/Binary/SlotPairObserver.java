package com.bbn.akbc.neolearnit.observers.instance.pattern.Binary;

import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo.LanguageMatchInfo;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;
import com.bbn.akbc.neolearnit.observations.pattern.BetweenSlotsContent;
import com.bbn.akbc.neolearnit.observations.pattern.BetweenSlotsPattern;
import com.bbn.akbc.neolearnit.observations.pattern.RegexableContent;
import com.bbn.akbc.neolearnit.observers.instance.pattern.MonolingualPatternObserver;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.Spanning;

public abstract class SlotPairObserver extends MonolingualPatternObserver {

	protected SlotPairObserver(InstanceToPatternMapping.Builder recorder, String language) {
		super(recorder, language);
	}

	@Override
	public void observe(MatchInfo parent, LanguageMatchInfo instance) {

		// collect content in both slot orderings (only one will be in the correct orientation)

		for (BetweenSlotsContent<RegexableContent> obs : observe(instance.getSentTheory(),instance.getSlot0().get(),instance.getSlot1().get())) {

			//record regular
			this.record(parent, new BetweenSlotsPattern(language,0,1,obs));
			//record symmetric
			//this.record(instance, new BetweenSlotsObservation(language,0,0,obs));
		}

		for (BetweenSlotsContent<RegexableContent> obs : observe(instance.getSentTheory(),instance.getSlot1().get(),instance.getSlot0().get())) {

			//record regular
			this.record(parent, new BetweenSlotsPattern(language,1,0,obs));
			//record symmetric
			//this.record(instance, new BetweenSlotsObservation(language,0,0,obs));
		}

	}

	public abstract Iterable<BetweenSlotsContent<RegexableContent>> observe(SentenceTheory sentence, Spanning slotA,
			Spanning slotB);
}
