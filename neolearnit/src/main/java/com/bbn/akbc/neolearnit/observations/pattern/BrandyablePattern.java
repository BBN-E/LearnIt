package com.bbn.akbc.neolearnit.observations.pattern;

import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.observations.pattern.restriction.Restriction;
import com.bbn.serif.patterns.Pattern;

public interface BrandyablePattern {

	public Pattern convertToBrandy(String factType, Target target, Iterable<Restriction> restrictions);

}
