package com.bbn.akbc.evaluation.tac.io;


import com.bbn.akbc.kb.text.TextMention;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SystemKbMentionLoader {

  public static Map<String, List<TextMention>> hop0queryId2listCanonicalMention =
      new HashMap<String, List<TextMention>>();

  public static void loadCanonicalMention(String fileSystemKB,
      Map<String, String> hop0QueryToEntityInSysOut) throws IOException {

    BufferedReader reader = new BufferedReader(new FileReader(fileSystemKB));
    String sline;

    int uid = 0;
    while ((sline = reader.readLine()) != null) {
      String[] fields = sline.trim().split("\t");

      if(fields.length<=1)
        continue;

      try {
        if (!fields[1].equals("canonical_mention")) {
          continue;
        }
      } catch (IndexOutOfBoundsException ex) {
        System.out.println("Unable to parse the following line:");
        System.out.println(sline.trim());
        continue;
      }

      // get KBmention
      String[] listTmpVar1 = fields[3].split(":");
      String docId = listTmpVar1[0];
      String[] listTmpVar2 = listTmpVar1[1].split("-");

      String start = listTmpVar2[0];
      String end = listTmpVar2[1];
      TextMention canonicalMention =
          new TextMention(docId, Integer.parseInt(start), Integer.parseInt(end));
      //

//			KBmention canonicalMention = new KBmention(fields[3], Integer.parseInt(fields[4]), Integer.parseInt(fields[5]), fields[2].replace("\"", ""));

      canonicalMention.setEntityId(fields[0]);
      // for efficiency reason, skip the ones that are not in final results
      if (!hop0QueryToEntityInSysOut.values().contains(canonicalMention.getEntityId().get())) {
        continue;
      }

      if (!hop0queryId2listCanonicalMention.containsKey(canonicalMention.getEntityId().get())) {
        hop0queryId2listCanonicalMention.put(canonicalMention.getEntityId().get(), new ArrayList<TextMention>());
      }

      hop0queryId2listCanonicalMention.get(canonicalMention.getEntityId().get()).add(canonicalMention);
    }
    reader.close();

    // debug
    for(String hop0queryId : hop0queryId2listCanonicalMention.keySet()) {
      for(TextMention kbMention : hop0queryId2listCanonicalMention.get(hop0queryId))
        System.out.println("hop0queryId2listCanonicalMention\t" + hop0queryId + "\t" +
            kbMention.getDocId() + "\t" +
            kbMention.getSpan().getStart() + "\t" + kbMention.getSpan().getEnd());
    }
  }
}
