package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.bilingual.BilingualDocTheory;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.common.targets.TargetFactory;
import com.bbn.akbc.neolearnit.common.util.TabularPathListsConverter;
import com.bbn.akbc.neolearnit.loaders.LoaderUtils;
import com.bbn.akbc.neolearnit.loaders.impl.BilingualDocTheoryInstanceLoader;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.modules.BilingualExtractionModule;
import com.bbn.serif.io.SerifXMLWriter;
import com.bbn.serif.theories.*;
import com.google.common.base.Optional;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProjectAllEventsToSecondLanguage {

    public static void main(String[] args) throws Exception {
        String paramsFile = args[0];
        LearnItConfig.loadParams(new File(paramsFile));
        String doclist = args[1];
        String outputDir = args[2];
        Map<SentenceTheory, List<EventMention>> sentTheoryToEMs = new HashMap<>();
        BilingualExtractionModule module = new BilingualExtractionModule(false);
        BilingualDocTheoryInstanceLoader docTheoryLoader = module.getDocTheoryLoader(TargetFactory.makeUnaryEventTarget());

        LoaderUtils.loadTabularPathLists(doclist, docTheoryLoader);
        //get our mappings out of the module (MAGIC!)
        Mappings info = module.getInformationForScoring();

        String primaryLanguage = LearnItConfig.getList("languages").get(0);
        String secondaryLanguage = LearnItConfig.getList("languages").get(1);
        Map<String, BilingualDocTheory> docIdToBiDoctheory = new HashMap<>();
        for (InstanceIdentifier instanceIdentifier : info.getSeedInstances()) {
            MatchInfo matchInfo = instanceIdentifier.reconstructMatchInfo(TargetFactory.makeUnaryEventTarget());
            // Step1 get aligned synnode
            BilingualDocTheory bilingualDocTheory = InstanceIdentifier.getBiDocTheoryFromDocId(instanceIdentifier.getDocid()).get();
            docIdToBiDoctheory.put(instanceIdentifier.getDocid(), bilingualDocTheory);
            EventMention srcEventMention = (EventMention) matchInfo.getPrimaryLanguageMatch().firstSpanning();
            SynNode anchor = srcEventMention.anchorNode();
            Optional<Spanning> possibleTarget = bilingualDocTheory.tryAlignSpanning(anchor, null, null);
            if (possibleTarget.isPresent()) {
                SynNode targetSynNode = (SynNode) possibleTarget.get();
                EventMention.Builder newEm = EventMention.builder(srcEventMention.type());
                newEm.setAnchorNode(targetSynNode);
                List<EventMention.Argument> newArguments = new ArrayList<>();
                for (EventMention.Argument argument : srcEventMention.arguments()) {
                    if (argument instanceof EventMention.MentionArgument) {
                        Mention srcArgMention = ((EventMention.MentionArgument) argument).mention();
                        Optional<Spanning> possibleArgumentTarget = bilingualDocTheory.tryAlignSpanning(srcArgMention, null, null);
                        if (possibleArgumentTarget.isPresent()) {
                            Mention dstArgMention = (Mention) possibleArgumentTarget.get();
                            newArguments.add(EventMention.MentionArgument.from(argument.role(), dstArgMention, argument.score()));
                        } else {
                            System.out.println("[WARNING]: Dropping eventarg due to mention alignment");
                        }
                    } else if (argument instanceof EventMention.ValueMentionArgument) {
                        ValueMention srcArgValueMention = ((EventMention.ValueMentionArgument) argument).valueMention();
                        Optional<Spanning> possibleArgumentTarget = bilingualDocTheory.tryAlignSpanning(srcArgValueMention, null, null);
                        if (possibleArgumentTarget.isPresent()) {
                            ValueMention dstArgMention = (ValueMention) possibleArgumentTarget.get();
                            newArguments.add(EventMention.ValueMentionArgument.from(argument.role(), dstArgMention, argument.score()));
                        } else {
                            System.out.println("[WARNING]: Dropping eventarg due to valuemention alignment");
                        }
                    }
                }
                newEm.setArguments(newArguments);
                if (!sentTheoryToEMs.containsKey(bilingualDocTheory.getTargetDoc().sentenceTheory(targetSynNode.span().sentenceIndex()))) {
                    sentTheoryToEMs.put(bilingualDocTheory.getTargetDoc().sentenceTheory(targetSynNode.span().sentenceIndex()), new ArrayList<>());
                }
                List<EventMention> newEms = sentTheoryToEMs.get(bilingualDocTheory.getTargetDoc().sentenceTheory(targetSynNode.span().sentenceIndex()));
                newEms.add(newEm.build());
            } else {
                System.err.println("[WARNING]: Dropping event due to synnode alignment");
            }

        }
        SerifXMLWriter serifXMLWriter = SerifXMLWriter.create();
        Map<String, Map<String, String>> docIdToEntries = TabularPathListsConverter.parseSingleTabularList(doclist);
        for (String docId : docIdToEntries.keySet()) {
            if (!InstanceIdentifier.getBiDocTheoryFromDocId(docId).isPresent()) {
                BilingualDocTheory bilingualDocTheory = BilingualDocTheory.fromTabularPathLists(docId, docIdToEntries.get(docId));
                InstanceIdentifier.putBiDocTheory(bilingualDocTheory);
            }
            DocTheory targetDocTheory = InstanceIdentifier.getBiDocTheoryFromDocId(docId).get().getTargetDoc();
            DocTheory.Builder targetNewDocTheory = targetDocTheory.modifiedCopyBuilder();
            targetNewDocTheory.events(Events.createEmpty());
            for (SentenceTheory sentenceTheory : targetDocTheory) {
                SentenceTheory.Builder newSent = sentenceTheory.modifiedCopyBuilder();
//                newSent.eventMentions(EventMentions.create(sentenceTheory.parse(), sentTheoryToEMs.getOrDefault(sentenceTheory, new ArrayList<>())));
                newSent.eventMentions(new EventMentions.Builder()
                        .eventMentions(sentTheoryToEMs.getOrDefault(sentenceTheory, new ArrayList<>()))
                        .build());
                targetNewDocTheory.replacePrimarySentenceTheory(sentenceTheory, newSent.build());
            }
            serifXMLWriter.saveTo(targetNewDocTheory.build(), new File(outputDir + File.separator + targetDocTheory.docid() + ".xml"));
        }
    }
}
