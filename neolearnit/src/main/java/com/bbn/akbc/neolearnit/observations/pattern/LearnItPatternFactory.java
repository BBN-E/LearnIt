package com.bbn.akbc.neolearnit.observations.pattern;

import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.observations.pattern.PropPattern.PropArgObservation;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.bue.sexp.Sexp;
import com.bbn.bue.sexp.SexpReader;
import com.bbn.serif.patterns.ArgumentPattern;
import com.bbn.serif.patterns.MapPatternReturn;
import com.bbn.serif.patterns.MentionPattern;
import com.bbn.serif.patterns.Pattern;
import com.bbn.serif.patterns.PatternFactory;
import com.bbn.serif.patterns.PropPattern;
import com.bbn.serif.patterns.RegexPattern;
import com.bbn.serif.patterns.TextPattern;
import com.bbn.serif.patterns.ValueMentionPattern;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class LearnItPatternFactory {
	static String language = "english";

	public static LearnitPattern fromBrandyPattern(Pattern pattern, Target t) throws Exception {
		if(pattern instanceof RegexPattern) {
			RegexPattern p = (RegexPattern) pattern;

			BetweenSlotsContent.Builder<RegexableContent> contentBuilder = new BetweenSlotsContent.Builder<RegexableContent>();
			for(int i=1; i<p.getSubpatterns().size()-1; i++) {
				if(p.getSubpatterns().get(i) instanceof TextPattern) {
					TextPattern textPattern = (TextPattern) p.getSubpatterns().get(i);
					SymbolContent symbolContent = new SymbolContent(textPattern.getText());
					contentBuilder.withAddContent(symbolContent);

					System.out.println("\t" + i + "\t" + textPattern.getText());
				}
				else if(p.getSubpatterns().get(i) instanceof MentionPattern) {
					MentionPattern mentionPattern = (MentionPattern) p.getSubpatterns().get(i);
					EntityTypeContent entityTypeContent = new EntityTypeContent(mentionPattern.getEntityTypes().get(0)); // only takes the first one
					contentBuilder.withAddContent(entityTypeContent);
				}
			}

			Pattern arg1pattern = p.getSubpatterns().get(0);
			Pattern arg2pattern = p.getSubpatterns().get(p.getSubpatterns().size()-1);

			// only support betweenSlotPattern at the moment
			if(!(arg1pattern instanceof MentionPattern) && !(arg1pattern instanceof ValueMentionPattern))
				return null;
			if(!(arg2pattern instanceof MentionPattern) && !(arg2pattern instanceof ValueMentionPattern))
				return null;

			MapPatternReturn patReturn1 = (MapPatternReturn)arg1pattern.getPatternReturn();
			for(String key : patReturn1.keySet())
				System.out.println("\t" + "key: " + key);

			if(patReturn1.hasValue("ff_role")) {
				if(patReturn1.get("ff_role").equals("AGENT1")) {
					return new BetweenSlotsPattern(language, 0, 1, contentBuilder.build());
				}
			}

			if(t.isSymmetric())
				return new BetweenSlotsPattern(language, 0, 1, contentBuilder.build());
			else
				return new BetweenSlotsPattern(language, 1, 0, contentBuilder.build());
		}
		else if(pattern instanceof PropPattern) {
			PropPattern p = (PropPattern) pattern;

//			System.out.println(p.getLanguage().toString());
			System.out.println(p.getPredicateType().toString());

			com.bbn.akbc.neolearnit.observations.pattern.PropPattern.Builder builder = new com.bbn.akbc.neolearnit.observations.pattern.PropPattern.Builder(language, p.getPredicateType());

			builder.withPredicates(p.getPredicates());
//			builder.withArg(p.getArgs());
//			for(ArgumentPattern argumentPattern : p.getArgs()) {
			assert(p.getArgs().size()==2);

			int idxOfAgent1=0;
			List<PropArgObservation> listArgs = new ArrayList<PropArgObservation>();

			for(int slot=0; slot<p.getArgs().size(); slot++) {
				ArgumentPattern argumentPattern = p.getArgs().get(slot);

				System.out.println("slot: " + slot);
				for(Symbol role : argumentPattern.getRoles()) {
					System.out.println("\trole: " + role.toString());
				}

				Symbol role = argumentPattern.getRoles().get(0); // TODO: pick multiple roles

				Pattern argPattern = argumentPattern.getPattern();

				MapPatternReturn patReturn1 = (MapPatternReturn)argPattern.getPatternReturn();
				if(patReturn1.hasValue("ff_role")) {
					if(patReturn1.get("ff_role").equals("AGENT1")) {
						idxOfAgent1 =slot;
					}
				}

				if(argPattern instanceof MentionPattern || argPattern instanceof ValueMentionPattern) {
					PropArgObservation propArgObservation = new PropArgObservation(role, slot);
					listArgs.add(propArgObservation);
//					builder.withArg(propArgObservation);
				}
				else if(argPattern instanceof PropPattern) {
					LearnitPattern pArg = fromBrandyPattern(argPattern, t);
					if(pArg==null)
						return null;

					if(pArg instanceof com.bbn.akbc.neolearnit.observations.pattern.PropPattern) {
						PropArgObservation propArgObservation = new PropArgObservation(role, slot,
								(com.bbn.akbc.neolearnit.observations.pattern.PropPattern) pArg);
						listArgs.add(propArgObservation);
//						builder.withArg(propArgObservation);
					}
				}
			}

			List<PropArgObservation> slotArgs = Lists.newArrayList();
			if(idxOfAgent1==0) {
				slotArgs.add(listArgs.get(0).copyWithSlot(0));
				slotArgs.add(listArgs.get(1).copyWithSlot(1));
			}
			else if(idxOfAgent1==1) {
				slotArgs.add(listArgs.get(0).copyWithSlot(1));
				slotArgs.add(listArgs.get(1).copyWithSlot(0));
			}

			if(t.isSymmetric()) {
				final ImmutableList<PropArgObservation> orderedArgs = Ordering.natural().onResultOf(PropArgObservation.ByRole).immutableSortedCopy( slotArgs );
				builder.withArg(orderedArgs.get(0).copyWithSlot(0));
				builder.withArg(orderedArgs.get(1).copyWithSlot(1));
			}
			else {
				builder.withArg(slotArgs.get(0));
				builder.withArg(slotArgs.get(1));
			}



			return builder.build();
		}
		else {
			// TODO: return optional

			System.out.println("Pattern not recognized!");
			return null;
		}
	}

	public static LearnitPattern fromBrandyPattern(Sexp sexp, Target t) {
		PatternFactory patternFactory = new PatternFactory();
		Pattern pattern = patternFactory.fromSexp(sexp);

		try {
			return LearnItPatternFactory.fromBrandyPattern(pattern, t);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	public static LearnitPattern fromBrandyPattern(String strPattern, Target t) {
		SexpReader sexpreader = SexpReader.createDefault(); // (new SexpReade.()).build();
		Sexp sexp;
		try {
			sexp = sexpreader.read(strPattern);
			return LearnItPatternFactory.fromBrandyPattern(sexp, t);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	public static Set<LearnitPattern> fromFile(File patternFile, Target t) {
        ImmutableSet.Builder<LearnitPattern> builder = ImmutableSet.builder();
        try {
			for (String line : Files.readLines(patternFile, Charsets.UTF_8)) {
				line = line.trim();

				if(line.isEmpty()) continue;
				if(!line.startsWith("(") || !line.endsWith("")) continue;

				System.out.println("line: " + line);

		        try {
					LearnitPattern pattern = LearnItPatternFactory.fromBrandyPattern(line, t);

					if(pattern!=null) {
						builder.add(pattern);
						System.out.println("pattern: " + pattern.toIDString());
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
//		} catch (IOException e) {
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        return builder.build();
	}

/*
	public static void main(String [] argv) throws Exception {
		String strPatternFile = "/nfs/mercury-04/u42/bmin/source/Active/Projects/neolearnit/inputs/patterns/bilingual_chinese_gigaword_default/personSocialFamily.patterns";
        Set<LearnitPattern> initPatternsInSexpFormat = ImmutableSet.of();
    	initPatternsInSexpFormat = LearnItPatternFactory.fromFile(new File(strPatternFile));

    	for(LearnitPattern p : initPatternsInSexpFormat)
    		System.out.println(p.toPrettyString() + "\n\n");

		String strPattern1 = "(vprop (id org_founded_by_2) (score 0.9) (score_group 1) (predicate founded) (args (argument (role <sub>) (mention (return (ff_role org_founded_by)) (min-entitylevel DESC) (block AGENT1) (acetype PER ORG GPE))) (argument (role <obj>) (mention (return (ff_role AGENT1) (ff_fact_type org_founded_by_2)) (min-entitylevel DESC) (entitylabel AGENT1) (acetype ORG)))))";
		String strPattern2 = "(regex (id org_founded_by_11) (score 0.9) (score_group 1) (re (mention (return (ff_role org_founded_by)) (min-entitylevel DESC) (block AGENT1) (acetype PER ORG GPE)) (text DONT_ADD_SPACES (string \"founded\")) (mention (return (ff_role AGENT1) (ff_fact_type org_founded_by_11)) (min-entitylevel DESC) (entitylabel AGENT1) (acetype ORG))))";

		SexpReader sexpreader = SexpReader.createDefault(); // (new SexpReade.()).build();
		Sexp sexp1 = sexpreader.read(strPattern1);
		Sexp sexp2 = sexpreader.read(strPattern2);

		PatternFactory patternFactory = new PatternFactory();
		Pattern pattern1 = patternFactory.fromSexp(sexp1);
		Pattern pattern2 = patternFactory.fromSexp(sexp2);

		LearnitPattern p1 = LearnItPatternFactory.fromBrandyPattern(pattern1);
		LearnitPattern p2 = LearnItPatternFactory.fromBrandyPattern(pattern2);
	}
	*/
}
