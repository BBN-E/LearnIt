package com.bbn.akbc.evaluation.tac.io;

import com.bbn.akbc.evaluation.tac.RelationFromSubmission;
import com.bbn.akbc.common.Justification;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SystemKbRelationLoader {

  public static Map<String, List<RelationFromSubmission>> doc2relations =
      new HashMap<String, List<RelationFromSubmission>>();

  public static void load(String fileSystemKB) throws IOException {

    BufferedReader reader = new BufferedReader(new FileReader(fileSystemKB));
    String sline;
    while ((sline = reader.readLine()) != null) {
      String[] fields = sline.trim().split("\t");
      if (fields.length <= 3) {
        continue;
      }

      String predicate = fields[1];

      if (predicate.equals("type") || predicate.equals("mention") || predicate
          .equals("canonical_mention")) {
        continue;
      } else {
        RelationFromSubmission relation = new RelationFromSubmission(sline);
        for (Justification justification : relation.justifications) {
          if (!doc2relations.containsKey(justification.docId)) {
            doc2relations.put(justification.docId, new ArrayList<RelationFromSubmission>());
          }

          doc2relations.get(justification.docId).add(relation);
        }
      }
    }
    reader.close();

  }
}
