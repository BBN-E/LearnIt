package com.bbn.akbc.neolearnit.observers.instance.pattern.Binary;

import com.bbn.akbc.neolearnit.observers.instance.pattern.MonolingualPatternObserver;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo.LanguageMatchInfo;
import com.bbn.akbc.neolearnit.common.resources.StopWords;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;
import com.bbn.akbc.neolearnit.observations.pattern.BeforeAfterSlotsPattern;
import com.bbn.akbc.neolearnit.observations.pattern.RegexableContent;
import com.bbn.akbc.neolearnit.observations.pattern.SymbolContent;
import com.bbn.serif.theories.EventMention;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.Spanning;
import com.google.inject.BindingAnnotation;
import com.google.inject.Inject;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

public class TextBeforeAfterSlotsObserver extends MonolingualPatternObserver {

	private final boolean allowAllStopWordPatterns;
	private final int maxTokensBeforeAfterSlots;
	private final StopWords stopwords;

	@BindingAnnotation @Target({ PARAMETER }) @Retention(RUNTIME)
	public @interface BeforeAfterSlotTokens {}

	@Inject
	public TextBeforeAfterSlotsObserver(InstanceToPatternMapping.Builder recorder,
			String language,
			@BeforeAfterSlotTokens int maxTokensBeforeAfterSlots) {
		super(recorder, language);
		this.maxTokensBeforeAfterSlots = maxTokensBeforeAfterSlots;
		this.stopwords = StopWords.getDefault();
		this.allowAllStopWordPatterns = true;
	}

	public int getStartTokenIndex(Spanning spanning) {
		if (spanning instanceof EventMention) {
			EventMention eventMention = ((EventMention) spanning);
			if (eventMention.semanticPhraseStart().isPresent())
				return eventMention.semanticPhraseStart().get();
		}

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
	public void observe(MatchInfo match, LanguageMatchInfo languageMatch) {

		SentenceTheory sentence = languageMatch.getSentTheory();

		Spanning first  = languageMatch.getSlot0().get();
		int firstSlot   = 0;
		Spanning second = languageMatch.getSlot1().get();
		int secondSlot  = 1;

		if (first.span().overlaps(second.span()) || second.span().overlaps(first.span()))
			return;

		if (second.span().startsBefore(first.span())) {
			Spanning temp = first;
			first = second;
			second = temp;

			firstSlot  = 1;
			secondSlot = 0;
		}

		if (getStartTokenIndex(second) - getEndTokenIndex(first) > LearnItConfig.getInt("max_regexp_pattern_words"))
			return;

		int startInd = getStartTokenIndex(first);
		int endInd = getEndTokenIndex(second);

		List<RegexableContent> beforeText = new ArrayList<RegexableContent>();
		boolean beforeTextHasContent = false;
		for (int i = Math.max(startInd-maxTokensBeforeAfterSlots, 0); i < startInd; ++i) {
			Symbol token = sentence.tokenSequence().token(i).symbol();
			beforeText.add(new SymbolContent(token));

			if ((allowAllStopWordPatterns || !stopwords.isStopWord(token)) &&
					(token.toString().length() > 0 || Character.isLetterOrDigit(token.toString().charAt(0))))
			{
				beforeTextHasContent = true;
			}
		}

		if (beforeTextHasContent) {
			this.record(match, new BeforeAfterSlotsPattern(language, firstSlot, true, beforeText));
		}

		List<RegexableContent> afterText = new ArrayList<RegexableContent>();
		boolean afterTextHasContent = false;
		for (int i = endInd; i < Math.min(endInd+maxTokensBeforeAfterSlots, sentence.tokenSequence().size()); i++) {
			Symbol token = sentence.tokenSequence().token(i).symbol();
			afterText.add(new SymbolContent(token));

			if ((allowAllStopWordPatterns || !stopwords.isStopWord(token)) &&
					(token.toString().length() > 0 || Character.isLetterOrDigit(token.toString().charAt(0))))
			{
				afterTextHasContent = true;
			}
		}

		if (afterTextHasContent) {
			this.record(match, new BeforeAfterSlotsPattern(language, secondSlot, false, afterText));
		}
	}
}
