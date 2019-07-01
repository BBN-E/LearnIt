package com.bbn.akbc.common;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

import java.util.HashSet;
import java.util.Set;

public class SlotConverter {

  static ImmutableMap<String, String> slotMap = (new ImmutableMap.Builder<String, String>())
      .put("PER_Title", "per_title")
      .put("PER_Member_Of", "per_employee_or_member_of")
      .put("ORG_Headquarters", "org_place_of_headquarters")
      .put("PER_Schools_Attended", "per_schools_attended")
      .put("PER_Residence", "per_place_of_residence")

      .put("PER_Date_of_Birth", "per_date_of_birth")
      .put("PER_Origin", "per_origin")
      .put("PER_Place_of_Birth", "per_place_of_birth")
      .put("PER_Date_of_Death", "per_date_of_death")
      .put("ORG_Subsidiaries", "org_parents-1") // inverse
      .put("PER_Place_of_Death", "per_place_of_death")
      .put("PER_Employee_Of", "per_employee_or_member_of")
      .put("ORG_Founded", "org_date_founded")
      .put("PER_Parents", "per_parents")
      .put("PER_Children", "per_parents-1")
      .put("PER_Spouse", "per_spouse")
      .put("PER_Siblings", "per_siblings")
      .put("ORG_Founded_by", "org_founded_by")
      .put("ORG_Dissolved", "org_date_dissolved")
      .put("ORG_Alternate_names", "org_alternate_names")
      .build();

  public static Set<String> getAllSlots() {
    return new HashSet<String>(slotMap.values());
  }

  // return have "per_parents-1" as return, which need special handling
  public static Optional<String> getNewSlot(String oldSlot) {
    if (!slotMap.containsKey(oldSlot)) {
      return Optional.absent();
    } else {
      return Optional.of(slotMap.get(oldSlot));
    }
  }
}
