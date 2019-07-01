package com.bbn.akbc.neolearnit.util;

import com.bbn.akbc.utility.FileUtil;
import com.bbn.serif.io.SerifIOUtils;
import com.bbn.serif.io.SerifXMLLoader;
import com.bbn.serif.theories.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class ReadsNamesForTrainingNeuralModels {

    // Example output: 0       2       Hirabai Badodekar , Gangubai Hangal , Mogubai Kurdikar -RRB- , made the Indian classical music so much greater .        /person/artist /person  HEAD|Badodekar NON_HEAD|Hirabai PARENT|made CLUSTER| CHARACTERS|:ba CHARACTERS|bad CHARACTERS|ado CHARACTERS|dod CHARACTERS|ode CHARACTERS|dek CHARACTERS|eka CHARACTERS|kar CHARACTERS|ar: SHAPE|AasAa ROLE|nsubj BEFORE|<s> AFTER|,
    static String generateLineFromNamedMention(Name name, SentenceTheory sentenceTheory) {
        StringBuilder stringBuilder = new StringBuilder();

        int nameStart = name.tokenSpan().startTokenIndexInclusive();
        int nameEnd = name.tokenSpan().endTokenIndexInclusive()+1;

        stringBuilder.append(nameStart + "\t");
        stringBuilder.append(nameEnd + "\t");

        stringBuilder.append(getTokenSequence(sentenceTheory) + "\t");

        stringBuilder.append(name.type().name().asString() + "\t");
        stringBuilder.append(getFeatures(name, sentenceTheory));

        return stringBuilder.toString().trim();
    }

    // TODO: fix this with head node in parse
    static int getHeadIdx(Name name, SentenceTheory sentenceTheory) {
        return name.tokenSpan().endTokenIndexInclusive();
    }

    static String getFeatures(Name name, SentenceTheory sentenceTheory) {
        StringBuilder stringBuilder = new StringBuilder();

        int headIdx = getHeadIdx(name, sentenceTheory);
        for(int tid=name.tokenSpan().startTokenIndexInclusive(); tid<=name.tokenSpan().endTokenIndexInclusive(); tid++) {
            String tokenText = sentenceTheory.tokenSequence().token(tid).tokenizedText().utf16CodeUnits()
                    .replace("\n", "").replace("\t", "");
            if(headIdx==tid)
                stringBuilder.append("HEAD|" + tokenText + " ");
            else
                stringBuilder.append("NON_HEAD|" + tokenText + " ");
        }

        return stringBuilder.toString().trim();
    }

    static String getTokenSequence(SentenceTheory sentenceTheory) {
        StringBuilder stringBuilder = new StringBuilder();

        for(int tid=0; tid<sentenceTheory.tokenSequence().size(); tid++) {
            Token token = sentenceTheory.tokenSequence().token(tid);
            stringBuilder.append(token.tokenizedText().utf16CodeUnits() + " ");
        }

        return stringBuilder.toString().trim();
    }

    public static void main(String [] argv) throws IOException {
        String strListSerifXmlFiles = argv[0];
        String strFileOutput = argv[1];

        PrintWriter printWriter = new PrintWriter(new File(strFileOutput));

        List<File> filesToProcess = new ArrayList<File>();
        List<String> listStringFiles = FileUtil.readLinesIntoList(strListSerifXmlFiles);
        for(String strFile : listStringFiles)
            filesToProcess.add(new File(strFile));

        SerifXMLLoader serifXMLLoader = SerifXMLLoader.builderWithDynamicTypes().build();

        // read sentence theories
        for (final DocTheory dt : SerifIOUtils.docTheoriesFromFiles(filesToProcess, serifXMLLoader)) {
            for (int sid=0; sid<dt.numSentences(); sid++) {
                SentenceTheory sentenceTheory = dt.sentenceTheory(sid);
                for(Name name : sentenceTheory.names()) {
                    String lineForInstance = generateLineFromNamedMention(name, sentenceTheory);
                    printWriter.println(lineForInstance);
                }
            }
        }

        printWriter.close();
    }
}
