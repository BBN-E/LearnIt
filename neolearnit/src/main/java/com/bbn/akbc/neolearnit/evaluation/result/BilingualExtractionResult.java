package com.bbn.akbc.neolearnit.evaluation.result;

import com.bbn.bue.common.StringUtils;
import com.bbn.akbc.neolearnit.common.bilingual.BilingualDocTheory;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo.LanguageMatchInfo;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.Entity;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.Spanning;
import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BilingualExtractionResult extends ExtractionResult<BilingualDocTheory> {

	protected BilingualExtractionResult(
			Map<String, BilingualDocTheory> documents, Multimap<MatchInfo,LearnitPattern> matches, Multimap<String,MatchInfo> matchesById) {
		super(documents, matches, matchesById);
	}

	@Override
	public void writeOutput(Writer writer) throws IOException {

		for (String docid : documents.keySet()) {
			writeDocOutput(writer, documents.get(docid), matchesById.get(docid));
		}

	}

	private Spanning getBestSpanning(Spanning spanning, DocTheory dt) {
		if (spanning instanceof Mention) {
			Mention m  = (Mention)spanning;
			Optional<Entity> e = m.entity(dt);
			if (e.isPresent()) {
				return e.get().representativeMention().mention();
			} else {
				return spanning;
			}
		} else {
			return spanning;
		}
	}

	private Collection<Spanning> getChildSpans(Spanning spanning, DocTheory dt) {
		if (spanning instanceof Mention) {
			Mention m  = (Mention)spanning;
			Optional<Entity> e = m.entity(dt);
			if (e.isPresent()) {
				return Lists.<Spanning>newArrayList(e.get().mentions());
			} else {
				return Lists.newArrayList(spanning);
			}
		} else {
			return Lists.newArrayList(spanning);
		}
	}

	private String getSentence(SentenceTheory sent, List<Spanning> spans, Multimap<Spanning,Spanning> children) {
		StringBuilder builder = new StringBuilder();

		for (int i=0;i<sent.tokenSequence().size();i++) {
			for (int j=0;j<spans.size();j++) {
				Spanning span = spans.get(j);
				if (span == null) continue;
				for (Spanning childSpan : children.get(span)) {
					if (childSpan.span().sentenceIndex() == sent.index()) {
						if (childSpan.span().startToken().index() == i) {
							builder.append("<"+j+">");
						}
					}
				}
			}

			builder.append(sent.tokenSequence().token(i).text());

			for (int j=spans.size()-1;j>=0;j--) {
				Spanning span = spans.get(j);
				for (Spanning childSpan : children.get(span)) {
					if (span == null) continue;
					if (childSpan.span().sentenceIndex() == sent.index()) {
						if (childSpan.span().endToken().index() == i) {
							builder.append("</"+j+">");
						}
					}
				}
			}

			builder.append(" ");
		}
		return builder.toString();
	}

	private String writeIndividuals(List<Spanning> bestSpans, Multimap<Spanning,Spanning> childSpans) {
		StringBuilder builder = new StringBuilder();
		for (int i=0; i<bestSpans.size();i++) {
			if (bestSpans.get(i) == null) continue;
			builder.append(i+" - "+bestSpans.get(i).span().tokenizedText()+"\n-----------------------------------\n");
			List<String> childSpanStrings = new ArrayList<String>();
			for (Spanning child : childSpans.get(bestSpans.get(i))) {
				childSpanStrings.add("\t"+child.span().tokenizedText());
			}
			builder.append(StringUtils.NewlineJoin.apply(childSpanStrings));
			builder.append("\n\n");
		}
		return builder.toString();
	}

	private void writeDocOutput(Writer writer, BilingualDocTheory doc, Collection<MatchInfo> matches) throws IOException {

		// don't write out empty docs
		if (matches.size() == 0) {
			writer.write("NO MATCHES");
			return;
		}

		writer.write("DOCUMENT: "+doc.getSourceDoc().docid().toString()+"\n\n");

		//get the spannings that show up in the matches and the best spanning and child spannings
		Map<String,List<Spanning>> bestSpanningsByLanguage = new HashMap<String,List<Spanning>>();

		bestSpanningsByLanguage.put(doc.getSourceDocLanguage(), new ArrayList<Spanning>());
		bestSpanningsByLanguage.put(doc.getTargetDocLanguage(), new ArrayList<Spanning>());

		Map<Spanning,Spanning> bestSpans = new HashMap<Spanning,Spanning>();
		Multimap<Spanning,Spanning> childSpans = HashMultimap.<Spanning,Spanning>create();

		for (MatchInfo match : matches) {
			for (String lang : match.getAvailableLanguages()) {

				LanguageMatchInfo lmi = match.getLanguageMatch(lang);
				Spanning best0 = getBestSpanning(lmi.getSlot0().get(), lmi.getDocTheory());
				bestSpans.put(lmi.getSlot0().get(), best0);
				Spanning best1 = getBestSpanning(lmi.getSlot1().get(), lmi.getDocTheory());
				bestSpans.put(lmi.getSlot1().get(), best1);

				if (!bestSpanningsByLanguage.get(lang).contains(best0)) {
					bestSpanningsByLanguage.get(lang).add(best0);
					for (Spanning child : getChildSpans(best0, lmi.getDocTheory())) {
						childSpans.put(best0, child);
					}
				}
				if (!bestSpanningsByLanguage.get(lang).contains(best1)) {
					bestSpanningsByLanguage.get(lang).add(best1);
					for (Spanning child : getChildSpans(best1, lmi.getDocTheory())) {
						childSpans.put(best1, child);
					}
				}
			}
			while (bestSpanningsByLanguage.get(doc.getTargetDocLanguage()).size() < bestSpanningsByLanguage.get(doc.getSourceDocLanguage()).size()) {
				bestSpanningsByLanguage.get(doc.getTargetDocLanguage()).add(null);
			}
		}

		// write the doc sentences
		for (int i=0;i<doc.getSourceDoc().sentenceTheories().size();i++) {
			writer.write("===== Sentence "+i+" =====\n");
			writer.write(getSentence(doc.getSourceDoc().sentenceTheory(i),
					bestSpanningsByLanguage.get(doc.getSourceDocLanguage()),
					childSpans)+"\n");
			writer.write(getSentence(doc.getTargetDoc().sentenceTheory(i),
					bestSpanningsByLanguage.get(doc.getTargetDocLanguage()),
					childSpans)+"\n");
		}

		// write the individuals
		writer.write("\n\n"+doc.getSourceDocLanguage().toUpperCase()+" RELATION ENTITIES:\n");
		writer.write(writeIndividuals(bestSpanningsByLanguage.get(doc.getSourceDocLanguage()), childSpans));
		writer.write("\n\n"+doc.getTargetDocLanguage().toUpperCase()+" RELATION ENTITIES:\n");
		writer.write(writeIndividuals(bestSpanningsByLanguage.get(doc.getTargetDocLanguage()), childSpans));

		// write the relations
		writer.write("\n\nFOUND RELATIONS:\n");
		Set<String> results = new HashSet<String>();
		for (MatchInfo match : matches) {
			String relationName = match.getTarget().getName();
			String slot0 = "";
			String slot1 = "";
			for (String lang : match.getAvailableLanguages()) {
				LanguageMatchInfo lmi = match.getLanguageMatch(lang);
				if (slot0 == "") {
					slot0 += bestSpanningsByLanguage.get(doc.getSourceDocLanguage()).indexOf(bestSpans.get(lmi.getSlot0()))+": ";
				} else {
					slot0 += "/";
				}
				slot0 += bestSpans.get(lmi.getSlot0()).span().tokenizedText();

				if (slot1 == "") {
					slot1 += bestSpanningsByLanguage.get(doc.getSourceDocLanguage()).indexOf(bestSpans.get(lmi.getSlot1()))+": ";
				} else {
					slot1 += "/";
				}
				slot1 += bestSpans.get(lmi.getSlot1()).span().tokenizedText();
			}
			results.add(relationName+"( "+slot0+" , "+slot1+" )");
		}
		writer.write(StringUtils.NewlineJoin.apply(results)+"\n");

	}


	public static class Builder extends ExtractionResult.Builder<BilingualDocTheory> {

		@Override
		public BilingualExtractionResult build() {
			return new BilingualExtractionResult(documents,matches,matchesById);
		}

	}

}
