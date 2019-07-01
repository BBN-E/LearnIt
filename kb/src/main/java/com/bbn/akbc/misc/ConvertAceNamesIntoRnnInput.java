package com.bbn.akbc.misc;

import com.bbn.akbc.common.FileUtil;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Created by bmin on 10/19/16.
 */
public class ConvertAceNamesIntoRnnInput {
  public static String converOneLine(String sline) {
    StringBuilder sb = new StringBuilder();

    // ((DAVAO GPE-ST) (, NONE-ST) (Philippines GPE-ST) (, NONE-ST) (March NONE-ST) (4 NONE-ST) )
    sline = sline.trim().substring(1, sline.length()-1);
    for(String item : sline.split("\\)")) {
      if(!item.contains("("))
        continue;

      String pairOfString = item.substring(item.indexOf("(")+1).trim();

      sb.append(pairOfString + "\n");
    }

    return sb.toString();
  }

  public static void main(String [] argv) throws IOException {
    String inputFile = argv[0];
    String outputFile = inputFile + ".converted";

    PrintWriter printWriter = new PrintWriter(new File(outputFile));

    List<String> lines = FileUtil.readLinesIntoList(inputFile);
    for(String line : lines) {
      if(line.trim().isEmpty())
        continue;

      String stringOfLines = converOneLine(line);
      printWriter.print(stringOfLines);

      printWriter.println();
    }

    printWriter.close();
  }
}
