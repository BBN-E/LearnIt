package com.bbn.akbc.neolearnit.observations.pattern;

import com.bbn.akbc.neolearnit.common.Clusters;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.observations.pattern.restriction.Restriction;
import com.bbn.bue.common.StringUtils;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.bue.sexp.Sexp;
import com.bbn.serif.patterns.ArgumentPattern;
import com.bbn.serif.patterns.LabelPatternReturn;
import com.bbn.serif.patterns.Pattern;
import com.bbn.serif.patterns.PatternReturn;
import com.bbn.serif.theories.Proposition;
import com.bbn.serif.theories.Proposition.PredicateType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

import java.util.*;

@JsonTypeInfo(use=JsonTypeInfo.Id.MINIMAL_CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public class PropPattern extends MonolingualPattern implements BrandyablePattern {
	private final PredicateType predType;
	private final ImmutableSet<Symbol> predicates;
	private final ImmutableList<PropArgObservation> args;

	private PropPattern(final String language, final PredicateType predType, final Set<Symbol> predicates, final Iterable<PropArgObservation> args) {
		super(language);
		this.predType = predType;
		this.predicates = ImmutableSet.copyOf(predicates);
		this.args = ImmutableList.copyOf(args);
	}

	@JsonProperty
	public String predicateType() {
		return predType.toString();
	}

	@JsonProperty
	public Set<Symbol> predicates() {
		return new HashSet<Symbol>(predicates);
	}

	@JsonProperty
	public List<PropArgObservation> args() {
		return new ArrayList<PropArgObservation>(args);
	}

	@JsonCreator
	private static PropPattern from(
			@JsonProperty("language") String language,
			@JsonProperty("predicateType") String predicateType,
			@JsonProperty("predicates") Set<Symbol> predicates,
			@JsonProperty("args") List<PropArgObservation> args) {
		final ImmutableList<PropArgObservation> orderedArgs = Ordering.natural().onResultOf(PropArgObservation.ByRole).immutableSortedCopy( args );
		return new PropPattern(language, PredicateType.from(Symbol.from(predicateType)), predicates, orderedArgs);
	}

	public static PropPattern from(com.bbn.serif.patterns.PropPattern brandyPropPattern) {
		ArrayList<PropArgObservation> learnitArgs = new ArrayList<>();
		for (ArgumentPattern argPattern : brandyPropPattern.getArgs()) {
			learnitArgs.add(PropArgObservation.from(argPattern));
		}
		return from("en",
				brandyPropPattern.getPredicateType().toString(),
				brandyPropPattern.getPredicates(),
				learnitArgs);
	}

	public ImmutableSet<Symbol> getPredicates() {
		return predicates;
	}

	public ImmutableList<PropArgObservation> getArgs() {
		return args;
	}

	public PredicateType getPredicateType() {
		return predType;
	}

	@Override
	public Pattern convertToBrandy(String factType, Target target, Iterable<Restriction> restrictions) {

		com.bbn.serif.patterns.PropPattern.Builder builder = new com.bbn.serif.patterns.PropPattern.Builder(predType);
		builder.withPredicates(predicates);

		List<ArgumentPattern> args = new ArrayList<ArgumentPattern>();
		for (PropArgObservation a : this.args) {
			args.add(a.makeArgBrandyPattern(factType, target, restrictions));
		}
		builder.withArgs(args);

		return builder.build();
	}

	public boolean hasExactlyOneUniqueSlot(){
		List<Integer> slotList = getNestedSlots();
		Set<Integer> slotSet = new HashSet<>(slotList);
		return (slotList.size() == slotSet.size() && slotSet.size() == 1);
	}

	public boolean hasExactlyTwoUniqueSlots() {
		List<Integer> slotList = getNestedSlots();
		Set<Integer> slotSet = new HashSet<Integer>(slotList);
		// non redundant and == 2
		return (slotList.size() == slotSet.size() && slotSet.size() == 2);
	}
	public boolean hasAtLeastTwoUniqueSlots() {
		List<Integer> slotList = getNestedSlots();
		Set<Integer> slotSet = new HashSet<Integer>(slotList);
		// non redundant and == 2
		return (slotList.size() == slotSet.size() && slotSet.size() >= 2);
	}

    public Set<Set<Symbol>> getAllPredicates(){
        Set<Set<Symbol>> ret = new HashSet<>();
        ret.add(this.getPredicates());
        for(PropArgObservation propArgObservation: this.args){
            ret.addAll(propArgObservation.getAllPredicates());
        }
        return ret;
    }

	public boolean exactPredicatesEqual(Set<Symbol> predicates){
	    Set<Set<Symbol>> allPredicates = this.getAllPredicates();
	    for(Set<Symbol> collectedpredicates : allPredicates){
	        if(predicates.equals(collectedpredicates)){
	            return true;
            }
        }
        return false;
    }

	// checks:
	// 1. This proposition has exactly 2 slots
	// 2. When you sort the slots (args) by proposition role, the first arg is slot0, the second arg is slot1
	@Override
	public boolean isInCanonicalSymmetryOrder() {
		final ImmutableList<PropArgObservation> args = getArgsHavingSlot();
		final ImmutableList<PropArgObservation> orderedArgs = Ordering.natural().onResultOf(PropArgObservation.ByRole).immutableSortedCopy( args );

		if (orderedArgs.size() != 2) throw new RuntimeException("This proposition does not have exactly 2 slots");

		return (orderedArgs.get(0).getNestedSlots().get(0) == 0) && (orderedArgs.get(1).getNestedSlots().get(0) == 1);
	}

    @Override
    public boolean isProposable(Target target) {
        return this.predType != PredicateType.SET || target.allowEmptySets() || !this.getLexicalItemsWithContent().isEmpty();
    }

    // depth 1 : none of the args is itself a Proposition
	@Override
	public Optional<? extends LearnitPattern> getInitializationVersion() {
		if (LearnItConfig.optionalParamTrue("only_depth_1_props_at_initialization") && this.depth() > 1) {
			return Optional.absent();
		} else {
			return Optional.of(this);
		}
	}

    @Override
    public Set<? extends LearnitPattern> getLexicallyExpandedVersions() {
        if (predType != PredicateType.NOUN)
            return ImmutableSet.of();
        ImmutableSet.Builder<PropPattern> builder = ImmutableSet.builder();
        for (Symbol pred : predicates) {
            List<Symbol> clusters = Clusters.getGoodClusters(pred);
            for (Symbol cluster : clusters) {
                for (Symbol word : Clusters.getWords(cluster)) {
                    if (word != pred)
                        builder.add(new PropPattern(language, predType, ImmutableSet.of(word), args));
                }
            }
        }
        return builder.build();
    }

    // construct all possible PropPattern by splitting each arg and then cartesian product them
	public Collection<PropPattern> split() {
		final ImmutableSet.Builder<PropPattern> ret = ImmutableSet.builder();

		List<Set<PropArgObservation>> choices = new ArrayList<Set<PropArgObservation>>();

		// split each arg into a Set<PropArgObservation>. Thus we have a list of Set
		for (PropArgObservation arg : args) {
			if (arg.hasNestedSlot())
				choices.add(arg.split());
		}

		// form all possible PropPattern by performing CartesianProduct
		for (List<PropArgObservation> argSet : Sets.cartesianProduct(choices)) {
			ret.add(new PropPattern(language, predType, predicates, argSet));
		}

		return ret.build();
	}

	public int depth() {
		int maxDepth = 0;
		for (PropArgObservation arg : args) {
			int argDepth = arg.depth();
			if (argDepth > maxDepth) {
				maxDepth = argDepth;
			}
		}
		return maxDepth+1;
	}

	// recursively get: (a) all predicates, (b) all roles not enclosed in <...>
	@Override
	public Set<Symbol> getLexicalItems() {
		final ImmutableSet.Builder<Symbol> ret = ImmutableSet.builder();
		// @hqiu Took this off for not interfering overlap in search
//		ret.add(Symbol.from(this.predType.name().asString()));
		ret.addAll(this.getPredicates());
		for (final PropArgObservation arg : this.getArgs()) {
			ret.addAll(arg.getLexicalItems());
		}

		return ret.build();
	}

	public boolean hasNestedSlot() {
		return getNestedSlots().size() > 0;
	}

	// recursively total up the number of slots in all PropArgObservation args
	public List<Integer> getNestedSlots() {
		final ImmutableList.Builder<Integer> ret = ImmutableList.builder();

		for (PropArgObservation arg : args) {
			ret.addAll(arg.getNestedSlots());
		}


		return ret.build();
	}

	// get all args having slot info
	private ImmutableList<PropArgObservation> getArgsHavingSlot() {
		final ImmutableList.Builder<PropArgObservation> ret = ImmutableList.builder();

		for(final PropArgObservation arg : getArgs()) {
			ret.addAll(arg.getArgsHavingSlot());
		}

		return ret.build();
	}

	public static class Builder {
		private final String language;
		private final PredicateType predType;
		private final Set<Symbol> predicates;
		private final List<PropArgObservation> args;

		public Builder(String language, PredicateType predType) {
			this.language = language;
			this.predType = predType;
			this.predicates = new HashSet<Symbol>();
			this.args = new ArrayList<PropArgObservation>();
		}

		public Builder withPredicate(Symbol pred) {
			predicates.add(Symbol.from(pred.toString().toLowerCase()));
			return this;
		}

		public Builder withPredicates(Collection<Symbol> preds) {
			for (Symbol pred : preds)
				predicates.add(Symbol.from(pred.toString().toLowerCase()));
			return this;
		}

		public Builder withArg(PropArgObservation arg) {
			args.add(arg);
			return this;
		}

		public PropPattern build() {
			final ImmutableList<PropArgObservation> orderedArgs = Ordering.natural().onResultOf(PropArgObservation.ByRole).immutableSortedCopy( args );
			return new PropPattern(language, predType, predicates, orderedArgs);
		}

	}

	// TODO : why am I doing a fuzzy match on the predicates?
	public boolean matchesPattern(final LearnitPattern p) {
		if (!(p instanceof PropPattern))
			return false;

		final PropPattern prop = (PropPattern) p;

		//if (!prop.getPredicateType().toString().startsWith(this.getPredicates().toString()))
		//    return false;
		// TODO : I believe the above is a bug, hence I replaced with the following
		if (!prop.getPredicateType().toString().equals(this.getPredicateType().toString())) {
			return false;
		}

		for (Symbol predicate : prop.getPredicates()) {
			if (!predicates.contains(predicate) && !hasOverlappingWildcardPred(predicate))
				return false;
		}

		//Try to match up the args. If symmetric, try reversing the args and matching
		// TODO : there is no checking of symmetric yet. Is this necessary?
		return matchesArgs(prop.getArgs(), this.getArgs());
	}

	// if the incoming 'predicate' starts with any of my predicates
	private boolean hasOverlappingWildcardPred(final Symbol predicate) {
		for (Symbol wildcardPred : this.getPredicates()) {
			if (predicate.toString().startsWith(wildcardPred.toString()))
				return true;
		}
		return false;
	}

	private boolean matchesArgs(final List<PropPattern.PropArgObservation> args1, final List<PropPattern.PropArgObservation> args2) {
		final ImmutableSet<PropArgObservation> set1 = ImmutableSet.copyOf(args1);
		final ImmutableSet<PropArgObservation> set2 = ImmutableSet.copyOf(args2);

		return (set1.size() == set2.size()) && set1.containsAll(set2);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		PropPattern other = (PropPattern) obj;
		if (args == null) {
			if (other.args != null)
				return false;
		} else if (!args.equals(other.args))
			return false;
		if (predType == null) {
			if (other.predType != null)
				return false;
		} else if (!predType.equals(other.predType))
			return false;
		if (predicates == null) {
			return other.predicates == null;
		} else return predicates.equals(other.predicates);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result;
		result = ((args == null) ? 0 : args.hashCode());
		result = prime * result
				+ ((predType == null) ? 0 : predType.hashCode());
		result = prime * result
				+ ((predicates == null) ? 0 : predicates.hashCode());
		return result;
	}

	public static class PropArgObservation {
		private final Symbol role;
		private final Optional<PropPattern> prop;	// is present when this arg is itself a Proposition
		private final Optional<Integer> slot; 		// slot 0 or slot 1

		private Symbol roleWithoutUnknown(final Symbol role) {
			if (role == Symbol.from("<unknown>"))
				return Symbol.from("<mod>");
			return role;
		}

		public PropArgObservation(final Symbol role) {
			this.role = roleWithoutUnknown(role);
			this.prop = Optional.absent();
			this.slot = Optional.absent();
		}

		public PropArgObservation(final Symbol role, final PropPattern prop) {
			this.role = roleWithoutUnknown(role);
			this.prop = Optional.of(prop);
			this.slot = Optional.absent();
		}

		public PropArgObservation(final Symbol role, final Integer slot) {
			this.role = roleWithoutUnknown(role);
			this.slot = Optional.of(slot);
			this.prop = Optional.absent();
		}

		public PropArgObservation(final Symbol role, final Integer slot, final PropPattern prop) {
			this.role = roleWithoutUnknown(role);
			this.slot = Optional.fromNullable(slot);
			this.prop = Optional.fromNullable(prop);
		}

		public PropArgObservation copyWithSlot(final Integer slot) {
			return new PropArgObservation(this.role, slot, this.prop.orNull());
		}

		@JsonProperty
		public Symbol role() {
			return role;
		}

		@JsonProperty
		public PropPattern prop() {
			return prop.orNull();
		}

		@JsonProperty
		public Integer slot() {
			return slot.orNull();
		}

		@JsonCreator
		private static PropArgObservation from(
				@JsonProperty("role") Symbol role,
				@JsonProperty("slot") Integer slot,
				@JsonProperty("prop") PropPattern prop) {
			return new PropArgObservation(role, slot, prop);
		}

		private static PropArgObservation from(ArgumentPattern argumentPattern) {
			if (argumentPattern.getRoles().size() != 1) {
				throw new NonConvertibleException(
						"Brandy ArgumentPatterns being converted into LearnIt PropArgObservations must have exactly one role");
			}
			Pattern brandyPattern = argumentPattern.getPattern();
			PropPattern propPattern;
			if (brandyPattern == null) {
				// This is just an empty argument pattern which does translate well
				// into PropArgObservation
				propPattern = null;
			} else if (brandyPattern instanceof com.bbn.serif.patterns.PropPattern) {
				// Create LearnIt prop pattern from Brandy PropPattern
				propPattern = PropPattern.from(
						(com.bbn.serif.patterns.PropPattern)brandyPattern);
			} else {
				throw new NonConvertibleException(
						"Brandy ArgumentPatterns being converted into LearnIt PropArgObservations must point to only Brandy PropPatterns");
			}

			Integer slot = null;
			PatternReturn patternReturn = argumentPattern.getPatternReturn();
			if (patternReturn != null && !(patternReturn instanceof LabelPatternReturn)) {
				throw new NonConvertibleException(
						"Brandy ArgumentPatterns being converted into LearnIt PropArgObservations must only have simple return values e.g. (return slot1)");
			}
			if (patternReturn != null) {
				LabelPatternReturn lpr = (LabelPatternReturn) patternReturn;
				Symbol label = lpr.getLabel();
				if (label.equalTo(Symbol.from("slot1")))
					slot = 1;
				else if (label.equalTo(Symbol.from("slot2")))
					slot = 2;
				else
					throw new NonConvertibleException(
							"Brandy ArgumentPatterns being converted into LearnIt PropArgObservations must only have return labels 'slot1' or 'slot2'");
			}

			return from(argumentPattern.getRoles().get(0), slot, propPattern);
		}

		public Optional<PropPattern> getProp() {
			return prop;
		}

		public Optional<Integer> getSlot() {
			return slot;
		}

		public Symbol getRole() {
			return role;
		}

		public ArgumentPattern makeArgBrandyPattern(String factType, Target target, Iterable<Restriction> restrictions) {
			ArgumentPattern.Builder builder = new ArgumentPattern.Builder();

			builder.withRoles(ImmutableList.of(role));
			if (slot.isPresent()) {
				builder.withPattern(target.makeSlotBrandyPattern(factType, slot.get(), restrictions));

			} else {
				builder.withPattern(prop.get().convertToBrandy(factType, target, restrictions));
			}

			return builder.build();
		}

		public int depth() {
			if (prop.isPresent()) {
				return prop.get().depth();
			} else {
				return 0;
			}
		}

		public Set<PropArgObservation> split() {
			final ImmutableSet.Builder<PropArgObservation> ret = ImmutableSet.builder();

			if (slot.isPresent()) {
				ret.add( new PropArgObservation(role, slot.get()));	// one with slot info
				ret.add( new PropArgObservation(role));				// one without slot info
			}

			if (prop.isPresent() && prop.get().hasNestedSlot()) {
				for (PropPattern po : prop.get().split()) {
					ret.add( new PropArgObservation(role, po));
				}

				//fall through sets
				if (prop.get().getPredicateType().equals(Proposition.PredicateType.SET)) {
					for (PropArgObservation arg : prop.get().args) {
						if (arg.hasNestedSlot()) {
							ret.addAll(arg.split());
						}
					}
				}
			}

			return ret.build();
		}

		public boolean hasNestedSlot() {
			return getNestedSlots().size() > 0;
		}

		// if this PropArgObservation has a slot value, include that
		// if this PropArgObservation is itself a Proposition,
		// then recursively include the slot values of the Proposition's PropArgObservation args
		public List<Integer> getNestedSlots() {
			List<Integer> result = new ArrayList<Integer>();
			if (slot.isPresent()) result.add(slot.get());
			if (prop.isPresent()) result.addAll(prop.get().getNestedSlots());
			return result;
		}

        public Set<Set<Symbol>> getAllPredicates(){
            Set<Set<Symbol>> ret = new HashSet<>();
            if(prop.isPresent()){
                ret.addAll(prop.get().getAllPredicates());
            }
            return ret;
        }

		public ImmutableList<PropArgObservation> getArgsHavingSlot() {
			final ImmutableList.Builder<PropArgObservation> ret = ImmutableList.builder();

			if(slot.isPresent()) {
				ret.add(this);
			}

			if(prop.isPresent()) {
				for(final PropArgObservation arg : prop.get().getArgs()) {
					ret.addAll(arg.getArgsHavingSlot());
				}
			}

			return ret.build();
		}

        // for a PropArgObservation, its LexicalItems include:
        // - its own role, if the role is not enclosed in < .. >
        // if this arg is itself a Proposition, then we also include the lexical items of the Proposition, which includes:
        // - the Predicates of the Proposition
        // - roles of the Proposition's args which are not enclosed in < .. >
		public Set<Symbol> getLexicalItems() {
			Set<Symbol> result = new HashSet<>();
			String roleString = this.getRole().asString();
			for(String token :roleString.split(" ")){
				if (!token.startsWith("<") && !token.equals("trigger")) {
					result.add(Symbol.from(token));
				}
			}
			if (this.getProp().isPresent()) {
				result.addAll(this.getProp().get().getLexicalItems());
			}
			return result;
		}

		public static final Function<PropArgObservation, String> ByRole = new Function<PropArgObservation, String>() {
    		@Override
    		public String apply(final PropArgObservation arg) {
    			return arg.getRole().toString();
    		}
    	};

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((prop == null) ? 0 : prop.hashCode());
			result = prime * result + ((role == null) ? 0 : role.hashCode());
			result = prime * result + ((slot == null) ? 0 : slot.hashCode());
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
			PropArgObservation other = (PropArgObservation) obj;
			if (prop == null) {
				if (other.prop != null)
					return false;
			} else if (!prop.equals(other.prop))
				return false;
			if (role == null) {
				if (other.role != null)
					return false;
			} else if (!role.equals(other.role))
				return false;
			if (slot == null) {
				return other.slot == null;
			} else return slot.equals(other.slot);
		}

		@Override
		public String toString() {
			return "PropArgObservation [role=" + role + ", prop=[" + prop
					+ "], slot=" + slot + "]";
		}

		public String toPrettyString(int indent) {
			StringBuilder builder = new StringBuilder();
			for (int i=0;i<indent;i++) builder.append("   ");
			builder.append(role.toString()+" = ");
			if (slot.isPresent()) {
				builder.append(slot.get());
				builder.append(" ");
			}
			if (prop.isPresent()) {
				builder.append(prop.get().toPrettyString(indent));
			}

			return builder.toString();
		}


		public List<String> findArg(int iSlot) {
			List<String> path = new ArrayList<String>();
			if(slot.isPresent()) {
				if(slot.get()==iSlot) {
					path.add(this.role.toString());
				}
			}
			else {
				if(!prop.get().findArg(iSlot).isEmpty()) {
					path.add(this.role.toString());
					path.addAll(prop.get().findArg(iSlot));
				}
			}
			return path;
		}

		public String toIDString() {
			if (slot.isPresent()) {
				return this.role.toString()+" = "+slot.get();
			} else if(prop.isPresent()){
				return this.role.toString()+" = "+prop.get().toIDString();
			}
			else{
			    return this.role.toString();
            }
		}
	}	// end of PropArgObservation class

	@Override
	public String toString() {
		return "PropObservation [language=" + language + " predType=" + predType + ", predicates="
				+ predicates + ", args=[" + args + "]]";
	}

	@Override
	@JsonProperty
	public String toPrettyString() {
		return toPrettyString(0);
	}

	@Override
	@JsonProperty
	public String toIDString() {
		String result = predType+":"+StringUtils.CommaJoin.apply(predicates);
		for (PropArgObservation arg : this.args) {
			result += "["+arg.toIDString()+"]";
		}
		return result;
	}

	public List<String> findArg(int slot) {
		List<String> path = new ArrayList<String>();
		for (PropArgObservation arg : this.args) {
			if(!arg.findArg(slot).isEmpty()) {
				path.add(StringUtils.CommaJoin.apply(predicates));
				path.addAll(arg.findArg(slot));
			}
			else
				continue;
		}
		return path;
	}

	public String toDepString() {
		List<String> pathToArg0 = findArg(0);
		List<String> pathToArg1 = findArg(1);

		if(pathToArg0.isEmpty() || pathToArg1.isEmpty())
			return "";

		StringBuilder stringBuilder = new StringBuilder();
		for(int i=pathToArg0.size()-1; i>=0; i--)
			stringBuilder.append(pathToArg0.get(i) + " ");
		for(int i=1; i<pathToArg1.size(); i++)
			stringBuilder.append(pathToArg1.get(i) + " ");

		return stringBuilder.toString().trim();
	}

/*
	List<String> pathToLeft = new ArrayList<String>();
	List<String> nodesToRight = new ArrayList<String>();

	public boolean findLeftPath(List<>) {

	}
	}


	public String toDepString(List<String> inputPath) {

		List<List<String>> listPaths = new ArrayList<List<String>>();

		for (PropArgObservation arg : this.args) {
			List<String> path = new ArrayList<String>(inputPath);

			if(arg.getSlot().isPresent()) {

				path.add(this.role.toString());
				path.add(slot.get());
			}
			else {path.add(this.role.toString());
				path.add(prop.get().toIDString();
		}





		PropArgObservation leftArg = this.args().get(0);
		PropArgObservation rightArg = this.args().get(1);

		if(leftArg.getSlot().isPresent()) {
			String leftArgumentString =
					leftArg.getSlot() + "|" + leftArg.getRole().toString();
		} else {
			String leftArgumentString =
					leftArg.getSlot() + "|" + leftArg.getRole().toString();
		}

		String leftArgumentString = leftArg.toDepString();
		String rightArgumntString = rightArg.toDepString();
		return leftArgumentString + "|" + predType+":"+StringUtils.CommaJoin.apply(predicates) + "|" + rightArgumntString;
	}

	if (slot.isPresent()) {
		return this.role.toString()+" = "+slot.get();
	} else {
		return this.role.toString()+" = "+prop.get().toIDString();
	}
	*/

	public String toPrettyString(int indent) {
		StringBuilder builder = new StringBuilder();
		builder.append(predType+": "+predicates);
		for (PropArgObservation arg : args) {
			builder.append("\n");
			builder.append(arg.toPrettyString(indent+1));

		}
		return builder.toString();
	}
}
