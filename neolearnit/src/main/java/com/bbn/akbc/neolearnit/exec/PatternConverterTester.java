package com.bbn.akbc.neolearnit.exec;

import java.io.File;

import com.bbn.akbc.neolearnit.observations.pattern.BetweenSlotsContent;
import com.bbn.akbc.neolearnit.observations.pattern.BetweenSlotsPattern;
import com.bbn.akbc.neolearnit.observations.pattern.BrandyablePattern;
import com.bbn.bue.common.parameters.Parameters;
import com.bbn.bue.sexp.Sexp;
import com.bbn.bue.sexp.SexpList;
import com.bbn.bue.sexp.SexpReader;
import com.bbn.serif.patterns.PatternFactory;
import com.bbn.serif.patterns.Pattern;
import com.bbn.akbc.neolearnit.observations.pattern.PropPattern;

import com.google.common.base.Optional;

public final class PatternConverterTester {

  public static void main(String args[]) throws Exception {
    Parameters params = Parameters.loadSerifStyle(new File(args[0]));
    SexpReader sexpReader = SexpReader.createDefault();
    PatternFactory patternFactory = new PatternFactory();

    Optional<File> propPatternsFile = params.getOptionalExistingFile("toPropPatterns");
    Optional<File> betweenSlotsPatternsFile =
        params.getOptionalExistingFile("toBetweenSlotsPatterns");

    if (propPatternsFile.isPresent()) {
      System.out.println("-----------------------------");
      System.out.println("PropPatterns\n");

      Sexp fullSexp = sexpReader.read(propPatternsFile.get());

      for (Sexp sexp : (SexpList) fullSexp) {
        Pattern p = patternFactory.fromSexp(sexp);
        System.out.println(p.toString() + "\n");

        if (p instanceof com.bbn.serif.patterns.PropPattern) {
          PropPattern propPattern = PropPattern.from((com.bbn.serif.patterns.PropPattern)p);
          System.out.println(propPattern.toPrettyString());
        } else {
          throw new BrandyablePattern.NonConvertibleException(
              "toPropPatterns must contain only Brandy PropPatterns");
        }
      }
      System.out.println("\nend PropPatterns");
    }


    if (betweenSlotsPatternsFile.isPresent()) {
      System.out.println("-----------------------------");
      System.out.println("BetweenSlotsPatterns\n");

      Sexp fullSexp = sexpReader.read(betweenSlotsPatternsFile.get());

      for (Sexp sexp : (SexpList) fullSexp) {
        Pattern p = patternFactory.fromSexp(sexp);
        System.out.println(p.toString() + "\n");

        if (p instanceof com.bbn.serif.patterns.RegexPattern) {
          BetweenSlotsPattern betweenSlotsPattern = BetweenSlotsPattern.from((com.bbn.serif.patterns.RegexPattern)p);
          if (betweenSlotsPattern != null)
            System.out.println(betweenSlotsPattern.toPrettyString());
        } else {
          throw new BrandyablePattern.NonConvertibleException(
              "toBetweenSlotsPatterns must contain only Brandy RegexPatterns");
        }
      }
      System.out.println("\nend BetweenSlotsPatterns");
    }




    System.out.println("-----------------------------");
  }
}
