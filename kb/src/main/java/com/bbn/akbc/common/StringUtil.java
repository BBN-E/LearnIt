package com.bbn.akbc.common;

import java.text.DecimalFormat;

public class StringUtil {

  public static boolean isNumeric(String str) {
    try {
      double d = Double.parseDouble(str);
    } catch (NumberFormatException nfe) {
      return false;
    }
    return true;
  }

  public static String formatDouble(double d) {
    DecimalFormat df = new DecimalFormat("#.###");
    return df.format(d);
  }

  public static String normalize(String str) {
    return str.replace("\t", " ").replace("\r", " ").replace("\n", " ");
  }
}
