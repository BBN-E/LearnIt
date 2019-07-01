package com.bbn.akbc.common;

import com.google.common.collect.ImmutableSet;

public class FactConfidence {
    /*
    static ImmutableSet<String> lowestTrainedModelConfidenceFacts = (new ImmutableSet.Builder<String>())
        .add("per_employee_or_member_of")
        .add("org_shareholders")
        .add("per_parents")
        .add("org_founded_by")
        .add("per_spouse")
        .add("per_place_of_residence")
        .add("per_other_family")
        .add("org_parents")
        .add("per_schools_attended")
        .add("per_title")
        .add("per_siblings")
        .add("per_place_of_birth")
        .add("org_members")
        .add("org_place_of_headquarters")
        .add("per_origin")
        .add("per_alternate_names")
        .add("per_place_of_death")
        .add("org_alternate_names")
        .build();
*/
  static ImmutableSet<String> lowConfidenceFacts = (new ImmutableSet.Builder<String>())
      .add("PerTitle1") // take coreference for titles
      .add("org_subsidiary_serif2")
      .add("org_subsidiary_serif3")
      .add("Headquarters_10") // take from ACE org:located_in
      .add("Headquarters_8")
      .add("per_alternate_names")
      .add("org_place_of_headquarters_ds_9")
      .add("org_place_of_headquarters_ds_12")
      .add("org_place_of_headquarters_ds_26")
      .add("org_place_of_headquarters_ds_40")
      .add("org_place_of_headquarters_ds_24")
      .add("org_place_of_headquarters_ds_163")
      .add("org_place_of_headquarters_ds_339")
      .add("org_place_of_headquarters_ds_251")
      .add("org_place_of_headquarters_7")
      .add("org_place_of_headquarters_16")
      .add("org_place_of_headquarters_19")
      .add("org_place_of_headquarters_15")
      .add("org_place_of_headquarters_56")
      .add("org_place_of_headquarters_27")
      .add("org_place_of_headquarters_53")
      .add("Headquarters_7")

      .add("per_employee_or_member_of")
      .add("per_employee_or_member_of_ds_21")
      .add("per_employee_or_member_of_ds_454")
      .add("per_employee_or_member_of_ds_42")
      .add("per_employee_or_member_of_ds_63")
      .add("per_employee_or_member_of_ds_142")
      .add("per_employee_or_member_of_ds_122")
      .add("per_employee_or_member_of_ds_49")
      .add("per_employee_or_member_of_ds_618")
      .add("per_employee_or_member_of_ds_515")
      .add("per_employee_or_member_of_ds_278")
      .add("per_employee_or_member_of_ds_30")
      .add("per_employee_or_member_of_ds_317")
      .add("per_employee_or_member_of_ds_86")
      .add("per_employee_or_member_of_ds_610")
      .add("per_employee_or_member_of_ds_115")
      .add("per_employee_or_member_of_ds_306")
      .add("per_employee_or_member_of_ds_142")
      .add("per_employee_or_member_of_138")
      .add("per_employee_or_member_of_54")
      .add("per_employee_or_member_of_ds_168")

      .add("resident_of1")
      .add("resident_of6")
      .add("per_place_of_residence_ds_13") // PER of GPE
      .add("per_place_of_residence_49")
      .add("per_place_of_residence_23")
      .add("per_place_of_residence_70")
      .add("per_place_of_residence_10001")
      .add("per_place_of_residence_ds_38")
      .add("per_origin_32")
      .add("org_parents_1")

      .add("DeathDate1")

      .add("per_place_of_residence_19")
      .add("per_place_of_residence_ds_22")
      .add("per_place_of_residence_ds_27")
      .add("per_place_of_residence_49")
      .add("per_place_of_residence_ds_252")
      .add("per_place_of_residence_ds_60")
      .add("per_place_of_residence_ds_228")
      .add("per_place_of_residence_ds_68")
      .add("per_place_of_residence_ds_119")
      .add("per_place_of_residence_ds_137")
      .add("per_place_of_residence_ds_680")
      .add("per_place_of_residence_ds_289")
      .add("per_place_of_residence_ds_270")
      .add("per_place_of_residence_ds_437")
      .add("per_place_of_residence_ds_489")
      .add("per_place_of_residence_ds_306")
      .add("per_place_of_residence_ds_91")
      .add("per_place_of_residence_ds_214")
      .add("per_place_of_residence_ds_217")
      .add("per_place_of_residence_ds_340")
      .add("per_place_of_residence_ds_229")
      .add("per_place_of_residence_ds_318")
      .add("per_place_of_residence_ds_105")
      .add("per_place_of_residence_ds_66")
      .add("per_place_of_residence_ds_111")
      .add("per_place_of_residence_40")
      .add("per_place_of_residence_70")

      .add("per_place_of_birth_12")

      .add("per_origin_ds_17")
      .add("per_origin_ds_828")
      .add("per_origin_2")
      .add("per_origin_15")
      .add("per_origin_158")
      .add("per_origin_44")
      .add("org_parents_ds_163")
      .add("org_parents_ds_391")

      .add("org_website_7")

      .add("per_alternate_names_7")

          // reviewed by Nick
      .add("per_origin_19")
      .add("org_members_91")
      .add("org_parents_14")
      .add("org_parents_216")
      .add("org_parents_23")
      .add("org_parents_274")
      .add("org_parents_ds_152")
      .add("org_parents_ds_20")
      .add("org_parents_ds_278")
      .add("org_parents_ds_279")
      .add("org_parents_ds_390")
      .add("org_parents_ds_387")
      .add("org_parents_ds_547")
      .add("org_parents_ds_73")
      .add("org_place_of_headquarters_ds_27")
      .add("org_place_of_headquarters_ds_547")
      .add("org_shareholders_156")
      .add("org_shareholders_24")
      .add("org_shareholders_8")
      .add("per_alternate_names_8")
      .add("per_employee_or_member_of_141")
      .add("per_employee_or_member_of_145")
      .add("per_employee_or_member_of_150")
      .add("per_employee_or_member_of_ds_159")
      .add("per_employee_or_member_of_ds_174")
      .add("per_employee_or_member_of_ds_258")
      .add("per_employee_or_member_of_ds_277")
      .add("per_employee_or_member_of_ds_295")
      .add("per_employee_or_member_of_ds_327")
      .add("per_employee_or_member_of_ds_354")
      .add("per_employee_or_member_of_ds_69")
      .add("per_origin_ds_155")
      .add("per_origin_ds_463")
      .add("per_place_of_residence_142")
      .add("per_place_of_residence_44")
      .add("per_place_of_residence_45")
      .add("per_place_of_residence_46")
      .add("per_spouse_ds_94")

        .add("per_employee_or_member_of_100022")
        .add("per_place_of_residence_100001")
        .add("per_place_of_residence_100000")
        .add("per_employee_or_member_of_100021")
          //

        .build();

  static ImmutableSet<String> moderateConfidenceFacts = (new ImmutableSet.Builder<String>())
      .add("org_place_of_headquarters") // learnit pattern
      .add("per_date_of_death")
      .add("per_siblings")

      .add("per_place_of_residence_21")
      .add("per_place_of_residence_ds_481")
      .add("per_place_of_residence_ds_292")
      .add("per_place_of_residence_ds_784")
      .add("per_place_of_residence_10000")

      .add("per_employee_or_member_of_serif1")
      .add("per_employee_or_member_of_serif2")
      .add("per_employee_or_member_of_59")
      .add("per_employee_or_member_of_ds_47")
      .add("per_employee_or_member_of_ds_316")
      .add("per_employee_or_member_of_ds_306")

      .add("org_founded_by_serif")
      .add("per_schools_attended_serif")
      .add("per_schools_attended")
      .add("per_place_of_death_serif")

      .add("per_place_of_birth")

      .add("Employer")

      .add("org_alternate_names_24")
      .add("Spouse")

      .add("per_alternate_names_10")

      .add("org_subsidiary_serif1")
      .add("org_subsidiary_serif3")
      .add("per_spouse")

      .add("per_parents")

      .add("per_date_of_death")

      .add("per_alternate_names")
      .add("per_alternate_names_1")
      .add("per_alternate_names_2")
      .add("per_alternate_names_5")
      .add("per_alternate_names_9")

      .add("Education")

      .add("org_parents")
      .add("per_charges_serif")
      .add("org_founded_by")
      .add("per_spouse_serif")

      .add("per_spouse_ds_227")
      .add("per_spouse_ds_14")
      .add("per_spouse_ds_13")
      .add("per_charges_serif")
      .add("per_employee_or_member_of_128")
      .add("per_employee_or_member_of_138")
      .add("per_place_of_birth_serif")
      .add("per_place_of_death")

      .add("per_origin_ds_44")
      .add("org_shareholders")
      .add("resident_of2")
      .add("per_place_of_residence_80")
      .add("per_date_of_death_ds_2")
      .add("per_date_of_death_serif")

      .add("org_date_founded")
      .add("org_members_serif")

          // reviewed by Nick
      .add("org_founded_by_ds_101")
      .add("org_founded_by_ds_162")
      .add("org_parents_24")
      .add("org_parents_ds_27")
      .add("org_parents_ds_41")
      .add("org_shareholders_serif")
      .add("per_employee_or_member_of_146")
      .add("per_employee_or_member_of_72")
      .add("per_employee_or_member_of_73")
      .add("per_employee_or_member_of_ds_198")
      .add("per_employee_or_member_of_ds_309")
      .add("per_origin_ds_167")
      .add("per_origin_ds_536")
      .add("per_origin_ds_828")
      .add("per_spouse_16")

      .add("per_employee_or_member_of_100001")
      .add("org_place_of_headquarters_100000")
      .add("per_employee_or_member_of_100023")
      .add("per_employee_or_member_of_ere_200005")
      .add("per_employee_or_member_of_ds_200025")
          //

      .build();


  public static double getSourcePatternConf(String sourcePattern) {
      /*
    // TODO: better to rename and make trained_model extracted fact seperated
    if (lowestTrainedModelConfidenceFacts.contains(sourcePattern)) {
          return 0.05;
    }
    */
    // else if (lowConfidenceFacts.contains(sourcePattern)) {
    if(sourcePattern.contains("_trained")) {
        return 0.05;
    } else if (lowConfidenceFacts.contains(sourcePattern)) {
      return 0.1; // low confidence
    } else if (moderateConfidenceFacts.contains(sourcePattern)) {
      return 0.5; // moderate confidence
    } else {
      return 0.9; // everything else
    }
  }

  public static double getSourcePatternConfAsProbabilities(String sourcePattern) {
      /*
    // TODO: better to rename and make trained_model extracted fact seperated
    if (lowestTrainedModelConfidenceFacts.contains(sourcePattern)) {
          return 0.05;
    }
    */
    // else if (lowConfidenceFacts.contains(sourcePattern)) {
    if(sourcePattern.contains("_trained")) {
      return 0.3;
    } else if (lowConfidenceFacts.contains(sourcePattern)) {
      return 0.5; // low confidence
    } else if (moderateConfidenceFacts.contains(sourcePattern)) {
      return 0.7; // moderate confidence
    } else {
      return 0.9; // everything else
    }
  }

  public static boolean hasHighConfidence(String sourcePattern) {
    if (getSourcePatternConf(sourcePattern) >= 0.9) {
      return true;
    } else {
      return false;
    }
  }

}
