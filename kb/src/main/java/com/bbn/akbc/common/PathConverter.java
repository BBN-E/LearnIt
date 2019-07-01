package com.bbn.akbc.common;

public class PathConverter {

  public static String convertToWindows(String strURL) {
    if (!strURL.startsWith("/nfs/") && !strURL.contains("/")) {
      return strURL;
    } else {
      return "\\\\" + strURL.substring(5).replace("/", "\\");
    }
  }
}
