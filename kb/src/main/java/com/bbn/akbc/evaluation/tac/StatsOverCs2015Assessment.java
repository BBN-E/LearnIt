package com.bbn.akbc.evaluation.tac;

import com.bbn.akbc.kb.text.TextSpan;
import com.bbn.akbc.common.FileUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bmin on 11/6/15.
 */
public class StatsOverCs2015Assessment {
  public static void main(String [] argv) {
    String strFileCs2015assessment = argv[0];
    List<String> lines = FileUtil.readLinesIntoList(strFileCs2015assessment);
    for(String line : lines) {
      line = line.trim();

      String [] fields = line.split("\t");

      String responseId = fields[0];
      String queryIdAndSlot = fields[1];
      String provenanceOfRelation = fields[2];
      String slotFiller = fields[3];
      String provenanceOfFiller = fields[4];
      String assessmentOfFiller = fields[5];
      String assessmentOfRelationProvenance = fields[6];
      String ldcEquivalantClassOfFiller = fields[7];

      List<TextSpan> textSpanRelationProvenance = new ArrayList<TextSpan>();
      for(String strTextSpanRelationProvenance : provenanceOfRelation.trim().split(",")) {
        if(strTextSpanRelationProvenance.trim().isEmpty())
          textSpanRelationProvenance.add(TextSpan.fromLine(strTextSpanRelationProvenance.trim()));
      }

      List<TextSpan> textSpanFillerProvenance = new ArrayList<TextSpan>();
      for(String strTextSpanFillerProvenance : provenanceOfFiller.trim().split(",")) {
        if(strTextSpanFillerProvenance.trim().isEmpty())
          textSpanFillerProvenance.add(TextSpan.fromLine(strTextSpanFillerProvenance.trim()));
      }

      String queryId = queryIdAndSlot.substring(0, queryIdAndSlot.indexOf(":"));
      String slot = queryIdAndSlot.substring(queryIdAndSlot.indexOf(":") + 1);

      int responseIdx = Integer.parseInt(responseId.substring(responseId.lastIndexOf("_")+1));
      responseId = responseId.substring(0, responseId.lastIndexOf("_"));
      int hopId = Integer.parseInt(responseId.substring(responseId.lastIndexOf("_")+1));
      String queryIdInResponseId = responseId.substring(0, responseId.lastIndexOf("_"));

      String queryIdInEquivalentClass = "NA";
      String equivalentClassId = "-1";
      if(ldcEquivalantClassOfFiller.contains(":")) {
        queryIdInEquivalentClass =
            ldcEquivalantClassOfFiller.substring(0, ldcEquivalantClassOfFiller.indexOf(
                ":"));
        equivalentClassId = ldcEquivalantClassOfFiller.substring(ldcEquivalantClassOfFiller.indexOf(":") + 1);
      }
      else
        equivalentClassId = ldcEquivalantClassOfFiller;

      System.out.println(queryIdInResponseId + "\t"
              + hopId + "\t"
              + responseIdx + "\t"
              + queryId + "\t"
              + slot + "\t"
              + assessmentOfFiller + "\t"
              + assessmentOfRelationProvenance + "\t"
              + queryIdInEquivalentClass +"\t"
              + equivalentClassId
      );
    }
  }
}
