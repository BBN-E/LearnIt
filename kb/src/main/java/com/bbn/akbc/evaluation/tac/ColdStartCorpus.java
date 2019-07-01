package com.bbn.akbc.evaluation.tac;

import com.bbn.akbc.common.FileUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by bmin on 5/4/16.
 */
public class ColdStartCorpus {

  static Map<String, String> docId2Path;
  static Map<String, String> docId2fullTextEscaped;

  static String encoding = "UTF-8";
  static int left_char_count = 100;
  static int right_char_count = 100;

  public static void loadDocId2PathMapping(String strSgmFilePath) {

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

  static String getFullTextEscaped(String strSgmPath) {
    String strText = FileUtil.readFileIntoString(strSgmPath, encoding);

    strText = strText.replace("<", "_").replace(">", "_").replace("/", "_").replace("\"", "_");

    return strText;
  }

  public static String getTextWithContext(String docId, int charStart, int charEnd) {
    EvalTextSpan evalTextSpan = new EvalTextSpan(docId, charStart, charEnd);
    return getTextWithContext(evalTextSpan);
  }

  public static String getTextWithContext(EvalTextSpan justification) {

    StringBuilder sb = new StringBuilder();
    String filePath = docId2Path.get(justification.docId);
    String filePathWindows = filePath.replace("/nfs/", "//").replace("/", "\\");

    if(!docId2fullTextEscaped.containsKey(justification.docId)) {
      docId2fullTextEscaped.put(justification.docId, getFullTextEscaped(filePath));
    }
    String text = docId2fullTextEscaped.get(justification.docId);

    int left = justification.charOffsetStart - left_char_count;
    int right = justification.charOffsetEnd + right_char_count;
    if (left <= 0) {
      left = 0;
    }
    if (right > text.length() - 1) {
      right = text.length() - 1;
    }

    for (int i = left; i < right; i++) {

      if (i == justification.charOffsetStart) {
        sb.append("_____");
      }

      sb.append(text.charAt(i));

      if (i == justification.charOffsetEnd) {
        sb.append("_____");
      }
    }

    return "Doc: " + filePathWindows + sb.toString();
  }


  public static String getFullTextWithContextHTML(EvalTextSpan justification) {

    StringBuilder sb = new StringBuilder();
    String filePath = docId2Path.get(justification.docId);
    String filePathWindows = filePath.replace("/nfs/", "//").replace("/", "\\");

    if(!docId2fullTextEscaped.containsKey(justification.docId)) {
      docId2fullTextEscaped.put(justification.docId, getFullTextEscaped(filePath));
    }
    String text = docId2fullTextEscaped.get(justification.docId);

    int left = 0;
    int right = text.length();
    if (left <= 0) {
      left = 0;
    }
    if (right > text.length() - 1) {
      right = text.length() - 1;
    }

    for (int i = left; i < right; i++) {

      if (i == justification.charOffsetStart) {
        sb.append("<font color=\"red\"><b>");
      }

      sb.append(text.charAt(i));

      if (i == justification.charOffsetEnd) {
        sb.append("</b></font>");
      }
    }

    return "Doc: " + filePathWindows + sb.toString();
  }

  public static void init(String strSgmFilePath) {
    docId2Path = new HashMap<String, String>();
    docId2fullTextEscaped = new HashMap<String, String>();
    loadDocId2PathMapping(strSgmFilePath);
  }
}
