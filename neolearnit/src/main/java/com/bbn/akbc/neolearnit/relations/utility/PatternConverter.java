package com.bbn.akbc.neolearnit.relations.utility;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.observations.pattern.BrandyablePattern;
import com.bbn.akbc.neolearnit.observations.pattern.restriction.Restriction;
import com.bbn.serif.patterns.Pattern;
import com.bbn.serif.patterns.converters.SexpPatternConverter;
import com.google.common.collect.Lists;

public class PatternConverter {
	// temporary fix
	public static String convertRolesToLowerCase(String patternString) {
		int idxBegin = 0;
		while(true) {
			int idxOfRoleStart = patternString.indexOf("(role", idxBegin);
			if(idxOfRoleStart<0)
				break;

			int idxOfRoleEnd = patternString.indexOf(")", idxOfRoleStart);

			patternString = patternString.substring(0, idxOfRoleStart) + patternString.substring(idxOfRoleStart, idxOfRoleEnd).toLowerCase() + patternString.substring(idxOfRoleEnd);

			idxBegin = idxOfRoleEnd;
		}

		return patternString;
	}

  public static String convertMentionTypeToLowerCase(String patternString) {
    int idxBegin = 0;
    while(true) {
      int idxOfTagStart = patternString.indexOf("(mentiontype", idxBegin);
      if(idxOfTagStart<0)
	break;

      int idxOfTagEnd = patternString.indexOf(")", idxOfTagStart);

      patternString = patternString.substring(0, idxOfTagStart) + patternString.substring(idxOfTagStart, idxOfTagEnd).toLowerCase() + patternString.substring(idxOfTagEnd);

      idxBegin = idxOfTagEnd;
    }

    return patternString;
  }

  static boolean isValidText(String regexText) {
    boolean isValid = true;

    String [] tokens = regexText.split(" ");
    for(String token : tokens) {
      if(token.equals(","))
        continue;

      for (char c : token.toCharArray()) {
        if (!Character.isLetter(c))
          isValid = false;
      }
    }

    return isValid;
  }

  public static boolean isValidRegex(String patternString) {
    String tag = "(text DONT_ADD_SPACES (string";
    int idxBegin = 0;
    while(true) {
      int idxOfTagStart = patternString.indexOf(tag, idxBegin);
      if(idxOfTagStart<0)
	break;

      int idxOfTagEnd = patternString.indexOf(")", idxOfTagStart);

      String regexText = patternString.substring(idxOfTagStart + tag.length(), idxOfTagEnd).trim();
      if(!regexText.startsWith("\"") || !regexText.endsWith("\""))
	return false;
      regexText = regexText.substring(1, regexText.length()-1);
      if(!isValidText(regexText))
	return false;

      idxBegin = idxOfTagEnd;
    }

    return true;

//  return patternString;
  }




	public static String getPatternString(Target target, BrandyablePattern pattern, int id) {
		// throw in an ID
		String idString = target.getName()+"_"+id;

          return getPatternString(target, idString, pattern);
          /*
		Pattern brandy = pattern.convertToBrandy(idString, target, Lists.<Restriction>newArrayList());
		brandy = brandy.modifiedCopyBuilder()
				.withId(Symbol.from(idString))
				.withScore((float) 0.9)
				.withScoreGroup(1)
				.build();

		return convertRolesToLowerCase(new SexpPatternConverter().convert(brandy).toString());
		*/
	}


  public static String getPatternString(Target target, String idString, BrandyablePattern pattern) {
    // throw in an ID
    Pattern brandy = pattern.convertToBrandy(idString, target, Lists.<Restriction>newArrayList());
    brandy = brandy.modifiedCopyBuilder()
        .withId(Symbol.from(idString))
        .withScore((float) 0.9)
        .withScoreGroup(1)
        .build();

    return convertRolesToLowerCase(new SexpPatternConverter().convert(brandy).toString());

  }


}
