package com.bbn.akbc.evaluation.tac;


import com.bbn.akbc.resource.Resources;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.SentenceTheory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE)
public class CanonicalMention {

  @JsonProperty
  public String id;
  @JsonProperty
  public String text;
  @JsonProperty
  public String docid;
  @JsonProperty
  public int beg;
  @JsonProperty
  public int end;

  public CanonicalMention(String id, String text, String docId, int beg, int end) {
    this.id = id;
    this.text = text;
    this.docid = docId.replace("null", "");
    this.beg = beg;
    this.end = end;
  }

  public String getAnnotatedSentText() {
    DocTheory dt = Resources.getDocTheory(docid);
    if (dt != null) {
      SentenceTheory st = Resources.getSentence(dt, beg, end);
      if (st != null) {
        int sentStart = st.span().startToken().startCharOffset().value();
        int sentEnd = st.span().endToken().endCharOffset().value();

        String sentText = dt.document().originalText().content().utf16CodeUnits().substring(sentStart, sentEnd);

        String str1 = sentText.substring(0, beg - sentStart);
        String str2 = sentText.substring(beg - sentStart, end - sentStart);
        String str3 = sentText.substring(end - sentStart);
        return str1 + "<u><b>" + str2 + "</b></u>" + str3;
      }
    }

    return "";
  }

  @Override
  public String toString() {
    return "CanonicalMention [id=" + id + ", text=" + text + ", docid="
        + docid + ", beg=" + beg + ", end=" + end + "]";
  }
}
