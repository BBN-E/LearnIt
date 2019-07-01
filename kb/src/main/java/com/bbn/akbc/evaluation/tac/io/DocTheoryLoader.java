package com.bbn.akbc.evaluation.tac.io;

import com.bbn.akbc.resource.Resources;
import com.bbn.serif.io.SerifXMLLoader;
import com.bbn.serif.theories.DocTheory;

import com.google.common.base.Optional;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DocTheoryLoader {

  static Map<String, DocTheory> docId2docTheory = new HashMap<String, DocTheory>();

  public static Optional<DocTheory> getDocTheory(String docid) {
    if (docId2docTheory.containsKey(docid)) {
      return Optional.of(docId2docTheory.get(docid));
    }

    String strPathSerifXml = Resources.getPathSerifXml() + docid;
    File fileSerifXml = new File(strPathSerifXml);
    if (!fileSerifXml.exists()) {
      strPathSerifXml = Resources.getPathSerifXml() + docid + ".xml";
      fileSerifXml = new File(strPathSerifXml);
      if (!fileSerifXml.exists()) {
        strPathSerifXml = Resources.getPathSerifXml() + docid + ".sgm.xml";
        fileSerifXml = new File(strPathSerifXml);
      }
      if (!fileSerifXml.exists()) {
        strPathSerifXml = Resources.getPathSerifXml() + docid + ".serifxml.xml";
        fileSerifXml = new File(strPathSerifXml);
      }
      if (!fileSerifXml.exists()) {
        strPathSerifXml = Resources.getPathSerifXml() + docid + ".mpdf.serifxml.xml";
        fileSerifXml = new File(strPathSerifXml);
      }
    }

    try {
      SerifXMLLoader fromXML = SerifXMLLoader.fromStandardACETypes();
      //				File fileSerifXml = new File(strPathSerifXml);
      if (!fileSerifXml.exists()) {
        System.err.println("File NOT found: " + strPathSerifXml);
        return Optional.absent();
      }
      DocTheory dt = fromXML.loadFrom(fileSerifXml);

      docId2docTheory.put(docid, dt);

      return Optional.of(dt);
    } catch (IOException e) {
      e.printStackTrace();
      //code to handle an IOException here
      System.err.println("Exception in reading: " + strPathSerifXml);
    }

    return Optional.absent();
  }

  public static Optional<DocTheory> getDocTheoryFromFile(String strPathSerifXml) {
    File fileSerifXml = new File(strPathSerifXml);

    try {
      // SerifXMLLoader fromXML = SerifXMLLoader.fromStandardACETypes();
      SerifXMLLoader fromXML = SerifXMLLoader.fromStandardACETypes(true);

      if (!fileSerifXml.exists()) {
        System.err.println("File NOT found: " + strPathSerifXml);
        return Optional.absent();
      }
      DocTheory dt = fromXML.loadFrom(fileSerifXml);

      return Optional.of(dt);
    } catch (IOException e) {
      e.printStackTrace();
      //code to handle an IOException here
      System.err.println("Exception in reading: " + strPathSerifXml);
    }

    return Optional.absent();
  }

  public static String getTextTagsEscaped(String docText) {
    return docText.replace("<", "_").replace(">", "_").replace("/", "_").replace("\\", "_");
  }

  public static Optional<DocTheory> readDocTheory(String strFileDocTheory) {
    try {
      SerifXMLLoader fromXML = SerifXMLLoader.fromStandardACETypes();
      DocTheory dt = fromXML.loadFrom(new File(strFileDocTheory));

      return Optional.of(dt);
    } catch (IOException e) {
      e.printStackTrace();
      //code to handle an IOException here
      System.err.println("Exception in reading: " + strFileDocTheory);
    }

    return Optional.absent();
  }
}
