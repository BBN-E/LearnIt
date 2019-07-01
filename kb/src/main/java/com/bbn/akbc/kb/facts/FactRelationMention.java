package com.bbn.akbc.kb.facts;

import com.bbn.akbc.kb.common.Slot;
import com.bbn.akbc.kb.common.SlotFactory;
import com.bbn.akbc.common.SimpleConfidences;
import com.bbn.akbc.kb.text.TextMention;
import com.bbn.akbc.kb.text.TextRelationMention;
import com.bbn.akbc.kb.text.TextSpan;
import com.bbn.akbc.common.format.DateKBPnormalizer;

import com.bbn.akbc.common.format.Normalization;
import com.google.common.base.Optional;

/**
 * Created by bmin on 9/21/15.
 */
public class FactRelationMention {
  String filler;
  boolean isValue;

  String relnType;
  String docId;

  String role1;

  // for agent1
  String srcEntityId;
  String queryType;
  String querySubType;
  int arg1_start;
  int arg1_end;
  double arg1_confidence;
  double arg1_linkConfidence;
  String arg1_brandyConfidence;

  // for filler
  String dstEntityId;
  String answerType;
  String answerSubType;
  int arg2_start;
  int arg2_end;
  double arg2_confidence;
  double arg2_linkConfidence;
  String arg2_brandyConfidence;

  int sent_start;
  int sent_end;

  double confidence;

  String strTextArg1;
  String strTextArg2;

  public String getSrcEntityId() {
    return srcEntityId;
  }

  public String getDstEntityId() {
    return dstEntityId;
  }

  public boolean getIsValue() {
    return isValue;
  }

  FactRelationMention(String filler,
      boolean isValue,
      String relnType,
      String docId,
      String role1,
      // for agent1
      String srcEntityId,
      String queryType,
      String querySubType,
      int arg1_start,
      int arg1_end,
      double arg1_confidence,
      double arg1_linkConfidence,
      String arg1_brandyConfidence,
      // for filler
      String dstEntityId,
      String answerType,
      String answerSubType,
      int arg2_start,
      int arg2_end,
      double arg2_confidence,
      double arg2_linkConfidence,
      String arg2_brandyConfidence,
      // for relation mention
      int sent_start,
      int sent_end,
      double confidence,
      String strTextArg1,
      String strTextArg2
  ) {
    this.isValue = isValue;
    this.relnType = relnType;
    this.docId = docId;
    this.role1 = role1;
    // for agent1
    this.srcEntityId = srcEntityId;
    this.queryType = queryType;
    this.querySubType = querySubType;
    this.arg1_start = arg1_start;
    this.arg1_end = arg1_end;
    this.arg1_confidence = arg1_confidence;
    this.arg1_linkConfidence = arg1_linkConfidence;
    this.arg1_brandyConfidence = arg1_brandyConfidence;
    // for filler
    this.dstEntityId = dstEntityId;
    this.answerType = answerType;
    this.answerSubType = answerSubType;
    this.arg2_start = arg2_start;
    this.arg2_end = arg2_end;
    this.arg2_confidence = arg2_confidence;
    this.arg2_linkConfidence = arg2_linkConfidence;
    this.arg2_brandyConfidence = arg2_brandyConfidence;
    // for relation mention
    this.sent_start = sent_start;
    this.sent_end = sent_end;
    this.confidence = confidence;
    this.strTextArg1 = strTextArg1;
    this.strTextArg2 = strTextArg2;
  }

  public static FactRelationMention fromLine(String sline) throws Exception {
    return fromLine(sline, false);
  }

  public static FactRelationMention fromLine(String sline, boolean adjust_offsets_for_adept) throws Exception {
    sline = sline.replace("\t\t", "\t");
    String [] fields = sline.trim().split("\t");

    if (fields.length < 25) {
      throw new Exception("[FactRelationMention] Error in format: " + sline);
    }

    String filler = fields[12];
    boolean isValue = filler.startsWith("\"") && filler.endsWith("\"");

    String relnType = fields[1];
    String docId = fields[0];

    String role1 = fields[2];

    // for agent1
    String srcEntityId;
    String queryType;
    String querySubType;
    int arg1_start;
    int arg1_end;
    double arg1_confidence;
    double arg1_linkConfidence;
    String arg1_brandyConfidence;

    // for filler
    String dstEntityId;
    String answerType;
    String answerSubType;
    int arg2_start;
    int arg2_end;
    double arg2_confidence;
    double arg2_linkConfidence;
    String arg2_brandyConfidence;

    if (role1.equals("AGENT1")) {
      srcEntityId = fields[3];
      queryType = fields[4];
      querySubType = fields[5];
      arg1_start = Integer.parseInt(fields[6]);
      if(adjust_offsets_for_adept)
        arg1_end = Integer.parseInt(fields[7])+1;
      else
        arg1_end = Integer.parseInt(fields[7]);

      arg1_confidence = Double.parseDouble(fields[8]);
      arg1_linkConfidence = Double.parseDouble(fields[9]);
      arg1_brandyConfidence = fields[10].trim();

//							System.out.println("fields[12]: " + fields[12]);
      dstEntityId = fields[12];
      answerType = fields[13];
      answerSubType = fields[14];
      arg2_start = Integer.parseInt(fields[15]);
      if(adjust_offsets_for_adept)
        arg2_end = Integer.parseInt(fields[16])+1;
      else
        arg2_end = Integer.parseInt(fields[16]);

      if (fields[17].equals("NULL")) {
        arg2_confidence = 0;
      } else {
        arg2_confidence = Double.parseDouble(fields[17]);
      }
//							System.out.println("fields[19]: " + fields[18]);
      if (fields[18].equals("NULL")) {
        arg2_linkConfidence = 0;
      } else {
        arg2_linkConfidence = Double.parseDouble(fields[18]);
      }
      arg2_brandyConfidence = fields[19].trim();
    } else {
      dstEntityId = fields[3];
      answerType = fields[4];
      answerSubType = fields[5];
      arg2_start = Integer.parseInt(fields[6]);
      if(adjust_offsets_for_adept)
        arg2_end = Integer.parseInt(fields[7])+1;
      else
        arg2_end = Integer.parseInt(fields[7]);

      arg2_confidence = Double.parseDouble(fields[8]);
      arg2_linkConfidence = Double.parseDouble(fields[9]);
      arg2_brandyConfidence = fields[10].trim();

      srcEntityId = fields[12];
      queryType = fields[13];
      querySubType = fields[14];
      arg1_start = Integer.parseInt(fields[15]);
      if(adjust_offsets_for_adept)
        arg1_end = Integer.parseInt(fields[16])+1;
      else
        arg1_end = Integer.parseInt(fields[16]);

      if (fields[17].equals("NULL")) {
        arg1_confidence = 0;
      } else {
        arg1_confidence = Double.parseDouble(fields[17]);
      }
      if (fields[18].equals("NULL")) {
        arg1_linkConfidence = 0;
      } else {
        arg1_linkConfidence = Double.parseDouble(fields[18]);
      }
      arg1_brandyConfidence = fields[19].trim();
    }

    // normalize IDs for submission
    srcEntityId = Normalization.convertIDtoOnlyHaveAsciiCharacter(srcEntityId);
    if (!isValue) {
      dstEntityId = Normalization.convertIDtoOnlyHaveAsciiCharacter(dstEntityId);
    }
    //

    int sent_start = Integer.parseInt(fields[20]);
    int sent_end;
    if(adjust_offsets_for_adept)
      sent_end = Integer.parseInt(fields[21])+1;
    else
      sent_end = Integer.parseInt(fields[21]);

    double confidence = Double.parseDouble(fields[22]);

    String strTextArg1 = fields[23].trim();
    String strTextArg2 = fields[24].trim();

    return new FactRelationMention(filler, isValue, relnType, docId, role1,
    // for agent1
    srcEntityId, queryType, querySubType, arg1_start, arg1_end, arg1_confidence, arg1_linkConfidence, arg1_brandyConfidence,
    // for filler
    dstEntityId, answerType, answerSubType, arg2_start, arg2_end, arg2_confidence, arg2_linkConfidence, arg2_brandyConfidence,
    // for relation mention
    sent_start, sent_end, confidence, strTextArg1, strTextArg2);
  }

  public Optional<TextRelationMention> toKBRelationMention() {

    if (answerType.trim().isEmpty() || queryType.trim().isEmpty()) {
      System.exit(0);
    }

    // these patterns has incorrect argument order
    if (relnType.equals("per_parents_24") ||
        relnType.equals("per_parents_ds_29") ||
        relnType.equals("per_parents_ds_65") ||
        relnType.equals("per_parents_ds_80") ||
        relnType.equals("per_parents_ds_81") ||
        relnType.equals("per_parents_ds_89") ||
        relnType.equals("per_parents_ds_101") ||
        relnType.equals("per_parents_ds_106") ||
        relnType.equals("per_parents_ds_153") ||
        relnType.equals("per_parents_ds_216") ||
        relnType.equals("per_parents_ds_219") ||
        relnType.equals("per_alternate_names_11")) {
      System.out.println("SKIP:\tlow precision patterns\t" + relnType);
      return Optional.absent();
    }

    if ((relnType.equals("org_parents_1") || relnType.equals("org_parents_23") || relnType
        .equals("org_parents_274") || relnType.equals("org_parents_ds_27") ||
             relnType.equals("org_parents_ds_120") || relnType
        .equals("org_parents_ds_156")
             || relnType.equals("org_parents_ds_164") || relnType
        .equals("org_parents_ds_225") ||
             relnType.equals("org_parents_ds_274") || relnType
        .equals("org_parents_ds_276")
             || relnType.equals("org_parents_ds_277") || relnType
        .equals("org_parents_ds_278") ||
             relnType.equals("org_parents_ds_332") || relnType
        .equals("org_parents_ds_333")
             || relnType.equals("org_parents_ds_339") || relnType
        .equals("org_parents_ds_385") ||
             relnType.equals("org_parents_ds_386") || relnType
        .equals("org_parents_ds_387")
             || relnType.equals("org_parents_ds_388") || relnType
        .equals("org_parents_ds_390") ||
             relnType.equals("org_parents_ds_391") || relnType
        .equals("org_parents_ds_411")
             || relnType.equals("org_parents_ds_419") || relnType
        .equals("org_parents_ds_449") ||
             relnType.equals("org_parents_ds_455") || relnType
        .equals("org_parents_ds_546")
             || relnType.equals("org_parents_ds_547") || relnType
        .equals("org_parents_ds_548") ||
             relnType.equals("org_parents_ds_549") || relnType
        .equals("org_parents_ds_550")
             || relnType.equals("org_parents_ds_551") ||
             relnType.equals("org_parents"))
        && answerType.equals("GPE")) {
      System.out.println("SKIP:\torg_parents patterns only work for ORG\t" + relnType);
      return Optional.absent();
    }


    // PER moved to ORG
    if ((relnType.equals("per_employee_or_member_of_ds_618") ||
             relnType.equals("per_employee_or_member_of_ds_35") ||
             relnType.equals("per_employee_or_member_of_ds_86") ||
             relnType.equals("per_employee_or_member_of_ds_355") ||
             relnType.equals("per_employee_or_member_of_ds_222") ||
             relnType.equals("per_employee_or_member_of_ds_553") ||
             relnType.equals("per_employee_or_member_of_ds_278"))
        && answerType.equals("GPE")) {
      System.out.println("SKIP:\t\"moved_to\" pattern only works for ORG\t" + relnType);
      return Optional.absent();
    }
    //

    if (relnType.equals("per_alternate_names_11")) {
      System.out.println("SKIP:\tbad alt_name pattern\t" + relnType);
      return Optional.absent();
    }

    Optional<Slot> slot =
        SlotFactory.fromFactTypeString(relnType, querySubType, answerSubType,
            Optional.of(strTextArg1), Optional.of(strTextArg2));
    if (!slot.isPresent()) {
      System.out.println("SKIP:\tno valid slot aligned\t" + relnType);
      return Optional.absent();
    }

    if (slot.get().toString().equals("per:title") && !isValue) {
      System.out.println("SKIP:\tslot is per:title but is not a value slot\t" + slot.get().toString());
    }

    //						if(slot.get().toString().equals("per:title"))
//							System.out.println("Gotcha0");

    Optional<String> normalizedDate = getNormalizedDate();


    TextMention queryMention =
        new TextMention(docId, arg1_start, arg1_end, queryType, querySubType);
    queryMention.setEntityId(srcEntityId);
    queryMention.setConfidence(arg1_confidence);
    queryMention.setLinkConfidence(arg1_linkConfidence);
    queryMention.setBrandyConfidence(arg1_brandyConfidence);
    queryMention.setText(strTextArg1);

    TextMention answerMention =
        new TextMention(docId, arg2_start, arg2_end, answerType, answerSubType);
    answerMention.setEntityId(dstEntityId);
    answerMention.setConfidence(arg2_confidence);
    answerMention.setLinkConfidence(arg2_linkConfidence);
    answerMention.setBrandyConfidence(arg2_brandyConfidence);


    // inject normalized date
    if (normalizedDate.isPresent()) {
      answerMention
          .setText(normalizedDate.get().substring(1, normalizedDate.get().length() - 1));
    } else {
      answerMention.setText(strTextArg2);
    }

    // filters non-name alternate names
    if (slot.get().toString().equals("org:alternate_names") || slot.get().toString()
        .equals("per:alternate_names"))
//							if(!queryMention.brandyConfidence.get().equals("AnyName") ||
//									!answerMention.brandyConfidence.get().equals("AnyName")) {
    {
      if (!answerMention.getBrandyConfidence().get().equals("AnyName")) {
        System.out
            .println("SKIP:\tquery/filler for alternate_names is not AnyName\t" + answerMention.getBrandyConfidence().get());
        return Optional.absent();
      }
    }

    if ((SlotFactory.isValueSlot(slot.get()) && !isValue) ||
        (!SlotFactory.isValueSlot(slot.get()) && isValue)) {
      System.out.println("SKIP:\tbecause of value & non-value confusion\t" + "slot.get()=" + slot.get() + ", isValue=" + isValue);
      return Optional.absent();
    }


    if (slot.get().toString().equals("per:title") &&
        (answerMention.getText().get().equalsIgnoreCase("head") ||
             answerMention.getText().get().equalsIgnoreCase("leader") ||
             answerMention.getText().get().equalsIgnoreCase("star") // ||
//											value.text.get().equalsIgnoreCase("official")
        )) {
      System.out.println("SKIP:\tvague title:\t" + answerMention.getText().get());
      return Optional.absent();
    }

    TextRelationMention
        rm = new TextRelationMention(queryMention, answerMention, slot.get());

//  rm.setConfidence(confidence);
    double relationConfidence = SimpleConfidences
        .getConfidenceByProduct(relnType, queryMention.getLinkConfidence().get(), answerMention.getLinkConfidence().get());
    rm.setConfidence(relationConfidence);

    rm.setSpan(new TextSpan(sent_start, sent_end));
    rm.setIsValueSlot(isValue);
    rm.setSourcePattern(relnType);

    return Optional.of(rm);
  }

  public Optional<String> getNormalizedDate() {
    Optional<Slot> slot =
        SlotFactory.fromFactTypeString(relnType, querySubType, answerSubType,
            Optional.of(strTextArg1), Optional.of(strTextArg2));
    if(slot.isPresent())
      return Optional.absent();

    Optional<String> normalizedDate = Optional.absent();
    if (isValue && SlotFactory.isDateSlot(slot.get())) {
      Optional<String> strNormalizedDate = DateKBPnormalizer.normalizeDate(filler);
      if (!strNormalizedDate.isPresent()) {
        System.out.println("DateKBPnormalizer:\tREMOVE\t" + filler);
        return Optional.absent();
      } else {
        System.out.println(
            "DateKBPnormalizer:\tNORMALIZE\t" + filler + "\t" + strNormalizedDate.get());
        filler = strNormalizedDate.get();

        normalizedDate = Optional.of(strNormalizedDate.get());
      }
    }

    return normalizedDate;
  }
}
