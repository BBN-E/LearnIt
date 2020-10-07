package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.bilingual.BilingualDocTheory;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfoDisplay;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.common.targets.TargetFactory;
import com.bbn.akbc.neolearnit.common.writers.LearnitHtmlOutputWriter;
import com.bbn.akbc.neolearnit.evaluation.AnswerFactory;
import com.bbn.akbc.neolearnit.evaluation.ContextFeatureWithAnswers;
import com.bbn.akbc.neolearnit.evaluation.PrecisionRecallScore;
import com.bbn.akbc.neolearnit.evaluation.answers.AnswerCollection;
import com.bbn.akbc.neolearnit.evaluation.answers.RelationAnswer;
import com.bbn.akbc.neolearnit.evaluation.offsets.BasicOffsetConverter;
import com.bbn.akbc.neolearnit.evaluation.offsets.BilingualChineseOffsetConverter;
import com.bbn.akbc.neolearnit.evaluation.offsets.OffsetConverter;
import com.bbn.akbc.neolearnit.loaders.impl.BilingualDocTheoryInstanceLoader;
import com.bbn.akbc.neolearnit.mappings.groups.EvalReportMappings;
import com.bbn.akbc.neolearnit.modules.EvaluationModule;
import com.bbn.akbc.neolearnit.observations.evaluation.EvalAnswer;
import com.bbn.akbc.neolearnit.observations.pattern.BetweenSlotsPattern;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.pattern.MonolingualPattern;
import com.bbn.akbc.neolearnit.observations.pattern.PropPattern;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.bue.common.parameters.Parameters;
import com.bbn.serif.apf.APFLoader;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * WARNING: Not set up for monolingual learnit
 * @author mshafir
 *
 */
public class OracleAnalysis {

	public static List<String> getFilenames(String bllfile) throws IOException {
		List<String> result = new ArrayList<String>();
		BufferedReader br = new BufferedReader(new FileReader(new File(bllfile)));
		String line;
		while ((line = br.readLine()) != null) {
			if (!br.ready()) continue;
			result.add(line.split(" ")[0].replace(".segment", ""));
			br.readLine(); //second line isn't needed for filenames
		}
		br.close();
		return result;
	}


	public static void main(String[] args) throws IOException {

		String paramFile = args[0];
		String coreDir = args[1];
		String bidoclist = args[2];

		String output = args[3];
		String targetName = args[4];

		LearnItConfig.loadParams(new File(paramFile));
		Parameters params = Parameters.loadSerifStyle(new File(paramFile));


		Target target = TargetFactory.fromXMLPathString(targetName);
		String targetPathRel  = String.format("inputs/targets/json/%s.json",targetName);

		if (LearnItConfig.optionalParamTrue("bilingual")) {

			OffsetConverter offConv = new BilingualChineseOffsetConverter(coreDir+"/serifxml/chinese", coreDir+"/rawtext", params);

			//load gold info
			APFLoader apfLoader = new APFLoader();
			AnswerCollection.Builder answersBuilder = new AnswerCollection.Builder();
			List<String> docs = getFilenames(bidoclist);
			for (String apfdocname : docs) {
				String apfDoc = coreDir+"/gold/"+apfdocname+".apf.xml";
				System.out.println("loading apf "+apfDoc+"...");
				AnswerFactory.collectAnswers(apfLoader.loadFrom(new File(apfDoc)), answersBuilder, offConv);
			}
			AnswerCollection answers = answersBuilder.build();

			EvaluationModule evalModule = new EvaluationModule(answers,true);
			BilingualDocTheoryInstanceLoader bidocLoader = evalModule.getBilingualDocTheoryLoader(target);

			//read pairs of lines
			BufferedReader br = new BufferedReader(new FileReader(new File(bidoclist)));
			String line;
			while ((line = br.readLine()) != null) {
				if (!br.ready()) continue;

				String line2 = br.readLine();
				BilingualDocTheory bidoc = BilingualDocTheory.fromVariantStyleString(line,line2);
				bidocLoader.load(bidoc);

			}
			br.close();

			LearnitHtmlOutputWriter writer = new LearnitHtmlOutputWriter(new File(output));
			writer.start();
			performAnalysis(writer, evalModule.getEvalMappings(new TargetAndScoreTables(targetPathRel)), answers, target);
			writer.close();

		} else {
			@SuppressWarnings("unused")
			OffsetConverter offConv = new BasicOffsetConverter(coreDir+"/gold_serifxml", params);
			//nothing for now
		}
	}

	public static PrecisionRecallScore getPotentialScore(Set<EvalAnswer> curSet, Set<EvalAnswer> newSet, Set<EvalAnswer> golds) {
		return PrecisionRecallScore.from(Sets.union(curSet, newSet), golds);
	}

	public static void writeAnswerInstances(LearnitHtmlOutputWriter writer, EvalAnswer ans, EvalReportMappings mappings) throws IOException {
		writer.startCollapsibleSection("Instances of Answer "+ans.toPrettyString(), false);
		writer.startCollapsibleSection("APF relation mention sources", false);
		for (RelationAnswer ra : ans.getMatchedAnnotations()) {
			writer.writeContent(ra.toString());
		}
		writer.endCollapsibleSection();
		for (InstanceIdentifier id : mappings.getInstance2Answer().getInstances(ans)) {
			MatchInfoDisplay mi = mappings.getInstance2MatchInfo().getMatchInfoDisplay(id);
			for (String language : mi.getAvailableLanguages()) {
				writer.writeHtmlContent(mi.getLanguageMatchInfoDisplay(language).html());
				writer.writeHtmlContent("<br />");
				writer.writeHtmlContent(mi.getLanguageMatchInfoDisplay(language).link());
			}
		}
		writer.endCollapsibleSection();
	}

	public static void oracleSet(LearnitHtmlOutputWriter writer, EvalReportMappings mappings,
			Set<EvalAnswer> goldAnswers, List<ContextFeatureWithAnswers> cfAnswers) throws IOException {

		//best precision, sort by cf precision accept until f goes down
		List<ContextFeatureWithAnswers> finalSet = new ArrayList<ContextFeatureWithAnswers>();
		Set<EvalAnswer> curAnsSet = new HashSet<EvalAnswer>();
		double curScore = -1;
		for (ContextFeatureWithAnswers cfAnswer : cfAnswers) {
			double newScore = getPotentialScore(curAnsSet, cfAnswer.getAnswers(), goldAnswers).getF1();

			if (newScore > curScore && cfAnswer.calculatePrecision() > 0.0) {
				curAnsSet.addAll(cfAnswer.getAnswers());
				finalSet.add(cfAnswer);
				curScore = newScore;
			} else {
				continue;
			}
		}
		writer.writeContent("Best Possible Score: "+PrecisionRecallScore.from(curAnsSet,goldAnswers));
		writer.startCollapsibleSection("pattern set", false);
		for (ContextFeatureWithAnswers cfAnswer : finalSet) {
			writer.startCollapsibleSection(cfAnswer.getCf().toPrettyString()+" has score of "+cfAnswer.calculatePrecision()+" over "+cfAnswer.getAnswers().size()+" answers",false);
			for (EvalAnswer ans : cfAnswer.getAnswers()) {
				writeAnswerInstances(writer,ans,mappings);
			}
			writer.endCollapsibleSection();
		}
		writer.endCollapsibleSection();
	}



	public static void performAnalysis(LearnitHtmlOutputWriter writer, EvalReportMappings mappings, AnswerCollection answers, Target target) throws IOException {

		List<RelationAnswer> unattested = new ArrayList<RelationAnswer>();
		for (RelationAnswer ans : answers.getRelations()) {
			if (target.getName().toLowerCase().contains(ans.getRelationType().toLowerCase())) {
				if (!ans.isAttested()) {
					unattested.add(ans);
				}
			}
		}

		writer.startCollapsibleSection(unattested.size()+" unattested APF relations", false);
		for (RelationAnswer ans : unattested) {
			writer.writeContent(ans.toString());
		}
		writer.endCollapsibleSection();

		Set<EvalAnswer> goldAnswers = mappings.getInstance2Answer().getGoldAnswers();
		System.out.println(goldAnswers.size());

		List<ContextFeatureWithAnswers> cfAnswers = new ArrayList<ContextFeatureWithAnswers>();
		for (LearnitPattern cf : mappings.getInstance2Pattern().getAllPatterns().elementSet()) {
			cfAnswers.add(ContextFeatureWithAnswers.from(cf, mappings.getInstance2Answer(), mappings.getInstance2Pattern()));
		}

		Set<EvalAnswer> systemBestRecallAnswers = new HashSet<EvalAnswer>();


		//best recall get all answers
		for (ContextFeatureWithAnswers cfAnswer : cfAnswers) {
			systemBestRecallAnswers.addAll(cfAnswer.getAnswers());
		}
		writer.writeContent("Best Recall Score: "+PrecisionRecallScore.from(systemBestRecallAnswers, goldAnswers));

		writer.startCollapsibleSection("Misses",true);
		int numMisses = 0;
		for (EvalAnswer ans : goldAnswers) {
			if (!systemBestRecallAnswers.contains(ans)) {
				writeAnswerInstances(writer,ans,mappings);
				numMisses++;
			}
		}
		writer.endCollapsibleSection();

		writer.writeHtmlContent("Count: "+numMisses+"<br /><br />");

		Collections.sort(cfAnswers);

		writer.writeContent("Best Overall pattern results: ");
		oracleSet(writer,mappings,goldAnswers,cfAnswers);

		List<ContextFeatureWithAnswers> english = ImmutableList.copyOf(
				Iterables.filter(cfAnswers, new Predicate<ContextFeatureWithAnswers>() {
					@Override
					public boolean apply(ContextFeatureWithAnswers cf) {
						if (cf.getCf() instanceof MonolingualPattern) {
							MonolingualPattern mcf = (MonolingualPattern)cf.getCf();
							return mcf.getLanguage().equals("english");
						}
						return false;
					}
				}));
		writer.writeContent("Best English-only pattern results: ");
		oracleSet(writer,mappings,goldAnswers,english);


		List<ContextFeatureWithAnswers> chinese = ImmutableList.copyOf(
				Iterables.filter(cfAnswers, new Predicate<ContextFeatureWithAnswers>() {
					@Override
					public boolean apply(ContextFeatureWithAnswers cf) {
						if (cf.getCf() instanceof MonolingualPattern) {
							MonolingualPattern mcf = (MonolingualPattern)cf.getCf();
							return mcf.getLanguage().equals("chinese");
						}
						return false;
					}
				}));
		writer.writeContent("Best Chinese-only pattern results: ");
		oracleSet(writer,mappings,goldAnswers,chinese);


		List<ContextFeatureWithAnswers> prop = ImmutableList.copyOf(
				Iterables.filter(cfAnswers, new Predicate<ContextFeatureWithAnswers>() {
					@Override
					public boolean apply(ContextFeatureWithAnswers cf) {
						return cf.getCf() instanceof PropPattern;
					}
				}));
		writer.writeContent("Best Prop-only pattern results: ");
		oracleSet(writer,mappings,goldAnswers,prop);


		List<ContextFeatureWithAnswers> regex = ImmutableList.copyOf(
				Iterables.filter(cfAnswers, new Predicate<ContextFeatureWithAnswers>() {
					@Override
					public boolean apply(ContextFeatureWithAnswers cf) {
						return cf.getCf() instanceof BetweenSlotsPattern;
					}
				}));
		writer.writeContent("Best Regex-only pattern results: ");
		oracleSet(writer,mappings,goldAnswers,regex);
	}


}
