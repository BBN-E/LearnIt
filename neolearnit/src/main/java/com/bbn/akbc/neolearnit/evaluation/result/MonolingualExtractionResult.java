package com.bbn.akbc.neolearnit.evaluation.result;

import java.io.Writer;
import java.util.Map;

import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.serif.theories.DocTheory;
import com.google.common.collect.Multimap;

public class MonolingualExtractionResult extends ExtractionResult<DocTheory> {

	protected MonolingualExtractionResult(
			Map<String, DocTheory> documents, Multimap<MatchInfo,LearnitPattern> matches, Multimap<String,MatchInfo> matchesById) {
		super(documents, matches, matchesById);
	}

	@Override
	public void writeOutput(Writer writer) {
		// TODO Auto-generated method stub

	}

	public static class Builder extends ExtractionResult.Builder<DocTheory> {

		@Override
		public MonolingualExtractionResult build() {
			return new MonolingualExtractionResult(documents,matches,matchesById);
		}

	}


}
