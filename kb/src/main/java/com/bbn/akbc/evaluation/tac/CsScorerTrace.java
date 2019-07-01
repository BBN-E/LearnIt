package com.bbn.akbc.evaluation.tac;

import com.bbn.akbc.evaluation.tac.io.AssessmentReader;
import com.bbn.akbc.common.FileUtil;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Created by bmin on 11/11/15.
 */
public class CsScorerTrace {
  public Multimap<String, String> ec2assessmentId;
  public Multimap<String, String> ecParent2child;

  public Map<String, String> assessmentId2judgement;

  public Set<String> rootECs;

  public Multimap<String, String> ec2submissionInfo;
  public Multimap<String, UnAssessedEntry> ec2unassessedEntry;

  public CsScorerTrace(String strFileScoringTrace) {
    ec2assessmentId = HashMultimap.create();
    ecParent2child = HashMultimap.create();
    assessmentId2judgement = new HashMap<String, String>();

    rootECs = new HashSet<String>();

    ec2submissionInfo = HashMultimap.create();
    ec2unassessedEntry = HashMultimap.create();

    List<String> lines = FileUtil.readLinesIntoList(strFileScoringTrace);

    for(String line : lines) {

      System.out.println("CsScorerTrace line: " + line);

      line = line.trim();
      String [] items = line.split(" ");
      if(line.isEmpty())
        continue;

      if(line.startsWith("trace")) {
        String judgement = items[1];
        String assessmentId = items[3];

        String parentEc = items[2];
        String thisEc = items[5];

        String queryAndSlotName = items[4];

        ecParent2child.put(parentEc, thisEc);

        if (parentEc.contains(":"))
          rootECs.add(parentEc.substring(0, parentEc.indexOf(":")));
        else
          rootECs.add(parentEc);

        System.out.println("ecParent2child.put: " + parentEc + " -> " + thisEc);

        assessmentId2judgement.put(assessmentId, judgement);
        ec2assessmentId.put(thisEc, assessmentId);
      }
      if(line.startsWith("UNASSESSED")) {
        String [] itemsByTab = line.trim().split("\t");
        String parentEc = itemsByTab[1];
        String thisEc = itemsByTab[2];

        String submissionInfo = line.substring(line.indexOf("SUBMISSION:"));
        ec2submissionInfo.put(thisEc, submissionInfo);

        ec2unassessedEntry.put(thisEc, UnAssessedEntry.fromLine(line));
      }
    }
  }

  public String toStringTreeWithAssessment() {
    StringBuilder sb = new StringBuilder();

    for(String rootEc : rootECs)
      sb.append(toStringTreeWithAssessment(rootEc, ""));

    return sb.toString();
  }

  public String getEcInfo(String ec) {
    StringBuilder sb = new StringBuilder();
    sb.append(ec + " ");
    if(QueryLoader.id2queryEntities.containsKey(ec))
      sb.append("Q:" + QueryLoader.id2queryEntities.get(ec).getText().replace(" ", "_") + " ");
    else if(AssessmentReader.eid2text.containsKey(ec)) {
      sb.append("EC:" + AssessmentReader.eid2text.get(ec));
    }

    if(ResolveQueryLogLoader2015.hop0QueryToEntityInSysOut.containsKey(ec))
      sb.append("Sys:" + ResolveQueryLogLoader2015.hop0QueryToEntityInSysOut.get(ec) + " ");

    return sb.toString().trim();
  }

  public String toStringTreeWithAssessment(String rootEC, String prefixPadding) {

    StringBuilder sb = new StringBuilder();

    sb.append(prefixPadding + "==== EC" + "(" + ecParent2child.get(rootEC).size() + ")" + ": " + getEcInfo(rootEC) + "\n");
    for(String assessmentId : ec2assessmentId.get(rootEC)) {
      System.out.println("assessmentId=" + assessmentId);

      CSAssessment csAssessment = AssessmentReader.assessmentId2assessment.get(assessmentId);

      String sysEidString = "NA";
      System.out.println("csAssessment.queryId=" + csAssessment.queryId);
      if(ResolveQueryLogLoader2015.hop0QueryToEntityInSysOut.containsKey(csAssessment.queryId))
        sysEidString = "Sys_h0:" +
            ResolveQueryLogLoader2015.hop0QueryToEntityInSysOut.get(csAssessment.queryId);
      else if(ResolveQueryLogLoader2015.hop1Query2Mention.containsKey(csAssessment.queryId))
        sysEidString = "Sys_h1:" +
            ResolveQueryLogLoader2015.hop1Query2Mention.get(csAssessment.queryId)
                .toColdStartString().replace("\t", "_").replace(" ", "_");

      sb.append(prefixPadding + "= " +
          assessmentId2judgement.get(assessmentId) + " " +
          csAssessment.slot.toString() + " " +
          "\"" + csAssessment.text + "\"" + " " +
          assessmentId + " " + sysEidString + " " + csAssessment.toStringForCsAnalysis() + "\n");
    }

    // output unassessed submission entries
    /*
    for(String submissionInfo : ec2submissionInfo.get(rootEC)) {
      sb.append(prefixPadding + "= " + "UNASSESSED " +
          submissionInfo
          + "\n");
    }
    */

    int new_annotation_gid=100000;
    for(UnAssessedEntry unAssessedEntry : ec2unassessedEntry.get(rootEC)) {

      StringBuilder stringBuilder = new StringBuilder();
      stringBuilder.append("Hop-" + ViewSubmissionTabFile.getHop(unAssessedEntry.parentEc) + "\t");
      stringBuilder.append(ViewSubmissionTabFile.normalizeText(this.getEcInfo(unAssessedEntry.parentEc)) + "\t");
      stringBuilder.append(unAssessedEntry.slot + "\t");
      stringBuilder.append(ViewSubmissionTabFile.normalizeText(unAssessedEntry.filler_text) + "\t");

      stringBuilder.append(ViewSubmissionTabFile.normalizeText(ColdStartCorpus.getTextWithContext(unAssessedEntry.filler_eval_text_span)) + "\t");
      for(EvalTextSpan evalTextSpan : unAssessedEntry.relation_provenances)
        stringBuilder.append(ViewSubmissionTabFile.normalizeText(ColdStartCorpus.getTextWithContext(evalTextSpan)) + "\t");

      // append the a few lines for human assessment
      sb.append(prefixPadding + "= " + "UNASSESSED " + stringBuilder.toString() + "\n");

      // append one line for appending to assessment file
      sb.append("ANNOTATION\t" + unAssessedEntry.toAnnotationString(++new_annotation_gid) + "\n");
      //
    }

    //

//  sb.append("\n");
    for(String childEc : ecParent2child.get(rootEC)) {
      sb.append(toStringTreeWithAssessment(childEc, prefixPadding + "    "));
      sb.append("\n");
    }

//  sb.append("\n");
//  sb.append("\n");

    return sb.toString();
  }
}
