package com.bbn.akbc.neolearnit.observers.instance.pattern.Unary;

import com.bbn.akbc.common.Pair;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;
import com.bbn.akbc.neolearnit.observations.pattern.UnaryPattern.UnaryLexicalPattern;
import com.bbn.akbc.neolearnit.observers.instance.pattern.MonolingualPatternObserver;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.*;
import com.bbn.serif.types.EntityType;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.*;

public class UnaryLexicalObserver extends MonolingualPatternObserver {
    static int NGram = 3;
    public UnaryLexicalObserver(InstanceToPatternMapping.Builder recorder,
                                String language){
        super(recorder,language);
    }

    static Set<EntityType> shouldCareEntityTypes = new HashSet<>(Arrays.asList(EntityType.PER,EntityType.ORG,EntityType.GPE));

    @Override
    public void observe(MatchInfo match, MatchInfo.LanguageMatchInfo languageMatch) {
        Optional<SynNode> node = getNode(languageMatch.firstSpanning(),languageMatch.docTheory());
        if(!node.isPresent())return;
        int currentTokenStart = node.get().span().startTokenIndexInclusive();
        int currentTokenEnd = node.get().span().endTokenIndexInclusive();

        Map<Integer,Set<Mention>> tokenIdxOffsetToMention = new HashMap<>();
        for(Mention mention: languageMatch.getSentTheory().mentions()){
            int mentionTokenStart = mention.span().startTokenIndexInclusive();
            int mentionTokenEnd = mention.span().endTokenIndexInclusive();
            for(int i = mentionTokenStart;i<=mentionTokenEnd;++i){
                Set<Mention> buf = tokenIdxOffsetToMention.getOrDefault(i,new HashSet<>());
                buf.add(mention);
                tokenIdxOffsetToMention.put(i,buf);
            }
        }
        Set<Integer>tokenIdxOffsetValueMention = new HashSet<>();
        for(ValueMention valueMention: languageMatch.getSentTheory().valueMentions()){
            int valueMentionTokenStart = valueMention.span().startTokenIndexInclusive();
            int valueMentionTokenEnd = valueMention.span().endTokenIndexInclusive();
            for(int i = valueMentionTokenStart;i <= valueMentionTokenEnd;++i){
                tokenIdxOffsetValueMention.add(i);
            }
        }


        for(int left = currentTokenStart;(left >=0) && (left >= (currentTokenStart-NGram));left--){
            for(int right = currentTokenEnd;(right < languageMatch.getSentTheory().tokenSequence().size()) && (right <= (currentTokenEnd+NGram));++right){
                List<Symbol> currentEntityTypeReplacedUnmergedTokenSet = new ArrayList<>();
                List<Symbol> currentOriginalTokenSet = new ArrayList<>();
//                System.out.println("Sentence length: "+languageMatch.getSentTheory().tokenSequence().size()+" Spanning start: "+currentTokenStart+ " Spanning end: "+currentTokenEnd+" left: "+left+" right: "+right + " There are "+((right-left+1)-(currentTokenEnd-currentTokenStart+1))+" extra tokens.");
                for(int i = left;i< currentTokenStart;++i){
                    if(tokenIdxOffsetToMention.containsKey(i)){
                        assert tokenIdxOffsetToMention.get(i).size() < 2;
                        for(Mention mention : tokenIdxOffsetToMention.get(i)){
                            if(shouldCareEntityTypes.contains(mention.entityType())){
                                currentEntityTypeReplacedUnmergedTokenSet.add(mention.entityType().name());
                                break;
                            }
                        }
                    }
                    else{
                        if(tokenIdxOffsetValueMention.contains(i)){
                            currentEntityTypeReplacedUnmergedTokenSet.add(Symbol.from("VAL"));
                        }
                        else if(languageMatch.getSentTheory().tokenSequence().token(i).symbol().asString().length()>0){
                            currentEntityTypeReplacedUnmergedTokenSet.add(languageMatch.getSentTheory().tokenSequence().token(i).symbol());
                        }
                    }
                    currentOriginalTokenSet.add(languageMatch.getSentTheory().tokenSequence().token(i).symbol());
                }
                currentEntityTypeReplacedUnmergedTokenSet.add(Symbol.from(node.get().span().tokenizedText().utf16CodeUnits().replace(" ","_").toLowerCase()));
                currentOriginalTokenSet.add(Symbol.from(node.get().span().tokenizedText().utf16CodeUnits().replace(" ","_").toLowerCase()));
                for(int i = currentTokenEnd+1;i<=right;++i){
                    if(tokenIdxOffsetToMention.containsKey(i)){
                        assert tokenIdxOffsetToMention.get(i).size() < 2;
                        for(Mention mention : tokenIdxOffsetToMention.get(i)){
                            if(shouldCareEntityTypes.contains(mention.entityType())){
                                currentEntityTypeReplacedUnmergedTokenSet.add(mention.entityType().name());
                                break;
                            }
                        }
                    }
                    else{
                        if(tokenIdxOffsetValueMention.contains(i)){
                            currentEntityTypeReplacedUnmergedTokenSet.add(Symbol.from("VAL"));
                        }
                        else if(languageMatch.getSentTheory().tokenSequence().token(i).symbol().asString().length()>0){
                            currentEntityTypeReplacedUnmergedTokenSet.add(languageMatch.getSentTheory().tokenSequence().token(i).symbol());
                        }
                    }
                    currentOriginalTokenSet.add(languageMatch.getSentTheory().tokenSequence().token(i).symbol());
                }
                if(currentEntityTypeReplacedUnmergedTokenSet.size()>0){
                    List<Symbol> currentEntityTypeReplacedMergedTokenSet = new ArrayList<>();
                    currentEntityTypeReplacedMergedTokenSet.add(currentEntityTypeReplacedUnmergedTokenSet.get(0));
                    for(int i = 1;i < currentEntityTypeReplacedUnmergedTokenSet.size();++i){
                        if(currentEntityTypeReplacedUnmergedTokenSet.get(i).equalTo(currentEntityTypeReplacedMergedTokenSet.get(currentEntityTypeReplacedMergedTokenSet.size()-1)))continue;
                        currentEntityTypeReplacedMergedTokenSet.add(currentEntityTypeReplacedUnmergedTokenSet.get(i));
                    }
                    this.record(match,new UnaryLexicalPattern(currentEntityTypeReplacedMergedTokenSet));
                }

                if(currentOriginalTokenSet.size()>0){
                    this.record(match,new UnaryLexicalPattern(currentOriginalTokenSet));
                }

            }
        }
    }

    public static Optional<SynNode> getNode(Spanning span, DocTheory dt) {
        if (span instanceof Mention) {
            return Optional.of(((Mention) span).atomicHead());
        } else if (span instanceof ValueMention) {
            return ValueMention.node(dt, ((ValueMention) span));
        } else if (span instanceof EventMention) {
            EventMention em = (EventMention) span;
            return Optional.of(em.anchorNode());

        } else if (span instanceof SentenceTheory) {
            final SentenceTheory sentence = (SentenceTheory) span;

            if (!sentence.parse().isAbsent())
                return Optional.of(sentence.parse().root().get());
            else
                return Optional.absent();
        } else {
            throw new RuntimeException("Unhandled span type: " + span);
        }
    }
}
