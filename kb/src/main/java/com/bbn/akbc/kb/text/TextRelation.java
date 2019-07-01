package com.bbn.akbc.kb.text;

import com.bbn.akbc.common.Justification;
import com.bbn.akbc.common.Pair;
import com.bbn.akbc.common.Triple;
import com.bbn.akbc.common.Utility;
import com.bbn.akbc.evaluation.tac.CSInitQuery;
import com.bbn.akbc.kb.common.Slot;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class TextRelation {

  public enum Source {
    TEXT,
    INFERED
  }

  @JsonProperty
  private TextRelationArgument arg1;
  @JsonProperty
  private TextRelationArgument arg2;

  private Source source = Source.TEXT;

  @JsonProperty
  private Slot slot;
  @JsonProperty
  private Set<TextRelationMention> mentions;
  @JsonProperty
  private Optional<String> judgement = Optional.absent();
  @JsonProperty
  private boolean isValSlot = false;

  // the confidence of filler
  private Optional<String> brandyConfidenceOfFiller = Optional.absent();

  @JsonProperty
  private CSInitQuery evalQuery;

  // for redundancy removal
  public Optional<Integer> mentionCount = Optional.absent();
  public Optional<HashSet<String>> sourcePatterns = Optional.absent();
  public List<Justification> justifications; // justification for ColdStart

  private double confidence = 0.01f;

  public TextRelation(
      @JsonProperty("arg1") TextEntity arg1,
      @JsonProperty("arg2") TextEntity arg2,
      @JsonProperty("slot") Slot slot,
      @JsonProperty("mentions") Set<TextRelationMention> mentions,
      @JsonProperty("judgement") Optional<String> judgement,
      @JsonProperty("isValueSlot") Optional<Boolean> isValueSlot,
      @JsonProperty("evalQuery") CSInitQuery evalQuery) {

    System.out.println("@JsonCreator Relation slot=" + slot);

    this.arg1 = arg1;
    this.arg2 = arg2;
    this.slot = slot;
    this.mentions = mentions;
    this.judgement = judgement;
    this.isValSlot = isValueSlot.get();
    this.evalQuery = evalQuery;
  }


  public TextRelation(TextEntity query, TextEntity answer,
      Slot slot) {
    this.arg1 = query;
    this.arg2 = answer;
    this.slot = slot;

    mentions = new HashSet<TextRelationMention>();
  }

  public TextRelation(TextEntity query, TextMention value,
      Slot slot) {
    this.arg1 = query;
    this.arg2 = value;
    this.slot = slot;

    mentions = new HashSet<TextRelationMention>();
  }

  public void setValue(TextMention valueMention) {
    this.arg2 = valueMention;
  }

  public boolean isInfered() {
    if(source == Source.INFERED)
      return true;
    else
      return false;
  }

  public void setToInfered() {
    source = Source.INFERED;
  }

  public Optional<String> getBrancyConfidenceOfFiller() {
    return brandyConfidenceOfFiller;
  }

  public TextEntity query() {
    return (TextEntity)arg1;
  }

  public void setQuery(TextEntity query) {
    arg1 = query;
  }

  public void setJustifications(List<Justification> justifications) {
    this.justifications = justifications;
  }


  public TextEntity answer() {
    return (TextEntity)arg2;
  }
  public void setAnswer(TextEntity answer) {
    arg2 = answer;
  }

  public Optional<TextMention> getArg2Value() {
    if(arg2 instanceof TextMention)
      return Optional.fromNullable((TextMention)arg2);
    else
      return Optional.absent();
  }

  public TextMention value() {
    return (TextMention) arg2;
  }

  public void setArg2Value(TextMention value) {
    arg2 = value;
  }

  public boolean isValSlot() {
    return arg2 instanceof TextMention;
  }

  public void setToValueSlot() {
    isValSlot = true;
  }

  public Optional<CSInitQuery> evalQuery() {
    return Optional.fromNullable(evalQuery);
  }

  public boolean hasEvalQuery() {
    return evalQuery != null;
  }

  public void setEvalQuery(CSInitQuery query) {
    evalQuery = query;
  }

  public void setMentions(Set<TextRelationMention> mentions) {
    this.mentions.clear();
    this.mentions.addAll(mentions);
  }

  public Set<TextRelationMention> getMentions() {
    return mentions;
  }

  public boolean hasMentionJudgement() {
    if (arg1 == null || arg2 == null) {
      return false;
    }
    for (TextRelationMention m : mentions) {
      if (m.judgementOnRelation.isPresent()) {
        return true;
      }
    }
    return false;
  }

  public void setConfidence(double confidence) {
    this.confidence = confidence;
  }

  public Slot getSlot() {
    return slot;
  }

  public double getConfidence() {
    return confidence;
  }

  public double getMaxConfidence() {
    double maxConfidence = 0;
    for(TextRelationMention kbpRelationMention : this.mentions) {
      if(kbpRelationMention.getConfidence().isPresent())
        if(kbpRelationMention.getConfidence().get()>maxConfidence) {
          maxConfidence = kbpRelationMention.getConfidence().get();
        }
    }

    return maxConfidence;
  }

  public Optional<String> getQueryType() {
    if(arg1 instanceof TextEntity)
      return ((TextEntity)arg1).getType();
    else
      return Optional.absent();
  }

  public Optional<String> getAnswerType() {
    if(arg2 instanceof TextEntity)
      return ((TextEntity)arg2).getType();
    else
      return Optional.absent();
  }

  public Optional<String> getQueryId() {
    if(arg1 instanceof TextEntity)
      return Optional.of(((TextEntity)arg1).getId());
    else
      return Optional.absent();
  }

  public Optional<String> getAnswerId() {
    if(arg2 instanceof TextEntity)
      return Optional.of(((TextEntity)arg2).getId());
    else
      return Optional.absent();
  }

  public void addMention(TextRelationMention rm) {
    mentions.add(rm);
    if (rm.judgementOnRelation.isPresent()) {
      judgement = rm.judgementOnRelation; // TODO: check consistency
    }
  }

  private String getArgSimpleString(TextRelationArgument arg) {
    if(arg instanceof TextEntity) {
      TextEntity entity = (TextEntity)arg;
      return entity.getId();
    }
    else if(arg instanceof TextMention){
      TextMention mention = (TextMention)arg;
      return mention.toString();
    }
    else {
      System.err.println("Invalid relation argument type");
      return "Error";
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("[Relation: ");
    sb.append("query=" + getArgSimpleString(arg1) + ", ");
    sb.append("answer=" + getArgSimpleString(arg2) + ", ");

    sb.append("slot=" + slot.toString() + ", ");

    for (TextRelationMention rm : mentions) {
      sb.append("RelationMention=" + rm.toString() + ", ");
    }
    sb.append("]");

    return sb.toString();
  }

  public String toSimpleString() {
    StringBuilder sb = new StringBuilder();
    sb.append("[Relation: ");
    sb.append("query=" + getArgSimpleString(arg1) + ", ");
    sb.append("answer=" + getArgSimpleString(arg2) + ", ");
    sb.append("slot=" + slot.toString() + " , ");
    sb.append("isValSlot=" + isValSlot + " , ");
    sb.append("isInfered=" + source);
    sb.append("]");

    return sb.toString();
  }

  public String toColdStartString() {
    return toColdStartString(false);
  }

  public String toColdStartString(boolean withAnnotation) {
    StringBuilder sb = new StringBuilder();

    TextEntity query = (TextEntity)arg1;

    Optional<String> valueText = Optional.absent();
    if (arg2 instanceof TextMention) {
      valueText = Optional.of("\"" + ((TextMention)arg2).getText().get() + "\"");
    }

    List<String> sortedRelationMentions = new ArrayList<String>();
    for (TextRelationMention rm : this.mentions) {
      String docId = rm.getDocId();
      String strConfidence =
          rm.confidence.isPresent() ? Double.toString(rm.confidence.get()) : "NA";
      String strSentStart =
          rm.spanOfSent().isPresent() ? Integer.toString(rm.spanOfSent().get().getStart()) : "NA";
      String strSentEnd =
          rm.spanOfSent().isPresent() ? Integer.toString(rm.spanOfSent().get().getEnd()) : "NA";

      String judgement = rm.judgementOnRelation.isPresent() ? rm.judgementOnRelation.get() : "NA";

      String strLineOfRelnMention = "";
      if (valueText.isPresent()) {
        strLineOfRelnMention =
            query.getId() + "\t" + this.slot.toString() + "\t" + valueText.get() + "\t" + docId
                + "\t"
                + rm.query.getSpan().getStart() + "\t" + rm.query.getSpan().getEnd() + "\t"
                + strSentStart + "\t" + strSentEnd + "\t"
                + rm.answer.getSpan().getStart() + "\t" + rm.answer.getSpan().getEnd() + "\t"
                + rm.slot.toString() + "\t" + strConfidence;
        if (withAnnotation) {
          strLineOfRelnMention += "\t" + judgement;
        }
        strLineOfRelnMention += "\n";
        sortedRelationMentions.add(strLineOfRelnMention);
      } else {
        strLineOfRelnMention =
            query.getId() + "\t" + this.slot.toString() + "\t" + ((TextEntity)arg2).getId() + "\t" + docId
                + "\t"
                + rm.query.getSpan().getStart() + "\t" + rm.query.getSpan().getEnd() + "\t"
                + strSentStart + "\t" + strSentEnd + "\t"
                + rm.answer.getSpan().getStart() + "\t" + rm.answer.getSpan().getEnd() + "\t"
                + rm.slot.toString() + "\t" + strConfidence;
        if (withAnnotation) {
          strLineOfRelnMention += "\t" + judgement;
        }
        strLineOfRelnMention += "\n";
        sortedRelationMentions.add(strLineOfRelnMention);
      }
    }
    Collections.sort(sortedRelationMentions);
    for (String s : sortedRelationMentions) {
      sb.append(s);
    }

    return sb.toString();
  }

  public String toColdStart2014String() {
    StringBuilder sb = new StringBuilder();

    Optional<String> valueText = Optional.absent();
    if (this.isValSlot) {
      valueText = Optional.of("\"" + this.value().getText().get() + "\"");
    }

    assert (this.mentions.size() == 1);
    System.out.println("WriteColdStart2014Submission-1\t" + this.toSimpleString());
    System.out.println(
        "WriteColdStart2014Submission-2\t" + "this.mentions.size(): " + this.mentions.size());
    // TextRelationMention rm = this.mentions.iterator().next();

    String strProvenance = "";
    for (Justification justification : this.justifications) {
      strProvenance +=
          Utility.removeMpdfSuffix(justification.docId) + ":" + justification.span.getFirst() + "-" + justification.span
              .getSecond() + ",";
    }
    if (strProvenance.endsWith(",")) {
      strProvenance = strProvenance.substring(0, strProvenance.length() - 1);
    }

    // String strConfidence = rm.confidence.isPresent() ? Double.toString(rm.confidence.get()) : "1.0";
    String strConfidence = Double.toString(this.getConfidence());

    String strLineOfRelnMention = "";
    if (valueText.isPresent()) {
      strLineOfRelnMention =
          ((TextEntity)this.query()).getId() + "\t" + this.slot.toString() + "\t" + valueText.get() + "\t"
              + strProvenance + "\t" + strConfidence;
      strLineOfRelnMention += "\n";
    } else {
      strLineOfRelnMention =
          ((TextEntity)this.query()).getId() + "\t" + this.slot.toString() + "\t" + ((TextEntity)this.answer()).getId() + "\t" + strProvenance
              + "\t" + strConfidence;
      strLineOfRelnMention += "\n";
    }
    sb.append(strLineOfRelnMention);

    return sb.toString();
  }

  // always create a new, random UUID for each value
  public String getUUID() {
    UUID uuid = UUID.randomUUID();
    return uuid.toString().replace("-", "_");
  }

  // TODO: date normalization!!!
  public String getNormalizedValue(String value) {
    return value;
  }

  public Pair<String, String> getStringEntityForValue(TextMention valueMention) {
    StringBuilder stringEntityTuples = new StringBuilder();

    String eid = ":String_" + getUUID();

    stringEntityTuples.append(eid + "\ttype\tSTRING\n");
    stringEntityTuples.append(eid + "\tmention\t" + "\"" + valueMention.getText().get().replace("\n", " ") + "\"" + "\t" +
        Utility.removeMpdfSuffix(valueMention.getDocId()) + ":" + valueMention.getSpan().getStart() + "-" + valueMention.getSpan().getEnd() + "\n");

    stringEntityTuples.append(eid + "\tnormalized_mention\t" + "\"" + getNormalizedValue(valueMention.getText().get().replace("\n", " ")) + "\"" + "\t" +
        Utility.removeMpdfSuffix(valueMention.getDocId()) + ":" + valueMention.getSpan().getStart() + "-" + valueMention.getSpan().getEnd());

    Pair<String, String> idAndString = new Pair<String, String>(eid, stringEntityTuples.toString());

    return idAndString;
  }


  public String getProvenance(String docId, TextSpan textSpan) {
    Pair<Integer, Integer> span = new Pair<Integer, Integer>(textSpan.getStart(), textSpan.getEnd());
    return getProvenance(docId, span);
  }

  public String getProvenance(String docId, Pair<Integer, Integer> span) {
    String strRelationProvenance =
        Utility.removeMpdfSuffix(docId) + ":" + span.getFirst()
            + "-" + span.getSecond();

    return strRelationProvenance;
  }

  public String toColdStart2017String() {
    StringBuilder sb = new StringBuilder();

    String slot = this.slot.toString();

    // assert (this.mentions.size() == 1);

    System.out.println("WriteColdStart2017Submission-1\t" + this.toSimpleString());
    System.out.println(
        "WriteColdStart2017Submission-2\t" + "this.mentions.size(): " + this.mentions.size());

    for(TextRelationMention textRelationMention : this.mentions) {
      Justification justification = textRelationMention.getJustificaion();
      String strRelationProvenance = "";

      strRelationProvenance += getProvenance(justification.docId, justification.span) + ",";
      if (strRelationProvenance.endsWith(",")) {
        strRelationProvenance = strRelationProvenance.substring(0, strRelationProvenance.length() - 1);
      }

      // String strConfidence = rm.confidence.isPresent() ? Double.toString(rm.confidence.get()) : "1.0";
      String strConfidence = Double.toString(textRelationMention.getConfidence().get());

      String strLineOfRelnMention = "";
      if (this.isValSlot()) {
        Pair<String, String> valueSlotIdAndString = getStringEntityForValue(textRelationMention.answer);

        String fillerProvenance = getProvenance(textRelationMention.answer.getDocId(), textRelationMention.answer.getSpan());

        // create a STRING entity for the value mention
        strLineOfRelnMention = valueSlotIdAndString.getSecond() + "\n";
        strLineOfRelnMention +=
            ((TextEntity) this.query()).getId() + "\t" + slot + "\t"
                + valueSlotIdAndString.getFirst() + "\t"
                + fillerProvenance + ";" + strRelationProvenance + "\t" + strConfidence + "\n"; // filler_provenance then followed by relation_predicate_provenance
      } else {
        strLineOfRelnMention =
            ((TextEntity) this.query()).getId() + "\t" + slot + "\t"
                + ((TextEntity) this.answer()).getId() + "\t" + strRelationProvenance
                + "\t" + strConfidence;
        strLineOfRelnMention += "\n";
      }
      sb.append(strLineOfRelnMention);
    }

    return sb.toString();
  }

  /*
  public String toColdStart2017String() {
    StringBuilder sb = new StringBuilder();

    assert (this.mentions.size() == 1);
    System.out.println("WriteColdStart2017Submission-1\t" + this.toSimpleString());
    System.out.println("WriteColdStart2017Submission-2\t" + "this.mentions.size(): " + this.mentions.size());

    String strProvenance = "";
    for (Justification justification : this.justifications) {
      strProvenance +=
          Utility.removeMpdfSuffix(justification.docId) + ":" + justification.span.getFirst() + "-" + justification.span
              .getSecond() + ",";
    }
    if (strProvenance.endsWith(",")) {
      strProvenance = strProvenance.substring(0, strProvenance.length() - 1);
    }

    // String strConfidence = rm.confidence.isPresent() ? Double.toString(rm.confidence.get()) : "1.0";
    String strConfidence = Double.toString(this.getConfidence());

    String strLineOfRelnMention = "";
    if (this.isValSlot()) {

      Pair<String, String> valueSlotIdAndString = getStringEntityForValue(this.value());

      // create a STRING entity for the value mention
      strLineOfRelnMention = valueSlotIdAndString.getSecond() + "\n";
      strLineOfRelnMention += ((TextEntity)this.query()).getId() + "\t" + this.slot.toString() + "\t" + valueSlotIdAndString.getFirst() + "\t"
              + strProvenance + "\t" + strConfidence + "\n";
    } else {
      strLineOfRelnMention =
          ((TextEntity)this.query()).getId() + "\t" + this.slot.toString() + "\t" + ((TextEntity)this.answer()).getId() + "\t" + strProvenance
              + "\t" + strConfidence;
      strLineOfRelnMention += "\n";
    }
    sb.append(strLineOfRelnMention);

    return sb.toString();
  }
  */

/*
        public String toColdStart2014String() {
		StringBuilder sb = new StringBuilder();

		Optional<String> valueText = Optional.absent();
		if(this.isValSlot)
			valueText = Optional.of("\"" + this.value.text.get() + "\"");

		assert(this.mentions.size()==1);
//		System.out.println("this.mentions.size(): " + this.mentions.size());
		RelationMention rm = this.mentions.iterator().next();

		String strProvenance = "";
		for(Justification justification : rm.listJustifications) {
			strProvenance += justification.docId + ":" + justification.span.getFirst() + "-" + justification.span.getSecond() + ",";
		}
		if(strProvenance.endsWith(","))
			strProvenance = strProvenance.substring(0, strProvenance.length()-1);

		String strConfidence = rm.confidence.isPresent()?Double.toString(rm.confidence.get()):"1.0";

		String strLineOfRelnMention = "";
		if(valueText.isPresent()) {
			strLineOfRelnMention = this.query.id + "\t" + this.slot.toString() + "\t" + valueText.get() + "\t" + strProvenance + "\t" + strConfidence;
			strLineOfRelnMention += "\n";
		}
		else {
			strLineOfRelnMention = this.query.id + "\t" + this.slot.toString() + "\t" + this.answer.id + "\t" + strProvenance + "\t" + strConfidence;
			strLineOfRelnMention += "\n";
		}
		sb.append(strLineOfRelnMention);

		return sb.toString();
	}
*/

  public String getValueID(String value) {
    return "VAL:" + value.toLowerCase().trim().replace(" ", "_");
  }

  @Override
  public int hashCode() {
    String relnType = this.slot.toString();

    // value slot
    if (this.isValSlot) {
      String strDst = getValueID(this.value().getText().get());
      Triple triple = new Triple(((TextEntity)this.query()).getId(), relnType, strDst);
      return triple.toString().hashCode();
    } else {
//      System.out.println("this.query()=" + this.query());
 //     System.out.println("this.answer()=" + this.answer());
   //   System.out.println("relnType=" + relnType);

      Triple triple = new Triple(((TextEntity)this.query()).getId(), relnType, ((TextEntity)this.answer()).getId());
      return triple.toString().hashCode();
    }


		/*
                final int prime = 31;
		int result = 1;
		result = prime * result + query.id.hashCode();

		result = prime * result + slot.toString().hashCode();

		if(isValSlot)
			result = prime * result + (new String("VALUE-SLOT")).hashCode();
		else
			result = prime * result + (new String("ENTITY-SLOT")).hashCode();

		if(value!=null)
			result = prime * result + value.hashCode();
		else
			result = prime * result + answer.id.hashCode();

//		for(RelationMention rm : this.mentions)
//			result = prime * result + rm.hashCode();
		return result;
		*/
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
    TextRelation other = (TextRelation) obj;

    if (!query().equals(other.query())) {
      return false;
    }

    if (!slot.equals(other.slot)) {
      return false;
    }

    if (this.isValSlot != other.isValSlot) {
      return false;
    }

    if (this.isValSlot) {
      String strDst = getValueID(this.value().getText().get());
      String strOtherDst = getValueID(other.value().getText().get());
      if (!strDst.equals(strOtherDst)) {
        return false;
      }
    } else {
      if (!answer().equals(other.answer())) {
        return false;
      }
    }

    return true;


		/*
                if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Relation other = (Relation) obj;

		if (!query.equals(other.query))
			return false;

		if (!slot.equals(other.slot))
			return false;

		if(this.isValSlot!=other.isValSlot)
			return false;

		if (value!=null) {
			if(!value.equals(other.value))
				return false;
		}
		else {
			if(!answer.equals(other.answer))
				return false;
		}
//		for(RelationMention rm : other.mentions)
//			if(!this.mentions.contains(rm))
//				return false;
		return true;
		*/
  }
        /*
        @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + query.hashCode();
		if(value!=null)
			result = prime * result + value.hashCode();
		else
			result = prime * result + answer.hashCode();
		result = prime * result + slot.hashCode();
		for(RelationMention rm : this.mentions)
			result = prime * result + rm.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Relation other = (Relation) obj;
		if (!query.equals(other.query))
			return false;
		if (value!=null) {
			if(!value.equals(other.value))
				return false;
		}
		else {
			if(!answer.equals(other.answer))
				return false;
		}
		if (!slot.equals(other.slot))
			return false;
		for(RelationMention rm : other.mentions)
			if(!this.mentions.contains(rm))
				return false;
		return true;
	}
	*/
}
