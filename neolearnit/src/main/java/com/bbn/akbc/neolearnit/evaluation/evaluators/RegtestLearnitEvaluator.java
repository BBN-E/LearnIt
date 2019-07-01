package com.bbn.akbc.neolearnit.evaluation.evaluators;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfoDisplay;
import com.bbn.akbc.neolearnit.evaluation.answers.AnswerCollection;
import com.bbn.akbc.neolearnit.evaluation.answers.RelationAnswer;
import com.bbn.akbc.neolearnit.exec.EvaluateLearnit;
import com.bbn.akbc.neolearnit.mappings.groups.EvalReportMappings;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.evaluation.EvalAnswer;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.observers.instance.evaluation.SentenceEntityPairAnswerObserver;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.scoring.scores.PatternScore;
import com.bbn.akbc.neolearnit.scoring.tables.AbstractScoreTable.ObjectWithScore;

import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RegtestLearnitEvaluator implements LearnitEvaluator {

	private final EvalReportMappings evalMappings;
	private final AnswerCollection answers;
	private final Writer writer;

	private final boolean onlyFrozen;
	private final double confidenceCutoff;

  	private final double precisionCutoff;

	public RegtestLearnitEvaluator(EvalReportMappings evalMappings,
			AnswerCollection answers,
			Writer writer) {

		this.evalMappings = evalMappings;
		this.answers = answers;
		this.writer = writer;

		this.onlyFrozen = !LearnItConfig.optionalParamTrue("regtest_evaluate_nonfrozen");
		if (LearnItConfig.defined("regtest_confidence_cutoff")) {
			this.confidenceCutoff = LearnItConfig.getDouble("regtest_confidence_cutoff");
		} else {
			this.confidenceCutoff = 0.5;
		}

	  if (LearnItConfig.defined("regtest_precision_cutoff")) {
	    this.precisionCutoff = LearnItConfig.getDouble("regtest_precision_cutoff");
	  } else {
	    this.precisionCutoff = 0.5;
	  }
	}

	public void writeAnswerInstances(EvalAnswer ans) throws IOException {
		writer.write("Answer: "+ans.toString()+"\n");
		List<String> txts = new ArrayList<String>();
		for (InstanceIdentifier id : evalMappings.getInstance2Answer().getInstances(ans)) {
			MatchInfoDisplay mi = evalMappings.getInstance2MatchInfo().getMatchInfoDisplay(id);
			txts.add(mi.html());
		}
		Collections.sort(txts);
		for (String txt : txts) {
			writer.write("\t"+txt+"\n");
		}
	}

	public void writeAnswerSet(Set<EvalAnswer> answers) throws IOException {
		List<EvalAnswer> sortedAnswers = Lists.newArrayList(answers);
		Collections.sort(sortedAnswers, new Comparator<EvalAnswer>() {

			@Override
			public int compare(EvalAnswer o1, EvalAnswer o2) {
				return o1.toString().compareTo(o2.toString());
			}

		});
		for (EvalAnswer ans : sortedAnswers) writeAnswerInstances(ans);
	}

	@Override
	public void evaluate(final TargetAndScoreTables data) throws IOException {
		Set<EvalAnswer> aset = new HashSet<EvalAnswer>();
//		Set<EvalAnswer> goldAnswers = evalMappings.getInstance2Answer().getGoldAnswers();
		Set<EvalAnswer> goldAnswers = new HashSet<EvalAnswer>();
        for (InstanceIdentifier id : evalMappings.getInstance2Answer().getAllInstances().elementSet()) {
            if (evalMappings.getInstance2Answer().getAnswer(id).isCorrect())
                goldAnswers.add(evalMappings.getInstance2Answer().getAnswer(id));
        }
//        for (EvalAnswer ans : evalMappings.getInstance2Answer().getAllAnswers()) {
//            if (ans.isCorrect())
//                goldAnswers.add(ans);
//        }

		List<LearnitPattern> acceptedPatterns = new ArrayList<LearnitPattern>();

		List<ObjectWithScore<LearnitPattern, PatternScore>> scoredObjs;
		if (this.onlyFrozen) {
			scoredObjs = data.getPatternScores().getFrozenObjectsWithScores();
		} else {
			scoredObjs = data.getPatternScores().getObjectsWithScores();
		}

		for (ObjectWithScore<LearnitPattern, PatternScore> scoredPattern : scoredObjs) {
			if (scoredPattern.getScore().getConfidence() >= confidenceCutoff &&
//					scoredPattern.getScore().isGood()) {
			    scoredPattern.getScore().isGood(precisionCutoff)) { // make precision configurable

				acceptedPatterns.add(scoredPattern.getObject());

				for (InstanceIdentifier id : evalMappings.getInstance2Pattern().getInstances(scoredPattern.getObject())) {
					  aset.add(evalMappings.getInstance2Answer().getAnswer(id));
				}
			}
		}

		Collections.sort(acceptedPatterns, new Comparator<LearnitPattern>() {

			@Override
			public int compare(LearnitPattern o1, LearnitPattern o2) {
				int iter1 = data.getPatternScores().getScore(o1).getFrozenIteration();
				int iter2 = data.getPatternScores().getScore(o2).getFrozenIteration();

				if (iter1 == iter2) {
					return o1.toIDString().compareTo(o2.toIDString());
				} else {
					return iter1 - iter2;
				}
			}

		});

		int totalGold = goldAnswers.size();
//        for (EvalAnswer ans : goldAnswers) {
//            writer.write(ans.toIDString() + "\n");
//        }

		//count up the wrong
		Set<EvalAnswer> correct = new HashSet<EvalAnswer>();
		Set<EvalAnswer> incorrect = new HashSet<EvalAnswer>();
		for (EvalAnswer ans : aset) {
			if (ans.isCorrect()) {
				correct.add(ans);
			} else {
				incorrect.add(ans);
			}
		}

		//count the matched answers
		int matched = 0;
		Set<EvalAnswer> missed = new HashSet<EvalAnswer>();
	  	Set<LearnitPattern> patternsGold = new HashSet<LearnitPattern>();
		for (EvalAnswer ans : goldAnswers) {
			for (InstanceIdentifier id : evalMappings.getInstance2Answer().getInstances(ans)) {
				if (evalMappings.getInstance2Pattern().getPatterns(id).size() > 0) {
					matched++;

				  // look at seeds and patterns
				  MatchInfoDisplay mi = evalMappings.getInstance2MatchInfo().getMatchInfoDisplay(id);
				  String seedString = "N/A";
				  if(mi.getPrimaryLanguageMatch().getCanonicalSeed().isPresent()) {
				    List<String> stringSlots = mi.getPrimaryLanguageMatch().getCanonicalSeed().get()
					.getStringSlots();
				    seedString = "<" + stringSlots.get(0) + ", " + stringSlots.get(1) + ">";
				  }
				  for(LearnitPattern p : evalMappings.getInstance2Pattern().getPatterns(id)) {
				      writer.write("RECALL_ANALYSIS\t" + matched + "\t" + seedString + "\t" + p.toIDString() + "\n");
				    patternsGold.add(p);
				  }
				  //

					break;
				}
			}
			if (!aset.contains(ans)) {
				missed.add(ans);
			}
		}


	  // print all seeds for analysis
	  if(EvaluateLearnit.strDirMappings.isPresent()) {
	    File dirMappings = new File(EvaluateLearnit.strDirMappings.get());
	    for (File fileMappings : dirMappings.listFiles()) {
	      System.out.println("deserialize mappings: " + fileMappings.getAbsolutePath());
	      Mappings info = Mappings.deserialize(fileMappings, true);
	      Multiset<Seed> seeds = info.getSeedsForPatterns(patternsGold);
	      for (Seed seed : seeds.elementSet()) {
		writer.write(
		    "RECALL_ANALYSIS_SEEDS\t"
			+ seeds.count(seed) + "\t"
			+ seed.getLanguage() + "\t"
			+ seed.toIDString()
			+ "\n");
	      }
	    }
	  }
	  //



		double p = (double)correct.size()/aset.size();
		double r = (double)correct.size()/totalGold;
		  double rUB = (double)matched/totalGold;
		double f1 = (2*p*r)/(p+r);

		writer.write(data.getTarget().getName()+ " Iteration "+data.getIteration()+" Regression Test Evaluation Results:\n");
		writer.write("=================================================\n\n");
		writer.write(String.format("Precision: %.2f (%d/%d)\n",p,correct.size(),aset.size()));
		writer.write(String.format("Recall: %.2f (%d/%d)\n",r,correct.size(),totalGold));
		writer.write(String.format("Recall upper bound: %.2f (%d/%d)\n",rUB,matched,totalGold));
		writer.write(String.format("F1: %.2f\n",f1));

		writer.write("\n\n\n");

		writer.write("Accepted Patterns:"+acceptedPatterns.size()+"\n-----------------------------\n");
		for (LearnitPattern pattern : acceptedPatterns) {
			int iter = data.getPatternScores().getScore(pattern).getFrozenIteration();
			if (iter == 0) {
				writer.write("INITIAL: ");
			} else if (iter == -1) {
				writer.write("PROPOSED: ");
			} else {
				writer.write(iter+": ");
			}
			writer.write(pattern.toIDString() + " - ");

		  double dbg_c = data.getPatternScores().getScore(pattern).getConfidence();
		  double dbg_p = data.getPatternScores().getScore(pattern).getPrecision();
		  double dbg_r = data.getPatternScores().getScore(pattern).getRecall();
		  double dbg_freq = data.getPatternScores().getScore(pattern).getFrequency();

		  writer.write(dbg_c + "/" +
		  	dbg_p + "/" +
		  	dbg_r + "/" +
		  	dbg_freq + " - ");

		  int pcorrect = 0;
			int ptotal = 0;
            Set<EvalAnswer> ans = new HashSet<EvalAnswer>();
			for (InstanceIdentifier id : evalMappings.getInstance2Pattern().getInstances(pattern)) {
				if (evalMappings.getInstance2Answer().getAnswer(id).isCorrect()) {
                    pcorrect++;
//                    writer.write("\n\t" + evalMappings.getInstance2Answer().getAnswer(id).toIDString());
                }
				ptotal++;
                ans.add(evalMappings.getInstance2Answer().getAnswer(id));
			}
//            writer.write("\n");
			writer.write(pcorrect+"/"+ptotal);
			writer.write("\n");

		}
		writer.write("\n\n");

		writer.write("Correct: "+correct.size()+"\n-----------------------------\n");
		writeAnswerSet(correct);
		writer.write("\n\n");

		writer.write("Incorrect: "+incorrect.size()+"\n-----------------------------\n");
		writeAnswerSet(incorrect);
		writer.write("\n\n");

		writer.write("Missed: "+missed.size()+"\n-----------------------------\n");
		writeAnswerSet(missed);
		writer.write("\n\n");

		int unatCount = 0;
		writer.write("Unattested\n-----------------------------\n");
		for (RelationAnswer ans : this.answers.getRelations()) {
			if (SentenceEntityPairAnswerObserver.relationNameMatch(data.getTarget().getName(), ans.getRelationType())) {
				if (!ans.isAttested()) {
					unatCount++;
					writer.write("\t"+ans+"\n");
				}
			}
		}
		writer.write("Total Unattested Count: "+unatCount+"\n\n");

	}



}
