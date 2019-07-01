package com.bbn.akbc.neolearnit.relations.utility;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by bmin on 5/4/15.
 */
public class SlotConverter {
  public static boolean isValid(String dsLabel) {
    if(dsLabels.contains(dsLabel))
      return true;
    else
      return false;
  }

  public static String getArg1Type(String dsLabel) {
    return dsLabel.substring(0, dsLabel.indexOf("_")).toUpperCase();
  }

  public static Set<String> getArg2Type(String dsLabel) {
    Set<String> fillerTypes = new HashSet<String>();
    for(String type : slot2fillerTypes.get(dsLabel))
      fillerTypes.add(type.toUpperCase());
    return fillerTypes;
  }

  public static int getNumPosInstForSlot(String slot) {
    return slot2numPosInst.get(slot);
  }

  public static Set<String> getValidSlots(String arg1entType, String arg2entType) {
    Set<String> validSlots = new HashSet<String>();
    for(String slot : dsLabels) {
      if(getArg1Type(slot).equals(arg1entType) && getArg2Type(slot).contains(arg2entType))
        validSlots.add(slot);
    }

    return validSlots;
  }

  /*
  static ImmutableMap<String, String> dsLabels = (new ImmutableMap.Builder<String, String>())
      .put("org_founded_by", "per:organizations_founded-1")
      .put("org_parents", "org:parents")
//    .put("org_place_of_headquarters", "")
      .put("per_employee_or_member_of", "")
      .put("per_origin", "")
      .put("per_parents", "")
//    .put("per_place_of_birth", "")
//    .put("per_place_of_death", "")
//    .put("per_place_of_residence", "")
      .put("per_schools_attended", "")
      .put("per_siblings", "")
      .put("per_spouse", "")
//    .put("per_title", "")
      .build();
*/

  static ImmutableSet<String> dsLabels =
      (new ImmutableSet.Builder<String>())
          .add("org_founded_by")
          .add("org_parents")
          .add("per_employee_or_member_of")
          .add("per_origin")
          .add("per_parents")
          .add("per_schools_attended")
          .add("per_siblings")
          .add("per_spouse")
          .build();

  public static ImmutableSet<String> allEntTypes =
      (new ImmutableSet.Builder<String>())
          .add("FAC")
          .add("GPE")
          .add("LOC")
          .add("ORG")
          .add("PER")
          .add("VEH")
          .add("WEA")
          .build();

  static ImmutableMultimap<String, String> slot2fillerTypes =
      (new ImmutableMultimap.Builder<String, String>())
          .put("org_founded_by", "per")
          .put("org_founded_by", "org")
          .put("org_parents", "org")
          .put("org_parents", "gpe")
          .put("per_employee_or_member_of", "org")
          .put("per_employee_or_member_of", "gpe")
          .put("per_origin", "gpe")
          .put("per_parents", "per")
          .put("per_schools_attended", "org")
          .put("per_siblings", "per")
          .put("per_spouse", "per")
          .build();

  static ImmutableMap<String, Integer> slot2numPosInst =
      (new ImmutableMap.Builder<String, Integer>())
          .put("org_founded_by", 65899)
          .put("org_parents", 60593)
          .put("per_employee_or_member_of", 1377226)
          .put("per_origin", 4285721)
          .put("per_parents", 48999)
          .put("per_schools_attended", 60905)
          .put("per_siblings", 14861)
          .put("per_spouse", 132389)
          .build();
}
