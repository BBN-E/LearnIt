package com.bbn.akbc.common.format;

import com.bbn.akbc.kb.common.SlotFactory;
import com.bbn.akbc.kb.common.Slot;
import com.google.common.base.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateKBPnormalizer {

  static Pattern time2Pattern = Pattern.compile("[X|\\d]{4}-[X|\\d]{2}-[X|\\d]{2}");

  static Pattern yearmonthPattern = Pattern.compile("[X|\\d]{4}-[X|\\d]{2}");

  static Pattern yearweekPattern = Pattern.compile("[X|\\d]{4}-W[\\d]{2}");

  static Pattern yearPattern = Pattern.compile("[X|\\d]{4}");

  static Pattern decadePattern = Pattern.compile("[X|\\d]{3}");


  public static Optional<String> normalizeDate(String strDate) {
//		System.out.println("strDate: " + strDate);
    strDate = strDate.substring(1, strDate.length() - 1); // strip ""
    if (strDate.length() > 10) {
      strDate = strDate.substring(0, 10); // 2010-09-28TNI -> 2010-09-28
    }

    // in TIMEX2 format: YYYY-MM-DD
    Matcher time2Matcher = time2Pattern.matcher(strDate);
    if (time2Matcher.matches()) {
      String s = time2Matcher.group();
      return Optional.of("\"" + s + "\"");
    }

    // year and month: YYYY-MM
    Matcher yearmonthMatcher = yearmonthPattern.matcher(strDate);
    if (yearmonthMatcher.matches()) {
      String s = yearmonthMatcher.group();
      s = s + "-XX";
      return Optional.of("\"" + s + "\"");
    }

    // year and week: 2008-W07
    Matcher yearweekMatcher = yearweekPattern.matcher(strDate);
    if (yearweekMatcher.matches()) {
      String s = yearweekMatcher.group();
      int idxWeek = Integer.parseInt(s.substring(s.indexOf("W") + 1));

      int idxMonth = (int) ((float) idxWeek / 4.345);
      s = s.substring(0, s.indexOf("-W")) + "-" + String.format("%02d", idxMonth) + "-XX";
      return Optional.of("\"" + s + "\"");
    }

    // just year: YYYY
    Matcher yearMatcher = yearPattern.matcher(strDate);
    if (yearMatcher.matches()) {
      String s = yearMatcher.group();
      s = s + "-XX-XX";
      return Optional.of("\"" + s + "\"");
    }

    // decade: 199 -> 1990s
    Matcher decadeMatcher = decadePattern.matcher(strDate);
    if (decadeMatcher.matches()) {
      String s = decadeMatcher.group();
      s = s + "X";
      return Optional.of("\"" + s + "\"");
    }

    return Optional.absent();
  }

  public static boolean isValidNormalizedDate(String date) {
    return date.matches("[X|\\d]{4}-[X|\\d]{2}-[X|\\d]{2}");
  }

  public static void do_test(Optional<Slot> slot, String strTextArg2) {
    if (SlotFactory.isDateSlot(slot.get())) {
      Optional<String> strNormalizedDate = DateKBPnormalizer.normalizeDate(strTextArg2);
      if (!strNormalizedDate.isPresent()) {
        System.out.println("DateKBPnormalizer:\tREMOVE\t" + strTextArg2);
      } else {
        System.out.println(
            "DateKBPnormalizer:\tNORMALIZE\t" + strTextArg2 + "\t" + strNormalizedDate.get());
        strTextArg2 = strNormalizedDate.get();
      }
    }
  }

  public static void main(String[] argv) {
    String slot = argv[0];
    String date = argv[1];

//    	do_test(Optional.of(SlotFactory.fromStringSlotName("per:date_of_birth")), "1993-02-01");
    do_test(Optional.of(SlotFactory.fromStringSlotName(slot)), date);
  }
}
