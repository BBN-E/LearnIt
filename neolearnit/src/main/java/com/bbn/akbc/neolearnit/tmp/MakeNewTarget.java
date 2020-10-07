package com.bbn.akbc.neolearnit.tmp;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.common.targets.TargetSlot;
import com.bbn.akbc.neolearnit.common.targets.constraints.impl.SpanningTypeConstraint;
import com.google.common.collect.Lists;

import java.util.Arrays;
import java.util.List;

public class MakeNewTarget {
    public static void main(String[] args) throws Exception {

        List<String> newTargetNames = Lists.newArrayList("Before-After", "Catalyst-Effect", "Cause-Effect", "MitigatingFactor-Effect", "Precondition-Effect", "Preventative-Effect");

        for (String name : newTargetNames) {
            Target newTarget = new Target.Builder(name)
                    .withTargetSlot(new TargetSlot.Builder(0, "all")
                            .build()).withTargetSlot(new TargetSlot.Builder(1, "all")
                            .build())
                    .withAddedConstraint(new SpanningTypeConstraint(0, Arrays.asList(InstanceIdentifier.SpanningType.EventMention)))
                    .withAddedConstraint(new SpanningTypeConstraint(1, Arrays.asList(InstanceIdentifier.SpanningType.EventMention)))
                    .build();
            String targetPathRel = String.format("inputs/targets/json/%s.json", name);
            String targetPathFull = String.format("%s/%s", "/home/hqiu/ld100/learnit", targetPathRel);
            newTarget.serialize(targetPathFull);

        }
    }
}
