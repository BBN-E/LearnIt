package com.bbn.akbc.kb.common;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SlotFactory {

  public static Slot fromStringSlotName(String slotName) {
    String[] slotFields = slotName.split(":", 2);
    if (slotFields.length != 2) {
      throw new RuntimeException("Invalid slot " + slotName);
    }

    boolean isSingleValueSlot = true;
    String slotType = getSlotType(slotName);
    if (slotType.equals("single")) {
      isSingleValueSlot = true;
    } else if (slotType.equals("list")) {
      isSingleValueSlot = false;
    } else if (slotType.equals("additional")) {
      isSingleValueSlot = true; // TODO: fix later
    } else {
      throw new RuntimeException("Invalid slot " + slotName);
    }

    return new Slot(slotFields[0], slotFields[1], isSingleValueSlot);
  }

  /*
   * not in TAC schema
   */
  public static boolean isVirtual(Slot slot) {
    String slotType = SlotFactory.getSlotType(slot.toString());
    if (slotType.equals("single") || slotType.equals("list")) {
      return false;
    } else {
      return true;
    }
  }

  public static Optional<Slot> fromFactTypeString(String factType,
      String querySubType, String answerSubtype,
      Optional<String> strTextArg1, Optional<String> strTextArg2) {
    Optional<String> slotName =
        getKBPSlotType(factType, querySubType, answerSubtype, strTextArg1, strTextArg2);
    if (slotName.isPresent()) {
      return Optional.of(SlotFactory.fromStringSlotName(slotName.get()));
    } else {
      return Optional.absent();
    }
  }

  public static Optional<Slot> fromFactTypeString(String factType,
      String querySubType, String answerSubtype) {
    Optional<String> slotName = getKBPSlotType(factType, querySubType, answerSubtype);
    if (slotName.isPresent()) {
      return Optional.of(SlotFactory.fromStringSlotName(slotName.get()));
    } else {
      return Optional.absent();
    }
  }

  public static Optional<Slot> getInverseSlot(Slot slot, String answerType) {
    Optional<String> inverseSlot = SlotFactory.getReverseRelationType(slot.toString(), answerType);
    if (!inverseSlot.isPresent()) {
      return Optional.absent();
    } else {
      return Optional.of(SlotFactory.fromStringSlotName(inverseSlot.get()));
    }
  }

  // delete for not exposing data structure
  public static ImmutableMap<String, String> getFactType2KBPSlots() {
    return factType2KBPSlots;
  }
  //

  /*********************** for TAC CS Chinese 2017 eval ******************************/
  // TODO: further check if this is the headword?
  // TODO: further check if arg1 and arg2 are a good pair, e.g., (son, parent)?
  static boolean mostLikelyParent(String answerSubtype, String strTextArg1, String strTextArg2) {
    if(strTextArg1.contains("爸") || strTextArg2.contains("爸") ||
        strTextArg1.contains("父") || strTextArg2.contains("父") ||
        strTextArg1.contains("妈") || strTextArg2.contains("妈") ||
        strTextArg1.contains("母") || strTextArg2.contains("母") ||
        strTextArg1.contains("儿") || strTextArg2.contains("儿") ||
        strTextArg1.contains("子") || strTextArg2.contains("子") ||
        strTextArg1.contains("女") || strTextArg2.contains("女") ||
        strTextArg1.contains("儿子") || strTextArg2.contains("儿子") ||
        strTextArg1.contains("女儿") || strTextArg2.contains("女儿") ||
        strTextArg1.contains("孩子") || strTextArg2.contains("孩子") ||
        strTextArg1.contains("子女") || strTextArg2.contains("子女") ||
        strTextArg1.contains("儿子") || strTextArg2.contains("儿子") ||
        strTextArg1.contains("儿子") || strTextArg2.contains("儿子"))
      return true;
    else
      return false;
  }

  static boolean mostLikelySiblings(String answerSubtype, String strTextArg1, String strTextArg2) {
    if(strTextArg1.contains("哥") || strTextArg2.contains("哥") ||
        strTextArg1.contains("妹") || strTextArg2.contains("妹") ||
        strTextArg1.contains("姐") || strTextArg2.contains("姐") ||
        strTextArg1.contains("弟") || strTextArg2.contains("弟") ||
        strTextArg1.contains("兄") || strTextArg2.contains("兄"))
      return true;
    else
      return false;
  }

  static boolean mostLikelySpouse(String answerSubtype, String strTextArg1, String strTextArg2) {
    if(strTextArg1.contains("妻") || strTextArg2.contains("妻") ||
        strTextArg1.contains("太太") || strTextArg2.contains("太太") ||
        strTextArg1.contains("爱人") || strTextArg2.contains("爱人") ||
        strTextArg1.contains("丈夫") || strTextArg2.contains("丈夫") ||
        strTextArg1.contains("配偶") || strTextArg2.contains("配偶") ||
        strTextArg1.contains("媳妇") || strTextArg2.contains("媳妇") ||
        strTextArg1.contains("新郎") || strTextArg2.contains("新郎") ||
        strTextArg1.contains("新娘") || strTextArg2.contains("新娘") ||
        strTextArg1.contains("老婆") || strTextArg2.contains("老婆"))
      return true;
    else
      return false;
  }

  static boolean mostLikelyReligion(String answerSubtype, String strTextArg1, String strTextArg2) {
    if(strTextArg2.contains("基督") ||
        strTextArg2.contains("佛") ||
        strTextArg2.contains("道教") ||
        strTextArg2.contains("伊斯兰") ||

        strTextArg2.contains("道教") ||
        strTextArg2.contains("基督教") ||
        strTextArg2.contains("伊斯兰教") ||

        strTextArg2.contains("神道教") ||
        strTextArg2.contains("佛教") ||
        strTextArg2.contains("犹太教") ||
        strTextArg2.contains("印度教") ||
        strTextArg2.contains("萨满教") ||
        strTextArg2.contains("儒教") ||
        strTextArg2.contains("天主教") ||
        strTextArg2.contains("摩门教") ||
        strTextArg2.contains("法轮大法") ||
        strTextArg2.contains("法轮功") ||
        strTextArg2.contains("摩门"))
    return true;
    else
      return false;
  }

  // TODO: add a list nationalities
  static boolean mostLikelyNationality(String answerSubtype) {
    if(answerSubtype.equals("Nation"))
      return true;
    else
      return false;
  }

  // TODO: add keywords indicating schools
  static boolean mostLikelySchool(String answerSubType) {
    if(answerSubType.trim().equals("Educational"))
      return true;
    else
      return false;
  }

  static ImmutableMap<String, String> cs2017SerifRelationType2KBPSlots =
      (new ImmutableMap.Builder<String, String>())
          .put("per_employee_or_member_of", "per:employee_or_member_of")
          .put("org_parents", "org:parents")
          .put("org_founded_by", "org:founded_by")
          .put("org_shareholders", "org:shareholders")
          .put("per_parents", "per:parents")
          .put("per_schools_attended", "per:schools_attended")
          .put("per_siblings", "per:siblings")
          .put("per_spouse", "per:spouse")

          .put("TAC.org_founded_by", "org:founded_by")
          .put("TAC.org_parents", "org:parents")
          .put("TAC.per_employee_or_member_of", "per:employee_or_member_of")
          .put("TAC.per_parents", "per:parents")
          .put("TAC.per_schools_attended", "per:schools_attended")
          .put("TAC.per_spouse", "per:spouse")
          .put("TAC.per_title", "per:title")
          .put("per_title", "per:title")

          .put("per_date_of_birth", "per:date_of_birth")
          .put("org_date_dissolved", "org:date_dissolved")
          .put("org_date_founded", "org:date_founded")
          .put("per_date_of_death", "per:date_of_death")
          .put("org_website", "org:website")
      .build();

  public static ImmutableMultimap<String, String> kbpSlot2arg2Types =
      (new ImmutableMultimap.Builder<String, String>())
          .put("per:employee_or_member_of", "ORG")
          .put("per:employee_or_member_of", "GPE")

          .put("per:parents", "PER")

          .put("org:subsidiarie", "ORG")
          .put("gpe:subsidiarie", "ORG")

          .put("org:parents", "ORG")
          .put("org:parents", "GPE")

          .put("org:founded_by", "PER")
          .put("org:founded_by", "ORG")
          .put("org:founded_by", "GPE")

          .put("org:shareholders", "GPE")
          .put("org:shareholders", "PER")
          .put("org:shareholders", "ORG")

          .put("per:siblings", "PER")
          .put("per:spouse", "PER")

          .put("per:schools_attended", "ORG")

          .build();


  // only works for relations whose arg2 is a value mention
  public static List<Slot> getKBPSlotsFromSerifXmlValueRelnTypeInCs2017(String serifXmlRelnTypeInCs2017,
      String queryType, String querySubType,
      String answerType, String answerSubtype,
      Optional<String> strTextArg1, Optional<String> strTextArg2) {

    List<Slot> returnSlots = new ArrayList<Slot>();
    // Optional<String> returnSlotName = Optional.absent();
    if (cs2017SerifRelationType2KBPSlots.containsKey(serifXmlRelnTypeInCs2017)) {
      String returnSlotName = cs2017SerifRelationType2KBPSlots.get(serifXmlRelnTypeInCs2017);
      Slot slot = SlotFactory
          .fromStringSlotName(returnSlotName);

      returnSlots.add(slot);
    }

    return returnSlots;
  }

      // only works for relations whose arg2 is an entity mention
  public static List<Slot> getKBPSlotsFromSerifXmlMentionRelnTypeInCs2017(String serifXmlRelnTypeInCs2017,
      String queryType, String querySubType,
      String answerType, String answerSubtype,
      Optional<String> strTextArg1, Optional<String> strTextArg2,
      List<String> titlesForTopMemberOrEmployee) {

    List<Slot> returnSlots = new ArrayList<Slot>();
    // Optional<String> returnSlotName = Optional.absent();
    if(cs2017SerifRelationType2KBPSlots.containsKey(serifXmlRelnTypeInCs2017)) {
      String returnSlotName = cs2017SerifRelationType2KBPSlots.get(serifXmlRelnTypeInCs2017);
      Slot slot = SlotFactory
          .fromStringSlotName(returnSlotName);

      returnSlots.add(slot);


      // answer type mismatch
      if(kbpSlot2arg2Types.containsKey(returnSlotName))
        if(!kbpSlot2arg2Types.get(returnSlotName).contains(answerType))
          return new ArrayList<Slot>();

      if(returnSlotName.equals("per:schools_attended") && !mostLikelySchool(answerSubtype))
        return new ArrayList<Slot>();

      // returnSlotName = Optional.of(cs2017SerifRelationType2KBPSlots.get(serifXmlRelnTypeInCs2017));
    }

    /*
    // check if arg2 type is compatible
    if(returnSlotName.isPresent())
      if(kbpSlot2arg2Types.containsKey(returnSlotName.get()))
        if(!kbpSlot2arg2Types.get(returnSlotName.get()).contains(answerType))
          returnSlotName = Optional.absent();
    */

    // per:origin
    // only nationality or ethic groups
    if (serifXmlRelnTypeInCs2017.equals("per_origin") ||
        serifXmlRelnTypeInCs2017.equals("TAC.per_origin") ||
        serifXmlRelnTypeInCs2017.equals("TAC.per_religion_or_origin")) {
      if ((answerSubtype.equals("Nation") || answerSubtype.equals("Special") || answerSubtype
          .equals("Continent"))
          && noDestcriptor(strTextArg2)) {
        // returnSlotName = Optional.of("per:origin");
        returnSlots.add(SlotFactory.fromStringSlotName("per:origin"));
      }
    }

    // per_place_of_birth
    // per_place_of_death
    // per_place_of_residence
    // org_place_of_headquarters
    // TAC.org_place_of_headquarters
    // TAC.per_place_of_death
    // TAC.per_place_of_residence
    if(serifXmlRelnTypeInCs2017.startsWith("per_place_of") || serifXmlRelnTypeInCs2017.startsWith("TAC.per_place_of") ||
        serifXmlRelnTypeInCs2017.startsWith("org_place_of") || serifXmlRelnTypeInCs2017.startsWith("TAC.org_place_of")) {
      String fillerSubType = "NA";
      if (strTextArg2.get().equals("北京")) { // special case that serif does incorrectly
        if(serifXmlRelnTypeInCs2017.endsWith("residence"))
          fillerSubType = "cities";
        else
          fillerSubType = "city";
      } else if (answerSubtype.equals("Nation") || answerSubtype.equals("Special")) {
        if(serifXmlRelnTypeInCs2017.endsWith("residence"))
          fillerSubType = "countries";
        else
          fillerSubType = "country";
      } else if (answerSubtype.equals("State-or-Province")) {
        if(serifXmlRelnTypeInCs2017.endsWith("residence"))
          fillerSubType = "statesorprovinces";
        else
          fillerSubType = "stateorprovince";
      } else if (answerSubtype.equals("Population-Center")) {
        if(serifXmlRelnTypeInCs2017.endsWith("residence"))
          fillerSubType = "cities";
        else
          fillerSubType = "city";
      } else if (mostLikelyCity(answerSubtype, strTextArg1, strTextArg2)) {
        if(serifXmlRelnTypeInCs2017.endsWith("residence"))
          fillerSubType = "cities";
        else
          fillerSubType = "city";
      } else if (mostLikelyStateOrProvince(answerSubtype, strTextArg1, strTextArg2)) {
        if(serifXmlRelnTypeInCs2017.endsWith("residence"))
          fillerSubType = "statesorprovinces";
        else
          fillerSubType = "stateorprovince";
      } else if (mostLikelyCountry(answerSubtype, strTextArg1, strTextArg2)) {
        if(serifXmlRelnTypeInCs2017.endsWith("residence"))
          fillerSubType = "countries";
        else
          fillerSubType = "country";
      }


      // make the return slot name
      if(!fillerSubType.equals("NA")) {
        String fillerNameLastWord =
            serifXmlRelnTypeInCs2017.substring(serifXmlRelnTypeInCs2017.lastIndexOf("_") + 1);
        if (serifXmlRelnTypeInCs2017.startsWith("per_place_of") || serifXmlRelnTypeInCs2017
            .startsWith("TAC.per_place_of")) {
          returnSlots.add(
              SlotFactory.fromStringSlotName("per:" + fillerSubType + "_of_" + fillerNameLastWord));
          // returnSlotName = Optional.of("per:" + fillerSubType + "_of_" + fillerNameLastWord);
        } else if (serifXmlRelnTypeInCs2017.startsWith("org_place_of") || serifXmlRelnTypeInCs2017
            .startsWith("TAC.org_place_of")) {
          returnSlots.add(
              SlotFactory.fromStringSlotName("org:" + fillerSubType + "_of_" + fillerNameLastWord));
          // returnSlotName = Optional.of("org:" + fillerSubType + "_of_" + fillerNameLastWord);
        }
      }
    }

    // TAC.per_family
    if(serifXmlRelnTypeInCs2017.equals("TAC.per_family") || serifXmlRelnTypeInCs2017.equals("per_family")) {
      if(strTextArg1.isPresent() && strTextArg2.isPresent()) {
        if (mostLikelyParent(answerSubtype, strTextArg1.get(), strTextArg2.get()))
          returnSlots.add(SlotFactory.fromStringSlotName("per:parents"));
          // returnSlotName = Optional.of("per:parents");
        else if (mostLikelySiblings(answerSubtype, strTextArg1.get(), strTextArg2.get()))
          returnSlots.add(SlotFactory.fromStringSlotName("per:siblings"));
          // returnSlotName = Optional.of("per:siblings");
        else if (mostLikelySpouse(answerSubtype, strTextArg1.get(), strTextArg2.get()))
          returnSlots.add(SlotFactory.fromStringSlotName("per:spouse"));
          // returnSlotName = Optional.of("per:spouse");
        else
          returnSlots.add(SlotFactory.fromStringSlotName("per:other_family"));
          // returnSlotName = Optional.of("per:other_family");
      }
    }
    //
    if(serifXmlRelnTypeInCs2017.equals("TAC.per_religion_or_origin")) {
      if(strTextArg1.isPresent() && strTextArg2.isPresent()) {
        if (mostLikelyReligion(answerSubtype, strTextArg1.get(), strTextArg2.get()))
          returnSlots.add(SlotFactory.fromStringSlotName("per:religion"));
          // returnSlotName = Optional.of("per:religion");
        else if(mostLikelyNationality(answerSubtype))
          returnSlots.add(SlotFactory.fromStringSlotName("per:origin"));
          // returnSlotName = Optional.of("per:origin");
        else
          System.out.println("Skip per:origin <" + strTextArg1 + ", " + strTextArg2 +"> because arg2 is not nationality nor origin");
      }
    }

    // add per:top_member_employee_of from per:member_employee_of
    if(!returnSlots.isEmpty() && strTextArg1.isPresent()) {
      if(returnSlots.get(0).toString().equals("per:employee_or_member_of") && answerType.equalsIgnoreCase("ORG")) {
        for(String title : titlesForTopMemberOrEmployee) {
          if(strTextArg1.get().contains(title)) {
            returnSlots.add(SlotFactory.fromStringSlotName("per:top_member_employee_of"));
            break;
          }
        }
      }
    }
    return returnSlots;

    /*
    if(returnSlotName.isPresent())
      return Optional.of(SlotFactory.fromStringSlotName(returnSlotName.get()));
    else
      return Optional.absent();
      */
  }

  /*********************** for TAC CS Chinese 2017 eval ******************************/


  static ImmutableMap<String, String> factType2KBPSlots =
      (new ImmutableMap.Builder<String, String>())
          .put("org_date_dissolved", "org:date_dissolved")
          .put("org_parents", "org:parents")
          .put("org_shareholders", "org:shareholders")
          .put("org_website", "org:website")
          .put("per_alternate_names", "per:alternate_names")
//			.put("per_origin", "per:origin")
          .put("org_alternate_names", "org:alternate_names")
          .put("org_founded_by_serif", "org:founded_by")
          .put("org_members_serif", "org:members")
          .put("org_members", "org:members")
          .put("org_top_members_employees", "org:top_members_employees")
          .put("org_shareholders_serif", "org:shareholders")
          .put("org_subsidiary_serif", "org:subsidiaries")
          .put("per_charges_serif", "per:charges")
          .put("per_employee_or_member_of_serif", "per:employee_or_member_of")
          .put("per_employee_or_member_of", "per:employee_or_member_of")

          /*
           * temporarily: need to do better
           */
          .put("per_top_member_employee_of", "per:employee_or_member_of")
          //

          .put("per_date_of_death_serif", "per:date_of_death")
          .put("per_date_of_death", "per:date_of_death")
          .put("per_schools_attended_serif", "per:schools_attended")
          .put("per_schools_attended", "per:schools_attended")
          .put("per_spouse_serif", "per:spouse")
          .put("BirthDate", "per:date_of_birth")
          .put("DeathDate", "per:date_of_death")
          .put("Education", "per:schools_attended")
          .put("Employer", "per:employee_or_member_of")
          .put("Children", "per:children")
          .put("Parents", "per:parents")
          .put("per_parents", "per:parents")
          .put("Siblings", "per:siblings")
          .put("per_siblings", "per:siblings")
          .put("Spouse", "per:spouse")
          .put("per_spouse", "per:spouse")
          .put("Other_family", "per:other_family")
          .put("per_other_family", "per:other_family")
          .put("Founder", "org:founded_by")
          .put("org_founded_by", "org:founded_by")
          .put("FoundingDate", "org:date_founded")
          .put("org_date_founded", "org:date_founded")
          .put("PerTitle", "per:title")
              // following are facts from assessments
//			.put("org_alternate_names", "org:alternate_names")
//			.put("org_date_dissolved", "org:date_dissolved")
//			.put("org_date_founded", "org:date_founded")
//			.put("org_founded_by", "org:founded_by")
//			.put("org_members", "org:members")
//			.put("org_parents", "org:parents")
//			.put("org_shareholders", "org:shareholders")
//			.put("org_website", "org:website")
//			.put("per_alternate_names", "per:alternate_names")
          .put("per_date_of_birth", "per:date_of_birth")
//			.put("per_date_of_death", "per:date_of_death")
//			.put("per_employee_or_member_of", "per:employee_or_member_of")
//			.put("per_origin", "per:origin")
//			.put("per_other_family", "per:other_family")
//			.put("per_parents", "per:parents")
//			.put("per_schools_attended", "per:schools_attended")
//			.put("per_siblings", "per:siblings")
//			.put("per_spouse", "per:spouse")
          .put("per_title", "per:title")
          .build();

  static ImmutableSet<String> notCities =
      (new ImmutableSet.Builder<String>()).add("wimbledon").add("newlands").add("kindle")
          .add("bajotierra").add("nordic")
          .add("middlesex").add("queens").add("dorset").add("staten island").build();

  static ImmutableSet<String> countries =
      (new ImmutableSet.Builder<String>()).add("costa rican").add("costa rica").add("rhodesia")
          .build();

  static ImmutableSet<String> stateorprovinces =
      (new ImmutableSet.Builder<String>()).add("pampanga").add("helmand").add("maguindanao")
          .add("guinea - bissau").add("tasmania").build();


  static boolean mostLikelyCity(String answerSubtype,
      Optional<String> strTextArg1, Optional<String> strTextArg2) {
    if (answerSubtype.equals("UNDET")) {
      if (strTextArg1.isPresent()) {
        String textArg1 = strTextArg1.get().toLowerCase().trim();
        if (notCities.contains(textArg1)
            || textArg1.contains("county") || textArg1.contains("district")) {
          return false;
        }
      }

      if (strTextArg2.isPresent()) {
        String textArg2 = strTextArg2.get().toLowerCase().trim();

        if(textArg2.endsWith("市"))
          return true;

        if (notCities.contains(textArg2)
            || textArg2.contains("county") || textArg2.contains("district")) {
          return false;
        }
      }

      return true;
    }

    return false;
  }

  static boolean mostLikelyStateOrProvince(String answerSubtype,
      Optional<String> strTextArg1, Optional<String> strTextArg2) {
    if(strTextArg2.isPresent()) {
      if(strTextArg2.get().trim().endsWith("州") || strTextArg2.get().trim().endsWith("省"))
        return true;
    }

    return false;
  }

  static boolean mostLikelyCountry(String answerSubtype,
      Optional<String> strTextArg1, Optional<String> strTextArg2) {
    if(strTextArg2.isPresent()) {
      if(strTextArg2.get().trim().endsWith("国"))
        return true;
    }

    return false;
  }


  static boolean mostLikelyCity(String subtype,
      Optional<String> strTextArg) {
    if (subtype.equals("UNDET")) {
      if (strTextArg.isPresent()) {
        String textArg = strTextArg.get().toLowerCase().trim();
        if (notCities.contains(textArg)
            || textArg.contains("county") || textArg.contains("district")) {
          return false;
        }
      }

      return true;
    }

    return false;
  }

  static Optional<String> getKBPSlotType(String returnLabel,
      String querySubType, String answerSubtype) {

    Optional<String> strTextArg1 = Optional.absent();
    Optional<String> strTextArg2 = Optional.absent();

    return getKBPSlotType(returnLabel,
        querySubType, answerSubtype,
        strTextArg1, strTextArg2);
  }

  static boolean noDestcriptor(Optional<String> strTextArg) {
    if (strTextArg.isPresent()) {
      if (strTextArg.get().toLowerCase().contains("nation") ||
          strTextArg.get().toLowerCase().contains("country") ||
          strTextArg.get().toLowerCase().contains("homeland") ||
          strTextArg.get().toLowerCase().contains("国家") ||
          strTextArg.get().toLowerCase().contains("家园")) {
        return false;
      }
    }

    return true;
  }

  // TODO:
  // -- Costa Rican, Costa Rica, Rhodesia -> country
  // -- Pampanga, Helmand, Maguindanao, Guinea - Bissau, tasmania -> providence
  static Optional<String> getKBPSlotType(String returnLabel,
      String querySubType, String answerSubtype,
      Optional<String> strTextArg1, Optional<String> strTextArg2) {

    String slotName = "";

    // hack to use patterns from cs2015 assessments
    if(returnLabel.startsWith("cs2015_")) {
      String newReturnLabel = returnLabel.substring(returnLabel.indexOf("_")+1);
      System.out.println("Change fact type: " + returnLabel + " -> " + newReturnLabel);
      returnLabel = newReturnLabel;
    }
    //

    for (String factType : factType2KBPSlots.keySet()) {
      if (returnLabel.startsWith(factType)) {
        slotName = factType2KBPSlots.get(factType);
      }
    }

    // only nationality or ethic groups
    if (returnLabel.startsWith("per_origin")) {
      if ((answerSubtype.equals("Nation") || answerSubtype.equals("Special") || answerSubtype
          .equals("Continent"))
          && noDestcriptor(strTextArg2)) {
        slotName = "per:origin";
      }
    }

    if (returnLabel.startsWith("per_place_of_birth_serif") || returnLabel
        .startsWith("Country_of_birth") || returnLabel.startsWith("per_place_of_birth")) {
      if (answerSubtype.equals("Nation") || answerSubtype.equals("Special")) {
        slotName = "per:country_of_birth";
      } else if (answerSubtype.equals("State-or-Province")) {
        slotName = "per:stateorprovince_of_birth";
      } else if (answerSubtype.equals("Population-Center")) {
        slotName = "per:city_of_birth";
      } else if (mostLikelyCity(answerSubtype, strTextArg1, strTextArg2)) {
        slotName = "per:city_of_birth";
      }
//				slotName = "per:city_of_birth"; // most likely
    }
    if (returnLabel.startsWith("per_place_of_death_serif") || returnLabel
        .startsWith("per_place_of_death")) {
      //split by subtype
      if (answerSubtype.startsWith("Nation") || answerSubtype.equals("Special")) {
        slotName = "per:country_of_death";
      } else if (answerSubtype.equals("State-or-Province")) {
        slotName = "per:stateorprovince_of_death";
      } else if (answerSubtype.equals("Population-Center")) {
        slotName = "per:city_of_death";
      } else if (mostLikelyCity(answerSubtype, strTextArg1, strTextArg2)) {
        slotName = "per:city_of_death";
      }
    }

    if (returnLabel.startsWith("Headquarters") || returnLabel
        .startsWith("org_place_of_headquarters")
        || returnLabel.startsWith("GPENestedInORG")) { // nested names
      //split by subtype
      if (answerSubtype.startsWith("Nation") || answerSubtype.equals("Special")) {
        slotName = "org:country_of_headquarters";
      } else if (answerSubtype.equals("State-or-Province")) {
        slotName = "org:stateorprovince_of_headquarters";
      } else if (answerSubtype.equals("Population-Center")) {
        slotName = "org:city_of_headquarters";
      } else if (mostLikelyCity(answerSubtype, strTextArg1, strTextArg2)) {
        slotName = "org:city_of_headquarters"; // default
      }
    }
    if (returnLabel.startsWith("resident_of") || returnLabel.startsWith("per_place_of_residence")) {
      //split by subtype
      if (answerSubtype.startsWith("Nation") || answerSubtype.equals("Special")) {
        slotName = "per:countries_of_residence";
      } else if (answerSubtype.equals("State-or-Province")) {
        slotName = "per:statesorprovinces_of_residence";
      } else if (answerSubtype.equals("Population-Center")) {
        slotName = "per:cities_of_residence";
      } else if (mostLikelyCity(answerSubtype, strTextArg1, strTextArg2)) {
        slotName = "per:cities_of_residence"; // default
      }
    }

    // relations outside of KBP schema for supporting inferences
    if (returnLabel.startsWith("part_whole_geo")) {
      if (querySubType.equals("Population-Center")) {
        querySubType = "city";
      } else if (querySubType.equals("State-or-Province")) {
        querySubType = "state-or-province";
      } else if (querySubType.equals("Nation") || answerSubtype.equals("Special")) {
        querySubType = "country";
      }
// 			else if(mostLikelyCity(querySubType, strTextArg1, strTextArg2))
      else if (mostLikelyCity(querySubType, strTextArg1)) {
        querySubType = "city"; // default
      }
      if (answerSubtype.equals("Population-Center")) {
        answerSubtype = "city";
      } else if (answerSubtype.equals("State-or-Province")) {
        answerSubtype = "state-or-province";
      } else if (answerSubtype.equals("Nation") || answerSubtype.equals("Special")) {
        answerSubtype = "country";
      }
//			else if(mostLikelyCity(answerSubtype, strTextArg1, strTextArg2))
      else if (mostLikelyCity(answerSubtype, strTextArg2)) {
        answerSubtype = "city"; // default
      }

      if ((querySubType.equals("city") || querySubType.equals("state-or-province") || querySubType
          .equals("country")) &&
          (answerSubtype.equals("city") || answerSubtype.equals("state-or-province")
               || answerSubtype.equals("country"))) {
        String strType = querySubType + ":gpe_contains:" + answerSubtype;
        slotName = strType;
      }
    }

    // KBP-compatible slot, fall through
    if (returnLabel.startsWith("per:") || returnLabel.startsWith("org:") || returnLabel
        .startsWith("gpe:")) {
      slotName = returnLabel;
    }

    if (slotName.trim().isEmpty()) {
      System.err.println("Unknown fact type: " + returnLabel + ", querySubType=" + querySubType
          + ", answerSubtype=" + answerSubtype);
//			throw new RuntimeException("Unknown fact type: " + returnLabel);
      return Optional.absent();
    } else {
      return Optional.of(slotName);
    }
  }

  static ImmutableMap<String, String> slot2inverse = (new ImmutableMap.Builder<String, String>())
      .put("per:children", "per:parents")
      .put("per:other_family", "per:other_family")
      .put("per:parents", "per:children")
      .put("per:siblings", "per:siblings")
      .put("per:spouse", "per:spouse")
      .put("org:alternate_names", "org:alternate_names") // should remove
      .put("per:alternate_names", "per:alternate_names") // should remove
      .put("per:schools_attended", "org:students")
      .put("org:students", "per:schools_attended")
      .put("per:city_of_birth", "gpe:births_in_city")
      .put("gpe:births_in_city", "per:city_of_birth")
      .put("gpe:births_in_stateorprovince", "per:stateorprovince_of_birth")
      .put("gpe:births_in_country", "per:country_of_birth")
      .put("per:stateorprovince_of_birth", "gpe:births_in_stateorprovince")
      .put("per:country_of_birth", "gpe:births_in_country")
      .put("per:cities_of_residence", "gpe:residents_of_city")
      .put("per:countries_of_residence", "gpe:residents_of_country")
      .put("per:statesorprovinces_of_residence", "gpe:residents_of_stateorprovince")
      .put("gpe:residents_of_city", "per:cities_of_residence")
      .put("gpe:residents_of_country", "per:countries_of_residence")
      .put("gpe:residents_of_stateorprovince", "per:statesorprovinces_of_residence")
      .put("per:stateorprovince_of_death", "gpe:deaths_in_stateorprovince")
      .put("gpe:deaths_in_stateorprovince", "per:stateorprovince_of_death")
      .put("per:city_of_death", "gpe:deaths_in_city")
      .put("gpe:deaths_in_city", "per:city_of_death")
      .put("per:country_of_death", "gpe:deaths_in_country")
      .put("gpe:deaths_in_country", "per:country_of_death")
//			.put("org:shareholders", "per:holds_shares_in")
      .put("per:holds_shares_in", "org:shareholders")
      .put("per:organizations_founded", "org:founded_by")
      .put("gpe:organizations_founded", "org:founded_by")
      .put("org:top_members_employees", "per:top_member_employee_of")
      .put("org:member_of", "org:members")
      .put("gpe:member_of", "org:members")
      .put("org:subsidiaries", "org:parents")
      .put("org:country_of_headquarters", "gpe:headquarters_in_country")
      .put("org:stateorprovince_of_headquarters", "gpe:headquarters_in_stateorprovince")
      .put("org:city_of_headquarters", "gpe:headquarters_in_city")
      .build();

  static ImmutableSet<String> eliteSlots = (new ImmutableSet.Builder<String>())
      .add("gpe:residents_of_city")
      .add("org:employees_or_members")
      .add("gpe:employees_or_members")
      .add("gpe:headquarters_in_city")
      .add("per:title")
      .add("per:employee_or_member_of")
      .add("org:membership")
      .add("per:top_member_employee_of")
      .add("org:top_members_employees")
      .build();


  static ImmutableMultimap<String, String> slot2fillerTypes =
      (new ImmutableMultimap.Builder<String, String>())
          .put("gpe:births_in_city", "per")
          .put("gpe:births_in_country", "per")
          .put("gpe:births_in_stateorprovince", "per")
          .put("gpe:deaths_in_city", "per")
          .put("gpe:deaths_in_country", "per")
          .put("gpe:deaths_in_stateorprovince", "per")
          .put("gpe:employees_or_members", "per")
          .put("gpe:headquarters_in_city", "org")
          .put("gpe:headquarters_in_country", "org")
          .put("gpe:headquarters_in_stateorprovince", "org")
          .put("gpe:holds_shares_in", "org")
          .put("gpe:member_of", "org")
          .put("gpe:organizations_founded", "org")
          .put("gpe:residents_of_city", "per")
          .put("gpe:residents_of_country", "per")
          .put("gpe:residents_of_stateorprovince", "per")
          .put("gpe:subsidiaries", "org")
          .put("org:alternate_names", "org")
          .put("org:city_of_headquarters", "gpe")
          .put("org:country_of_headquarters", "gpe")
          .put("org:employees_or_members", "per")
          .put("org:holds_shares_in", "org")
          .put("org:member_of", "org")

          .put("org:parents", "org")
          .put("org:parents", "gpe")

          .put("org:shareholders", "per")
          .put("org:shareholders", "org")
          .put("org:shareholders", "gpe")

          .put("org:stateorprovince_of_headquarters", "gpe")
          .put("org:students", "per")
          .put("org:subsidiaries", "org")
          .put("org:top_members_employees", "per")
          .put("per:alternate_names", "per")
          .put("per:children", "per")
          .put("per:cities_of_residence", "gpe")
          .put("per:city_of_birth", "gpe")
          .put("per:city_of_death", "gpe")
          .put("per:countries_of_residence", "gpe")
          .put("per:country_of_birth", "gpe")
          .put("per:country_of_death", "gpe")

          .put("per:employee_or_member_of", "org")
          .put("per:employee_or_member_of", "gpe")

          .put("per:holds_shares_in", "org")

          .put("per:organizations_founded", "org")
          .put("org:organizations_founded", "org")

          .put("per:other_family", "per")
          .put("per:parents", "per")
          .put("per:schools_attended", "org")
          .put("per:siblings", "per")
          .put("per:spouse", "per")
          .put("per:stateorprovince_of_birth", "gpe")
          .put("per:stateorprovince_of_death", "gpe")
          .put("per:statesorprovinces_of_residence", "gpe")
          .put("per:top_member_employee_of", "org")
          .build();

  public static boolean isValidFillerTypeForSlot(String fillerType, String strSlot) {
    if (slot2fillerTypes.get(strSlot).contains(fillerType.toLowerCase())) {
      return true;
    }

    return false;
  }

  public static boolean isEliteSlot(Slot slot) {
    return eliteSlots.contains(slot);
  }

  static Optional<String> getReverseRelationType(String relnType, String answerType) {
    if (slot2inverse.containsKey(relnType)) {
      return Optional.of(slot2inverse.get(relnType));
    } else if (relnType.equals("per:employee_or_member_of")) {
      return Optional.of(answerType.substring(0, 3).toLowerCase() + ":employees_or_members");
    } else if (relnType.equals("org:employees_or_members") || relnType
        .equals("gpe:employees_or_members")) {
      return Optional.of("per:employee_or_member_of");
    } else if (relnType.equals("org:founded_by")) {
      return Optional
          .of(answerType.substring(0, 3).toLowerCase() + ":organizations_founded"); // per or gpe
    } else if (relnType.equals("org:members")) {
      if (!answerType.substring(0, 3).equals("loc")) {
        return Optional.of(answerType.substring(0, 3).toLowerCase() + ":member_of");
      }
    } else if (relnType.equals("org:parents")) {
      if(answerType.substring(0, 3).toLowerCase().equals("org") || answerType.substring(0, 3).toLowerCase().equals("gpe"))
        return Optional.of(answerType.substring(0, 3).toLowerCase() + ":subsidiaries");
      else
        return Optional.absent();
    } else if (relnType.equals("org:shareholders")) {
      return Optional.of(answerType.substring(0, 3).toLowerCase() + ":holds_shares_in");
    } else if (relnType.equals("per:holds_shares_in") || relnType.equals("org:holds_shares_in")
        || relnType.equals("gpe:holds_shares_in")) {
      return Optional.of("org:shareholders");
    }

//		else if(relnType.equals("org:member_of")||relnType.equals("gpe:member_of")) reverseRelnType = getArg2Type()+":membership";// ??
//		else if(relnType.equals("org:membership")||relnType.equals("gpe:membership")) reverseRelnType = "per:member_of";

//		else if(relnType.equals("org:members")) reverseRelnType = getArg2Type()+":member_of";
//		else if(relnType.equals("org:member_of")||relnType.equals("gpe:member_of")) reverseRelnType = "org:members";

//		else if(relnType.equals("org:founded_by")) reverseRelnType = "per:organizations_founded";

    return Optional.absent();
  }

  public static boolean isSingleValueSlot(String strSlot) {
    if (singleValuedSlots.contains(strSlot)) {
      return true;
    } else {
      return false;
    }
  }

  static ImmutableSet<String> dateSlots = (new ImmutableSet.Builder<String>())
      .add("per:date_of_birth")
      .add("per:date_of_death")
      .add("org:date_founded")
      .add("org:date_dissolved")
      .build();

  public static boolean isDateSlot(Slot slot) {
    if (dateSlots.contains(slot.toString())) {
      return true;
    } else {
      return false;
    }
  }

  static List<String> valueSlots = Arrays.asList(
      "per:alternate_names",
      "per:date_of_birth",
      "per:age",
      "per:origin",
      "per:date_of_death",
      "per:cause_of_death",
      "per:title",
      "per:religion",
      "per:charges",

      "org:alternate_names",
      "org:political_religious_affiliation",
      "org:number_of_employees_members",
      "org:date_founded",
      "org:date_dissolved",
      "org:website");

  public static boolean isValueSlot(Slot slot) {
    if (valueSlots.contains(slot.toString())) {
      return true;
    } else {
      return false;
    }
  }

  static List<String> singleValuedSlots = Arrays.asList(
      "per:date_of_birth",
      "per:age",
      "per:country_of_birth",
      "per:stateorprovince_of_birth",
      "per:city_of_birth",
      "per:date_of_death",
      "per:country_of_death",
      "per:stateorprovince_of_death",
      "per:city_of_death",
      "per:cause_of_death",
      "per:religion",
      "org:number_of_employees_members",
      "org:date_founded",
      "org:date_dissolved",
      "org:country_of_headquarters",
      "org:stateorprovince_of_headquarters",
      "org:city_of_headquarters",
      "org:website");

  static List<String> listValuedSlots = Arrays.asList(
      "per:alternate_names",
      "per:origin",
      "per:countries_of_residence",
      "per:statesorprovinces_of_residence",
      "per:cities_of_residence",
      "per:schools_attended",
      "per:title",
      "per:member_of",
      "per:employee_of",
      "per:spouse",
      "per:children",
      "per:parents",
      "per:siblings",
      "per:other_family",
      "per:charges",
      "org:alternate_names",
      "org:political_religious_affiliation",
      "org:top_members_employees",
      "org:members",
      "org:member_of",
      "org:subsidiaries",
      "org:parents",
      "org:founded_by",
      "org:shareholders",

      // Cold Start inverse slots added to regular slot-filling slots
      "org:employees",
      "gpe:employees",
      "gpe:member_of",
      "org:membership",
      "org:students",
      "gpe:births_in_city",
      "gpe:births_in_stateorprovince",
      "gpe:births_in_country",
      "gpe:residents_of_city",
      "gpe:residents_of_stateorprovince",
      "gpe:residents_of_country",
      "gpe:deaths_in_city",
      "gpe:deaths_in_stateorprovince",
      "gpe:deaths_in_country",
      "per:holds_shares_in",
      "org:holds_shares_in",
      "gpe:holds_shares_in",
      "per:organizations_founded",
      "org:organizations_founded",
      "gpe:organizations_founded",
      "per:top_member_employee_of",
      "gpe:headquarters_in_city",
      "gpe:headquarters_in_stateorprovince",
      "gpe:headquarters_in_country",

      // 2013 slots
      "per:employee_or_member_of",
      "org:employees_or_members",
      "gpe:employees_or_members");


  static ImmutableMap<String, Integer> slot2maxCardinality =
      (new ImmutableMap.Builder<String, Integer>())
          .put("per:alternate_names", 20)
          .put("per:origin", 3)
          .put("per:countries_of_residence", 30)
          .put("per:statesorprovinces_of_residence", 30)
          .put("per:cities_of_residence", 30)
          .put("per:schools_attended", 20)
          .put("per:title", 50)
          .put("per:member_of", 30)
          .put("per:employee_of", 30)
          .put("per:spouse", 8)
          .put("per:children", 30)
          .put("per:parents", 8)
          .put("per:siblings", 10)
          .put("per:other_family", 20)
          .put("per:charges", 30)
          .put("org:alternate_names", 20)
          .put("org:political_religious_affiliation", 10)
          .put("org:top_members_employees", 40)
          .put("org:members", 200)
          .put("org:member_of", 30)
          .put("org:subsidiaries", 200)
          .put("org:parents", 40)
          .put("org:founded_by", 10)
          .put("org:shareholders", 100)

              // Cold Start inverse slots added to regular slot-filling slots
          .put("org:employees", 300)
          .put("gpe:employees", 300)
          .put("gpe:member_of", 30)
          .put("org:membership", 100)
          .put("org:students", 1000)
          .put("gpe:births_in_city", 1000)
          .put("gpe:births_in_stateorprovince", 1000)
          .put("gpe:births_in_country", 5000)
          .put("gpe:residents_of_city", 1000)
          .put("gpe:residents_of_stateorprovince", 1000)
          .put("gpe:residents_of_country", 10000)
          .put("gpe:deaths_in_city", 1000)
          .put("gpe:deaths_in_stateorprovince", 1000)
          .put("gpe:deaths_in_country", 5000)
          .put("per:holds_shares_in", 100)
          .put("org:holds_shares_in", 100)
          .put("gpe:holds_shares_in", 100)
          .put("per:organizations_founded", 20)
          .put("org:organizations_founded", 10)
          .put("gpe:organizations_founded", 10)
          .put("per:top_member_employee_of", 10)
          .put("gpe:headquarters_in_city", 500)
          .put("gpe:headquarters_in_stateorprovince", 2000)
          .put("gpe:headquarters_in_country", 5000)

              // 2013 slots
          .put("per:employee_or_member_of", 100)
          .put("org:employees_or_members", 600)
          .put("gpe:employees_or_members", 1200)

          .build();

  // for D2D Nigeria
  static ImmutableMap<String, Integer> slot2maxCardinalityHighPrecision =
      (new ImmutableMap.Builder<String, Integer>())
          .put("per:alternate_names", 5)
          .put("per:origin", 3)
          .put("per:countries_of_residence", 5)
          .put("per:statesorprovinces_of_residence", 5)
          .put("per:cities_of_residence", 5)
          .put("per:schools_attended", 5)
          .put("per:title", 5)
          .put("per:member_of", 5)
          .put("per:employee_of", 5)
          .put("per:spouse", 3)
          .put("per:children", 6)
          .put("per:parents", 4)
          .put("per:siblings", 5)
          .put("per:other_family", 5)
          .put("per:charges", 3)
          .put("org:alternate_names", 5)
          .put("org:political_religious_affiliation", 3)
          .put("org:top_members_employees", 10)
          .put("org:members", 20)
          .put("org:member_of", 10)
          .put("org:subsidiaries", 8)
          .put("org:parents", 4)
          .put("org:founded_by", 5)
          .put("org:shareholders", 8)

              // Cold Start inverse slots added to regular slot-filling slots
          .put("org:employees", 100)
          .put("gpe:employees", 50)
          .put("gpe:member_of", 5)
          .put("org:membership", 8)
          .put("org:students", 100)
          .put("gpe:births_in_city", 100)
          .put("gpe:births_in_stateorprovince", 100)
          .put("gpe:births_in_country", 200)
          .put("gpe:residents_of_city", 100)
          .put("gpe:residents_of_stateorprovince", 100)
          .put("gpe:residents_of_country", 200)
          .put("gpe:deaths_in_city", 100)
          .put("gpe:deaths_in_stateorprovince", 100)
          .put("gpe:deaths_in_country", 200)
          .put("per:holds_shares_in", 8)
          .put("org:holds_shares_in", 8)
          .put("gpe:holds_shares_in", 8)
          .put("per:organizations_founded", 5)
          .put("org:organizations_founded", 5)
          .put("gpe:organizations_founded", 5)
          .put("per:top_member_employee_of", 5)
          .put("gpe:headquarters_in_city", 30)
          .put("gpe:headquarters_in_stateorprovince", 50)
          .put("gpe:headquarters_in_country", 200)

              // 2013 slots
          .put("per:employee_or_member_of", 6)
          .put("org:employees_or_members", 10)
          .put("gpe:employees_or_members", 10)

          .build();


  public static int getSlotMaxCardinalityHighPrecision(String strSlot) {
    if (slot2maxCardinalityHighPrecision.containsKey(strSlot)) {
      return slot2maxCardinalityHighPrecision.get(strSlot);
    } else if (singleValuedSlots.contains(strSlot)) {
      return 1;
    } else {
      System.out.println(
          "getSlotMaxCardinality:\tslot2maxCardinalityHighPrecision constraint not set for slot: "
              + strSlot);
    }
    return Integer.MAX_VALUE;
  }

  public static int getSlotMaxCardinality(String strSlot) {
    if (slot2maxCardinality.containsKey(strSlot)) {
      return slot2maxCardinality.get(strSlot);
    } else if (singleValuedSlots.contains(strSlot)) {
      return 1;
    } else {
      System.out
          .println("getSlotMaxCardinality:\tCardinality constraint not set for slot: " + strSlot);
    }
    return Integer.MAX_VALUE;
  }

  static ImmutableSet<String> additionalSlots = (new ImmutableSet.Builder<String>())
      //	static List<String> additionalSlots = Arrays.asList(
      .add("state-or-province:gpe_contains:city")
      .add("state-or-province:gpe_contains:state-or-province")
      .add("state-or-province:gpe_contains:country")
      .add("country:gpe_contains:city")
      .add("country:gpe_contains:state-or-province")
      .add("country:gpe_contains:country")
      .add("city:gpe_contains:city")
      .add("city:gpe_contains:state-or-province")  // TODO: fix bad slots
      .add("city:gpe_contains:country")
      .add("loc:employees_or_members")
      .add("per:employees_or_members")
      .add("fac:employees_or_members")
      .add("wea:employees_or_members")
      .add("veh:employees_or_members")
      .add("gpe:subsidiaries")
      .add("loc:organizations_founded")
      .add("fac:member_of")
      .build();


  static ImmutableSet<String> allSlots = (new ImmutableSet.Builder<String>())
      .addAll(singleValuedSlots)
      .addAll(listValuedSlots)
      .addAll(additionalSlots)
      .build();

  static public ImmutableSet<String> getAllSlots() {
    return allSlots;

  }

  static public ImmutableSet<Slot> getSFSlotValues(List<Slot> slotsToIgnore, String entityType) {
    entityType = entityType.toLowerCase();
    ImmutableSet.Builder<Slot> validSlots = new ImmutableSet.Builder<Slot>();
    for (String slot : allSlots) {
      if (slot.startsWith(entityType) && !slotsToIgnore.contains(fromStringSlotName(slot))) {
        validSlots.add(fromStringSlotName(slot));
      }
    }
    return validSlots.build();
  }


  public static boolean isAddtionalSlot(Slot slot) {
    return additionalSlots.contains(slot);
  }

	/*
         * given queryId:slot:parent_eclass, classify slot as "single" or "list" valued
	 */

  static String getSlotType(String slot_name) {
    if (singleValuedSlots.contains(slot_name)) {
      return "single";
    }
    if (listValuedSlots.contains(slot_name)) {
      return "list";
    }
    if (additionalSlots.contains(slot_name)) {
      return "additional";
    }
    System.out.println("Invalid slot " + slot_name);
    return "error";
  }
}
