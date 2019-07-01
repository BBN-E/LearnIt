package com.bbn.akbc.evaluation.tac;

import com.bbn.akbc.common.FileUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConvertCS2014ListToPath {

  static Map<String, String> docId2path;

  static void loadMapping() {
    docId2path = new HashMap<String, String>();

    List<String> lines =
        FileUtil.readLinesIntoList("/nfs/mercury-04/u10/KBP_ColdStart/tac_2013_full_corpus");
    for (String line : lines) {
      line = line.trim();
      if (line.isEmpty()) {
        continue;
      }

      String docId = line.substring(line.lastIndexOf("/") + 1, line.lastIndexOf("."));
      docId2path.put(docId, line);
    }
  }

  static void loadCS2014List() {
    List<String> lines = FileUtil.readLinesIntoList(
        "/nfs/mercury-04/u42/bmin/projects/coldstart2014/official_submission_cs_2014/corpus/ldc_release/LDC2014R42_TAC_2014_KBP_English_Cold_Start_Evaluation_Source_Corpus/data/tac_2014_kbp_cold_start_evaluation_document_collection.lst");

    List<String> paths = new ArrayList<String>();

    for (String line : lines) {
      line = line.trim();
      if (line.isEmpty()) {
        continue;
      }

      String docId = line;
      String path = docId2path.get(docId);

      paths.add(docId + "\t" + path);
    }

    Collections.shuffle(paths);

    for (String path : paths) {
      System.out.println(path);
    }

  }

  public static void main(String[] argv) {
    loadMapping();

    loadCS2014List();

  }
}
