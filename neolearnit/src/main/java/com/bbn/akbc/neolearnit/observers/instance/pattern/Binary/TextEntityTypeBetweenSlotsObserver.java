package com.bbn.akbc.neolearnit.observers.instance.pattern.Binary;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;
import com.bbn.akbc.neolearnit.observations.pattern.BetweenSlotsContent;
import com.bbn.akbc.neolearnit.observations.pattern.EntityTypeContent;
import com.bbn.akbc.neolearnit.observations.pattern.RegexableContent;
import com.bbn.akbc.neolearnit.observations.pattern.SymbolContent;
import com.bbn.bue.common.collections.PowerSetIterable;
import com.bbn.serif.theories.*;
import com.google.common.base.Optional;
import com.google.inject.BindingAnnotation;
import com.google.inject.Inject;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collection;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

public class TextEntityTypeBetweenSlotsObserver extends SlotPairObserver {

	private final int maxTokensBetweenSlots;

	private final boolean entityGeneralization;
	private final boolean alwaysGeneralizeEType;

	@BindingAnnotation @Target({ PARAMETER }) @Retention(RUNTIME)
	public @interface BetweenSlotTokens {}

	@Inject
	public TextEntityTypeBetweenSlotsObserver(InstanceToPatternMapping.Builder recorder,
			String language,
			@BetweenSlotTokens int maxBetweenSlotTokens) {
		super(recorder, language);
		this.maxTokensBetweenSlots = maxBetweenSlotTokens;

		entityGeneralization = LearnItConfig.optionalParamTrue("entity_type_generalization") || LearnItConfig.optionalParamTrue("always_generalize_entity_type");
		alwaysGeneralizeEType = LearnItConfig.optionalParamTrue("always_generalize_entity_type");
	}

	public int getStartTokenIndex(Spanning spanning) {
        if (spanning instanceof Mention && ((Mention)spanning).mentionType() == Mention.Type.DESC)
		    return ((Mention)spanning).atomicHead().span().startToken().index();
        else if (spanning instanceof EventMention) {
        	EventMention eventMention = ((EventMention) spanning);
        	if(eventMention.semanticPhraseStart().isPresent())
        		return eventMention.semanticPhraseStart().get();
        	else
        		return eventMention.span().startToken().index();
		} else
            return spanning.span().startToken().index();
	}

	public int getEndTokenIndex(Spanning spanning) {
		if (spanning instanceof EventMention) {
			EventMention eventMention = ((EventMention) spanning);
			if(eventMention.semanticPhraseEnd().isPresent())
				return eventMention.semanticPhraseEnd().get()+1; // TODO: check whether this index is inclusive
		}

		return spanning.span().endToken().index()+1;
	}

	@Override
	public Iterable<BetweenSlotsContent<RegexableContent>> observe(SentenceTheory sentence, Spanning slotA, Spanning slotB) {

		Collection<BetweenSlotsContent<RegexableContent>> results =
				new ArrayList<BetweenSlotsContent<RegexableContent>>();

		PowerSetIterable.Builder<RegexableContent> textBetweenSlots =
				new PowerSetIterable.Builder<RegexableContent>();


        int dist = getStartTokenIndex(slotB) - getEndTokenIndex(slotA);

		if (dist <= maxTokensBetweenSlots && dist >= 0) {

			for (int i=getEndTokenIndex(slotA);i<getStartTokenIndex(slotB);i++) {

				Optional<Name> nameSpan = getNameSpan(sentence,i,getStartTokenIndex(slotB));
				if (entityGeneralization && nameSpan.isPresent() && nameSpan.get().type().isNotUndetOrOth()) {
					textBetweenSlots.withChoiceAdd(new EntityTypeContent(nameSpan.get().type()));

					if (!alwaysGeneralizeEType) {
						textBetweenSlots.withChoiceAdd(new SymbolContent(sentence.tokenSequence().span(
									getStartTokenIndex(nameSpan.get()),
									getEndTokenIndex(nameSpan.get())-1)));
					}

					i = getEndTokenIndex(nameSpan.get())-1;

				} else {
					textBetweenSlots.withChoiceAdd(new SymbolContent(sentence.tokenSequence().token(i).symbol()));
				}
				textBetweenSlots.withCommitChoiceSet();
			}
			for (Iterable<RegexableContent> it : textBetweenSlots.build()) {
				BetweenSlotsContent<RegexableContent> content = new BetweenSlotsContent<RegexableContent>(it);
				results.add(content);
			}
		}

		return results;
	}

	private Optional<Name> getNameSpan(SentenceTheory sentence, int idx, int end) {
		for (Name s : sentence.names()) {
			if (idx == getStartTokenIndex(s) && getEndTokenIndex(s)-1 < end) {
				return Optional.of(s);
			}
		}
		return Optional.absent();
	}
}
