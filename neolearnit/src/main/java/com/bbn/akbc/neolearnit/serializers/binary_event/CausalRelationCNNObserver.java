package com.bbn.akbc.neolearnit.serializers.binary_event;

import com.bbn.akbc.common.Pair;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.common.targets.TargetFactory;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;
import com.bbn.akbc.neolearnit.observers.Observer;
import com.bbn.akbc.neolearnit.serializers.observations.CausalRelationCNN;

import java.io.IOException;
import java.io.Writer;

@Deprecated
public class CausalRelationCNNObserver implements Observer<Pair<InstanceIdentifier,LabelPattern>> {
    private final Writer writerSentence;
    private final Writer writerLabel;
    private final Writer writerDistanceToArg1;
    private final Writer writerDistanceToArg2;
    private final Writer writerType;
    private final Writer writerMentionLevel;
    private final Writer writerJson;

    public CausalRelationCNNObserver(Writer writerSentence, Writer writerLabel, Writer writerDistanceToArg1, Writer writerDistanceToArg2, Writer writerType, Writer writerMentionLevel, Writer writerJson){
        this.writerSentence = writerSentence;
        this.writerLabel = writerLabel;
        this.writerDistanceToArg1 = writerDistanceToArg1;
        this.writerDistanceToArg2 = writerDistanceToArg2;
        this.writerType = writerType;
        this.writerMentionLevel = writerMentionLevel;
        this.writerJson = writerJson;
    }

    @Override
    public void observe(Pair<InstanceIdentifier, LabelPattern> observation) {
        InstanceIdentifier instanceIdentifier = observation.getFirst();
        MatchInfo.LanguageMatchInfo languageMatchInfo =
                instanceIdentifier.reconstructMatchInfo(TargetFactory.makeEverythingTarget()).getPrimaryLanguageMatch();
        CausalRelationCNN.Builder builder = new CausalRelationCNN.Builder(languageMatchInfo);
        String arg1type = "NA";
        String arg2type = "NA";
        String arg1MentionLevel = "NA";
        String arg2MentionLevel = "NA";

        builder.withArg1entityType(arg1type);
        builder.withArg1mentionLevel(arg1MentionLevel);
        builder.withArg2entityType(arg2type);
        builder.withArg2mentionLevel(arg2MentionLevel);
        builder.withLabel(observation.getSecond().toIDString());
        builder.withmarkedSentenceString(languageMatchInfo.markedUpTokenString());
        CausalRelationCNN relationMentionInfo = builder.build();
        CausalRelationCNN.SerializationFactory.StringSerializer relationMentionPrinter = new CausalRelationCNN.SerializationFactory.StringSerializer(relationMentionInfo);
        try{
            writerSentence.write(relationMentionPrinter.getSentenceInOneLine() + "\n");
            writerLabel.write(relationMentionPrinter.getLabel() + "\n");
            writerDistanceToArg1.write(relationMentionPrinter.distanceToArg1InOneLine() + "\n");
            writerDistanceToArg2.write(relationMentionPrinter.distanceToArg2InOneLine() + "\n");
            writerType.write(relationMentionPrinter.entityTypeLabelsCoveringArgSpanInOneLine() + "\n");
            writerMentionLevel.write(relationMentionPrinter.mentionLevelLabelsCoveringArgSpanInOneLine() + "\n");
        } catch (IOException e){
            e.printStackTrace();
        }
        CausalRelationCNN.SerializationFactory.JSONStringSerializer jsonStringSerializer = new CausalRelationCNN.SerializationFactory.JSONStringSerializer(relationMentionInfo);
        try{
            writerJson.write(jsonStringSerializer.getJSON() + "\n");
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}
