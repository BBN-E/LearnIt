package com.bbn.akbc.evaluation.tac;

import com.bbn.akbc.evaluation.tac.io.AssessmentReader;
import com.bbn.akbc.evaluation.tac.io.SystemKbMentionLoader;
import com.bbn.akbc.evaluation.tac.io.SystemKbRelationLoader;
import com.bbn.akbc.kb.text.TextSpan;
import com.bbn.akbc.resource.Resources;
import com.bbn.akbc.common.EntityVisualizationWriter;
import com.bbn.akbc.common.Util;

import com.google.common.base.Optional;
import com.google.common.collect.Multimap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VisualizationHTML2015CS {
  static List<CSAssessment> correctAssessments = new ArrayList<CSAssessment>();
  static List<CSAssessment> incorrectAssessments = new ArrayList<CSAssessment>();
  static List<CSAssessment> missedAssessments = new ArrayList<CSAssessment>();

  static Set<String> setSlotPlusEquivalentClassIDinCorrect = new HashSet<String>();

  static Optional<String> getSlotPlusEquivalentClassID(CSAssessment csAssessment) {
    if(csAssessment.getEquivalent_class_str_in_file().isPresent())
      return Optional.of(csAssessment.slot.toString() + "-" + csAssessment.getEquivalent_class_str_in_file().get());
    else
      return Optional.absent();
  }

  public static void main(String[] argv) throws IOException {
    String resolveQueriesLog = argv[0];
    String fileSystemSubmissionTab = argv[1];
    String fileSystemKB = argv[2];
    String fileAssessment = argv[3];
    String fileQuery = argv[4];
    String fileVisualDir = argv[5];

    String dirSerifXml = argv[6]; // "/nfs/mercury-04/u10/KBP_ColdStart/2013/processed/scripts/mini_corpus_CS2013/output.parallel.win/output/"
    String dirSerifXmlVisualization = argv[7];
    String evalYear = argv[8];

    Resources.setPathSerifXml(dirSerifXml);
    Resources.setDirSerifXmlVisualization(dirSerifXmlVisualization);
    Resources.setEvalYear(evalYear);

    System.out.println("=== ResolveQueryLogLoader2015");
    ResolveQueryLogLoader2015.init(resolveQueriesLog);

//    Map<String, String> responseID2judgement = ScorerTraceLoader.loadCanonicalMention(scoreTraceLog);
    System.out.println("=== AssessmentReader.readCSAssessmentsFromFileCS2015");
    AssessmentReader.initForCS2015(fileAssessment);

    System.out.println("=== systemResponseLoaderRun1.initFromFile");
    System.out.println("=== systemResponseLoaderRun1.alignToAccessment");
    SystemResponseLoader systemResponseLoaderRun1 = new SystemResponseLoader();
    systemResponseLoaderRun1.initFromFile(fileSystemSubmissionTab);
    systemResponseLoaderRun1.alignToAccessment();



    System.out.println("=== QueryLoader.loadCanonicalMention");
    Map<String, CSInitQuery> id2queryEntities = QueryLoader.load(fileQuery);
    for(String queryId : id2queryEntities.keySet()) {
      System.out.println("id2queryEntities.put" + "\t" + "queryId=" + queryId + "\t" + id2queryEntities.get(queryId).toString().replace("\n", "|"));
    }

    System.out.println("=== SystemKbMentionLoader.loadCanonicalMention");
    SystemKbMentionLoader.loadCanonicalMention(fileSystemKB,
        ResolveQueryLogLoader2015.hop0QueryToEntityInSysOut);

    System.out.println("=== SystemKbRelationLoader.loadCanonicalMention");
    SystemKbRelationLoader.load(fileSystemKB);
    for(String docId : SystemKbRelationLoader.doc2relations.keySet()) {
      for(RelationFromSubmission relationFromSubmission : SystemKbRelationLoader.doc2relations.get(docId))
        System.out.println("SystemKbRelationLoader.doc2relations\t" + docId + "\t" + relationFromSubmission.toString());
    }

    // make subdirectories to hold entities
    String fileDirEntities = Util.make_sub_directories_for_entities(fileVisualDir);
    String fileRelationHTMLCorrect = fileVisualDir + "/relationCorrect.html";
    String fileRelationHTMLIncorrect = fileVisualDir + "/relationIncorrect.html";
    String fileRelationHTMLMissed = fileVisualDir + "/relationMissed.html";

    divideAccessmentsByJudgement(AssessmentReader.query2assessments.values(), systemResponseLoaderRun1.queryId2systemOutRelationLines);

    System.out.println("num missed: " + missedAssessments.size());
    System.out.println("num correct: " + correctAssessments.size());
    System.out.println("num incorrect: " + incorrectAssessments.size());

    /*
    System.out.println("writing file: " + fileRelationHTMLCorrect);
    RelationVisualizationWriter
        .write(fileRelationHTMLCorrect, fileVisualDir, correctAssessments, "C",
            ResolveQueryLogLoader2015.hop0QueryToEntityInSysOut,
            SystemKbMentionLoader.hop0queryId2listCanonicalMention,
            id2queryEntities,
            ResolveQueryLogLoader2015.hop1Query2Mention);

    System.out.println("writing file: " + fileRelationHTMLIncorrect);
    RelationVisualizationWriter
        .write(fileRelationHTMLIncorrect, fileVisualDir, incorrectAssessments, "W",
            ResolveQueryLogLoader2015.hop0QueryToEntityInSysOut,
            SystemKbMentionLoader.hop0queryId2listCanonicalMention,
            id2queryEntities,
            ResolveQueryLogLoader2015.hop1Query2Mention);

    System.out.println("writing file: " + fileRelationHTMLMissed);
    RelationVisualizationWriter
        .write(fileRelationHTMLMissed, fileVisualDir, missedAssessments, "M",
            ResolveQueryLogLoader2015.hop0QueryToEntityInSysOut,
            SystemKbMentionLoader.hop0queryId2listCanonicalMention,
            id2queryEntities,
            ResolveQueryLogLoader2015.hop1Query2Mention);
*/
    // write HTMLs for entities
    System.out.println("=== EntityVisualizationWriter.write");
    EntityVisualizationWriter.write(fileDirEntities, SystemKbMentionLoader.hop0queryId2listCanonicalMention);
  }

  static boolean isFoundInSubmission(CSAssessment csAssessment,
      Multimap<String, RelationAfterApplyingQueryToSystemKB> queryId2systemOutRelationLines) {

    if (!queryId2systemOutRelationLines.containsKey(csAssessment.queryId)) {
      System.out.println("queryId not in submission set: " + csAssessment.queryId);
      return false;
    }
    else {
      for(RelationAfterApplyingQueryToSystemKB systemOutRelationLine : queryId2systemOutRelationLines.get(csAssessment.queryId)) {
        if(systemOutRelationLine.slot.equals(csAssessment.slot.toString())) {
          for(EvalTextSpan spanSys : systemOutRelationLine.fillerProvenances) {
            TextSpan spanAssessment = csAssessment.spanOfFiller;
            if(spanSys.docId.equals(csAssessment.docid) &&
                spanSys.charOffsetStart==csAssessment.spanOfFiller.getStart() &&
                spanSys.charOffsetEnd==csAssessment.spanOfFiller.getEnd())
              return true;

//            for(EvalTextSpan spanAssessment : systemOutRelationLine.fillerProvenances) {
//              if(spanSys.equals(spanAssessment))
//                return true;
//            }
          }
        }
      }
    }
    return false;
  }

  static void divideAccessmentsByJudgement(Collection<CSAssessment> assessments,
      Multimap<String, RelationAfterApplyingQueryToSystemKB> queryId2systemOutRelationLines) {

    for(CSAssessment assessment : assessments) {
      System.out.println("dbg_ass\t" + assessment.queryId + "\t" + assessment.slot
          + "\t" + assessment.docid
          + "\t" + assessment.spanOfFiller.getStart() + "\t" + assessment.spanOfFiller.getEnd());
    }

    for(String queryId : queryId2systemOutRelationLines.keySet()) {
      for (RelationAfterApplyingQueryToSystemKB relationAfterApplyingQueryToSystemKB : queryId2systemOutRelationLines.get(queryId)) {
        System.out.println(
            "dbg_rel\t" + relationAfterApplyingQueryToSystemKB.queryId + "\t" + relationAfterApplyingQueryToSystemKB.slot.toString() + "\t"
                + relationAfterApplyingQueryToSystemKB.fillerProvenances.iterator().next().docId
                + "\t" + relationAfterApplyingQueryToSystemKB.fillerProvenances.iterator().next().charOffsetStart
                + "\t" + relationAfterApplyingQueryToSystemKB.fillerProvenances.iterator().next().charOffsetEnd + "\t" + queryId);
      }
    }

    List<CSAssessment> filteredAssessments = new ArrayList<CSAssessment>();
    for (CSAssessment assessment : assessments) {
      if (isFoundInSubmission(assessment, queryId2systemOutRelationLines)) {
        if(assessment.judgement1.equals("C")) {
          correctAssessments.add(assessment);

          Optional<String> strSlotPlusEid = getSlotPlusEquivalentClassID(assessment);
          if(strSlotPlusEid.isPresent())
            setSlotPlusEquivalentClassIDinCorrect.add(strSlotPlusEid.get());
        }
        else if(assessment.judgement1.equals("W") || assessment.judgement1.equals("X"))
          incorrectAssessments.add(assessment);
      }
    }

    for (CSAssessment assessment : assessments) {
      if (!isFoundInSubmission(assessment, queryId2systemOutRelationLines)) {
        System.out.println("assessment is not found in submission: " + assessment.responseId);

        // skip those we found an equivalent class
        Optional<String> strSlotPlusEid = getSlotPlusEquivalentClassID(assessment);
        if(strSlotPlusEid.isPresent()) {
          if(setSlotPlusEquivalentClassIDinCorrect.contains(strSlotPlusEid.get()))
            continue;
        }

        if(assessment.judgement1.equals("C"))
          missedAssessments.add(assessment);
      }
    }

  /*
    Set<CSAssessment> correctAnswersFoundInSubmission = new HashSet<CSAssessment>();
    correctAnswersFoundInSubmission.addAll(correctAssessments);
    Set<String> correctAnswerEquivalentClassesFoundInSubmission = new HashSet<String>();
    for (CSAssessment assessment : correctAnswersFoundInSubmission) {
      correctAnswerEquivalentClassesFoundInSubmission.add(assessment.equivalent_class_str);
    }

    for (CSAssessment assessment : filteredAssessments) {
      String judgement = responseID2judgement.get(assessment.responseId);

      if (judgement.equals("GT") && !correctAnswersFoundInSubmission.contains(assessment) &&
          !correctAnswerEquivalentClassesFoundInSubmission
              .contains(assessment.equivalent_class_str)) {
        missedAssessments.add(assessment);
      }
    }
     */
  }
}

