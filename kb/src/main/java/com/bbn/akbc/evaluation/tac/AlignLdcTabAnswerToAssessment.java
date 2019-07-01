package com.bbn.akbc.evaluation.tac;

import com.bbn.akbc.evaluation.tac.io.AssessmentReader;
import com.bbn.akbc.kb.text.TextSpan;
import com.bbn.akbc.resource.Resources;
import com.bbn.serif.io.SerifXMLLoader;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.Entity;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.SentenceTheory;

import com.google.common.base.Optional;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AlignLdcTabAnswerToAssessment {

  static boolean isCS2014Eval = false;

  static Map<String, String> line2lineIdInAssessment = new HashMap<String, String>();

  static double MAX_DISPARITY = 10;
  static double NON_OVERLAPPING_DISPARITY = 100;
  // the large value that indicates two mention does not overlap

  static Map<String, CSAssessment> id2assessment = new HashMap<String, CSAssessment>();

  public static void main(String[] argv) throws IOException {
    String fileAssessment = argv[0];
    String fileLdcTabAnswer = argv[1];

    String fileOutput = argv[2]; // fileLdcTabAnswer + ".out";

    String dirSerifXml =
        argv[3]; // "/nfs/mercury-04/u10/KBP_ColdStart/2013/processed/scripts/mini_corpus_CS2013/output.parallel.win/output/"

    // 2014 CS evaluation
    if (argv.length > 4) {
      if (argv[4].equals("2014CS")) {
        isCS2014Eval = true;
      }
    }

    Resources.setPathSerifXml(dirSerifXml);

    List<CSAssessment> assessments;
    if (isCS2014Eval) {
      assessments = AssessmentReader.readCSAssessmentsFromFileCS2014(fileAssessment);
    } else {
      assessments = AssessmentReader.readCSAssessmentsFromFile(fileAssessment);
    }

    for (CSAssessment assessment : assessments) {
      id2assessment.put(assessment.getResponseId(), assessment);
    }

    List<LDCTabAnswer> ldcTabAnswers;
    if (isCS2014Eval) {
      ldcTabAnswers = AssessmentReader.readLDCTabAnswerFromFileCS2014(fileLdcTabAnswer);
    } else {
      ldcTabAnswers = AssessmentReader.readLDCTabAnswerFromFile(fileLdcTabAnswer);
    }

    alignLdcTabAnswerFile(assessments, ldcTabAnswers, fileOutput);
  }

  public static void alignLdcTabAnswerFile(List<CSAssessment> assessments,
      List<LDCTabAnswer> ldcTabAnswers, String fileOutput) {
    Map<String, List<String>> rID2sourcePatterns = new HashMap<String, List<String>>();

    for (LDCTabAnswer ldcTabAnswer : ldcTabAnswers) {
      // fuzzy match
      List<String> listRID = getOverlapsInLDCAssessment(ldcTabAnswer, assessments);
      if (listRID.isEmpty()) {
        System.out.println("===no line found: \t" + ldcTabAnswer.toString());
      } else if (listRID.size() > 1) {
        System.out.println("===too many matches for: \t" + ldcTabAnswer);
      } else {
        // setRID.add(listRID.get(0));
        String rid = listRID.get(0);
        if (!rID2sourcePatterns.containsKey(rid)) {
          rID2sourcePatterns.put(rid, new ArrayList<String>());
        }
        rID2sourcePatterns.get(rid).add(ldcTabAnswer.getSourcePattern());

        System.out.println(
            "+++line found: \t" + ldcTabAnswer.toString() + "\t->\t" + id2assessment.get(rid));

      }
    }

    // write results
    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter(new File(fileOutput)));
      for (String rid : rID2sourcePatterns.keySet()) {
        writer.write(rID2sourcePatterns.get(rid).get(0) + "\t" + rid + "\n");
      }
      writer.close();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  static List<String> getOverlapsInLDCAssessment(LDCTabAnswer ldcTabAnswer,
      List<CSAssessment> assessments) {
    List<String> listRID = new ArrayList<String>();

    for (CSAssessment assessment : assessments) {
      if (ldcTabAnswer.getQueryId().equals(assessment.getQueryId()) &&
          ldcTabAnswer.getDocId().equals(assessment.getDocId()) &&
          ldcTabAnswer.getSlot().equals(assessment.getSlot()) &&
          matchByCoref(ldcTabAnswer.getDocId(), ldcTabAnswer.getSpanOfFiller(),
              assessment.getSpanOfFiller(), 5)) {
        listRID.add(assessment.getResponseId());
        break;
      }
    }

    return listRID;
  }

  static Optional<Entity> getEntityByCharOffsets(DocTheory dt, int start, int end) {
    Optional<Entity> matchedEntity = Optional.absent();
    double closest_distance = Double.POSITIVE_INFINITY;

    for (Entity e : dt.entities().asList()) {
      for (Mention m : e.mentions()) {
        int mention_start = m.span().startCharOffset().value();
        int mention_end = m.span().endCharOffset().value();
        if (!overlaps(start, end, mention_start, mention_end)) {
          continue;
        }
        double distance = spanDistance(start, end, mention_start, mention_end);
        if (distance < closest_distance) {
          closest_distance = distance;
          matchedEntity = Optional.of(e);
        }
      }
    }

    return matchedEntity;
  }

  /**
   * @return true if two spans have any overlap
   */
  static boolean overlaps(int start1, int end1, int start2, int end2) {
    return end1 >= start2 && start1 <= end2;
  }

  /**
   * @return an Double quantifying how much two spans overlap. Used to determine closest matching
   * spans.
   *
   * This is a distance function--the higher the distance, the less two spans overlap (or the
   * further apart they are).
   *
   * 0.0 = exact match 1.0 = partial match, one char off sqrt(2.0) = less exact partial match, 2
   * chars off ... etc.
   */
  static Double spanDistance(int start1, int start2, int end1, int end2) {
    return Math.sqrt((start1 - start2) ^ 2 + (end1 - end2) ^ 2);
  }

  static Optional<Integer> getSentenceId(DocTheory dt, int offset) {
    for (int sid = 0; sid < dt.sentenceTheories().size(); sid++) {
      SentenceTheory s = dt.sentenceTheories().get(sid);

      if (!s.tokenSequence().isEmpty()) {
        int start = s.span().startCharOffset().value();
        int end = s.span().endCharOffset().value();

        if (offset >= start && offset <= end) {
          return Optional.of(sid);
        }
      }
    }

    return Optional.absent();
  }


  static boolean matchByCoref(String docId,
      TextSpan span1, TextSpan span2,
      int thres_num_sentences_apart) {

    int start1 = span1.getStart();
    int end1 = span1.getEnd();
    int start2 = span2.getStart();
    int end2 = span2.getEnd();

    // String strPathSerifXml = "/nfs/mercury-04/u42/bmin/source/Active/Projects/ColdStart/2del/xWin/output.parallel.win/output/" + docId + ".xml";
    String strPathSerifXml = Resources.getPathSerifXml() + docId + ".xml";
    File f = new File(strPathSerifXml);
    // test to see if path to serifxml exists, if not try adding .sgm
    if (!f.exists()) {
      strPathSerifXml = Resources.getPathSerifXml() + docId + ".sgm.xml";
      f = new File(strPathSerifXml);
    }
    try {
      SerifXMLLoader fromXML = SerifXMLLoader.fromStandardACETypes();
      DocTheory dt = fromXML.loadFrom(f);

      final Optional<Integer> sid1 = getSentenceId(dt, start1);
      final Optional<Integer> sid2 = getSentenceId(dt, start2);

      if (!sid1.isPresent() || !sid2.isPresent()
          || Math.abs(sid1.get() - sid2.get()) > thres_num_sentences_apart) {
        return false;
      }

      Optional<Entity> e1 = getEntityByCharOffsets(dt, start1, end1);
      Optional<Entity> e2 = getEntityByCharOffsets(dt, start2, end2);

      if (!e1.isPresent() || !e2.isPresent()) {
        return false;
      }

      return e1.get().equals(e2.get());

    } catch (IOException e) {
      e.printStackTrace();
      //code to handle an IOException here
      System.out.println("Exception in reading: " + strPathSerifXml);
    }

    return false;
  }
}

