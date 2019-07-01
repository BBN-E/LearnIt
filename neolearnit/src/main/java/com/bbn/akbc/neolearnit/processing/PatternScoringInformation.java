package com.bbn.akbc.neolearnit.processing;

import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.processing.patternpruning.PatternPruningInformation.PatternPartialInfo;

public interface PatternScoringInformation extends PartialInfoWithCounts<LearnitPattern> {

	public PatternPartialInfo getPartialInfo(LearnitPattern pattern);

	public boolean hasPartialInfo(LearnitPattern pattern);

}
