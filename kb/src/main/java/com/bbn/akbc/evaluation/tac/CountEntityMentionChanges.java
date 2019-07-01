package com.bbn.akbc.evaluation.tac;

import com.bbn.akbc.common.FileUtil;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import java.util.List;

/**
 * Created by bmin on 11/12/15.
 */
public class CountEntityMentionChanges {
  public static void main(String [] argv) {
    Multiset<String> docEntityMentionsInRun1 = updateCounter(argv[0]);
    Multiset<String> docEntityMentionsInRun2 = updateCounter(argv[1]);

    // ColdStartKB kb1 = (new ColdStartKB.Builder()).fromFile(argv[2]);

    int totalDocEntity = 0;
    int totalDocEntityChanged = 0;

    int totalMention = 0;
    int totalMentionChangeItsEntity = 0;

    for(String docEntityName : docEntityMentionsInRun1.elementSet()) {
      totalDocEntity++;
      totalMention+=docEntityMentionsInRun1.count(docEntityName);
    }

    for(String docEntityName : docEntityMentionsInRun2.elementSet()) {
      int numMentionInRun1=0;
      if(docEntityMentionsInRun1.contains(docEntityName))
        numMentionInRun1=docEntityMentionsInRun1.count(docEntityName);

      int numMentionInRun2=0;
      if(docEntityMentionsInRun2.contains(docEntityName))
        numMentionInRun2=docEntityMentionsInRun2.count(docEntityName);

      if(numMentionInRun1!=0) {
        if(numMentionInRun1!=numMentionInRun2) {
          System.out.println("CHANGED docEntityName: " + docEntityName);
          totalDocEntityChanged++;
        }

        int numDiff=numMentionInRun2-numMentionInRun1;
//      if(numDiff>0) {
//        System.out.println("diff\t" + numDiff + "\t" + numMentionInRun1 + "\t" + numMentionInRun2 + "\t" + docEntityName);
          totalMentionChangeItsEntity += Math.abs(numDiff);
//      }
      }
    }

    System.out.println("totalDocEntity:\t" + totalDocEntity);
    System.out.println("totalDocEntityChanged:\t" + totalDocEntityChanged);
    System.out.println("totalMention:\t" + totalMention);
    System.out.println("totalMentionChangeItsEntity:\t" + totalMentionChangeItsEntity);
  }

  public static Multiset<String> updateCounter(String strFileOfLines) {
    List<String> linesInRun = FileUtil.readLinesIntoList(strFileOfLines);
    Multiset<String> docEntityMentionsInRun = HashMultiset.<String>create();
    for(String line : linesInRun) {
      line = line.trim();
      if(line.isEmpty()) continue;

      String [] fields = line.split(" ");
      docEntityMentionsInRun.add(fields[1], Integer.parseInt(fields[0]));
    }

    return docEntityMentionsInRun;
  }
}
