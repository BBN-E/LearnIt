package com.bbn.akbc.neolearnit.observations.pattern;

import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.observations.pattern.restriction.Restriction;
import com.bbn.serif.patterns.Pattern;

public interface BrandyablePattern {

	public Pattern convertToBrandy(String factType, Target target, Iterable<Restriction> restrictions);

	// Implement this interface:
	//     LearnitPattern fromBrandyString(String sexp);

	// Three target types: (1) unary event, (2) binary event-event relation, (3) binary event-argument/mention/valuemention relation
	// Patterns in use:
	// - binary
	//  - BetweenSlotsPattern
	//  - PropPattern
	// - unary
	//  - PropPattern
	//  - HeadWordPOSTagPattern
	//  - NounPhrase pattern
	//  - BeforeAfterSlotsPattern

	public class NonConvertibleException extends RuntimeException {
		public NonConvertibleException(String message) {
			super(message);
		}
	};

}
