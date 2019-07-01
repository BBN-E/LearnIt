package com.bbn.akbc.neolearnit.common.util;

import com.bbn.bue.common.StringUtils;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.patterns.*;
import com.bbn.serif.patterns.converters.PatternConverter;
import com.bbn.serif.types.EntitySubtype;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class BrandyToLearnItStringPatternConverter extends PatternConverter<String> {
    public class SymbolComparator implements Comparator<Symbol> {
        public SymbolComparator() {
        }

        public int compare(Symbol arg0, Symbol arg1) {
            return arg0.toString().compareTo(arg1.toString());
        }
    }
	public String convert(PatternSet patternSet) {
		StringBuilder result = new StringBuilder();
		if (patternSet.getEntityLabels().size() > 0) {
			result.append("[EntityLabels: ");
			List<Symbol> elabels = new ArrayList<Symbol>(patternSet.getEntityLabels().keySet());
			Collections.sort(elabels, new SymbolComparator());
			for (Symbol label : elabels) {
				result.append("["+label+": ");
				result.append(convert(patternSet.getEntityLabels().get(label)));
				result.append("]");
			}
			result.append(" ] ");
		}

		List<String> pats = new ArrayList<String>();
		for (Pattern p : patternSet.getTopLevelPatterns()) {
			pats.add(convert(p));
		}
		result.append(StringUtils.CommaSpaceJoin.apply(pats));

		return result.toString();
	}

	@Override
	public String convertArgumentPattern(ArgumentPattern pattern) {
		StringBuilder result = new StringBuilder();

		String roles = StringUtils.CommaJoin.apply(pattern.getRoles());

		result.append(roles.replace("unknown", "mod"));
		result.append("=");
		result.append(convert(pattern.getPattern()));

		return result.toString();
	}

	@Override
	public String convertCombinationPattern(CombinationPattern pattern) {
		StringBuilder result = new StringBuilder();
		result.append(pattern.getCombinationType().toString()+"(");
		List<String> patterns = new ArrayList<String>();
		for (Pattern p : pattern.getPatternList()) {
			patterns.add(convert(p));
		}
		result.append(StringUtils.CommaJoin.apply(patterns));
		result.append(")");
		return result.toString();
	}

	@Override
	public String convertIntersectionPattern(IntersectionPattern pattern) {
		List<String> patterns = new ArrayList<String>();
		for (Pattern p : pattern.getPatternList()) {
			patterns.add(convert(p));
		}
		return StringUtils.join(patterns, " and ");
	}

	private final static int NUM_CLUSTER_WORDS_TO_SHOW = 5;
	private String clusterPreview(Symbol constraint) {
		@SuppressWarnings("unused")
		Symbol clust = Symbol.from(constraint.toString().replace("*", ""));
		List<String> words = new ArrayList<String>();
		Collections.sort(words);
		return StringUtils.CommaJoin.apply(
				words.subList(0, Math.min(words.size(), NUM_CLUSTER_WORDS_TO_SHOW)));
	}

	@Override
	public String convertMentionPattern(MentionPattern pattern) {
		// StringBuilder result = new StringBuilder();

		String labelString;
		if (pattern.getPatternReturn() instanceof LabelPatternReturn) {
			labelString = ((LabelPatternReturn)pattern.getPatternReturn()).getLabel().toString();
			@SuppressWarnings("unused")
			int slotNum = Integer.parseInt(labelString.substring(labelString.length()-1));
			if (pattern.getEntityTypes().size() == 1)
				labelString += ";ETYPE="+pattern.getEntityTypes().get(0);
		} else {
			labelString = "MENTION";
			if (pattern.getEntityTypes().size() > 0)
				labelString += ";ETYPE="+StringUtils.CommaJoin.apply(pattern.getEntityTypes());
		}


		if (pattern.getRegexPattern() != null) {
			labelString += ";CONTAINSWORD=";
			RegexPattern regex = (RegexPattern)(pattern.getRegexPattern());
			for (Pattern sub : regex.getSubpatterns())
				labelString += convert(sub);
		}

		if (pattern.getHeadwords().size() + pattern.getHeadwordPrefixes().size() > 0) {
			labelString += ";HEADWORDS=";
			List<String> allHeadwords = new ArrayList<String>();
			for (Symbol s : pattern.getHeadwords())
				allHeadwords.add(s.toString());
			for (Symbol s : pattern.getHeadwordPrefixes())
				allHeadwords.add(s.toString() + "*");

			if (pattern.getRegexPattern() != null) {
				labelString += ";CONTAINSWORD=";
				RegexPattern regex = (RegexPattern)(pattern.getRegexPattern());
				for (Pattern sub : regex.getSubpatterns())
					labelString += convert(sub);
			}

			Collections.sort(allHeadwords);

			labelString += StringUtils.CommaJoin.apply(allHeadwords);
		}

		if (!pattern.getAceSubtypes().isEmpty()) {
			for(EntitySubtype subtype : pattern.getAceSubtypes())
				labelString += ";ESUBTYPE="+subtype.toString();
		}

		if (pattern.getBrownClusterConstraint()!=null) {
			//labelString += ";HEADCLUSTER="+pattern.getBrownClusterConstraint();
			labelString += ";HEADCLUSTER="+clusterPreview(pattern.getBrownClusterConstraint());
		}

		if (pattern.getEntityLabels().size() > 0) {
			labelString += ";ENTITYLABELS=";
			List<String> allLabels = new ArrayList<String>();
			for (Symbol s : pattern.getEntityLabels()) {
				allLabels.add(s.toString());
			}
			labelString += StringUtils.CommaJoin.apply(allLabels);
		}

		for (ComparisonConstraint cc : pattern.getComparisonConstraints())
			labelString += String.format(";%s%s%d", cc.getConstraintType(),
					cc.getComparisonOperator(),cc.getValue());

		return String.format("<%s>", labelString);
	}

	@Override
	public String convertValuePattern(ValueMentionPattern pattern) {
		StringBuilder result = new StringBuilder();

		if (pattern.getPatternReturn() instanceof LabelPatternReturn) {
			String labelString = ((LabelPatternReturn)pattern.getPatternReturn()).getLabel().toString();
			return String.format("<%s>", labelString);
		}

		result.append("Value");

		return result.toString();
	}

	@Override
	public String convertPropPattern(PropPattern pattern) {
		StringBuilder result = new StringBuilder();
		result.append(pattern.getPredicateType().toString());

		if (!pattern.getPredicates().isEmpty() || !pattern.getPredicatePrefixes().isEmpty()) {
			List<String> allPredicates = new ArrayList<String>();

			List<Symbol> predicates = new ArrayList<Symbol>(pattern.getPredicates());
			Collections.sort(predicates, new SymbolComparator());
			if (predicates.size() > 8) { //Parameterize?
				predicates = predicates.subList(0, 8);
			}
			for (Symbol s : predicates)
				allPredicates.add(s.toString());

			List<Symbol> prefixes = new ArrayList<Symbol>(pattern.getPredicatePrefixes());
			Collections.sort(prefixes, new SymbolComparator());
			for (Symbol s : prefixes)
				allPredicates.add(s.toString() + "*");

			result.append(":"+StringUtils.CommaJoin.apply(allPredicates));
		}
		if (!pattern.getAlignedPredicates().isEmpty()) {
			List<Symbol> predicates = new ArrayList<Symbol>(pattern.getAlignedPredicates());
			Collections.sort(predicates, new SymbolComparator());
			result.append(":aligned="+StringUtils.CommaJoin.apply(predicates));
		}
		List<String> args = new ArrayList<String>();
		for (Pattern p : pattern.getArgs()) {
			args.add("["+convert(p)+"]");
			//result.append("["+convert(p)+"]");
		}
		Collections.sort(args);
		for (String arg : args)
			result.append(arg);

		//if (symmetricSlots.size() > 0)
		//	return replaceSymmetricSlots(result.toString());

		return result.toString();
	}

	@Override
	public String convertRegexPattern(RegexPattern pattern) {
		StringBuilder result = new StringBuilder();
		result.append("Regex(");
		List<String> patterns = new ArrayList<String>();
		for (Pattern p : pattern.getSubpatterns()) {
			patterns.add(convert(p));
		}
		result.append(StringUtils.SpaceJoin.apply(patterns));
		result.append(")");

		//if (symmetricSlots.size() > 0)
		//	return replaceSymmetricSlots(result.toString());

		return result.toString();
	}

	/**
	 * Duplicated code from IndriPatternConverter, think of a better place to
	 * put this (TODO)
	 * @param pattern
	 * @return
	 */
	private Integer textPatternSeparationLevel(TextPattern pattern) {
		if (pattern.getText().equals("(.*)")) {
			return -1;
		} else if (pattern.getText().contains("\\S*( +\\S+)")) {
			String[] strs = pattern.getText().split("[\\,\\{\\}]");
			//reverse loop - find the last number
			for (int i = strs.length-1;i >= 0;i--) {
				try {
					int val = Integer.parseInt(strs[i])+1;
					if (val != 0) {
						return val;
					}
				} catch (Exception e) {

				}
			}
		}
		return 0;
	}

	@Override
	public String convertTextPattern(TextPattern pattern) {
		StringBuilder result = new StringBuilder();

		Integer tpSep = textPatternSeparationLevel(pattern);
		if (tpSep == -1) {
			result.append("<*>");
		} else if (tpSep != 0) {
			result.append("<");
			for (int i=0;i<tpSep;i++) result.append(".");
			result.append(">");
		} else {
			result.append(pattern.getText().replaceAll("\\\\s", " "));
		}

		return result.toString();
	}

	@Override
	public String convertUnionPattern(UnionPattern pattern) {
		List<String> patterns = new ArrayList<String>();
		for (Pattern p : pattern.getPatternList()) {
			patterns.add(convert(p));
		}
		return StringUtils.join(patterns, " or ");
	}

}
