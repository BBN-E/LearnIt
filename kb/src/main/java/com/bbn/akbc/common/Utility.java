package com.bbn.akbc.common;

public class Utility {
  public static String normalizeName(String strName) {
    return strName.replace("&apos;", "'").replace("&amp;", "&");
  }

  public static boolean doesRemoveMpdfSuffix = true;
  public static String removeMpdfSuffix(String docid) {
    if(doesRemoveMpdfSuffix) {
      if(docid.endsWith(".mpdf"))
        return docid.substring(0, docid.lastIndexOf(".mpdf"));
    }
    return docid;
  }
}
