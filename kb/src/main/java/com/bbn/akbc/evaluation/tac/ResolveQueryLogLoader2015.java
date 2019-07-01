package com.bbn.akbc.evaluation.tac;

import com.bbn.akbc.kb.text.TextMention;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by bmin on 11/7/15.
 */
public class ResolveQueryLogLoader2015 {
  public static Map<String, String> hop0QueryToEntityInSysOut;
  public static Map<String, TextMention> hop1Query2Mention;

  public static void init(String resolveQueryLog) throws IOException {
    loadHop0Query2Entity(resolveQueryLog);
    loadHop1Query2Mention(resolveQueryLog);
  }

  public static void loadHop0Query2Entity(String resolveQueriesLog) throws IOException {
    hop0QueryToEntityInSysOut = new HashMap<String, String>();

    BufferedReader reader = new BufferedReader(new FileReader(resolveQueriesLog));
    String sline;
    while ((sline = reader.readLine()) != null) {
      String[] fields = sline.trim().split("\t");
      if (fields.length != 3) {
        continue;
      }

      if (!fields[0].equals("dbg")) {
        continue;
      }

      hop0QueryToEntityInSysOut.put(fields[1], fields[2]);

      System.out.println("hop0QueryToEntityInSysOut.put" + "\t" + fields[1] + "\t" + fields[2]);
    }
    reader.close();
  }

  public static void loadHop1Query2Mention(String resolveQueriesLog) throws IOException, StringIndexOutOfBoundsException {
    hop1Query2Mention = new HashMap<String, TextMention>();

    BufferedReader reader = new BufferedReader(new FileReader(resolveQueriesLog));
    String sline;
    while ((sline = reader.readLine()) != null) {
      if(!sline.startsWith("query1"))
        continue;

      String[] fields = sline.trim().split("\t");
      if(fields.length<3)
        continue;


      String queryId = fields[0].substring(fields[0].indexOf(" ") + 1);

      System.out.println("loadHop1Query2Mention sline: " + sline);

      int idxStart = fields[3].indexOf(":") + 1;
      int idxEnd = fields[3].indexOf("-");

      if(idxStart<0 || idxEnd<0 || idxEnd<idxStart)
        continue;

      String docId = fields[3].substring(0, fields[3].indexOf(":"));

      int start = Integer.parseInt(
          fields[3].substring(idxStart, idxEnd));
      int end = Integer.parseInt(fields[3].substring(idxEnd+1));

      String text = fields[2].trim();
      TextMention kbMention = new TextMention(docId, start, end, text);

      hop1Query2Mention.put(queryId, kbMention);

      System.out.println("queryToMention.put" + "\t" + queryId + "\t" + kbMention.getDocId() + "\t" +
        kbMention.getSpan().getStart() + "\t" + kbMention.getSpan().getEnd());
    }
    reader.close();
  }
}
