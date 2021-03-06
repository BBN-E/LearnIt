package com.bbn.akbc.neolearnit.common.bilingual;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.io.SerifXMLLoader;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.EventMention;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.Spanning;
import com.bbn.serif.theories.SynNode;
import com.bbn.serif.theories.TokenSequence.Span;
import com.bbn.serif.theories.ValueMention;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BilingualDocTheory {

	@Override
	public String toString(){
		return this.sourceDocLanguage+"\t"+this.sourceDoc.docid().asString()+"\t"+this.targetDocLanguage+"\t"+this.targetDoc.docid().asString();
	}

	private final String sourceDocLanguage;
	private final DocTheory sourceDoc;
	private final String targetDocLanguage;
	private final DocTheory targetDoc;
	private final Map<Integer,TokenAlignmentTable> sentenceAlignments;

	private BilingualDocTheory(DocTheory sourceDoc, String sourceDocLanguage,
			DocTheory targetDoc, String targetDocLanguage,
			Map<Integer, TokenAlignmentTable> sentenceAlignments) {
		this.sourceDoc = sourceDoc;
		this.sourceDocLanguage = sourceDocLanguage;
		this.targetDoc = targetDoc;
		this.targetDocLanguage = targetDocLanguage;
		this.sentenceAlignments = sentenceAlignments;
	}

	public DocTheory getSourceDoc() {
		return sourceDoc;
	}

	public DocTheory getTargetDoc() {
		return targetDoc;
	}

	public String getSourceDocLanguage() {
		return sourceDocLanguage;
	}

	public String getTargetDocLanguage() {
		return targetDocLanguage;
	}

	public Map<Integer, TokenAlignmentTable> getSentenceAlignments() {
		return sentenceAlignments;
	}

	public static class SpanningPair {
		public Spanning slot0;
		public Spanning slot1;
		public SpanningPair(Spanning slot0, Spanning slot1) {
			this.slot0 = slot0;
			this.slot1 = slot1;
		}
	}

	public Optional<Spanning> tryAlignSpanning(final Spanning slot, final DocTheory alignedDoc, final SentenceTheory alignedSent) {
		final Iterable<? extends Spanning> slotCandidates = LearnItConfig.optionalParamTrue("relax_spanning_candidates")? getSpanningCandidatesRelaxed(slot): getSpanningCandidates(slot);	// candidates (Mention or ValueMention) from target sentence

		/*
		System.out.println("====================");
		System.out.println("target slot = [" + slot.span().text().utf16CodeUnits().toString() + "]");
		for(final Spanning span : slotCandidates) {
			if(span instanceof Mention) {
				final Mention m = (Mention)span;
				System.out.println("slotCandidate: " + this.sourceDoc.docid().asString() + " " + m.span().text().utf16CodeUnits().toString());
			}
		}
		*/

		Map<Span, Double> slotScoreCache = new HashMap<Span,Double>();

		double bestScore = 0.0;
		for(final Spanning s : slotCandidates) {
			final double alignmentScore = scoreAlignment(slot, s, slotScoreCache, false);
			if(alignmentScore == 0.0) {
                        	continue;
			}
			if(alignmentScore > bestScore) {
				bestScore = alignmentScore;
			}
		}

		List<Spanning> bestCandidates = new ArrayList<Spanning>();
		Spanning bestCandidate = null;
		for(final Spanning s : slotCandidates) {
			final double alignmentScore = scoreAlignment(slot, s, slotScoreCache, false);
			if((alignmentScore > 0) && (alignmentScore == bestScore)) {
				if(alignmentScore >= LearnItConfig.getDouble("min_alignment_overlap")) {
					bestCandidate = s;
					bestCandidates.add(s);
				}
			}
		}

                String spanType = "";
              	if(slot instanceof Mention) {
                        spanType = "Mention";
                } else if(slot instanceof ValueMention) {
			spanType = "ValueMention";
                } else if(slot instanceof EventMention) {
			spanType = "EventMention";
                } else if(slot instanceof SynNode) {
			spanType = "SynNode";
                }

		/*
		if(bestCandidate == null) {
			System.out.println("cannot find alignment for " + spanType + " " + this.sourceDoc.docid().asString() + " [" + slot.span().text().utf16CodeUnits().toString() + "] alignmentScore=" + bestScore + " threshold=" + LearnItConfig.getDouble("min_alignment_overlap"));
		} else {
			final StringBuffer s = new StringBuffer("");
			for(final Spanning cand : bestCandidates) {
				s.append(" ____ " + cand.span().text().utf16CodeUnits().toString() + " ");
			}
			System.out.println("Found alignment for " + spanType + "\t" + this.sourceDoc.docid().asString() + "\t[" + slot.span().text().utf16CodeUnits().toString() + "]\talignmentScore=" + bestScore + "\tthreshold=" + LearnItConfig.getDouble("min_alignment_overlap") + " aligned to\t" + s.toString());
                }
		System.out.println("====================");
		*/

		return Optional.fromNullable(bestCandidate);
	}

	public static BilingualDocTheory fromTabularPathLists(String docId, Map<String, String> pathEntries) throws IOException {
		String lang1 = LearnItConfig.getList("languages").get(0);
		String lang2 = LearnItConfig.getList("languages").get(1);
		return fromPaths(lang1, pathEntries.get(lang1), lang2, pathEntries.get(lang2), pathEntries.get("alignment"));
	}

	// Given slot0 & slot1 from source sentence (each MUST BE either Mention or ValueMention):
	// - from target sentence, generate target Mention or ValueMention spans
	// - find combination pairs that align approximately to slot0 & slot1
	// - from plausible aligned pairs, find the best pair
	// slot0 and slot1 MUST BE either Mention or ValueMention, else getSpanningCandidates will throw a RunTimeException
	// slot0, slot1 will be Mention or ValueMention from source sentence
	public Optional<SpanningPair> tryAlignSpanningPair(Spanning slot0, Spanning slot1,
			DocTheory alignedDoc, SentenceTheory alignedSent, Target target) {
		Iterable<? extends Spanning> slot0Candidates = LearnItConfig.optionalParamTrue("relax_spanning_candidates")? getSpanningCandidatesRelaxed(slot0): getSpanningCandidates(slot0);	// candidates (Mention or ValueMention) from target sentence
		Iterable<? extends Spanning> slot1Candidates = LearnItConfig.optionalParamTrue("relax_spanning_candidates")? getSpanningCandidatesRelaxed(slot1): getSpanningCandidates(slot1);	// candidates (Mention or ValueMention) from target sentence

		Map<Span, Double> slot0ScoreCache = new HashMap<Span,Double>();
		Map<Span, Double> slot1ScoreCache = new HashMap<Span,Double>();

		boolean same = slot0 == slot1;

		// find pairs of (target sentence's) slot0Candidates & slot1Candidates that align approximately to (source sentence's) slot0 & slot1
		List<SpanningPair> pairCandidates = new ArrayList<SpanningPair>();	// target pairs that approximately align to slot0 & slot1
		for (Spanning s0 : slot0Candidates) {
			if (scoreAlignment(slot0,s0,slot0ScoreCache,false) >= LearnItConfig.getDouble("min_alignment_overlap")) {
				for (Spanning s1 : slot1Candidates) {
					if ((same && s0 == s1) || (!same && s0 != s1)) {
						if (scoreAlignment(slot1,s1,slot1ScoreCache,false) >= LearnItConfig.getDouble("min_alignment_overlap")) {
							pairCandidates.add(new SpanningPair(s0,s1));
						}
					}
				}
			}
		}

		return bestSpanningPair(new SpanningPair(slot0,slot1), pairCandidates,
				slot0ScoreCache, slot1ScoreCache);
	}

	// if source is a Mention, get the set of Mention in target sentence
	// if source is a ValueMention, get the set of ValueMention in target sentence
	// if neither Mention nor ValueMention, RunTimeException
	protected Iterable<? extends Spanning> getSpanningCandidates(Spanning source) {
		if (source instanceof Mention) {
			List<Mention> results = new ArrayList<Mention>();
			Mention sourceM = (Mention)source;
			for (Mention m : targetDoc.sentenceTheory(source.span().sentenceIndex()).mentions()) {
				if (m.entityType().equals(sourceM.entityType()) && m.mentionType().equals(sourceM.mentionType())) {
					if (m.mentionType().equals(Mention.Type.NAME) || !(m.child().isPresent())) { // check that it's atomic
						results.add(m);
					}
				}
			}
			return results;
		} else if (source instanceof ValueMention) {
			List<ValueMention> results = new ArrayList<ValueMention>();
			ValueMention sourceVM = (ValueMention)source;
			for (ValueMention vm : targetDoc.sentenceTheory(source.span().sentenceIndex()).valueMentions()) {
				if (vm.type().equals(sourceVM.type())) {
					results.add(vm);
				}
			}
			return results;
		} else if (source instanceof EventMention) {
			List<EventMention> results = new ArrayList<EventMention>();
			EventMention sourceM = (EventMention)source;
			for (EventMention m : targetDoc.sentenceTheory(source.span().sentenceIndex()).eventMentions()) {
				Set<Symbol> eventTypes1 = new HashSet<Symbol>();
				Set<Symbol> eventTypes2 = new HashSet<Symbol>();
				for(EventMention.EventType eventType : sourceM.eventTypes())
					eventTypes1.add(eventType.eventType());
				for(EventMention.EventType eventType : m.eventTypes())
					eventTypes2.add(eventType.eventType());

				if (eventTypes1.equals(eventTypes2)) {
					results.add(m);
				}
			}
			return results;
		} else if (source instanceof SynNode) {
			SynNode srcSynNode = (SynNode) source;
			GetAllSynNode synNodeGetter = new GetAllSynNode();
			targetDoc.sentenceTheory(srcSynNode.span().sentenceIndex()).parse().preorderTraversal(synNodeGetter);
			return new ArrayList<>(synNodeGetter.getAllNode());
		} else {
			throw new RuntimeException("Invalid spanning type "+source);
		}
	}

	protected Iterable<? extends Spanning> getSpanningCandidatesRelaxed(Spanning source) {
		if (source instanceof Mention) {		// relaxed vs getSpanningCandidates
			List<Mention> results = new ArrayList<Mention>();
			Mention sourceM = (Mention)source;
			for (Mention m : targetDoc.sentenceTheory(source.span().sentenceIndex()).mentions()) {
				results.add(m);
			}
			return results;
		} else if (source instanceof ValueMention) {	// relaxed vs getSpanningCandidates
			List<ValueMention> results = new ArrayList<ValueMention>();
			ValueMention sourceVM = (ValueMention)source;
			for (ValueMention vm : targetDoc.sentenceTheory(source.span().sentenceIndex()).valueMentions()) {
				results.add(vm);
			}
			return results;
		} else if (source instanceof EventMention) {	// no change vs getSpanningCandidates
			List<EventMention> results = new ArrayList<EventMention>();
			EventMention sourceM = (EventMention)source;
			for (EventMention m : targetDoc.sentenceTheory(source.span().sentenceIndex()).eventMentions()) {
				Set<Symbol> eventTypes1 = new HashSet<Symbol>();
				Set<Symbol> eventTypes2 = new HashSet<Symbol>();
				for(EventMention.EventType eventType : sourceM.eventTypes())
					eventTypes1.add(eventType.eventType());
				for(EventMention.EventType eventType : m.eventTypes())
					eventTypes2.add(eventType.eventType());

				if (eventTypes1.equals(eventTypes2)) {
					results.add(m);
				}
			}
			return results;
		} else if (source instanceof SynNode) {		// no change vs getSpanningCandidates
			SynNode srcSynNode = (SynNode) source;
			GetAllSynNode synNodeGetter = new GetAllSynNode();
			targetDoc.sentenceTheory(srcSynNode.span().sentenceIndex()).parse().preorderTraversal(synNodeGetter);
			return new ArrayList<>(synNodeGetter.getAllNode());
		} else {
			throw new RuntimeException("Invalid spanning type "+source);
		}
	}

	protected boolean validSpanningPair(DocTheory doc, SentenceTheory sent,
			Spanning slot0, Spanning slot1, Target target) {
		return target.validMatch(MatchInfo.from(target, doc, sent, slot0, slot1), true);
	}

	// from 'candidates', find the SpanningPair that has the highest alignment score to 'source'
	protected Optional<SpanningPair> bestSpanningPair(SpanningPair source,
			Iterable<SpanningPair> candidates,
			Map<Span, Double> slot0ScoreCache,
			Map<Span, Double> slot1ScoreCache) {

		double bestScore = 0.0;
		SpanningPair best = null;
		for (SpanningPair target : candidates) {
			double score0 = scoreAlignment(source.slot0,target.slot0,slot0ScoreCache,false);
			double score1 = scoreAlignment(source.slot1,target.slot1,slot1ScoreCache,false);
			double score = score0+score1;
			if (score > bestScore) {
				best = target;
				bestScore = score;
			}
		}
		if (best == null) {
			/*
			System.out.println("ERROR EXTRACTING MENTION IN SENTENCE: "+source.span().sentenceIndex());
			System.out.println(sourceDoc.sentenceTheory(source.span().sentenceIndex()).tokenSequence().text());
			System.out.println(targetDoc.sentenceTheory(source.span().sentenceIndex()).tokenSequence().text());
			for (Spanning targ : candidates) {
				System.out.println(targ.span().tokenizedText());
				if (validSlot(targ,constraints)) {
					System.out.println("\tValid, Overlap: "+scoreAlignment(source,targ,true));
				}
			}*/
			//throw new RuntimeException("Couldn't find aligned valid span from "+numValid+" candidates");
			return Optional.absent();
		}
		return Optional.of(best);
	}

	// Get the token indices of 'source'
	// Go through the tokens of 'target' and get the set of aligned (source) token indices
	// overlap proportion is the score returned
	protected double scoreAlignment(Spanning source, Spanning target,
			Map<Span, Double> spanningScoreCache, boolean debug) {

		if (spanningScoreCache.containsKey(target.span())) {
			return spanningScoreCache.get(target.span());
		}

		Set<Integer> sourceTokens = new HashSet<Integer>();
		for (int i=source.span().startToken().index();i<=source.span().endToken().index();i++) {
			sourceTokens.add(i);
		}

		TokenAlignmentTable aligns = sentenceAlignments.get(source.span().sentenceIndex());
		Set<Integer> targetTokens = new HashSet<Integer>();
		for (int i=target.span().startToken().index();i<=target.span().endToken().index();i++) {
			targetTokens.addAll(aligns.getAlignedTokens(targetDocLanguage, sourceDocLanguage, i));
		}

		if (debug) {
			System.out.println("\tsource: "+source.span().tokenizedText()+" : "+sourceTokens);
			System.out.println("\ttarget: "+target.span().tokenizedText()+" : "+targetTokens);
			//System.out.println(aligns);
		}

		int overlap = Sets.intersection(sourceTokens, targetTokens).size();
		int total = Sets.union(sourceTokens, targetTokens).size();
		if (total == 0) return 0.0;
		double result = (double)overlap/total;

		if (result < LearnItConfig.getDouble("min_alignment_overlap")) {
			//System.out.println("Overlap 0 "+source.span().tokenizedText()+" - "+target.span().tokenizedText());
			return 0.0;
		}

		spanningScoreCache.put(target.span(),result);
		return result;
	}

	public static class Builder {
		private final String sourceDocLanguage;
		private DocTheory sourceDoc;
		private final String targetDocLanguage;
		private DocTheory targetDoc;
		private final Map<Integer,TokenAlignmentTable.Builder> sentenceAlignmentBuilders;

		public Builder(String sourceDocLanguage, String targetDocLanguage) {
			sentenceAlignmentBuilders = new HashMap<Integer,TokenAlignmentTable.Builder>();
			this.sourceDocLanguage = sourceDocLanguage;
			this.targetDocLanguage = targetDocLanguage;
		}

		public Builder setSourceDoc(DocTheory doc) {
			this.sourceDoc = doc;
			return this;
		}

		public Builder setTargetDoc(DocTheory doc) {
			this.targetDoc = doc;
			return this;
		}

		public Builder withAddedAlignment(int sentid, int sourceIdx, int targetIdx) {
			if (!sentenceAlignmentBuilders.containsKey(sentid)) {
				sentenceAlignmentBuilders.put(sentid, new TokenAlignmentTable.Builder());
			}
			sentenceAlignmentBuilders.get(sentid).addAlignment(sourceDocLanguage, targetDocLanguage, sourceIdx, targetIdx);
			return this;
		}

		public Builder withAddedMultiAlignment(int sentid, int sourceIdx, Iterable<Integer> targetIdxs) {
			if (!sentenceAlignmentBuilders.containsKey(sentid)) {
				sentenceAlignmentBuilders.put(sentid, new TokenAlignmentTable.Builder());
			}
			sentenceAlignmentBuilders.get(sentid).addMultiAlignment(sourceDocLanguage, targetDocLanguage, sourceIdx, targetIdxs);
			return this;
		}

		public BilingualDocTheory build() {
			ImmutableMap.Builder<Integer,TokenAlignmentTable> result = new ImmutableMap.Builder<Integer,TokenAlignmentTable>();
			for (Integer sentid : sentenceAlignmentBuilders.keySet()) {
				result.put(sentid,sentenceAlignmentBuilders.get(sentid).build());
			}
			return new BilingualDocTheory(sourceDoc,sourceDocLanguage,targetDoc,targetDocLanguage,result.build());
		}

	}

	public static BilingualDocTheory fromPaths(String sourceLang, String sourcePath, String targetLang, String targetPath, String alignmentPath) throws IOException {
		Builder builder = new Builder(sourceLang,targetLang);
//		System.out.println("Loading "+sourcePath);
		DocTheory sourceDoc = SerifXMLLoader.fromStandardACETypes().loadFrom(new File(sourcePath));
		builder.setSourceDoc(sourceDoc);
//		System.out.println("Loading "+targetPath);
		DocTheory targetDoc = SerifXMLLoader.fromStandardACETypes().loadFrom(new File(targetPath));
		builder.setTargetDoc(targetDoc);

		BufferedReader br = new BufferedReader(new FileReader(new File(alignmentPath)));
		String line;
		int sent = 0;
		while ((line = br.readLine()) != null) {

			for (String alignment : line.trim().split(" ")) {

				String[] pieces = alignment.split(":");
				if(pieces.length == 2){
					if(LearnItConfig.optionalParamTrue("should_reverse_alignment_table")){
						builder.withAddedAlignment(sent, Integer.parseInt(pieces[1]), Integer.parseInt(pieces[0]));
					}
					else{
						builder.withAddedAlignment(sent, Integer.parseInt(pieces[0]), Integer.parseInt(pieces[1]));
					}
				}
				else{
					try{
						throw new RuntimeException("Unparseable alignment file " + alignmentPath);
					}
					catch (Exception e){
						System.err.println("Exception: Unparseable alignment file "+alignmentPath+" .Ignore until MT have a fix");
					}
				}

			}
			sent++;
		}
		br.close();

		return builder.build();
	}

	public static BilingualDocTheory fromVariantStyleString(String line1, String line2) throws IOException {
		//   Sample Lines:
		//CBS20001021.1000.0734.segment English source /nfs/mercury-04/u41/ChineseACE/serifxml/english/CBS20001021.1000.0734.segment.xml,Chinese source /nfs/mercury-04/u41/ChineseACE/serifxml/chinese/CBS20001021.1000.0734.segment.xml
		//Chinese:source=English:source /nfs/mercury-04/u41/ChineseACE/alignments/english/CBS20001021.1000.0734.segment.alignment
		try {
			String[] docs = line1.split(",");
			String sourceDoc = docs[1].split(" ")[2];
			String targetDoc = docs[0].split(" ")[3];
			String alignmentFile = line2.split(" ")[1];
			return fromPaths("chinese",sourceDoc,"english",targetDoc,alignmentFile);
		} catch (IndexOutOfBoundsException ex) {
			ex.printStackTrace();
			System.out.println("Failed to parse input string: \n"+line1+"\n"+line2);
			throw new RuntimeException("Error reading input bilingual file");
		}


	}

	public static BilingualDocTheory fromRegularString(String input) throws IOException {
		//english /nfs/mercury-04/u41/ChineseGigawordV5/data/serifxml/english/AFP_CMN/200411/AFP-CMN-20041104.0020.xml.xml chinese /nfs/mercury-04/u41/ChineseGigawordV5/data/serifxml/chinese/AFP_CMN/200411/AFP-CMN-20041104.0020.xml.xml /nfs/mercury-04/u41/ChineseGigawordV5/data/alignments/english/AFP_CMN/200411/AFP-CMN-20041104.0020.xml.alignment
		String[] parts = input.trim().split(" ");
		return fromPaths(parts[2],parts[3],parts[0],parts[1],parts[4]);
//		return fromPaths(parts[0],parts[1],parts[2],parts[3],parts[4]);
	}

	public static class GetAllSynNode implements SynNode.PreorderVisitor {
		final Set<SynNode> buf;

		public GetAllSynNode() {
			buf = new HashSet<>();
		}

		@Override
		public boolean visitChildren(SynNode synNode) {
			buf.add(synNode);
			return true;
		}

		public Set<SynNode> getAllNode() {
			return buf;
		}
	}

}
