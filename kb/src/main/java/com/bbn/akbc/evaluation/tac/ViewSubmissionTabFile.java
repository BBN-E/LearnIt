package com.bbn.akbc.evaluation.tac;

import com.bbn.akbc.evaluation.tac.io.AssessmentReader;
import com.bbn.akbc.evaluation.tac.io.SystemKbMentionLoader;
import com.bbn.akbc.kb.text.TextMention;
import com.bbn.akbc.resource.Resources;
import com.bbn.akbc.common.FileUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by bmin on 11/10/15.
 */
public class ViewSubmissionTabFile {
  public static void main(String [] argv) throws IOException, StringIndexOutOfBoundsException {
    String resolveQueriesLog = argv[0];
    String fileSystemKB = argv[1];
    String fileAssessment = argv[2]; // "/nfs/mercury-04/u42/bmin/everything/projects/coldstart/coldstart2015/official_evaluation_results.20151022/BBN.KBP2015_Cold_Start_Scores_20151021/slot-filling-evaluation/corrected_runs/KB_BBN1.valid.ldc.tab.txt";
    String fileQuery = argv[3];
    String fileVisualDir = argv[4];

    String dirSerifXml = argv[5]; // "/nfs/mercury-04/u10/KBP_ColdStart/2013/processed/scripts/mini_corpus_CS2013/output.parallel.win/output/"
    String dirSerifXmlVisualization = argv[6];
    String evalYear = argv[7];

    String fileSystemSubmissionTabRun = argv[8];

    String fileTraceCsScorer = argv[9];

    String fileVisualizeTabFile = argv[10];

    CsScorerTrace csScorerTrace = new CsScorerTrace(fileTraceCsScorer);

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
    systemResponseLoaderRun1.initFromFile(fileSystemSubmissionTabRun);
    systemResponseLoaderRun1.alignToAccessment();

    System.out.println("=== QueryLoader.loadCanonicalMention");
    QueryLoader.load(fileQuery);
    for(String queryId : QueryLoader.id2queryEntities.keySet()) {
      System.out.println("id2queryEntities.put" + "\t" + "queryId=" + queryId + "\t" +
          QueryLoader.id2queryEntities.get(queryId).toString().replace("\n", "|"));
    }

    System.out.println("=== SystemKbMentionLoader.loadCanonicalMention");
    SystemKbMentionLoader.loadCanonicalMention(fileSystemKB,
        ResolveQueryLogLoader2015.hop0QueryToEntityInSysOut);

    List<String> strings = new ArrayList<String>();
    strings.add("---------------------------------------------------------");
    ColdStartCorpus.init("/nfs/mercury-04/u42/bmin/projects/coldstart/coldstart2015/bbn_official_submissions/corpus/cs2015_doc_list_paths.all.shuffled");
    int new_annotation_gid=100000;
    for(String ec : csScorerTrace.ec2unassessedEntry.keySet()) {
      for(UnAssessedEntry unAssessedEntry : csScorerTrace.ec2unassessedEntry.get(ec)) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Hop-" + getHop(unAssessedEntry.parentEc) + "\n");
        stringBuilder.append(normalizeText(csScorerTrace.getEcInfo(unAssessedEntry.parentEc)) + "\n");
        stringBuilder.append(unAssessedEntry.slot + "\n");
        stringBuilder.append(normalizeText(unAssessedEntry.filler_text) + "\n");

//      stringBuilder.append(getAnnotatedTextForQuery(coldStartCorpus, QueryLoader.id2queryEntities.get(ec)) + "\t");
        stringBuilder.append(normalizeText(ColdStartCorpus.getTextWithContext(unAssessedEntry.filler_eval_text_span)) + "\n");
        for(EvalTextSpan evalTextSpan : unAssessedEntry.relation_provenances)
          stringBuilder.append(normalizeText(ColdStartCorpus.getTextWithContext(evalTextSpan)) + "\n");

        // append one line for appending to assessment file
        stringBuilder.append("ANNOTATION\t" + unAssessedEntry.toAnnotationString(++new_annotation_gid) + "\n");
        //

        strings.add(stringBuilder.toString().toString());
      }
    }

    strings.add("---------------------------------------------------------");
    strings.add("=========================================================");
    strings.add(csScorerTrace.toStringTreeWithAssessment());
    strings.add("=========================================================");

    FileUtil.writeToFile(strings, "\n", fileVisualizeTabFile);

  }

  static String normalizeText(String text) {
    return text.replace("\n", " ").replace("\r", " ").replace("\t", " ");
  }

  static int getHop(String parentEC) {
    String [] items = parentEC.split(":");
    return items.length-1;
  }


  // QueryLoader.id2queryEntities.get(ec)
  static String getAnnotatedTextForQuery(Query query) {

    TextMention textMention = query.mentions.iterator().next();

    return ColdStartCorpus.getTextWithContext(textMention.getDocId(),
        textMention.getSpan().getStart(),
        textMention.getSpan().getEnd());
  }
}
