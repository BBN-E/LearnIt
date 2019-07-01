package com.bbn.akbc.topics;

import com.bbn.akbc.evaluation.tac.io.DocTheoryLoader;
import com.bbn.akbc.common.FileUtil;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.SentenceTheory;

import com.google.common.base.Optional;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Created by bmin on 8/11/16.
 */
public class GenerateTestDataSetFromCS2015 {
  public static void main(String [] argv) throws IOException {
    String strFileListSerifXMLs = "/nfs/mercury-04/u42/bmin/projects/coldstart/coldstart2016/names_distribution/list.cs2015.all.serifxmls.mgCoref_cs16actors";

    PrintWriter printWriter = new PrintWriter("/nfs/mercury-04/u42/bmin/projects/coldstart/coldstart2016/metonymy/test_corpus_cs2015.fulltext.txt");

    List<String> listSerifXMLs = FileUtil.readLinesIntoList(strFileListSerifXMLs);
    for (String serifXml : listSerifXMLs) {
      System.out.println("== Processing " + serifXml);

      Optional<DocTheory> docTheory = DocTheoryLoader.readDocTheory(serifXml);
      if (docTheory.isPresent()) {
        printWriter.println(docTheory.get().docid().asString() + " " + "\"" + extractFullTextToLine(docTheory.get()) + "\"");
      }
    }

    printWriter.close();
  }

  static String extractFullTextToLine(DocTheory docTheory) {
    StringBuilder sb = new StringBuilder();
    for(int sid=0; sid<docTheory.numSentences(); sid++) {
      SentenceTheory sentenceTheory = docTheory.sentenceTheory(sid);
      if(sentenceTheory.tokenSequence().isAbsent())
        continue;
      if(sentenceTheory.tokenSequence().isEmpty())
        continue;

      String strSent = sentenceTheory.tokenSequence().tokenSpan().tokenizedText(docTheory).utf16CodeUnits();
      sb.append(strSent.replace("\t", " ").replace("\n", " ").trim() + " ");
    }

    return sb.toString().trim();
  }
}
