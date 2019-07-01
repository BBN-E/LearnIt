package com.bbn.akbc.common.format;

/**
 * Created by bmin on 9/22/15.
 */
public class Normalization {
  public static String convertIDtoOnlyHaveAsciiCharacter(String subjectString) {
    String subjectStringNoLeadingColon = subjectString.substring(1); // strip leading ":"
    // String resultString = subjectString.replaceAll("[^\\x00-\\x7F]", "");
//			String resultString = ":" + subjectStringNoLeadingColon.replaceAll("[^A-Za-z0-9_]", "_");
    String resultString = ":" + subjectStringNoLeadingColon.replaceAll("[^A-Za-z0-9_]", "");
    if (!subjectString.equals(resultString)) {
      System.out
          .println("convertIDtoOnlyHaveAsciiCharacter:\t" + subjectString + "\t" + resultString);
    }
    return resultString;
  }
}
