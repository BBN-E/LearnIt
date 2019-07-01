package com.bbn.akbc.common;

import com.bbn.akbc.kb.ColdStartKB;
import com.bbn.akbc.kb.text.TextEntity;
import com.bbn.akbc.kb.text.TextMention;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TypeCloner {

  //	ColdStartKB kb;
  Map<String, Set<String>> canonicalname2eids;

  Map<String, Set<TextMention>> eid2canonicalMentions;
  Map<String, Set<TextMention>> eid2mentions;


  public TypeCloner(ColdStartKB kb) {
//		this.kb = kb;
    init(kb);
  }

  private void init(ColdStartKB kb) {
    canonicalname2eids = new HashMap<String, Set<String>>();
    eid2canonicalMentions = new HashMap<String, Set<TextMention>>();
    eid2mentions = new HashMap<String, Set<TextMention>>();

    for (String eid : kb.getId2entities().keySet()) {
      TextEntity entity = kb.getId2entities().get(eid);

      // only do per and org confusion
      if (!entity.getTypes().contains("per") && !entity.getTypes().contains("org")) {
        continue;
      }

      for (TextMention canonicalMention : entity.getCanonicalMentions()) {
        String canonicalName = canonicalMention.getText().get();

        if (!canonicalname2eids.containsKey(canonicalName)) {
          canonicalname2eids.put(canonicalName, new HashSet<String>());
        }

        canonicalname2eids.get(canonicalName).add(entity.getId());

        if (!eid2canonicalMentions.containsKey(eid)) {
          eid2canonicalMentions.put(eid, new HashSet<TextMention>());
        }
        eid2canonicalMentions.get(eid).add(canonicalMention);
      }

      for (TextMention mention : entity.getMentions()) {
        if (!eid2mentions.containsKey(eid)) {
          eid2mentions.put(eid, new HashSet<TextMention>());
        }
        eid2mentions.get(eid).add(mention);
      }
    }

//		canonicalname2eid = canonicalname2eidBuilder.build();
  }

  public void printStats() {
    for (String canonicalname : canonicalname2eids.keySet()) {
      Set<String> eids = canonicalname2eids.get(canonicalname);
      for (String eid : eids) {
        System.out.println("printStats\t" + eids.size() + "\t" + canonicalname + "\t" + eid);
      }
    }
  }

  String getEntityTypeFromID(String eid) {
    if (!eid.startsWith(":")) {
      System.err.println("error in eid: " + eid);
      System.exit(-1);
    }

    return eid.substring(1, eid.indexOf("_")).toLowerCase();
  }

  public ColdStartKB cloneEntitiesForTypeExtension(ColdStartKB kb) {
    for (String canonicalname : canonicalname2eids.keySet()) {
      Set<TextMention> canonicalMentions = new HashSet<TextMention>();
      Set<TextMention> mentions = new HashSet<TextMention>();

      Set<String> eids = canonicalname2eids.get(canonicalname);
      for (String eid : eids) {
        canonicalMentions.addAll(eid2canonicalMentions.get(eid));
        mentions.addAll(eid2mentions.get(eid));
      }

      for (String eid : eids) {
        TextEntity entity = kb.getId2entities().get(eid);
        String type1 = getEntityTypeFromID(entity.getId());

        // only do per and org confusion
        if (!entity.getTypes().contains("per") && !entity.getTypes().contains("org")) {
          continue;
        }

        Set<String> docsHaveCanonicalMention = new HashSet<String>();
        for (TextMention m : entity.getCanonicalMentions()) {
          docsHaveCanonicalMention.add(m.getDocId());
        }

        // only add if the document don't have a canonical_mention
        for (TextMention m : canonicalMentions) {
          String type2 = getEntityTypeFromID(m.getEntityId().get());

          if (!entity.getCanonicalMentions().contains(m) &&
              !docsHaveCanonicalMention.contains(m.getDocId()) &&
              !type1.equals(type2)) {

            System.out.println(
                "cloneEntitiesForTypeExtension:\t" + "canonical" + "\t" + m.getEntityId().get() + " -> "
                    + entity.getId() + "\t" + m.toString());

            TextMention mention = new TextMention(m.getDocId(),
                m.getSpan().getStart(), m.getSpan().getEnd());
            mention.setText(m.getText().get());
            mention.setEntityId(entity.getId());
            // mention.setConfidence(m.confidence.get());
            // mention.setLinkConfidence(m.linkConfidence.get());

            // TODO: check
            // mention.setBrandyConfidence(m.brandyConfidence.get());

            entity.addCanonicalMention(mention);

            docsHaveCanonicalMention.add(m.getDocId());
          }
        }

        for (TextMention m : mentions) {
          String type2 = getEntityTypeFromID(m.getEntityId().get());

          if (!entity.getMentions().contains(m) &&
              !type1.equals(type2)) {
            System.out.println(
                "cloneEntitiesForTypeExtension:\t" + "mention" + "\t" + m.getEntityId().get() + " -> "
                    + entity.getId() + "\t" + m.toString());

            TextMention mention = new TextMention(m.getDocId(), m.getSpan().getStart(), m.getSpan().getEnd());
            mention.setText(m.getText().get());
            mention.setEntityId(entity.getId());
            mention.setConfidence(m.getConfidence().get());
            mention.setLinkConfidence(m.getLinkConfidence().get());

            mention.setBrandyConfidence(m.getBrandyConfidence().get());

            entity.addMention(mention);
          }
        }
      }
    }

    return kb;
  }


  public static void main(String[] argv) {
    String strFileKBInput = argv[0];

    ColdStartKB kb = (new ColdStartKB.Builder("BBN"))
        .fromFile(strFileKBInput); // filters non-name values for per/org:alternate_names

    TypeCloner typeCloner = new TypeCloner(kb);
    typeCloner.printStats();
    kb = typeCloner.cloneEntitiesForTypeExtension(kb);
  }
}
