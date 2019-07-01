package com.bbn.akbc.evaluation.tac;

import com.bbn.akbc.io.SimpleHtmlOutputWriter;
import com.bbn.akbc.common.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;


/**
 * Created by bmin on 6/9/16.
 */
public class VisualizeQueries {
  static String getSerifVisHtmlUrlCode(String docId) {
    if(!docId.contains("ENG"))
      docId = docId + ".mpdf";
    String fileUrl = "file://///mercury-04/u42/bmin/runjobs2/expts/scripts/2015CS_bbn2.v1.no_indoc_split.megaDefender/serifxml/"
        + docId + ".xml.vis/index.html";
    return "<a href=\"" + fileUrl + "\">SERIF XML</a>";
  }

  public static void main(String [] argv) throws IOException {
    String strFileQuery = "/nfs/mercury-04/u42/bmin/projects/coldstart/coldstart2015/official_evaluation_results/tools_and_resources_20160401/official_evaluation_results.20160401/KBP2015_Cold_Start_Slot-Filling_Evaluation_Results_2016-03-31/coldstart2015_end_to_end_scoring/tac_kbp_2015_english_cold_start_slot_filling_evaluation_queries_v2.xml";
    String strFileHtmlQuery = strFileQuery+".visualizeHTML.html";

    List<String> queryIdsMissed = FileUtil.readLinesIntoList("/nfs/mercury-04/u42/bmin/projects/coldstart/coldstart2016/list.queries.cs2015.missed");


    System.out.println("=== QueryLoader.loadCanonicalMention");
    QueryLoader.load(strFileQuery);
    for(String queryId : QueryLoader.id2queryEntities.keySet()) {
      System.out.println("id2queryEntities.put" + "\t" + "queryId=" + queryId + "\t" +
          QueryLoader.id2queryEntities.get(queryId).toString().replace("\n", "|"));
    }

    ColdStartCorpus.init("/nfs/mercury-04/u42/bmin/projects/coldstart/coldstart2015/bbn_official_submissions/corpus/cs2015_doc_list_paths.all.shuffled");

    SimpleHtmlOutputWriter writer = new SimpleHtmlOutputWriter(new File(strFileHtmlQuery));
    writer.start();

    for(String queryId : QueryLoader.id2queryEntities.keySet()) {
      if(!queryIdsMissed.contains(queryId))
        continue;

      writer.startCollapsibleSection(queryId, true);

      CSInitQuery csInitQuery = QueryLoader.id2queryEntities.get(queryId);

      int start = csInitQuery.mentions.iterator().next().getSpan().getStart();
      int end = csInitQuery.mentions.iterator().next().getSpan().getEnd();

      EvalTextSpan evalTextSpan = new EvalTextSpan(csInitQuery.getDocId(), start, end);
      String text = getSerifVisHtmlUrlCode(csInitQuery.getDocId()) + "<br>"
          + csInitQuery.getText() + "<br>"
          + csInitQuery.getSlotsHop0Str() + "<br>"
          + normalizeText(ColdStartCorpus.getFullTextWithContextHTML(evalTextSpan));

      writer.writeHtmlContent(text + "<br><br>");

      writer.endCollapsibleSection();
    }

    writer.close();
  }

  static String normalizeText(String text) {
    return text.replace("\n", " ").replace("\r", " ").replace("\t", " ");
  }
}
