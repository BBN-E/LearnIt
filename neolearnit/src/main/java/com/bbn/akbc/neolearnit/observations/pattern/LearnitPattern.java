package com.bbn.akbc.neolearnit.observations.pattern;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.akbc.neolearnit.common.resources.StopWords;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.observations.LearnItObservation;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown=true)
public abstract class LearnitPattern extends LearnItObservation {

	@Override
	public abstract String toPrettyString();

	/**
	 * Used for identification purposes in server code, must align to javascript pattern objects
	 * found in static/scripts/objects/Pattern.js
	 * @return
	 */
	public abstract String toIDString();

	public abstract boolean isInCanonicalSymmetryOrder();

	public abstract Set<Symbol> getLexicalItems();

    public Set<Symbol> getLexicalItemsWithContent() {
        StopWords stopwords = StopWords.getDefault();
        Set<Symbol> lexItems = this.getLexicalItems();
        ImmutableSet.Builder<Symbol> content = ImmutableSet.builder();
        for (Symbol s : lexItems) {
            if (!stopwords.isStopWord(Symbol.from(s.toString().toLowerCase()))) content.add(s);
        }
        return content.build();
    }

	/**
	 * Should this type of pattern be proposed outright from seeds?
	 * @return
	 */
	public boolean isProposable(Target target) {
		return true;
	}

	/**
	 * Is this a full-fledged pattern on its own, or not?
	 * @return
	 */
	public boolean isCompletePattern() {
		return true;
	}

	/**
	 * If applicable, return a modified version of the pattern for use during initialization,
	 * or return absent() if this pattern isn't suited to initialization.
	 * @return
	 */
	public Optional<? extends LearnitPattern> getInitializationVersion() {
		return Optional.of(this);
	}

    public Set<? extends LearnitPattern> getLexicallyExpandedVersions() {
        return ImmutableSet.of();
    }

    public abstract boolean matchesPattern(LearnitPattern p);
}
