package com.bbn.akbc.evaluation.tac;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE)
public class EvalKB {

  @JsonProperty
  List<String> entryQueries;
  @JsonProperty
  Map<String, Entity> id2entities;
  @JsonProperty
  Map<Integer, Relation> id2relations;


  // Set<Relation> listRelations;
  Map<Integer, Map<String, Relation>> hop2srcId2Relation;
  Map<Integer, Map<String, Relation>> hop2dstId2Relation;


  Map<String, EquivalentClass> eclassStr2equivalentClasses;
  Map<String, Assessment> responseId2assessment;
  Map<String, String> responseID2eclassID;
  Map<String, String> sysAlignedResponseID2sourcePattern;

  Map<String, String> queryToEntityInSysOut;
  Map<String, List<CanonicalMention>> id2listCanonicalMention;

  public EvalKB(String fileQuery, String fileAssessment, String fileSysKbAligned,
      String fileResolveQueries, String sysKb) throws IOException {
    //String dirSerifXml = "/nfs/mercury-04/u10/KBP_ColdStart/2013/processed/scripts/mini_corpus_CS2013/output.parallel.win/output/";
//		String dirSerifXml = "/nfs/mercury-04/u10/KBP_ColdStart/2012/processed/corpus/mini/serifxml/";
    //String dirSerifXmlVisualization = argv[6];
    //String evalYear = argv[7];

//		Resources.setPathSerifXml(dirSerifXml);
    //Resources.setDirSerifXmlVisualization(dirSerifXmlVisualization);
    //Resources.setEvalYear(evalYear);

    loadQueries(fileQuery);
    loadSysResponse(fileSysKbAligned);
    loadEquivalentClassLDC(fileAssessment);
    loadSysEntities(fileResolveQueries, sysKb);
    populateData(eclassStr2equivalentClasses);
  }

  public List<CanonicalMention> getSystemMentionsForQueryEntity(String queryEntity) {
    if (queryToEntityInSysOut.containsKey(queryEntity)) {
      System.out.println("Get canonical mentions for " + queryToEntityInSysOut.get(queryEntity));
      return id2listCanonicalMention.get(queryToEntityInSysOut.get(queryEntity));
    }
    return new ArrayList<CanonicalMention>();
  }

  public void loadSysEntities(String fileResolveQueries, String fileSysKb) throws IOException {
    // map query entities to system entities
    queryToEntityInSysOut = new HashMap<String, String>();
    String sline;
    BufferedReader reader = new BufferedReader(new FileReader(fileResolveQueries));
    while ((sline = reader.readLine()) != null) {
      String[] fields = sline.trim().split("\t");
      if (fields.length != 3) {
        continue;
      }

      if (!fields[0].equals("dbg")) {
        continue;
      }
      queryToEntityInSysOut.put(fields[1], fields[2]);
    }
    reader.close();

    id2listCanonicalMention = new HashMap<String, List<CanonicalMention>>();
    // collect the system entities
    reader = new BufferedReader(new FileReader(fileSysKb));

    while ((sline = reader.readLine()) != null) {
      String[] fields = sline.trim().split("\t");
      if (fields[1].equals("canonical_mention") || fields[1].equals("mention")) {

        CanonicalMention canonicalMention = new CanonicalMention(fields[0],
            fields[2].replace("\"", ""), fields[3], Integer.parseInt(fields[4]),
            Integer.parseInt(fields[5]));

        // for efficiency reason, skip the ones that are not in final results
        if (!queryToEntityInSysOut.values().contains(canonicalMention.id)) {
          continue;
        }

        if (!id2listCanonicalMention.containsKey(canonicalMention.id)) {
          id2listCanonicalMention.put(canonicalMention.id, new ArrayList<CanonicalMention>());
        }

        id2listCanonicalMention.get(canonicalMention.id).add(canonicalMention);
      }
    }
    reader.close();

  }

  public Collection<Relation> getRelations() {
    return id2relations.values();
  }

  public void loadSysResponse(String fileSysKbAligned) throws IOException {
    sysAlignedResponseID2sourcePattern = new HashMap<String, String>();

    BufferedReader reader = new BufferedReader(new FileReader(fileSysKbAligned));
    String sline;
    while ((sline = reader.readLine()) != null) {
      String[] fields = sline.split("\t");
      sysAlignedResponseID2sourcePattern.put(fields[1], fields[0]);
    }
    reader.close();
  }

  public void print() {
    for (String eid : id2entities.keySet()) {
      Entity e = id2entities.get(eid);
      System.out.println(e.toString());
    }

    for (int hop : hop2srcId2Relation.keySet()) {
      for (String srcId : hop2srcId2Relation.get(hop).keySet()) {
        Relation r = hop2srcId2Relation.get(hop).get(srcId);
        System.out.println(r.toString());
      }
    }
  }

  public void loadQueries(String fileQuery) throws IOException {
    id2entities = new HashMap<String, Entity>();
    List<String> entryQueries = new ArrayList<String>();

    BufferedReader reader = new BufferedReader(new FileReader(fileQuery));
    String sline;

//		sline = reader.readLine();
//		sline = reader.readLine();

    while ((sline = reader.readLine()) != null) {
      String queryId = "";
      String text = "";
      String docid = "";
      int beg = 0;
      int end = 0;
      int offset = 0;
      String slot0 = "";

      while (true) {
        sline = sline.trim();
        if (sline.startsWith("<query id=\"")) {
          queryId = sline.replace("<query id=\"", "").replace("\">", "");
        }

        if (sline.startsWith("<name>")) {
          text = sline.replace("<name>", "").replace("</name>", "");
        }

        if (sline.startsWith("<docid>")) {
          docid = sline.replace("<docid>", "").replace("</docid>", "");
        }

        if (sline.startsWith("<beg>")) {
          beg = Integer.parseInt(sline.replace("<beg>", "").replace("</beg>", ""));
        }

        if (sline.startsWith("<end>")) {
          end = Integer.parseInt(sline.replace("<end>", "").replace("</end>", ""));
        }

        if (sline.startsWith("<offset>")) {
          offset = Integer.parseInt(sline.replace("<offset>", "").replace("</offset>", ""));
        }

        if (sline.startsWith("<slot0>")) {
          slot0 = sline.replace("<slot0>", "").replace("</slot0>", "");
        }

        if (sline.startsWith("</query>")) {
          queryId = queryId + "_00"; // make it compatible with assessment
          Entity query = new Entity(queryId);
          query.addMention(new MentionInfo(text, docid, beg, end));
//					System.out.println("== queryId:" + queryId + ", text:" + text + ", docid:" + docid + ", beg:" + beg + ", end:" + end);
          id2entities.put(queryId, query);

          entryQueries.add(queryId);
        }

        sline = reader.readLine();
        if (sline == null) {
          break;
        }
      }
    }
    reader.close();
  }


  public void populateData(Map<String, EquivalentClass> eclassStr2equivalentClasses) {
//		listRelations = new HashSet<Relation>();
    id2relations = new HashMap<Integer, Relation>();

    hop2srcId2Relation = new HashMap<Integer, Map<String, Relation>>();
    hop2dstId2Relation = new HashMap<Integer, Map<String, Relation>>();

    List<EquivalentClass> listEquivalentClass = new ArrayList<EquivalentClass>();
    listEquivalentClass.addAll(eclassStr2equivalentClasses.values());

    Collections.sort(listEquivalentClass, new Comparator<EquivalentClass>() {
      @Override
      public int compare(EquivalentClass arg0, EquivalentClass arg1) {
        return arg0.hop - arg1.hop;
      }
    });

//		for(String equivalentClassID : eclassStr2equivalentClasses.keySet()) {
//			EquivalentClass eclass = eclassStr2equivalentClasses.get(equivalentClassID);
    for (EquivalentClass eclass : listEquivalentClass) {
      int hopId = eclass.hop;
      String judgement = eclass.judgement;

      for (Assessment assessment : eclass.listAssessment) {
        String eid1;
        if (assessment.parentId.equals("NIL")) {
          eid1 = assessment.queryId;
        } else
//					eid1 = responseId2assessment.get(assessment.parentId).equivalent_class;
        {
          eid1 = responseId2assessment.get(assessment.parentId)
              .getEquivalentClassStr(responseID2eclassID);
        }

        System.out.println("eid1: " + eid1);
        Entity e1 = id2entities.get(
            eid1); // must exists in current set of entities // TODO: may not exist if it is a hop-N query (not yet populated);

        if (e1 == null) {
          throw new RuntimeException("Couldn't get query entity for " + eid1);
        }

        // String eid2 = assessment.equivalent_class;	// simply use the equivalent class ID
        String eid2 = assessment
            .getEquivalentClassStr(responseID2eclassID); // simply use the equivalent class ID

        if (!id2entities.containsKey(eid2)) {
          id2entities.put(eid2, new Entity(eid2));
        }
        Entity e2 = id2entities.get(eid2);
        MentionInfo mention2 =
            new MentionInfo(assessment.text, assessment.docid, assessment.filler_beg,
                assessment.filler_end);
        e2.addMention(mention2);

        boolean inSysKB =
            sysAlignedResponseID2sourcePattern.containsKey(assessment.responseId) ? true : false;
        String sourcePattern = "NIL";
        if (inSysKB) {
          sourcePattern = sysAlignedResponseID2sourcePattern.get(assessment.responseId);
        }

        Relation relation = new Relation(e1, e2, assessment.slot);
        if (!id2relations.containsKey(relation.hashCode())) {
          id2relations.put(relation.hashCode(), relation);
        }
        id2relations.get(relation.hashCode())
            .addMention(new RelationMention(e1, mention2, assessment.slot,
                assessment.hopId, assessment.judgement1, inSysKB, sourcePattern));

//				listRelations.add(relation);
      }
    }

    // consistency check
    for (Relation r : id2relations.values()) {
      Set<Integer> hopsFound = new HashSet<Integer>();
      Set<String> judgementsFound = new HashSet<String>();

      for (RelationMention rm : r.relationMentions) {
        hopsFound.add(rm.hopId);
        judgementsFound.add(rm.judgement);
      }

      if (hopsFound.size() != 1) {
        System.err.println("ERROR: relation mentions from multiple hop exists in one relation");
        System.exit(-1);
      }

			/*
                         * TODO: handle inexact, redudant and correct; multiple may exist
			if(judgementsFound.size()!=1) {
				System.err.println("ERROR: relation mentions with different judgements hop exists in one relation");
				System.exit(-1);
			}
			*/
    }

    for (Relation r : id2relations.values()) {
      int hopId = r.relationMentions.iterator().next().hopId;
      if (!hop2srcId2Relation.containsKey(hopId)) {
        hop2srcId2Relation.put(hopId, new HashMap<String, Relation>());
      }
      if (!hop2dstId2Relation.containsKey(hopId)) {
        hop2dstId2Relation.put(hopId, new HashMap<String, Relation>());
      }

      hop2srcId2Relation.get(hopId).put(r.queryEntity.id, r);
      hop2dstId2Relation.get(hopId).put(r.answerEntity.id, r);
    }
  }

  public Map<String, EquivalentClass> loadEquivalentClassLDC(String fileAssessment)
      throws IOException {
    int equivalent_class_generator = 0;

    eclassStr2equivalentClasses = new HashMap<String, EquivalentClass>();
    responseId2assessment = new HashMap<String, Assessment>();

    responseID2eclassID = new HashMap<String, String>();

    List<String> listAssessmentLines = new ArrayList<String>();
    BufferedReader reader = new BufferedReader(new FileReader(fileAssessment));
    String sline;
    while ((sline = reader.readLine()) != null) {
      listAssessmentLines.add(sline);
    }
    reader.close();

    Collections.sort(listAssessmentLines, new Comparator<String>() {
      @Override
      public int compare(String a1, String a2) {
        String[] fields1 = a1.split("\t");
        int hop1 = Integer.parseInt(fields1[1].substring(fields1[1].lastIndexOf("_") + 1));

        String[] fields2 = a2.split("\t");
        int hop2 = Integer.parseInt(fields2[1].substring(fields2[1].lastIndexOf("_") + 1));

        return hop1 - hop2;
      }
    });

    for (String assessmentLine : listAssessmentLines) {
      sline = assessmentLine;

      String equivalent_class;
      if (sline.endsWith("\t0")) {
        equivalent_class = Integer.toString(equivalent_class_generator++);
      } else {
        equivalent_class = sline.substring(sline.lastIndexOf("\t")).trim();
      }

      Assessment assessment = Assessment.fromLine(sline, equivalent_class);

      // loadCanonicalMention correct and missed answers (eclass)
      String equivalentClassID = assessment.getEquivalentClassStr(responseID2eclassID);
      EquivalentClass eclass = new EquivalentClass(equivalentClassID, assessment.hopId);

      if (!eclassStr2equivalentClasses.containsKey(equivalentClassID)) {
        eclassStr2equivalentClasses.put(equivalentClassID, eclass);
      }

      // TODO: fix this
      if(assessment instanceof CSAssessment)
        eclassStr2equivalentClasses.get(equivalentClassID).addAssessment((CSAssessment)assessment);

      responseId2assessment.put(assessment.responseId, assessment);

      responseID2eclassID.put(assessment.responseId, equivalentClassID);
    }
    reader.close();

    return eclassStr2equivalentClasses;
  }

}
