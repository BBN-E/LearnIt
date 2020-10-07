package com.bbn.akbc.neolearnit.observers.instance.pattern.Unary;

import com.bbn.akbc.common.Pair;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo.LanguageMatchInfo;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;
import com.bbn.akbc.neolearnit.observations.pattern.PropPattern;
import com.bbn.akbc.neolearnit.observations.pattern.PropPattern.PropArgObservation;
import com.bbn.akbc.neolearnit.observers.instance.pattern.MonolingualPatternObserver;
import com.bbn.akbc.neolearnit.util.GeneralUtils;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.Proposition;
import com.bbn.serif.theories.Proposition.Argument;
import com.bbn.serif.theories.Proposition.MentionArgument;
import com.bbn.serif.theories.Proposition.PropositionArgument;
import com.bbn.serif.theories.SynNode;
import com.bbn.serif.theories.TokenSequence;
import com.bbn.serif.types.EntityType;
import com.google.common.base.Optional;
import com.google.inject.BindingAnnotation;
import com.google.inject.Inject;

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

    static int MAX_DEPTH = 1; // TODO: there is a bug in going beyong 1 hop
    static int MAX_NUM_OF_PROP_ARGS = 2;
    static Set<EntityType> shouldNotCareEntityTypes = new HashSet<>(Arrays.asList(EntityType.undetermined(), EntityType.of("OTH")));


    private void DFS(MatchInfo matchInfo, LanguageMatchInfo languageMatchInfo, Proposition proposition, Set<PropPattern> retReplacewithEntityType, Set<PropPattern> retOriginalSpan, boolean predHeadShouldMatch, int depthRemaining, Set<Pair<TokenSequence.Span, Proposition.PredicateType>> visitedProposition) {
        if (proposition.predType().name().asString().equals("modifier"))
            return;

        List<SynNode> nodes = InstanceIdentifier.getNode(languageMatchInfo.getSlot0().get(), languageMatchInfo.docTheory());

        for (SynNode nodeObj : nodes) {
            Optional<SynNode> node = Optional.of(nodeObj);

            if (depthRemaining < 1) return;
            if (!node.isPresent()) return;
            if (!proposition.predHead().isPresent()) return;
            if (predHeadShouldMatch && !proposition.predHead().get().span().equals(node.get().span())) return;
            if (visitedProposition.contains(new Pair<>(proposition.span(), proposition.predType()))) return;

            visitedProposition.add(new Pair<>(proposition.span(), proposition.predType()));
            List<PropArgObservation> candidatesReplaceWithEntityType = new ArrayList<>();

            List<PropArgObservation> candidatesOriginalSpan = new ArrayList<>();

            for (Argument argument : proposition.args()) {
                if (!argument.role().isPresent()) continue;
                if (argument.role().get().asString().equals("<temp>") || argument.role().get().asString().equals("<ref>"))
                    continue;
                if (argument instanceof MentionArgument) {
                    MentionArgument mentionArgument = (MentionArgument) argument;
                    if (!shouldNotCareEntityTypes.contains(mentionArgument.mention().entityType())) {
                        candidatesReplaceWithEntityType.add(new PropArgObservation(Symbol.from(mentionArgument.role().get() + " " + mentionArgument.mention().entityType().name().asString())));
                    }
                    Symbol role = mentionArgument.role().get();
                    if (mentionArgument.mention().span().numTokens(languageMatchInfo.docTheory()) <= 5) {
                        String mentionText = mentionArgument.mention().span().tokenizedText().utf16CodeUnits();
                        candidatesOriginalSpan.add(new PropArgObservation(Symbol.from(role + " " + mentionText)));

                    }
                    String mentionHeadText = mentionArgument.mention().atomicHead().span().tokenizedText().utf16CodeUnits();
                    candidatesOriginalSpan.add(new PropArgObservation(Symbol.from(role + " " + mentionHeadText)));

                } else if (argument instanceof Proposition.TextArgument) {
                    Proposition.TextArgument textArgument = (Proposition.TextArgument) argument;
                    candidatesReplaceWithEntityType.add(new PropArgObservation(Symbol.from(textArgument.span().tokenizedText().utf16CodeUnits())));
                    candidatesOriginalSpan.add(new PropArgObservation(Symbol.from(textArgument.span().tokenizedText().utf16CodeUnits())));
                } else if (argument instanceof PropositionArgument) {
                    PropositionArgument propositionArgument = (PropositionArgument) argument;
                    Set<PropPattern> replaceWithEntityTypeSet = new HashSet<>();
                    Set<PropPattern> originalSpanSet = new HashSet<>();
                    DFS(matchInfo, languageMatchInfo, propositionArgument.proposition(), replaceWithEntityTypeSet, originalSpanSet, false, depthRemaining - 1, visitedProposition);
                    if (replaceWithEntityTypeSet.isEmpty() && originalSpanSet.isEmpty()) {
                        PropPattern propOnlyPredicate = (new PropPattern.Builder(language, propositionArgument.proposition().predType()).withPredicate(proposition.predSymbol().get())).build();
                        candidatesOriginalSpan.add(new PropArgObservation(argument.role().get(), propOnlyPredicate));
                    } else {
                        for (PropPattern propPattern : replaceWithEntityTypeSet) {
                            candidatesReplaceWithEntityType.add(new PropArgObservation(argument.role().get(), propPattern));
                        }
                        for (PropPattern propPattern : originalSpanSet) {
                            candidatesOriginalSpan.add(new PropArgObservation(argument.role().get(), propPattern));
                        }
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
            if (propPattern.args().size() > 0) {
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

            visitedProposition.remove(new Pair<>(proposition.span(), proposition.predType()));
        }
    }

    @Override
    public void observe(MatchInfo matchInfo, LanguageMatchInfo languageMatchInfo) {
        List<SynNode> nodes = InstanceIdentifier.getNode(languageMatchInfo.firstSpanning(), languageMatchInfo.docTheory());
        if (nodes.isEmpty()) return;
        Set<PropPattern> retReplacewithEntityType = new HashSet<>();
        Set<PropPattern> retOriginalSpan = new HashSet<>();
        for (Proposition proposition : languageMatchInfo.getSentTheory().propositions()) {
            DFS(matchInfo, languageMatchInfo, proposition, retReplacewithEntityType, retOriginalSpan, true, MAX_DEPTH, new HashSet<>());
        }
        for (PropPattern propPattern : retReplacewithEntityType) {
            this.record(matchInfo, propPattern);
        }
        for (PropPattern propPattern : retOriginalSpan) {
            this.record(matchInfo, propPattern);
        }
    }
}
