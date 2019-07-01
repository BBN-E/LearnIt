package com.bbn.akbc.neolearnit.observations.pattern.restriction;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.serif.patterns.MentionPattern;
import com.bbn.serif.patterns.PropPattern;
import com.bbn.serif.patterns.RegexPattern;
import com.bbn.serif.patterns.ValueMentionPattern;

import java.util.Collection;

public abstract class Restriction extends LearnitPattern {

	public abstract boolean appliesTo(InstanceIdentifier instance, Mappings mappings);

	@Override
	public final boolean isProposable(Target target) {
		return false;
	}

	@Override
	public boolean isCompletePattern() {
		return false;
	}


	public interface MentionPatternBrandyRestrictor {
		public MentionPattern restrictBrandyPattern(MentionPattern pattern);
	}
	public interface ValueMentionPatternBrandyRestrictor {
		public ValueMentionPattern restrictBrandyPattern(ValueMentionPattern pattern);
	}
	public interface RegexPatternBrandyRestrictor {
		public RegexPattern restrictBrandyPattern(RegexPattern pattern);
	}
	public interface PropPatternBrandyRestrictor {
		public PropPattern restrictBrandyPattern(PropPattern pattern);
	}

	public abstract static class RestrictionFactory {

		protected final TargetAndScoreTables data;
		protected final Mappings mappings;

		protected RestrictionFactory(TargetAndScoreTables data, Mappings mappings) {
			this.data = data;
			this.mappings = mappings;
		}

		public abstract Collection<Restriction> getRestrictions(LearnitPattern pattern);

	}
}
