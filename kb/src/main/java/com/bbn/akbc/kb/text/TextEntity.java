package com.bbn.akbc.kb.text;

import com.bbn.akbc.common.Utility;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * TODO:
 * 	- consolidate entities: pick a best canonical mention,
 * 	and a best type (maybe should allow multiple types)
 */

@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@refId")
public class TextEntity extends TextRelationArgument {

  @JsonProperty
  private String id;
  @JsonProperty
  private Map<String, Double> type2confidence;
  @JsonProperty
  private Map<TextMention, Double> canonicalMention2confidence;
  @JsonProperty
  private Map<TextMention, Double> mention2confidence;

  private Optional<TextMention> canonicalMention;

  private Optional<String> bestNameString;

  private Multimap<String, TextMention> docid2mentions = HashMultimap.create();
  private Multimap<String, TextMention> docid2canonicalmentions = HashMultimap.create();
  /*
   * to delete
   */
  public Optional<String> htmlForD2D;

  private Optional<Double> confidence = Optional.of(0.5);//default

  @JsonCreator
  public TextEntity(
      @JsonProperty("id") String id,
      @JsonProperty("type2confidence") Map<String, Double> type2confidence,
      @JsonProperty("canonicalMention2confidence") Map<TextMention, Double> canonicalMention2confidence,
      @JsonProperty("mention2confidence") Map<TextMention, Double> mention2confidence,
      @JsonProperty("bestNameString") Optional<String> bestNameString) {

   // System.out.println("@JsonCreator EntityInfo id=" + id);

    this.id = id;
    this.type2confidence = type2confidence;
    this.canonicalMention2confidence = canonicalMention2confidence;
    this.mention2confidence = mention2confidence;
    this.bestNameString = bestNameString;
  }

  public TextEntity(String id) {
    this.id = id;

    type2confidence = new HashMap<String, Double>();
    canonicalMention2confidence = new HashMap<TextMention, Double>();
    mention2confidence = new HashMap<TextMention, Double>();
    bestNameString = Optional.absent();
  }

  public boolean isValid() {
    if(type2confidence.isEmpty() || canonicalMention2confidence.isEmpty())
      return false;

    return true;
  }

  public double getConfidence() {
    if(confidence.isPresent())
      return confidence.get();
    else
      return 0.0;
  }

  public Multimap<String, TextMention> get_docid2mentions() {
    if(docid2mentions.isEmpty()) {
      for(TextMention textMention : mention2confidence.keySet()) {
        docid2mentions.put(textMention.getDocId(), textMention);
      }
    }

    return docid2mentions;
  }

  public Multimap<String, TextMention> get_docid2canonicalmentions() {
    if(docid2canonicalmentions.isEmpty()) {
      for(TextMention textMention : canonicalMention2confidence.keySet()) {
        docid2canonicalmentions.put(textMention.getDocId(), textMention);
      }
    }

    return docid2canonicalmentions;
  }

  public Optional<String> bestName() {
    return this.getCanonialMention().get().getText();
  }

  /*
   * functions on types
   */
  public void addType(String type, double confidence) {
    type2confidence.put(type, confidence);
  }

  public void addType(String type) {
    addType(type, 1.0f);
  }

  public void setTypes(Collection<String> types) {
    type2confidence.clear();
    for(String type : types)
      type2confidence.put(type, 1.0);
  }

  public Optional<String> getType() {
    double maxConfidence=0.0f;
    Optional<String> bestType = Optional.absent();

    for(String type : type2confidence.keySet()) {
      double currentConfidence = type2confidence.get(type);
      if(currentConfidence>maxConfidence) {
        maxConfidence = currentConfidence;
        bestType = Optional.of(type);
      }
    }

    if(bestType.isPresent())
      return bestType;
    else
      return Optional.absent();
  }

  public Set<String> getTypes() {
    return type2confidence.keySet();
  }
  //

  /*
   * functions on canonical mentions
   */
  public void addCanonicalMention(TextMention mention, double confidence) {
    canonicalMention2confidence.put(mention, confidence);
  }

  public void addCanonicalMention(TextMention mention) {
    addCanonicalMention(mention, 1.0f);
  }

  public Optional<TextMention> getCanonialMention() {
    selectGlobalCanonicalMention();
    return canonicalMention;
    /**
     * select by confidence

    double maxConfidence=0.0f;
    Optional<TextMention> bestCanonicalMention = Optional.absent();

    for(TextMention mention : canonicalMention2confidence.keySet()) {
      double currentConfidence = canonicalMention2confidence.get(mention);
      if(currentConfidence>maxConfidence) {
        maxConfidence = currentConfidence;
        bestCanonicalMention = Optional.of(mention);
      }
    }

    if(bestCanonicalMention.isPresent())
      return bestCanonicalMention;
    else
      return Optional.absent();
     */
  }

  public void setCanonicalMentions(Set<TextMention> canonicalMentions) {
    canonicalMention2confidence.clear();

    for(TextMention canonicalMention : canonicalMentions)
      canonicalMention2confidence.put(canonicalMention, 1.0);
  }

  public void setCanonicalMention(TextMention canonicalMention) {
    canonicalMention2confidence.clear();
    canonicalMention2confidence.put(canonicalMention, 1.0);
    this.canonicalMention = Optional.of(canonicalMention);
  }
  //

  /*
   * functions on mentions
   */
  public void addMention(TextMention mention, double confidence) {
    mention2confidence.put(mention, confidence);
  }

  public void addMention(TextMention mention) {
    addMention(mention, 1.0f);
  }
  //

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }
  public void setType(String type) {
    this.type2confidence.clear();
    this.type2confidence.put(type, 1.0);
  }

  public Set<TextMention> getMentions() {
    return mention2confidence.keySet();
  }

  public void removeMention(TextMention textMention) {
    mention2confidence.remove(textMention);
  }

  public Set<TextMention> getCanonicalMentions() {
    return canonicalMention2confidence.keySet();
  }

  public void removeCanonicalMention(TextMention textMention) {
    canonicalMention2confidence.remove(textMention);
  }

  public void setMentions(Set<TextMention> mentions) {
    mention2confidence.clear();

    for(TextMention mention : mentions)
      mention2confidence.put(mention, 1.0);
  }

  /*
   * pick the first one
   */
  public void fillInBestName() {
    if(!canonicalMention2confidence.isEmpty()) {
      TextMention mention = canonicalMention2confidence.keySet().iterator().next();
      bestNameString = mention.getText();
    }
  }

  public void fillInHtmlForD2D(String htmlForD2D) {
    this.htmlForD2D = Optional.of(htmlForD2D);
  }

  public boolean hasMention(TextMention textMention) {
    for (TextMention mention : mention2confidence.keySet()) {
      if (mention.getSpan().overlapWith(textMention.getSpan())) {
        return true;
      }
    }
    return false;
  }

  /*
   * select the most common canonical mention as the global canonical mention
   */
  public void selectGlobalCanonicalMention() {
    Multimap<String, TextMention> text2mentions = HashMultimap.create();
    for(TextMention mention : canonicalMention2confidence.keySet()) {
//      if(!mention.getText().get().isEmpty())
      text2mentions.put(mention.getText().get(), mention);
    }

    // choose the most common one
    int maxCount=0;
    Optional<TextMention> mostCommonMention = Optional.absent();
    for(String text : text2mentions.keys()) {
      int mentionCount = text2mentions.get(text).size();
      if(mentionCount > maxCount) {
        maxCount = mentionCount;
        mostCommonMention = Optional.of(text2mentions.get(text).iterator().next());
      }
    }

    if(maxCount>0) {
      canonicalMention = mostCommonMention;
    }
  }


  public String toSimpleString() {
    StringBuilder sb = new StringBuilder();
    sb.append("TextEntity: ");
    sb.append("type: ");
    for (String type : this.type2confidence.keySet()) {
      sb.append(type + ",");
    }
    sb.append(", ");
    sb.append("#mentions: " + this.mention2confidence.size() + ", ");
    sb.append("#canonicalMention: " + this.canonicalMention2confidence.size());
    return sb.toString().trim();
  }


  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("TextEntity: ");
    sb.append("type: ");
    for (String type : this.type2confidence.keySet()) {
      sb.append(type + ", ");
    }

    for (TextMention canonicalMention : this.canonicalMention2confidence.keySet()) {
      sb.append("[canonical_mention: " + canonicalMention.toString() + "], ");
    }

    for (TextMention mention : this.mention2confidence.keySet()) {
      sb.append("[mention: " + mention.toString() + "], ");
    }

    return sb.toString();
  }

  public String toColdStartString() {
    StringBuilder sb = new StringBuilder();

    List<String> sortedTypes = new ArrayList<String>(this.type2confidence.keySet());
    Collections.sort(sortedTypes);
    for (String type : sortedTypes) {
      sb.append(id + "\t" + "type" + "\t" + type + "\n");
    }

    List<String> sortedCanonicalMentions = new ArrayList<String>();
    for (TextMention canonicalMention : canonicalMention2confidence.keySet()) {
      sortedCanonicalMentions.add(
          id + "\t" + "canonical_mention" + "\t" + "\"" + canonicalMention.getText().get() + "\"" + "\t"
              + canonicalMention.getDocId() + "\t"
              + canonicalMention.getSpan().getStart() + "\t"
              + canonicalMention.getSpan().getEnd() + "\n");
    }
    Collections.sort(sortedCanonicalMentions);
    for (String s : sortedCanonicalMentions) {
      sb.append(s);
    }

    List<String> sortedMentions = new ArrayList<String>();
    for (TextMention mention : mention2confidence.keySet()) {
      sortedMentions.add(id + "\t"
          + "mention" + "\t" + "\"" + mention.getText().get() + "\"" + "\t"
          + mention.getDocId() + "\t"
          + mention.getSpan().getStart() + "\t"
          + mention.getSpan().getEnd() + "\n");
    }
    Collections.sort(sortedMentions);
    for (String s : sortedMentions) {
      sb.append(s);
    }

    return sb.toString();
  }

  public String toColdStart2014String() {
    return toColdStart2014String(false);
  }

  public String toColdStart2014String(boolean writeSpecialPredicateForNominals) {
    StringBuilder sb = new StringBuilder();

    List<String> sortedTypes = new ArrayList<String>(type2confidence.keySet());
    Collections.sort(sortedTypes);
    for (String type : sortedTypes) {
      sb.append(id + "\t" + "type" + "\t" + type.toUpperCase() + "\n");
    }

    List<String> sortedCanonicalMentions = new ArrayList<String>();
    for (TextMention canonicalMention : canonicalMention2confidence.keySet()) {
      sortedCanonicalMentions.add(id + "\t"
              + "canonical_mention" + "\t" + "\"" + canonicalMention.getText().get().replace("\n", " ") + "\"" + "\t"
              + Utility.removeMpdfSuffix(canonicalMention.getDocId()) + ":" + canonicalMention.getSpan().toColdStart2014String() + "\n");
    }
    Collections.sort(sortedCanonicalMentions);
    for (String s : sortedCanonicalMentions) {
      sb.append(s);
    }

    List<String> sortedMentions = new ArrayList<String>();
    for (TextMention mention : mention2confidence.keySet()) {
      String predicate = "mention";
      if(writeSpecialPredicateForNominals) { // set it to nominal_mention if it is nominals
        if(mention.getBrandyConfidence().isPresent())
          if(mention.getBrandyConfidence().get().contains("Desc")) {
            predicate = "nominal_mention";
          }
      }
      sortedMentions.add(id + "\t" + predicate + "\t" + "\"" + mention.getText().get().replace("\n", " ") + "\"" + "\t"
          + Utility.removeMpdfSuffix(mention.getDocId()) + ":" + mention.getSpan().toColdStart2014String() + "\n");
    }
    Collections.sort(sortedMentions);
    for (String s : sortedMentions) {
      sb.append(s);
    }

    return sb.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;

    result = prime * result + id.hashCode(); // id must be uniq
                /* too slow
                for(KBmention mention : this.mentions)
			result = prime * result + mention.hashCode();
		for(KBmention mention : this.canonicalMentions)
			result = prime * result + mention.hashCode();
			*/
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    TextEntity other = (TextEntity) obj;

    if (!this.id.equals(other.id)) {
      return false;
    }
                /* too slow
                for(KBmention mention : other.mentions)
			if(!this.mentions.contains(mention))
				return false;
		for(KBmention mention : other.canonicalMentions)
			if(!this.canonicalMentions.contains(mention))
				return false;
				*/
    return true;
  }
}
