package com.bbn.akbc.neolearnit.common.targets.constraints;

import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo.LanguageMatchInfo;
import com.bbn.serif.theories.Spanning;

public abstract class SlotPairMatchConstraint extends LanguageMatchConstraint {

	public SlotPairMatchConstraint(String language) {
		super(language);
	}

	public SlotPairMatchConstraint() {
		super();
	}

	@Override
	public boolean valid(LanguageMatchInfo match) {
		if(!match.getSlot0().isPresent() || !match.getSlot1().isPresent()) {
			return false;
		}
		else {
			return valid(match.getSlot0().get(), match.getSlot1().get());
		}
	}

	public abstract boolean valid(Spanning slot0, Spanning slot1);

}
