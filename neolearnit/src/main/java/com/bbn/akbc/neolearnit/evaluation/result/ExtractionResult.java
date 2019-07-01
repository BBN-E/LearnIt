package com.bbn.akbc.neolearnit.evaluation.result;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfoDisplay;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public abstract class ExtractionResult<T> {

	protected final Map<String,T> documents;
	protected final Multimap<MatchInfo,LearnitPattern> matches;
	protected final Multimap<String,MatchInfo> matchesById;

	protected ExtractionResult(Map<String,T> documents, Multimap<MatchInfo,LearnitPattern> matches, Multimap<String,MatchInfo> matchesById) {
		this.documents = documents;
		this.matches = matches;
		this.matchesById = matchesById;
	}

	public Set<String> getDocIds() {
		return documents.keySet();
	}

	public T getDocument(String docid) {
		return documents.get(docid);
	}

	public Collection<MatchInfo> getMatches(String docid) {
		return matchesById.get(docid);
	}

	public Multimap<MatchInfoDisplay,LearnitPattern> getAllDisplays(Optional<Map<Symbol, Symbol>> chiEngNameMapping) {
		Multimap<MatchInfoDisplay,LearnitPattern> displayMap = HashMultimap.create();
		for (MatchInfo matchInfo : matches.keySet()) {
			displayMap.putAll(MatchInfoDisplay.fromMatchInfo(matchInfo, chiEngNameMapping),matches.get(matchInfo));
		}
		return displayMap;
	}

	public abstract void writeOutput(Writer writer) throws IOException;

	public static abstract class Builder<T> {

		protected final Map<String,T> documents;
		protected final Multimap<MatchInfo,LearnitPattern> matches;
		protected final Multimap<String,MatchInfo> matchesById;

		public Builder() {
			this.documents = new HashMap<String,T>();
			this.matches = HashMultimap.create();
			this.matchesById = HashMultimap.create();
		}

		public Builder<T> withDocument(String docid, T document) {
			documents.put(docid, document);
			return this;
		}

		public Builder<T> withMatches(Multimap<MatchInfo,LearnitPattern> matches) {
			for (MatchInfo match : matches.keySet()) withMatch(match, matches.get(match));
			return this;
		}

		public Builder<T> withMatch(MatchInfo match, Collection<LearnitPattern> patterns) {
			matches.putAll(match, patterns);
			matchesById.put(match.getPrimaryLanguageMatch().getDocTheory().docid().toString(), match);
			return this;
		}

		public abstract ExtractionResult<T> build();


	}

}
