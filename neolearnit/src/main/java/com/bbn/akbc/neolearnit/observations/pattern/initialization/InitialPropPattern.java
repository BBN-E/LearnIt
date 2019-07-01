package com.bbn.akbc.neolearnit.observations.pattern.initialization;

import com.bbn.bue.common.StringUtils;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.pattern.PropPattern;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by mcrivaro on 8/1/2014.
 */
public class InitialPropPattern extends InitializationPattern {

    private static final Set<String> SPECIAL_ROLES = ImmutableSet.of("sub","obj","mod","poss","iobj","ref");

    private final Symbol propType;
    private final Set<Symbol> predicates;
    private final Set<Symbol> predicateWildcards;
    private final Map<Integer,Set<Symbol>> roles;

    protected InitialPropPattern(String string, Target t) {
        super(t);
        String[] info = string.split(" ");

        //Handle a situation like "v:" or "noun:" or "v."
        boolean needsTrim = !Character.isLetter(info[0].charAt(info[0].length()-1));
        propType = needsTrim ? Symbol.from(info[0].substring(0,info[0].length()-1).toLowerCase()) : Symbol.from(info[0].toLowerCase());

        ImmutableSet.Builder<Symbol> predicatesBuilder = ImmutableSet.builder();
        ImmutableSet.Builder<Symbol> predicateWildsBuilder = ImmutableSet.builder();
        for (String pred : info[1].split(",")) {
            if (pred.charAt(pred.length()-1) == '*')
                predicateWildsBuilder.add(Symbol.from(pred.toLowerCase().substring(0,pred.length()-1)));
            else
                predicatesBuilder.add(Symbol.from(pred.toLowerCase()));
        }
        predicates = predicatesBuilder.build();
        predicateWildcards = predicateWildsBuilder.build();

        ImmutableMap.Builder<Integer,Set<Symbol>> rolesBuilder = ImmutableMap.builder();
        for (int i = 2; i < info.length; i++) {
            int slot = Integer.parseInt(info[i].substring(0,1));
            ImmutableSet.Builder<Symbol> builder = ImmutableSet.builder();
            for (String role : info[i].substring(2).split(",")) {
                if (SPECIAL_ROLES.contains(role)) builder.add(Symbol.from("<"+role+">"));
                else builder.add(Symbol.from(role));
            }
            rolesBuilder.put(slot,builder.build());
        }
        roles = rolesBuilder.build();
    }

    private boolean hasOverlappingWildcardPred(Symbol predicate) {
        for (Symbol wildcardPred : predicateWildcards) {
            if (predicate.toString().startsWith(wildcardPred.toString()))
                return true;
        }
        return false;
    }

    private boolean matchesArgs(List<PropPattern.PropArgObservation> args, Map<Integer,Set<Symbol>> roleMap) {
        for (PropPattern.PropArgObservation arg : args) {
            if (arg.getSlot().isPresent() && roleMap.containsKey(arg.slot())) {
                if (!roleMap.get(arg.slot()).contains(arg.getRole()))
                    return false;
            }
        }
        return true;
    }

    @Override
    public boolean matchesPattern(LearnitPattern p) {
        if (!(p instanceof PropPattern))
            return false;
        PropPattern prop = (PropPattern)p;
        if (!prop.getPredicateType().toString().startsWith(propType.toString()))
            return false;
        for (Symbol predicate : prop.getPredicates()) {
            if (!predicates.contains(predicate) && !hasOverlappingWildcardPred(predicate))
                return false;
        }
        //Try to match up the args. If symmetric, try reversing the args and matching
        if (matchesArgs(prop.getArgs(), roles)) return true;
        else if (target.isSymmetric()) {
            ImmutableMap.Builder<Integer,Set<Symbol>> symRoles = ImmutableMap.builder();
            for (int slot : roles.keySet())
                symRoles.put((1+slot)%2, roles.get(slot));
            return matchesArgs(prop.getArgs(), symRoles.build());
        } else return false;
    }

    private String roleString() {
        StringBuilder builder = new StringBuilder();
        for (int i=0; i <= 1; ++i) {
            if (roles.containsKey(i))
                builder.append(String.format("%d: %s ", i, StringUtils.CommaJoin.apply(roles.get(i))));
        }
        return builder.toString();
    }

    @Override
    public String toString() {
        return String.format("%s: %s %s", propType, StringUtils.CommaJoin.apply(Sets.union(predicates,predicateWildcards)), roleString());
    }
}
