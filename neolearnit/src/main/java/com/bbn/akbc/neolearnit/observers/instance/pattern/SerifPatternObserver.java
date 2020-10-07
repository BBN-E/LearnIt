package com.bbn.akbc.neolearnit.observers.instance.pattern;

import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;
import com.bbn.akbc.neolearnit.observations.pattern.SerifPattern;
import com.bbn.serif.patterns.Pattern;
import com.bbn.serif.patterns.PatternGenerator;
import com.bbn.serif.patterns.PatternSexpParsingException;
import com.bbn.serif.patterns.PropPattern;
import com.bbn.serif.patterns.PatternGenerator.CONSTRAINTS;
import com.bbn.serif.theories.*;

import com.google.common.base.Optional;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import static com.bbn.serif.patterns.PatternGenerator.CONSTRAINTS.*;

public class SerifPatternObserver extends MonolingualPatternObserver {

    private final static Set<String> badPatternPrettyStrSet = new HashSet<>(Arrays.asList("[1] [0]","[0] [1]"));
    private final int maxUnaryPropDepth;
    private final int maxBinaryPropDepth;
    private final int maxInBetweenWord;
    private final Map<DocTheory,PatternGenerator> docTheoryToPatternGenerator;

    // constraint sets
    private static Set<Set<CONSTRAINTS>> possibleConstraintsForMention;
    private static Set<Set<CONSTRAINTS>> possibleConstraintsForUnaryEventProp;
    private static Set<Set<CONSTRAINTS>> possibleConstraintsForBinaryEventArgProp;
    private static Set<Set<CONSTRAINTS>> possibleConstraintsForBinaryEventEventProp;
    private static Set<Set<CONSTRAINTS>> possibleConstraintsForBinaryEntityEntityProp;
    private static Set<Set<CONSTRAINTS>> possibleConstraintsForLexical;

    static {
        // Mention
        possibleConstraintsForMention = new HashSet<>();
        possibleConstraintsForMention
            .add(new HashSet<>(Arrays.asList(SLOT_MENTION_HEAD_WORD, MENTION_PREMOD)));

        // Unary prop
        possibleConstraintsForUnaryEventProp = new HashSet<>();
        possibleConstraintsForUnaryEventProp
            .add(new HashSet<>(Arrays.asList(PREDICATE_HEAD, SLOT_PREDICATE_HEAD, ARGUMENT_ROLE,
                PROPOSITION_ARGUMENT, MENTION_ARGUMENT, MENTION_ENTITY_TYPE, MENTION_HEAD_WORD,
                MENTION_PREMOD, PRUNE_SIMPLE_REFS, NO_LIST_PROPS)));

        // Event Mention - Event Arg
        possibleConstraintsForBinaryEventArgProp = new HashSet<>();
        possibleConstraintsForBinaryEventArgProp
            .add(new HashSet<>(Arrays.asList(PREDICATE_HEAD, SLOT_PREDICATE_HEAD,
                SLOT_MENTION_ENTITY_TYPE, PROPOSITION_ARGUMENT, ARGUMENT_ROLE,
                VALUE_MENTION_VALUE_TYPE)));

        // Event Mention - Event Mention
        possibleConstraintsForBinaryEventEventProp = new HashSet<>();
        possibleConstraintsForBinaryEventEventProp
            .add(new HashSet<>(Arrays.asList(PREDICATE_HEAD, PROPOSITION_ARGUMENT, ARGUMENT_ROLE,
                PRUNE_SIMPLE_REFS, NO_LIST_PROPS)));

        // Entity Mention - Entity Mention
        possibleConstraintsForBinaryEntityEntityProp = new HashSet<>();
        possibleConstraintsForBinaryEntityEntityProp.add(new HashSet<>(Arrays.asList(PREDICATE_HEAD,
                SLOT_PREDICATE_HEAD,SLOT_MENTION_ENTITY_TYPE,SLOT_MENTION_HEAD_WORD,
                PROPOSITION_ARGUMENT, ARGUMENT_ROLE)));

        // Lexical
        possibleConstraintsForLexical = new HashSet<>();
        possibleConstraintsForLexical.add(new HashSet<>(Arrays.asList()));
    }

    private ReentrantLock lock = new ReentrantLock();

    public SerifPatternObserver(
        InstanceToPatternMapping.Builder recorder, String language,
        int maxUnaryPropDepth, int maxBinaryPropDepth, int maxInBetweenWord)
    {
        super(recorder, language);
        this.maxUnaryPropDepth = maxUnaryPropDepth;
        this.maxBinaryPropDepth = maxBinaryPropDepth;
        this.maxInBetweenWord = maxInBetweenWord;
        this.docTheoryToPatternGenerator = new ConcurrentHashMap<>();
    }

    private boolean unaryPatternTooGeneral(PropPattern propPattern) {
        return propPattern.getPredicates().size() == 0;
    }

    @Override
    public void observe(MatchInfo match, MatchInfo.LanguageMatchInfo languageMatch) {
        // observe may be called simultaneously in several threads, so lock before updating the
        // docTheory/PatternGenerator cache
        lock.lock();
        DocTheory docTheory = languageMatch.docTheory();
        if (!docTheoryToPatternGenerator.containsKey(docTheory)) {
            // * TODO: mentionPremodStopWords to be added as second parameter to constructor
            docTheoryToPatternGenerator
                .put(docTheory, new PatternGenerator(docTheory, new HashSet<>()));
        }
        PatternGenerator patternGenerator = docTheoryToPatternGenerator.get(docTheory);
        lock.unlock();

        Spanning slot0 = languageMatch.getSlot0().get();
        Optional<Spanning> slot1Opt = languageMatch.getSlot1();
        boolean isUnary = !slot1Opt.isPresent();
        boolean isBinary = slot1Opt.isPresent();


        List<Pattern> generatedPatterns = new ArrayList<>();

        // Unary event generation patterns
        if (isUnary && slot0 instanceof EventMention) {
            EventMention slot0EventMention = (EventMention) slot0;

            for (Set<CONSTRAINTS> constraintSubSet : possibleConstraintsForUnaryEventProp) {
                List<Proposition> startingProps = getMatchingPropositions(
                    slot0EventMention, slot0EventMention.sentenceTheory(docTheory));

                for (Proposition startingProp : startingProps) {
                    List<Pattern> unaryPropPatterns = patternGenerator
                        .generateUnaryPropPatternsGeneral(startingProp, startingProp,
                            maxUnaryPropDepth, constraintSubSet);
                    for (Pattern p : unaryPropPatterns)
                        if (!unaryPatternTooGeneral((PropPattern) p))
                            generatedPatterns.add(p);
                }
            }

            for (Set<CONSTRAINTS> constraintSubSet : possibleConstraintsForMention) {
                List<Pattern> unaryMentionPatterns = patternGenerator
                    .generateUnaryMentionPatterns((EventMention) slot0, constraintSubSet);
                generatedPatterns.addAll(unaryMentionPatterns);
            }

        }

        // Unary mention patterns

        if (isUnary && (slot0 instanceof Mention)){
            for (Set<CONSTRAINTS> constraintSubSet : possibleConstraintsForMention) {
                List<Pattern> unaryMentionPatterns = patternGenerator
                        .generateUnaryMentionPatterns(((Mention) slot0).node(), constraintSubSet);
                generatedPatterns.addAll(unaryMentionPatterns);
            }
        }

        // Binary event - event patterns
        if (isBinary && slot0 instanceof EventMention && slot1Opt.get() instanceof EventMention) {
            Spanning slot1 = slot1Opt.get();

            for (Set<CONSTRAINTS> constraintSubSet : possibleConstraintsForBinaryEventEventProp) {
                List<Pattern> patterns =
                    patternGenerator.generateBinaryPropPatterns(
                        (EventMention) slot0, (EventMention) slot1, maxBinaryPropDepth,
                        constraintSubSet);
                generatedPatterns.addAll(patterns);
            }

            for (Set<CONSTRAINTS> constraintSubSet : possibleConstraintsForLexical) {
                List<Pattern> patterns = patternGenerator.generateBinaryLexicalPatterns(
                    (EventMention) slot0, (EventMention) slot1, maxInBetweenWord,
                    constraintSubSet);
                generatedPatterns.addAll(patterns);
            }
        }

        // Binary event - argument patterns
        if (isBinary && slot0 instanceof EventMention &&
            (slot1Opt.get() instanceof Mention || slot1Opt.get() instanceof ValueMention)) {

            Spanning slot1 = slot1Opt.get();

            for (Set<CONSTRAINTS> constraintSubSet : possibleConstraintsForBinaryEventArgProp) {
                List<Pattern> patterns =
                    patternGenerator.generateBinaryPropPatterns(
                        (EventMention) slot0, slot1, maxBinaryPropDepth, constraintSubSet);
                generatedPatterns.addAll(patterns);
            }

            if (slot1Opt.get() instanceof Mention) {
                for (Set<CONSTRAINTS> constraintSubSet : possibleConstraintsForLexical) {
                    List<Pattern> patterns = patternGenerator.generateBinaryLexicalPatterns(
                        (EventMention) slot0, (Mention) slot1, maxInBetweenWord,
                        constraintSubSet);
                    generatedPatterns.addAll(patterns);
                }
            }
        }

        // Binary entity - entity patterns
        if(isBinary && slot0 instanceof Mention && slot1Opt.get() instanceof Mention){
            for(Set<CONSTRAINTS> constraintsSubSet: possibleConstraintsForBinaryEntityEntityProp){
                List<Pattern> patterns = patternGenerator.generateBinaryPropPatterns(
                        (Mention) slot0,(Mention)slot1Opt.get(),
                        maxBinaryPropDepth,constraintsSubSet);
                generatedPatterns.addAll(patterns);
            }
            for (Set<CONSTRAINTS> constraintSubSet : possibleConstraintsForLexical) {
                List<Pattern> patterns = patternGenerator.generateBinaryLexicalPatterns(
                        (Mention) slot0, (Mention) slot1Opt.get(), maxInBetweenWord,
                        constraintSubSet);
                generatedPatterns.addAll(patterns);
            }
        }

        for (Pattern pattern : generatedPatterns) {
            // System.out.println(pattern.toPrettyString());

            if(!badPatternPrettyStrSet.contains(pattern.toPrettyString())){
                try {
                    Pattern anotherPattern = SerifPattern.construcPatternFromSexpStr(pattern.toString());
                    if (pattern.toString().equals(anotherPattern.toString())) {
                        //System.out.println(pattern.toString() + "\n\n" + pattern.toPrettyString() + "\n\n");
                        this.record(match, new SerifPattern(pattern));
                    }
                    else{
                        //System.err.println("[SerifPatternObserver]Not equal pattern string detected " + pattern.toString().replace('\t', ' ').replace('\n', ' ') +  " and " +  anotherPattern.toString().replace('\t', ' ').replace('\n', ' ') + " . Dropping it.");
                    }
                } catch (PatternSexpParsingException | IOException e) {
//                System.err.println("[SerifPatternObserver]Cannot read " + pattern.toString().replace('\t', ' ').replace('\n', ' ') + " back. Dropping it.");
                }
            }
        }
    }

    private List<Proposition> getMatchingPropositions(EventMention em, SentenceTheory st) {
        List<Proposition> results = new ArrayList<>();

        for (Proposition p : st.propositions()) {
            if (!p.predHead().isPresent())
                continue;
            SynNode propHead = p.predHead().get().headPreterminal();
            if (propHead == em.anchorNode().headPreterminal())
                results.add(p);
        }
        return results;
    }
}
