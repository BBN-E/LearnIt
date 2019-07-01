package com.bbn.akbc.neolearnit.serializers.binary_event;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfoDisplay;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;
import com.bbn.akbc.neolearnit.serializers.ExternalAnnotationBuilder;
import com.bbn.akbc.neolearnit.serializers.observations.HTMLTemplateString;
import com.bbn.akbc.neolearnit.storage.StorageUtils;
import com.bbn.serif.theories.EventMention;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.Token;
import com.bbn.serif.theories.TokenSequence;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AMTBinaryEventRelationObserver extends ExternalAnnotationBuilder {
    final private String filePrefix;

    public AMTBinaryEventRelationObserver(String filePrefix) {
        super();
        this.filePrefix = filePrefix;

    }

    @Override
    public void build() throws Exception {
        Writer amtcsvWriter = new BufferedWriter(new FileWriter(new File(filePrefix + "_amt.csv")));
        CSVPrinter AMTCsvPrinter = new CSVPrinter(amtcsvWriter, CSVFormat.DEFAULT);

        Writer instanceIdToHeadIdxOffCsvWriter = new BufferedWriter(new FileWriter(new File(filePrefix + "_instanceId.csv")));
        CSVPrinter instanceIdToHEadIdxOffCsvPrinter = new CSVPrinter(instanceIdToHeadIdxOffCsvWriter, CSVFormat.DEFAULT);

        List<String> htmlStrList = new ArrayList<>();

        AMTCsvPrinter.printRecord(
                "instanceIdentifier",
                "arg1HeadToken",
                "arg2HeadToken",
                "relationType",
                "htmlStr");

        instanceIdToHEadIdxOffCsvPrinter.printRecord(
                "instanceIdentifier",
                "docId",
                "sentId",
                "leftHeadTokenIdx",
                "rightHeadTokenIdx"
        );

        Map<InstanceIdentifier, SentenceTheory> instanceIdentifierSentenceTheoryMap = this.resolveSentenceTheory();
        ObjectMapper objectMapper = StorageUtils.getDefaultMapper();
        for (InstanceIdentifier instanceIdentifier : this.inMemoryAnnotationStorage.getAllInstanceIdentifier()) {
            final SentenceTheory sentenceTheory = instanceIdentifierSentenceTheoryMap.get(instanceIdentifier);
            final TokenSequence tokenSequence = sentenceTheory.tokenSequence();
            final EventMention leftEventMention = (EventMention) InstanceIdentifier.getSpanning(sentenceTheory, instanceIdentifier.getSlot0Start(), instanceIdentifier.getSlot0End(), instanceIdentifier.getSlotEntityType(0)).get();
            final EventMention rightEventMention = (EventMention) InstanceIdentifier.getSpanning(sentenceTheory, instanceIdentifier.getSlot1Start(), instanceIdentifier.getSlot1End(), instanceIdentifier.getSlotEntityType(1)).get();

            final int leftHeadWordTokenIdx = leftEventMention.anchorNode().head().tokenSpan().endTokenIndexInclusive();
            final int rightHeadWordTokenIdx = rightEventMention.anchorNode().head().tokenSpan().endTokenIndexInclusive();

            for (LabelPattern labelPattern : this.inMemoryAnnotationStorage.lookupInstanceIdentifierAnnotation(instanceIdentifier)) {

                Token leftHead = tokenSequence.token(leftHeadWordTokenIdx);
                Token rightHead = tokenSequence.token(rightHeadWordTokenIdx);
                StringBuilder sb = new StringBuilder();
                for (Token token : tokenSequence) {
                    String tokenString = MatchInfoDisplay.sanitize(token.originalText().content().utf16CodeUnits());
                    if (token == leftHead) {
                        sb.append("<span class=\"slot0\">" + tokenString + "</span> ");
                    } else if (token == rightHead) {
                        sb.append("<span class=\"slot1\">" + tokenString + "</span> ");
                    } else {
                        sb.append(tokenString + " ");
                    }
                }
                sb.append("<br/>");
                sb.append("(<span class=\"slot0\">" + MatchInfoDisplay.sanitize(leftHead.originalText().content().utf16CodeUnits()) + "</span>,<span class=\"slot1\">" + MatchInfoDisplay.sanitize(rightHead.originalText().content().utf16CodeUnits()) + "</span>)");
                String orininalHtml = sb.toString();

                // outputCsv
                AMTCsvPrinter.printRecord(
                        objectMapper.writeValueAsString(instanceIdentifier),
                        leftHead.originalText().content().utf16CodeUnits(),
                        rightHead.originalText().content().utf16CodeUnits(),
                        labelPattern.getLabel(),
                        orininalHtml
                );
                // Obviously, the size of htmlStrList can be used as index
                htmlStrList.add(String.valueOf(htmlStrList.size() + 1) + ", " + orininalHtml);
            }

            instanceIdToHEadIdxOffCsvPrinter.printRecord(
                    objectMapper.writeValueAsString(instanceIdentifier),
                    instanceIdentifier.getDocid(),
                    instanceIdentifier.getSentid(),
                    leftHeadWordTokenIdx,
                    rightHeadWordTokenIdx
            );


        }

        BufferedWriter htmlWriter = new BufferedWriter(new FileWriter(new File(filePrefix + "_peek.html")));
        htmlWriter.write(HTMLTemplateString.Bootstrap.header);

        for (String htmlStr : htmlStrList) {
            htmlWriter.write(htmlStr + "<br><br>\n");
        }

        htmlWriter.write(HTMLTemplateString.Bootstrap.footer);


        htmlWriter.close();
        amtcsvWriter.close();
        instanceIdToHeadIdxOffCsvWriter.close();

    }
}
