package com.bbn.akbc.neolearnit.common.matchinfo;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.bilingual.BilingualDocTheory;
import com.bbn.akbc.neolearnit.common.bilingual.BilingualDocTheory.SpanningPair;
import com.bbn.akbc.neolearnit.common.bilingual.TokenAlignmentTable;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.serif.theories.*;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MatchInfo {

	private final Optional<Target> target; // if we treat MatchInfo as an instance/example, then it might not have been matched by any Target relation type
	private final String primaryLanguage;
	private final Map<String,LanguageMatchInfo> languageMatches;
	private final TokenAlignmentTable alignments;
	private final String debugText;
	private final HashCode cachedSHA1Hash;

	private MatchInfo(Target target,
			String primaryLanguage,
			Map<String, LanguageMatchInfo> languageMatches,
			TokenAlignmentTable alignments) {
		this.target = Optional.fromNullable(target);
		this.primaryLanguage = primaryLanguage;
		this.languageMatches = languageMatches;
		this.alignments = alignments;
		this.debugText = "";
		this.cachedSHA1Hash = computeSHA1Hash();
	}

	private MatchInfo(Target target,
			String primaryLanguage,
			Map<String, LanguageMatchInfo> languageMatches,
			TokenAlignmentTable alignments,
			String debugText) {
		this.target = Optional.fromNullable(target);
		this.primaryLanguage = primaryLanguage;
		this.languageMatches = languageMatches;
		this.alignments = alignments;
		this.debugText = debugText;
		this.cachedSHA1Hash = computeSHA1Hash();
	}

	public MatchInfo copyWithTarget(final Target target) {
      return new MatchInfo(target, primaryLanguage, languageMatches, alignments, debugText);
    }

	public static MatchInfo from(Target target, DocTheory doc, SentenceTheory sent,
			Spanning slot0, Spanning slot1) {

		return from(target,doc,sent,slot0,slot1,"");
	}

	public static MatchInfo from(Target target, DocTheory doc, SentenceTheory sent,
			Spanning slot0) {

		return from(target,doc,sent,slot0,"");
	}

	public static MatchInfo from(Target target, DocTheory doc, SentenceTheory sent,
			Spanning slot0, Spanning slot1, String debugText) {
		String primaryLanguage = LearnItConfig.getList("languages").get(0);

		return new MatchInfo(target, primaryLanguage,
				ImmutableMap.of(primaryLanguage, new LanguageMatchInfo(primaryLanguage, doc, sent, slot0, slot1)),
				null, debugText);
	}

	public static MatchInfo from(Target target, DocTheory doc, SentenceTheory sent,
			Spanning slot0, String debugText) {
		String primaryLanguage = LearnItConfig.getList("languages").get(0);

		return new MatchInfo(target, primaryLanguage,
				ImmutableMap.of(primaryLanguage, new LanguageMatchInfo(primaryLanguage, doc, sent, slot0)),
				null, debugText);
	}

	// for MatchInfo from bilingual docs, we will try to find alignments for the slots
	// sent : source sentence
	// slot0, slot1 : Mention or ValueMention from source sentence
	public static MatchInfo from(Target target, BilingualDocTheory bidoc, SentenceTheory sent,
			Spanning slot0, Spanning slot1) {

		LanguageMatchInfo sourceLMI = new LanguageMatchInfo(bidoc.getSourceDocLanguage(), bidoc.getSourceDoc(), sent, slot0, slot1);

		DocTheory alignedDoc = bidoc.getTargetDoc();									// get target document
		SentenceTheory alignedSent = bidoc.getTargetDoc().sentenceTheory(sent.index());	// get target sentence

		// for source slot0,slot1, try to find their alignments in target sentence
		Optional<SpanningPair> alignedSlots = bidoc.tryAlignSpanningPair(slot0, slot1, alignedDoc, alignedSent, target); // find alignments

        LanguageMatchInfo targetLMI;

		if (alignedSlots.isPresent()) {
			targetLMI = new LanguageMatchInfo(bidoc.getTargetDocLanguage(), alignedDoc, alignedSent,
					alignedSlots.get().slot0, alignedSlots.get().slot1);
		} else {
            targetLMI = new LanguageMatchInfo(bidoc.getTargetDocLanguage(), alignedDoc, alignedSent);
		}

        return new MatchInfo(target, sourceLMI.getLanguage(),
                ImmutableMap.of(sourceLMI.getLanguage(), sourceLMI, targetLMI.getLanguage(), targetLMI),
                bidoc.getSentenceAlignments().get(sent.index()));
	}

	// unary
	// sent : source sentence
	public static MatchInfo from(final Target target, final BilingualDocTheory bidoc, final SentenceTheory sent, final Spanning slot0) {
		final LanguageMatchInfo sourceLMI = new LanguageMatchInfo(bidoc.getSourceDocLanguage(), bidoc.getSourceDoc(), sent,
				slot0, sent.parse().root().get());

		final DocTheory alignedDoc = bidoc.getTargetDoc();										// get target document
		final SentenceTheory alignedSent = bidoc.getTargetDoc().sentenceTheory(sent.index());	// get target sentence

		// find alignment in target sentence
		Optional<Spanning> alignedSlot = bidoc.tryAlignSpanning(slot0, alignedDoc, alignedSent); // find alignments

        LanguageMatchInfo targetLMI;

		if (alignedSlot.isPresent()) {
			targetLMI = new LanguageMatchInfo(bidoc.getTargetDocLanguage(), alignedDoc, alignedSent, alignedSlot.get(), alignedSent.parse().root().get());
		} else {
            targetLMI = new LanguageMatchInfo(bidoc.getTargetDocLanguage(), alignedDoc, alignedSent);
		}

        return new MatchInfo(target, sourceLMI.getLanguage(),
                ImmutableMap.of(sourceLMI.getLanguage(), sourceLMI, targetLMI.getLanguage(),targetLMI),
                bidoc.getSentenceAlignments().get(sent.index()));
	}


	// I would've preferred to return Optional<Target>, but that would break various other code
	public Target getTarget() {
	  if(target.isPresent()) {
	    return target.get();
	  }
	  else {
	    return null;
	  }
	}

	public String getPrimaryLanguage() {
		return primaryLanguage;
	}

	public LanguageMatchInfo getLanguageMatch(String language) {
		if (languageMatches.containsKey(language)) {
			return languageMatches.get(language);
		} else {
			throw new RuntimeException("Couldn't find match in language "+language);
		}
	}

	public LanguageMatchInfo getPrimaryLanguageMatch() {
		return languageMatches.get(primaryLanguage);
	}

	public TokenAlignmentTable getAlignments() {
		return alignments;
	}

	public Set<String> getAvailableLanguages() {
        Set<String> availableLangs = new HashSet<String>();
        for (String key : languageMatches.keySet()) {
            if (languageMatches.get(key).hasSlots()) availableLangs.add(key);
        }
		return availableLangs;
	}

    public Set<String> getAllLanguages() {
        return languageMatches.keySet();
    }



	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if(target.isPresent()) {
		  builder.append("Instance for target: "+target.get().getName()+"\n");
		}
		else {
		  builder.append("Instance for target: NULL\n");
		}
		for (String language : languageMatches.keySet()) {
			if (language.equals(primaryLanguage)) {
				builder.append("*");
			}
			builder.append(languageMatches.get(language).toString());
		}
		if (alignments != null) {
			builder.append(alignments.toString());
		} else {
			if (debugText.equals("")) {
				builder.append("No Alignment Info");
			} else {
				builder.append("Translation with no aligned seeds: "+debugText);
			}
		}
		return builder.toString();
	}

	private static final HashFunction SHA1_HASHER = Hashing.sha1();

	private HashCode computeSHA1Hash() {
	    final Hasher hasher = SHA1_HASHER.newHasher();

	    for(final LanguageMatchInfo matchInfo : languageMatches.values()) {
	      hasher.putString(matchInfo.language, Charsets.UTF_8);
	      hasher.putString(matchInfo.docTheory.docid().toString(), Charsets.UTF_8);
	      hasher.putInt(matchInfo.sentTheory.sentenceNumber());
	      if(matchInfo.slot0.isPresent()) {
	        hasher.putInt(matchInfo.slot0.get().span().startIndex());
	        hasher.putInt(matchInfo.slot0.get().span().endIndex());
	      }
	      if(matchInfo.slot1.isPresent()) {
            hasher.putInt(matchInfo.slot1.get().span().startIndex());
            hasher.putInt(matchInfo.slot1.get().span().endIndex());
          }
	    }

	    return hasher.hash();
	}

	public String uniqueIdentifier() {
	    return cachedSHA1Hash.toString();
	}


	public static class LanguageMatchInfo implements HasSpanningPair {
		private final String language;
		private final DocTheory docTheory;
		private final SentenceTheory sentTheory;
		private final Optional<Spanning> slot0;
		private final Optional<Spanning> slot1;

		public LanguageMatchInfo(String language, DocTheory docTheory,
				SentenceTheory sentTheory, Spanning slot0, Spanning slot1) {
			this.language = language;
			this.docTheory = docTheory;
			this.sentTheory = sentTheory;
			this.slot0 = Optional.fromNullable(slot0);
			this.slot1 = Optional.fromNullable(slot1);
		}

		public LanguageMatchInfo(String language, DocTheory docTheory,
				SentenceTheory sentTheory, Spanning slot0) {
			this.language = language;
			this.docTheory = docTheory;
			this.sentTheory = sentTheory;
			this.slot0 = Optional.fromNullable(slot0);
			this.slot1 = Optional.absent();
		}

		private LanguageMatchInfo(String language, DocTheory docTheory,
				SentenceTheory sentTheory) {
			this.language = language;
			this.docTheory = docTheory;
			this.sentTheory = sentTheory;
			this.slot0 = Optional.absent();
			this.slot1 = Optional.absent();
		}

		public String getLanguage() {
			return language;
		}

		public DocTheory getDocTheory() {
			return docTheory;
		}

		public SentenceTheory getSentTheory() {
			return sentTheory;
		}

		public boolean hasSlots() {
			return slot0.isPresent();
		}

		public Optional<Spanning> getSlot0() {
			return slot0;
		}

		public Optional<Spanning> getSlot1() {
			return slot1;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("Language: " + language + ", Document: " +
					docTheory.docid().toString() + ", Sentence: " +
					sentTheory.index() + "\n");
			for (Token t : sentTheory.tokenSequence()) {
				// if (slot0.isPresent()) {
					if (t.index() == slot0.get().span().startToken().index()) {
						builder.append("<SLOT0>");
					}
					if(slot1.isPresent()) {
						if (t.index() == slot1.get().span().startToken()
								.index()) {
							builder.append("<SLOT1>");
						}
					}
				// }
				builder.append(t.text());
				// if (slot0.isPresent()) {
					if(slot1.isPresent()) {
						if (t.index() == slot1.get().span().endToken()
								.index()) {
							builder.append("</SLOT1>");
						}
					}
					if (t.index() == slot0.get().span().endToken().index()) {
						builder.append("</SLOT0>");
					}
				// }
				builder.append(" ");
			}
			builder.append("\n");
			return builder.toString();
		}

		public String markedUpTokenString() {
			final SynNode rootNode = sentTheory.parse().root().get();
			StringBuilder builder = new StringBuilder();
			for (Token t : sentTheory.tokenSequence()) {
				if (slot0.isPresent()) {
					if (t.index() == rootNode.coveringNodeFromTokenSpan(
							slot0.get().span()).head().span()
							.startIndex()) {
						builder.append("<SLOT0>");
					}
					if (t.index() == rootNode.coveringNodeFromTokenSpan(
							slot1.get().span()).head().span()
							.startIndex()) {
						builder.append("<SLOT1>");
					}
				}
				builder.append(t.text());
				if (slot0.isPresent()) {
					if (t.index() == rootNode.coveringNodeFromTokenSpan(
							slot1.get().span()).head().span()
							.endIndex()) {
						builder.append("</SLOT1>");
					}
					if (t.index() == rootNode.coveringNodeFromTokenSpan(
							slot0.get().span()).head().span()
							.endIndex()) {
						builder.append("</SLOT0>");
					}
				}
				builder.append(" ");
			}
			return builder.toString().trim();
		}

		// interfaces for JSERIF
		@Override
		public Spanning firstSpanning() {
			return slot0.get();
		}

		@Override
		public SentenceTheory firstSpanningSentence() {
			return getSentTheory();
		}

		@Override
		public Spanning secondSpanning() {
			return slot1.get();
		}

		@Override
		public SentenceTheory secondSpanningSentence() {
			return getSentTheory();
		}

		@Override
		public DocTheory docTheory() {
			return getDocTheory();
		}
		//
	}

	public static class Builder {

		private final Target target;
		private final String primaryLanguage;
		private ImmutableMap.Builder<String,LanguageMatchInfo> languageMatchesBuilder;
		private TokenAlignmentTable.Builder alignmentsBuilder;

		public Builder(Target target) {
			this.target = target;
			primaryLanguage = LearnItConfig.getList("languages").get(0);
		}

		public Builder(Target target, String primaryLanguage) {
			this.target = target;
			this.primaryLanguage = primaryLanguage;
			this.languageMatchesBuilder = new ImmutableMap.Builder<String,LanguageMatchInfo>();
			this.alignmentsBuilder = new TokenAlignmentTable.Builder();
		}

		public Builder withLanguageMentionPair(String language, DocTheory dt,
				SentenceTheory st, Mention slot0, Mention slot1) {

			languageMatchesBuilder.put(language, new LanguageMatchInfo(
					language, dt, st, slot0, slot1));
			return this;
		}

		public Builder withMentionPair(DocTheory dt,
				SentenceTheory st, Mention slot0, Mention slot1) {

			languageMatchesBuilder.put(primaryLanguage, new LanguageMatchInfo(
					primaryLanguage, dt, st, slot0, slot1));
			return this;
		}

		public Builder withAlignment(String source, String target,
				int sourceIdx, int targetIdx) {
			alignmentsBuilder.addAlignment(source, target, sourceIdx, targetIdx);
			return this;
		}

		public Builder withMultiAlignment(String source, String target,
				int sourceIdx, Iterable<Integer> targetIdxs) {
			alignmentsBuilder.addMultiAlignment(source, target, sourceIdx, targetIdxs);
			return this;
		}

		public MatchInfo build() {
			return new MatchInfo(target, primaryLanguage, languageMatchesBuilder.build(),
					alignmentsBuilder.build());
		}
	}
}
