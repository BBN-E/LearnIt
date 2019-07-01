package com.bbn.akbc.neolearnit.common.targets.constraints;

import com.bbn.serif.patterns.Pattern;

public interface SlotMatchConstraint extends MatchConstraint {

	public int getSlot();
	public Pattern.Builder setBrandyConstraints(Pattern.Builder builder);

}
