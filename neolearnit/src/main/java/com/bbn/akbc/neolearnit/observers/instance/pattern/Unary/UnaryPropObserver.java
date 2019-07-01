package com.bbn.akbc.neolearnit.observers.instance.pattern.Unary;

import com.bbn.akbc.common.Pair;
import com.bbn.akbc.neolearnit.observers.instance.pattern.MonolingualPatternObserver;
import com.bbn.akbc.neolearnit.util.GeneralUtils;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo.LanguageMatchInfo;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;
import com.bbn.akbc.neolearnit.observations.pattern.PropPattern;
import com.bbn.akbc.neolearnit.observations.pattern.PropPattern.PropArgObservation;
import com.bbn.serif.theories.*;
import com.bbn.serif.theories.Proposition.Argument;
import com.bbn.serif.theories.Proposition.MentionArgument;
import com.bbn.serif.theories.Proposition.PropositionArgument;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.inject.BindingAnnotation;
import com.google.inject.Inject;

import javax.swing.text.html.Option;
import javax.xml.soap.Text;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.*;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

public class UnaryPropObserver extends MonolingualPatternObserver {

    @BindingAnnotation
    @Target({PARAMETER})
    @Retention(RUNTIME)
    public @interface PropDepth {
    }

    @Inject
    public UnaryPropObserver(InstanceToPatternMapping.Builder recorder, String language) {
        super(recorder, language);
    }

    static int MAX_DEPTH =2;
    static int MAX_NUM_OF_PROP_ARGS = 3;

    private void DFS(MatchInfo matchInfo, LanguageMatchInfo languageMatchInfo, Proposition proposition, Set<PropPattern> retReplacewithEntityType,Set<PropPattern> retOriginalSpan, boolean predHeadShouldMatch,int depthRemaining,Set<Pair<TokenSequence.Span,Proposition.PredicateType>> visitedProposition) {

        Optional<SynNode> node = getNode(languageMatchInfo.getSlot0().get(), languageMatchInfo.docTheory());
        if(depthRemaining<1)return;
        if (!node.isPresent()) return;
        if (!proposition.predHead().isPresent()) return;
        if (predHeadShouldMatch && !proposition.predHead().get().span().equals(node.get().span())) return;
        if(visitedProposition.contains(new Pair<>(proposition.span(),proposition.predType())))return;

        visitedProposition.add(new Pair<>(proposition.span(),proposition.predType()));
        List<PropArgObservation> candidatesReplaceWithEntityType = new ArrayList<>();

        List<PropArgObservation> candidatesOriginalSpan = new ArrayList<>();

        for (Argument argument : proposition.args()) {
            if (!argument.role().isPresent()) continue;
            if (argument instanceof MentionArgument) {
                MentionArgument mentionArgument = (MentionArgument) argument;
                candidatesReplaceWithEntityType.add(new PropArgObservation(Symbol.from(mentionArgument.role().get() + " " + mentionArgument.mention().entityType().name().asString())));
                candidatesOriginalSpan.add(new PropArgObservation(Symbol.from(mentionArgument.role().get() + " " + mentionArgument.mention().span().tokenizedText().utf16CodeUnits())));
            } else if (argument instanceof Proposition.TextArgument) {
                Proposition.TextArgument textArgument = (Proposition.TextArgument) argument;
                candidatesReplaceWithEntityType.add(new PropArgObservation(Symbol.from(textArgument.span().tokenizedText().utf16CodeUnits())));
                candidatesOriginalSpan.add(new PropArgObservation(Symbol.from(textArgument.span().tokenizedText().utf16CodeUnits())));
            } else if (argument instanceof PropositionArgument) {
                PropositionArgument propositionArgument = (PropositionArgument) argument;
                Set<PropPattern> replaceWithEntityTypeSet = new HashSet<>();
                Set<PropPattern> originalSpanSet = new HashSet<>();
                DFS(matchInfo, languageMatchInfo, propositionArgument.proposition(), replaceWithEntityTypeSet,originalSpanSet, false,depthRemaining-1,visitedProposition);
                for (PropPattern propPattern : replaceWithEntityTypeSet) {
                    candidatesReplaceWithEntityType.add(new PropArgObservation(argument.role().get(), propPattern));
                }
                for (PropPattern propPattern : originalSpanSet){
                    candidatesOriginalSpan.add(new PropArgObservation(argument.role().get(), propPattern));
                }
            }
        }

        // PropArgObservation only contain trigger itself case
        PropPattern.Builder propPatternBuilder = new PropPattern.Builder(languageMatchInfo.getLanguage(), proposition.predType());
        propPatternBuilder.withPredicate(Symbol.from(node.get().span().tokenizedText().utf16CodeUnits()));
        if (predHeadShouldMatch) {
            PropArgObservation triggerMark = new PropArgObservation(Symbol.from("trigger"), 0);
            propPatternBuilder.withArg(triggerMark);
        }
        PropPattern propPattern = propPatternBuilder.build();
        if(propPattern.args().size()>0){
            retReplacewithEntityType.add(propPattern);
            retOriginalSpan.add(propPattern);
        }
        for (int i = 1; i <= MAX_NUM_OF_PROP_ARGS; i++) {
            // PropArgObservation contain trigger and other argument
            List<List<PropArgObservation>> candidateCombination = GeneralUtils.Combination(candidatesReplaceWithEntityType, i);
            for (List<PropArgObservation> onePossiblePropArgObservationCombinarion : candidateCombination) {
                propPatternBuilder = new PropPattern.Builder(languageMatchInfo.getLanguage(), proposition.predType());
                propPatternBuilder.withPredicate(Symbol.from(node.get().span().tokenizedText().utf16CodeUnits()));
                if (predHeadShouldMatch) {
                    PropArgObservation triggerMark = new PropArgObservation(Symbol.from("trigger"), 0);
                    propPatternBuilder.withArg(triggerMark);
                }
                for (PropArgObservation propArgObservation : onePossiblePropArgObservationCombinarion) {
                    propPatternBuilder.withArg(propArgObservation);
                }
                propPattern = propPatternBuilder.build();
                retReplacewithEntityType.add(propPattern);
            }

            candidateCombination = GeneralUtils.Combination(candidatesOriginalSpan, i);
            for (List<PropArgObservation> onePossiblePropArgObservationCombinarion : candidateCombination) {
                propPatternBuilder = new PropPattern.Builder(languageMatchInfo.getLanguage(), proposition.predType());
                propPatternBuilder.withPredicate(Symbol.from(node.get().span().tokenizedText().utf16CodeUnits()));
                if (predHeadShouldMatch) {
                    PropArgObservation triggerMark = new PropArgObservation(Symbol.from("trigger"), 0);
                    propPatternBuilder.withArg(triggerMark);
                }
                for (PropArgObservation propArgObservation : onePossiblePropArgObservationCombinarion) {
                    propPatternBuilder.withArg(propArgObservation);
                }
                propPattern = propPatternBuilder.build();
                retOriginalSpan.add(propPattern);
            }
        }

        visitedProposition.remove(new Pair<>(proposition.span(),proposition.predType()));
//        if(predHeadShouldMatch){
//            if(ret.size() ==232 && candidates.size() == 11){
//                System.out.println(proposition);
//                for(PropArgObservation propArgObservation:candidates){
//                    System.out.println(propArgObservation);
//                }
//                System.out.println("We added "+ret.size()+" patterns.");
//            }
//
//        }
    }

    @Override
    public void observe(MatchInfo matchInfo, LanguageMatchInfo languageMatchInfo) {
        Optional<SynNode> node = getNode(languageMatchInfo.firstSpanning(), languageMatchInfo.docTheory());
        if (!node.isPresent()) return;
        Set<PropPattern> retReplacewithEntityType = new HashSet<>();
        Set<PropPattern> retOriginalSpan = new HashSet<>();
        for (Proposition proposition : languageMatchInfo.getSentTheory().propositions()) {
            DFS(matchInfo, languageMatchInfo, proposition, retReplacewithEntityType,retOriginalSpan, true,MAX_DEPTH,new HashSet<>());
        }
        for (PropPattern propPattern : retReplacewithEntityType) {
            this.record(matchInfo, propPattern);
        }
        for (PropPattern propPattern : retOriginalSpan) {
            this.record(matchInfo, propPattern);
        }
    }

    public static Optional<SynNode> getNode(Spanning span, DocTheory dt) {
        if (span instanceof Mention) {
            return Optional.of(((Mention) span).node());
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
