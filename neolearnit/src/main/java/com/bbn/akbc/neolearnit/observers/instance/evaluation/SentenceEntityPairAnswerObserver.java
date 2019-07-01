package com.bbn.akbc.neolearnit.observers.instance.evaluation;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo.LanguageMatchInfo;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.evaluation.answers.AnswerCollection;
import com.bbn.akbc.neolearnit.evaluation.answers.MentionAnswer;
import com.bbn.akbc.neolearnit.evaluation.answers.RelationAnswer;
import com.bbn.akbc.neolearnit.evaluation.answers.SpanningAnswer;
import com.bbn.akbc.neolearnit.evaluation.answers.ValueMentionAnswer;
import com.bbn.akbc.neolearnit.mappings.Recorder;
import com.bbn.akbc.neolearnit.observations.evaluation.EvalAnswer;
import com.bbn.akbc.neolearnit.observations.evaluation.SentenceEntityPairAnswer;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.Spanning;
import org.eclipse.jetty.util.ConcurrentHashSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SentenceEntityPairAnswerObserver extends AbstractAnswerObserver<EvalAnswer> {

	private final AnswerCollection answerCollection;
	private final int fuzzyAmount;
	private final boolean useSerifCoref;

	public SentenceEntityPairAnswerObserver(
			Recorder<InstanceIdentifier, EvalAnswer> recorder,
			AnswerCollection answerCollection) {

		this(recorder, answerCollection, 1, false);
	}

	public SentenceEntityPairAnswerObserver(
			Recorder<InstanceIdentifier, EvalAnswer> recorder,
			AnswerCollection answerCollection,
			int fuzzyAmount) {

		this(recorder, answerCollection, fuzzyAmount, false);
	}

	public SentenceEntityPairAnswerObserver(
			Recorder<InstanceIdentifier, EvalAnswer> recorder,
			AnswerCollection answerCollection,
			boolean useSerifCoref) {

		this(recorder, answerCollection, 1, useSerifCoref);
	}

	public SentenceEntityPairAnswerObserver(
			Recorder<InstanceIdentifier, EvalAnswer> recorder,
			AnswerCollection answerCollection,
			int fuzzyAmount, boolean useSerifCoref) {

		super(recorder);
		this.fuzzyAmount = fuzzyAmount;
		this.answerCollection = answerCollection;
		this.useSerifCoref = useSerifCoref;
		recordedGoodAnswers = new ConcurrentHashSet<SentenceEntityPairAnswer>();
		recordedBadAnswers = new ConcurrentHashSet<SentenceEntityPairAnswer>();
	}

	private final Set<SentenceEntityPairAnswer> recordedGoodAnswers;
	private final Set<SentenceEntityPairAnswer> recordedBadAnswers;
	@Override
	public void observe(MatchInfo match) {

		SentenceEntityPairAnswer ans = SentenceEntityPairAnswer.fromJudgementAndMatchInfo(answerCollection, correct(match), match);

		for (RelationAnswer relA : ans.getMatchedAnnotations()) {
			relA.setAttested(ans);
			if (relA.getBrandyMatch().isPresent())
				ans.addBrandyMatch(relA.getBrandyMatch().get());
		}

		if (recordedGoodAnswers.contains(ans) && !ans.isCorrect()) {
//            System.err.println("Contradictory answers produced, ("+ans+") this leads to an invalid state, ID: "+InstanceIdentifier.from(match));
//			throw new RuntimeException("Contradictory answers produced, ("+ans+") this leads to an invalid state, ID: "+InstanceIdentifier.from(match));
		}
		if (recordedBadAnswers.contains(ans) && ans.isCorrect()) {
            recordedBadAnswers.remove(ans);
//			System.err.println("Contradictory answers produced, ("+ans+") this leads to an invalid state, ID: "+InstanceIdentifier.from(match));
//			throw new RuntimeException("Contradictory answers produced, ("+ans+") this leads to an invalid state, ID: "+InstanceIdentifier.from(match));
		}
		if (ans.isCorrect()) recordedGoodAnswers.add(ans);
		else if (!recordedGoodAnswers.contains(ans)) recordedBadAnswers.add(ans);

		this.record(match, ans);
	}

	/**
	 * Determine whether two spans overlap allowing mismatch up to fuzzyAmount
	 * If fuzzyAmount is 1, then this is exact match
	 * @param s1
	 * @param e1
	 * @param s2
	 * @param e2
	 * @return
	 */
	protected static boolean overlap(int s1, int e1, int s2, int e2, int fuzzyAmount) {
		return Math.abs(s2-s1) < fuzzyAmount && Math.abs(e2-e1) < fuzzyAmount;
	}

	public static boolean mentionSentenceMatch(SpanningAnswer slotAns,
			SentenceTheory st, Spanning slot, int fuzzyAmount) {

		//exact offset match
		int slotStartToken;
		int slotEndToken;

		if (slotAns.getSentid() != st.index()) {
			return false;
		}

		//for mentions check the head first
		if (slot instanceof Mention) {
			Mention mSlot = (Mention)slot;

			slotStartToken = mSlot.atomicHead().span().startToken().index();
			slotEndToken = mSlot.atomicHead().span().endToken().index();

			if (overlap(slotStartToken, slotEndToken, slotAns.getStartToken(), slotAns.getEndToken(), fuzzyAmount)) {
				return true;
			}
		}

		slotStartToken = slot.span().startToken().index();
		slotEndToken = slot.span().endToken().index();

		//Check overlap of full span
		if (overlap(slotStartToken, slotEndToken, slotAns.getStartToken(), slotAns.getEndToken(), fuzzyAmount)) {
			return true;
		} else {

			if (slotAns.getText().toLowerCase().equals(slot.span().tokenizedText().utf16CodeUnits().toLowerCase())) {

				//If the text is identical, let the matching get a little fuzzier. It's probably an offset issue
				if (overlap(slotStartToken, slotEndToken, slotAns.getStartToken(), slotAns.getEndToken(), fuzzyAmount+1)) {
					return true;
				}
//				System.out.println(slotAns.getText() + ":" +slotAns.getSentid()+":"+slotAns.getStartToken()+":"+slotAns.getEndToken()+
//						" -- "+slot.span().tokenizedText()+":"+st.index()+":"+slotStartToken+":"+slotEndToken);
			}
			return false;
		}
		//return slotAns.getStartToken() == slotStartToken && slotAns.getEndToken() == slotEndToken;

	}

	protected boolean entitySentenceMatch(SpanningAnswer slotAns, SentenceTheory st, DocTheory dt, Spanning slot) {

		if (slotAns instanceof MentionAnswer) {
			MentionAnswer mAns = (MentionAnswer)slotAns;
			if (answerCollection.getCoreferences().get(mAns.getEntity()).isEmpty()) {
				throw new RuntimeException("Error: unknown entity "+mAns.getEntity());
			}

			if (useSerifCoref) {
				if (slot instanceof Mention) {
					Mention mention = (Mention)slot;
					if (mention.entity(dt).isPresent()) {
						for (Mention corefMen : mention.entity(dt).get().mentions()) {
							if (corefMen.sentenceNumber() == st.index()) {
								if (mentionSentenceMatch(mAns,st,corefMen,fuzzyAmount))
									return true;
							}
						}
					} else {
						if (mentionSentenceMatch(mAns,st,slot,fuzzyAmount))
							return true;
					}
				}
			} else {
				for (MentionAnswer corefAns : answerCollection.getCoreferences().get(mAns.getEntity())) {
					//if it's in the same sentence
					if (mentionSentenceMatch(corefAns,st,slot,fuzzyAmount))
						return true;
				}
			}
			return false;

		} else if (slotAns instanceof ValueMentionAnswer) {
			//simpler logic for values for now
			ValueMentionAnswer vmAns = (ValueMentionAnswer)slotAns;

			return mentionSentenceMatch(vmAns,st,slot,fuzzyAmount);

		} else {
			throw new RuntimeException("Unrecognized spanning answer type: "+slotAns);
		}
	}

	public static boolean relationNameMatch(String targetName, String apfSubtypeName) {
		if(targetName.toUpperCase().equals("ALL_MENTION_PAIRS"))
			return true;

		String tname = targetName.toLowerCase();
		String aname = apfSubtypeName.toLowerCase();

		if (tname.equals("agent_artifact_user-owner-inventor-manufacturer")) {
			return aname.equals("user-owner-inventor-manufacturer");

		} else if (tname.equals("per_social_family")) {
			return aname.equals("family");

		} else if (tname.equals("org_aff_membership")) {
			return aname.equals("membership");

		} else if (tname.equals("part_whole_geographical")) {
			return aname.equals("geographical");

		} else if (tname.equals("gen_aff_org-location")) {
			return aname.equals("org-location");

		} else {

			return tname.contains(aname) || aname.contains(tname);
		}
	}

	protected boolean relationMatch(RelationAnswer relAns, Target target, LanguageMatchInfo langMatch) {
		if (relationNameMatch(target.getName(),relAns.getRelationType())) {

			if (target.isSymmetric()) {
				boolean matchesNormal =
						entitySentenceMatch(relAns.getArg0(), langMatch.getSentTheory(), langMatch.getDocTheory(), langMatch.getSlot0().get()) &&
						entitySentenceMatch(relAns.getArg1(), langMatch.getSentTheory(), langMatch.getDocTheory(), langMatch.getSlot1().get());

				boolean matchesReverse =
						entitySentenceMatch(relAns.getArg0(), langMatch.getSentTheory(), langMatch.getDocTheory(), langMatch.getSlot1().get()) &&
						entitySentenceMatch(relAns.getArg1(), langMatch.getSentTheory(), langMatch.getDocTheory(), langMatch.getSlot0().get());

				return matchesNormal || matchesReverse;

			} else {

				return entitySentenceMatch(relAns.getArg0(), langMatch.getSentTheory(), langMatch.getDocTheory(), langMatch.getSlot0().get()) &&
						entitySentenceMatch(relAns.getArg1(), langMatch.getSentTheory(), langMatch.getDocTheory(), langMatch.getSlot1().get());
			}
		}
		return false;
	}

	protected List<RelationAnswer> correct(Target target, LanguageMatchInfo langMatch) {
		List<RelationAnswer> answers = new ArrayList<RelationAnswer>();
		List<RelationAnswer> potentials = answerCollection.getRelations(
				langMatch.getDocTheory().docid().toString(), langMatch.getSentTheory().index());
		/*
		if (potentials.isEmpty()) {
			System.err.println("no potential relations for doc "+
						langMatch.getDocTheory().docid().toString().replace(".segment", "")+
						" sent "+langMatch.getSentTheory().index());
		}*/

		for (RelationAnswer relAns : potentials) {
			if (relationMatch(relAns, target, langMatch)) {
				answers.add(relAns);
				//relAns.setAttested();
			}
		}
		return answers;
	}

	protected List<RelationAnswer> correct(MatchInfo match) {
		return correct(match.getTarget(), match.getPrimaryLanguageMatch());
	}


}
