package com.bbn.akbc.evaluation.tac;

import com.bbn.akbc.evaluation.tac.io.AssessmentReader;
import com.bbn.akbc.common.FileUtil;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by bmin on 11/8/15.
 */
public class SystemResponseLoader {
  public Multimap<String, RelationAfterApplyingQueryToSystemKB> queryId2systemOutRelationLines;
  public Map<RelationAfterApplyingQueryToSystemKB, CSAssessment> relationSubmission2accessment;

  public void initFromFile(String systemCorrecteTabRun) {

    // read results in a system submission
    List<String> linesInSysOut = FileUtil.readLinesIntoList(systemCorrecteTabRun);
    queryId2systemOutRelationLines = HashMultimap.create();
    for(String line : linesInSysOut) {
      RelationAfterApplyingQueryToSystemKB systemOutRelationLine = RelationAfterApplyingQueryToSystemKB
          .fromLine(line);
      queryId2systemOutRelationLines.put(systemOutRelationLine.queryId, systemOutRelationLine);

      System.out.println("initFromFile: " + systemOutRelationLine.queryId + "\t" + systemOutRelationLine.toString());
    }
  }

  public void alignToAccessment() {
    relationSubmission2accessment = new HashMap<RelationAfterApplyingQueryToSystemKB, CSAssessment>();

    for(String queryId : queryId2systemOutRelationLines.keySet()) {
      for(RelationAfterApplyingQueryToSystemKB relationAfterApplyingQueryToSystemKB : queryId2systemOutRelationLines.get(queryId)) {
        System.out.println();

        if(!AssessmentReader.query2assessments.containsKey(queryId))
          System.out.println("No aligned relation for " + relationAfterApplyingQueryToSystemKB.toString());
        else {
          for(CSAssessment csAssessment : AssessmentReader.query2assessments.get(queryId)) {
            System.out.println("[1] candidate1: " + relationAfterApplyingQueryToSystemKB.toString() +
                "\t" +
                "candidate2: " + csAssessment.toString());

            if(relationAfterApplyingQueryToSystemKB.slot.equals(csAssessment.slot.toString())) {
              System.out.println("[2] candidate1: " + relationAfterApplyingQueryToSystemKB.toString() +
                  "\t" +
                  "candidate2: " + csAssessment.toString());


              for(EvalTextSpan textSpan : relationAfterApplyingQueryToSystemKB.fillerProvenances) {

                System.out.println("[3] candidate1: " + relationAfterApplyingQueryToSystemKB.toString() +
                    "\t" +
                    "candidate2: " + csAssessment.toString());


                if(csAssessment.docid.equals(textSpan.docId)) {
                  System.out.println("[4] candidate1: " + relationAfterApplyingQueryToSystemKB.toString() +
                      "\t" +
                      "candidate2: " + csAssessment.toString());

                  if(csAssessment.spanOfFiller.getStart() == textSpan.charOffsetStart &&
                      csAssessment.spanOfFiller.getEnd() == textSpan.charOffsetEnd) {
                    relationSubmission2accessment.put(relationAfterApplyingQueryToSystemKB,
                        csAssessment);

                    System.out.println(
                        "alignToAssessment: " + relationAfterApplyingQueryToSystemKB.toString()
                            + " -> " +
                            csAssessment.toString());

                    break;
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
