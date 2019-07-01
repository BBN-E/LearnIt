package com.bbn.akbc.kb;


import com.bbn.akbc.common.FileUtil;
import com.bbn.akbc.common.Justification;
import com.bbn.akbc.common.Pair;
import com.bbn.akbc.common.StringUtil;
import com.bbn.akbc.common.Triple;
import com.bbn.akbc.common.comparator.RelationMentionBrandyConfidenceComparator;
import com.bbn.akbc.common.format.DateKBPnormalizer;
import com.bbn.akbc.common.format.Normalization;
import com.bbn.akbc.evaluation.tac.CSAssessment;
import com.bbn.akbc.evaluation.tac.CSInitQuery;
import com.bbn.akbc.evaluation.tac.EquivalentClass;
import com.bbn.akbc.evaluation.tac.Query;
import com.bbn.akbc.evaluation.tac.RelationFromSubmission;
import com.bbn.akbc.evaluation.tac.io.AssessmentReader;
import com.bbn.akbc.evaluation.tac.io.QueryReader;
import com.bbn.akbc.kb.common.Slot;
import com.bbn.akbc.kb.common.SlotFactory;
import com.bbn.akbc.kb.facts.FactRelationMention;
import com.bbn.akbc.kb.text.TextEntity;
import com.bbn.akbc.kb.text.TextMention;
import com.bbn.akbc.kb.text.TextRelation;
import com.bbn.akbc.kb.text.TextRelationFactory;
import com.bbn.akbc.kb.text.TextRelationMention;
import com.bbn.akbc.kb.text.TextSpan;
import com.bbn.akbc.resource.Resources;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.io.SerifXMLLoader;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.Entity;
import com.bbn.serif.theories.EventMention;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.RelationMention;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.ValueMention;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
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
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class ColdStartKB {
  @JsonProperty
  String id;
  @JsonProperty
  Map<String, TextEntity> id2entities;
  @JsonProperty
  Set<TextRelation> listKBRelations;

  // cache from docID to relations
  Multimap<String, TextRelationMention> docId2relationMentions;

  @JsonCreator
  public ColdStartKB(
      @JsonProperty("id") String id,
      @JsonProperty("id2entities") Map<String, TextEntity> id2entities,
      @JsonProperty("listRelations") Set<TextRelation> listKBRelations) {
    this.id = id;
    this.id2entities = new HashMap<String, TextEntity>(id2entities);
    this.listKBRelations = new HashSet<TextRelation>(listKBRelations);
  }

  public Justification getJustification(TextRelationMention textRelationMention) {
    Justification justification = textRelationMention.listJustifications.get(textRelationMention.listJustifications.size()-1);
    return justification;
  }

  public void updateDocId2relationMentions() {
    docId2relationMentions = HashMultimap.create();

    for(TextRelation textRelation : listKBRelations) {
      for (TextRelationMention textRelationMention : textRelation.getMentions()) {
        Justification justification = textRelationMention.getJustificaion();
        docId2relationMentions.put(justification.docId, textRelationMention);
      }
    }
  }

  public void updateRelationMentionsWithFullText(String docId, String fulltext) {
    for(TextRelationMention textRelationMention : docId2relationMentions.get(docId)) {
      Justification justification = textRelationMention.getJustificaion();

      if(fulltext.length()>justification.span.getSecond()) {
        System.out.println("Update provenance for relation mention: " + textRelationMention.toSimpleString());
        String stringJustification = fulltext.substring(justification.span.getFirst(), justification.span.getSecond()+1);
        textRelationMention.setStringJustification(stringJustification);
      }
    }
  }

  public Map<String, TextEntity> getId2entities() {
    return this.id2entities;
  }

  public Collection<TextEntity> getEntities() {
    return this.id2entities.values();
  }

  public Set<TextRelation> getRelations() {
    return this.listKBRelations;
  }

  public String getId() {
    return this.id;
  }

  public Set<String> getStringCanonicalNames(TextMention kbpMention) {
    Set<String> stringCanonicalNames = new HashSet<String>();
    if(kbpMention.getEntityId().isPresent()) {
      if(id2entities.containsKey(kbpMention.getEntityId().get())) {
        TextEntity entity = id2entities.get(kbpMention.getEntityId().get());
        for(TextMention canonical_mention : entity.getCanonicalMentions())
          if(canonical_mention.getText().isPresent())
            stringCanonicalNames.add(canonical_mention.getText().get());
      }
    }

    return stringCanonicalNames;
  }

  public void addRelation(TextRelation KBRelation) {
    listKBRelations.add(KBRelation);
  }

  public Optional<String> getEntityIdByMention(TextMention KBMention) {
    for (String entityId : id2entities.keySet()) {
      if (id2entities.get(entityId).hasMention(KBMention)) {
        return Optional.of(entityId);
      }
    }

    return Optional.absent();
  }

  public Set<TextRelation> getRelationsByQuery(String queryId) {
    Set<TextRelation> filteredKBRelationSet = new HashSet<TextRelation>();

    for (TextRelation reln : listKBRelations) {
      if (reln.query().getId().equals(queryId)) {
        filteredKBRelationSet.add(reln);
      }
    }

    return filteredKBRelationSet;
  }

  public Collection<TextMention> getSystemMentionsForQuery(String queryId) {
    return id2entities.get(queryId).getMentions();
  }

  public void testChopList(int sizeChoppedList) {
    throw new RuntimeException("Not implemented yet. code refactoring in progress");
  }
        /*
        public void testChopList(int sizeChoppedList) {
		this.addInverse();
		try {
			this.applyInference("/nfs/mercury-04/u42/bmin/Git_repo/repo/kbp/coldstart/tmp/empty_file_of_inference_rules.txt", 1, Optional.of("/nfs/mercury-04/u42/bmin/Git_repo/repo/kbp/coldstart/tmp/dummy_file_of_inference_traces.html"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		eidToDocs.clear();
		eid1ToRelnToSetEid2.clear();
		relnToSrcEid.clear();
		eid2ToRelnToSetEid1.clear();
		relnToSrcEid.clear();
		id2entities.clear();

		System.out.println("listRelations.size(): " + listRelations.size());

		List<Relation> listRelationsNew = new ArrayList<Relation>();
		listRelationsNew.addAll(listRelations);
		listRelationsNew = listRelationsNew.subList(0, sizeChoppedList);
		listRelations.clear();
		listRelations.addAll(listRelationsNew);
	}
	*/

  public void printStats() {
    System.out.println("=============== System KB Info ===============");
    System.out.println("= # entities: " + this.id2entities.size());
    System.out.println("= # relnInfo: " + this.listKBRelations.size());
    System.out.println("==============================================");
  }

  public void writeToFile(String strFileKB) {
    writeToFile(strFileKB, false);
  }

  public void writeToFile(String strFileKB, boolean withAnnotation) {
    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter(new File(strFileKB)));

      List<String> id2entitiesKeys = new ArrayList<String>(id2entities.keySet());
      Collections.sort(id2entitiesKeys);
      for (String id : id2entitiesKeys) {
        TextEntity KBEntity = id2entities.get(id);
        writer.write(KBEntity.toColdStartString().replace("#", "_"));
      }

      List<String> relations = new ArrayList<String>();
      for (TextRelation KBRelation : listKBRelations) {
        if (SlotFactory.isVirtual(KBRelation.getSlot())) {
          continue; // virtual relations are for inference only
        }
        relations.add(KBRelation.toColdStartString(withAnnotation).replace("#", "_"));

        List<TextRelationMention> listRMs = new ArrayList<TextRelationMention>();
        listRMs.addAll(KBRelation.getMentions());
        Collections.sort(listRMs, new RelationMentionBrandyConfidenceComparator());

        for (TextRelationMention rm : listRMs) {
          System.out.println(
              "RM_size:\t" + KBRelation.getMentions().size() + "\t" + KBRelation.toSimpleString() + "\t" + rm
                  .toSimpleString());
        }

      }
      Collections.sort(relations);
      for (String s : relations) {
        writer.write(s);
      }

      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void writeOfficialSubmissionForColdStart2014(String strFileKB) {
    writeOfficialSubmissionForColdStart2014(strFileKB, false);
  }

  public void writeOfficialSubmissionForColdStart2014(String strFileKB, boolean writeSpecialPredicateForNominals) {
    writeOfficialSubmissionForColdStart2014(strFileKB, false, writeSpecialPredicateForNominals);
  }

  public void writeOfficialSubmissionForColdStart2014(String strFileKB,
      boolean writeOnlyInferredRelations,
      boolean writeSpecialPredicateForNominals) {
    writeOfficialSubmissionForColdStart2014("BBN1", strFileKB,
        writeOnlyInferredRelations,
        writeSpecialPredicateForNominals);
  }

  public void writeOfficialSubmissionForColdStart2014(String runID, String strFileKB,
      boolean writeOnlyInferredRelations,
      boolean writeSpecialPredicateForNominals) {
    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter(new File(strFileKB)));

      writer.write(runID + "\n");

      List<String> id2entitiesKeys = new ArrayList<String>(id2entities.keySet());
      Collections.sort(id2entitiesKeys);
      for (String id : id2entitiesKeys) {
        TextEntity KBEntity = id2entities.get(id);
        writer.write(KBEntity.toColdStart2014String(writeSpecialPredicateForNominals).replace("#", "_"));
      }

      List<String> relations = new ArrayList<String>();
      for (TextRelation KBRelation : listKBRelations) {
        if (SlotFactory.isVirtual(KBRelation.getSlot())) {
          continue; // virtual relations are for inference only
        }
        if (writeOnlyInferredRelations && !KBRelation.isInfered()) {
          System.out.println(
              "writeOfficialSubmissionForColdStart2014-1:\t" + writeOnlyInferredRelations + "\t"
                  + KBRelation.toSimpleString());
          continue; // only write inferred relations
        }

        System.out.println(
            "writeOfficialSubmissionForColdStart2014-2:\t" + writeOnlyInferredRelations + "\t"
                + KBRelation.toSimpleString());

        relations.add(KBRelation.toColdStart2014String().replace("#", "_"));
      }
      Collections.sort(relations);
      for (String s : relations) {
        writer.write(s);
      }

      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void writeOfficialSubmissionForColdStart2017(String runID,
      String strFileKB,
      boolean writeOnlyInferredRelations,
      boolean writeSpecialPredicateForNominals) {
    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter(new File(strFileKB)));

      writer.write(runID + "\n");

      List<String> id2entitiesKeys = new ArrayList<String>(id2entities.keySet());
      Collections.sort(id2entitiesKeys);
      for (String id : id2entitiesKeys) {
        TextEntity KBEntity = id2entities.get(id);
        writer.write(KBEntity.toColdStart2014String(writeSpecialPredicateForNominals).replace("#", "_"));
      }

      List<String> relations = new ArrayList<String>();
      for (TextRelation KBRelation : listKBRelations) {
        if (SlotFactory.isVirtual(KBRelation.getSlot())) {
          continue; // virtual relations are for inference only
        }
        if (writeOnlyInferredRelations && !KBRelation.isInfered()) {
          System.out.println(
              "writeOfficialSubmissionForColdStart2017-1:\t" + writeOnlyInferredRelations + "\t"
                  + KBRelation.toSimpleString());
          continue; // only write inferred relations
        }

        System.out.println(
            "writeOfficialSubmissionForColdStart2017-2:\t" + writeOnlyInferredRelations + "\t"
                + KBRelation.toSimpleString());

        relations.add(KBRelation.toColdStart2017String().replace("#", "_"));
      }
      Collections.sort(relations);
      for (String s : relations) {
        writer.write(s);
      }

      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void writeAnnotationToFile(String strFile) {
    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter(new File(strFile)));

			/*
                        for(String id : id2entities.keySet()) {
				EntityInfo entityInfo = id2entities.get(id);
				writer.write(entityInfo.toColdStartString().replace("#", "_"));
			}
			*/

      for (TextRelation KBRelation : listKBRelations) {
        if (SlotFactory.isVirtual(KBRelation.getSlot())) {
          continue; // virtual relations are for inference only
        }

        for (TextRelationMention rm : KBRelation.getMentions()) {
          if (rm.judgementOnQuery.isPresent() || rm.judgementOnRelation.isPresent()) {
            writer.write(rm.toStringAssessMent() + "\n");
          }
        }
      }

      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  public void addInverse() {
    Set<TextRelation> listRelationsNew = new HashSet<TextRelation>();
    listRelationsNew.addAll(listKBRelations);

    // add reverse relations
    for (TextRelation KBRelationInfo : listKBRelations) {
//			System.out.println(relationInfo.toSimpleString());
      Optional<TextRelation> inverseRelnMention = TextRelationFactory.getReverseRelation(KBRelationInfo);

      if (inverseRelnMention.isPresent()) {
        listRelationsNew.add(inverseRelnMention.get());
      }
    }
    listKBRelations = listRelationsNew;
  }

  private BiMap<String, String> loadSysEntityToQueryMapping(String fileEvalLog) {
    BiMap<String, String> sysEntity2query = HashBiMap.create();

    try {
      String sline;
      BufferedReader reader = new BufferedReader(new FileReader(fileEvalLog));
      while ((sline = reader.readLine()) != null) {
        String[] fields = sline.trim().split("\t");
        if (fields.length != 3) {
          continue;
        }

        if (!fields[0].equals("dbg")) {
          continue;
        }

        sysEntity2query.put(fields[2], fields[1] + "_00"); // append hop info for initial queries
      }
      reader.close();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    return sysEntity2query;
  }

  static int getSentenceId(DocTheory dt, int offset) {
    for (int sid = 0; sid < dt.sentenceTheories().size(); sid++) {
      SentenceTheory s = dt.sentenceTheories().get(sid);

      int start = s.span().startCharOffset().value();
      int end = s.span().endCharOffset().value();

      if (offset >= start && offset <= end) {
        return sid;
      }
    }

    return -1;
  }

  static Entity getEntityByCharOffsets(DocTheory dt, int start, int end) {
    for (Entity e : dt.entities().asList()) {
      for (Mention m : e.mentions()) {
        int mention_start = m.span().startCharOffset().value();
        int mention_end = m.span().endCharOffset().value();

        if (overlap(start, end, mention_start, mention_end)) {
          return e;
        }
      }
    }

    return null;
  }

  static boolean overlap(int start1, int end1, int start2, int end2) {
    if (start1 <= start2) {
      if (end1 >= start2) {
        return true;
      } else {
        return false;
      }
    } else {
      if (end2 >= start1) {
        return true;
      } else {
        return false;
      }
    }
  }

  static boolean matchByCoref(String docId,
      TextSpan span1, TextSpan span2,
      int thres_num_sentences_apart) {

    int start1 = span1.getStart();
    int end1 = span1.getEnd();
    int start2 = span2.getStart();
    int end2 = span2.getEnd();

    String strPathSerifXml = Resources.getPathSerifXml() + docId + ".xml";
    try {
      SerifXMLLoader fromXML = SerifXMLLoader.fromStandardACETypes();
      DocTheory dt = fromXML.loadFrom(new File(strPathSerifXml));

      int sid1 = getSentenceId(dt, start1);
      int sid2 = getSentenceId(dt, start2);

      if (sid1 == -1 || sid2 == -1 || Math.abs(sid1 - sid2) > thres_num_sentences_apart) {
        return false;
      }

      Entity e1 = getEntityByCharOffsets(dt, start1, end1);
      Entity e2 = getEntityByCharOffsets(dt, start2, end2);

      if (e1 == null || e2 == null) {
        return false;
      }

      if (e1.equals(e2)) {
        return true;
      } else {
        return false;
      }

    } catch (IOException e) {
      e.printStackTrace();
      //code to handle an IOException here
      System.out.println("Exception in reading: " + strPathSerifXml);
    }

    return false;
  }


  // queryId+docId -> list of assessments: for fast alignment
  static Multimap<String, CSAssessment> queryIdAndDocId2listOfAssessments =
      ArrayListMultimap.create();

  static Optional<CSAssessment> findAlignedAssessment(String queryId, TextRelationMention rm) {
    String strKey = queryId + "|" + rm.slot.toString() + "|" + rm.getDocId();
    for (CSAssessment assessment : queryIdAndDocId2listOfAssessments
        .get(strKey)) { // match queryId + docId
      if (assessment.spanOfFiller.overlapWith(rm.answer.getSpan()))
      //	if(matchByCoref(rm.getDocId(), assessment.spanOfFiller, rm.answer.span, 5))
      {
        return Optional.of(assessment);
      }
    }

    return Optional.absent();
  }

  public void injectQueryAndAssessmentsAndEquivalentClasses(String strFileAssessment,
      String strFileEvalLog, String strFileQuery) {
    injectQueryAndAssessmentsAndEquivalentClasses(strFileAssessment, strFileEvalLog, strFileQuery,
        false);
  }

  public void injectQueryAndAssessmentsAndEquivalentClasses(String strFileAssessment,
      String strFileEvalLog, String strFileQuery,
      boolean addMissesFromAssessment) {
    // read assessment
    List<CSAssessment> listAssessments =
        AssessmentReader.readCSAssessmentsFromFile(strFileAssessment);
    for (CSAssessment assessment : listAssessments) {
      String strKey = assessment.queryId + "|" + assessment.slot + "|" + assessment.docid;
      ColdStartKB.queryIdAndDocId2listOfAssessments
          .put(strKey, assessment); // TODO: put more judgement fields

      System.out.println("Assessment: strKey=" + strKey + "\t" + assessment.toString());
    }

    Set<CSAssessment> setOfAssessmentsFound = new HashSet<CSAssessment>();

    // read queries
    List<Query> listQueries = QueryReader.readQueriesFromFile(strFileQuery);
    Map<String, Query> id2query = Maps.newHashMap();
    for (Query query : listQueries) {
      id2query.put(query.id, query);
    }

    // loadCanonicalMention alignment query -> sysEntity
    BiMap<String, String> sysEntity2query = loadSysEntityToQueryMapping(strFileEvalLog);

    // attach queries & judgements to the system KB
    for (TextRelation KBRelation : this.listKBRelations) {
      String sysEntityId = KBRelation.query().getId();
      if (sysEntity2query.containsKey(sysEntityId)) { // found query entity
        String queryId = sysEntity2query.get(sysEntityId);
        KBRelation.setEvalQuery(
            (CSInitQuery) id2query.get(queryId)); // set query entity on query of a relation

        for (TextRelationMention rm : KBRelation.getMentions()) {
          Optional<CSAssessment> assessment = ColdStartKB.findAlignedAssessment(queryId, rm);
          if (assessment.isPresent()) {
            System.out.println("[dbg] Found an aligned assessment for query: " + queryId);
            rm.judgementOnRelation =
                Optional.of(assessment.get().judgement1); // set judgement on relation mention
            rm.answer.setIdEquivalentClass(assessment.get().equivalent_class_id);

            setOfAssessmentsFound.add(assessment.get());
          }
        }
      }
    }

    // add missed relation mentions from assessments
    if (addMissesFromAssessment) {
      List<TextRelation> listMissedKBRelations =
          constructListOfRelationsFromMissingAnswers(listAssessments, setOfAssessmentsFound,
              sysEntity2query);

      for (TextRelation r : listMissedKBRelations) {
        this.addRelation(r);
      }
    }

  }

  public List<TextRelation> constructListOfRelationsFromMissingAnswers(
      List<CSAssessment> listAssessments, Set<CSAssessment> setOfAssessmentsFound,
      BiMap<String, String> sysEntity2query) {
    List<TextRelation> listKBRelations = new ArrayList<TextRelation>();

    // eclassId 2 eclass
    Map<String, EquivalentClass> eclassStr2equivalentClasses =
        new HashMap<String, EquivalentClass>();
    Map<String, String> eclassStr2eid = new HashMap<String, String>();

    Map<String, String> responseId2eclassId = new HashMap<String, String>();
    for (CSAssessment assessment : listAssessments) {
      String equivalent_class = Integer.toString(assessment.equivalent_class_id);

      responseId2eclassId.put(assessment.responseId, equivalent_class);

      EquivalentClass eclass = new EquivalentClass(equivalent_class, assessment.hopId);
      if (!eclassStr2equivalentClasses.containsKey(equivalent_class)) {
        eclassStr2equivalentClasses.put(equivalent_class, eclass);
      }
      eclassStr2equivalentClasses.get(equivalent_class).addAssessment(assessment);

      TextMention fillerMention = new TextMention(assessment.docid,
          assessment.spanOfFiller.getStart(), assessment.spanOfFiller.getEnd());
      Optional<String> idTmp = getEntityIdByMention(fillerMention);

      if (idTmp.isPresent()) {
        eclassStr2eid.put(equivalent_class, idTmp.get());
      }
    }

    List<EquivalentClass> listEquivalentClass = new ArrayList<EquivalentClass>();
    listEquivalentClass.addAll(eclassStr2equivalentClasses.values());
    Collections.sort(listEquivalentClass, new Comparator<EquivalentClass>() {
      @Override
      public int compare(EquivalentClass arg0, EquivalentClass arg1) {
        return arg0.hop - arg1.hop;
      }
    });

    // generate relations from missing equivalent classes
    for (EquivalentClass eclass : listEquivalentClass) {
      boolean isFoundInSysKB = false;
      boolean isCorrect = true;
      for (CSAssessment assessment : eclass.listAssessment) {
        if (setOfAssessmentsFound.contains(assessment)) {
          isFoundInSysKB = true;
        }

        if (!assessment.isCorrect()) {
          isCorrect = false;
        }
      }

      if (!isFoundInSysKB && isCorrect) {
        for (CSAssessment assessment : eclass.listAssessment) {
          Optional<String> eid1 = Optional.absent();
          String queryId1;

          if (assessment.parentId.equals("NIL")) {
            queryId1 = assessment.queryId;
            eid1 = Optional.of(sysEntity2query.inverse().get(queryId1));
          } else {
            String eclassId = responseId2eclassId.get(assessment.parentId);
            if (eclassStr2eid.containsKey(eclassId)) {
              eid1 = Optional.of(eclassStr2eid.get(eclass.id));
            } else {
              continue; // entry point not found
            }
          }

          if (!eid1.isPresent()) {
            continue; // entry point not found
          }

          TextEntity e1 = id2entities.get(eid1);

          TextEntity e2 = null;
          String eid2 = "CS_ANSWER_E_" + eclass.id;
          TextMention fillerMention = new TextMention(assessment.docid,
              assessment.spanOfFiller.getStart(), assessment.spanOfFiller.getEnd());
          fillerMention.setText(assessment.text);
          Optional<String> fillerId = getEntityIdByMention(fillerMention);
          if (fillerId.isPresent()) {
            eid2 = fillerId.get();
            e2 = this.id2entities.get(eid2);
          } else {
            e2 = new TextEntity(eid2);
            e2.addMention(fillerMention);
          }

          TextRelation KBRelation = new TextRelation(e1, e2, assessment.slot);
          TextRelationMention rm =
              new TextRelationMention(new TextMention(e1), fillerMention, assessment.slot);
          rm.setHopId(assessment.hopId);
          rm.setJudgement(assessment.judgement1);
          KBRelation.addMention(rm);

          listKBRelations.add(KBRelation);

//					id2relations.get(relation.hashCode()).addMention(rm);
        }
      }
    }

    return listKBRelations;
  }

  public static class Builder {

    String id;
    private final ImmutableMultimap.Builder<String, TextEntity> id2entitiesBuilder;
    private final ImmutableSet.Builder<TextRelation> listRelationsBuilder;

    public Builder(String id) {
      this.id = id;
      this.id2entitiesBuilder = new ImmutableMultimap.Builder<String, TextEntity>();
      this.listRelationsBuilder = new ImmutableSet.Builder<TextRelation>();
    }

    public Builder withAddedEntity(TextEntity entity) {
      this.id2entitiesBuilder.put(entity.getId(), entity);
      return this;
    }

    public Builder withAddedRelation(TextRelation KBRelation) {
      this.listRelationsBuilder.add(KBRelation);
      return this;
    }

    public Builder withAddedRelationAll(Set<TextRelation> KBRelations) {
      this.listRelationsBuilder.addAll(KBRelations);
      return this;
    }

    public Builder withAddedEntityAll(Map<String, TextEntity> id2entities) {
      for (String id : id2entities.keySet()) {
        this.id2entitiesBuilder.put(id, id2entities.get(id));
      }
      return this;
    }

    public ColdStartKB build() {
      ImmutableMap.Builder<String, TextEntity> id2entitiesMapBuilder =
          new ImmutableMap.Builder<String, TextEntity>();
      ImmutableMultimap<String, TextEntity> id2entities = id2entitiesBuilder.build();
      for (String id : id2entities.keySet()) {
        TextEntity entityMerged = new TextEntity(id);
        for (TextEntity e : id2entities.get(id)) {
          for (String type : e.getTypes()) {
            entityMerged.addType(type);
          }
          for (TextMention m : e.getMentions()) {
            entityMerged.addMention(m);
          }
          for (TextMention m : e.getCanonicalMentions()) {
            entityMerged.addCanonicalMention(m);
          }
        }

        // select the global canonical mention
        entityMerged.selectGlobalCanonicalMention();

        id2entitiesMapBuilder.put(id, entityMerged);
      }

      return new ColdStartKB(id, id2entitiesMapBuilder.build(), listRelationsBuilder.build());
//			return new CSInitQuery(id, mentionListBuilder.build(), slotsHop0Builder.build(), slotsHop1Builder.build());
    }

    public String getValueID(String value) {
      return "VAL:" + value.toLowerCase().trim().replace(" ", "_");
    }

    String getRelationSignature(TextRelation r) {
      if (r.isValSlot()) {
        String strDst = getValueID(r.value().getText().get());
        Triple triple = new Triple(r.query().getId(), r.getSlot().toString(), strDst);
        return triple.toString();
      } else {
        Triple triple = new Triple(r.query().getId(), r.getSlot().toString(), r.answer().getId());
        return triple.toString();
      }
    }

    String escapeQuoteAndBackslash(String mentionText) {
      String resultString = mentionText.replaceAll("\"", "\\\"").replace("\\", "\\\\");

      if (!mentionText.equals(resultString)) {
        System.out.println("escapeQuoteAndBackslash:\t" + mentionText + "\t->\t" + resultString);
      }

      return resultString;
    }

    public ColdStartKB fromSubmissionKB(String strFileKB) {
      return fromSubmissionKB(strFileKB, false);
    }

    public ColdStartKB fromSubmissionKB(String strFileKB, boolean adjust_offsets_for_adept) {
      CsSubmissionKB csSubmissionKB = new CsSubmissionKB();
      csSubmissionKB.loadFromFile(strFileKB, adjust_offsets_for_adept);

      Map<String, TextEntity> cacheEid2entities = csSubmissionKB.getId2entities();

      // for value relations, TAC requires the first provenance to be the filler span, therefore we need to take a longer span to display provenance
      // this in general also helps to remove provenances that are not useful (as a sub-span of another)
      for (String eid : cacheEid2entities.keySet()) {
//      System.out.println("dbg2:\t" + eid);
        this.withAddedEntity(cacheEid2entities.get(eid));
      }
      for(RelationFromSubmission relationFromSubmission : csSubmissionKB.getRelations()) {
        // System.out.println("relationFromSubmission=" + relationFromSubmission);
        TextRelation kbpRelation = relationFromSubmission.toKBPRelation(cacheEid2entities);
        this.withAddedRelation(kbpRelation);
      }

      return this.build();
    }

    public ColdStartKB fromSubmissionKB(String strFileKB, boolean adjust_offsets_for_adept, boolean update_cache_docid2relations) {
      ColdStartKB kb = fromSubmissionKB(strFileKB, adjust_offsets_for_adept);
      if(update_cache_docid2relations)
        kb.updateDocId2relationMentions();
      return kb;
    }

    public ColdStartKB fromFile(String strFileKB) {
      return fromFile(strFileKB, false);
    }

    public ColdStartKB fromFile(String strFileKB, boolean adjust_offsets_for_adept, boolean update_cache_docid2relations) {
      ColdStartKB kb = fromFile(strFileKB, adjust_offsets_for_adept);
      if(update_cache_docid2relations)
        kb.updateDocId2relationMentions();
      return kb;
    }

    public ColdStartKB fromFile(String strFileKB, boolean adjust_offsets_for_adept) {
      Map<String, TextEntity> cacheEid2entities = new HashMap<String, TextEntity>();
      Map<String, TextRelation> cacheSig2Relations = new HashMap<String, TextRelation>();

      try {
        BufferedReader reader = new BufferedReader(new FileReader(strFileKB));
        int nLine = 0;
        String sline;
        while ((sline = reader.readLine()) != null) {
//          try {
            if (nLine++ % 100000 == 0) {
              System.out.println("# lines read: " + nLine);
            }

            String[] fields = sline.trim().split("\t");

            String predicate = fields[1];

            if (predicate.equals("type") || predicate.equals("mention") || predicate
                .equals("canonical_mention")) {
              String srcId = fields[0];
              srcId = Normalization.convertIDtoOnlyHaveAsciiCharacter(srcId); // normalize for submission

              if (!cacheEid2entities.containsKey(srcId)) {
                cacheEid2entities.put(srcId, new TextEntity(srcId));
              }

              if (predicate.equals("type")) {
                cacheEid2entities.get(srcId).addType(fields[2].toLowerCase());
              } else if (predicate.equals("mention")) {
                if (!StringUtil.isNumeric(fields[4]) || !StringUtil.isNumeric(fields[5])) {
                  throw new RuntimeException("Invalid line: " + sline);
                }

                TextMention mention;
                if(adjust_offsets_for_adept)
                  mention = new TextMention(fields[3], Integer.parseInt(fields[4]),
                    Integer.parseInt(fields[5])+1);
                else
                  mention = new TextMention(fields[3], Integer.parseInt(fields[4]),
                      Integer.parseInt(fields[5]));

                String mentionText = fields[2]
                    .substring(1, fields[2].length() - 1); // for mentions, already stripped ""
                mentionText = escapeQuoteAndBackslash(mentionText); // TODO: test
                mention.setText(mentionText);
                mention.setEntityId(srcId);

                mention.setConfidence(Double.parseDouble(fields[6]));
                mention.setLinkConfidence(Double.parseDouble(fields[7]));
                mention.setBrandyConfidence(fields[8].trim());

                if(fields.length>=10) {
                  // new for cs2016
                  if (fields[9].trim().equals("1"))
                    mention.setIsPlural(true);
                  else
                    mention.setIsPlural(false);
                  mention.setType(fields[10].trim());
                  mention.setSubType(fields[11].trim());
                }
                //

                cacheEid2entities.get(srcId).addMention(mention);

              } else if (predicate.equals("canonical_mention")) {

                if (!StringUtil.isNumeric(fields[4]) || !StringUtil.isNumeric(fields[5])) {
                  throw new RuntimeException("Invalid line: " + sline);
                }

                TextMention mention;
                if(adjust_offsets_for_adept)
                   mention = new TextMention(fields[3],
                       Integer.parseInt(fields[4]),
                       Integer.parseInt(fields[5])+1);
                else
                  mention = new TextMention(fields[3],
                      Integer.parseInt(fields[4]),
                      Integer.parseInt(fields[5]));

                String mentionText = fields[2]
                    .substring(1, fields[2].length() - 1); // for mentions, already stripped ""
                mentionText = escapeQuoteAndBackslash(mentionText); // TODO: test
                mention.setText(mentionText);
                mention.setEntityId(srcId);

                mention.setBrandyConfidence("AnyName");
                cacheEid2entities.get(srcId).addCanonicalMention(mention);
              }
            } else {
              FactRelationMention factRelationMention = FactRelationMention.fromLine(sline, adjust_offsets_for_adept);

              if (!cacheEid2entities.containsKey(factRelationMention.getSrcEntityId())) {
                System.out.println("SKIP:\tCannot find EntityInfo for srcEntityId: " + factRelationMention.getSrcEntityId() + "\t" + sline);
                continue;
              }

              TextEntity query = cacheEid2entities.get(factRelationMention.getSrcEntityId());

              Optional<TextRelationMention> kbRelationMentionOptional = factRelationMention.toKBRelationMention();
              if(!kbRelationMentionOptional.isPresent())
                continue;

              // update relation map
              TextRelation KBRelation = null;
              if (!factRelationMention.getIsValue()) {
                TextEntity answer = cacheEid2entities.get(factRelationMention.getDstEntityId()); // KBmention

                if (!cacheEid2entities.containsKey(factRelationMention.getDstEntityId())) {
                  System.out.println(
                      "SKIP:\tCannot find EntityInfo for dstEntityId: " + factRelationMention.getDstEntityId() + "\t"
                          + sline);
                  continue;
                }

                // skip relation between itself
                if (query.equals(answer)) {
                  System.out.println("SKIP:\tquery&answer are the same entity\t" + sline);
                  continue;
                }

                KBRelation = new TextRelation(query, answer, kbRelationMentionOptional.get().getSlot());
              } else {
                TextMention value = kbRelationMentionOptional.get().getAnswer();

                Optional<String> normalizedDate = factRelationMention.getNormalizedDate();
                if (normalizedDate.isPresent()) // use normalized version of the date
                {
                  value.setText(
                      normalizedDate.get().substring(1, normalizedDate.get().length() - 1));
                } else {
                  value.setText(factRelationMention.getDstEntityId()
                      .substring(1, factRelationMention.getDstEntityId().length() - 1)); // dstEntityId is text; strip ""
                }

                KBRelation = new TextRelation(query, value, kbRelationMentionOptional.get().getSlot());
              }
              if(factRelationMention.getIsValue())
                KBRelation.setToValueSlot();

              String strRelnSig = getRelationSignature(KBRelation);
//            System.out.println("getRelationSignature:\t" + strRelnSig + "\t" + KBRelation.toSimpleString());
              if (!cacheSig2Relations.containsKey(strRelnSig))
              // cacheSig2Relations.put(relation.hashCode(), relation);
              {
                cacheSig2Relations.put(strRelnSig, KBRelation);
              } else
              // relation = cacheSig2Relations.get(relation.hashCode());
              {
                KBRelation = cacheSig2Relations.get(strRelnSig);
              }
              //

              KBRelation.addMention(kbRelationMentionOptional.get());
              if (!KBRelation.mentionCount.isPresent()) {
                KBRelation.mentionCount = Optional.of(1);
              } else {
                KBRelation.mentionCount = Optional.of(KBRelation.mentionCount.get() + 1);
              }
              if (!KBRelation.sourcePatterns.isPresent()) {
                HashSet<String> setPatterns = new HashSet<String>();
                setPatterns.add(kbRelationMentionOptional.get().sourcePattern.get());
                KBRelation.sourcePatterns = Optional.of(setPatterns);
              } else {
                HashSet<String> setPatterns = KBRelation.sourcePatterns.get();
                setPatterns.add(kbRelationMentionOptional.get().sourcePattern.get());
                KBRelation.sourcePatterns = Optional.of(setPatterns);
              }

//            System.out.println("DateDbg: " + kbRelationMentionOptional.get().toSimpleString());

              // relation.isValueSlot = Optional.of(isValue);
              if(factRelationMention.getIsValue())
                KBRelation.setToValueSlot();

              // for debug
              if (KBRelation.getSlot().toString().equals("per:title") && KBRelation.query().getId()
                  .equals(":PER_Actor_521867") && KBRelation.isValSlot() == false) {
                System.out.println(
                    KBRelation.getSlot().toString() + ", "
                        + KBRelation.query().getId() + ", "
                        + KBRelation.isValSlot() + "\t" +
                        "error in line: " + sline);
              }

              if (KBRelation.getSlot().toString().equals("per:employee_or_member_of")
                  && KBRelation.isValSlot() == true) {
                System.out.println(
                    KBRelation.getSlot().toString() + ", "
                        + KBRelation.query().getId() + ", "
                        + KBRelation.isValSlot()
                        + "\t" +
                        "error in line: " + sline);
              }

              //

//						this.withAddedRelation(relation);
            }
          /*
          } catch(ArrayIndexOutOfBoundsException ae) {
            ae.printStackTrace();
            System.out.println("error_line=" + sline);
          }
          */
        }
        reader.close();
      } catch (Exception e) {
        e.printStackTrace();
      }

      //		Map<String, EntityInfo> cacheEid2entities = new HashMap<String, EntityInfo>();
      for (String eid : cacheEid2entities.keySet()) {
        this.withAddedEntity(cacheEid2entities.get(eid));
      }
      for (TextRelation KBRelation : cacheSig2Relations.values()) {
        this.withAddedRelation(KBRelation);
      }

      return this.build();
    }


    public Optional<String> getNormalizedDate(Slot slot, String filler) {
      Optional<String> normalizedDate = Optional.absent();
      if (SlotFactory.isDateSlot(slot)) {
        Optional<String> strNormalizedDate = DateKBPnormalizer.normalizeDate(filler);
        if (!strNormalizedDate.isPresent()) {
          System.out.println("DateKBPnormalizer:\tREMOVE\t" + filler);
          return Optional.absent();
        } else {
          System.out.println(
              "DateKBPnormalizer:\tNORMALIZE\t" + filler + "\t" + strNormalizedDate.get());
          filler = strNormalizedDate.get();

          normalizedDate = Optional.of(strNormalizedDate.get());
        }
      }

      return normalizedDate;
    }

    public List<String> readTitlesFromList(){
      List<String> titles = new ArrayList<String>();

      String strFileTitleList = "/nfs/mercury-04/u42/bmin/repositories/git/local/akbc/end-to-end-relation-finding-sequence-for-cs2017/resources/titles.translations.selected.titleStrings";
      List<String> lines = FileUtil.readLinesIntoList(strFileTitleList);
      for(String line :lines) {
        line = line.trim();
        if(line.isEmpty() || line.startsWith("#"))
          continue;

        titles.add(line);

        titles.add("助理" + line);
        titles.add("实习" + line);
        titles.add("资深" + line);
        titles.add("高级" + line);
        titles.add("副" + line);
      }

      Collections.sort(titles, new StringLengthComparator());
      /*************************************/
      System.out.println("Reading titles from: " + strFileTitleList);
      for(String title : titles) {
        System.out.println("title\t" + title);
      }
      System.out.println();
      /*************************************/

      return titles;
    }

    public List<String> readTopTitlesFromList() {
      List<String> titles = new ArrayList<String>();

      String strFileTitleList = "/nfs/mercury-04/u42/bmin/repositories/git/local/akbc/end-to-end-relation-finding-sequence-for-cs2017/resources/titles.translations.selected.titleStrings.top_member_employee";
      List<String> lines = FileUtil.readLinesIntoList(strFileTitleList);
      for(String line :lines) {
        line = line.trim();
        if(line.isEmpty() || line.startsWith("#"))
          continue;

        titles.add(line);
      }

      Collections.sort(titles, new StringLengthComparator());

      return titles;
    }

    /*
    public static Optional<Pair<Integer, Integer>> findMatchedSubStringSpanOffSet(String subStringNoSpace, String rawStringWithSpace,
        boolean backward) {
      Optional<Pair<Integer, Integer>> spanOffsetOptional = Optional.absent();

      System.out.println("subStringNoSpace: " + subStringNoSpace);
      System.out.println("rawStringWithSpace: " + rawStringWithSpace);

      StringBuilder sb = new StringBuilder();

      // get the mapping from no-space-index to the raw index
      Map<Integer, Integer> noSpaceIdxToRawIdx = new HashMap<Integer, Integer>();
      int rawIdx=0;
      int noSpaceIdx=0;
      for(char c : rawStringWithSpace.toCharArray()) {
        if(c!=' ') {
          noSpaceIdxToRawIdx.put(noSpaceIdx, rawIdx);
          noSpaceIdx++;
        }
        rawIdx++;
      }

      for(int i : noSpaceIdxToRawIdx.keySet())
        sb.append(i + " -> " + noSpaceIdxToRawIdx.get(i) + " ");
      System.out.println("noSpaceIdxToRawIdx: " + sb.toString().trim());

      String stringWithNoSpace = rawStringWithSpace.replace(" ", "");
      int charStartInString = stringWithNoSpace.indexOf(subStringNoSpace);
      if(backward)
        charStartInString = stringWithNoSpace.lastIndexOf(subStringNoSpace);

      if(charStartInString>0) {
        System.out.println("Look up charStartInString: " + charStartInString);
        System.out.println("Look up charStartInString+stringWithNoSpace.length()-1): " + (charStartInString+stringWithNoSpace.length()-1));

        System.out.println("Found " + subStringNoSpace + " in " + rawStringWithSpace
            + " , offsets: <" + noSpaceIdxToRawIdx.get(charStartInString) + ", " + noSpaceIdxToRawIdx.get(charStartInString+subStringNoSpace.length()-1) + ">");

        spanOffsetOptional = Optional.of(new Pair<Integer, Integer>(noSpaceIdxToRawIdx.get(charStartInString), noSpaceIdxToRawIdx.get(charStartInString+subStringNoSpace.length()-1))); // both inclusive
      }

      return spanOffsetOptional;
    }
*/

    public static Optional<Pair<String, Pair<Integer, Integer>>> findTitlesInTheMention(DocTheory docTheory, Mention mention, List<String> titles) {
      int MAX_DISTANCE_TO_NAME=1;

      // String mentionHeadText = mention.atomicHead().span().tokenizedText(docTheory);
      // String mentionText = mention.span().tokenizedText(docTheory);
      String mentionHeadText = mention.atomicHead().span().originalText().content().utf16CodeUnits(); //(docTheory);
      String mentionText = mention.span().originalText().content().utf16CodeUnits(); //tokenizedText(docTheory);


      // remove white space
      // mentionText = mentionText.replace(" ", "");
      // mentionHeadText = mentionHeadText.replace(" ", "");

      // first one
      // titles must be sorted inversely by length
      for(String title : titles) {
        Optional<Pair<Integer, Integer>> charOffsetPairOptional = Optional.absent();
        if (mentionHeadText.contains(title)) {
          // Optional<Pair<Integer, Integer>> spanOffsetOptional = findMatchedSubStringSpanOffSet(title, mentionHeadText, true);
          Optional<Pair<Integer, Integer>> spanOffsetOptional = Optional.of(new Pair<Integer, Integer>(mentionHeadText.indexOf(title), mentionHeadText.indexOf(title)+title.length()-1));
          if(spanOffsetOptional.isPresent()) {
            int charOffsetStart = mention.atomicHead().span().charOffsetRange().startInclusive().asInt();
            int charOffsetEnd = mention.atomicHead().span().charOffsetRange().endInclusive().asInt();

            if(charOffsetEnd-charOffsetStart-spanOffsetOptional.get().getSecond()<=MAX_DISTANCE_TO_NAME) {

              System.out.println("Found title " + title + " in mentionHeadText " + mentionHeadText
                  + " , offsets: <" + charOffsetStart + spanOffsetOptional.get().getFirst() + ", " + charOffsetStart + spanOffsetOptional.get().getSecond() + ">");

              charOffsetPairOptional = Optional.of(new Pair<Integer, Integer>(
                  charOffsetStart + spanOffsetOptional.get().getFirst(),
                  charOffsetStart + spanOffsetOptional.get().getSecond()));
            }
          }
        }
        else if(mentionText.contains(title)) {
          // Optional<Pair<Integer, Integer>> spanOffsetOptional = findMatchedSubStringSpanOffSet(title, mentionText, true);
          Optional<Pair<Integer, Integer>> spanOffsetOptional = Optional.of(new Pair<Integer, Integer>(mentionText.indexOf(title), mentionText.indexOf(title)+title.length()-1));
          if(spanOffsetOptional.isPresent()) {
            int charOffsetStart = mention.span().charOffsetRange().startInclusive().asInt();
            int charOffsetEnd = mention.span().charOffsetRange().endInclusive().asInt();

            if(charOffsetEnd-charOffsetStart-spanOffsetOptional.get().getSecond()<=MAX_DISTANCE_TO_NAME) {
              System.out.println("charOffsetStart: " + charOffsetStart);
              System.out.println("spanOffsetOptional.get().getFirst(): " + spanOffsetOptional.get().getFirst());
              System.out.println("spanOffsetOptional.get().getFirst(): " + spanOffsetOptional.get().getFirst());

              System.out.println("Found title " + title + " in mentionText " + mentionText
                  + " , offsets: <" + charOffsetStart + spanOffsetOptional.get().getFirst() + ", " + charOffsetStart + spanOffsetOptional.get().getSecond() + ">");

              charOffsetPairOptional = Optional.of(new Pair<Integer, Integer>(
                  charOffsetStart + spanOffsetOptional.get().getFirst(),
                  charOffsetStart + spanOffsetOptional.get().getSecond()));
            }
          }
        }

        if(charOffsetPairOptional.isPresent()) {
          return Optional.of(new Pair<String, Pair<Integer, Integer>>(title, charOffsetPairOptional.get()));
        }
      }

      return Optional.absent();
    }

    public Multimap<TextRelation, TextRelationMention> getPerTitleRelationMentionsfromDocTheory (DocTheory docTheory, List<String> titles,
        Map<String, TextEntity> cacheEid2entities,
        DocEntityToKbEntityAligner docEntityToKbEntityAligner) {

      Multimap<TextRelation, TextRelationMention> relation2relationMention = HashMultimap.create();

      for(Entity entity : docTheory.entities()) {
        // left entity
        Optional<String> kbIdLeftEntity = docEntityToKbEntityAligner
            .getKbIdForEntity(docTheory.docid().asString(), entity, Optional.of(docTheory));
        if(!kbIdLeftEntity.isPresent())
          continue;

        Optional<TextEntity> leftEntityOptional = Optional.absent();
        if(cacheEid2entities.containsKey(kbIdLeftEntity.get()))
          leftEntityOptional = Optional.of(cacheEid2entities.get(kbIdLeftEntity.get()));

        // representative mention to TextMention
        TextMention representativeMention = new TextMention(docTheory.docid().asString(),
            entity.representativeMention().mention().atomicHead().span().charOffsetRange().startInclusive().asInt(),
            entity.representativeMention().mention().atomicHead().span().charOffsetRange().endInclusive().asInt());
        representativeMention.setText(entity.representativeMention().mention().atomicHead().span().originalText().content().utf16CodeUnits()); // TODO:check if this is correct text for mention
        representativeMention.setConfidence(1.0);
        representativeMention.setBrandyConfidence(entity.representativeMention().mention().mentionType().name());

        for(Mention mention :entity.mentions()) {
          String mentionHeadText = mention.atomicHead().span().originalText().content().utf16CodeUnits(); //(docTheory);
          String mentionText = mention.span().originalText().content().utf16CodeUnits(); //tokenizedText(docTheory);

          System.out.println("mentionHeadText: " + mentionHeadText);
          System.out.println("mentionText: " + mentionText);

          //// remove white space
          // mentionText = mentionText.replace(" ", "");

          ////////////////!!!!!!!!!!!!!
          // TODO: check log to see if this found good titles
          Optional<Pair<String, Pair<Integer, Integer>>> titleAndOffSetOptional = findTitlesInTheMention(docTheory, mention, titles);
          //Optional<Pair<String, Pair<Integer, Integer>>> titleAndOffSetOptional = Optional.absent();
          /////////////////////////////

          if(titles.contains(mentionHeadText) || titles.contains(mentionText)) {
            TextMention answer;
            if(titles.contains(mentionHeadText)) {
              // titleText = mentionHeadText;
              answer = new TextMention(docTheory.docid().asString(),
                  mention.atomicHead().span().charOffsetRange().startInclusive().asInt(),
                  mention.atomicHead().span().charOffsetRange().endInclusive().asInt());
              answer.setText(mentionHeadText);
            } else {
              // titleText = mentionText;
              answer = new TextMention(docTheory.docid().asString(),
                  mention.span().charOffsetRange().startInclusive().asInt(),
                  mention.span().charOffsetRange().endInclusive().asInt());
              answer.setText(mentionText);
            }

//            // relation
//            TextMention answer = new TextMention(docTheory.docid().asString(),
//                mention.span().charOffsetRange().startInclusive().asInt(),
//                mention.span().charOffsetRange().endInclusive().asInt());
//            answer.setText(titleText);
            answer.setConfidence(1.0);
            answer.setBrandyConfidence("STRING");

            TextRelation textRelation = new TextRelation(leftEntityOptional.get(), answer, SlotFactory.fromStringSlotName("per:title"));

            // relation mention
            TextRelationMention rm = new TextRelationMention(representativeMention, answer, SlotFactory.fromStringSlotName("per:title"));

            rm.setConfidence(1.0f);
            rm.setSpan(new TextSpan(answer.getSpan().getStart(), answer.getSpan().getEnd()));
            rm.setIsValueSlot(true);

            // hack
            rm.setSourcePattern("serifCorefTitle");

            textRelation.addMention(rm);
            textRelation.setToValueSlot();

            relation2relationMention.put(textRelation, rm);
          }
          // not an exact match, but we may want to take it
          else if (titleAndOffSetOptional.isPresent()){

            System.out.println("titleAndOffSetOptional: " + titleAndOffSetOptional.get().getFirst() + "\t"
                + titleAndOffSetOptional.get().getSecond().getFirst() + ", " + titleAndOffSetOptional.get().getSecond().getSecond() + "\tin\t"
                + mentionText + "\t" + mentionHeadText);
            String titleText = titleAndOffSetOptional.get().getFirst();

            // relation
            TextMention answer = new TextMention(docTheory.docid().asString(),
                titleAndOffSetOptional.get().getSecond().getFirst(),
                titleAndOffSetOptional.get().getSecond().getSecond());
            answer.setText(titleText);
            answer.setConfidence(1.0);
            answer.setBrandyConfidence("STRING");

            TextRelation textRelation = new TextRelation(leftEntityOptional.get(), answer, SlotFactory.fromStringSlotName("per:title"));

            // relation mention
            TextRelationMention rm = new TextRelationMention(representativeMention, answer, SlotFactory.fromStringSlotName("per:title"));

            rm.setConfidence(1.0f);
            rm.setSpan(new TextSpan(answer.getSpan().getStart(), answer.getSpan().getEnd()));
            rm.setIsValueSlot(true);

            // hack
            rm.setSourcePattern("serifCorefTitle");

            textRelation.addMention(rm);
            textRelation.setToValueSlot();

            relation2relationMention.put(textRelation, rm);
          }
          else {
            // not found
            continue;
          }
        }
      }

      return relation2relationMention;
    }

    public class PairComparator implements Comparator {
      public int compare(Object o1, Object o2) {
        Pair<String, Entity> p1 = (Pair<String, Entity>) o1;
        Pair<String, Entity> p2 = (Pair<String, Entity>) o2;

        return p2.getFirst().length() - p1.getFirst().length();
      }
    }


    public class StringLengthComparator implements Comparator {
      public int compare(Object o1, Object o2) {
        String p1 = (String) o1;
        String p2 = (String) o2;

        return p2.length() - p1.length();
      }
    }


    Map<String, List<Pair<String, Entity>>> getType2NameAndEntityPairList(DocTheory docTheory) {
      Map<String, List<Pair<String, Entity>>> type2nameAndEntityPairList = new HashMap<String, List<Pair<String, Entity>>>();

      for(Entity entity : docTheory.entities()) {
        String entityType = entity.type().name().asString().substring(0, 3).toUpperCase();
        if(!type2nameAndEntityPairList.containsKey(entityType))
          type2nameAndEntityPairList.put(entityType, new ArrayList<Pair<String, Entity>>());

        for(Mention mention : entity.mentions()) {
          if(mention.isName()) {

            String text = mention.tokenSpan().originalText().content().utf16CodeUnits();
            String headText = mention.atomicHead().tokenSpan().originalText().content().utf16CodeUnits();

            // add both text and head text
            type2nameAndEntityPairList.get(entityType).add(new Pair<String, Entity>(text, entity));
            type2nameAndEntityPairList.get(entityType).add(new Pair<String, Entity>(headText, entity));

            if(entityType.equalsIgnoreCase("GPE")) {
              // 美国, 山西省, 广州, 香港 特区
              if (headText.endsWith("市") || headText.endsWith("国") || headText.endsWith("省")
                  || headText.endsWith("州") || headText.endsWith("区") || headText.endsWith("镇")
                  || headText.endsWith("县")) {
                String headTextWithoutSuffic = headText.substring(0, headText.length() - 1).trim();
                type2nameAndEntityPairList.get(entityType)
                    .add(new Pair<String, Entity>(headTextWithoutSuffic, entity));
              }
            }
          }
        }
      }


      for(String entityType : type2nameAndEntityPairList.keySet()) {
        Collections.sort(type2nameAndEntityPairList.get(entityType), new PairComparator());
        /*************************************/
        System.out.println();
        for(Pair<String, Entity> pair : type2nameAndEntityPairList.get(entityType)) {
          System.out.println("entity\t" + pair.getFirst());
        }
        System.out.println();
        /*************************************/
      }

      return type2nameAndEntityPairList;
    }

    Optional<Mention> findNestedNameAtStartingPosition(DocTheory docTheory, Mention mention, List<Pair<String, Entity>> nameAndEntityPairList) {
      if(nameAndEntityPairList==null)
        return Optional.absent();

      String text = mention.tokenSpan().originalText().content().utf16CodeUnits();
      String headText = mention.atomicHead().tokenSpan().originalText().content().utf16CodeUnits();

      // nameAndEntityPairList must be sorted
      // from the longest to the shortest
      for(Pair<String, Entity> nameAndEnityPair : nameAndEntityPairList) {
        if(text.startsWith(nameAndEnityPair.getFirst()) || headText.startsWith(nameAndEnityPair.getFirst())) {
          Entity entity = nameAndEnityPair.getSecond();
          if(!entity.containsMention(mention)) // must to find another entity, not itself
            return Optional.of(entity.representativeMention().mention());
        }
      }

      return Optional.absent();
    }

    /*
    public (RelationMentionCandidate relationMentionCandidate,
    Map<String, TextEntity> cacheEid2entities,
    Optional<DocTheory> testDocTheory,
    Mention leftMention, Mention, rightMention,
    DocEntityToKbEntityAligner docEntityToKbEntityAligner
    String arg1type,
    String arg1subType,
    String arg1text,
    int arg1_start,
    int arg1_end,
    String arg2type,
    String arg2subType,
    String arg2text,
    int arg2_start,
    int arg2_end
    ) {
      DocTheory docTheory = testDocTheory.get();

      System.out.println("Try aligning a relation mention");

      Optional<Entity> leftEntity = docTheory.entityByMention(leftMention);
      Optional<Entity> rightEntity = docTheory.entityByMention(rightMention);

      if (!leftEntity.isPresent() || !rightEntity.isPresent()) {
        System.out.println("Couldn't find doc-entity for left or right mention");
        continue;
      }


      Optional<String> kbIdLeftEntity = docEntityToKbEntityAligner
          .getKbIdForEntity(docTheory.docid().asString(), leftEntity.get(), testDocTheory);
      Optional<String> kbIdRightEntity = docEntityToKbEntityAligner
          .getKbIdForEntity(docTheory.docid().asString(), rightEntity.get(), testDocTheory);

      if (!kbIdLeftEntity.isPresent() || !kbIdRightEntity.isPresent()) {
        System.out.println("Couldn't find kb-entity ID for left or right doc-entity");
        continue;
      }

      System.out.println("aligning: ID found");

      // couldn't find in the mention to entity map
      if(!cacheEid2entities.containsKey(kbIdLeftEntity.get()) || ! cacheEid2entities.containsKey(kbIdRightEntity.get())) {
        System.out.println("Couldn't look up a kb-entity for left or right doc-entity");
        continue;
      }

      System.out.println("aligning: Entity found");

      // left and right argument can't be the same argument
      if(kbIdLeftEntity.get().equals(kbIdRightEntity.get())) {
        System.out.println("left and right KB entity can't be the same");
        continue;
      }

      System.out.println("kbIdLeftEntity: " + kbIdLeftEntity.get());
      System.out.println("kbIdRightEntity: " + kbIdRightEntity.get());

      List<Slot> kbpSlots = SlotFactory
          .getKBPSlotsFromSerifXmlMentionRelnTypeInCs2017(relationMentionCandidate.type,
              arg1type, arg1subType, arg2type, arg2subType, Optional.of(arg1text),
              Optional.of(arg2text));
    }
    */

    List<RelationMentionCandidate> getValueSlotRelationMentionCandidatesFromEvents(SentenceTheory sentenceTheory) {
      final double RELATION_SCORE_FROM_EVENT_MENTION = 0.4f;

      List<RelationMentionCandidate> relationMentionCandidates = new ArrayList<RelationMentionCandidate>();

      for (EventMention eventMention : sentenceTheory.eventMentions()) {

        if (eventMention.type().asString().equalsIgnoreCase("Life.Be-Born")) {
          Collection<EventMention.Argument> arg1s = eventMention.argsForRole(Symbol.from("Person"));
          Collection<EventMention.Argument> arg2s = eventMention.argsForRole(Symbol.from("Place"));
          Collection<EventMention.Argument> dates = eventMention.argsForRole(Symbol.from("Time-Within"));

          if (arg1s.size() == 1 && dates.size() == 1) {
            if (dates.iterator().next() instanceof EventMention.ValueMentionArgument) {
              Mention arg1 = ((EventMention.MentionArgument) arg1s.iterator().next()).mention();
              ValueMention dateArg =
                  ((EventMention.ValueMentionArgument) dates.iterator().next()).valueMention();

              System.out.println("Converted a relation: per_date_of_birth");

              final Symbol type = Symbol.from("per_date_of_birth");

              RelationMentionCandidate relationMentionCandidate = new RelationMentionCandidate(
                  arg1,
                  dateArg,
                  // eventMention.score(),// This will be negative
                  RELATION_SCORE_FROM_EVENT_MENTION,
                  type.asString(),
                  eventMention.span().charOffsetRange().startInclusive().asInt(),
                  eventMention.span().charOffsetRange().endInclusive().asInt());

              relationMentionCandidates.add(relationMentionCandidate);
            }
          }

        } else if (eventMention.type().asString().equalsIgnoreCase("Life.Die")) {
          Collection<EventMention.Argument> arg1s = eventMention.argsForRole(Symbol.from("Victim"));
          Collection<EventMention.Argument> arg2s = eventMention.argsForRole(Symbol.from("Place"));
          Collection<EventMention.Argument> dates =
              eventMention.argsForRole(Symbol.from("Time-Within"));

          if (arg1s.size() == 1 && dates.size() == 1) {
            if (dates.iterator().next() instanceof EventMention.ValueMentionArgument) {
              Mention arg1 = ((EventMention.MentionArgument) arg1s.iterator().next()).mention();
              ValueMention dateArg =
                  ((EventMention.ValueMentionArgument) dates.iterator().next()).valueMention();

              System.out.println("Converted a relation: per_date_of_death");

              final Symbol type = Symbol.from("per_date_of_death");

              RelationMentionCandidate relationMentionCandidate = new RelationMentionCandidate(
                  arg1,
                  dateArg,
                  RELATION_SCORE_FROM_EVENT_MENTION,
                  type.asString(),
                  eventMention.span().charOffsetRange().startInclusive().asInt(),
                  eventMention.span().charOffsetRange().endInclusive().asInt());

              relationMentionCandidates.add(relationMentionCandidate);
            }
          }
        } else if (eventMention.type().asString().equalsIgnoreCase("Business.Start-Org")) {
          Collection<EventMention.Argument> arg1s = eventMention.argsForRole(Symbol.from("Org"));
          Collection<EventMention.Argument> dates =
              eventMention.argsForRole(Symbol.from("Time-Within"));

          if (arg1s.size() == 1 && dates.size() == 1) {
            if (dates.iterator().next() instanceof EventMention.ValueMentionArgument) {
              Mention arg1 = ((EventMention.MentionArgument) arg1s.iterator().next()).mention();
              ValueMention dateArg =
                  ((EventMention.ValueMentionArgument) dates.iterator().next()).valueMention();

              System.out.println("Converted a relation: org_date_founded");

              final Symbol type = Symbol.from("org_date_founded");

              RelationMentionCandidate relationMentionCandidate = new RelationMentionCandidate(
                  arg1,
                  dateArg,
                  RELATION_SCORE_FROM_EVENT_MENTION,
                  type.asString(),
                  eventMention.span().charOffsetRange().startInclusive().asInt(),
                  eventMention.span().charOffsetRange().endInclusive().asInt());

              relationMentionCandidates.add(relationMentionCandidate);
            }
          }
        } else if (eventMention.type().asString().equalsIgnoreCase("Business.End-Org") || eventMention.type().asString().equalsIgnoreCase("Business.Declare-Bankruptcy")) {
          Collection<EventMention.Argument> arg1s = eventMention.argsForRole(Symbol.from("Org"));
          Collection<EventMention.Argument> dates =
              eventMention.argsForRole(Symbol.from("Time-Within"));

          if (arg1s.size() == 1 && dates.size() == 1) {
            if (dates.iterator().next() instanceof EventMention.ValueMentionArgument) {
              Mention arg1 = ((EventMention.MentionArgument) arg1s.iterator().next()).mention();
              ValueMention dateArg =
                  ((EventMention.ValueMentionArgument) dates.iterator().next()).valueMention();

              System.out.println("Converted a relation: org_date_dissolved");

              final Symbol type = Symbol.from("org_date_dissolved");

              RelationMentionCandidate relationMentionCandidate = new RelationMentionCandidate(
                  arg1,
                  dateArg,
                  RELATION_SCORE_FROM_EVENT_MENTION,
                  type.asString(),
                  eventMention.span().charOffsetRange().startInclusive().asInt(),
                  eventMention.span().charOffsetRange().endInclusive().asInt());

              relationMentionCandidates.add(relationMentionCandidate);
            }
          }
        }
      }

      return relationMentionCandidates;
    }

    public ColdStartKB fromCsEntityTuplesAndListSerifXmlsAndEntityAligner(
        String strFileTacEntityTuples,
        List<File> serifXmlFileList,
        DocEntityToKbEntityAligner docEntityToKbEntityAligner) {
      Map<String, TextEntity> cacheEid2entities = new HashMap<String, TextEntity>();
      Map<String, TextRelation> cacheSig2Relations = new HashMap<String, TextRelation>();

      try {
        CsSubmissionKB csSubmissionKB = new CsSubmissionKB();
        csSubmissionKB.loadFromFile(strFileTacEntityTuples, false);
        cacheEid2entities = csSubmissionKB.getId2entities();

        // title list
        List<String> titles = readTitlesFromList();
        List<String> titlesForTopMemberOrEmployee = readTopTitlesFromList();

        // for value relations, TAC requires the first provenance to be the filler span,
        // therefore we need to take a longer span to display provenance
        // this in general also helps to remove provenances that are not useful (as a sub-span of another)
        for (String eid : cacheEid2entities.keySet()) {
          System.out.println("[KB]:\tAdding an entity " + eid);
          this.withAddedEntity(cacheEid2entities.get(eid));
        }

        // loading the list of SERIF XMLs
        final SerifXMLLoader loader = SerifXMLLoader.fromStandardACETypes(true);
        for (final File file : serifXmlFileList) {
          System.out.println("[KB] loading " + file.getAbsolutePath());

          final Optional<DocTheory> testDocTheory = Optional.of(loader.loadFrom(file));

          if (testDocTheory.isPresent()) {
            DocTheory docTheory = testDocTheory.get();

            // caching GPEs
            Map<String, List<Pair<String, Entity>>> type2nameAndEntityPairList = getType2NameAndEntityPairList(docTheory);
            //

            String docId = docTheory.docid().asString();

            for (int sentenceId = 0; sentenceId < docTheory.numSentences(); sentenceId++) {
              SentenceTheory sentenceTheory = docTheory.sentenceTheory(sentenceId);

              // source #1: make a relation mention from the sentence theory
              List<RelationMentionCandidate> relationMentionCandidates = new ArrayList<RelationMentionCandidate>();
              for (RelationMention relationMention : sentenceTheory.relationMentions()) {
                RelationMentionCandidate relationMentionCandidate = RelationMentionCandidate.fromRelationMentionInSentenceTheory(relationMention);
                relationMentionCandidates.add(relationMentionCandidate);

                System.out.println("Read a relation mention : " + relationMentionCandidate.toString());
              }

              for(EventMention eventMention : sentenceTheory.eventMentions()) {
                List<RelationMentionCandidate> rmCandidateFromEvents = getValueSlotRelationMentionCandidatesFromEvents(sentenceTheory);
                relationMentionCandidates.addAll(rmCandidateFromEvents);

                for(RelationMentionCandidate relationMentionCandidate : rmCandidateFromEvents)
                  System.out.println("Read an event mention : " + relationMentionCandidate.toString());
              }

              // nested names
              for (Mention mention : sentenceTheory.mentions()) {
                String entityType = mention.entityType().name().asString().substring(0, 3).toUpperCase();

                if(mention.isName() && entityType.equalsIgnoreCase("ORG")) {

                  // source #2: find nested GPEs
                  // Example: 天津八十 中学
                  // TODO: 天津 八十 中学: GPE appears not within, but to the left of the current mention
                  Optional<Mention> headquarterGPEbestMention = findNestedNameAtStartingPosition(docTheory, mention, type2nameAndEntityPairList.get("GPE"));
                  if(headquarterGPEbestMention.isPresent()) {
                    // add a relation
                    double presetScore = 0.8;
                    String presetType = "org_place_of_headquarters";

                    int relationJustificationCharOffsetStartInclusive = mention.span().charOffsetRange().startInclusive().asInt();
                    int relationJustificationCharOffsetEndInclusive = mention.span().charOffsetRange().endInclusive().asInt(); // the longer mention span is the provenance
                    RelationMentionCandidate relationMentionCandidate = new RelationMentionCandidate(mention, headquarterGPEbestMention.get(),
                        presetScore,
                        presetType,
                        relationJustificationCharOffsetStartInclusive,
                        relationJustificationCharOffsetEndInclusive);
                    relationMentionCandidates.add(relationMentionCandidate);

                    System.out.println("[nest] org_place_of_headquarters: " + docId + " < " + mention.tokenSpan().originalText().content().utf16CodeUnits() + " , " + headquarterGPEbestMention.get().tokenSpan().originalText().content().utf16CodeUnits() + " >");
                  }

                  // TODO: test
                  /* very noisy, not used
                  // source #2: find nested ORG
                  // Example:
                  Optional<Mention> parentORGbestMention = findNestedNameAtStartingPosition(docTheory, mention, type2nameAndEntityPairList.get("ORG"));
                  if(parentORGbestMention.isPresent()) {
                    // Optional<Entity> parentORGentityOptional =
                    //    parentORGbestMention.get().entity(docTheory);
                    // Optional<Entity> currentEntityOptional = mention.entity(docTheory);
                    // if (parentORGentityOptional.isPresent() && currentEntityOptional.isPresent()) {
                    //  if (!parentORGentityOptional.get().equals(currentEntityOptional.get()))
                    //  { // not the same entity
                        // add a relation
                        double presetScore = 0.8;
                        String presetType = "org_parent";

                        int relationJustificationCharOffsetStartInclusive =
                            parentORGbestMention.get().span().charOffsetRange().startInclusive()
                                .asInt();
                        int relationJustificationCharOffsetEndInclusive =
                            parentORGbestMention.get().span().charOffsetRange().endInclusive()
                                .asInt();
                        RelationMentionCandidate relationMentionCandidate =
                            new RelationMentionCandidate(mention, parentORGbestMention.get(),
                                presetScore,
                                presetType,
                                relationJustificationCharOffsetStartInclusive,
                                relationJustificationCharOffsetEndInclusive);
                        relationMentionCandidates.add(relationMentionCandidate);

                        System.out.println(
                            "[nest] org_parent: " + docId + " < " + mention
                                .tokenSpan().tokenizedText(docTheory) + " , " + parentORGbestMention
                                .get().tokenSpan().tokenizedText(docTheory) + " >");
                    //  }
                    // }
                  }
                  */
                }
              }

              // source #2: find nested ORG (parent)

              ///////////////////////////////////

              for (RelationMentionCandidate relationMentionCandidate : relationMentionCandidates) {
                Mention leftMention = relationMentionCandidate.leftMention;
                Optional<Mention> rightEntityMention = relationMentionCandidate.rightMention;
                Optional<ValueMention> rightValueMention = relationMentionCandidate.rightValueMention;

                // brandy confidence
                Optional<String> leftMentionLevel = Optional.of(leftMention.mentionType().name());
                Optional<String> rightMentionLevel = Optional.absent();

                // arg1
                String arg1type =
                    leftMention.entityType().toString().substring(0, 3)
                        .toUpperCase();
                String arg1subType =
                    leftMention.entitySubtype().name().asString();
                String arg1text = leftMention.atomicHead().tokenSpan().originalText().content().utf16CodeUnits();
//                    .tokenizedText(testDocTheory.get());
                int arg1_start =
                    leftMention.atomicHead().tokenSpan().charOffsetRange().startInclusive()
                        .asInt();
                int arg1_end =
                    leftMention.atomicHead().tokenSpan().charOffsetRange().endInclusive()
                        .asInt();

                Optional<Entity> leftEntity = docTheory.entityByMention(leftMention);
                if (!leftEntity.isPresent()) {
                  System.out.println("Couldn't find doc-entity for left mention: " + arg1text);
                  continue;
                }
                Optional<String> kbIdLeftEntity = docEntityToKbEntityAligner
                    .getKbIdForEntity(docTheory.docid().asString(), leftEntity.get(), testDocTheory);
                if (!kbIdLeftEntity.isPresent()) {
                  System.out.println("[Left-mention] Couldn't find kb-entity ID for left doc-entity: " + arg1text);
                  continue;
                } {
                  System.out.println("[Left-mention] look up succeed: " + arg1text);
                }
                //

                // TODO: hacky initialization
                String arg2type = "";
                String arg2subType = "";
                String arg2text = "";
                int arg2_start = 0;
                int arg2_end = 0;
                Optional<Entity> rightEntity = Optional.absent();
                Optional<String> kbIdRightEntity = Optional.absent();


                List<Slot> kbpSlots = new ArrayList<Slot>();

                boolean isThisValueRelation = false;

                //// for entity arg2 mentions
                if(rightValueMention.isPresent()) {
                  // set to value-slot
                  isThisValueRelation = true;

                  // set mention level
                  rightMentionLevel = Optional.of("STRING");

                  ValueMention rightMention = relationMentionCandidate.rightValueMention.get();

                  arg2type = "STRING";
                  arg2subType = "STRING";
                  arg2text = rightMention.tokenSpan().originalText().content().utf16CodeUnits();
//                      .tokenizedText(testDocTheory.get());
                  arg2_start = rightMention.tokenSpan().charOffsetRange().startInclusive()
                      .asInt();
                  arg2_end = rightMention.tokenSpan().charOffsetRange().endInclusive()
                      .asInt();

                  // TODO: arg2text has chinese characters
                  kbIdRightEntity = Optional.of("NOT_IN_KB:" + arg2text);

                  if(!cacheEid2entities.containsKey(kbIdLeftEntity.get())) {
                    System.out.println("Couldn't look up a kb-entity for left doc-entity: " + arg1text);
                    continue;
                  }

                  System.out.println("aligning: Entity found");

                  System.out.println("kbIdLeftEntity: " + kbIdLeftEntity.get());

                  kbpSlots = SlotFactory
                      .getKBPSlotsFromSerifXmlValueRelnTypeInCs2017(relationMentionCandidate.type,
                          arg1type, arg1subType, arg2type, arg2subType, Optional.of(arg1text),
                          Optional.of(arg2text));

                  StringBuilder stringBuilder = new StringBuilder();
                  for(Slot slot : kbpSlots)
                    stringBuilder.append(slot + ", ");
                  System.out.println("SlotFactory lookup: " + "type=" + relationMentionCandidate.type +
                      ", arg1type=" + arg1type + ", arg1subType=" + arg1subType + ", arg1text=" + arg1text +
                      ", arg2type=" + arg2type + ", arg2subType=" + arg2subType + ", arg2text=" + arg2text +
                  " -> " + stringBuilder.toString());

                } else if(rightEntityMention.isPresent()) {
                  Mention rightMention = relationMentionCandidate.rightMention.get();

                  // set mention level
                  rightMentionLevel = Optional.of(rightMention.mentionType().name());

                  arg2type =
                      rightMention.entityType().toString().substring(0, 3)
                          .toUpperCase();
                  arg2subType =
                      rightMention.entitySubtype().name().asString();
                  arg2text = rightMention.atomicHead().tokenSpan().originalText().content().utf16CodeUnits();
//                      .tokenizedText(testDocTheory.get());
                  arg2_start = rightMention.atomicHead().tokenSpan().charOffsetRange().startInclusive()
                      .asInt();
                  arg2_end = rightMention.atomicHead().tokenSpan().charOffsetRange().endInclusive()
                      .asInt();

                  System.out.println("Try aligning a relation mention");

                  rightEntity = docTheory.entityByMention(rightMention);

                  if (!leftEntity.isPresent()) {
                    System.out.println("Couldn't find doc-entity for left mention: " + arg1text);
                    continue;
                  }
                  if (!rightEntity.isPresent()) {
                    System.out.println("Couldn't find doc-entity for right mention: " + arg2text);
                    continue;
                  }

                  kbIdRightEntity = docEntityToKbEntityAligner
                      .getKbIdForEntity(docTheory.docid().asString(), rightEntity.get(), testDocTheory);

                  if (!kbIdLeftEntity.isPresent()) {
                    System.out.println("Couldn't find kb-entity ID for left doc-entity: " + arg1text);
                    continue;
                  }
                  if (!kbIdRightEntity.isPresent()) {
                    System.out.println("[right-mention] Couldn't find kb-entity ID for right doc-entity: " + arg2text);
                    continue;
                  } {
                    System.out.println("[right-mention] look up succeed: " + arg1text);
                  }

                  System.out.println("aligning: ID found");

                  // couldn't find in the mention to entity map
                  if(!cacheEid2entities.containsKey(kbIdLeftEntity.get())) {
                    System.out.println("Couldn't look up a kb-entity for left doc-entity: " + arg1text);
                    continue;
                  }
                  if(!cacheEid2entities.containsKey(kbIdRightEntity.get())) {
                    System.out.println("Couldn't look up a kb-entity for right doc-entity: " + arg2text);
                    continue;
                  }

                  System.out.println("aligning: Entity found");

                  // left and right argument can't be the same argument
                  if(!relationMentionCandidate.type.contains("title")) { // except for per:titlels -
                    if (kbIdLeftEntity.get().equals(kbIdRightEntity.get())) {
                      System.out.println(
                          "left and right KB entity can't be the same: " + arg1text + ", "
                              + arg2text);
                      continue;
                    }
                  }

                  System.out.println("kbIdLeftEntity: " + kbIdLeftEntity.get());
                  System.out.println("kbIdRightEntity: " + kbIdRightEntity.get());

                  kbpSlots = SlotFactory
                      .getKBPSlotsFromSerifXmlMentionRelnTypeInCs2017(relationMentionCandidate.type,
                          arg1type, arg1subType, arg2type, arg2subType, Optional.of(arg1text),
                          Optional.of(arg2text),
                          titlesForTopMemberOrEmployee);

                  StringBuilder stringBuilder = new StringBuilder();
                  for(Slot slot : kbpSlots)
                    stringBuilder.append(slot + ", ");
                  System.out.println("SlotFactory lookup: " + "type=" + relationMentionCandidate.type +
                      ", arg1type=" + arg1type + ", arg1subType=" + arg1subType + ", arg1text=" + arg1text +
                      ", arg2type=" + arg2type + ", arg2subType=" + arg2subType + ", arg2text=" + arg2text +
                      " -> " + stringBuilder.toString());
                }
                else {
                  System.out.println("Skip relation mention because of no valid arg2 found");
                }
                //////////////////////////////

                /*
                System.out.println("Try aligning a relation mention");

                Optional<Entity> leftEntity = docTheory.entityByMention(leftMention);
                Optional<Entity> rightEntity = docTheory.entityByMention(rightMention);

                if (!leftEntity.isPresent() || !rightEntity.isPresent()) {
                  System.out.println("Couldn't find doc-entity for left or right mention");
                  continue;
                }


                Optional<String> kbIdLeftEntity = docEntityToKbEntityAligner
                    .getKbIdForEntity(docTheory.docid().asString(), leftEntity.get(), testDocTheory);
                Optional<String> kbIdRightEntity = docEntityToKbEntityAligner
                    .getKbIdForEntity(docTheory.docid().asString(), rightEntity.get(), testDocTheory);

                if (!kbIdLeftEntity.isPresent() || !kbIdRightEntity.isPresent()) {
                  System.out.println("Couldn't find kb-entity ID for left or right doc-entity");
                  continue;
                }

                System.out.println("aligning: ID found");

                // couldn't find in the mention to entity map
                if(!cacheEid2entities.containsKey(kbIdLeftEntity.get()) || ! cacheEid2entities.containsKey(kbIdRightEntity.get())) {
                  System.out.println("Couldn't look up a kb-entity for left or right doc-entity");
                  continue;
                }

                System.out.println("aligning: Entity found");

                // left and right argument can't be the same argument
                if(kbIdLeftEntity.get().equals(kbIdRightEntity.get())) {
                  System.out.println("left and right KB entity can't be the same");
                  continue;
                }

                System.out.println("kbIdLeftEntity: " + kbIdLeftEntity.get());
                System.out.println("kbIdRightEntity: " + kbIdRightEntity.get());


                List<Slot> kbpSlots = SlotFactory
                    .getKBPSlotsFromSerifXmlMentionRelnTypeInCs2017(relationMentionCandidate.type,
                        arg1type, arg1subType, arg2type, arg2subType, Optional.of(arg1text),
                        Optional.of(arg2text));

                if (kbpSlots.isEmpty()) {
                  System.out.println("Skip relation type " + relationMentionCandidate.type);
                  continue;
                }
                */

                System.out.println("aligning: slot found");

                // This has multiple slots if and only if slot = per:employee_member_of and per:top_employee_member_of
                for(Slot kbpSlot : kbpSlots) {
                  ////// contruct a relation mention
                  System.out.println("[KB] found an alignable relation mention");

                  // This only works for English
                  // Optional<String> normalizedDate =
                  //    getNormalizedDate(kbpSlot, arg2text);

                  TextMention queryMention =
                      new TextMention(docId, arg1_start, arg1_end, arg1type, arg1subType);
                  queryMention.setEntityId(kbIdLeftEntity.get());
                  queryMention.setText(arg1text);

                  // set mention level
                  queryMention.setBrandyConfidence(leftMentionLevel.get());

                  TextMention answerMention =
                      new TextMention(docId, arg2_start, arg2_end, arg2type, arg2subType);
                  answerMention.setEntityId(kbIdRightEntity.get());
                  answerMention.setText(arg2text);

                  /*
                  // inject normalized date
                  if (normalizedDate.isPresent()) {
                    answerMention
                        .setText(
                            normalizedDate.get().substring(1, normalizedDate.get().length() - 1));
                  } else {
                    answerMention.setText(arg2text);
                  }
                  */

                  // set mention level
                  answerMention.setBrandyConfidence(rightMentionLevel.get());

                  TextRelationMention
                      rm =
                      new TextRelationMention(queryMention, answerMention, kbpSlot);

                  rm.setConfidence(relationMentionCandidate.score);
                  rm.setSpan(
                      new TextSpan(
                          relationMentionCandidate.startCharOffSetInclusive,
                          relationMentionCandidate.endCharOffSetInclusive));
                  if(isThisValueRelation)
                    rm.setIsValueSlot(true);
                  // TODO: set to value-slot again
                  rm.setIsValueSlot(SlotFactory.isValueSlot(kbpSlot));

                  rm.setConfidence(relationMentionCandidate.score);
                  // hack
                  rm.setSourcePattern("serif");
                  //

                  ////// construct a relation

                  // update relation map
                  TextRelation KBRelation = null;
                  TextEntity query = cacheEid2entities.get(kbIdLeftEntity.get());

                  // skip query types mis-compatible with TAC KB entity types
                  String queryTypeInSlotName = kbpSlot.toString().substring(0, kbpSlot.toString().indexOf(":"));
                  if(!query.getTypes().contains(queryTypeInSlotName) && !query.getTypes().contains(queryTypeInSlotName.toUpperCase())) {
                    System.out.println("skip because type mismatch: queryTypeInSlotName: " + queryTypeInSlotName + ", kbIdLeftEntity.get()=" + kbIdLeftEntity.get());
                    continue;
                  }
                  if(!query.getTypes().contains(arg1type) && !query.getTypes().contains(arg1type.toLowerCase())) {
                    System.out.println("skip because type mismatch: arg1type: " + arg1type + ", kbIdLeftEntity.get()=" + kbIdLeftEntity.get());
                    continue;
                  }
                  //

                  if (!rm.isValueSlot.get()) {
                    TextEntity answer = cacheEid2entities.get(kbIdRightEntity.get());

                    // skip query types mis-compatible with TAC KB entity types
                    if(SlotFactory.kbpSlot2arg2Types.containsKey(kbpSlot.toString())) {

                      Collection<String> answerTypeRequired = SlotFactory.kbpSlot2arg2Types.get(kbpSlot.toString());

                      Set<String> union = new HashSet<String>();
                      StringBuilder stringBuilder = new StringBuilder();
                      for(String type : answer.getTypes()) {
                        stringBuilder.append(type + ", ");
                        union.add(type.toUpperCase());
                      }
                      for(String type : answerTypeRequired)
                        union.add(type.toUpperCase());

                      // only == is possible
                      if(union.size()>=answerTypeRequired.size() + answer.getTypes().size()) {
                        System.out.println("skip because type mismatch: kbpSlot =  " + kbpSlot.toString() + ", answer_types=" + stringBuilder.toString());
                        continue;
                      }
                      if(!answer.getTypes().contains(arg2type) && !answer.getTypes().contains(arg2type.toLowerCase())) {
                        System.out.println("skip because type mismatch: arg2type =  " + arg2type
                            + ", answer_types=" + stringBuilder.toString());
                        continue;
                      }
                    }
                    //

                    // skip relation between itself
                    if (query.equals(answer)) {
                      System.out.println("[KB] SKIP:\tquery&answer are the same entity");
                      continue;
                    }

                    KBRelation = new TextRelation(query, answer, kbpSlot);
                  } else {
                    TextMention value = answerMention;
                    value.setText(arg2text);

                    /*
                    if (normalizedDate.isPresent()) // use normalized version of the date
                      value.setText(
                          normalizedDate.get().substring(1, normalizedDate.get().length() - 1));
                    else
                      value.setText(arg2text);
                    */

                    KBRelation = new TextRelation(query, value, kbpSlot);
                  }
                  if (rm.isValueSlot.get())
                    KBRelation.setToValueSlot();

                  String strRelnSig = getRelationSignature(KBRelation);
                  if (!cacheSig2Relations.containsKey(strRelnSig))
                    cacheSig2Relations.put(strRelnSig, KBRelation);
                  else
                    KBRelation = cacheSig2Relations.get(strRelnSig);

                  System.out.println("aligning: relation added");

                  KBRelation.addMention(rm);
                  if (!KBRelation.mentionCount.isPresent())
                    KBRelation.mentionCount = Optional.of(1);
                  else
                    KBRelation.mentionCount = Optional.of(KBRelation.mentionCount.get() + 1);

                  if(relationMentionCandidate.score > KBRelation.getConfidence())
                    KBRelation.setConfidence(relationMentionCandidate.score);  // MAX confidence
                }
              }
            }
          }

          // add title relations from SERIF
          Multimap<TextRelation, TextRelationMention> relation2relationMentions = getPerTitleRelationMentionsfromDocTheory (testDocTheory.get(), titles,
              cacheEid2entities,
              docEntityToKbEntityAligner);

          for(TextRelation textRelation :relation2relationMentions.keySet()) {
            Optional<TextRelation> textRelationOptional = Optional.absent();

            String strRelnSig = getRelationSignature(textRelation);
            if(!cacheSig2Relations.containsKey(strRelnSig))
              cacheSig2Relations.put(strRelnSig, textRelation);

            textRelationOptional = Optional.of(cacheSig2Relations.get(strRelnSig));

            for(TextRelationMention textRelationMention : relation2relationMentions.get(textRelation)) {
              textRelationOptional.get().addMention(textRelationMention);
            }
          }
        }

        for (String eid : cacheEid2entities.keySet()) {
          this.withAddedEntity(cacheEid2entities.get(eid));
        }
        for (TextRelation KBRelation : cacheSig2Relations.values()) {
          this.withAddedRelation(KBRelation);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }

      return this.build();
    }



/*
    public ColdStartKB fromFileNoConfidenceMeasures(String strFileKB) {
      Map<String, KBEntity> cacheEid2entities = new HashMap<String, KBEntity>();

      List<String> lines = FileUtil.readLinesIntoList(strFileKB);
      for (String sline : lines) {
        String[] fields = sline.trim().split("\t");

        String predicate = fields[1];

        if (predicate.equals("type") || predicate.equals("mention") || predicate
            .equals("canonical_mention")) {
          String srcId = fields[0];
          if (!cacheEid2entities.containsKey(srcId)) {
            cacheEid2entities.put(srcId, new KBEntity(srcId));
          }

          if (predicate.equals("type")) {
            cacheEid2entities.get(srcId).addType(fields[2].toLowerCase());
          } else if (predicate.equals("mention")) {
            if (!StringUtil.isNumeric(fields[4]) || !StringUtil.isNumeric(fields[5])) {
              throw new RuntimeException("Invalid line: " + sline);
            }

            KBMention mention = new KBMention(fields[3], Integer.parseInt(fields[4]),
                Integer.parseInt(fields[5]));
            mention.setText(fields[2].substring(1, fields[2].length() - 1));
            mention.setEntityId(srcId);
            cacheEid2entities.get(srcId).addMention(mention);
          } else if (predicate.equals("canonical_mention")) {

            if (!StringUtil.isNumeric(fields[4]) || !StringUtil.isNumeric(fields[5])) {
              throw new RuntimeException("Invalid line: " + sline);
            }

            KBMention mention = new KBMention(fields[3], Integer.parseInt(fields[4]),
                Integer.parseInt(fields[5]));
            mention.setText(fields[2].substring(1, fields[2].length() - 1));
            mention.setEntityId(srcId);
            cacheEid2entities.get(srcId).addCanonicalMention(mention);
          }
        } else {
          sline = sline.replace("\t\t", "\t");
          fields = sline.trim().split("\t");

          if (fields.length < 17) {
            System.out.println("Error in format: " + sline);
            continue;
          }

          String filler = fields[9];
          boolean isValue = filler.startsWith("\"") && filler.endsWith("\"");

          String relnType = fields[1];
          String docId = fields[0];

          String role1 = fields[2];

          // for agent1
          String srcEntityId;
          String queryType;
          String querySubType;
          int arg1_start;
          int arg1_end;

          // for filler
          String dstEntityId;
          String answerType;
          String answerSubType;
          int arg2_start;
          int arg2_end;

          if (role1.equals("AGENT1")) {
            srcEntityId = fields[3];
            queryType = fields[4];
            querySubType = fields[5];
            arg1_start = Integer.parseInt(fields[6]);
            arg1_end = Integer.parseInt(fields[7]);

            dstEntityId = fields[9];
            answerType = fields[10];
            answerSubType = fields[11];
            arg2_start = Integer.parseInt(fields[12]);
            arg2_end = Integer.parseInt(fields[13]);
          } else {
            dstEntityId = fields[3];
            answerType = fields[4];
            answerSubType = fields[5];
            arg2_start = Integer.parseInt(fields[6]);
            arg2_end = Integer.parseInt(fields[7]);

            srcEntityId = fields[9];
            queryType = fields[10];
            querySubType = fields[11];
            arg1_start = Integer.parseInt(fields[12]);
            arg1_end = Integer.parseInt(fields[13]);
          }

          int sent_start = Integer.parseInt(fields[14]);
          int sent_end = Integer.parseInt(fields[15]);

          double confidence = Double.parseDouble(fields[16]);

          if (answerType.trim().isEmpty() || queryType.trim().isEmpty()) {
            System.out.println("-- " + sline);
            System.exit(0);
          }

          if (!cacheEid2entities.containsKey(srcEntityId)) {
            throw new RuntimeException("No srcEntityId found: " + sline);
          }

          KBEntity query = cacheEid2entities.get(srcEntityId);
          Optional<Slot> slot =
              SlotFactory.fromFactTypeString(relnType, querySubType, answerSubType);
          if (!slot.isPresent()) {
            System.out.println("SKIP: " + sline);
            continue;
          }

          KBMention queryMention =
              new KBMention(docId, arg1_start, arg1_end, queryType, querySubType);
          queryMention.setEntityId(srcEntityId);
          KBMention answerMention =
              new KBMention(docId, arg2_start, arg2_end, answerType, answerSubType);
          answerMention.setEntityId(dstEntityId);

          Relation relation = null;
          if (!isValue) {
            KBEntity answer = cacheEid2entities.get(dstEntityId); // KBmention
            relation = new Relation(query, answer, slot.get());
          } else {
            KBMention value = answerMention;
            value.setText(dstEntityId
                .substring(1, dstEntityId.length() - 1)); // dstEntityId is text; strip ""
            relation = new Relation(query, value, slot.get());
          }

          KBRelationMention rm = new KBRelationMention(queryMention, answerMention, slot.get());
          rm.setConfidence(confidence);
          rm.setSpan(new Span(sent_start, sent_end));
          rm.setIsValueSlot(isValue);

          relation.addMention(rm);

          relation.isValSlot = isValue;

          this.withAddedRelation(relation);
        }
      }

      //		Map<String, EntityInfo> cacheEid2entities = new HashMap<String, EntityInfo>();
      for (String eid : cacheEid2entities.keySet()) {
        this.withAddedEntity(cacheEid2entities.get(eid));
      }

      return this.build();
    }


    public ColdStartKB fromValidKBFile(String strFileKB) {
      Map<String, KBEntity> cacheEid2entities = new HashMap<String, KBEntity>();

      List<String> lines = FileUtil.readLinesIntoList(strFileKB);
      for (String sline : lines) {
        String[] fields = sline.trim().split("\t");

        String predicate = fields[1];

        if (predicate.equals("type") || predicate.equals("mention") || predicate
            .equals("canonical_mention")) {
          String srcId = fields[0];
          if (!cacheEid2entities.containsKey(srcId)) {
            cacheEid2entities.put(srcId, new KBEntity(srcId));
          }

          if (predicate.equals("type")) {
            cacheEid2entities.get(srcId).addType(fields[2].toLowerCase());
          } else if (predicate.equals("mention")) {
            if (!StringUtil.isNumeric(fields[4]) || !StringUtil.isNumeric(fields[5])) {
              throw new RuntimeException("Invalid line: " + sline);
            }

            KBMention mention = new KBMention(fields[3], Integer.parseInt(fields[4]),
                Integer.parseInt(fields[5]));
            mention.setText(fields[2].substring(1, fields[2].length() - 1));
            mention.setEntityId(srcId);
            cacheEid2entities.get(srcId).addMention(mention);
          } else if (predicate.equals("canonical_mention")) {

            if (!StringUtil.isNumeric(fields[4]) || !StringUtil.isNumeric(fields[5])) {
              throw new RuntimeException("Invalid line: " + sline);
            }

            KBMention mention = new KBMention(fields[3], Integer.parseInt(fields[4]),
                Integer.parseInt(fields[5]));
            mention.setText(fields[2].substring(1, fields[2].length() - 1));
            mention.setEntityId(srcId);
            cacheEid2entities.get(srcId).addCanonicalMention(mention);
          }
        } else {
          sline = sline.replace("\t\t", "\t");
          fields = sline.trim().split("\t");

          if (fields.length < 12) {
            System.out.println("Error in format: " + sline);
            continue;
          }

          String srcEntityId = fields[0];
          String relnType = fields[1];
          String dstEntityId = fields[2];
          String docId = fields[3];

          int arg1_start = Integer.parseInt(fields[4]);
          int arg1_end = Integer.parseInt(fields[5]);

          int sent_start = Integer.parseInt(fields[6]);
          int sent_end = Integer.parseInt(fields[7]);

          int arg2_start = Integer.parseInt(fields[8]);
          int arg2_end = Integer.parseInt(fields[9]);

          double confidence = Double.parseDouble(fields[11]);

          boolean isValue = dstEntityId.startsWith("\"") && dstEntityId.endsWith("\"");

          if (!cacheEid2entities.containsKey(srcEntityId)) {
            throw new RuntimeException("No srcEntityId found: " + sline);
          }

          KBEntity query = cacheEid2entities.get(srcEntityId);
          Slot slot = SlotFactory.fromStringSlotName(relnType);

          KBMention queryMention = new KBMention(docId, arg1_start, arg1_end);
          queryMention.setEntityId(srcEntityId);
          KBMention answerMention = new KBMention(docId, arg2_start, arg2_end);
          answerMention.setEntityId(dstEntityId);

          Relation relation = null;
          if (!isValue) {
            KBEntity answer = cacheEid2entities.get(dstEntityId); // KBmention
            relation = new Relation(query, answer, slot);
          } else {
            KBMention value = answerMention;
            value.setText(dstEntityId
                .substring(1, dstEntityId.length() - 1)); // dstEntityId is text; strip ""
            relation = new Relation(query, value, slot);
          }

          KBRelationMention rm = new KBRelationMention(queryMention, answerMention, slot);
          rm.setConfidence(confidence);
          rm.setSpan(new Span(sent_start, sent_end));
          rm.setIsValueSlot(isValue);

          relation.addMention(rm);

          relation.isValSlot = isValue;

          this.withAddedRelation(relation);
        }
      }

      //		Map<String, EntityInfo> cacheEid2entities = new HashMap<String, EntityInfo>();
      for (String eid : cacheEid2entities.keySet()) {
        this.withAddedEntity(cacheEid2entities.get(eid));
      }

      return this.build();
    }
    */
  }
}
