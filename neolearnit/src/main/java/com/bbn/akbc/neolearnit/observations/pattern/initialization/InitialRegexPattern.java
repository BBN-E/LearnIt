package com.bbn.akbc.neolearnit.observations.pattern.initialization;

import com.bbn.bue.common.StringUtils;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.bue.common.symbols.SymbolUtils;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.observations.pattern.BetweenSlotsPattern;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.pattern.RegexableContent;
import com.bbn.akbc.neolearnit.observations.pattern.SymbolContent;
import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Created by mcrivaro on 8/4/2014.
 */
public class InitialRegexPattern extends InitializationPattern
{
    private static final Symbol WILDCARD_SYMBOL = Symbol.from("<*>");

    private final int firstSlot;
    private final int secondSlot;
    private final List<Symbol> content;

    protected InitialRegexPattern(String patternString, Target t) {
        super(t);

        if (!(patternString.startsWith("<0>") || patternString.startsWith("<1>")) && !(patternString.endsWith("<0>") || patternString.endsWith("<1>")))
            throw new RuntimeException("Invalid Initial Regex Pattern String: " + patternString);

        firstSlot = Integer.parseInt(patternString.substring(1,2));
        secondSlot = Integer.parseInt(patternString.substring(patternString.length()-2,patternString.length()-1));

        List<String> tokens = ImmutableList.copyOf(patternString.split(" "));
        content = SymbolUtils.listFrom(tokens.subList(1,tokens.size()-1));
    }

    private boolean matchesContent(Symbol s, RegexableContent r) {
        return s == WILDCARD_SYMBOL || (r instanceof SymbolContent && ((SymbolContent)r).getSymbol() == s);
    }

    @Override
    public boolean matchesPattern(LearnitPattern p) {
        if(!(p instanceof BetweenSlotsPattern))
            return false;
        BetweenSlotsPattern slotsPattern = (BetweenSlotsPattern)p;
        if (!target.isSymmetric() && (slotsPattern.getFirstSlot() != firstSlot || slotsPattern.getSecondSlot() != secondSlot))
            return false;
        if (slotsPattern.getContent().getContent().size() != content.size())
            return false;
        for (int i=0; i < content.size(); ++i) {
            if (!matchesContent(content.get(i), slotsPattern.getContent().getContent().get(i)))
                return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return String.format("<%d> %s <%d>", firstSlot, StringUtils.SpaceJoin.apply(content), secondSlot);
    }
}
