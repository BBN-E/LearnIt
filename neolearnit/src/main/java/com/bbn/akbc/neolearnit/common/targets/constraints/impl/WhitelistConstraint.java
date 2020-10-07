package com.bbn.akbc.neolearnit.common.targets.constraints.impl;

import com.bbn.akbc.common.Pair;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.common.targets.constraints.MatchConstraint;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.bue.common.exceptions.NotImplementedException;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class WhitelistConstraint implements MatchConstraint {


    final WhitelistFilter unaryEventFilter;
    final WhitelistFilter unaryEntityFilter;
    final WhitelistFilter unaryValueMentionFilter;
    final WhitelistFilter binaryEventEventFilter;
    final WhitelistFilter binaryEventMentionFilter;
    final WhitelistFilter binaryEventValueMentionFilter;
    final WhitelistFilter binaryMentionMentionFilter;
    final WhitelistFilter binaryValueMentionMentionFilter;

    public WhitelistConstraint() throws IOException {
        String whiteListRoot = String.format("%s/inputs/whitelists/migration/", LearnItConfig.get("learnit_root"));
        File unaryEvent = new File(whiteListRoot + File.separator + "unaryEvent.txt");
        File unaryEntity = new File(whiteListRoot + File.separator + "unaryEntity.txt");
        File unaryValueMention = new File(whiteListRoot + File.separator + "unaryValueMention.txt");
        File binaryEventEvent = new File(whiteListRoot + File.separator + "binaryEventEvent.txt");
        File binaryEventMention = new File(whiteListRoot + File.separator + "binaryEventMention.txt");
        File binaryEventValueMention = new File(whiteListRoot + File.separator + "binaryEventValueMention.txt");
        File binaryMentionMention = new File(whiteListRoot + File.separator + "binaryMentionMention.txt");
        File binaryValueMentionMention = new File(whiteListRoot + File.separator + "binaryValueMentionMention.txt");

        this.unaryEventFilter = new WhitelistFilter(unaryEvent, true);
        this.unaryEntityFilter = new WhitelistFilter(unaryEntity, true);
        this.unaryValueMentionFilter = new WhitelistFilter(unaryValueMention, true);
        this.binaryEventEventFilter = new WhitelistFilter(binaryEventEvent, true);
        this.binaryEventMentionFilter = new WhitelistFilter(binaryEventMention, true);
        this.binaryEventValueMentionFilter = new WhitelistFilter(binaryEventValueMention, true);
        this.binaryMentionMentionFilter = new WhitelistFilter(binaryMentionMention, true);
        this.binaryValueMentionMentionFilter = new WhitelistFilter(binaryValueMentionMention, true);
    }

    public static String getSpanText(EventMention eventMention) {
        return eventMention.anchorNode().span().tokenizedText().utf16CodeUnits();
    }

    public static String getSpanText(Mention mention) {
        return mention.atomicHead().span().tokenizedText().utf16CodeUnits();
    }

    public static String getSpanText(ValueMention valueMention) {
        return valueMention.span().tokenizedText().utf16CodeUnits();
    }

    public boolean unaryEventJudger(EventMention slot0, SentenceTheory sentenceTheory) {
        return this.unaryEventFilter.shouldKeepLeft(getSpanText(slot0));
    }

    public boolean unaryEntityJudger(Mention slot0, SentenceTheory sentenceTheory) {
        return this.unaryEntityFilter.shouldKeepLeft(getSpanText(slot0));
    }

    public boolean unaryValueMentionJudger(ValueMention slot0, SentenceTheory sentenceTheory) {
        return this.unaryValueMentionFilter.shouldKeepLeft(getSpanText(slot0));
    }

    public boolean binaryEventEventJudger(EventMention slot0, EventMention slot1, SentenceTheory sentenceTheory) {
        return this.binaryEventEventFilter.shouldKeepLeft(getSpanText(slot0)) && this.binaryEventEventFilter.shouldKeepRight(getSpanText(slot1)) && this.binaryEventEventFilter.shouldKeepPair(getSpanText(slot0), getSpanText(slot1));
    }

    public boolean binaryEventMentionJudger(EventMention slot0, Mention slot1, SentenceTheory sentenceTheory) {
        return this.binaryEventMentionFilter.shouldKeepLeft(getSpanText(slot0)) && this.binaryEventMentionFilter.shouldKeepRight(getSpanText(slot1)) && this.binaryEventMentionFilter.shouldKeepPair(getSpanText(slot0), getSpanText(slot1));
    }

    public boolean binaryEventValueMentionJudger(EventMention slot0, ValueMention slot1, SentenceTheory sentenceTheory) {
        return this.binaryEventValueMentionFilter.shouldKeepLeft(getSpanText(slot0)) && this.binaryEventValueMentionFilter.shouldKeepRight(getSpanText(slot1)) && this.binaryEventValueMentionFilter.shouldKeepPair(getSpanText(slot0), getSpanText(slot1));
    }

    public boolean binaryMentionEventJudger(Mention slot0, EventMention slot1, SentenceTheory sentenceTheory) {
        return false;
    }

    public boolean binaryMentionMentionJudger(Mention slot0, Mention slot1, SentenceTheory sentenceTheory) {
        return this.binaryMentionMentionFilter.shouldKeepLeft(getSpanText(slot0)) && this.binaryMentionMentionFilter.shouldKeepRight(getSpanText(slot1)) && this.binaryMentionMentionFilter.shouldKeepPair(getSpanText(slot0), getSpanText(slot1));
    }

    public boolean binaryMentionValueMentionJudger(Mention slot0, ValueMention slot1, SentenceTheory sentenceTheory) {
        return false;
    }

    public boolean binaryValueMentionEventJudger(ValueMention slot0, EventMention slot1, SentenceTheory sentenceTheory) {
        return false;
    }

    public boolean binaryValueMentionMentionJudger(ValueMention slot0, Mention slot1, SentenceTheory sentenceTheory) {
        return this.binaryValueMentionMentionFilter.shouldKeepLeft(getSpanText(slot0)) && this.binaryValueMentionMentionFilter.shouldKeepRight(getSpanText(slot1)) && this.binaryValueMentionMentionFilter.shouldKeepPair(getSpanText(slot0), getSpanText(slot1));
    }

    public boolean binaryValueMentionValueMentionJudger(ValueMention slot0, ValueMention slot1, SentenceTheory sentenceTheory) {
        return false;
    }

    @Override
    public boolean valid(MatchInfo match) {
        MatchInfo.LanguageMatchInfo languageMatchInfo = match.getPrimaryLanguageMatch();
        SentenceTheory sentenceTheory = languageMatchInfo.getSentTheory();
        if (!languageMatchInfo.getSlot1().isPresent()) {

            Spanning slot0 = languageMatchInfo.getSlot0().get();
            if (slot0 instanceof EventMention) {
                return unaryEventJudger((EventMention) slot0, sentenceTheory);
            } else if (slot0 instanceof Mention) {
                return unaryEntityJudger((Mention) slot0, sentenceTheory);
            } else if (slot0 instanceof ValueMention) {
                return unaryValueMentionJudger((ValueMention) slot0, sentenceTheory);
            }

        } else {
            Spanning slot0 = languageMatchInfo.getSlot0().get();
            Spanning slot1 = languageMatchInfo.getSlot1().get();
            if ((slot0 instanceof EventMention) && (slot1 instanceof EventMention)) {
                return binaryEventEventJudger((EventMention) slot0, (EventMention) slot1, sentenceTheory);
            } else if ((slot0 instanceof EventMention) && (slot1 instanceof Mention)) {
                return binaryEventMentionJudger((EventMention) slot0, (Mention) slot1, sentenceTheory);
            } else if ((slot0 instanceof EventMention) && (slot1 instanceof ValueMention)) {
                return binaryEventValueMentionJudger((EventMention) slot0, (ValueMention) slot1, sentenceTheory);
            } else if ((slot0 instanceof Mention) && (slot1 instanceof EventMention)) {
                return binaryMentionEventJudger((Mention) slot0, (EventMention) slot1, sentenceTheory);
            } else if ((slot0 instanceof Mention) && (slot1 instanceof Mention)) {
                return binaryMentionMentionJudger((Mention) slot0, (Mention) slot1, sentenceTheory);
            } else if ((slot0 instanceof Mention) && (slot1 instanceof ValueMention)) {
                return binaryMentionValueMentionJudger((Mention) slot0, (ValueMention) slot1, sentenceTheory);
            } else if ((slot0 instanceof ValueMention) && (slot1 instanceof EventMention)) {
                return binaryValueMentionEventJudger((ValueMention) slot0, (EventMention) slot1, sentenceTheory);
            } else if ((slot0 instanceof ValueMention) && (slot1 instanceof Mention)) {
                return binaryValueMentionMentionJudger((ValueMention) slot0, (Mention) slot1, sentenceTheory);
            } else if ((slot0 instanceof ValueMention) && (slot1 instanceof ValueMention)) {
                return binaryValueMentionValueMentionJudger((ValueMention) slot0, (ValueMention) slot1, sentenceTheory);
            }
        }
        return false;
    }

    @Override
    public boolean valid(InstanceIdentifier instanceId, Collection<Seed> seeds, Target t) {
        throw new NotImplementedException("This Constraint filter is not meant to be called in filtering instanceId. In order to to that, you need to pass in the LanguageMatchInfo which defect the purpose.");
    }

    @Override
    public boolean offForEvaluation() {
        return false;
    }

    public static class WhitelistFilter {
        final Set<Symbol> left;
        final Set<Symbol> right;
        final Set<Pair<Symbol, Symbol>> pair;
        final boolean convertToLower;

        final boolean s0AlwaysPass;
        final boolean s1AlwaysPass;
        final boolean s0s1AlwaysPass;


        public WhitelistFilter(Set<Symbol> left, Set<Symbol> right, Set<Pair<Symbol, Symbol>> pair, boolean s0AlwaysPass, boolean s1AlwaysPass, boolean s0s1AlwaysPass, boolean convertToLower) {
            this.left = left;
            this.right = right;
            this.pair = pair;
            this.convertToLower = convertToLower;
            this.s0AlwaysPass = s0AlwaysPass;
            this.s1AlwaysPass = s1AlwaysPass;
            this.s0s1AlwaysPass = s0s1AlwaysPass;
        }

        public WhitelistFilter(File blacklistFile, boolean convertToLower) throws IOException {
            this.left = new HashSet<>();
            this.right = new HashSet<>();
            this.pair = new HashSet<>();
            this.convertToLower = convertToLower;
            boolean s0AlwaysPass = false;
            boolean s1AlwaysPass = false;
            boolean s0s1AlwaysPass = false;
            BufferedReader br = new BufferedReader(new FileReader(blacklistFile));
            String line;
            while ((line = br.readLine()) != null) {
                String[] split = line.split("\t");
                String word0;
                String word1;
                switch (split[0]) {
                    case "0":
                        word0 = split[1].trim();
                        if (word0.equals("*")) {
                            s0AlwaysPass = true;
                            break;
                        }
                        if (convertToLower) word0 = word0.toLowerCase();
                        this.left.add(Symbol.from(word0));
                        break;
                    case "1":
                        word1 = split[1].trim();
                        if (word1.equals("*")) {
                            s1AlwaysPass = true;
                            break;
                        }
                        if (convertToLower) word1 = word1.toLowerCase();
                        this.left.add(Symbol.from(word1));
                        break;
                    case "01":
                        word0 = split[1].trim();
                        word1 = split[2].trim();
                        if (word0.equals("*")) {
                            s0s1AlwaysPass = true;
                            break;
                        }
                        if (convertToLower) {
                            word0 = word0.toLowerCase();
                            word1 = word1.toLowerCase();
                        }
                        this.left.add(Symbol.from(word0));
                        this.right.add(Symbol.from(word1));
                        break;
                }
            }
            this.s0AlwaysPass = s0AlwaysPass;
            this.s1AlwaysPass = s1AlwaysPass;
            this.s0s1AlwaysPass = s0s1AlwaysPass;
        }

        public boolean shouldKeepLeft(String left) {
            if (s0AlwaysPass) return true;
            if (convertToLower) {
                left = left.toLowerCase();
            }
            return this.left.contains(Symbol.from(left));
        }

        public boolean shouldKeepRight(String right) {
            if (s1AlwaysPass) return true;
            if (convertToLower) {
                right = right.toLowerCase();
            }
            return this.right.contains(Symbol.from(right));
        }

        public boolean shouldKeepPair(String left, String right) {
            if (s0s1AlwaysPass) return true;
            if (convertToLower) {
                left = left.toLowerCase();
                right = right.toLowerCase();
            }
            return this.pair.contains(new Pair<>(Symbol.from(left), Symbol.from(right)));
        }

    }
}
