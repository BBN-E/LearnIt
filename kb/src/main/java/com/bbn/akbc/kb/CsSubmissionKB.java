package com.bbn.akbc.kb;


import com.bbn.akbc.evaluation.tac.RelationFromSubmission;
import com.bbn.akbc.kb.text.TextEntity;
import com.bbn.akbc.kb.text.TextMention;
import com.bbn.akbc.common.FileUtil;
import com.bbn.akbc.common.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by bmin on 1/27/16.
 */
public class CsSubmissionKB {
  private Map<String, TextEntity> id2entities;
  private List<RelationFromSubmission> relations;

  public CsSubmissionKB() {
    id2entities = new HashMap<String, TextEntity>();
    relations = new ArrayList<RelationFromSubmission>();
  }

  public Map<String, TextEntity> getId2entities() {
    return id2entities;
  }

  public List<RelationFromSubmission> getRelations() {
    return relations;
  }

  public void loadFromFile(String strFileKB) {
    loadFromFile(strFileKB, false);
  }

  public void loadFromFile(String strFileKB, boolean adjust_offsets_for_adept) {
    List<String> lines = FileUtil.readLinesIntoList(strFileKB);
    boolean isFirstLine = true;
    for (String sline : lines) {
      if (isFirstLine) {
        isFirstLine = false;
        if (!sline.startsWith(":")) {
          continue;
        }
      }

      if(sline.trim().isEmpty()) // skip empty line
        continue;

      try {

        String[] fields = sline.trim().split("\t");

        String predicate = fields[1];

        if (predicate.equals("type") || predicate.equals("mention")
            || predicate.equals("canonical_mention")
            || predicate.equals("nominal_mention")
            || predicate.equals("normalized_mention")) {
          String srcId = fields[0];
//        System.out.println("dbg:\t" + srcId + "\t" + sline);

          if (!id2entities.containsKey(srcId)) {
            id2entities.put(srcId, new TextEntity(srcId));
          }

          if (predicate.equals("type")) {
            id2entities.get(srcId).addType(fields[2]);
          } else if (predicate.equals("mention") || predicate.equals("canonical_mention") || predicate.equals("nominal_mention")) {
            String[] listTmpVar1 = fields[3].split(":");
            String docId = listTmpVar1[0];
            String[] listTmpVar2 = listTmpVar1[1].split("-");

            String start = listTmpVar2[0];
            String end = listTmpVar2[1];

            double confidence = 1.0f;
            if(fields.length>4)
              confidence = Double.parseDouble(fields[4]);

            if (!StringUtil.isNumeric(start) || !StringUtil.isNumeric(end)) {
              throw new RuntimeException("Invalid line: " + sline);
            }

            TextMention mention;
            if(adjust_offsets_for_adept)
              mention = new TextMention(docId, Integer.parseInt(start), Integer.parseInt(end)+1);
            else
              mention = new TextMention(docId, Integer.parseInt(start), Integer.parseInt(end));
            mention.setText(fields[2].substring(1, fields[2].length() - 1));
            mention.setEntityId(srcId);
            mention.setConfidence(confidence);

            if (predicate.equals("mention") || predicate.equals("nominal_mention")) {
              id2entities.get(srcId).addMention(mention);
            } else if (predicate.equals("canonical_mention")) {
              id2entities.get(srcId).addCanonicalMention(mention);
            }
          }
        } else if (predicate.equals("link")) {
          System.out.println("Skip predicate \"link\"");
        }
        else {
          relations.add(new RelationFromSubmission(sline, adjust_offsets_for_adept));
        }
      }
      catch (Exception e) {
        System.out.print("Exception when parsing line: " + sline);
        e.printStackTrace();
      }
    }

    // remove entities with no mentions, or no canonical mentions
    Set<String> eids = new HashSet<String>(id2entities.keySet());
    for(String eid : eids) {
      if(!id2entities.get(eid).isValid())
        id2entities.remove(eid);
    }
    //
  }

}
