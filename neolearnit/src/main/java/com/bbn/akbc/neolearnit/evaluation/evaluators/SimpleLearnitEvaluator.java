package com.bbn.akbc.neolearnit.evaluation.evaluators;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfoDisplay;
import com.bbn.akbc.neolearnit.common.writers.LearnitHtmlOutputWriter;
import com.bbn.akbc.neolearnit.evaluation.answers.AnswerCollection;
import com.bbn.akbc.neolearnit.evaluation.answers.RelationAnswer;
import com.bbn.akbc.neolearnit.mappings.groups.EvalReportMappings;
import com.bbn.akbc.neolearnit.observations.evaluation.EvalAnswer;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.scoring.scores.PatternScore;
import com.bbn.akbc.neolearnit.scoring.tables.AbstractScoreTable.ObjectWithScore;
import com.google.common.collect.Sets;

public class SimpleLearnitEvaluator implements LearnitEvaluator {

	private final EvalReportMappings evalMappings;
	private final AnswerCollection answers;
	private final boolean matchAll;
	private final LearnitHtmlOutputWriter writer;

	public SimpleLearnitEvaluator(EvalReportMappings evalMappings, AnswerCollection answers,
			LearnitHtmlOutputWriter writer, boolean matchAll) {

		this.evalMappings = evalMappings;
		this.answers = answers;
		this.matchAll = matchAll;
		this.writer = writer;
	}

	public void writeAnswerInstances(EvalAnswer ans) throws IOException {
		writer.startCollapsibleSection("Instances of Answer "+ans.toPrettyString(), false);
		writer.startCollapsibleSection("APF relation mention sources", false);
		for (RelationAnswer ra : ans.getMatchedAnnotations()) {
			writer.writeContent(ra.toString());
		}
		writer.endCollapsibleSection();
		for (InstanceIdentifier id : evalMappings.getInstance2Answer().getInstances(ans)) {
			MatchInfoDisplay mi = evalMappings.getInstance2MatchInfo().getMatchInfoDisplay(id);
			for (String language : mi.getAvailableLanguages()) {
				writer.writeHtmlContent(mi.getLanguageMatchInfoDisplay(language).html());
				writer.writeHtmlContent("<br />");
				writer.writeHtmlContent(mi.getLanguageMatchInfoDisplay(language).link());
			}
		}
		writer.endCollapsibleSection();
	}


	public void scoreAnswers(Set<EvalAnswer> answers, Set<EvalAnswer> goldAnswers, boolean showGoldTotals) throws IOException {
		int totalGold = goldAnswers.size();
		int capturedGold = Sets.intersection(answers, goldAnswers).size();

		//count up the wrong
		Set<EvalAnswer> correct = new HashSet<EvalAnswer>();
		Set<EvalAnswer> incorrect = new HashSet<EvalAnswer>();
		for (EvalAnswer ans : answers) {
			if (ans.isCorrect()) {
				correct.add(ans);
			} else {
				incorrect.add(ans);
			}
		}

		//count the matched answers
		int matched = 0;
		for (EvalAnswer ans : goldAnswers) {
			for (InstanceIdentifier id : evalMappings.getInstance2Answer().getInstances(ans)) {
				if (evalMappings.getInstance2Pattern().getPatterns(id).size() > 0) {
					matched++;
					break;
				}
			}
		}

		if (showGoldTotals) writer.writeContent("Total gold: "+totalGold);
		if (showGoldTotals) writer.writeContent("Captured gold: "+capturedGold);

		writer.startCollapsibleSection("Correct: "+correct.size(),false);
		for (EvalAnswer ans : correct) writeAnswerInstances(ans);
		writer.endCollapsibleSection();

		writer.startCollapsibleSection("Incorrect: "+incorrect.size(),false);
		for (EvalAnswer ans : incorrect) writeAnswerInstances(ans);
		writer.endCollapsibleSection();

		double p = (double)correct.size()/answers.size();
		double r = (double)correct.size()/totalGold;
		double rUB = (double)matched/totalGold;
		double f1 = (2*p*r)/(p+r);

		writer.writeContent("Precision: "+p);
		writer.writeContent("Recall: "+r);
		if (showGoldTotals) writer.writeContent("Recall upper bound: "+rUB);
		writer.writeContent("F1: "+f1);
	}


	@Override
	public void evaluate(TargetAndScoreTables data) {
		for (float cutoff = 0.1F; cutoff <= 1.0; cutoff+=0.1) {
			System.out.println(String.format("Evaluating at %.2f",cutoff));
			try {
				writer.startCollapsibleSection(String.format("Scores at %.2f",cutoff), false);
				evaluate(data,cutoff);
				writer.endCollapsibleSection();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void evaluate(TargetAndScoreTables data, float cutoff) throws IOException {
		Set<EvalAnswer> answers = new HashSet<EvalAnswer>();
		Set<EvalAnswer> goldAnswers = evalMappings.getInstance2Answer().getGoldAnswers();

		List<ObjectWithScore<LearnitPattern, PatternScore>> scoredObjs;
		if (matchAll) {
			scoredObjs = data.getPatternScores().getObjectsWithScores();
		} else {
			scoredObjs = data.getPatternScores().getFrozenObjectsWithScores();
		}

		writer.startCollapsibleSection("patterns", false);
		for (ObjectWithScore<LearnitPattern, PatternScore> scoredCF : scoredObjs) {

			if (scoredCF.getScore().getPrecision() >= cutoff) {
				Set<EvalAnswer> cfAnswers = new HashSet<EvalAnswer>();
				for (InstanceIdentifier id : evalMappings.getInstance2Pattern().getInstances(scoredCF.getObject())) {
					answers.add(evalMappings.getInstance2Answer().getAnswer(id));
					cfAnswers.add(evalMappings.getInstance2Answer().getAnswer(id));
				}

				if (cfAnswers.size() > 0) {
					writer.writeContent("Score for\n"+scoredCF.getObject().toPrettyString());
					writer.writeContent("\t"+scoredCF.getScore());
					scoreAnswers(cfAnswers,goldAnswers,false);
					writer.writeHtmlContent("<hr />");
				}
			}
		}
		writer.endCollapsibleSection();

		writer.startCollapsibleSection("uncapturable gold", false);
		int unatCount = 0;
		for (RelationAnswer ans : this.answers.getRelations()) {
			if (data.getTarget().getName().toLowerCase().contains(ans.getRelationType().toLowerCase())) {
				if (!ans.isAttested()) {
					writer.writeContent(ans.getArg0().getDocid()+"--"+ans.getArg0().getSentid());
					writer.writeContent(ans.getArg0().getText()+":"+ans.getArg0().getStartToken()+":"+ans.getArg0().getEndToken()+" to "+
							ans.getArg1().getText()+":"+ans.getArg1().getStartToken()+":"+ans.getArg1().getEndToken());
					//for (Spanning s : evalMappings.getInstance2MatchInfo().getSpanningsInSentence(ans.getArg0().getDocid(), ans.getArg0().getSentid())) {
					//	writer.writeContent("\t"+s.span().tokenizedText()+":"+s.span().startIndex()+":"+(s.span().endIndex()+1));
					//}

					unatCount++;
				}
			}
		}
		writer.endCollapsibleSection();

		writer.writeHeader("\n\nResults for "+data.getTarget().getName(), 1);
		writer.writeContent(unatCount+" unattested relations.");
		scoreAnswers(answers,goldAnswers,true);
	}

}
