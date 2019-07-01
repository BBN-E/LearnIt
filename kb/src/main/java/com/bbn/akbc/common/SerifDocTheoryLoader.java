package com.bbn.akbc.common;

import com.bbn.serif.io.SerifXMLLoader;
import com.bbn.serif.theories.DocTheory;

import com.google.common.base.Optional;

import java.io.File;
import java.io.IOException;


public class SerifDocTheoryLoader {

  public static Optional<DocTheory> getDocTheoryFromFile(String strPathSerifXml) {
    return getDocTheoryFromFile(strPathSerifXml, false);
  }
    public static Optional<DocTheory> getDocTheoryFromFile(String strPathSerifXml, boolean allowSloppyOffsets) {
    File fileSerifXml = new File(strPathSerifXml);

    try {
      SerifXMLLoader fromXML;
      if(allowSloppyOffsets)
        fromXML = SerifXMLLoader.builderFromStandardACETypes().allowSloppyOffsets().build();
      else
        fromXML = SerifXMLLoader.builderFromStandardACETypes().makeAllTypesDynamic().build();


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
}
