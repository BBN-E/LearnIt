package com.bbn.akbc.neolearnit.mappings.filters;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.common.targets.TargetSlot;
import com.bbn.akbc.neolearnit.common.targets.constraints.MatchConstraint;
import com.bbn.akbc.neolearnit.common.targets.constraints.impl.SpanningTypeConstraint;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.utility.Pair;

import java.util.*;

public class TargetFilterWithCache implements MappingsFilter {

    Target focusTarget;
    Map<Pair<String, String>, Mappings> spanningStrToFilteredMappings;

    Mappings originalMappings;
    boolean shouldFollowOriginalTargetFilter;

    public TargetFilterWithCache(boolean shouldFollowOriginalTargetFilter) {
        this.spanningStrToFilteredMappings = new HashMap<>();
        this.focusTarget = null;
        this.originalMappings = null;
        this.shouldFollowOriginalTargetFilter = shouldFollowOriginalTargetFilter;
    }

    public void setFocusTarget(Target newFocusTarget) {
        this.focusTarget = newFocusTarget;
    }

    public void setShouldFollowOriginalTargetFilter(boolean shouldFollowOriginalTargetFilter) {
        this.shouldFollowOriginalTargetFilter = shouldFollowOriginalTargetFilter;
    }

    @Override
    public Mappings makeFiltered(Mappings input) {
        if (this.originalMappings == null) {
            this.originalMappings = input;
        } else if (this.originalMappings != input) {
            throw new RuntimeException("You have to serve the EXACT the same original mappings, or the behavior of caching is unknown!!!");
        }
        List<String> slot0AllowedTypes = new ArrayList<>();
        List<String> slot1AllowedTypes = new ArrayList<>();
        for (MatchConstraint matchConstraint : this.focusTarget.getConstraints()) {
            if (matchConstraint instanceof SpanningTypeConstraint) {
                SpanningTypeConstraint spanningTypeConstraint = (SpanningTypeConstraint) matchConstraint;
                if (spanningTypeConstraint.getSlot() == 0) {
                    for (InstanceIdentifier.SpanningType spanningType : spanningTypeConstraint.getAllowedSpanningType()) {
                        slot0AllowedTypes.add(spanningType.toString());
                    }
                } else if (spanningTypeConstraint.getSlot() == 1) {
                    for (InstanceIdentifier.SpanningType spanningType : spanningTypeConstraint.getAllowedSpanningType()) {
                        slot1AllowedTypes.add(spanningType.toString());
                    }
                } else {
                    throw new com.bbn.bue.common.exceptions.NotImplementedException();
                }
            }
        }
        Collections.sort(slot0AllowedTypes);
        Collections.sort(slot1AllowedTypes);
        String slot0AllowedStr = String.join(",", slot0AllowedTypes);
        String slot1AllowedStr = String.join(",", slot1AllowedTypes);
        if (this.spanningStrToFilteredMappings.containsKey(new Pair<>(slot0AllowedStr, slot1AllowedStr))) {
            return this.spanningStrToFilteredMappings.get(new Pair<>(slot0AllowedStr, slot1AllowedStr));
        } else {
            Set<InstanceIdentifier.SpanningType> slot0Types = new HashSet<>();
            Set<InstanceIdentifier.SpanningType> slot1Types = new HashSet<>();
            for (String slot0Type : slot0AllowedTypes) {
                slot0Types.add(InstanceIdentifier.SpanningType.valueOf(slot0Type));
            }
            for (String slot1Type : slot1AllowedTypes) {
                slot1Types.add(InstanceIdentifier.SpanningType.valueOf(slot1Type));
            }
            Target dummyTarget = new Target.Builder("DUMMY")
                    .withAddedConstraint(new SpanningTypeConstraint(0, slot0Types))
                    .withAddedConstraint(new SpanningTypeConstraint(1, slot1Types)).build();
            TargetFilter spanningFilter = new TargetFilter(dummyTarget);
            Mappings filteredMappings = spanningFilter.makeFiltered(input);
            if (slot0AllowedStr.length() > 0 && slot1AllowedStr.length() > 0) {
                this.spanningStrToFilteredMappings.put(new Pair<>(slot0AllowedStr, slot1AllowedStr), filteredMappings);
            } else {
                System.out.println("Target " + this.focusTarget.getName() + " is not supporting caching due to it's missing SpanningTypeConstraint on one or more slots, please recreate and re-serialize target when available.");
            }
            if (this.shouldFollowOriginalTargetFilter) {
                TargetFilter targetFilterNormal = new TargetFilter(this.focusTarget);
                filteredMappings = targetFilterNormal.makeFiltered(filteredMappings);
            }
            return filteredMappings;
        }
    }
}
