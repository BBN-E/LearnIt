package com.bbn.akbc.neolearnit.loaders.impl;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.loaders.AbstractInstanceLoader;
import com.bbn.akbc.neolearnit.observers.InstanceObservers;
import com.bbn.akbc.neolearnit.observers.Observer;
import com.bbn.akbc.neolearnit.observers.instance.pattern.SerifPatternObserver;
import com.bbn.serif.patterns.PatternGenerator;
import com.bbn.serif.theories.*;

import com.fasterxml.jackson.databind.deser.DataFormatReaders;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.List;

public class MonolingualDocTheoryInstanceLoader extends AbstractInstanceLoader<DocTheory> {

	private final Target target;

	@Inject
	public MonolingualDocTheoryInstanceLoader(Target target, InstanceObservers binaryObservers,InstanceObservers unaryObserver, boolean evaluating) {
		super(binaryObservers,unaryObserver, evaluating);
		Preconditions.checkNotNull(target);
		this.target = target;
	}

	@Inject
	public MonolingualDocTheoryInstanceLoader(Target target, InstanceObservers binaryObserver,InstanceObservers unaryObserver) {
		this(target,binaryObserver,unaryObserver,false);
		Preconditions.checkNotNull(target);
	}


	@Override
	public void load(DocTheory doc) {
		if (evaluating) docTheoryCache.put(doc.docid().toString(), doc);

		for (SentenceTheory st : doc.nonEmptySentenceTheories()) {
			if(LearnItConfig.defined("max_number_of_tokens_per_sentence")){
				int maxNumberOfTokensPerSentence = LearnItConfig.getInt("max_number_of_tokens_per_sentence");
				if(st.tokenSequence().size() > maxNumberOfTokensPerSentence){
					continue;
				}
			}
			List<Spanning> allSpans = new ArrayList<>();
			allSpans.addAll(st.mentions().mentions());
			allSpans.addAll(st.valueMentions().valueMentions());
			allSpans.addAll(st.eventMentions().eventMentions());

			// mentions as unary relations
			for (Spanning slot0 : allSpans) {
				MatchInfo match = MatchInfo.from(target,doc,st,slot0);
				if (target.validMatch(match, this.isEvaluating())) {
					this.handleUnaryMatch(match);
				}
			}

			// binary relations
			for (Spanning slot0 : allSpans) {
				for (Spanning slot1 : allSpans) {

					// Same slot
					if(slot0.equals(slot1))
					 	continue;

					// Identical spans
					if (slot0.span().startToken().index() == slot1.span().startToken().index() &&
							slot0.span().endToken().index() == slot1.span().endToken().index()) {
						continue;
					}

					// Avoid creating patterns for two spans that overlap,
					// and are anaphoric, e.g. "the President of the United States"
					// and "the President"
					if(slotsHaveTheSameHead(slot0,slot1)) {
						continue;
					}

					MatchInfo match = MatchInfo.from(target,doc,st,slot0,slot1);

					// for debugging
					// should be disabled before running on a large corpus
//					if(slot0 instanceof EventMention && slot1 instanceof EventMention) {
//						EventMention e0 = (EventMention) slot0;
//						EventMention e1 = (EventMention) slot1;
//
//						System.out.println("[Event Pair] slot0: " + e0.anchorNode().tokenSpan().originalText().get().text()
//						 + ", slot1: " + e1.anchorNode().tokenSpan().originalText().get().text());
//					}

					if (target.validMatch(match, this.isEvaluating())) {
//						System.out.println("<Slot0,Slot1>:\t" + getInfoForSpanning(slot0).replace(":", "\t") + "\t" + getInfoForSpanning(slot1).replace(":", "\t")
//								+ "\t" + st.tokenSpan().originalText().content().utf16CodeUnits().replace("\n", " "));

						this.handleBinaryMatch(match);
					}
				}
			}
		}
	}

	static boolean slotsHaveTheSameHead(Spanning slot0, Spanning slot1) {
		TokenSequence.Span headSpan1 = getHeadSpan(slot0).orNull();
		TokenSequence.Span headSpan2 = getHeadSpan(slot1).orNull();
		if(headSpan1==null || headSpan2 == null){
			return false;
		}
		if(headSpan1.equals(headSpan2)){
			return true;
		}
		return false;
	}

	static Optional<TokenSequence.Span> getHeadSpan(Spanning spanning){
		TokenSequence.Span headSpan = null;
		if(spanning instanceof EventMention) {
			EventMention eventMention = (EventMention) spanning;
			headSpan = eventMention.anchorNode().span();
		} else if(spanning instanceof Mention) {
			Mention mention = (Mention) spanning;
			headSpan =  mention.atomicHead().span();
		} else if(spanning instanceof ValueMention) {
			ValueMention valueMention = (ValueMention) spanning;
			headSpan =  valueMention.span();
		}
		return Optional.fromNullable(headSpan);
	}

	static String getInfoForSpanning(Spanning spanning) {
		if(spanning instanceof EventMention) {
			EventMention eventMention = (EventMention) spanning;
			return "Event:" + eventMention.type().asString() + ":" + eventMention.anchorNode().span().originalText().content().utf16CodeUnits().replace("\n", " ");
		} else if(spanning instanceof Mention) {
			Mention mention = (Mention) spanning;
			return "Entity:" + mention.entityType().name().asString() + ":" + mention.atomicHead().tokenSpan().originalText().content().utf16CodeUnits().replace("\n", " ");
		} else if(spanning instanceof ValueMention) {
			ValueMention valueMention = (ValueMention) spanning;
			return "Val:" + valueMention.type().asString() + ":" + valueMention.tokenSpan().originalText().content().utf16CodeUnits().replace("\n", " ");
		} else {
			return "NA";
		}
	}
}
