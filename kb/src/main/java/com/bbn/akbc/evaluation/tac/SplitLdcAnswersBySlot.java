package com.bbn.akbc.evaluation.tac;

import com.bbn.akbc.common.FileUtil;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Created by bmin on 7/24/15.
 */
public class SplitLdcAnswersBySlot {
  static Multimap<String, String> slot2lines = HashMultimap.create();

  public static void main(String [] argv) throws IOException {
    String strFileLdcAnswers = argv[0].trim();

    List<String> lines = FileUtil.readLinesIntoList(strFileLdcAnswers);
    for(String line : lines) {
      line = line.trim();
      if(line.isEmpty()) continue;

      String [] items = line.split("\t");

      String query = items[0];
      String slot = items[1];

      /*
      // try on both hop0 and hop1
      slot2lines.put(slot, line);
      //
      */

      // only consider hop-0
      if(query.split("_").length==3) {
        slot2lines.put(slot, line);
      }
    }

    // write to seperate files
    for(String slot : slot2lines.keySet()) {
      BufferedWriter bw = new BufferedWriter(new FileWriter(new File(strFileLdcAnswers
          + "." + slot.replace(":", "_") + ".answers")));
      for(String line : slot2lines.get(slot))
        bw.write(line + "\n");
      bw.close();
    }
  }
}
