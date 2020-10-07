package com.bbn.akbc.neolearnit.serializers;

import com.bbn.akbc.common.Pair;
import com.bbn.akbc.neolearnit.common.Annotation;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.loaders.LoaderUtils;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.util.GeneralUtils;
import com.bbn.akbc.utility.FileUtil;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.io.SerifXMLLoader;
import com.bbn.serif.io.SerifXMLWriter;
import com.bbn.serif.theories.*;
import com.bbn.serif.types.EntitySubtype;
import com.bbn.serif.types.EntityType;
import com.bbn.serif.types.Modality;
import com.bbn.serif.types.Tense;
import com.google.common.collect.Sets;
import org.json.simple.JSONArray;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

public class SerifXMLSerializer extends ExternalAnnotationBuilder {

    final static boolean shouldCopyNoLabeledSerifXML = true;
    final static float learnitEventEventRelationConfidence = 0.8f;
    final static float learnitEventConfidence = 0.8f;
    final static float learnitEventArgumentConfidence = 0.8f;
    final static float learnitEntityEntityRelationCOnfidence = 0.8f;
    final static boolean createEntitiesForMentions = true;
    final TaskType taskType;

    final Map<String, DocTheory> docIdToDocTheory;
    final String outputDir;
    Map<InstanceIdentifier, Map<LabelPattern, List<Symbol>>> instanceIdentifierToLabeledPatternToAux;
    Map<InstanceIdentifier,Map<LabelPattern,List<Symbol>>> instanceIdentifierToTriggerTextAux;

    public SerifXMLSerializer(String outputDir, Map<String, DocTheory> docIdToDocTheory, TaskType taskType) {
        this.docIdToDocTheory = docIdToDocTheory;
//        new File(outputDir).mkdirs();
        this.outputDir = outputDir;
        this.taskType = taskType;
        this.instanceIdentifierToLabeledPatternToAux = new HashMap<>();
        this.instanceIdentifierToTriggerTextAux = new HashMap<>();
    }

    public void setInstanceIdentifierToLabeledPatternToPattern(Map<InstanceIdentifier, Map<LabelPattern, List<Symbol>>> instanceIdentifierToLabeledPatternToAux) {
        this.instanceIdentifierToLabeledPatternToAux = instanceIdentifierToLabeledPatternToAux;
    }

    public void setInstanceIdentifierToTriggerText(Map<InstanceIdentifier, Map<LabelPattern, List<Symbol>>> instanceIdentifierToTriggerText){
        this.instanceIdentifierToTriggerTextAux = instanceIdentifierToTriggerText;
    }

    public static void main(String[] args) throws Exception {
        String params = args[0];
        LearnItConfig.loadParams(new File(params));
        String listSerifXMLs = args[1]; // this can also be a directory of list files
        String labeledMappingsPath = args[2];
        String taskTypeStr = args[3];
        String outputDir = args[4];


        List<File> listOfListFiles = new ArrayList<>();
        if (new File(listSerifXMLs).isFile()) {
            listOfListFiles.add(new File(listSerifXMLs));
        } else {
            listOfListFiles = Arrays.asList(new File(listSerifXMLs).listFiles());
        }
        Set<String> serifList = new HashSet<>();
        for (File listFile : listOfListFiles) {
            serifList.addAll(FileUtil.readLinesIntoList(listFile));
        }
        Map<String, DocTheory> docIdToDocTheory = new HashMap<>();
        Set<DocTheory> docTheories = LoaderUtils.resolvedDocTheoryFromPathList(serifList);
        for (DocTheory docTheory : docTheories) {
            docIdToDocTheory.put(docTheory.docid().asString(), docTheory);
        }
        SerifXMLSerializer serifXMLSerializer = new SerifXMLSerializer(outputDir, docIdToDocTheory, TaskType.valueOf(taskTypeStr));
        serifXMLSerializer.observe(Mappings.deserialize(new File(labeledMappingsPath), true));
        serifXMLSerializer.build();
    }

    public static <T extends Spanning> Map<Pair<Integer, Integer>, T> buildOffsetToSpanning(Iterable<T> collections) {
        Map<Pair<Integer, Integer>, T> ret = new HashMap<>();
        for (T t : collections) {
            ret.put(new Pair<>(t.span().startToken().index(), t.span().endToken().index()), t);
        }
        return ret;
    }


    public static DocTheory labelUnaryEventAndEventArg(DocTheory docTheory, Map<Integer, Map<InstanceIdentifier, Set<LabelPattern>>> sentenceToInstanceIdentifierToLabel, Map<String, Map<Integer, Map<InstanceIdentifier, Set<Symbol>>>> instanceTrackingMap) {

        DocTheory.Builder newDocTheory = docTheory.modifiedCopyBuilder();
        for (int sentid : sentenceToInstanceIdentifierToLabel.keySet()) {
            Map<Pair<Integer, Integer>, List<Pair<String,Set<Symbol>>>> eventSpanToLabel = new HashMap<>();
            Map<Pair<Integer, Integer>, Map<Pair<Integer, Integer>, Set<String>>> eventArgSpanToLabel = new HashMap<>();
            for (InstanceIdentifier instanceIdentifier : sentenceToInstanceIdentifierToLabel.get(sentid).keySet()) {
                if (instanceIdentifier.getSlot0SpanningType().equals(InstanceIdentifier.SpanningType.EventMention)) {
                    if (instanceIdentifier.getSlot1SpanningType().equals(InstanceIdentifier.SpanningType.Empty)) {
                        // Unary event case
                        List<Pair<String,Set<Symbol>>> buf = eventSpanToLabel.getOrDefault(new Pair<>(instanceIdentifier.getSlot0Start(), instanceIdentifier.getSlot0End()), new ArrayList<>());
                        for (LabelPattern labelPattern : sentenceToInstanceIdentifierToLabel.get(sentid).get(instanceIdentifier)) {
                            buf.add(new Pair<String, Set<Symbol>>(labelPattern.getLabel(),instanceTrackingMap.getOrDefault(docTheory.docid().asString(),new HashMap<>()).getOrDefault(sentid,new HashMap<>()).getOrDefault(instanceIdentifier,new HashSet<>())));
                        }
                        eventSpanToLabel.put(new Pair<>(instanceIdentifier.getSlot0Start(), instanceIdentifier.getSlot0End()), buf);

                    } else if (instanceIdentifier.getSlot1SpanningType().equals(InstanceIdentifier.SpanningType.Mention) || instanceIdentifier.getSlot1SpanningType().equals(InstanceIdentifier.SpanningType.ValueMention)) {
                        // Event arg case
                        for (LabelPattern labelPattern : sentenceToInstanceIdentifierToLabel.get(sentid).get(instanceIdentifier)) {
                            Map<Pair<Integer, Integer>, Set<String>> triggerArgbuf = eventArgSpanToLabel.getOrDefault(new Pair<>(instanceIdentifier.getSlot0Start(), instanceIdentifier.getSlot0End()), new HashMap<>());
                            Set<String> buf = triggerArgbuf.getOrDefault(new Pair<>(instanceIdentifier.getSlot1Start(), instanceIdentifier.getSlot1End()), new HashSet<>());
                            buf.add(labelPattern.getLabel());
                            triggerArgbuf.put(new Pair<>(instanceIdentifier.getSlot1Start(), instanceIdentifier.getSlot1End()), buf);
                            eventArgSpanToLabel.put(new Pair<>(instanceIdentifier.getSlot0Start(), instanceIdentifier.getSlot0End()), triggerArgbuf);
                        }

                    }
                }
            }
            SentenceTheory sentenceTheory = docTheory.sentenceTheory(sentid);
            Map<Pair<Integer, Integer>, Mention> mentionOffToMention = buildOffsetToSpanning(sentenceTheory.mentions());
            Map<Pair<Integer, Integer>, ValueMention> valueMentionOffToValueMention = buildOffsetToSpanning(sentenceTheory.valueMentions());

            List<EventMention.Builder> newEventMentions = new ArrayList<>();
            for (EventMention oldEventMention : sentenceTheory.eventMentions()) {
                newEventMentions.add(oldEventMention.modifiedCopyBuilder());
            }
            for (Pair<Integer, Integer> newEventSpan : eventSpanToLabel.keySet()) {

                for (Pair<String,Set<Symbol>> typeAuxsBuf : eventSpanToLabel.get(newEventSpan)) {
                    SynNode anchorNode = findSynNodeBySpan(sentenceTheory, newEventSpan.getFirst(), newEventSpan.getSecond());
                    String label = typeAuxsBuf.getFirst();
                    Set<Symbol> auxs = typeAuxsBuf.getSecond();
                    JSONArray jsonArray = new JSONArray();
                    jsonArray.addAll(auxs.stream().map(Symbol::asString).collect(Collectors.toSet()));

                    if (anchorNode != null) {
                        EventMention.Builder newEventMention = EventMention
                                .builder(Symbol.from(label))
                                .setAnchorNode(anchorNode)
                                .setAnchorPropFromNode(sentenceTheory)
                                .setScore(learnitEventConfidence)
                                .setPatternID(Symbol.from(jsonArray.toJSONString()))
                                .setModel(Symbol.from("LearnIt"))
                                ;
                        newEventMentions.add(newEventMention);
                    } else {
                        System.out.println("[ALERT] Cannot find proper anchor node.");
                    }
                }
            }

            for (EventMention.Builder newEventMention : newEventMentions) {
                Pair<Integer, Integer> eventSpan = new Pair<>(newEventMention.build().span().startToken().index(), newEventMention.build().span().endToken().index());
                List<EventMention.Argument> newArguments = new ArrayList<>();
                for (EventMention.Argument oldArgument : newEventMention.build().arguments()) {
                    newArguments.add(oldArgument);
                }
                Set<Pair<Integer, Integer>> eventArgSpans = eventArgSpanToLabel.getOrDefault(eventSpan, new HashMap<>()).keySet();
                for (Pair<Integer, Integer> eventArgSpan : eventArgSpans) {
                    for (String label : eventArgSpanToLabel.getOrDefault(eventSpan, new HashMap<>()).getOrDefault(eventArgSpan, new HashSet<>())) {
                        if (valueMentionOffToValueMention.containsKey(eventArgSpan)) {
                            ValueMention valueMention = valueMentionOffToValueMention.get(eventArgSpan);
                            EventMention.ValueMentionArgument argument = EventMention.ValueMentionArgument.from(Symbol.from(label), valueMention, learnitEventArgumentConfidence);
                            newArguments.add(argument);
                        } else {
                            Mention mention = mentionOffToMention.get(eventArgSpan);
                            EventMention.MentionArgument argument = EventMention.MentionArgument.from(Symbol.from(label), mention, learnitEventArgumentConfidence);
                            newArguments.add(argument);
                        }
                    }
                }
                newEventMention.setArguments(newArguments);
            }
            EventMentions.Builder newEventMentionsBuilder = new EventMentions.Builder();
            for (EventMention.Builder newEventMention : newEventMentions) {
                newEventMentionsBuilder.addEventMentions(newEventMention.build());
            }
            SentenceTheory.Builder newSentenceTheory = sentenceTheory.modifiedCopyBuilder();
            newSentenceTheory.eventMentions(newEventMentionsBuilder.build());
            newDocTheory.replacePrimarySentenceTheory(sentenceTheory, newSentenceTheory.build());
        }
        newDocTheory.events(Events.createEmpty());
        return newDocTheory.build();
    }

    public static DocTheory labelUnaryEntity(DocTheory docTheory, Map<Integer, Map<InstanceIdentifier, Set<LabelPattern>>> sentenceToInstanceIdentifierToLabel, Map<String, Map<Integer, Map<InstanceIdentifier, Set<Symbol>>>> instanceTrackingMap) {
        if (sentenceToInstanceIdentifierToLabel.keySet().size() < 1) {
            return docTheory;
        } else {
            Set<Mention> touchedMentions = new HashSet<>();
            for (int sentid : sentenceToInstanceIdentifierToLabel.keySet()) {
                for (InstanceIdentifier instanceIdentifier : sentenceToInstanceIdentifierToLabel.get(sentid).keySet()) {
                    if (instanceIdentifier.getSlot0SpanningType().equals(InstanceIdentifier.SpanningType.Mention) && instanceIdentifier.getSlot1SpanningType().equals(InstanceIdentifier.SpanningType.Empty)) {
                        for (Spanning spanning : InstanceIdentifier.getSpannings(docTheory.sentenceTheory(sentid), instanceIdentifier.getSlot0Start(), instanceIdentifier.getSlot0End(), InstanceIdentifier.SpanningType.Mention)) {
                            Mention mention = (Mention) spanning;
                            for (LabelPattern labelPattern : sentenceToInstanceIdentifierToLabel.get(sentid).get(instanceIdentifier)) {
                                mention.setEntityType(EntityType.of(labelPattern.getLabel()));
                                mention.setModel(com.google.common.base.Optional.of("LearnIt"));
                                Set<Symbol> trackingPatterns = instanceTrackingMap.getOrDefault(instanceIdentifier.getDocid(), new HashMap<>()).getOrDefault(instanceIdentifier.getSentid(), new HashMap<>()).getOrDefault(instanceIdentifier, new HashSet<>());
                                JSONArray jsonArray = new JSONArray();
                                jsonArray.addAll(trackingPatterns.stream().map(Symbol::asString).collect(Collectors.toSet()));
                                mention.setPattern(com.google.common.base.Optional.of(jsonArray.toJSONString()));
                                touchedMentions.add(mention);
                            }

                        }
                    }
                }
            }
            if(touchedMentions.size()>0 && createEntitiesForMentions){
                Set<Mention> entityCoveredMentions = new HashSet<>();
                for(Entity entity:docTheory.entities()){
                    for(Mention mention:entity.mentionSet()){
                        entityCoveredMentions.add(mention);
                    }
                }
                Set<Mention> unCoveredMentions = Sets.difference(touchedMentions,entityCoveredMentions);
                if(unCoveredMentions.size()>0){
                    List<Entity> entities = new ArrayList<>(docTheory.entities().asList());
                    for(Mention mention : unCoveredMentions){
                        Entity.Builder entity = new Entity.Builder();
                        entity.addMentionSet(mention);
                        Map<Mention,MentionConfidence> mentionConf = new HashMap<>();
                        mentionConf.put(mention,MentionConfidence.DEFAULT);
                        entity.confidences(mentionConf);
                        entity.generic(true);
                        entity.type(mention.entityType());
                        entity.subtype(mention.entitySubtype());
                        entities.add(entity.build());
                    }
                    docTheory = docTheory.modifiedCopyBuilder().entities(Entities.create(entities,"0.8")).build();
                }
            }
            return docTheory;
        }
    }

    public static DocTheory labelBinaryEntityEntity(DocTheory docTheory, Map<Integer, Map<InstanceIdentifier, Set<LabelPattern>>> sentenceToInstanceIdentifierToLabel, Map<String, Map<Integer, Map<InstanceIdentifier, Set<Symbol>>>> instanceTrackingMap) {
        if (sentenceToInstanceIdentifierToLabel.keySet().size() < 1) {
            return docTheory;
        } else {
            DocTheory.Builder modifiedDoctheory = docTheory.modifiedCopyBuilder();
            for (int sentid : sentenceToInstanceIdentifierToLabel.keySet()) {
                SentenceTheory sentenceTheory = docTheory.sentenceTheory(sentid);
                RelationMentions.Builder relationMentions = new RelationMentions.Builder();
                if(!sentenceTheory.relationMentions().isAbsent()){
                    relationMentions.addAllRelationMentions(sentenceTheory.relationMentions());
                }
                Map<Pair<Integer, Integer>, Mention> spanToMention = buildOffsetToSpanning(sentenceTheory.mentions());
                for (InstanceIdentifier instanceIdentifier : sentenceToInstanceIdentifierToLabel.get(sentid).keySet()) {
                    if (instanceIdentifier.getSlot0SpanningType().equals(InstanceIdentifier.SpanningType.Mention) && instanceIdentifier.getSlot1SpanningType().equals(InstanceIdentifier.SpanningType.Mention)) {
                        Mention leftMention = spanToMention.get(new Pair<Integer, Integer>(instanceIdentifier.getSlot0Start(),instanceIdentifier.getSlot0End()));
                        Mention rightMention = spanToMention.get(new Pair<Integer, Integer>(instanceIdentifier.getSlot1Start(),instanceIdentifier.getSlot1End()));
                        Set<Symbol> trackingPatterns = instanceTrackingMap.getOrDefault(instanceIdentifier.getDocid(), new HashMap<>()).getOrDefault(instanceIdentifier.getSentid(), new HashMap<>()).getOrDefault(instanceIdentifier, new HashSet<>());
                        JSONArray jsonArray = new JSONArray();
                        jsonArray.addAll(trackingPatterns.stream().map(Symbol::asString).collect(Collectors.toSet()));
                        String jsonArrayStr = jsonArray.toJSONString();
                        for (LabelPattern labelPattern : sentenceToInstanceIdentifierToLabel.get(sentid).get(instanceIdentifier)) {

                            RelationMention.Builder relationMention = new RelationMention.Builder();
                            relationMention.leftMention(leftMention);
                            relationMention.rightMention(rightMention);
                            relationMention.type(Symbol.from(labelPattern.getLabel()));
                            relationMention.tense(Tense.UNSPECIFIED);
                            relationMention.modality(Modality.OTHER);
                            relationMention.score(learnitEntityEntityRelationCOnfidence);
                            relationMention.model(com.google.common.base.Optional.of("LearnIt"));
                            relationMention.pattern(com.google.common.base.Optional.of(jsonArrayStr));
                            relationMentions.addRelationMentions(relationMention.build());
                        }
                    }
                }
                SentenceTheory.Builder modifiedSentenceTheory = sentenceTheory.modifiedCopyBuilder();
                modifiedSentenceTheory.relationMentions(relationMentions.build());
                modifiedDoctheory.replacePrimarySentenceTheory(sentenceTheory,modifiedSentenceTheory.build());
            }
            return modifiedDoctheory.build();
        }
    }

//    public static DocTheory labelUnaryEntity(DocTheory docTheory, Map<Integer, Map<InstanceIdentifier, Set<LabelPattern>>> sentenceToInstanceIdentifierToLabel) {
//        DocTheory.Builder newDocTheory = docTheory.modifiedCopyBuilder();
//
//        Map<Mention, Mention> globalOldMentionToNewMention = new HashMap<>();
//        for (int sentid : sentenceToInstanceIdentifierToLabel.keySet()) {
//            Map<Pair<Integer, Integer>, Set<String>> mentionSpanToLabel = new HashMap<>();
//            for (InstanceIdentifier instanceIdentifier : sentenceToInstanceIdentifierToLabel.get(sentid).keySet()) {
//                if (instanceIdentifier.getSlot0SpanningType().equals(InstanceIdentifier.SpanningType.Mention) && instanceIdentifier.getSlot1SpanningType().equals(InstanceIdentifier.SpanningType.Empty)) {
//                    Set<String> buf = mentionSpanToLabel.getOrDefault(new Pair<>(instanceIdentifier.getSlot0Start(), instanceIdentifier.getSlot0End()), new HashSet<>());
//                    for (LabelPattern labelPattern : sentenceToInstanceIdentifierToLabel.get(sentid).get(instanceIdentifier)) {
//                        buf.add(labelPattern.getLabel());
//                    }
//                    mentionSpanToLabel.put(new Pair<>(instanceIdentifier.getSlot0Start(), instanceIdentifier.getSlot0End()), buf);
//                }
//            }
//
//            // Step1: Fix Mention
//            Map<Mention, Mention> oldMentionToNewMention = new HashMap<>();
//            SentenceTheory sentenceTheory = docTheory.sentenceTheory(sentid);
//            SentenceTheory.Builder newSentenceTheory = sentenceTheory.modifiedCopyBuilder();
//            for (Mention mention : sentenceTheory.mentions()) {
//                Pair<Integer, Integer> mentionSpan = new Pair<>(mention.span().startToken().index(), mention.span().endToken().index());
//                if (mentionSpanToLabel.containsKey(mentionSpan)) {
//                    // TODO: Fix me
//                    Mention newMention = new Mention(mention.node(), mention.mentionType(), EntityType.of(mentionSpanToLabel.get(mentionSpan).iterator().next()), EntitySubtype.undetermined(), mention.metonymyInfo().orNull(), mention.confidence(), mention.linkConfidence(), mention.externalID().orNull());
//                    oldMentionToNewMention.put(mention, newMention);
//                    globalOldMentionToNewMention.put(mention, newMention);
//                } else {
//                    oldMentionToNewMention.put(mention, mention);
//                    globalOldMentionToNewMention.put(mention, mention);
//                }
//            }
//            Mentions.Builder newMentions = new Mentions.Builder();
//            for (Mention newMention : oldMentionToNewMention.values()) {
//                newMentions.addMentions(newMention);
//            }
//            newSentenceTheory.mentions(newMentions.build());
//
//            // Step2: Fix Proposition
//            Map<Proposition, Proposition> oldPropositionToNewProposition = new HashMap<>();
//            Map<Proposition, Set<Proposition.ArgumentBuilder>> newPropositionToArgs = new HashMap<>();
//            for (Proposition oldProposition : sentenceTheory.propositions()) {
//                boolean mentionChanged = false;
//                for (Proposition.Argument argument : oldProposition.args()) {
//                    if (argument instanceof Proposition.MentionArgument) {
//                        Proposition.MentionArgument mentionArgument = (Proposition.MentionArgument) argument;
//                        mentionChanged = true;
//                    }
//                }
//                if (!mentionChanged) {
//                    oldPropositionToNewProposition.put(oldProposition, oldProposition);
//                } else {
//                    Proposition newProposition = new Proposition(oldProposition.predType(), oldProposition.predHead().orNull(), oldProposition.particle().orNull(), oldProposition.adverb().orNull(), oldProposition.negation().orNull(), oldProposition.modal().orNull(), new ArrayList<>(), oldProposition.statuses());
//                    oldPropositionToNewProposition.put(oldProposition, newProposition);
//                    Set<Proposition.ArgumentBuilder> newArgs = newPropositionToArgs.getOrDefault(newProposition, new HashSet<>());
//                    for (Proposition.Argument argument : oldProposition.args()) {
//                        if (argument instanceof Proposition.MentionArgument) {
//                            Proposition.MentionArgument mentionArgument = (Proposition.MentionArgument) argument;
//                            newArgs.add(new Proposition.MentionArgumentBuilder(argument.role().orNull(), oldMentionToNewMention.get(mentionArgument.mention())));
//                        } else if (argument instanceof Proposition.TextArgument) {
//                            Proposition.TextArgument textArgument = (Proposition.TextArgument) argument;
//                            newArgs.add(new Proposition.TextArgumentBuilder(textArgument.role().orNull(), textArgument.node()));
//                        } else {
//                            Proposition.PropositionArgument propositionArgument = (Proposition.PropositionArgument) argument;
//                        }
//                    }
//                    newPropositionToArgs.put(newProposition, newArgs);
//                }
//            }
//
//            for (Proposition oldProposition : oldPropositionToNewProposition.keySet()) {
//                Proposition newProposition = oldPropositionToNewProposition.get(oldProposition);
//                Set<Proposition.ArgumentBuilder> newArgs = newPropositionToArgs.getOrDefault(newProposition, new HashSet<>());
//                for (Proposition.Argument argument : oldProposition.args()) {
//                    if (argument instanceof Proposition.PropositionArgument) {
//                        Proposition.PropositionArgument propositionArgument = (Proposition.PropositionArgument) argument;
//                        newArgs.add(new Proposition.PropositionArgumentBuilder(propositionArgument.role().orNull(), oldPropositionToNewProposition.get(propositionArgument.proposition())));
//                    }
//                }
//                newPropositionToArgs.put(newProposition, newArgs);
//            }
//
//            Propositions.Builder newPropositions = new Propositions.Builder();
//            for (Proposition newProposition : oldPropositionToNewProposition.values()) {
//                for (Proposition.ArgumentBuilder argumentBuilder : newPropositionToArgs.getOrDefault(newProposition, new HashSet<>())) {
//                    newProposition.addArg(argumentBuilder.build(newProposition));
//                }
//                newPropositions.addPropositions(newProposition);
//            }
//            newPropositions.mentions(newMentions.build());
//            newSentenceTheory.propositions(newPropositions.build());
//            newDocTheory.replacePrimarySentenceTheory(sentenceTheory, newSentenceTheory.build());
//        }
//
//        Set<Entity> newEntities = new HashSet<>();
//        for (Entity entity : docTheory.entities()) {
//            boolean shouldReGenerateEntity = false;
//            for (Mention mention : entity.mentionSet()) {
//                if (globalOldMentionToNewMention.containsKey(mention)) {
//                    shouldReGenerateEntity = true;
//                    break;
//                }
//            }
//            if (!shouldReGenerateEntity) {
//                newEntities.add(entity);
//            } else {
//                Entity.Builder newEntityBuilder = new Entity.Builder();
//                newEntityBuilder.from(entity);
//                List<Mention> newMentions = new ArrayList<>();
//                for (Mention mention : entity.mentionSet()) {
//                    newMentions.add(globalOldMentionToNewMention.getOrDefault(mention, mention));
//                }
//                newEntityBuilder.mentionSet(newMentions);
//                Map<Mention, MentionConfidence> mentionConfidenceMap = new HashMap<>();
//                for (Mention mention : entity.confidences().keySet()) {
//                    MentionConfidence mentionConfidence = entity.confidences().get(mention);
//                    Mention newMention = globalOldMentionToNewMention.getOrDefault(mention, mention);
//                    mentionConfidenceMap.put(newMention, mentionConfidence);
//                }
//                newEntityBuilder.confidences(mentionConfidenceMap);
//                newEntities.add(newEntityBuilder.build());
//            }
//        }
//        newDocTheory.entities(Entities.create(newEntities, docTheory.entities().score()));
//        return newDocTheory.build();
//    }

    public static DocTheory labelBinaryEventEvent(DocTheory docTheory, Map<Integer, Map<InstanceIdentifier, Set<LabelPattern>>> sentenceToInstanceIdentifierToLabel, Map<String, Map<Integer, Map<InstanceIdentifier, Set<Symbol>>>> instanceTrackingMap,Map<String, Map<Integer, Map<InstanceIdentifier, List<Symbol>>>> instanceTokenMap) {

        DocTheory.Builder newDocTheory = docTheory.modifiedCopyBuilder();
        List<EventEventRelationMention> newEventEventRelationMentions = new ArrayList<>();
        for (int sentid : sentenceToInstanceIdentifierToLabel.keySet()) {
            Map<Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>, Set<String>> eventEventToLabel = new HashMap<>();
            Map<Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>, String> eventEventToTrackingLabel = new HashMap<>();
            Map<Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>, String> eerTriggerMap = new HashMap<>();
            for (InstanceIdentifier instanceIdentifier : sentenceToInstanceIdentifierToLabel.get(sentid).keySet()) {
                if (instanceIdentifier.getSlot0SpanningType().equals(InstanceIdentifier.SpanningType.EventMention) && instanceIdentifier.getSlot1SpanningType().equals(InstanceIdentifier.SpanningType.EventMention)) {
                    Set<String> buf = eventEventToLabel.getOrDefault(new Pair<>(new Pair<>(instanceIdentifier.getSlot0Start(), instanceIdentifier.getSlot0End()), new Pair<>(instanceIdentifier.getSlot1Start(), instanceIdentifier.getSlot1End())), new HashSet<>());
                    for (LabelPattern labelPattern : sentenceToInstanceIdentifierToLabel.get(sentid).get(instanceIdentifier)) {
                        buf.add(labelPattern.getLabel());
                        Set<Symbol> trackingPatterns = instanceTrackingMap.getOrDefault(instanceIdentifier.getDocid(), new HashMap<>()).getOrDefault(instanceIdentifier.getSentid(), new HashMap<>()).getOrDefault(instanceIdentifier, new HashSet<>());
                        JSONArray jsonArray = new JSONArray();
                        jsonArray.addAll(trackingPatterns.stream().map(Symbol::asString).collect(Collectors.toSet()));
                        eventEventToTrackingLabel.put(new Pair<>(new Pair<>(instanceIdentifier.getSlot0Start(), instanceIdentifier.getSlot0End()), new Pair<>(instanceIdentifier.getSlot1Start(), instanceIdentifier.getSlot1End())), jsonArray.toJSONString());
                        Set<String> triggerText = instanceTokenMap.getOrDefault(instanceIdentifier.getDocid(), new HashMap<>()).getOrDefault(instanceIdentifier.getSentid(), new HashMap<>()).getOrDefault(instanceIdentifier, new ArrayList<>()).stream().map(Symbol::asString).collect(Collectors.toSet());
                        List<String> markedUpStr = new ArrayList<>();
                        Set<String> outputedToken = new HashSet<>();
                        for(Token token : docTheory.sentenceTheory(sentid).tokenSequence()){
                            String tokenStr = token.tokenizedText().utf16CodeUnits();
                            if(triggerText.contains(tokenStr) && !outputedToken.contains(tokenStr)){
                                markedUpStr.add(token.tokenizedText().utf16CodeUnits());
                                outputedToken.add(tokenStr);
                            }
                        }
                        eerTriggerMap.put(new Pair<>(new Pair<>(instanceIdentifier.getSlot0Start(), instanceIdentifier.getSlot0End()), new Pair<>(instanceIdentifier.getSlot1Start(), instanceIdentifier.getSlot1End())), markedUpStr.stream().collect(Collectors.joining(" ")));

                    }
                    eventEventToLabel.put(new Pair<>(new Pair<>(instanceIdentifier.getSlot0Start(), instanceIdentifier.getSlot0End()), new Pair<>(instanceIdentifier.getSlot1Start(), instanceIdentifier.getSlot1End())), buf);
                }
            }

            SentenceTheory sentenceTheory = docTheory.sentenceTheory(sentid);
            Map<Pair<Integer, Integer>, EventMention> spanToEventMention = buildOffsetToSpanning(sentenceTheory.eventMentions());
            for (Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> eerSpan : eventEventToLabel.keySet()) {
                for (String label : eventEventToLabel.get(eerSpan)) {
                    EventEventRelationMention.Builder newEventEventRelation = new EventEventRelationMention.Builder();
                    newEventEventRelation.leftEventMention(new EventEventRelationMention.EventMentionArgument(spanToEventMention.get(eerSpan.getFirst()), Symbol.from("arg1")));
                    newEventEventRelation.rightEventMention(new EventEventRelationMention.EventMentionArgument(spanToEventMention.get(eerSpan.getSecond()), Symbol.from("arg2")));
                    newEventEventRelation.confidence(learnitEventEventRelationConfidence);
                    newEventEventRelation.model("LearnIt");
                    newEventEventRelation.relationType(Symbol.from(label));
                    newEventEventRelation.pattern(eventEventToTrackingLabel.getOrDefault(eerSpan, "[]"));
                    newEventEventRelation.triggerText(eerTriggerMap.getOrDefault(eerSpan,""));
                    newEventEventRelationMentions.add(newEventEventRelation.build());
                }
            }
        }
        newDocTheory.eventEventRelationMentions(EventEventRelationMentions.create(newEventEventRelationMentions));
        return newDocTheory.build();
    }

    public static SynNode findSynNodeBySpan(SentenceTheory sentenceTheory, int startTokenIdx, int endTokenIdx) {
        GetSynNodeByStartEndTokenIdx getSynNodeByStartEndTokenIdx = new GetSynNodeByStartEndTokenIdx(startTokenIdx, endTokenIdx);
        sentenceTheory.parse().preorderTraversal(getSynNodeByStartEndTokenIdx);
        return getSynNodeByStartEndTokenIdx.getMatch();
    }

    public enum TaskType {
        unary_entity,
        binary_entity_entity,
        unary_event_and_binary_event_argument,
        unary_event,
        binary_event_event,
        binary_event_entity_or_value
    }

    public List<DocTheory> buildNewDocTheory() {
        Map<String, Map<Integer, Map<InstanceIdentifier, Set<LabelPattern>>>> docIdToSentIdInstanceId = new HashMap<>();
        Map<String, Map<Integer, Map<InstanceIdentifier, Set<Symbol>>>> docIdToSentIdInstanceIdSymbol = new HashMap<>();
        Map<String, Map<Integer, Map<InstanceIdentifier, List<Symbol>>>> docIdToSentInstanceIdTriggerText = new HashMap<>();
        List<DocTheory> ret = new ArrayList<>();
        for (InstanceIdentifier instanceIdentifier : this.inMemoryAnnotationStorage.getAllInstanceIdentifier()) {
            for (LabelPattern labelPattern : this.inMemoryAnnotationStorage.lookupInstanceIdentifierAnnotation(instanceIdentifier)) {

                Map<Integer, Map<InstanceIdentifier, Set<LabelPattern>>> docBuf = docIdToSentIdInstanceId.getOrDefault(instanceIdentifier.getDocid(), new HashMap<>());
                Map<InstanceIdentifier, Set<LabelPattern>> sentBuf = docBuf.getOrDefault(instanceIdentifier.getSentid(), new HashMap<>());
                Set<LabelPattern> instanceBuf = sentBuf.getOrDefault(instanceIdentifier, new HashSet<>());
                instanceBuf.add(labelPattern);
                sentBuf.put(instanceIdentifier, instanceBuf);
                docBuf.put(instanceIdentifier.getSentid(), sentBuf);
                docIdToSentIdInstanceId.put(instanceIdentifier.getDocid(), docBuf);

                Map<Integer, Map<InstanceIdentifier, Set<Symbol>>> docBufForTracking = docIdToSentIdInstanceIdSymbol.getOrDefault(instanceIdentifier.getDocid(), new HashMap<>());
                Map<Integer, Map<InstanceIdentifier, List<Symbol>>> docBufForTriggerText = docIdToSentInstanceIdTriggerText.getOrDefault(instanceIdentifier.getDocid(),new HashMap<>());
                Map<InstanceIdentifier, Set<Symbol>> sentBufForTracking = docBufForTracking.getOrDefault(instanceIdentifier.getSentid(), new HashMap<>());
                Map<InstanceIdentifier, List<Symbol>> sentBufForTriggerText = docBufForTriggerText.getOrDefault(instanceIdentifier.getSentid(),new HashMap<>());
                Set<Symbol> trackingBuf = sentBufForTracking.getOrDefault(instanceIdentifier, new HashSet<>());
                for (Symbol auxStr : this.instanceIdentifierToLabeledPatternToAux.getOrDefault(instanceIdentifier, new HashMap<>()).getOrDefault(labelPattern, new ArrayList<>())) {
                    trackingBuf.add(auxStr);
                }
                List<Symbol> triggerTokens = sentBufForTriggerText.getOrDefault(instanceIdentifier,new ArrayList<>());
                for(Symbol auxStr: this.instanceIdentifierToTriggerTextAux.getOrDefault(instanceIdentifier,new HashMap<>()).getOrDefault(labelPattern,new ArrayList<>())){
                    triggerTokens.add(auxStr);
                }
                sentBufForTracking.put(instanceIdentifier, trackingBuf);
                sentBufForTriggerText.put(instanceIdentifier,triggerTokens);
                docBufForTracking.put(instanceIdentifier.getSentid(), sentBufForTracking);
                docBufForTriggerText.put(instanceIdentifier.getSentid(),sentBufForTriggerText);
                docIdToSentIdInstanceIdSymbol.put(instanceIdentifier.getDocid(), docBufForTracking);
                docIdToSentInstanceIdTriggerText.put(instanceIdentifier.getDocid(),docBufForTriggerText);
            }
        }
        for (String docId : this.docIdToDocTheory.keySet()) {
            if (!docIdToSentIdInstanceId.containsKey(docId)) {
                if (shouldCopyNoLabeledSerifXML) {

                    DocTheory docTheory = this.docIdToDocTheory.get(docId);
                    ret.add(docTheory);
                }
            } else {
                if (taskType.equals(TaskType.unary_entity)) {
                    DocTheory resolvedDocTheory = labelUnaryEntity(this.docIdToDocTheory.get(docId), docIdToSentIdInstanceId.get(docId), docIdToSentIdInstanceIdSymbol);
                    ret.add(resolvedDocTheory);
                } else if(taskType.equals(TaskType.binary_entity_entity)){
                    DocTheory resolvedDocTheory = labelBinaryEntityEntity(this.docIdToDocTheory.get(docId), docIdToSentIdInstanceId.get(docId), docIdToSentIdInstanceIdSymbol);
                    ret.add(resolvedDocTheory);
                }
                else if (taskType.equals(TaskType.unary_event) || taskType.equals(TaskType.unary_event_and_binary_event_argument) || taskType.equals(TaskType.binary_event_entity_or_value)) {
                    DocTheory resolvedDocTheory = labelUnaryEventAndEventArg(this.docIdToDocTheory.get(docId), docIdToSentIdInstanceId.get(docId), docIdToSentIdInstanceIdSymbol);
                    ret.add(resolvedDocTheory);
                } else if (taskType.equals(TaskType.binary_event_event)) {
                    DocTheory resolvedDocTheory = labelBinaryEventEvent(this.docIdToDocTheory.get(docId), docIdToSentIdInstanceId.get(docId), docIdToSentIdInstanceIdSymbol,docIdToSentInstanceIdTriggerText);
                    ret.add(resolvedDocTheory);
                }
            }
        }
        return ret;
    }

    @Override
    public void build() throws Exception {
        List<DocTheory> docTheoryList = buildNewDocTheory();
        SerifXMLWriter serifXMLWriter = SerifXMLWriter.create();
        for (DocTheory docTheory : docTheoryList) {
            serifXMLWriter.saveTo(docTheory, this.outputDir + File.separator + docTheory.docid().asString() + ".xml");
        }
    }

    public static class GetSynNodeByStartEndTokenIdx implements SynNode.PreorderVisitor {
        final int tokenStartIdx;
        final int tokenEndIdx;
        SynNode match;

        public GetSynNodeByStartEndTokenIdx(int tokenStartIdx, int tokenEndIdx) {
            this.tokenStartIdx = tokenStartIdx;
            this.tokenEndIdx = tokenEndIdx;
            this.match = null;
        }

        @Override
        public boolean visitChildren(SynNode synNode) {
            if (synNode.span().startToken().index() >= this.tokenStartIdx && synNode.span().endToken().index() <= this.tokenEndIdx) {
                if (synNode.span().startToken().index() == this.tokenStartIdx && synNode.span().endToken().index() == this.tokenEndIdx) {
                    this.match = synNode;
                    return false;
                } else {
                    return true;
                }
            }
            return true;
        }

        public SynNode getMatch() {
            return this.match;
        }
    }
}
