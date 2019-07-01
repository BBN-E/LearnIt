package com.bbn.akbc.neolearnit.common.targets.constraints.impl;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo.LanguageMatchInfo;
import com.bbn.akbc.neolearnit.common.targets.constraints.LanguageMatchConstraint;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.serif.theories.Mention;

public class MustNotCoreferConstraint extends LanguageMatchConstraint {

	@Override
	public boolean offForEvaluation() {
		return false;
	}

	@Override
	public boolean valid(LanguageMatchInfo match) {
		if(!match.getSlot0().isPresent() || !match.getSlot1().isPresent()) {
			return false;
		}
		if (match.getSlot0().get() instanceof Mention && match.getSlot1().get() instanceof Mention) {
			Mention m0 = (Mention)match.getSlot0().get();
			Mention m1 = (Mention)match.getSlot1().get();
			if (m0.entity(match.getDocTheory()).equals(m1.entity(match.getDocTheory()))) {
				return false;
			}
		}

		return !match.getSlot0().get().span().equals(match.getSlot1().get().span());
	}

	@Override
	public boolean valid(InstanceIdentifier instanceId, Seed seed) {
		return true;
	}

}
