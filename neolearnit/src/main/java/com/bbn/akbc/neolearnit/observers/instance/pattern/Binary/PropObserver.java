package com.bbn.akbc.neolearnit.observers.instance.pattern.Binary;

import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo.LanguageMatchInfo;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;
import com.bbn.akbc.neolearnit.observations.pattern.PropPattern;
import com.bbn.akbc.neolearnit.observations.pattern.PropPattern.PropArgObservation;
import com.bbn.akbc.neolearnit.observers.instance.pattern.MonolingualPatternObserver;
import com.bbn.bue.common.collections.PowerSetIterable;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.*;
import com.bbn.serif.theories.Proposition.Argument;
import com.bbn.serif.theories.Proposition.MentionArgument;
import com.bbn.serif.theories.Proposition.PropositionArgument;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.inject.BindingAnnotation;
import com.google.inject.Inject;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

public class PropObserver extends MonolingualPatternObserver {

	private final int maxDepth;

	@BindingAnnotation @Target({ PARAMETER }) @Retention(RUNTIME)
	public @interface PropDepth {}

	@Inject
	public PropObserver(InstanceToPatternMapping.Builder recorder, String language, @PropDepth int maxDepth) {
		super(recorder, language);
		this.maxDepth = maxDepth;
	}

	@Override
	public void observe(MatchInfo parent, LanguageMatchInfo match) {


		Optional<SynNode> s0Op = getNode(match.getSlot0().get(), match.getDocTheory());
		Optional<SynNode> s1Op = getNode(match.getSlot1().get(), match.getDocTheory());

		if (s0Op.isPresent() && s1Op.isPresent()) {
			SynNode s0 = s0Op.get();
			SynNode s1 = s1Op.get();
//			boolean isEventPairInstance = match.getSlot0().isPresent()&&match.getSlot1().isPresent()&&match.getSlot0().get() instanceof EventMention && match.getSlot1().get() instanceof EventMention;
			//Hard-coding the following to true
			//It may be a good idea to always treat Entities and Events the same way
			boolean isEventPairInstance = true;
			List<PropPattern> props = new ArrayList<PropPattern>(); // stores all instantiated PropPattern
			for (Proposition topProp : match.getSentTheory().propositions()) {
				collectPropObservations(topProp,s0,s1,props,match.getSentTheory(), new HashSet<Proposition>());

			}

			Set<PropPattern> unique = new HashSet<PropPattern>();
			for (PropPattern pobs : props) {
				int numNestedSlotsInMainPattern = pobs.getNestedSlots().size();
				int numArgsInMainPattern = pobs.args().size();
				for (PropPattern variant : pobs.split()) {
					if (!unique.contains(variant) &&
						(variant.depth() <= maxDepth)) {
						if (variant.hasAtLeastTwoUniqueSlots() ||
								(variant.getNestedSlots().size() == numNestedSlotsInMainPattern && variant.args().size()==numArgsInMainPattern && isEventPairInstance)) {
							this.record(parent, variant);
							unique.add(variant);
						}
					}
				}
			}
		}
	}

	// for the incoming Proposition 'p': expand out each of its argument, then go over all possible combination of the expansions
	// - for each of its Argument, invoke getArgInfo to collect a set of PropArgObservation
	//   so for example if p has 2 Arguments: [ [A,B] , [1,2] ]
	// - now I will have 4 possible combinations
	public Set<PropPattern> collectPropObservations(Proposition p, SynNode s1, SynNode s2,
			List<PropPattern> props, SentenceTheory sent, Set<Proposition> history) {
        ImmutableSet.Builder<PropPattern> propsBuilder = ImmutableSet.builder();
	    history.add(p);

	    // PowerSetIterabler is actually just Cartesian Product. Internally, it is a List of List, e.g.: [ [A,B], [1,2] ]
	    // And then the powerset is : A,1  B,1  A,2  B,2
        PowerSetIterable.Builder<PropArgObservation> argArrangementsBuilder = new PowerSetIterable.Builder<PropArgObservation>();

		for (Argument a : p.args()) {
			// for each argument of this proposition, collect ArgInfo and then commit as a List in PowerSetIterable
            Collection<PropArgObservation> args = getArgInfo(a,s1,s2,props,sent,history);
            for (PropArgObservation arg : args)
                argArrangementsBuilder.withChoiceAdd(arg);
            if (args.size() > 0)
                argArrangementsBuilder.withCommitChoiceSet();
		}

        for (Iterable<PropArgObservation> args : argArrangementsBuilder.build()) {	// for each combination
            PropPattern.Builder propBuilder = new PropPattern.Builder(language, p.predType());
            if (p.predSymbol().isPresent())
                propBuilder.withPredicate(p.predSymbol().get());
            for (PropArgObservation arg : args)
                propBuilder.withArg(arg);
            PropPattern prop = propBuilder.build();
            if (prop.hasAtLeastTwoUniqueSlots()) {
				props.add(prop);
//	    }//TODO: check if we want atLeastTwoUnique for exactlyTwoUnique for this conditional statement
//            if (prop.hasExactlyTwoUniqueSlots()) {
//                props.add(prop);
            }else if(prop.getNestedSlots().size()==1&&prop.args().size()==1){
				//if s0 and s1 are anchorNodes of EventMentions, if one of the synNodes is an argument, the other one should be the predicate of the parent proposition
				int slotNo = prop.getNestedSlots().get(0);
				Optional<SynNode> predicateNode = p.predHead();
				SynNode synNodeToMatchToPredicate = slotNo==0?s2:s1;
				if(predicateNode.isPresent()&&predicateNode.get().span().equals(synNodeToMatchToPredicate.span())){
					props.add(prop);
				}
			}
            propsBuilder.add(prop);
        }

		return propsBuilder.build();
	}

	public Set<PropArgObservation> getArgInfo(Argument a, SynNode s0, SynNode s1,
			List<PropPattern> props, SentenceTheory sent, Set<Proposition> history) {
        ImmutableSet.Builder<PropArgObservation> argsBuilder = ImmutableSet.builder();

        final Symbol role = a.role().isPresent()? a.role().get() : Symbol.from("UNKNOWN");


		if (a instanceof Proposition.MentionArgument) {
			Proposition.MentionArgument ma = (Proposition.MentionArgument)a;

            SynNode mention = ma.mention().node();
            while (mention != null) {
                if (mention.equals(s0)) {
                    argsBuilder.add(new PropArgObservation(role, 0));
                }
                else if (mention.equals(s1)) {
                    argsBuilder.add(new PropArgObservation(role, 1));
                }
                mention = mention.head().equals(mention) ? null : mention.head();
            }
		}
		else if (a instanceof Proposition.TextArgument) {
			Proposition.TextArgument ta = (Proposition.TextArgument)a;

			if (ta.span().equals(s0.span())) {
                argsBuilder.add(new PropArgObservation(role, 0));
			}
			else if (ta.span().equals(s1.span())) {
                argsBuilder.add(new PropArgObservation(role, 1));
			}else if (ta.node().highestHead().head().equals(s0.span())) {
				argsBuilder.add(new PropArgObservation(role, 0));
			}else if(ta.node().highestHead().head().equals(s1.span())){
				argsBuilder.add(new PropArgObservation(role, 1));
			}
		}else if(a instanceof PropositionArgument){
			PropositionArgument pa = (PropositionArgument)a;
			//if s0 and s1 are anchorNodes of EventMentions, we may just want to match the predicate of the Proposition with them
			Optional<SynNode> paPredHead = pa.proposition().predHead();
			if (paPredHead.isPresent() && paPredHead.get().span().equals(s0.span())) {
				argsBuilder.add(new PropArgObservation(role, 0));
			}else if(paPredHead.isPresent() && paPredHead.get().span().equals(s1.span())){
				argsBuilder.add(new PropArgObservation(role, 1));
			}else{
				Optional<Proposition> opP = getSubprop(a, sent);
				if (opP.isPresent() && !history.contains(opP.get())) {
					for (PropPattern prop : collectPropObservations(opP.get(),s0,s1,props,sent,history)) {
						argsBuilder.add(new PropArgObservation(role, prop));
					}
				}
			}
		}


		return argsBuilder.build();
	}

	private static Optional<Proposition> getSubprop(final Argument a, final SentenceTheory st) {
		if (a instanceof PropositionArgument) {
			final PropositionArgument pa = (PropositionArgument)a;
			return Optional.of(pa.proposition());
		}
		else if (a instanceof MentionArgument) {
			final MentionArgument ma = (MentionArgument)a;
			Optional<Mention> cur = Optional.of(ma.mention());
			//get bottom-most child and work up
			while (cur.isPresent() && cur.get().child().isPresent()) {
				cur = cur.get().child();
			}
			//work up, but not above original mention
			while (cur.isPresent() && !cur.get().equals(ma.mention().parent().orNull())) {
				final Optional<Proposition> opProp = st.propositions().definition(cur.get());
				if (a.role().isPresent() && opProp.isPresent()) {
					return opProp;
				}
				cur = cur.get().parent();
			}
		}

		return Optional.absent();
	}

	// Given a Proposition 'p', include 'p', and recursively include all children proposition of p
	private static void getPropAndChildren(final SentenceTheory st, final Proposition p, Set<Proposition> result) {
		result.add(p);

		for (final Argument a : p.args()) {
			final Optional<Proposition> opP = getSubprop(a,st);
			if (opP.isPresent() && !result.contains(opP.get())) {
				getPropAndChildren(st,opP.get(),result);
			}
		}
	}

	// find all top-level proposition in sentence : props having no children prop
	public static Collection<Proposition> getTopLevelProps(final SentenceTheory sentence) {
		final ImmutableSet.Builder<Proposition> ret = ImmutableSet.builder();
		Set<Proposition> children = new HashSet<Proposition>();

		for(final Proposition p : sentence.propositions()) {
			Set<Proposition> newSet = new HashSet<Proposition>();
			getPropAndChildren(sentence, p, newSet);

			for(final Proposition subP : newSet) {
				if (!subP.equals(p)) {
					children.add(subP);
				}
			}
		}

		for(final Proposition p : sentence.propositions()) {
			if (!children.contains(p)) {
				ret.add(p);
			}
		}

		return ret.build();
	}

	// the input 'span' must be either a Mention, ValueMention, or SynNode
	public static Optional<SynNode> getNode(Spanning span, DocTheory dt) {
		if (span instanceof Mention) {
			return Optional.of(((Mention)span).node());
		} else if (span instanceof ValueMention) {
			return ValueMention.node(dt, ((ValueMention)span));
		} else if (span instanceof SynNode) {
			return Optional.of(((SynNode) span));
		} else if (span instanceof EventMention) {
			return Optional.of(((EventMention) span).anchorNode());
		} else {
			throw new RuntimeException("Unhandled span type: "+span);
		}
	}
}
