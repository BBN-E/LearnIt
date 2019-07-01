package com.bbn.akbc.kb.facts;

import com.bbn.akbc.common.format.Normalization;

/**
 * Created by bmin on 9/21/15.
 */
public class FactEntityType {
  String srcID;
  String entityType;

  FactEntityType(String srcID, String entityType) {
    this.srcID = srcID;
    this.entityType = entityType;
  }

  FactEntityType fromLine(String sline) {
    String[] fields = sline.trim().split("\t");
    String predicate = fields[1];

    String srcId = fields[0];
    srcId = Normalization.convertIDtoOnlyHaveAsciiCharacter(srcId); // normalize for submission

    entityType = fields[2].toLowerCase();

    return new FactEntityType(srcID, entityType);
  }
}
