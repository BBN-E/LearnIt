package com.bbn.akbc.neolearnit.observations.evaluation;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo.LanguageMatchInfo;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.evaluation.answers.AnswerCollection;
import com.bbn.akbc.neolearnit.evaluation.answers.MentionAnswer;
import com.bbn.akbc.neolearnit.evaluation.answers.RelationAnswer;
import com.bbn.akbc.neolearnit.observers.instance.evaluation.SentenceEntityPairAnswerObserver;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.Entity;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.Spanning;
import com.bbn.serif.theories.ValueMention;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Slightly deceptive name because this also can handle values as arguments
 * @author mshafir
 *
 */
public class SentenceEntityPairAnswer extends EvalAnswer {

	@JsonProperty
	private final Target target;
	@JsonProperty
	private final String docid;
	@JsonProperty
	private final int sentid;
	@JsonProperty
	private final String arg0EntityId;
	@JsonProperty
	private final String arg1EntityId;

	@JsonCreator
	public SentenceEntityPairAnswer(
			@JsonProperty("target") Target target,
			@JsonProperty("docid") String docid,
			@JsonProperty("sentid") int sentid,
			@JsonProperty("arg0EntityId") String arg0EntityId,
			@JsonProperty("arg1EntityId") String arg1EntityId,
			@JsonProperty("matchedAnnotations") List<RelationAnswer> matchedAnnotations) {
		super(matchedAnnotations);
		this.target = target;
		this.docid = docid;
		this.sentid = sentid;
		this.arg0EntityId = arg0EntityId;
		this.arg1EntityId = arg1EntityId;
	}

	public SentenceEntityPairAnswer(Target target,
			String docid,
			int sentid,
			String arg0EntityId,
			String arg1EntityId,
			Set<String> brandyMatches,
			List<RelationAnswer> matchedAnnotations) {
		super(matchedAnnotations, brandyMatches);
		this.target = target;
		this.docid = docid;
		this.sentid = sentid;
		this.arg0EntityId = arg0EntityId;
		this.arg1EntityId = arg1EntityId;
	}

	public String getDocId() {
		return docid;
	}

	protected static String getSlotId(AnswerCollection ansCollection, DocTheory dt, SentenceTheory st, Spanning slot) {

		if (slot instanceof Mention) {
			Mention m = (Mention)slot;

			if (LearnItConfig.optionalParamTrue("use_gold_coreference")) {

				int fuzzyAmount = LearnItConfig.getInt("partial_match_allowance");

				String result = "";

				for (MentionAnswer ma : ansCollection.getCoreferences().values()) {

					if (ma.getDocid().equals(dt.docid().toString().replace(".segment", ""))) {

						if (SentenceEntityPairAnswerObserver.mentionSentenceMatch(ma, st, slot, fuzzyAmount)) {

							if (!result.equals("")) result += " & ";
							result += ma.getEntity().getEntityId();
						}
					}
				}

				if (result.equals("")) {
					return "m"+st.mentions().asList().indexOf(m);
				} else {
					return result;
				}

			} else {

				Optional<Entity> opE = m.entity(dt);
				if (opE.isPresent()) {
					return "e"+dt.entities().asList().indexOf(opE.get());
				} else {
					return "m"+st.mentions().asList().indexOf(m);
				}
			}
		} else if (slot instanceof ValueMention) {
			ValueMention vm = (ValueMention)slot;
			return "v"+st.valueMentions().asList().indexOf(vm);
		} else {
			throw new RuntimeException("Unrecognized slot type for "+slot);
		}
	}


	public static SentenceEntityPairAnswer fromJudgementAndMatchInfo(
			AnswerCollection ansCollection, List<RelationAnswer> corrects, MatchInfo match) {

		LanguageMatchInfo lmi = match.getPrimaryLanguageMatch();

		return new SentenceEntityPairAnswer(match.getTarget(),
				lmi.getDocTheory().docid().toString(),
				lmi.getSentTheory().index(),
				getSlotId(ansCollection, lmi.getDocTheory(), lmi.getSentTheory(), lmi.getSlot0().get()),
				getSlotId(ansCollection, lmi.getDocTheory(), lmi.getSentTheory(), lmi.getSlot1().get()),
				new HashSet<String>(), corrects);
	}



	@Override
	public String toPrettyString() {
		return this.toString();
	}

	@Override
	public String toString() {
		List<String> args = Lists.newArrayList(arg0EntityId, arg1EntityId);
		if (target.isSymmetric()) {
			Collections.sort(args);
		}
		String correct = this.isCorrect() ? "CORRECT" : "INCORRECT";
		return "SentenceEntityPairAnswer ["+correct+" target=" + target.getName() + ", docid="
				+ docid + ", sentid=" + sentid + ", args=" + args + "]";
	}

	@Override
	public String toIDString() {
		return toString();
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SentenceEntityPairAnswer that = (SentenceEntityPairAnswer) o;

        if (sentid != that.sentid) return false;
        if (target != null ? !target.equals(that.target) : that.target != null) return false;
        if (target != null && target.isSymmetric()) {
            if (!((arg0EntityId.equals(that.arg0EntityId) && arg1EntityId.equals(that.arg1EntityId)) ||
                  (arg0EntityId.equals(that.arg1EntityId) && arg1EntityId.equals(that.arg0EntityId))))
            {
                return false;
            }
        } else {
            if (!arg0EntityId.equals(that.arg0EntityId)) return false;
            if (!arg1EntityId.equals(that.arg1EntityId)) return false;
        }

        return docid.equals(that.docid);
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + (target != null ? target.getName().hashCode() : 0);
        result = 31 * result + (docid != null ? docid.hashCode() : 0);
        result = 31 * result + sentid;
        if (target != null && target.isSymmetric()) {
            result = 31 * result + arg0EntityId.hashCode() + arg1EntityId.hashCode();
        } else {
            result = 31 * result + arg0EntityId.hashCode();
            result = 31 * result + arg1EntityId.hashCode();
        }
        return result;
    }
}
