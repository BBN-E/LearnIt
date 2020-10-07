package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.neolearnit.common.BBNInternalOntology;
import com.bbn.akbc.neolearnit.common.Domain;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.common.targets.TargetSlot;
import com.bbn.akbc.neolearnit.common.targets.constraints.impl.AtomicMentionConstraint;
import com.bbn.akbc.neolearnit.common.targets.constraints.impl.MinEntityLevelConstraint;
import com.bbn.akbc.neolearnit.common.targets.constraints.impl.SpanningTypeConstraint;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.bue.common.exceptions.NotImplementedException;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class CreateTargetBasedOnYAMLs {
    final static boolean SHOULD_OVERRIDE_EXISTING_EXTRACTOR = false;
    final static boolean SHOULD_SKIP_EXISTING_EXTRACTOR = true;

    public static void serializeTargetAndScoreTables(String targetPathDir,TargetAndScoreTables ext) throws IOException {
        String strPathJson = targetPathDir + File.separator + ext.getTarget().getName() + ".json";


        if(!SHOULD_OVERRIDE_EXISTING_EXTRACTOR && new File(strPathJson).exists()){
            return;
        }
        if(new File(strPathJson).exists() && SHOULD_SKIP_EXISTING_EXTRACTOR){
            return;
        }
        System.out.println("\t serializing extractor for " + ext.getTarget().getName() + "...");
        ext.serialize(new File(strPathJson));
        String strPathTargetJson = targetPathDir + ".target" + File.separator + ext.getTarget().getName() + ".json";
        ext.getTarget().serialize(strPathTargetJson);
    }

    public static void main(String[] args) throws Exception {
        String params = args[0];
        LearnItConfig.loadParams(new File(params));

        final Map<String, String> ontologyNameToPathMap = Domain.getOntologyNameToPathMap();
        Map<String, BBNInternalOntology.BBNInternalOntologyNode> ontologyMap = new HashMap<>();
        for (String ontologyName : ontologyNameToPathMap.keySet()) {
            final File ontologyFile = new File(ontologyNameToPathMap.get(ontologyName));
            ontologyMap.put(ontologyName, BBNInternalOntology.BBNInternalOntologyNode.fromInternalOntologyFile(ontologyFile));
        }
        for (String rootOntologyName : ontologyMap.keySet()) {
            BBNInternalOntology.BBNInternalOntologyNode root = ontologyMap.get(rootOntologyName);
            Multimap<String, BBNInternalOntology.BBNInternalOntologyNode> childrenNodeMap = root.getPropToNodeMultiMap(new BBNInternalOntology.BBNInternalOntologyNode.PropStringGetter("originalKey"));
            for (String nodeName : childrenNodeMap.keySet()) {
                Target newTarget;
                switch (rootOntologyName) {
                    case "unaryEvent":
                        newTarget = new Target.Builder(nodeName).
                                withTargetSlot(new TargetSlot.Builder(0, "all").build()).
                                withTargetSlot(new TargetSlot.Builder(1, "all").build())
                                .withAddedConstraint(new SpanningTypeConstraint(0, Arrays.asList(InstanceIdentifier.SpanningType.EventMention)))
                                .withAddedConstraint(new SpanningTypeConstraint(1, Arrays.asList(InstanceIdentifier.SpanningType.Empty))).build();
                        break;
                    case "binaryEvent":
                        newTarget = new Target.Builder(nodeName)
                                .withTargetSlot(new TargetSlot.Builder(0, "all")
                                        .build()).withTargetSlot(new TargetSlot.Builder(1, "all")
                                        .build())
                                .withAddedConstraint(new SpanningTypeConstraint(0, Arrays.asList(InstanceIdentifier.SpanningType.EventMention)))
                                .withAddedConstraint(new SpanningTypeConstraint(1, Arrays.asList(InstanceIdentifier.SpanningType.EventMention)))
                                .build();
                        break;
                    case "binaryEntity":
                        newTarget = new Target.Builder(nodeName)
                                .withTargetSlot(new TargetSlot.Builder(0, "all")
                                        .build()).withTargetSlot(new TargetSlot.Builder(1, "all")
                                        .build())
                                .withAddedConstraint(new SpanningTypeConstraint(0, Arrays.asList(InstanceIdentifier.SpanningType.Mention)))
                                .withAddedConstraint(new SpanningTypeConstraint(1, Arrays.asList(InstanceIdentifier.SpanningType.Mention)))
                                .build();
                        break;
                    case "binaryEventEntityOrValueMention":
                        newTarget = new Target.Builder(nodeName)
                                .withTargetSlot(new TargetSlot.Builder(0, "all")
                                        .build()).withTargetSlot(new TargetSlot.Builder(1, "all").build())
                                .withAddedConstraint(new SpanningTypeConstraint(0, Arrays.asList(InstanceIdentifier.SpanningType.EventMention)))
                                .withAddedConstraint(new SpanningTypeConstraint(1, Arrays.asList(InstanceIdentifier.SpanningType.Mention, InstanceIdentifier.SpanningType.ValueMention)))
                                .build();
                        break;
                    case "unaryEntity":
                        newTarget = new Target.Builder(nodeName)
                                .withTargetSlot(new TargetSlot.Builder(0, "all")
                                        .build()).withTargetSlot(new TargetSlot.Builder(1, "all")
                                        .build())
                                .withAddedConstraint(new SpanningTypeConstraint(0, Arrays.asList(InstanceIdentifier.SpanningType.Mention)))
                                .withAddedConstraint(new SpanningTypeConstraint(1, Arrays.asList(InstanceIdentifier.SpanningType.Empty)))
                                .build();
                        break;
                    default:
                        throw new NotImplementedException();
                }
                serializeTargetAndScoreTables(Domain.getExtractorsPath(),new TargetAndScoreTables(newTarget));
            }
        }
    }
}
