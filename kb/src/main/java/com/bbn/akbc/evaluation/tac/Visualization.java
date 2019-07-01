package com.bbn.akbc.evaluation.tac;

import com.bbn.akbc.common.Util;
import com.bbn.akbc.evaluation.tac.io.AssessmentReader;
import com.bbn.akbc.evaluation.tac.io.QueryReader;
import com.bbn.akbc.kb.text.TextMention;
import com.bbn.akbc.common.HTMLHelper;
import com.bbn.akbc.resource.Resources;
import com.bbn.akbc.common.SerifHelper;

import com.google.common.base.Optional;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Visualization {

  static boolean isCS2014Eval = false;

  static int NUM_CANONICAL_MENTION_PER_SYS_ENTITY_SHOWN = 5;
  // query entity ID to entity in system output
  static Map<String, String> queryToEntityInSysOut = new HashMap<String, String>();

  static List<CSAssessment> listAssessment = new ArrayList<CSAssessment>();

  // set of equivalent classes
  static Set<EquivalentClass> setEquivalentClass = new HashSet<EquivalentClass>();

  // set of wrong/inexact answer
  static List<String> setResponseIdWrong = new ArrayList<String>();
  static List<CSAssessment> listAssessmentWrong = new ArrayList<CSAssessment>();
  static List<String> setResponseIdInExact = new ArrayList<String>();
  static List<CSAssessment> listAssessmentInExact = new ArrayList<CSAssessment>();

  static Map<String, List<TextMention>> id2listCanonicalMention =
      new HashMap<String, List<TextMention>>();
  static Map<String, List<TextMention>> eclassStr2listMentions =
      new HashMap<String, List<TextMention>>();
  static Map<String, String> response2eclassStr = new HashMap<String, String>();

  static Map<String, CSInitQuery> id2queryEntities = new HashMap<String, CSInitQuery>();

  static String fileVisualDir;
  static String fileDirEntities;
  static String fileDirEquivalentClasses;

  public static void make_sub_directories() {
    fileDirEntities = fileVisualDir + "/entities/";
    fileDirEquivalentClasses = fileVisualDir + "/eclass/";

    File fDirEntities = new File(fileDirEntities);
    if (!fDirEntities.exists()) {
      fDirEntities.mkdir();
    }

    File fDirEclass = new File(fileDirEquivalentClasses);
    if (!fDirEclass.exists()) {
      fDirEclass.mkdir();
    }
  }

  public static void main(String[] argv) throws IOException {
    for (int i = 0; i < 9; i++) {
      System.out.println("argv[" + i + "]: " + argv[i]);
    }

    String resolveQueriesLog = argv[0];
    String scoreTraceLog = argv[1];
    String fileSystemKB = argv[2];
    String fileAssessment = argv[3];
    String fileQuery = argv[4];
    fileVisualDir = argv[5];

    // make subdirectories to hold entities and eclasses
    make_sub_directories();

    String dirSerifXml =
        argv[6]; // "/nfs/mercury-04/u10/KBP_ColdStart/2013/processed/scripts/mini_corpus_CS2013/output.parallel.win/output/"
    String dirSerifXmlVisualization = argv[7];
    String evalYear = argv[8];

    if (evalYear.equals("2014CS")) {
      isCS2014Eval = true;
    } else {
      isCS2014Eval = false;
    }

    Resources.setPathSerifXml(dirSerifXml);
    Resources.setDirSerifXmlVisualization(dirSerifXmlVisualization);
    Resources.setEvalYear(evalYear);

    String fileVisualDirWindows = "\\" + fileVisualDir.substring(4).replace("/", "\\");

    File dir = new File(fileVisualDir);
    if (!dir.exists()) {
      if (!dir.mkdir()) {
        System.err.println("can't create directory: " + fileVisualDir);
      }

    }

    String fileHTML = fileVisualDir + "/index.html";

    loadEvalData(resolveQueriesLog, scoreTraceLog);
    loadSystemMentions(fileSystemKB);
    loadAssessmentLDC(fileAssessment);
    loadQueries(fileQuery);

    // write entities
    writeSystemMentionHTML(fileVisualDir);

    // write equivalent classes
    writeEquivalentClasses(fileVisualDir);

    writeHTML(fileHTML, fileVisualDirWindows);
  }

  public static void writeEquivalentClasses(String fileVisualDir) throws IOException {
    for (EquivalentClass eclass : setEquivalentClass) {
//			if(eclass.judgement.equals("r")) continue;

      String fileHTML = fileDirEquivalentClasses + eclass.id.replace(":", "-").replace("/", "-")
          .replace("\\", "-") + ".html";

      PrintWriter pwHtml = new PrintWriter(new FileWriter(fileHTML));
      pwHtml.println("<html>");
      pwHtml
          .println("<head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">");
      pwHtml.println("<title>" + eclass.id + "</title>");
      pwHtml.println("</head>");
      pwHtml.println("<body>");

      pwHtml.println("<table border=\"1\">");
      pwHtml.println("<thead>");
      pwHtml.println("<tr>");
      pwHtml.println("<th>uid</th>");
      pwHtml.println("<th>responseId</th>");
      pwHtml.println("<th>query</th>");
      pwHtml.println("<th>system entity</th>");
      pwHtml.println("<th>slot</th>");
      pwHtml.println("<th>filler</th>");
      pwHtml.println("<th>J1</th>");
      pwHtml.println("<th>J2</th>");
      pwHtml.println("<th>equivalent_class</th>");
      pwHtml.println("</tr>");
      pwHtml.println("</thead>");
      pwHtml.println("<tbody>");

      int row_id = 0;
      for (CSAssessment assessment : eclass.listAssessment) {
        pwHtml.println("<tr style=\"color: " + "black" + "; background: " + HTMLHelper
            .getColorForJudgement(assessment.judgement1, assessment.inSys.get()) + ";\">");
        String systemEntityId = "NIL";

        if (queryToEntityInSysOut
            .containsKey(assessment.queryId)) { // initial queries & query found
          systemEntityId = queryToEntityInSysOut.get(assessment.queryId);

          String pathToEntityHTML =
              "../entities/" + Util.normalizedEntityId(systemEntityId) + ".html";
          String entityHTMLCode = "NIL";
          if (id2listCanonicalMention.containsKey(systemEntityId)) {
            if (!id2listCanonicalMention.get(systemEntityId).isEmpty()) {
              entityHTMLCode = "<a href=\"" + pathToEntityHTML + "\">" + systemEntityId + "</a>";
            }
          }
          assessment.setUid(row_id++);
          pwHtml.println(HTMLHelper
              .getHTML(assessment, id2queryEntities.get(assessment.queryId), entityHTMLCode, ""));
        } else if (response2eclassStr
            .containsKey(assessment.parentId)) { // query are higher hops that needs to be resolved
          String query_eclass_id = response2eclassStr.get(assessment.parentId);
          if (eclassStr2listMentions.containsKey(query_eclass_id)) {
            String pathToEntityHTML =
                "../entities/" + Util.normalizedEntityId(query_eclass_id) + ".html";
            String entityHTMLCode = "NIL";
            if (eclassStr2listMentions.containsKey(query_eclass_id)) {
              if (!eclassStr2listMentions.get(query_eclass_id).isEmpty()) {
                entityHTMLCode = "<a href=\"" + pathToEntityHTML + "\">" + query_eclass_id + "</a>";
              }
            }
            assessment.setUid(row_id++);
            pwHtml.println(
                HTMLHelper.getHTML(assessment, eclassStr2listMentions.get(query_eclass_id).get(0),
                    query_eclass_id,
                    entityHTMLCode, ""));
          }
        } else {
          System.out.println(
              "Query NOT found (visualization.writeEquivalentClasses): " + assessment.toString());
        }
        pwHtml.println("</tr>");
      }

      pwHtml.println("</table>");
      pwHtml.println("</body>");
      pwHtml.println("</html>");
      pwHtml.close();
    }
  }

  public static void writeHTML(String fileHTML, String fileVisualDir) throws IOException {
    PrintWriter pwHtml = new PrintWriter(new FileWriter(fileHTML));

    pwHtml.println("<html>");
    pwHtml.println(HTMLHelper.getHTMLVisMainHeader());
    pwHtml.println("<body>");
    pwHtml.println(EvalStats.toHTMLbySlotAndOverallString());
    pwHtml.println("<br>");

//      pwHtml.println(EvalStats.toHTMLoverallStats());

    pwHtml.println(
        "<p>J1: human judgement on the slot filler. J2: human judgement on the reported justification<br>");
    pwHtml.println(
        "Correct - <font color = \"green\">green</font>, Wrong - <font color = \"orange\">orange</font>, Miss - <font color = \"lightblue\">lightblue</font>, Inexact - <font color = \"grey\">grey</font></p><br>");

//	    pwHtml.println("<table border=\"1\">");
//   	pwHtml.println("<tr><td>uid</td><td>responseId</td><td>query</td><td>system entity</td><td>slot</td><td>filler</td><td>J1</td><td>J2</td><td>equivalent_class</td></tr>");

    pwHtml.println("<table id=\"tbl1\" border=\"1\">");
    pwHtml.println("<thead>");
    pwHtml.println("<tr>");
    pwHtml.println("<th>uid</th>");
    pwHtml.println("<th>responseId</th>");
    pwHtml.println("<th>query</th>");
    pwHtml.println("<th>system entity</th>");
    pwHtml.println("<th>slot</th>");
    pwHtml.println("<th>filler</th>");
    pwHtml.println("<th>J1</th>");
    pwHtml.println("<th>J2</th>");
    pwHtml.println("<th>equivalent_class</th>");
    pwHtml.println("</tr>");
    pwHtml.println("</thead>");
    pwHtml.println("<tbody>");

    int row_id = 0;
    for (EquivalentClass eclass : setEquivalentClass) {
//	    	if(eclass.judgement.equals("r")) continue; // skip redundant

      String color = "grey";
      if (eclass.judgement.equals("C")) {
        color = "green";
      } else if (eclass.judgement.equals("W")) {
        color = "orange";
      } else if (eclass.judgement.equals("M")) {
        color = "lightblue";
      }

      if (eclass.listAssessment.isEmpty()) {
        System.out.println("+++++ " + eclass.hop + " " + eclass.id + " " + eclass.judgement + " "
            + eclass.responseID);
        continue;
      }

      CSAssessment assessment = eclass.listAssessment.get(0); // representative assessment;

      String systemEntityId = "NIL";
      if (queryToEntityInSysOut.containsKey(assessment.queryId)) { // initial query & found
        pwHtml.println("<tr style=\"color: " + "black" + "; background: " + color + ";\">");

        systemEntityId = queryToEntityInSysOut.get(assessment.queryId);
        String pathToEntityHTML = "./entities/" + Util.normalizedEntityId(systemEntityId) + ".html";
        String entityHTMLCode = "NIL";
        if (id2listCanonicalMention.containsKey(systemEntityId)) {
          if (!id2listCanonicalMention.get(systemEntityId).isEmpty()) {
            entityHTMLCode = "<a href=\"" + pathToEntityHTML + "\">" + systemEntityId + "</a>";
          }
        }
        assessment.uid = Optional.of(row_id++);
        String path2eclassHTML =
            "./eclass/" + eclass.id.replace(":", "-").replace("/", "-").replace("\\", "-")
                + ".html";
        String eclassHTMLCode =
            "<a href=\"" + path2eclassHTML + "\">" + eclass.id + "(" + eclass.listAssessment.size()
                + ")" + "</a>";
        pwHtml.println(HTMLHelper
            .getHTML(assessment, id2queryEntities.get(assessment.queryId), entityHTMLCode,
                eclassHTMLCode));
        pwHtml.println("</tr>");
      } else if (response2eclassStr
          .containsKey(assessment.parentId)) { // query are higher hops that needs to be resolved
        pwHtml.println("<tr style=\"color: " + "black" + "; background: " + color + ";\">");

        String query_eclass_id = response2eclassStr.get(assessment.parentId);
        if (eclassStr2listMentions.containsKey(query_eclass_id)) {
          String pathToEntityHTML =
              "./entities/" + Util.normalizedEntityId(query_eclass_id) + ".html";
          String entityHTMLCode = "NIL";
          if (eclassStr2listMentions.containsKey(query_eclass_id)) {
            if (!eclassStr2listMentions.get(query_eclass_id).isEmpty()) {
              entityHTMLCode = "<a href=\"" + pathToEntityHTML + "\">" + query_eclass_id + "</a>";
            }
          }
          assessment.uid = Optional.of(row_id++);
          String path2eclassHTML =
              "./eclass/" + eclass.id.replace(":", "-").replace("/", "-").replace("\\", "-")
                  + ".html";
          String eclassHTMLCode =
              "<a href=\"" + path2eclassHTML + "\">" + eclass.id + "(" + eclass.listAssessment
                  .size() + ")" + "</a>";
          pwHtml.println(HTMLHelper
              .getHTML(assessment, eclassStr2listMentions.get(query_eclass_id).get(0),
                  query_eclass_id,
                  entityHTMLCode, eclassHTMLCode));
          pwHtml.println("</tr>");
        }
      } else {
        System.out.println("Query NOT found (visualization.writeHtml1): " + assessment.toString());
      }
    }

    for (CSAssessment assessment : listAssessmentWrong) {
      String color = "orange";

      if (queryToEntityInSysOut.containsKey(assessment.queryId)) { // initial query & found
        pwHtml.println("<tr style=\"color: " + "black" + "; background: " + color + ";\">");

        String systemEntityId = "NIL";
        if (queryToEntityInSysOut.containsKey(assessment.queryId)) {
          systemEntityId = queryToEntityInSysOut.get(assessment.queryId);
        }
        String pathToEntityHTML = "./entities/" + Util.normalizedEntityId(systemEntityId) + ".html";
        String entityHTMLCode = "NIL";
        if (id2listCanonicalMention.containsKey(systemEntityId)) {
          if (!id2listCanonicalMention.get(systemEntityId).isEmpty()) {
            entityHTMLCode = "<a href=\"" + pathToEntityHTML + "\">" + systemEntityId + "</a>";
          }
        }
        assessment.uid = Optional.of(row_id++);

        String eclassHTMLCode = "NIL";

        pwHtml.println(HTMLHelper
            .getHTML(assessment, id2queryEntities.get(assessment.queryId), entityHTMLCode,
                eclassHTMLCode));
        pwHtml.println("</tr>");
      } else if (response2eclassStr
          .containsKey(assessment.parentId)) { // query are higher hops that needs to be resolved
        pwHtml.println("<tr style=\"color: " + "black" + "; background: " + color + ";\">");

        String query_eclass_id = response2eclassStr.get(assessment.parentId);
        if (eclassStr2listMentions.containsKey(query_eclass_id)) {
          String pathToEntityHTML =
              "./entities/" + Util.normalizedEntityId(query_eclass_id) + ".html";
          String entityHTMLCode = "NIL";
          if (eclassStr2listMentions.containsKey(query_eclass_id)) {
            if (!eclassStr2listMentions.get(query_eclass_id).isEmpty()) {
              entityHTMLCode = "<a href=\"" + pathToEntityHTML + "\">" + query_eclass_id + "</a>";
            }
          }
          assessment.uid = Optional.of(row_id++);
          String eclassHTMLCode = "NIL";
          pwHtml.println(HTMLHelper
              .getHTML(assessment, eclassStr2listMentions.get(query_eclass_id).get(0),
                  query_eclass_id,
                  entityHTMLCode, eclassHTMLCode));
          pwHtml.println("</tr>");
        }
      } else {
        System.out.println("Query NOT found (visualization.writeHtml2): " + assessment.toString());
      }
    }
    for (CSAssessment assessment : listAssessmentInExact) {
      String color = "grey";

      if (queryToEntityInSysOut.containsKey(assessment.queryId)) { // initial query & found
        pwHtml.println("<tr style=\"color: " + "black" + "; background: " + color + ";\">");

        String systemEntityId = "NIL";
        if (queryToEntityInSysOut.containsKey(assessment.queryId)) {
          systemEntityId = queryToEntityInSysOut.get(assessment.queryId);
        }
        String pathToEntityHTML = "./entities/" + Util.normalizedEntityId(systemEntityId) + ".html";
        String entityHTMLCode = "NIL";
        if (id2listCanonicalMention.containsKey(systemEntityId)) {
          if (!id2listCanonicalMention.get(systemEntityId).isEmpty()) {
            entityHTMLCode = "<a href=\"" + pathToEntityHTML + "\">" + systemEntityId + "</a>";
          }
        }
        assessment.uid = Optional.of(row_id++);

        String eclassHTMLCode = "NIL";

        pwHtml.println(HTMLHelper
            .getHTML(assessment, id2queryEntities.get(assessment.queryId), entityHTMLCode,
                eclassHTMLCode));
        pwHtml.println("</tr>");
      } else if (response2eclassStr
          .containsKey(assessment.parentId)) { // query are higher hops that needs to be resolved
        pwHtml.println("<tr style=\"color: " + "black" + "; background: " + color + ";\">");

        String query_eclass_id = response2eclassStr.get(assessment.parentId);
        if (eclassStr2listMentions.containsKey(query_eclass_id)) {
          String pathToEntityHTML =
              "./entities/" + Util.normalizedEntityId(query_eclass_id) + ".html";
          String entityHTMLCode = "NIL";
          if (eclassStr2listMentions.containsKey(query_eclass_id)) {
            if (!eclassStr2listMentions.get(query_eclass_id).isEmpty()) {
              entityHTMLCode = "<a href=\"" + pathToEntityHTML + "\">" + query_eclass_id + "</a>";
            }
          }
          assessment.uid = Optional.of(row_id++);
          String eclassHTMLCode = "NIL";
          pwHtml.println(HTMLHelper
              .getHTML(assessment, eclassStr2listMentions.get(query_eclass_id).get(0),
                  query_eclass_id,
                  entityHTMLCode, eclassHTMLCode));
          pwHtml.println("</tr>");
        }
      } else {
        System.out.println("Query NOT found (visualization.writeHtml3): " + assessment.toString());
      }
    }

    pwHtml.println("</tbody>");
    pwHtml.println("</table>");

    pwHtml.println("</body>");
    pwHtml.println("</html>");
    pwHtml.close();
  }

  public static void loadQueries(String fileQuery) throws IOException {
    List<Query> queries = QueryReader.readQueriesFromFile(fileQuery, false);
    for (Query query : queries) {
      if (query instanceof CSInitQuery) {
        id2queryEntities.put(query.id, (CSInitQuery) query);
      }
    }
  }

  static String getSlot(String expandedQueryId) {
    String[] slotFields = expandedQueryId.split(":", 4);
    if (slotFields.length != 4) {
      System.out.println("Invalid slot " + expandedQueryId);
      return "error";
    }
    return slotFields[1] + ":" + slotFields[2];
  }

  public static boolean isHop1Query(String queryStringAndSlot) {
    String[] fields = queryStringAndSlot.split(":");
    if (fields[0].split("_").length >= 4) {
      return true;
    } else {
      return false;
    }
  }

  public static String getCompatibleJudgementString(String strJudgementCS2014) {
    if (strJudgementCS2014.equals("CORRECT")) {
      return "C";
    } else if (strJudgementCS2014.equals("INCORRECT")) {
      return "W";
    } else {
      System.err.println("error judgement string: " + strJudgementCS2014);
      System.exit(-1);
    }

    return "W";
  }

  public static void loadEvalData(String resolveQueriesLog, String scoreTraceLog)
      throws IOException {
    BufferedReader reader;
    String sline;

    reader = new BufferedReader(new FileReader(resolveQueriesLog));
    while ((sline = reader.readLine()) != null) {
      String[] fields = sline.trim().split("\t");
      if (fields.length != 3) {
        continue;
      }

      if (!fields[0].equals("dbg")) {
        continue;
      }

      if (isCS2014Eval) {
        queryToEntityInSysOut.put(fields[1], fields[2]);
      } else {
        queryToEntityInSysOut.put(fields[1] + "_00", fields[2]);
      }
    }
    reader.close();

//		List<String> listOfCSscorerJudgements = new ArrayList<String>();
    reader = new BufferedReader(new FileReader(scoreTraceLog));
    // trace 1 M CS_ENG_000_01:per:title:CS_ENG_000_00:org:membership:0:CS_ENG_000_entity1 NIL CS_ENG_000_01:per:title:CS_ENG_000_00:org:membership:0:CS_ENG_000_entity1:CS_ENG_000_entity2
    while ((sline = reader.readLine()) != null) {
      String[] fields = sline.trim().split(" ");

      if (!fields[0].equals("trace")) {
        continue;
      }

      int hop;
      String judgement;
      String expandedQueryId;
      String responseId;
      String equivalent_class;
      String slot;

      if (isCS2014Eval) {
        hop = isHop1Query(fields[3]) ? 1 : 0;
        judgement = getCompatibleJudgementString(fields[1]);
        expandedQueryId = fields[3];
        responseId = "dummy response ID";
        equivalent_class = fields[3] + "_" + fields[2];
        slot = expandedQueryId.substring(expandedQueryId.indexOf(":") + 1);
      } else {
        hop = Integer.parseInt(fields[1]);
        judgement = fields[2];
        expandedQueryId = fields[3];
        responseId = fields[4];
        equivalent_class = fields[5];
        slot = getSlot(expandedQueryId);
      }

      EvalStats.setSlots.add(slot);

      if (!EvalStats.hop2slot2nAllRedudant.containsKey(hop)) {
        EvalStats.hop2slot2nAllRedudant.put(hop, new HashMap<String, Integer>());
      }
      if (!EvalStats.hop2slot2nAllInExact.containsKey(hop)) {
        EvalStats.hop2slot2nAllInExact.put(hop, new HashMap<String, Integer>());
      }
      if (!EvalStats.hop2slot2nAllCorrectNonNIL.containsKey(hop)) {
        EvalStats.hop2slot2nAllCorrectNonNIL.put(hop, new HashMap<String, Integer>());
      }
      if (!EvalStats.hop2slot2nAllWrong.containsKey(hop)) {
        EvalStats.hop2slot2nAllWrong.put(hop, new HashMap<String, Integer>());
      }
      if (!EvalStats.hop2slot2nAllMissed.containsKey(hop)) {
        EvalStats.hop2slot2nAllMissed.put(hop, new HashMap<String, Integer>());
      }

      if (judgement.equals("C") && !responseId.equals("NIL")) {
        if (!EvalStats.hop2nAllCorrectNonNIL.containsKey(hop)) {
          EvalStats.hop2nAllCorrectNonNIL.put(hop, 1);
        } else {
          EvalStats.hop2nAllCorrectNonNIL.put(hop, EvalStats.hop2nAllCorrectNonNIL.get(hop) + 1);
        }

        if (!EvalStats.hop2slot2nAllCorrectNonNIL.get(hop).containsKey(slot)) {
          EvalStats.hop2slot2nAllCorrectNonNIL.get(hop).put(slot, 1);
        } else {
          EvalStats.hop2slot2nAllCorrectNonNIL.get(hop)
              .put(slot, EvalStats.hop2slot2nAllCorrectNonNIL.get(hop).get(slot) + 1);
        }

        EvalStats.nAllCorrectNonNIL++;
      } else if (judgement.equals("W")) {
        if (!EvalStats.hop2nAllWrong.containsKey(hop)) {
          EvalStats.hop2nAllWrong.put(hop, 1);
        } else {
          EvalStats.hop2nAllWrong.put(hop, EvalStats.hop2nAllWrong.get(hop) + 1);
        }

        if (!EvalStats.hop2slot2nAllWrong.get(hop).containsKey(slot)) {
          EvalStats.hop2slot2nAllWrong.get(hop).put(slot, 1);
        } else {
          EvalStats.hop2slot2nAllWrong.get(hop)
              .put(slot, EvalStats.hop2slot2nAllWrong.get(hop).get(slot) + 1);
        }

        EvalStats.nAllWrong++;
      } else if (judgement.equals("M")) {
        if (!EvalStats.hop2nAllMissed.containsKey(hop)) {
          EvalStats.hop2nAllMissed.put(hop, 1);
        } else {
          EvalStats.hop2nAllMissed.put(hop, EvalStats.hop2nAllMissed.get(hop) + 1);
        }

        if (!EvalStats.hop2slot2nAllMissed.get(hop).containsKey(slot)) {
          EvalStats.hop2slot2nAllMissed.get(hop).put(slot, 1);
        } else {
          EvalStats.hop2slot2nAllMissed.get(hop)
              .put(slot, EvalStats.hop2slot2nAllMissed.get(hop).get(slot) + 1);
        }

        EvalStats.nAllMissed++;
      } else if (judgement.equals("r")) {
        if (!EvalStats.hop2nAllRedudant.containsKey(hop)) {
          EvalStats.hop2nAllRedudant.put(hop, 1);
        } else {
          EvalStats.hop2nAllRedudant.put(hop, EvalStats.hop2nAllRedudant.get(hop) + 1);
        }

        if (!EvalStats.hop2slot2nAllRedudant.get(hop).containsKey(slot)) {
          EvalStats.hop2slot2nAllRedudant.get(hop).put(slot, 1);
        } else {
          EvalStats.hop2slot2nAllRedudant.get(hop)
              .put(slot, EvalStats.hop2slot2nAllRedudant.get(hop).get(slot) + 1);
        }

        EvalStats.nAllRedudant++;
      } else if (judgement.equals("X")) {
        if (!EvalStats.hop2nAllInExact.containsKey(hop)) {
          EvalStats.hop2nAllInExact.put(hop, 1);
        } else {
          EvalStats.hop2nAllInExact.put(hop, EvalStats.hop2nAllInExact.get(hop) + 1);
        }

        if (!EvalStats.hop2slot2nAllInExact.get(hop).containsKey(slot)) {
          EvalStats.hop2slot2nAllInExact.get(hop).put(slot, 1);
        } else {
          EvalStats.hop2slot2nAllInExact.get(hop)
              .put(slot, EvalStats.hop2slot2nAllInExact.get(hop).get(slot) + 1);
        }

        EvalStats.nAllInExact++;
      }

//			if(fields[0].equals("trace") && fields[1].equals("0") && (fields[2].equals("C")||fields[2].equals("M")||fields[2].equals("W")||fields[2].equals("r"))) {
      // for "W" and "X", there is no equivalent class, should manually add later on
//			if(hop==0) {
      if (judgement.equals("C") || judgement.equals("M")) {
        EquivalentClass ec;
        if (isCS2014Eval) {
          ec = new EquivalentClass(hop, judgement, expandedQueryId, responseId);
        } else {
          ec = new EquivalentClass(sline);
        }

        if (judgement.equals("C")) {
          ec.inSys = true;
        }
        if (judgement.equals("M")) {
          ec.inSys = false;
        }
        setEquivalentClass.add(ec);
        //				System.out.println(sline);
        //				listOfCSscorerJudgements.add(sline);
      } else if (judgement.equals("W")) {
        setResponseIdWrong.add(responseId);
      } else if (judgement.equals("X")) {
        setResponseIdInExact.add(responseId);
      }
//			}
    }
    reader.close();
  }

  public static String getExpandedQueryId(CSAssessment assessment,
      Map<String, String> response2eclassStr) {
    String parent_eclass = "0";
    if (!assessment.parentId.equals("NIL")) {
      parent_eclass = response2eclassStr.get(assessment.parentId);
    }

    String expandedQueryId =
        assessment.queryId + ":" + assessment.slot.toString() + ":" + parent_eclass;
    return expandedQueryId;
  }

  // T00019  CS_ENG_328_00   NIL     org:students    PENN_SAS_ENG_07773      Tatyana Svitkina        1726    1741    1876    1917    1       3       CS_ENG_328_entity1
  public static void loadAssessmentLDC(String fileAssessment) throws IOException {
    if (isCS2014Eval) {
      listAssessment = AssessmentReader.readCSAssessmentsFromFileCS2014(fileAssessment);
    } else {
      listAssessment = AssessmentReader.readCSAssessmentsFromFile(fileAssessment);
    }

    // get expanded equivalent classes
    for (CSAssessment assessment : listAssessment) {
      String expandedQueryId = getExpandedQueryId(assessment, response2eclassStr);
      String eclass = expandedQueryId + ":" + assessment.equivalent_class_str_in_file.get();

      // fill in eclass mentions
      TextMention KBMention =
          new TextMention(assessment.docid, assessment.spanOfFiller.getStart(),
              assessment.spanOfFiller.getEnd(),
              assessment.text);
      KBMention.setEntityId(eclass);
      if (!eclassStr2listMentions.containsKey(eclass)) {
        eclassStr2listMentions.put(eclass, new ArrayList<TextMention>());
      }
      eclassStr2listMentions.get(eclass).add(KBMention);

      assessment.eclass_str = Optional.of(eclass);
      response2eclassStr.put(assessment.responseId, eclass);
    }

    for (CSAssessment assessment : listAssessment) {
//			if(!assessment.parentId.equals("NIL")) continue; // only process hop-0 at the moment

      // loadCanonicalMention wrong and inexact answers
      if (setResponseIdWrong.contains(assessment.responseId)) {
        listAssessmentWrong.add(assessment);
      }
      if (setResponseIdInExact.contains(assessment.responseId)) {
        listAssessmentInExact.add(assessment);
      }

      // loadCanonicalMention correct and missed answers (eclass)
      //String equivalentClassID = Integer.toString(assessment.equivalent_class_id);
//			String equivalentClassID = assessment.equivalent_class_str;
      String equivalentClassID = assessment.eclass_str.get();

//			String equivalentClassID = assessment.queryId + ":" + assessment.slot.toString() + ":" + "0" + ":" + assessment.equivalent_class;
      EquivalentClass eclass = new EquivalentClass(equivalentClassID, assessment.hopId);
      System.out.println("eclass.id=" + eclass.id + ", eclass.hop=" + eclass.hop);
      for (EquivalentClass eclassInSet : setEquivalentClass) {
        if (eclassInSet.equals(eclass)) {
          assessment.setInSys(eclassInSet.inSys);
          eclassInSet.addAssessment(assessment);
          break;
        }
      }
    }
  }

  private static void loadSystemMentions(String fileSystemKB) throws IOException {
    BufferedReader reader = new BufferedReader(new FileReader(fileSystemKB));
    String sline;

    int uid = 0;
    while ((sline = reader.readLine()) != null) {
      String[] fields = sline.trim().split("\t");
      try {
        if (!fields[1].equals("canonical_mention")) {
          continue;
        }
      } catch (IndexOutOfBoundsException ex) {
        System.out.println("Unable to parse the following line:");
        System.out.println(sline.trim());
        continue;
      }

      TextMention canonicalMention =
          new TextMention(fields[3], Integer.parseInt(fields[4]), Integer.parseInt(fields[5]),
              fields[2].replace("\"", ""));

      canonicalMention.setEntityId(fields[0]);
      // for efficiency reason, skip the ones that are not in final results
      if (!queryToEntityInSysOut.values().contains(canonicalMention.getEntityId().get())) {
        continue;
      }

      if (!id2listCanonicalMention.containsKey(canonicalMention.getEntityId().get())) {
        id2listCanonicalMention.put(canonicalMention.getEntityId().get(), new ArrayList<TextMention>());
      }

      id2listCanonicalMention.get(canonicalMention.getEntityId().get()).add(canonicalMention);
    }
    reader.close();
  }

  public static void writeSystemMentionHTML(String fileVisualDir) throws IOException {
    Map<String, List<TextMention>> id2mentions = new HashMap<String, List<TextMention>>();
    id2mentions.putAll(eclassStr2listMentions);
    id2mentions.putAll(id2listCanonicalMention);

    for (String sysEntityId : id2mentions.keySet()) {
      String fileHTML = fileDirEntities + Util.normalizedEntityId(sysEntityId) + ".html";

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
      if (listMentions.size() > NUM_CANONICAL_MENTION_PER_SYS_ENTITY_SHOWN) {
        listMentions = listMentions.subList(0, NUM_CANONICAL_MENTION_PER_SYS_ENTITY_SHOWN);
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
