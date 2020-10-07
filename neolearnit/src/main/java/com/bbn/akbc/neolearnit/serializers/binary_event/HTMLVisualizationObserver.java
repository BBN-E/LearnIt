package com.bbn.akbc.neolearnit.serializers.binary_event;

import com.bbn.akbc.common.Pair;
import com.bbn.akbc.neolearnit.common.Annotation;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfoDisplay;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;
import com.bbn.akbc.neolearnit.serializers.ExternalAnnotationBuilder;
import com.bbn.akbc.neolearnit.serializers.observations.HTMLTemplateString;
import com.bbn.bue.common.exceptions.NotImplementedException;
import com.bbn.serif.theories.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HTMLVisualizationObserver extends ExternalAnnotationBuilder {

    final Map<SentenceTheory, Map<Pair<Integer, Integer>, List<String>>> labeledSpanTypes;
    final String outputHtmlPath;

    public HTMLVisualizationObserver(String outputHtmlPath) {
        super();
        this.labeledSpanTypes = new HashMap<>();
        this.outputHtmlPath = outputHtmlPath;
    }

    public String getSpanningType(SentenceTheory st, int start, int end, InstanceIdentifier.SpanningType slotType) {
        if (this.labeledSpanTypes.getOrDefault(st, new HashMap<>()).containsKey(new Pair<>(start, end))) {
            return String.join(",", this.labeledSpanTypes.get(st).get(new Pair<>(start, end)));
        } else {
            Spanning spanning = InstanceIdentifier.getSpanning(st, start, end, slotType).get();
            if (spanning instanceof EventMention) {
                EventMention eventMention = (EventMention) spanning;
                return eventMention.type().asString();
            } else if (spanning instanceof Mention) {
                Mention mention = (Mention) spanning;
                return mention.entityType().name().asString();
            } else {
                throw new NotImplementedException("You should decide the case for ValueMention");
            }
        }
    }

    @Override
    public void build() throws Exception {
        Map<InstanceIdentifier, SentenceTheory> instanceIdentifierSentenceTheoryMap = this.resolveSentenceTheory();
        for (InstanceIdentifier instanceIdentifier : this.inMemoryAnnotationStorage.getAllInstanceIdentifier()) {
            if (instanceIdentifier.getSlot1Start() == -1 && instanceIdentifier.getSlot1End() == -1) {
                Map<Pair<Integer, Integer>, List<String>> sentSpanBuf = this.labeledSpanTypes.getOrDefault(instanceIdentifierSentenceTheoryMap.get(instanceIdentifier), new HashMap<>());
                List<String> typeBuf = sentSpanBuf.getOrDefault(new Pair<>(instanceIdentifier.getSlot0Start(), instanceIdentifier.getSlot0End()), new ArrayList<>());
                for (LabelPattern labelPattern : this.inMemoryAnnotationStorage.lookupInstanceIdentifierAnnotation(instanceIdentifier)) {
                    String frozenString;
                    if (labelPattern.getFrozenState().equals(Annotation.FrozenState.FROZEN_GOOD)) {
                        frozenString = "+";
                    } else if (labelPattern.getFrozenState().equals(Annotation.FrozenState.FROZEN_BAD)) {
                        frozenString = "-";
                    } else {
                        frozenString = "N";
                    }
                    typeBuf.add(labelPattern.getLabel() + "/" + frozenString);
                }
                sentSpanBuf.put(new Pair<>(instanceIdentifier.getSlot0Start(), instanceIdentifier.getSlot0End()), typeBuf);
                this.labeledSpanTypes.put(instanceIdentifierSentenceTheoryMap.get(instanceIdentifier), sentSpanBuf);
            }
        }

        List<String> htmlStrList = new ArrayList<>();

        for (InstanceIdentifier instanceIdentifier : this.inMemoryAnnotationStorage.getAllInstanceIdentifier()) {
            if (instanceIdentifier.getSlot1Start() != -1 && instanceIdentifier.getSlot1End() != -1) {
                final SentenceTheory sentenceTheory = instanceIdentifierSentenceTheoryMap.get(instanceIdentifier);
                final TokenSequence tokenSequence = sentenceTheory.tokenSequence();
                final EventMention leftEventMention = (EventMention) InstanceIdentifier.getSpanning(sentenceTheory, instanceIdentifier.getSlot0Start(), instanceIdentifier.getSlot0End(), instanceIdentifier.getSlot0SpanningType()).get();
                final EventMention rightEventMention = (EventMention) InstanceIdentifier.getSpanning(sentenceTheory, instanceIdentifier.getSlot1Start(), instanceIdentifier.getSlot1End(), instanceIdentifier.getSlot1SpanningType()).get();

                final int leftHeadWordTokenIdx = leftEventMention.anchorNode().head().tokenSpan().endTokenIndexInclusive();
                final int rightHeadWordTokenIdx = rightEventMention.anchorNode().head().tokenSpan().endTokenIndexInclusive();

                final String leftType = this.getSpanningType(sentenceTheory, instanceIdentifier.getSlot0Start(), instanceIdentifier.getSlot0End(), instanceIdentifier.getSlot0SpanningType());
                final String rightType = this.getSpanningType(sentenceTheory, instanceIdentifier.getSlot1Start(), instanceIdentifier.getSlot1End(), instanceIdentifier.getSlot1SpanningType());
                for (LabelPattern labelPattern : this.inMemoryAnnotationStorage.lookupInstanceIdentifierAnnotation(instanceIdentifier)) {
                    Token leftHead = tokenSequence.token(leftHeadWordTokenIdx);
                    Token rightHead = tokenSequence.token(rightHeadWordTokenIdx);
                    StringBuilder sb = new StringBuilder();
                    // Obviously, the size of htmlStrList can be used as index
                    sb.append(htmlStrList.size() + 1);
                    sb.append(", ");
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
//                    sb.append("(<span class=\"slot0\">" + MatchInfoDisplay.sanitize(leftHead.originalText().content().utf16CodeUnits()) + "</span>,<span class=\"slot1\">" + MatchInfoDisplay.sanitize(rightHead.originalText().content().utf16CodeUnits()) + "</span>)");
//                    sb.append("(<span class=\"slot0\">" + MatchInfoDisplay.sanitize(leftType) + "</span>,<span class=\"slot1\">" + MatchInfoDisplay.sanitize(rightType) + "</span>)");
                    sb.append("&lt;<span class=\"slot0\">" + MatchInfoDisplay.sanitize(leftHead.originalText().content().utf16CodeUnits()) + ": [" + MatchInfoDisplay.sanitize(leftType) + "]</span>");
                    sb.append(", " + labelPattern.getLabel() + ", ");
                    sb.append("<span class=\"slot1\">" + MatchInfoDisplay.sanitize(rightHead.originalText().content().utf16CodeUnits()) + ": [" + MatchInfoDisplay.sanitize(rightType) + "]</span>&gt;");
                    String orininalHtml = sb.toString();
                    htmlStrList.add(orininalHtml);
                }
            }
        }
        BufferedWriter htmlWriter = new BufferedWriter(new FileWriter(new File(outputHtmlPath)));
        htmlWriter.write(HTMLTemplateString.Bootstrap.header);
        for (String htmlStr : htmlStrList) {
            htmlWriter.write(htmlStr + "<br><br>\n");
        }
        htmlWriter.write(HTMLTemplateString.Bootstrap.footer);
        htmlWriter.close();
    }
}
