package com.bbn.akbc.evaluation.tac;

import com.bbn.akbc.common.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EvalStats {

  // set of slots
  public static Set<String> setSlots = new HashSet<String>();

  // for scoring
  public static int nAllRedudant = 0;
  public static int nAllInExact = 0;
  public static int nAllCorrectNonNIL = 0;
  public static int nAllWrong = 0;
  public static int nAllMissed = 0;

  public static Map<Integer, Integer> hop2nAllRedudant = new HashMap<Integer, Integer>();
  public static Map<Integer, Integer> hop2nAllInExact = new HashMap<Integer, Integer>();
  public static Map<Integer, Integer> hop2nAllCorrectNonNIL = new HashMap<Integer, Integer>();
  public static Map<Integer, Integer> hop2nAllWrong = new HashMap<Integer, Integer>();
  public static Map<Integer, Integer> hop2nAllMissed = new HashMap<Integer, Integer>();

  public static Map<Integer, Map<String, Integer>> hop2slot2nAllRedudant =
      new HashMap<Integer, Map<String, Integer>>();
  public static Map<Integer, Map<String, Integer>> hop2slot2nAllInExact =
      new HashMap<Integer, Map<String, Integer>>();
  public static Map<Integer, Map<String, Integer>> hop2slot2nAllCorrectNonNIL =
      new HashMap<Integer, Map<String, Integer>>();
  public static Map<Integer, Map<String, Integer>> hop2slot2nAllWrong =
      new HashMap<Integer, Map<String, Integer>>();
  public static Map<Integer, Map<String, Integer>> hop2slot2nAllMissed =
      new HashMap<Integer, Map<String, Integer>>();
  //

  public static String toHTMLbySlotAndOverallString() {
    StringBuilder sb = new StringBuilder();

    sb.append("<table border = \"1\">");
    sb.append("<thead>");
    sb.append("<tr>");
    sb.append("<th colspan=\"1\" rowspan=\"2\">slot</th>");
    for (int hop : hop2nAllRedudant.keySet()) {
      sb.append("<th colspan=\"4\">hop-" + hop + "</th>");
    }
    sb.append("<th colspan=\"4\">All hops</th>");
    sb.append("</tr>");
    sb.append("<tr>");
    for (int hop : hop2nAllRedudant.keySet()) {
      sb.append("<th>P</th><th>R</th><th>F1</th><th>#total non-NIL</th>");
    }
    sb.append("<th>P</th><th>R</th><th>F1</th><th>#total non-NIL</th>");
    sb.append("</tr>");
    sb.append("</thead>");
    sb.append("<tbody>");

    // sort slots
    List<String> listSlots = new ArrayList<String>();
    listSlots.addAll(setSlots);
    Collections.sort(listSlots);

    for (String slot : listSlots) {
      sb.append("<tr>");
      sb.append("<td>" + slot + "</td>");

      int allHopRedudant = 0;
      int allHopInExact = 0;
      int allHopCorrectNonNIL = 0;
      int allHopWrong = 0;
      int allHopMissed = 0;

      for (int hop : hop2nAllRedudant.keySet()) {
        int corretNonNIL = 0;
        int wrong = 0;
        int redudant = 0;
        int inexact = 0;
        int miss = 0;
        if (hop2slot2nAllCorrectNonNIL.get(hop).containsKey(slot)) {
          corretNonNIL = hop2slot2nAllCorrectNonNIL.get(hop).get(slot);
        }
        if (hop2slot2nAllWrong.get(hop).containsKey(slot)) {
          wrong = hop2slot2nAllWrong.get(hop).get(slot);
        }
        if (hop2slot2nAllRedudant.get(hop).containsKey(slot)) {
          redudant = hop2slot2nAllRedudant.get(hop).get(slot);
        }
        if (hop2slot2nAllInExact.get(hop).containsKey(slot)) {
          inexact = hop2slot2nAllInExact.get(hop).get(slot);
        }
        if (hop2slot2nAllMissed.get(hop).containsKey(slot)) {
          miss = hop2slot2nAllMissed.get(hop).get(slot);
        }

        double precision = 1.0 * corretNonNIL /
            (corretNonNIL + wrong + redudant + inexact);
        double recall = 1.0 * corretNonNIL /
            (corretNonNIL + miss);
        double f1 = 2.0 * precision * recall / (precision + recall);
        int total = corretNonNIL + miss;
        sb.append("<td>" + StringUtil.formatDouble(precision) + "</td>");
        sb.append("<td>" + StringUtil.formatDouble(recall) + "</td>");
        sb.append("<td>" + StringUtil.formatDouble(f1) + "</td>");
        sb.append("<td>" + StringUtil.formatDouble(total) + "</td>");

        allHopRedudant += redudant;
        allHopInExact += inexact;
        allHopCorrectNonNIL += corretNonNIL;
        allHopWrong += wrong;
        allHopMissed += miss;
      }

      // all hop
      double precision = 1.0 * allHopCorrectNonNIL /
          (allHopCorrectNonNIL + allHopWrong + allHopRedudant + allHopInExact);
      double recall = 1.0 * allHopCorrectNonNIL /
          (allHopCorrectNonNIL + allHopMissed);
      double f1 = 2.0 * precision * recall / (precision + recall);
      int total = allHopCorrectNonNIL + allHopMissed;
      sb.append("<td>" + StringUtil.formatDouble(precision) + "</td>");
      sb.append("<td>" + StringUtil.formatDouble(recall) + "</td>");
      sb.append("<td>" + StringUtil.formatDouble(f1) + "</td>");
      sb.append("<td>" + StringUtil.formatDouble(total) + "</td>");

      sb.append("</tr>");
    }

    // all slots
    sb.append("<tr>");
    sb.append("<td><b>Overall</b></td>");
    for (int hop : hop2nAllRedudant.keySet()) {
      int corretNonNIL = 0;
      int wrong = 0;
      int redudant = 0;
      int inexact = 0;
      int miss = 0;
      if (hop2nAllCorrectNonNIL.containsKey(hop)) {
        corretNonNIL = hop2nAllCorrectNonNIL.get(hop);
      }
      if (hop2nAllWrong.containsKey(hop)) {
        wrong = hop2nAllWrong.get(hop);
      }
      if (hop2nAllRedudant.containsKey(hop)) {
        redudant = hop2nAllRedudant.get(hop);
      }
      if (hop2nAllInExact.containsKey(hop)) {
        inexact = hop2nAllInExact.get(hop);
      }
      if (hop2nAllMissed.containsKey(hop)) {
        miss = hop2nAllMissed.get(hop);
      }

      double precision = 1.0 * corretNonNIL /
          (corretNonNIL + wrong + redudant + inexact);
      double recall = 1.0 * corretNonNIL /
          (corretNonNIL + miss);
      double f1 = 2.0 * precision * recall / (precision + recall);
      int total = corretNonNIL + miss;
      sb.append("<td><b>" + StringUtil.formatDouble(precision) + "</b></td>");
      sb.append("<td><b>" + StringUtil.formatDouble(recall) + "</b></td>");
      sb.append("<td><b>" + StringUtil.formatDouble(f1) + "</b></td>");
      sb.append("<td><b>" + StringUtil.formatDouble(total) + "</b></td>");
    }

    // all hop
    double precision = 1.0 * nAllCorrectNonNIL /
        (nAllCorrectNonNIL + nAllWrong + nAllRedudant + nAllInExact);
    double recall = 1.0 * nAllCorrectNonNIL /
        (nAllCorrectNonNIL + nAllMissed);
    double f1 = 2.0 * precision * recall / (precision + recall);
    int total = nAllCorrectNonNIL + nAllMissed;
    sb.append("<td><b>" + StringUtil.formatDouble(precision) + "</b></td>");
    sb.append("<td><b>" + StringUtil.formatDouble(recall) + "</b></td>");
    sb.append("<td><b>" + StringUtil.formatDouble(f1) + "</b></td>");
    sb.append("<td><b>" + StringUtil.formatDouble(total) + "</b></td>");

    sb.append("</tr>");

    sb.append("</tbody></table>");

    return sb.toString();
  }

	/*
        public static String toHTMLoverallStats() {
//	    double precision = 1.0*nFound/(nFound+nWrong+nInexact);
//	    double recall = 1.0*nFound/(nFound+nMiss);
//	    double f1 = 2.0*precision*recall/(precision+recall);
	    double precision = 1.0*nCorrectNonNIL/(nCorrect+nWrong+nRedudant);
	    double recall = 1.0*nCorrectNonNIL/(nCorrectNonNIL+nMissed);
	    double f1 = 2.0*precision*recall/(precision+recall);

		StringBuilder sb = new StringBuilder();

	    sb.append("<table border=\"1\">");
	    sb.append("<tr><td>P</td><td>R</td><td>F1</td></tr>");
	    sb.append("<tr><td>" + precision + "</td><td>" + recall + "</td><td>" + f1 + "</td></tr>");
	    sb.append("</table>");
	    sb.append("<br>");

	    return sb.toString();
	}
	*/
}
