package com.bbn.akbc.neolearnit.util;

import com.bbn.akbc.utility.FileUtil;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.io.SerifXMLLoader;
import com.bbn.serif.io.SerifXMLWriter;
import com.bbn.serif.theories.*;
import com.google.common.collect.Sets;

import java.io.File;
import java.util.*;

public class AttachEventArgBasedOnGuessing {

    static final Set<String> locationTypes = Sets.newHashSet("SourceLocation", "TargetLocation", "Location");
    static final Set<String> actorTypes = Sets.newHashSet("Actor");
    static final Set<String> timeTypes = Sets.newHashSet("Time");


    public static Mention findNearestMention(EventMention eventMention, SentenceTheory sentenceTheory, Set<String> eligibleEntityType) {
        Mention ret = null;
        int nearestDis = Integer.MAX_VALUE;
        Map<Integer, Mention> mentionStartToMention = new HashMap<>();
        Map<Integer, Mention> mentionEndToMention = new HashMap<>();
        for (Mention mention : sentenceTheory.mentions()) {
            mentionStartToMention.put(mention.atomicHead().span().startIndex(), mention);
            mentionEndToMention.put(mention.atomicHead().span().endIndex(), mention);
        }
        // Going Left
        for (int i = eventMention.span().startIndex() - 1; i >= 0; --i) {
            if (mentionEndToMention.containsKey(i) && eligibleEntityType.contains(mentionEndToMention.get(i).entityType().name().asString())) {
                if (Math.abs(i - eventMention.span().startIndex()) < nearestDis) {
                    nearestDis = Math.abs(i - eventMention.span().startIndex());
                    ret = mentionEndToMention.get(i);
                    break;
                }
            }
        }
        for (int i = eventMention.span().endIndex() + 1; i < sentenceTheory.span().endIndex() + 1; ++i) {
            if (mentionStartToMention.containsKey(i) && eligibleEntityType.contains(mentionStartToMention.get(i).entityType().name().asString())) {
                if (Math.abs(i - eventMention.span().endIndex()) < nearestDis) {
                    nearestDis = Math.abs(i - eventMention.span().endIndex());
                    ret = mentionStartToMention.get(i);
                    break;
                }
            }
        }
        return ret;

    }

    public static ValueMention findNearestValueMention(EventMention eventMention, SentenceTheory sentenceTheory) {
        ValueMention ret = null;
        int nearestDis = Integer.MAX_VALUE;
        Map<Integer, ValueMention> valueMentionStartToValueMention = new HashMap<>();
        Map<Integer, ValueMention> valueMentionEndToValueMention = new HashMap<>();
        for (ValueMention valueMention : sentenceTheory.valueMentions()) {
            valueMentionStartToValueMention.put(valueMention.span().startIndex(), valueMention);
            valueMentionEndToValueMention.put(valueMention.span().endIndex(), valueMention);
        }
        // Going Left
        for (int i = eventMention.span().startIndex() - 1; i >= 0; --i) {
            if (valueMentionEndToValueMention.containsKey(i) && valueMentionEndToValueMention.get(i).isTimexValue()) {
                if (Math.abs(i - eventMention.span().startIndex()) < nearestDis) {
                    nearestDis = Math.abs(i - eventMention.span().startIndex());
                    ret = valueMentionEndToValueMention.get(i);
                    break;
                }
            }
        }
        for (int i = eventMention.span().endIndex() + 1; i < sentenceTheory.span().endIndex() + 1; ++i) {
            if (valueMentionStartToValueMention.containsKey(i) && valueMentionStartToValueMention.get(i).isTimexValue()) {
                if (Math.abs(i - eventMention.span().endIndex()) < nearestDis) {
                    nearestDis = Math.abs(i - eventMention.span().endIndex());
                    ret = valueMentionStartToValueMention.get(i);
                    break;
                }
            }
        }
        return ret;

    }


    public static void main(String[] args) throws Exception {

        String listSerifXMLs = args[0]; // this can also be a directory of list files
        String outputDir = args[1];
        new File(outputDir).mkdirs();


        List<File> listOfListFiles = new ArrayList<>();
        if (new File(listSerifXMLs).isFile()) {
            listOfListFiles.add(new File(listSerifXMLs));
        } else {
            listOfListFiles = Arrays.asList(new File(listSerifXMLs).listFiles());
        }
        List<String> serifList = new ArrayList<>();
        for (File listFile : listOfListFiles) {
            serifList.addAll(FileUtil.readLinesIntoList(listFile));
        }
        int numOfEvent = 0;
        int timeCntBefore = 0;
        int locationCntBefore = 0;
        int actorCntBefore = 0;
        int timeCntAfter = 0;
        int locationCntAfter = 0;
        int actorCntAfter = 0;

        SerifXMLLoader loader = new SerifXMLLoader.Builder().allowSloppyOffsets().build();
        SerifXMLWriter serifXMLWriter = SerifXMLWriter.create();
        for (String filePath : serifList) {
            DocTheory docTheory = loader.loadFrom(new File(filePath));
            DocTheory.Builder newDocTheory = docTheory.modifiedCopyBuilder();
            for (int i = 0; i < docTheory.sentenceTheories().size(); ++i) {
                SentenceTheory sentenceTheory = docTheory.sentenceTheory(i);
                EventMentions.Builder newEventMentions = new EventMentions.Builder();
                for (EventMention eventMention : sentenceTheory.eventMentions()) {
                    if (eventMention.type().asString().equals("Migration")) {
                        numOfEvent++;
                        boolean timeExisted = false;
                        boolean locationExisted = false;
                        boolean actorExisted = false;
                        for (EventMention.Argument argument : eventMention.arguments()) {
                            if (locationTypes.contains(argument.role().asString())) {
                                locationExisted = true;
                                locationCntBefore++;
                            }
                            if (timeTypes.contains(argument.role().asString())) {
                                timeExisted = true;
                                timeCntBefore++;
                            }
                            if (actorTypes.contains(argument.role().asString())) {
                                actorExisted = true;
                                actorCntBefore++;
                            }
                        }
                        if (timeExisted && locationExisted && actorExisted) {
                            newEventMentions.addEventMentions(eventMention);
                            continue;
                        }
                        List<EventMention.Argument> newEventMentionArgList = new ArrayList<>();
                        newEventMentionArgList.addAll(eventMention.arguments());
                        if (!timeExisted) {
                            ValueMention valueMention = findNearestValueMention(eventMention, sentenceTheory);
                            if (valueMention != null) {
                                newEventMentionArgList.add(EventMention.ValueMentionArgument.from(Symbol.from("Time"), valueMention, 0.5f));
                                timeCntAfter++;
                            }
                        }
                        if (!actorExisted) {
                            Mention mention = findNearestMention(eventMention, sentenceTheory, Sets.newHashSet("PER", "REFUGEE", "ORG"));
                            if (mention != null) {
                                newEventMentionArgList.add(EventMention.MentionArgument.from(Symbol.from("Actor"), mention, 0.5f));
                                actorCntAfter++;
                            }
                        }
                        if (!locationExisted) {
                            Mention mention = findNearestMention(eventMention, sentenceTheory, Sets.newHashSet("GPE", "LOC", "FAC"));
                            if (mention != null) {
                                newEventMentionArgList.add(EventMention.MentionArgument.from(Symbol.from("Actor"), mention, 0.5f));
                                locationCntAfter++;
                            }
                        }
                        EventMention.Builder newEventMention = eventMention.modifiedCopyBuilder();
                        newEventMention.setArguments(newEventMentionArgList);
                        newEventMentions.addEventMentions(newEventMention.build());
                    }
                }
                SentenceTheory.Builder newSentenceTheory = sentenceTheory.modifiedCopyBuilder();
                newSentenceTheory.eventMentions(newEventMentions.build());
                newDocTheory.replacePrimarySentenceTheory(sentenceTheory, newSentenceTheory.build());
            }
            newDocTheory.events(Events.createEmpty());
            serifXMLWriter.saveTo(newDocTheory.build(), outputDir + File.separator + docTheory.docid() + ".xml");
        }
        System.out.println(numOfEvent);
        System.out.println(timeCntBefore);
        System.out.println(locationCntBefore);
        System.out.println(actorCntBefore);
        System.out.println(timeCntAfter);
        System.out.println(locationCntAfter);
        System.out.println(actorCntAfter);
    }
}
