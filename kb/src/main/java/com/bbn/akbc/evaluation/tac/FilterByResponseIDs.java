package com.bbn.akbc.evaluation.tac;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FilterByResponseIDs {

  static Set<String> responseIdInEval;

  public static void main(String[] argv) throws IOException {
    String strFileAllCSV = argv[0];
    String strFileRespnoseIdInEval = argv[1];

    String strFileCSVshrinked = argv[2];

    boolean isDuplicateLinesForMultipleSourceOfError = argv[3].equals("true");

    List<String> linesInAllCSV = readLinesIntoList(strFileAllCSV);
    List<String> linesAllResponseIdInEval = readLinesIntoList(strFileRespnoseIdInEval);

    responseIdInEval = new HashSet<String>();
    for (String responseId : linesAllResponseIdInEval) {
      responseIdInEval.add(responseId);
    }

    BufferedWriter writer = new BufferedWriter(new FileWriter(new File(strFileCSVshrinked)));
    for (int i = 1; i < linesInAllCSV.size(); i++) {
      // System.out.println("-> " + linesInAllCSV.get(i));
      if (!linesInAllCSV.get(i).startsWith("0_")) {
        continue;
      }

      String[] items = linesInAllCSV.get(i).split(",");
      String responseID = items[0].trim();

      if (responseIdInEval.contains(responseID)) {
        if (!isDuplicateLinesForMultipleSourceOfError) {
          writer.write(responseID + "," + items[1] + "\n");
        } else {
          for (int idx = 1; idx <= 4; idx++) {
            if (!items[idx].trim().isEmpty()) {
              writer.write(responseID + "," + items[idx] + "\n");
            }
          }
        }
      }
    }
    writer.close();
  }

  public static List<String> readLinesIntoList(String file) {
    List<String> lines = new ArrayList<String>();

    int nLine = 0;

    try {
      BufferedReader reader = new BufferedReader(new FileReader(file));
      String sline;
      while ((sline = reader.readLine()) != null) {
        if (nLine++ % 100000 == 0) {
          System.out.println("# lines read: " + nLine);
        }

        lines.add(sline);
      }
      reader.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return lines;
  }
}
