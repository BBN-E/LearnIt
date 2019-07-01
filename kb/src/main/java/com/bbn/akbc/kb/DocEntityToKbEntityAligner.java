package com.bbn.akbc.kb;


import com.bbn.akbc.common.Pair;
import com.bbn.bue.common.files.FileUtils;
import com.bbn.serif.io.SerifXMLLoader;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.Entity;
import com.bbn.serif.theories.Mention;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by bmin on 6/9/17.
 */
public final class DocEntityToKbEntityAligner {

  private static final Logger log =
      LoggerFactory.getLogger(DocEntityToKbEntityAligner.class);

  Map<String, Map<String, Map<Pair<Integer, Integer>, Pair<String, String>>>> entType2docId2MentionSpan2kbIdAndText =
      new HashMap<String, Map<String, Map<Pair<Integer, Integer>, Pair<String, String>>>>();

  // This one will read
  // /nfs/mercury-04/bmin/projects/relation_extraction/export_to_tac_cskb_submission_format/TacFormatConverter/test4/test4.ColdStart.tsv
  void loadMentionSpanToEntityIDMapping(String file) {
    try {
      BufferedReader br = new BufferedReader(new FileReader(file));
      String curLine;

      curLine = br.readLine(); // skip the first line

      String currentKBID = ""; // bad init
      String currentType = ""; // bad init

      while ((curLine = br.readLine()) != null) {
        System.out.println(curLine);

        String[] parts = curLine.split("\t");

        if (curLine.startsWith(":") && parts.length >= 3) {

          if(parts[1].trim().equals("type")) {
            String kbId = parts[0].trim();
            String type = parts[2].trim();

            currentKBID = kbId;
            currentType = type;

            if(!entType2docId2MentionSpan2kbIdAndText.containsKey(currentType))
              entType2docId2MentionSpan2kbIdAndText.put(currentType, new HashMap<String, Map<Pair<Integer, Integer>, Pair<String, String>>>());
          }
          else if(parts[1].trim().equals("mention")) {
            String docIdAndSpan = parts[3].trim();
            String text = parts[2].trim();

            String docId = docIdAndSpan.substring(0, docIdAndSpan.indexOf(":"));

            String span = docIdAndSpan.substring(docIdAndSpan.indexOf(":")+1).trim();
            String [] offsets = span.split("-");

            int start = Integer.parseInt(offsets[0].trim());
            int end = Integer.parseInt(offsets[1].trim());

            // get Adept KBID as entity ID
            String kbId = parts[0].trim();
            // kbId = ":Entity_" + kbId.substring(kbId.indexOf("#") + 1);

            if(kbId.equals(currentKBID)) {
              if (!entType2docId2MentionSpan2kbIdAndText.get(currentType).containsKey(docId))
                entType2docId2MentionSpan2kbIdAndText.get(currentType).put(docId, new HashMap<Pair<Integer, Integer>, Pair<String, String>>());
              entType2docId2MentionSpan2kbIdAndText.get(currentType).get(docId).put(new Pair<Integer, Integer>(start, end), new Pair<String, String>(kbId, text));
            }
          }
        } else {
          log.info("Issue with string: {} {}", curLine, parts.length);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  Optional<String> getKbIdForEntity(String docId, Entity entity) {
    return getKbIdForEntity(docId, entity, Optional.<DocTheory>absent());
  }

  /*
  // Bonan: hack when Chinese offsets are wrong
  Optional<String> getKbIdForEntity(String docId, Entity entity, Optional<DocTheory> docTheoryOptional) {
    return Optional.of(entity.type().name().asString() + ":" + entity.representativeMention().mention().tokenSpan().tokenizedText(docTheoryOptional.get()));
  }
  */

  double getRatioOverlapCharacters(Pair<Integer, Integer> kbMentionSpan, Pair<Integer, Integer> characterSpan) {
    double ret = 0;

    if((kbMentionSpan.getFirst() <= characterSpan.getFirst() && kbMentionSpan.getSecond() >= characterSpan.getFirst())) {
      return 1.0f * Math.min(kbMentionSpan.getSecond() - characterSpan.getFirst(), characterSpan.getSecond() - characterSpan.getFirst()) /
          (characterSpan.getSecond() - characterSpan.getFirst());
    } else if(kbMentionSpan.getFirst() > characterSpan.getFirst() && characterSpan.getSecond() >= kbMentionSpan.getFirst()) {
      return 1.0f * Math.min(characterSpan.getSecond()-kbMentionSpan.getFirst(), kbMentionSpan.getSecond()-kbMentionSpan.getFirst()) /
          (characterSpan.getSecond() - characterSpan.getFirst());
    }
    else
      return 0;
  }


  Optional<Pair<String, String>> findAlignedKbMentionForSerifMention(String docId, Optional<DocTheory> docTheoryOptional,
      String entType, Mention mention) {
    Optional<Pair<String, String>> ret = Optional.absent();

    Pair<Integer, Integer> characterSpan = new Pair<Integer, Integer>(
        mention.span().charOffsetRange().startInclusive().asInt(),
        mention.span().charOffsetRange().endInclusive().asInt());

    if(entType2docId2MentionSpan2kbIdAndText.containsKey(entType)) {
      if (entType2docId2MentionSpan2kbIdAndText.get(entType).containsKey(docId)) {
        if (entType2docId2MentionSpan2kbIdAndText.get(entType).get(docId)
            .containsKey(characterSpan)) // exact mention span has been found in the KB
          ret = Optional.of(entType2docId2MentionSpan2kbIdAndText.get(entType).get(docId).get(characterSpan));
        else {
          double maxRatioOverlapChars = 0;

          for (Pair<Integer, Integer> kbMentionSpan : entType2docId2MentionSpan2kbIdAndText.get(entType).get(docId)
              .keySet()) {

            double ratioOverlapChars = getRatioOverlapCharacters(kbMentionSpan, characterSpan);
            if(ratioOverlapChars>maxRatioOverlapChars) {
              ret = Optional.of(entType2docId2MentionSpan2kbIdAndText.get(entType).get(docId).get(kbMentionSpan));
              maxRatioOverlapChars=ratioOverlapChars;
            }
          }
        }
      }
    }

    return ret;
  }


  // align doc-entity to kb-entity by span alignment
  Optional<String> getKbIdForEntity(String docId, Entity entity, Optional<DocTheory> docTheoryOptional) {
    String entType = entity.type().name().toString().substring(0, 3).toUpperCase();

    System.out.println("entTypt: " + entType);

    Multiset<String> alignedIds = HashMultiset.create();

    for(Mention mention : entity.mentions()) {
      Optional<Pair<String, String>> alignedId = findAlignedKbMentionForSerifMention(docId, docTheoryOptional,
          entType, mention);
      if(alignedId.isPresent())
        alignedIds.add(alignedId.get().getFirst());
    }

    if(!alignedIds.isEmpty()) {
      return Optional.of(Multisets.copyHighestCountFirst(alignedIds).elementSet().iterator().next());
    }
    else
      return Optional.absent();
  }

  public DocEntityToKbEntityAligner(String kbEntitySpanFile) {
    loadMentionSpanToEntityIDMapping(kbEntitySpanFile);
  }

  public static void main(String [] args) throws IOException {
    DocEntityToKbEntityAligner docEntityToKbEntityAligner = new DocEntityToKbEntityAligner(args[0]);
    String strFileList = args[1];

    List<File> fileList = FileUtils.loadFileList(new File(strFileList));

    final SerifXMLLoader loader = SerifXMLLoader.fromStandardACETypes(true);

    // for (final Symbol docID : docIDsToFileMap.keySet()) {
    for (final File file : fileList) {
      log.info("loading {}", file.getAbsolutePath());

      // final Optional<DocTheory> testDocTheory = Optional.of(loader.loadFrom(docIDsToFileMap.get(docID)));
      final Optional<DocTheory> testDocTheory = Optional.of(loader.loadFrom(file));

      if (testDocTheory.isPresent()) {
        String docId = testDocTheory.get().docid().asString();

        int eid=0;
        for(Entity entity : testDocTheory.get().entities()) {
          eid++;

          String entityId = docId + "-eid-" + eid;
          String entityText = entity.representativeMention().mention().tokenSpan().tokenizedText(testDocTheory.get()).utf16CodeUnits();

          for(Mention mention : entity.mentions()) {

            if(mention.isName()) {
            /*
            Optional<String> kbEntityOptional = docEntityToKbEntityAligner
                .getKbIdForEntity(testDocTheory.get().docid().asString(), entity);
            String kbEntityStr = kbEntityOptional.isPresent() ? kbEntityOptional.get() : "NA";
            System.out.println(
                entity.type().toString() + ":" + entity.representativeMention().mention()
                    .tokenSpan().tokenizedText(testDocTheory.get()) +
                    "\t->\t" + kbEntityStr);
                    */

              Optional<Pair<String, String>> alignedIdAndTextOptional =
                  docEntityToKbEntityAligner.findAlignedKbMentionForSerifMention(docId, testDocTheory,
                      entity.type().toString(), mention);

              StringBuilder stringBuilder = new StringBuilder();
              stringBuilder.append("[KB]\tserif:\t" +
                      entityId + "\t" + entityText + "\t" +
                  docId + ":" + mention.atomicHead().tokenSpan().charOffsetRange().startInclusive()
                  .asInt() + "-" + mention.atomicHead().tokenSpan().charOffsetRange().endInclusive()
                  .asInt() + "\t" +
                  mention.atomicHead().tokenSpan().tokenizedText(testDocTheory.get()));

              if (alignedIdAndTextOptional.isPresent()) {
                stringBuilder.append("\t" +
                    alignedIdAndTextOptional.get().getFirst() + "\t" + alignedIdAndTextOptional
                    .get().getSecond());
              }

              System.out.println(stringBuilder.toString());
            }
          }
        }
      }
    }
  }
}
