package com.bbn.akbc.utility;

/**
 * Created by bmin on 5/12/15.
 */

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileUtil {

  public static List<String> readLinesIntoList(File file) {
    return readLinesIntoList(file.getAbsolutePath());
  }

  public static List<String> readLinesIntoList(String file) {
    List<String> lines = new ArrayList<String>();

    int nLine = 0;

    try {
      BufferedReader reader = new BufferedReader(new FileReader(file));
      String sline;
      while ((sline = reader.readLine()) != null) {
//        if (nLine++ % 100000 == 0) {
//          System.out.println("# lines read: " + nLine);
//        }

        lines.add(sline);
      }
      reader.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return lines;
  }

  public static String readFileIntoString(String file, String encoding) {
    StringBuilder sb = new StringBuilder();

    try {
      FileInputStream fis = new FileInputStream(file);
      BufferedReader reader = new BufferedReader(new InputStreamReader(fis, encoding));
      int i;
      while ((i = reader.read()) != -1) {
        char ch = (char) i;
        sb.append(ch);
      }
      reader.close();

/*
                        FileInputStream fis = new FileInputStream(file);
			reader = new BufferedReader(new InputStreamReader(fis,"UTF-8"));
			String text = null;
			while ((text = reader.readLine()) != null)
			{
			 contents.append(text).append(System.getProperty("line.separator"));
			}
			s = contents.toString();
*/
    } catch (IOException e) {
      e.printStackTrace();
    }

    return sb.toString();
  }

  public static boolean isFileExist(String strFile) {
    File f = new File(strFile);
    return f.exists();
  }

  public static void writeToFile(String text, String fileName) {
    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter(new File(fileName)));
      writer.write(text);
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // example: delimilator="\n"
  public static void writeToFile(List<String> strings, String delimilator,
      String fileName) {
    StringBuilder sb = new StringBuilder();
    for (String str : strings) {
      sb.append(str + delimilator);
    }

    String text = sb.toString();
    text = text.substring(0, text.length() - delimilator.length());

    writeToFile(text, fileName);
  }
}
