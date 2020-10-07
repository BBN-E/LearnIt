package com.bbn.akbc.neolearnit.observations.pattern;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.restriction.Restriction;
import com.bbn.akbc.neolearnit.observations.pattern.restriction.Restriction.PropPatternBrandyRestrictor;
import com.bbn.akbc.neolearnit.observations.pattern.restriction.Restriction.RegexPatternBrandyRestrictor;
import com.bbn.akbc.neolearnit.processing.patternpruning.PatternPruningInformation;
import com.bbn.serif.patterns.IntersectionPattern;
import com.bbn.serif.patterns.Pattern;
import com.bbn.serif.patterns.PropPattern;
import com.bbn.serif.patterns.RegexPattern;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@JsonTypeInfo(use=JsonTypeInfo.Id.MINIMAL_CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public class ComboPattern extends LearnitPattern implements BrandyablePattern {

	@JsonProperty
	private final LearnitPattern pat1;
	@JsonProperty
	private final LearnitPattern pat2;
	private final boolean pat2IsRestriction;

	@JsonCreator
	public ComboPattern(@JsonProperty("pat1") LearnitPattern pat1,
						@JsonProperty("pat2") LearnitPattern pat2)
	{
		boolean p1res = pat1 instanceof Restriction, p2res = pat2 instanceof Restriction;

		if (p1res && p2res) {
			throw new RuntimeException("Cannot combine two restrictions! Need a base pattern!");
		}

		if (p1res) {
			this.pat1 = pat2;
			this.pat2 = pat1;
			pat2IsRestriction = true;
		} else if (p2res) {
			this.pat1 = pat1;
			this.pat2 = pat2;
			pat2IsRestriction = true;
		} else {
			pat2IsRestriction = false;
			if (pat1.toIDString().compareTo(pat2.toIDString()) <= 0) {
				this.pat1 = pat1;
				this.pat2 = pat2;
			} else {
				this.pat1 = pat2;
				this.pat2 = pat1;
			}
		}
	}

	public LearnitPattern getRootPattern() {
		return pat1;
	}

	public Collection<LearnitPattern> getPatterns() {
		return ImmutableList.of(pat1,pat2);
	}

	public Collection<InstanceIdentifier> getInstances(Mappings mappings) {
		if (pat2IsRestriction) {
			Restriction restriction = (Restriction)pat2;

			Collection<InstanceIdentifier> instances = new HashSet<InstanceIdentifier>();
			for (InstanceIdentifier instance : mappings.getInstancesForPattern(pat1)) {
				if (restriction.appliesTo(instance, mappings)) {
					instances.add(instance);
				}
			}

			return instances;
		} else {
			return Sets.intersection(ImmutableSet.copyOf(mappings.getInstancesForPattern(pat1)),
									 ImmutableSet.copyOf(mappings.getInstancesForPattern(pat2)));
		}
	}

	/**
	 * Returns whether or not this combination pattern has fewer total instances than either of
	 * its subpatterns. Otherwise, it's not actually reducing the space at all.
	 */
	public boolean isUseful(PatternPruningInformation info) {

        return info.getPartialInfo(this).knownInstanceCount > 0 &&
               info.getPartialInfo(this).totalInstanceCount > 0 &&
               info.getPartialInfo(this).totalInstanceCount < info.getPartialInfo(pat1).totalInstanceCount &&
               (pat2IsRestriction || info.getPartialInfo(this).totalInstanceCount < info.getPartialInfo(pat2).totalInstanceCount);
	}


	@Override
	public Pattern convertToBrandy(String factType, Target target, Iterable<Restriction> restrictions) {
		if (this.pat2IsRestriction && this.pat1 instanceof BrandyablePattern) {
			List<Restriction> newRestrictions = Lists.newArrayList(restrictions);
			newRestrictions.add((Restriction)pat2);

			Pattern result = ((BrandyablePattern)pat1).convertToBrandy(factType, target, newRestrictions);

			if (result instanceof RegexPattern && pat2 instanceof RegexPatternBrandyRestrictor) {
				result = ((RegexPatternBrandyRestrictor)pat2).restrictBrandyPattern((RegexPattern)result);
			} else if (result instanceof PropPattern && pat2 instanceof PropPatternBrandyRestrictor){
				result = ((PropPatternBrandyRestrictor)pat2).restrictBrandyPattern((PropPattern)result);
			}

			return result;

		} else if (pat1 instanceof BrandyablePattern && pat2 instanceof BrandyablePattern) {

			Pattern p1 = ((BrandyablePattern)pat1).convertToBrandy(factType, target, restrictions);
			Pattern p2 = ((BrandyablePattern)pat2).convertToBrandy(factType, target, restrictions);

			return new IntersectionPattern.Builder()
				.withPatternList(ImmutableList.of(p1, p2))
				.build();

		} else {

			throw new RuntimeException("Couldn't convert combo pattern "+this.toIDString()+" to Brandy");
		}
	}

	@Override
	public boolean isInCanonicalSymmetryOrder() {
		return pat1.isInCanonicalSymmetryOrder() && pat2.isInCanonicalSymmetryOrder();
	}

	@Override
	public String toString() {
		return toPrettyString();
	}

	@Override
	@JsonProperty
	public String toPrettyString() {
		return String.format("CombinationPattern[%s][%s]",pat1.toPrettyString(),pat2.toPrettyString());
	}

	@Override
	@JsonProperty
	public String toIDString() {
		return pat1.toIDString() + "&" + pat2.toIDString();
	}

	@Override
	public Set<Symbol> getLexicalItems() {
		return Sets.union(pat1.getLexicalItems(), pat2.getLexicalItems());
	}

	@Override
	public boolean isProposable(Target target) {
		return false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((pat1 == null) ? 0 : pat1.hashCode());
		result = prime * result + ((pat2 == null) ? 0 : pat2.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ComboPattern other = (ComboPattern) obj;
		if (pat1 == null) {
			if (other.pat1 != null)
				return false;
		} else if (!pat1.equals(other.pat1))
			return false;
		if (pat2 == null) {
			if (other.pat2 != null)
				return false;
		} else if (!pat2.equals(other.pat2))
			return false;
		return true;
	}

	@Override
	public boolean matchesPattern(LearnitPattern p) {
		// TODO Auto-generated method stub
		return false;
	}

}
