package com.bbn.akbc.neolearnit.observers.instance.pattern.Unary;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;
import com.bbn.akbc.neolearnit.observations.pattern.ComboPattern;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.pattern.UnaryPattern.HeadWordPOSTagPattern;
import com.bbn.akbc.neolearnit.observations.pattern.UnaryPattern.UnaryLexicalPattern;
import com.bbn.akbc.neolearnit.observers.instance.pattern.MonolingualPatternObserver;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.SynNode;
import com.bbn.serif.theories.Token;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class EventMentionNounPhraseObserver extends MonolingualPatternObserver {

    public EventMentionNounPhraseObserver(InstanceToPatternMapping.Builder recorder,
                                          String language) {
        super(recorder, language);
    }

    boolean tooLongOrContainsInValidCharactersOrWords(List<Symbol> symbols) {
        int MAX_NUM_WORDS = 5;
        if(symbols.size()>MAX_NUM_WORDS)
            return true;

        for(Symbol symbol : symbols) {
            String s = symbol.asUnicodeFriendlyString().utf16CodeUnits();
            if(s.equals("and"))
                return true;
            if(s.contains("[") || s.contains("]") || s.contains("{") || s.contains("}")|| s.contains(":"))
                return true;
        }

        return false;
    }

    @Override
    public void observe(MatchInfo match, MatchInfo.LanguageMatchInfo languageMatch) {
        if (languageMatch.getSlot1().isPresent()) return;

        List<SynNode> nodes = InstanceIdentifier.getNode(languageMatch.firstSpanning(), languageMatch.docTheory());
        for (SynNode node : nodes) {

            SynNode head = node.headPreterminal();
            final String POSTag = head.headPOS().asString();
            String trigger = head.span().originalText().content().utf16CodeUnits();

            Set<LearnitPattern> uniqPatterns = new HashSet<LearnitPattern>();
            for (Mention mention : languageMatch.firstSpanningSentence().mentions()) {
                if (mention.atomicHead().head().span().equals(node.span())) {
                    List<Token> tokens = mention.tokenSpan().tokens(languageMatch.docTheory());
                    List<Symbol> allSymbols = new ArrayList<>();
                    for (Token token : tokens) {
                        allSymbols.add(Symbol.from(token.tokenizedText().utf16CodeUnits().trim()));
                    }

                    // skip invalid phrases
                    if (tooLongOrContainsInValidCharactersOrWords(allSymbols))
                        continue;

                    // right to left
                    for (int numTokens = 0; numTokens <= tokens.size(); numTokens++) {
                        List<Symbol> symbols = allSymbols.subList(allSymbols.size() - numTokens, allSymbols.size());

                        ComboPattern comboPattern = new ComboPattern(new UnaryLexicalPattern(symbols), new HeadWordPOSTagPattern(trigger, POSTag));
                        uniqPatterns.add(comboPattern);
                    }

                    // left to right
                    for (int numTokens = 0; numTokens <= tokens.size(); numTokens++) {
                        List<Symbol> symbols = allSymbols.subList(0, numTokens);

                        ComboPattern comboPattern = new ComboPattern(new UnaryLexicalPattern(symbols), new HeadWordPOSTagPattern(trigger, POSTag));
                        uniqPatterns.add(comboPattern);
                    }

                    for (LearnitPattern learnitPattern : uniqPatterns) {
                        this.record(match, learnitPattern);

//                        System.out.println("EventMentionNounPhraseObserver:\t" + node.tokenSpan().tokenizedText(languageMatch.docTheory()).utf16CodeUnits() + "\t" +
//                                "mention: " + mention.tokenSpan().tokenizedText(languageMatch.docTheory()).utf16CodeUnits() + "\t" +
//                                "pattern: " + learnitPattern.toIDString());
                    }
                }
            }
        }
    }
}
