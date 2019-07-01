package com.bbn.akbc.evaluation.tac;

import com.bbn.akbc.evaluation.tac.io.AssessmentReader;
import com.bbn.akbc.evaluation.tac.io.SystemKbMentionLoader;
import com.bbn.akbc.resource.Resources;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by bmin on 11/8/15.
 */
public class DiffSubmissions {
  public static void main(String [] argv) throws IOException {
    String resolveQueriesLog = argv[0];
    String fileSystemKB = argv[1];
    String fileAssessment = argv[2];
    String fileQuery = argv[3];
    String fileVisualDir = argv[4];

    String dirSerifXml =
        argv[5]; // "/nfs/mercury-04/u10/KBP_ColdStart/2013/processed/scripts/mini_corpus_CS2013/output.parallel.win/output/"
    String dirSerifXmlVisualization = argv[6];
    String evalYear = argv[7];

    String fileSystemSubmissionTabRun1 = argv[8];
    String fileSystemSubmissionTabRun2 = argv[9];


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
    systemResponseLoaderRun1.initFromFile(fileSystemSubmissionTabRun1);
    systemResponseLoaderRun1.alignToAccessment();

    System.out.println("=== systemResponseLoaderRun2.initFromFile");
    System.out.println("=== systemResponseLoaderRun2.alignToAccessment");
    SystemResponseLoader systemResponseLoaderRun2 = new SystemResponseLoader();
    systemResponseLoaderRun2.initFromFile(fileSystemSubmissionTabRun2);
    systemResponseLoaderRun2.alignToAccessment();



    System.out.println("=== QueryLoader.loadCanonicalMention");
    Map<String, CSInitQuery> id2queryEntities = QueryLoader.load(fileQuery);
    for(String queryId : id2queryEntities.keySet()) {
      System.out.println("id2queryEntities.put" + "\t" + "queryId=" + queryId + "\t" + id2queryEntities.get(queryId).toString().replace("\n", "|"));
    }

    System.out.println("=== SystemKbMentionLoader.loadCanonicalMention");
    SystemKbMentionLoader.loadCanonicalMention(fileSystemKB,
        ResolveQueryLogLoader2015.hop0QueryToEntityInSysOut);




    // divide assessments, generate diff
    Set<CSAssessment> csAssessmentsInRun1 = new HashSet<CSAssessment>();
    Set<CSAssessment> csAssessmentsInRun2 = new HashSet<CSAssessment>();
    List<CSAssessment> csAssessmentsInRun1NotRun2 = new ArrayList<CSAssessment>();
    List<CSAssessment> csAssessmentsInRun2NotRun1 = new ArrayList<CSAssessment>();

    for(RelationAfterApplyingQueryToSystemKB relationAfterApplyingQueryToSystemKB :
        systemResponseLoaderRun1.relationSubmission2accessment.keySet()) {
      CSAssessment csAssessment =
          systemResponseLoaderRun1.relationSubmission2accessment.get(relationAfterApplyingQueryToSystemKB);
      csAssessmentsInRun1.add(csAssessment);
    }
    for(RelationAfterApplyingQueryToSystemKB relationAfterApplyingQueryToSystemKB :
        systemResponseLoaderRun2.relationSubmission2accessment.keySet()) {
      CSAssessment csAssessment =
          systemResponseLoaderRun2.relationSubmission2accessment.get(relationAfterApplyingQueryToSystemKB);
      csAssessmentsInRun2.add(csAssessment);
    }

    for(CSAssessment csAssessment1 : csAssessmentsInRun1) {
      if(!csAssessmentsInRun2.contains(csAssessment1))
        csAssessmentsInRun1NotRun2.add(csAssessment1);
    }
    for(CSAssessment csAssessment2 : csAssessmentsInRun2) {
      if(!csAssessmentsInRun1.contains(csAssessment2))
        csAssessmentsInRun2NotRun1.add(csAssessment2);
    }

    System.out.println("csAssessmentsInRun1.size(): " + csAssessmentsInRun1.size());
    System.out.println("csAssessmentsInRun2.size(): " + csAssessmentsInRun2.size());

    System.out.println("csAssessmentsInRun1NotRun2.size(): " + csAssessmentsInRun1NotRun2.size());
    System.out.println("csAssessmentsInRun2NotRun1.size(): " + csAssessmentsInRun2NotRun1.size());

    String fileRelationInRun1NotRun2HTML = fileVisualDir + "/relationInRun1NotRun2.html";
    String fileRelationInRun2NotRun1HTML = fileVisualDir + "/relationInRun2NotRun1.html";

    AssessmentTextPrinter.write(fileRelationInRun1NotRun2HTML,
        csAssessmentsInRun1NotRun2,
        ResolveQueryLogLoader2015.hop0QueryToEntityInSysOut,
        SystemKbMentionLoader.hop0queryId2listCanonicalMention,
        id2queryEntities,
        ResolveQueryLogLoader2015.hop1Query2Mention);

    AssessmentTextPrinter.write(fileRelationInRun2NotRun1HTML,
        csAssessmentsInRun2NotRun1,
        ResolveQueryLogLoader2015.hop0QueryToEntityInSysOut,
        SystemKbMentionLoader.hop0queryId2listCanonicalMention,
        id2queryEntities,
        ResolveQueryLogLoader2015.hop1Query2Mention);


    /*
    System.out.println("=== SystemKbRelationLoader.loadCanonicalMention");
    SystemKbRelationLoader.load(fileSystemKB);
    for(String docId : SystemKbRelationLoader.doc2relations.keySet()) {
      for(RelationFromSubmission relationFromSubmission : SystemKbRelationLoader.doc2relations.get(docId))
        System.out.println("SystemKbRelationLoader.doc2relations\t" + docId + "\t" + relationFromSubmission.toString());
    }
    */

  }
}
