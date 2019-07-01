package com.bbn.akbc.common;

import com.bbn.akbc.kb.text.TextMention;
import com.bbn.akbc.resource.Resources;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


public class EntityVisualizationWriter {

  static boolean truncate_mention_list = false;
  static int NUM_CANONICAL_MENTION_PER_SYS_ENTITY_SHOWN = 5;

  public static void write(String fileVisualDir,
      Map<String, List<TextMention>> id2mentions) throws IOException {

    for (String sysEntityId : id2mentions.keySet()) {
      String fileHTML = fileVisualDir + "/" + Util.normalizedEntityId(sysEntityId) + ".html";

      PrintWriter pwHtml = new PrintWriter(new FileWriter(fileHTML));
      pwHtml.println("<html>");
      pwHtml
          .println("<head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">");
      pwHtml.println("<title>" + sysEntityId + "</title>");
      pwHtml.println("<body>");

      pwHtml.println("<table border=\"1\">");

      pwHtml.println(
          "<tr><td>doc ID</td><td>offset (start, end)</td><td>Canonical mention (with reference sentence)</tr>");

      List<TextMention> listMentions = new ArrayList<TextMention>(id2mentions.get(sysEntityId));
      Collections.shuffle(listMentions);
      if (truncate_mention_list) {
        if (listMentions.size() > NUM_CANONICAL_MENTION_PER_SYS_ENTITY_SHOWN) {
          listMentions = listMentions.subList(0, NUM_CANONICAL_MENTION_PER_SYS_ENTITY_SHOWN);
        }
      }

      for (TextMention canonicalMention : listMentions) {
//				String sysEntityPathSerifHTML = "\\\\mercury-04\\u42\\bmin\\source\\Active\\Projects\\ColdStart\\data\\ldc_release\\TAC_2012\\TAC_2012_KBP_Cold_Start_Evaluation_Corpus_v1.3.visualization\\" +
//						canonicalMention.docid.substring(canonicalMention.docid.lastIndexOf("_")+1, canonicalMention.docid.lastIndexOf("_")+3) + "\\" + canonicalMention.docid + "\\index.html";
        // String sysEntityPathSerifHTML = "\\\\mercury-04\\u10\\KBP_ColdStart\\2013\\processed\\corpus\\mini_corpus_CS2013.vis\\" +
        // String sysEntityPathSerifHTML = "\\\\mercury-04\\u10\\KBP_ColdStart\\2013\\processed\\corpus\\mini_corpus\\visualization\\" +
        //		canonicalMention.docid + "\\index.html";

        String sysEntityPathSerifHTML =
            Resources.getPathSerifXmlVisualization(canonicalMention.getDocId());
        pwHtml.println(
            "<tr><td>" + "<a href=\"" + sysEntityPathSerifHTML + "\">" + canonicalMention.getDocId()
                + "</a>" + "</td><td>" + "(" + Integer.toString(canonicalMention.getSpan().getStart())
                + "," + Integer.toString(canonicalMention.getSpan().getEnd()) + ")" + "</td><td>"
                + SerifHelper.getTextAnnotated(canonicalMention) + "</td></tr>");
      }

      pwHtml.println("</table>");

      pwHtml.println("</body>");
      pwHtml.println("</html>");
      pwHtml.close();
    }
  }
}
