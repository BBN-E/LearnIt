package com.bbn.akbc.evaluation.tac;

import com.bbn.akbc.kb.CsSubmissionKB;
import com.bbn.akbc.kb.text.TextEntity;
import com.bbn.akbc.kb.text.TextMention;
import com.bbn.akbc.common.Justification;
import com.bbn.akbc.io.SimpleHtmlOutputWriter;
import com.bbn.akbc.common.FileUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VisualizeSubmissionFile {

  static int left_char_count = 200;
  static int right_char_count = 200;

  static int MAX_RELATIONS_TO_VIS_PER_RELN = 1000;
  static int MAX_MENTIONS_TO_VIS_PER_ENTITY = 100;

  static String color = "red";
  static String encoding = "UTF-8";

  static String strSgmFilePath;
  static Map<String, String> docId2Path;

//  static Map<String, TextEntity> id2entities = new HashMap<String, TextEntity>();
//  static List<RelationFromSubmission> relations = new ArrayList<RelationFromSubmission>();
  static CsSubmissionKB csSubmissionKB = new CsSubmissionKB();

  static Map<Justification, String> cacheJustificationToText;

  static Set<String> setRelnTypes = new HashSet<String>();


  public static void loadDocId2PathMapping() {
    docId2Path = new HashMap<String, String>();
    List<String> listPathSgms = FileUtil.readLinesIntoList(strSgmFilePath);
    for (String pathSgm : listPathSgms) {
      String docId = pathSgm.substring(pathSgm.lastIndexOf("/") + 1);
      if (docId.endsWith(".mpdf.xml")) {
        docId = docId.replace(".mpdf.xml", "");

      } else if (docId.endsWith(".sgm.xml")) {
        docId = docId.replace(".sgm.xml", "");
      } else if (docId.endsWith(".sgm")) {
        docId = docId.replace(".sgm", "");
      } else if (docId.endsWith(".xml")) {
        docId = docId.replace(".xml", "");
      } else if (docId.endsWith(".txt")) {
        docId = docId.replace(".txt", "");
      } else {
        System.err.println("error in docId: " + docId);
      }

      docId2Path.put(docId, pathSgm);
    }
  }

  public static String getTextContentEscaped(String strSgmPath) {
    String strText = FileUtil.readFileIntoString(strSgmPath, encoding);

    strText = strText.replace("<", "_").replace(">", "_").replace("/", "_").replace("\"", "_");

    return strText;

  }


  public static String getJustificationText(Justification justification) {

    StringBuilder sb = new StringBuilder();

    System.out.println("justification.docId=" + justification.docId);
    String filePath = docId2Path.get(justification.docId);
    System.out.println("filePath=" + filePath);
    String filePathWindows = filePath.replace("/nfs/", "//").replace("/", "\\");

    String text = getTextContentEscaped(filePath);

//		if(justification.docId.startsWith("2014"))
//			return "NA:" + justification.docId;

    int left = justification.span.getFirst() - left_char_count;
    int right = justification.span.getSecond() + right_char_count;
    if (left <= 0) {
      left = 0;
    }
    if (right > text.length() - 1) {
      right = text.length() - 1;
    }

    for (int i = left; i < right; i++) {

      if (i == justification.span.getFirst()) {
        sb.append("<span style=\"background-color: #FFFF00\">");
      }

      sb.append(text.charAt(i));

      if (i == justification.span.getSecond()) {
        sb.append("</span>");
      }
    }

//		return justification.docId + "(" + justification.span.getFirst() + ", " + justification.span.getSecond() + ")" + sb.toString();
    return "<a href=\"" + filePathWindows + "\">" + justification.docId + "</a>" +
        "(" + justification.span.getFirst() + ", " + justification.span.getSecond() + ")" + sb
        .toString();
  }


  public static String getMentionText(TextMention mention) {

    StringBuilder sb = new StringBuilder();

    String filePath = docId2Path.get(mention.getDocId());
    String filePathWindows = filePath.replace("/nfs/", "//").replace("/", "\\");

    String text = getTextContentEscaped(filePath);

//		if(mention.docId.startsWith("2014"))
//			return "NA:" + mention.docId;

    int left = mention.getSpan().getStart() - left_char_count;
    int right = mention.getSpan().getEnd() + right_char_count;
    if (left <= 0) {
      left = 0;
    }
    if (right > text.length() - 1) {
      right = text.length() - 1;
    }

    for (int i = left; i < right; i++) {

      if (i == mention.getSpan().getStart()) {
        sb.append("<span style=\"background-color: #FFFF00\">");
      }

      sb.append(text.charAt(i));

      if (i == mention.getSpan().getEnd()) {
        sb.append("</span>");
      }
    }

//		return mention.docId + "(" + mention.span.getStart() + ", " + mention.span.getEnd() + ")" + sb.toString();
    return "<a href=\"" + filePathWindows + "\">" + mention.getDocId() + "</a>" +
        "(" + mention.getSpan().getStart() + ", " + mention.getSpan().getEnd() + ")" + sb.toString();
  }

  public static String normalizeFileName(String str) {
    return str.replace("/", "_").replace("\\", "_").replace(":", "_").replace("#", "_");
  }

  public static void writeHTMLForRelations(String relnHtmlDir, String entityHtmlDir,
      String strRelnType) throws IOException {

    entityHtmlDir = entityHtmlDir.replace("/nfs/", "//").replace("/", "\\"); // for visualization

    SimpleHtmlOutputWriter writer = new SimpleHtmlOutputWriter(
        new File(relnHtmlDir + normalizeFileName(strRelnType) + ".html"));
    writer.start();

    Collections.shuffle(csSubmissionKB.getRelations());

    int num_visualized_for_this_relation = 0;
    for (int i = 0; i < csSubmissionKB.getRelations().size(); i++) {
      RelationFromSubmission relation = csSubmissionKB.getRelations().get(i);

      if (!relation.slot.toString().equals(strRelnType)) {
        continue;
      }

      num_visualized_for_this_relation++;
      if (num_visualized_for_this_relation > MAX_RELATIONS_TO_VIS_PER_RELN) {
        break;
      }

      String hasMultipleJustification = relation.justifications.size() > 1 ? "Multi-J" : "";

//			if(relation.justifications.size()>1) {

      writer.startCollapsibleSection(
          hasMultipleJustification + " " + relation.slot.toString() + "(" + relation.srcId + ", "
              + relation.dstId + ")", false);
      String strUrlSrcId = entityHtmlDir + normalizeFileName(relation.srcId) + ".html";
      String strUrlDstId = entityHtmlDir + normalizeFileName(relation.dstId) + ".html";

      writer.writeHtmlContent(
          relation.slot.toString() + "(" + "<a href=\"" + strUrlSrcId + "\">" + relation.srcId
              + "</a>" + ", " + "<a href=\"" + strUrlDstId + "\">" + relation.dstId + "</a>" + ")"
              + "<br><br>");
      for (Justification justification : relation.justifications) {
        String justificationText = getJustificationText(justification);
        writer.writeHtmlContent(justificationText + "<br>");
      }
      writer.endCollapsibleSection();
//			}
    }

    writer.close();
  }

  private static void writeHTMLForEntities(String strDirEntities,
      TextEntity entity) throws IOException, FileNotFoundException {

    SimpleHtmlOutputWriter writer = new SimpleHtmlOutputWriter(
        new File(strDirEntities + normalizeFileName(entity.getId()) + ".html"));
    writer.start();

    List<TextMention> mentions = new ArrayList<TextMention>();
    mentions.addAll(entity.getMentions());

    Collections.shuffle(mentions);
    int max_to_visualize = mentions.size() < MAX_MENTIONS_TO_VIS_PER_ENTITY ? mentions.size()
                                                                            : MAX_MENTIONS_TO_VIS_PER_ENTITY;
    mentions = mentions.subList(0, max_to_visualize);

    writer.startCollapsibleSection(entity.getId(), true);

    for (int i = 0; i < mentions.size(); i++) {
      TextMention m = mentions.get(i);

      writer.writeHtmlContent(getMentionText(m) + "<br><br>");

//			}
    }

    writer.endCollapsibleSection();

    writer.close();
  }

  public static void main(String[] argv) throws IOException {
//		String strFileKB = "/nfs/mercury-04/u42/bmin/Git_repo/repo/kbp/coldstart/experiments/expts/2013_2013_inference_1_with-mc-patterns/output/all.kb.valid.submission";
//		String strFileKB = "/nfs/mercury-04/u42/bmin/runjobs2/expts/experiments/2013_2013_best/output/results.with_redudancy_filter/all.kb.valid.submission";
    // String strFileKB = "/nfs/mercury-04/u42/bmin/runjobs2/expts/experiments/2013_2013_e2e_w_awake_conservative/output/all.kb.valid.submission.infered";

    String strFileKB = argv[0];
    strSgmFilePath = argv[1];

    // create directories
    String strDirRelations = strFileKB + ".reln/";
    String strDirEntities = strFileKB + ".entities/";
    File f = new File(strDirRelations);
    if (!f.exists()) {
      f.mkdir();
    }
    f = new File(strDirEntities);
    if (!f.exists()) {
      f.mkdir();
    }
/*
                // set necessary paths
		String evaluation = "2013CS";
		if(evaluation.equals("2013SF") || evaluation.equals("2013SF_QUERY"))
			strSgmFilePath = "/nfs/mercury-04/u10/KBP_ColdStart/SF_resources/2013/processed/mini_corpus/all_2013_sf_query_and_assessment_docs_full_paths.linux.txt";
		else if(evaluation.equals("2013CS") || evaluation.equals("2013CS_QUERY"))
			strSgmFilePath = "/nfs/mercury-04/u10/KBP_ColdStart/2013/processed/corpus/mini_corpus/doclist_originalxml.linux.txt";
		else if(evaluation.equals("2012SF") || evaluation.equals("2012SF_QUERY"))
			strSgmFilePath = "/nfs/mercury-04/u10/KBP_ColdStart/SF_resources/2012/processed/mini_corpus/all_docs_2012_sf_linux.txt";
		else {
			System.err.println("Error in evaluation: " + evaluation);
			System.exit(-1);
		}
		*/

    loadDocId2PathMapping();

    csSubmissionKB.loadFromFile(strFileKB);

    // see what are the relations found
    for(RelationFromSubmission relationFromSubmission : csSubmissionKB.getRelations()) {
      setRelnTypes.add(relationFromSubmission.slot.toString());
    }

    for (String relnType : setRelnTypes) {
      writeHTMLForRelations(strDirRelations, strDirEntities, relnType);
    }

    for (TextEntity entity : csSubmissionKB.getId2entities().values()) {
      try {
        writeHTMLForEntities(strDirEntities, entity);
      } catch (FileNotFoundException e){
        e.printStackTrace();
      }
    }

  }


}
