package com.bbn.akbc.neolearnit.observers.instance.pattern.Unary;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;
import com.bbn.akbc.neolearnit.observations.pattern.ComboPattern;
import com.bbn.akbc.neolearnit.observations.pattern.UnaryPattern.HeadWordPOSTagPattern;
import com.bbn.akbc.neolearnit.observations.pattern.UnaryPattern.TypePattern;
import com.bbn.akbc.neolearnit.observations.pattern.UnaryPattern.UnaryLexicalPattern;
import com.bbn.akbc.neolearnit.observers.instance.pattern.MonolingualPatternObserver;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.*;
import com.bbn.serif.types.EntityType;

import java.util.*;

public class ContextWindowObserver extends MonolingualPatternObserver {
    static int NGram = 3;


    static Set<Symbol> shouldSkipTokens = new HashSet<>(Arrays.asList(
            Symbol.from("-LRB-"),
            Symbol.from("-RRB-"),
            Symbol.from("-LCB-"),
            Symbol.from("-RCB-"),
            Symbol.from(",")
    ));

    public ContextWindowObserver(InstanceToPatternMapping.Builder recorder,
                                 String language){
        super(recorder,language);

    }

//    static Set<EntityType> shouldCareEntityTypes = new HashSet<>(Arrays.asList(EntityType.PER,EntityType.ORG,EntityType.GPE,EntityType.of("HQIUEntity")));

    static Set<EntityType> shouldNotCareEntityTypes = new HashSet<>(Arrays.asList(EntityType.undetermined(), EntityType.of("OTH")));

    @Override
    public void observe(MatchInfo match, MatchInfo.LanguageMatchInfo languageMatch) {
        Spanning spanning = languageMatch.getSlot0().get();
        List<SynNode> nodes = InstanceIdentifier.getNode(languageMatch.firstSpanning(), languageMatch.docTheory());
        for (SynNode node : nodes) {
            SynNode head = node.headPreterminal();
            final String POSTag = head.headPOS().asString();
            final int currentTokenStart = node.span().startTokenIndexInclusive();
            final int currentTokenEnd = node.span().endTokenIndexInclusive();

            Map<Integer, Set<Mention>> tokenIdxOffsetToMention = new HashMap<>();
            for (Mention mention : languageMatch.getSentTheory().mentions()) {
                int mentionTokenStart = mention.span().startTokenIndexInclusive();
                int mentionTokenEnd = mention.span().endTokenIndexInclusive();
                for (int i = mentionTokenStart; i <= mentionTokenEnd; ++i) {
                    Set<Mention> buf = tokenIdxOffsetToMention.getOrDefault(i, new HashSet<>());
                    buf.add(mention);
                    tokenIdxOffsetToMention.put(i, buf);
                }
            }
            Map<Integer, Set<ValueMention>> tokenIdxOffsetValueMention = new HashMap<>();
            for (ValueMention valueMention : languageMatch.getSentTheory().valueMentions()) {
                int valueMentionTokenStart = valueMention.span().startTokenIndexInclusive();
                int valueMentionTokenEnd = valueMention.span().endTokenIndexInclusive();
                for (int i = valueMentionTokenStart; i <= valueMentionTokenEnd; ++i) {
                    Set<ValueMention> buf = tokenIdxOffsetValueMention.getOrDefault(i, new HashSet<>());
                    buf.add(valueMention);
                    tokenIdxOffsetValueMention.put(i, buf);
                }
            }


            for (int left = currentTokenStart; (left >= 0) && (left >= (currentTokenStart - NGram)); left--) {
                for (int right = currentTokenEnd; (right < languageMatch.getSentTheory().tokenSequence().size()) && (right <= (currentTokenEnd + NGram)); ++right) {
//                System.out.println("Trigger: "+node.get().span().tokenizedText().utf16CodeUnits());
//                System.out.println("Sentence: "+languageMatch.getSentTheory().span().tokenizedText().utf16CodeUnits());
//                System.out.println("Span Start: "+currentTokenStart);
//                System.out.println("Span End: "+ currentTokenEnd);
//                System.out.println("Current left: "+left);
//                System.out.println("Current right: "+right);
                    List<Symbol> currentEntityTypeReplacedUnmergedTokenSet = new ArrayList<>();
                    List<Symbol> currentOriginalTokenSet = new ArrayList<>();
                    for (int i = left; i < currentTokenStart; ++i) {

                        if (tokenIdxOffsetToMention.containsKey(i)) {
                            assert tokenIdxOffsetToMention.get(i).size() < 2;
                            for (Mention mention : tokenIdxOffsetToMention.get(i)) {
                                if (!shouldNotCareEntityTypes.contains(mention.entityType())) {
                                    currentEntityTypeReplacedUnmergedTokenSet.add(mention.entityType().name());
                                }
                            }
                        } else {
                            if (tokenIdxOffsetValueMention.containsKey(i)) {
                                for (ValueMention valueMention : tokenIdxOffsetValueMention.get(i)) {
                                    currentEntityTypeReplacedUnmergedTokenSet.add(valueMention.fullType().name());
                                }
                            } else if (languageMatch.getSentTheory().tokenSequence().token(i).symbol().asString().length() > 0 && !shouldSkipTokens.contains(languageMatch.getSentTheory().tokenSequence().token(i).symbol())) {
                                currentEntityTypeReplacedUnmergedTokenSet.add(languageMatch.getSentTheory().tokenSequence().token(i).symbol());
                            }
                        }
                        if (languageMatch.getSentTheory().tokenSequence().token(i).symbol().asString().length() > 0 && !shouldSkipTokens.contains(languageMatch.getSentTheory().tokenSequence().token(i).symbol())) {
                            currentOriginalTokenSet.add(languageMatch.getSentTheory().tokenSequence().token(i).symbol());
                        }
//                    System.out.println("Currently we're adding "+languageMatch.getSentTheory().tokenSequence().token(i).symbol().asString());
//                    System.out.println("Result array: "+currentOriginalTokenSet);

                    }
                    for (int i = currentTokenStart; i <= currentTokenEnd; ++i) {
                        Token token = languageMatch.getSentTheory().tokenSequence().token(i);
                        if (!shouldSkipTokens.contains(token.symbol())) {
                            currentEntityTypeReplacedUnmergedTokenSet.add(Symbol.from(token.tokenizedText().utf16CodeUnits()));
                            currentOriginalTokenSet.add(Symbol.from(token.tokenizedText().utf16CodeUnits()));
                        }
//                    System.out.println("Currently we're adding "+languageMatch.getSentTheory().tokenSequence().token(i).symbol().asString());
//                    System.out.println("Result array: "+currentOriginalTokenSet);
                    }
                    for (int i = currentTokenEnd + 1; i <= right; ++i) {
                        if (tokenIdxOffsetToMention.containsKey(i)) {
                            assert tokenIdxOffsetToMention.get(i).size() < 2;
                            for (Mention mention : tokenIdxOffsetToMention.get(i)) {
                                if (!shouldNotCareEntityTypes.contains(mention.entityType())) {
                                    currentEntityTypeReplacedUnmergedTokenSet.add(mention.entityType().name());
                                }
                            }
                        } else {
                            if (tokenIdxOffsetValueMention.containsKey(i)) {
                                for (ValueMention valueMention : tokenIdxOffsetValueMention.get(i)) {
                                    currentEntityTypeReplacedUnmergedTokenSet.add(valueMention.fullType().name());
                                }
                            } else if (languageMatch.getSentTheory().tokenSequence().token(i).symbol().asString().length() > 0 && !shouldSkipTokens.contains(languageMatch.getSentTheory().tokenSequence().token(i).symbol())) {
                                currentEntityTypeReplacedUnmergedTokenSet.add(languageMatch.getSentTheory().tokenSequence().token(i).symbol());
                            }
                        }
                        if (languageMatch.getSentTheory().tokenSequence().token(i).symbol().asString().length() > 0 && !shouldSkipTokens.contains(languageMatch.getSentTheory().tokenSequence().token(i).symbol())) {
                            currentOriginalTokenSet.add(languageMatch.getSentTheory().tokenSequence().token(i).symbol());
                        }
//                    System.out.println("Currently we're adding "+languageMatch.getSentTheory().tokenSequence().token(i).symbol().asString());
//                    System.out.println("Result array: "+currentOriginalTokenSet);
                    }
                    if (currentEntityTypeReplacedUnmergedTokenSet.size() > 0) {
                        List<Symbol> currentEntityTypeReplacedMergedTokenSet = new ArrayList<>();
                        currentEntityTypeReplacedMergedTokenSet.add(currentEntityTypeReplacedUnmergedTokenSet.get(0));
                        for (int i = 1; i < currentEntityTypeReplacedUnmergedTokenSet.size(); ++i) {
                            if (currentEntityTypeReplacedUnmergedTokenSet.get(i).equalTo(currentEntityTypeReplacedMergedTokenSet.get(currentEntityTypeReplacedMergedTokenSet.size() - 1)))
                                continue;
                            currentEntityTypeReplacedMergedTokenSet.add(currentEntityTypeReplacedUnmergedTokenSet.get(i));
                        }
                        if (spanning instanceof EventMention) {
                            String trigger = head.span().originalText().content().utf16CodeUnits();
                            this.record(match, new ComboPattern(new UnaryLexicalPattern(currentEntityTypeReplacedMergedTokenSet), new HeadWordPOSTagPattern(trigger, POSTag)));
                        } else if (spanning instanceof Mention) {
                            Mention mention = (Mention) spanning;
                            String type = mention.entityType().name().asString();
                            this.record(match, new ComboPattern(new UnaryLexicalPattern(currentEntityTypeReplacedMergedTokenSet), new TypePattern(type)));
                        } else if (spanning instanceof ValueMention) {
                            ValueMention valueMention = (ValueMention) spanning;
                            String type = valueMention.fullType().name().asString();
                            this.record(match, new ComboPattern(new UnaryLexicalPattern(currentEntityTypeReplacedMergedTokenSet), new TypePattern(type)));
                        }
//                    System.out.println("P1\t"+currentEntityTypeReplacedMergedTokenSet);
                    }

                    if (currentOriginalTokenSet.size() > 0) {
                        if (spanning instanceof EventMention) {
                            String trigger = head.span().originalText().content().utf16CodeUnits();
                            this.record(match, new ComboPattern(new UnaryLexicalPattern(currentOriginalTokenSet), new HeadWordPOSTagPattern(trigger, POSTag)));
                        } else if (spanning instanceof Mention) {
                            Mention mention = (Mention) spanning;
                            String type = mention.entityType().name().asString();
                            this.record(match, new ComboPattern(new UnaryLexicalPattern(currentOriginalTokenSet), new TypePattern(type)));
                        } else if (spanning instanceof ValueMention) {
                            ValueMention valueMention = (ValueMention) spanning;
                            String type = valueMention.fullType().name().asString();
                            this.record(match, new ComboPattern(new UnaryLexicalPattern(currentOriginalTokenSet), new TypePattern(type)));
                        }
//                    System.out.println("P2\t"+currentOriginalTokenSet);
                    }
                }
            }
        }

    }
}
