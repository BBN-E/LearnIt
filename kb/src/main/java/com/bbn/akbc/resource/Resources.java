package com.bbn.akbc.resource;

import com.bbn.serif.io.SerifXMLLoader;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.SentenceTheory;

import java.io.File;
import java.io.IOException;

public class Resources {

  static String strPathSerifXml;
  static String dirSerifXmlVisualization;
  static String evalYear;

  Resources() {
  }

  public static void setPathSerifXml(String strPath) {
    strPathSerifXml = strPath;
  }

  public static String getPathSerifXml() {
    return strPathSerifXml;
  }

  public static void setDirSerifXmlVisualization(String strPath) {
    dirSerifXmlVisualization = strPath;
  }

  public static String getDirSerifXmlVisualization() {
    return dirSerifXmlVisualization;
  }

  public static void setEvalYear(String year) {
    evalYear = year;
  }

  public static String getEvalYear() {
    return evalYear;
  }

  public static String getPathSerifXmlVisualization(String docid) {
    if (Resources.getEvalYear().equals("2013")) {
      String pathSerifHTML = Resources.getDirSerifXmlVisualization() +
          docid + "\\index.html";
      return pathSerifHTML;
    } else if (Resources.getEvalYear().equals("2012")) {
      String pathSerifHTML = Resources.getDirSerifXmlVisualization() +
          docid.substring(docid.lastIndexOf("_") + 1, docid.lastIndexOf("_") + 3) + "\\" + docid
          + "\\index.html";
      return pathSerifHTML;
    } else if (Resources.getEvalYear().endsWith("SF")) {
      String pathSerifHTML = Resources.getDirSerifXmlVisualization() +
          docid + "\\index.html";
      return pathSerifHTML;
    } else if (Resources.getEvalYear().equals("2014CS")) {
      String pathSerifHTML = Resources.getDirSerifXmlVisualization() +
          docid + ".sgm" + "\\index.html";
      return pathSerifHTML;
    } else if (Resources.getEvalYear().equals("2015CS")) {
      String pathSerifHTML = Resources.getDirSerifXmlVisualization() +
          docid + ".sgm" + "\\index.html";
      return pathSerifHTML;
    }
    else {
      System.err.println("Wrong evaluation year: " + Resources.getEvalYear());
      System.exit(-1);
    }

    return null;
  }


  public static DocTheory getDocTheory(String docid) {
    String strPathSerifXml = Resources.getPathSerifXml() + docid + ".xml";
    File f = new File(strPathSerifXml);
    if (!f.exists()) {
      strPathSerifXml = Resources.getPathSerifXml() + docid + ".sgm.xml";
      f = new File(strPathSerifXml);
    }
    try {
      SerifXMLLoader fromXML = SerifXMLLoader.fromStandardACETypes();
      DocTheory dt = fromXML.loadFrom(f);
      return dt;
    } catch (IOException e) {
      e.printStackTrace();
      //code to handle an IOException here
      System.out.println("Exception in reading: " + strPathSerifXml);
      return null;
    }
  }

  public static SentenceTheory getSentence(DocTheory dt, int beg, int end) {
    for (SentenceTheory sentTheory : dt.sentenceTheories()) {
      if (sentTheory.span().size() <= 0) {
        continue;
      }
      int sentStart = sentTheory.span().startToken().startCharOffset().value();
      int sentEnd = sentTheory.span().endToken().endCharOffset().value();

      if (sentStart <= beg && sentEnd >= end) {

        return sentTheory;
      }
    }

    return null;
  }
}
