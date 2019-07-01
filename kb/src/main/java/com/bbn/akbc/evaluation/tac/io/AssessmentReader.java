package com.bbn.akbc.evaluation.tac.io;

import com.bbn.akbc.evaluation.tac.CSAssessment;
import com.bbn.akbc.evaluation.tac.LDCTabAnswer;
import com.bbn.akbc.common.FileUtil;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AssessmentReader {
  public static Multimap<String, CSAssessment> query2assessments;
  public static Map<String, CSAssessment> assessmentId2assessment;
  public static Map<String, String> eid2text;

  public static void initForCS2015(String fileAssessment) {
    query2assessments = HashMultimap.create();
    assessmentId2assessment = new HashMap<String, CSAssessment>();
    eid2text = new HashMap<String, String>();

    List<CSAssessment> assessments = AssessmentReader.readCSAssessmentsFromFileCS2015(fileAssessment);
    for(CSAssessment assessment : assessments)
      System.out.println("assessment.add\t" + assessment.toString());

    for(CSAssessment csAssessment : assessments) {
      query2assessments.put(csAssessment.queryId, csAssessment);
      assessmentId2assessment.put(csAssessment.responseId, csAssessment);
      eid2text.put(csAssessment.equivalent_class_str_in_file.get(), csAssessment.text);
    }
  }

  public static void initForCS2016(String fileAssessment) {
    query2assessments = HashMultimap.create();
    assessmentId2assessment = new HashMap<String, CSAssessment>();
    eid2text = new HashMap<String, String>();

    List<CSAssessment> assessments = AssessmentReader.readCSAssessmentsFromFileCS2016(fileAssessment);
    for(CSAssessment assessment : assessments)
      System.out.println("assessment.add\t" + assessment.toString());

    for(CSAssessment csAssessment : assessments) {
      query2assessments.put(csAssessment.queryId, csAssessment);
      assessmentId2assessment.put(csAssessment.responseId, csAssessment);
      eid2text.put(csAssessment.equivalent_class_str_in_file.get(), csAssessment.text);
    }
  }

  public static List<CSAssessment> readCSAssessmentsFromFile(String file) {
    List<CSAssessment> assessments = new ArrayList<CSAssessment>();

    // TODO: replace with eclass generator in scorer
    int equivalent_class_generator = 0;

    Map<String, Integer> equivalentClassStr2id = new HashMap<String, Integer>();

    // read lines
    List<String> lines = FileUtil.readLinesIntoList(file);

    // sort by hops
    Collections.sort(lines, new Comparator<String>() {
      @Override
      public int compare(String s1, String s2) {
        String[] fields1 = s1.split("\t");
        int hop1 = Integer.parseInt(fields1[1].substring(fields1[1].lastIndexOf("_") + 1));

        String[] fields2 = s2.split("\t");
        int hop2 = Integer.parseInt(fields2[1].substring(fields2[1].lastIndexOf("_") + 1));

        return hop1 - hop2;
      }
    });

    for (String assessmentLine : lines) {
      CSAssessment assessment = null;
      int equivalent_class_id = -1;

      if (assessmentLine.endsWith("\t0")) {
        equivalent_class_id = ++equivalent_class_generator;
        assessment = CSAssessment.fromLine(assessmentLine, equivalent_class_id);
      } else {
        assessment = CSAssessment.fromLine(assessmentLine, -1);
        String equivalent_class = assessment.queryId + "|" + assessment.slot + "|" + assessmentLine
            .substring(assessmentLine.lastIndexOf("\t")).trim(); // get equivalent class ID

        if (!equivalentClassStr2id.containsKey(equivalent_class)) {
          equivalent_class_id = ++equivalent_class_generator;
          equivalentClassStr2id.put(equivalent_class, equivalent_class_id);
        } else {
          equivalent_class_id = equivalentClassStr2id.get(equivalent_class);
        }
        assessment.equivalent_class_id = equivalent_class_id;
      }

      assessments.add(assessment);
    }

    return assessments;
  }

  public static List<CSAssessment> readCSAssessmentsFromFileCS2014(String file) {
    List<CSAssessment> assessments = new ArrayList<CSAssessment>();

    int lineno = 0;

    List<String> lines = FileUtil.readLinesIntoList(file);
    for (String assessmentLine : lines) {
      if(assessmentLine.isEmpty()) continue;
//    System.out.println("assessmentLine: " + assessmentLine);
      CSAssessment assessment = CSAssessment.fromLine2014CS(assessmentLine, ++lineno);
      assessments.add(assessment);
    }

    return assessments;
  }

  public static List<CSAssessment> readCSAssessmentsFromFileCS2015(String file) {
    List<CSAssessment> assessments = new ArrayList<CSAssessment>();

    int lineno = 0;

    List<String> lines = FileUtil.readLinesIntoList(file);
    for (String assessmentLine : lines) {
      if(assessmentLine.isEmpty()) continue;
//    System.out.println("assessmentLine: " + assessmentLine);
      CSAssessment assessment = CSAssessment.fromLine2015CS(assessmentLine);
      assessments.add(assessment);
    }

    return assessments;
  }

  public static List<CSAssessment> readCSAssessmentsFromFileCS2016(String file) {
    List<CSAssessment> assessments = new ArrayList<CSAssessment>();

    int lineno = 0;

    List<String> lines = FileUtil.readLinesIntoList(file);
    for (String assessmentLine : lines) {
      if(assessmentLine.isEmpty()) continue;
      CSAssessment assessment = CSAssessment.fromLine2016CS(assessmentLine);
      assessments.add(assessment);
    }

    return assessments;
  }

  public static List<LDCTabAnswer> readLDCTabAnswerFromFile(String file) {
    List<LDCTabAnswer> ldcTabAnswers = new ArrayList<LDCTabAnswer>();

    List<String> lines = FileUtil.readLinesIntoList(file);
    for (String line : lines) {
      Optional<LDCTabAnswer> answer = LDCTabAnswer.fromLine(line);
      if (answer.isPresent()) {
        ldcTabAnswers.add(answer.get());
      }
    }

    return ldcTabAnswers;
  }

  public static List<LDCTabAnswer> readLDCTabAnswerFromFileCS2014(String file) {
    List<LDCTabAnswer> ldcTabAnswers = new ArrayList<LDCTabAnswer>();

    List<String> lines = FileUtil.readLinesIntoList(file);
    for (String line : lines) {
      Optional<LDCTabAnswer> answer = LDCTabAnswer.fromLineCS2014(line);
      if (answer.isPresent()) {
        ldcTabAnswers.add(answer.get());
      }
    }

    return ldcTabAnswers;
  }

  public static void main(String[] argv) {
    List<CSAssessment> assessments = AssessmentReader.readCSAssessmentsFromFile(
        "/nfs/mercury-04/u10/KBP_ColdStart/2013/processed/assessment/cat_of_all_files_hop0_and_hop1.2012format.txt");
    for (CSAssessment assessment : assessments) {
      System.out.println(assessment.toString());
    }

    List<LDCTabAnswer> ldcTabAnswers = AssessmentReader.readLDCTabAnswerFromFile(
        "/nfs/mercury-04/u42/bmin/projects/coldstart2014/output.parallel.win.2013_combine_pruned/all.kb.valid.ldc.tab.txt");
    for (LDCTabAnswer ldcTabAnswer : ldcTabAnswers) {
      System.out.println(ldcTabAnswer.toString());
    }
  }
}
