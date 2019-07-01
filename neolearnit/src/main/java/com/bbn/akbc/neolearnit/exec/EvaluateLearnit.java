package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.common.targets.TargetFactory;
import com.bbn.akbc.neolearnit.common.util.BrandyToLearnItStringPatternConverter;
import com.bbn.akbc.neolearnit.common.writers.LearnitHtmlOutputWriter;
import com.bbn.akbc.neolearnit.evaluation.AnswerFactory;
import com.bbn.akbc.neolearnit.evaluation.answers.AnswerCollection;
import com.bbn.akbc.neolearnit.evaluation.answers.RelationAnswer;
import com.bbn.akbc.neolearnit.evaluation.evaluators.RegtestLearnitEvaluator;
import com.bbn.akbc.neolearnit.evaluation.offsets.BasicOffsetConverter;
import com.bbn.akbc.neolearnit.evaluation.offsets.BilingualChineseOffsetConverter;
import com.bbn.akbc.neolearnit.evaluation.offsets.OffsetConverter;
import com.bbn.akbc.neolearnit.evaluation.pelf.ElfLoader;
import com.bbn.akbc.neolearnit.loaders.LoaderUtils;
import com.bbn.akbc.neolearnit.loaders.impl.BilingualDocTheoryInstanceLoader;
import com.bbn.akbc.neolearnit.loaders.impl.MonolingualDocTheoryInstanceLoader;
import com.bbn.akbc.neolearnit.mappings.groups.EvalReportMappings;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToAnswerMapping;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToMatchInfoDisplayMap;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;
import com.bbn.akbc.neolearnit.modules.EvaluationModule;
import com.bbn.akbc.neolearnit.modules.PrematchedEvaluationModule;
import com.bbn.akbc.neolearnit.observations.evaluation.EvalAnswer;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observers.instance.evaluation.SentenceEntityPairAnswerObserver;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.storage.MapStorage;
import com.bbn.akbc.neolearnit.storage.StorageUtils;
import com.bbn.akbc.neolearnit.storage.impl.HashMapStorage;
import com.bbn.bue.common.files.FileUtils;
import com.bbn.bue.sexp.SexpReader;
import com.bbn.serif.apf.APFLoader;
import com.bbn.serif.patterns.Pattern;
import com.bbn.serif.patterns.PatternSetFactory;
import com.bbn.serif.theories.*;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import java.io.*;
import java.util.*;

public class EvaluateLearnit {

  public static Optional<String> strDirMappings = Optional.absent();
  public static int iteration = 0;

  public static void main(String[] args) throws IOException {

    String paramFile = args[0];
    String dataFileOrTarget = args[1];
    String relationFileName = args[2];

    // debug
    if(args.length>3)
      strDirMappings = Optional.of(args[3]);
    //
    String [] tmpItems = dataFileOrTarget.split("/");
    iteration = Integer.parseInt(tmpItems[tmpItems.length-2]);

    boolean oldStyle = args.length > 3 && args[3].equals("--old-style");
    boolean pruned = args.length > 3 && args[3].equals("--pruned");

    boolean isTuneParam = args.length > 3 && args[3].equals("--tune");
    Optional<String> suffix = Optional.absent();
    if(isTuneParam)
      suffix = Optional.of(args[4]);

    LearnItConfig.loadParams(new File(paramFile));

    if (oldStyle) {
      String target = args[4];
      String serifxmlDir = args[5];
      String elfDir = args[6];
      String apfDir = args[7];
      String outputFile = args[8];

      evaluateOldStyle(target, serifxmlDir, elfDir, apfDir, outputFile);

    } else {
      //Allowing for both parameter and command line approaches, to accommodate different evaluation setups
      String coreDir;
      String docList;
      String apfList;

//            boolean oracle = args[args.length-1].equals("--oracle");

      Target target;
      TargetAndScoreTables data;
//            if (oracle) {
//                target = TargetFactory.fromString(dataFileOrTarget);
//            } else {
      data = StorageUtils.getDefaultMapper().readValue(new FileInputStream(dataFileOrTarget), TargetAndScoreTables.class);
      target = data.getTarget();
//            }

      if (args.length == 6) {
	coreDir = args[3];
	docList = args[4];
	apfList = args[5];
      } else {
	coreDir = LearnItConfig.get("eval_dir");
	docList = LearnItConfig.get("test_set_serifxmls");
	apfList = LearnItConfig.get("test_set_apfs");

	if (coreDir.contains("%s")) {
	  coreDir = String.format(coreDir,target.getName());
	}
	if (docList.contains("%s")) {
	  docList = String.format(docList,target.getName());
	}
	if (apfList.contains("%s")) {
	  apfList = String.format(apfList,target.getName());
	}
	System.out.println("Using core dir: " + coreDir);
	System.out.println("Using serifxmls: " + docList);
	System.out.println("Using APFs: " + apfList);
      }

      EvaluationModule evalModule;
      if (LearnItConfig.optionalParamTrue("bilingual")) {
	evalModule = evaluateBilingual(apfList, coreDir, docList, target);
      } else {
	evalModule = evaluateMonolingual(apfList, coreDir, docList, target);
      }

      EvalReportMappings initEvalMappings;
//			if (oracle) {
//				evalMappings = evalModule.getEvalMappings(new TargetAndScoreTables(String.format("inputs/targets/json/%s.json",dataFileOrTarget)));
//				//nothing here at the moment

//			} else {
      System.out.println("Writing output evaluation alignment report...");
      initEvalMappings = evalModule.getEvalMappings(data);

      //Have to rebuild the Instance-Answer here because older, incomplete data was breaking :(
      Set<EvalAnswer> initGold = new HashSet<EvalAnswer>();
      Set<EvalAnswer> initNonGold = new HashSet<EvalAnswer>();
      for (InstanceIdentifier id : initEvalMappings.getInstance2Answer().getAllInstances()) {
	EvalAnswer ans = initEvalMappings.getInstance2Answer().getAnswer(id);
	if (ans.isCorrect()) initGold.add(ans);
	else initNonGold.add(ans);
      }

      //Put the answers back in with GOLD FIRST
      MapStorage.Builder<InstanceIdentifier,EvalAnswer> newAnswerMapBuilder = new HashMapStorage.Builder<InstanceIdentifier,EvalAnswer>();
      for (EvalAnswer ans : initGold) {
	for (InstanceIdentifier id : initEvalMappings.getInstance2Answer().getInstances(ans))
	  newAnswerMapBuilder.put(id,ans);
      }
      for (EvalAnswer ans : initNonGold) {
	for (InstanceIdentifier id : initEvalMappings.getInstance2Answer().getInstances(ans))
	  newAnswerMapBuilder.put(id,ans);
      }

      EvalReportMappings evalMappings = new EvalReportMappings(initEvalMappings.getExtractor(),
	  new InstanceToAnswerMapping(newAnswerMapBuilder.build()),
	  initEvalMappings.getInstance2Pattern(), initEvalMappings.getInstance2MatchInfo());

      System.out.println(evalMappings.getInstance2Answer().getGoldAnswers().size() + " ANSWERS");

      File outpath;
      File scorepath;
      if (pruned) {
	outpath = new File(LearnItConfig.get("archive_dir")+"/reports/"+LearnItConfig.get("learnit_expt_suffix")+"_pruned/"+ relationFileName+"/"+data.formattedStartDate()+"_report.json");
	scorepath = new File(LearnItConfig.get("archive_dir")+"/scores/"+LearnItConfig.get("learnit_expt_suffix") +"_pruned/"+relationFileName + "-" + iteration +"_scores.txt");
      } else {
	if(suffix.isPresent()) {
	  outpath = new File(LearnItConfig.get("archive_dir") + "/reports/" + LearnItConfig
	      .get("learnit_expt_suffix") + "/" + relationFileName + "/" + data.formattedStartDate()
	      + "_report.json" + "." + suffix.get());
	  scorepath = new File(LearnItConfig.get("archive_dir") + "/scores/" + LearnItConfig
	      .get("learnit_expt_suffix") + "/" + relationFileName + "-" + iteration  + "_scores.txt" + "." + suffix.get());
	}
	else {
	  outpath = new File(LearnItConfig.get("archive_dir") + "/reports/" + LearnItConfig
	      .get("learnit_expt_suffix") + "/" + relationFileName + "/" + data.formattedStartDate()
	      + "_report.json");
	  scorepath = new File(LearnItConfig.get("archive_dir") + "/scores/" + LearnItConfig
	      .get("learnit_expt_suffix") + "/" + relationFileName + "-" + iteration  + "_scores.txt");
	}
      }

      new File(outpath.getParent()).mkdirs();
      StorageUtils.serialize(outpath, evalMappings, true);

      System.out.println("Writing output scores...");

      new File(scorepath.getParent()).mkdirs();
      Writer scoreOut = new OutputStreamWriter(new FileOutputStream(scorepath),"UTF-8");
      RegtestLearnitEvaluator eval = new RegtestLearnitEvaluator(evalMappings, evalModule.getAnswers(), scoreOut);
      eval.evaluate(data);
      scoreOut.close();
//			}
    }

  }

//	private static boolean validSpanning(Target target, Spanning spanning) {
//		for (TargetSlot slot : target.getSlots()) {
//			for (SlotMatchConstraint sc : slot.getSlotConstraints()) {
//				if (sc instanceof AbstractSlotMatchConstraint) {
//					AbstractSlotMatchConstraint asc = (AbstractSlotMatchConstraint)sc;
//
//					if (asc.offForEvaluation() || asc.valid(spanning)) {
//						return true;
//					}
//				}
//			}
//		}
////		for (SlotMatchConstraint sc : target.getSlotConstraints(1)) {
////			if (sc instanceof AbstractSlotMatchConstraint) {
////				AbstractSlotMatchConstraint asc = (AbstractSlotMatchConstraint)sc;
////
////				if (asc.offForEvaluation() || asc.valid(spanning)) {
////					return true;
////				}
////			}
////		}
//		return false;
//	}

  private static void writeErrors(Target target, AnswerCollection answers, EvaluationModule evalModule, Map<String,DocTheory> docs,
      OffsetConverter offConv) throws IOException {

    File errorDir = new File(LearnItConfig.get("archive_dir")+"/errors/"+LearnItConfig.get("learnit_expt_suffix"));
    if (!errorDir.exists()) {
      errorDir.mkdirs();
    }
    File errorFile = new File(errorDir.toString(), target.getName()+"_eval_errors.html");
    LearnitHtmlOutputWriter writer = new LearnitHtmlOutputWriter(errorFile);
    writer.start();
    writer.startCollapsibleSection("NOT ATTESTED", true);
    //InstanceToMatchInfoMap matchInfos = evalModule.getMatchInfoMap();
    for (RelationAnswer relAns : answers.getRelations()) {
      if (SentenceEntityPairAnswerObserver.relationNameMatch(target.getName(), relAns.getRelationType()) && !relAns.isAttested()) {
	writer.startCollapsibleSection(relAns.toString(), false);
	writer.writeContent(relAns.getArg0().toString());
	writer.writeHtmlContent("<br />");
	writer.writeContent(relAns.getArg1().toString());
	writer.writeHtmlContent("<hr />");

	DocTheory dt = docs.get(relAns.getArg0().getDocid());
	if (dt == null) {
	  writer.writeContent("Couldn't find doc theory from "+docs.keySet());
	} else {
	  SentenceTheory st = dt.sentenceTheory(relAns.getArg0().getSentid());
	  if (st != null) {
	    writer.writeContent("offsets: "+st.span().startCharOffset().value()+"-"+st.span().endCharOffset().value()+"   text: ");
	    writer.writeContent(st.span().tokenizedText().utf16CodeUnits());
	    writer.writeHtmlContent("<a target=\"_blank\" href=\"file:////mercury-04/u18/mshafir/"+
		"source/trunk/Active/Projects/learnit/evaluation/chinese_ace/serifxml_vis/chinese/all/"+
		dt.docid().toString()+".xml-sent-"+st.index()+"-details.html\">serifxml visualization</a><br />");

	    if(offConv instanceof BilingualChineseOffsetConverter && LearnItConfig.optionalParamTrue("sent_builder_dbg_info")) {
	      StringBuilder sb = new StringBuilder();

	      List<String> listTokens = new ArrayList<String>();
	      Iterator<Token> it = st.tokenSequence().iterator();
	      while(it.hasNext()) {
		listTokens.add(it.next().tokenizedText().utf16CodeUnits());
	      }
	      sb.append("------------\n");
	      sb.append("arg0: " + relAns.getArg0().getText() + "\n");
	      sb.append(((BilingualChineseOffsetConverter)offConv).getDebugStringBuilder(relAns.getArg0().getSentid(), relAns.getArg0().getDocid(), listTokens));
	      sb.append("------------\n");

	      sb.append("arg1: " + relAns.getArg1().getText() + "\n");
	      sb.append(((BilingualChineseOffsetConverter)offConv).getDebugStringBuilder(relAns.getArg1().getSentid(), relAns.getArg1().getDocid(), listTokens));
	      sb.append("------------\n");

	      for(String line : sb.toString().split("\n")) {
		writer.writeContent(line);
	      }
	    }


	    for (Spanning arg : st.mentions()) {
	      //if (validSpanning(target,arg)) {
	      if (arg instanceof Mention) {
		Mention marg = (Mention)arg;


		int startToken = marg.atomicHead().span().startToken().index();
		int endToken = marg.atomicHead().span().endToken().index();
		int startChar = arg.span().startCharOffset().value();
		int endChar = arg.span().endCharOffset().value();


		String toWrite = arg.span().tokenizedText()+" ("+startToken+":"+endToken+"), chars: ("+startChar+":"+endChar+")";
		toWrite += " "+marg.entityType()+", "+marg.mentionType();
		writer.writeContent(toWrite);


	      }
	      //}
	    }
	  } else {
	    writer.writeContent("Could not find sentence!");
	  }
	}

	writer.endCollapsibleSection();
      }
    }

    writer.endCollapsibleSection();

    EvalReportMappings evalMappings = evalModule.getEvalMappings(new TargetAndScoreTables(target));

    InstanceToAnswerMapping instance2Answer = evalMappings.getInstance2Answer();
    InstanceToMatchInfoDisplayMap instance2MatchInfoDisplay = evalMappings.getInstance2MatchInfo();
    InstanceToPatternMapping instance2Pattern = evalMappings.getInstance2Pattern();

    writer.startCollapsibleSection("MULTIPLE ATTESTED", true);
    for (RelationAnswer relAns : answers.getRelations()) {
      if (target.getName().toLowerCase().contains(relAns.getRelationType().toLowerCase()) && relAns.getAttestedBy().size() > 1) {
	writer.startCollapsibleSection(relAns.toString(), true);
	for (EvalAnswer evalAns : relAns.getAttestedBy()) {
	  writer.startCollapsibleSection(evalAns.toString(), false);
	  for (InstanceIdentifier instId : instance2Answer.getInstances(evalAns)) {
	    writer.writeHtmlContent(instance2MatchInfoDisplay.getMatchInfoDisplay(instId).html());
	    writer.writeHtmlContent("<br/>");
	  }
	  writer.endCollapsibleSection();
	}
	writer.endCollapsibleSection();
      }
    }
    writer.endCollapsibleSection();

    writer.startCollapsibleSection("CORRECT SINGLE ATTESTED", false);
    for (RelationAnswer relAns : answers.getRelations()) {
      if (target.getName().toLowerCase().contains(relAns.getRelationType().toLowerCase()) && relAns.getAttestedBy().size() == 1) {
	writer.startCollapsibleSection(relAns.toString(), false);
	for (EvalAnswer evalAns : relAns.getAttestedBy()) {
	  writer.startCollapsibleSection(evalAns.toString(), false);
	  for (InstanceIdentifier instId : instance2Answer.getInstances(evalAns)) {
	    writer.writeHtmlContent(instance2MatchInfoDisplay.getMatchInfoDisplay(instId).html());
	    writer.writeHtmlContent("<br/>");
	    writer.startCollapsibleSection("patterns", false);
	    for (LearnitPattern pattern : instance2Pattern.getPatterns(instId)) {
	      writer.writeContent(pattern.toIDString());
	    }
	    writer.endCollapsibleSection();
	    writer.writeHtmlContent("<br/>");
	  }
	  writer.endCollapsibleSection();
	}
	writer.endCollapsibleSection();
      }
    }
    writer.endCollapsibleSection();

    writer.close();
  }

  private static EvaluationModule evaluateMonolingual(
      String apfList, String coreDir, String docList, Target target) throws IOException
  {
      ImmutableList<File> apfs;
    if (new File(apfList).isDirectory()) {
        apfs = ImmutableList.copyOf(new File(apfList).listFiles());
    } else {
        apfs = FileUtils.loadFileList(new File(apfList));
    }

    //If we have serifxml files with gold mentions, we want to align to those.
    if (new File(coreDir,"gold_serifxml").exists()) {
      coreDir += "/gold_serifxml";
    }
    OffsetConverter offConv = new BasicOffsetConverter(coreDir, LearnItConfig.params(),
	!LearnItConfig.optionalParamTrue("use_char_offsets_for_apf"));

    //load gold info
    APFLoader apfLoader = new APFLoader();
    AnswerCollection.Builder answersBuilder = new AnswerCollection.Builder();
    for (File apf : apfs) {
      System.out.println("loading apf "+apf.toString()+"...");
      AnswerFactory.collectAnswers(apfLoader.loadFrom(apf), answersBuilder, offConv);
    }
    AnswerCollection answers = answersBuilder.build();

    System.out.println("Loading serifxml...");

    EvaluationModule evalModule = new EvaluationModule(answers,false);

    MonolingualDocTheoryInstanceLoader docTheoryLoader = evalModule.getDocTheoryLoader(target);
    LoaderUtils.loadFileList(new File(docList), docTheoryLoader);

    writeErrors(target, evalModule.getAnswers(), evalModule, docTheoryLoader.getLoadedDocTheories(), offConv);

    return evalModule;
  }

  private static EvaluationModule evaluateBilingual(
      String apfList, String coreDir, String docList, Target target) throws IOException
  {
    OffsetConverter offConv = new BilingualChineseOffsetConverter(coreDir+"/serifxml/chinese", coreDir+"/rawtext", LearnItConfig.params());

    //load gold info
    APFLoader apfLoader = new APFLoader();
    AnswerCollection.Builder answersBuilder = new AnswerCollection.Builder();
    List<String> docs = OracleAnalysis.getFilenames(docList);
    for (String apfdocname : docs) {
      String apfDoc = coreDir+"/gold/"+apfdocname+".apf.xml";
      System.out.println("loading apf "+apfDoc+"...");
      AnswerFactory.collectAnswers(apfLoader.loadFrom(new File(apfDoc)), answersBuilder, offConv);
    }
    AnswerCollection answers = answersBuilder.build();

    EvaluationModule evalModule = new EvaluationModule(answers,true);
    BilingualDocTheoryInstanceLoader bidocLoader = evalModule.getBilingualDocTheoryLoader(target);

    LoaderUtils.loadVariantBilingualFileList(new File(docList), bidocLoader);

    writeErrors(target, evalModule.getAnswers(), evalModule, bidocLoader.getLoadedDocTheories(), offConv);

    return evalModule;
  }


    private static void evaluateOldStyle(String target, String serifxmlDir, String elfDir, String apfDir, String outputFile) throws IOException {

    OffsetConverter apfConv, elfConv;

    if (LearnItConfig.optionalParamTrue("bilingual")) {
      apfConv = new BilingualChineseOffsetConverter(serifxmlDir, LearnItConfig.get("rawtext_dir"), LearnItConfig.params(), ".segment.xml");
      elfConv = new BilingualChineseOffsetConverter(serifxmlDir, LearnItConfig.get("rawtext_dir"), LearnItConfig.params(), ".xml");
    } else {
      apfConv = new BasicOffsetConverter(serifxmlDir, LearnItConfig.params(), !LearnItConfig.optionalParamTrue("use_char_offsets_for_apf"));
      elfConv = new BasicOffsetConverter(serifxmlDir, LearnItConfig.params());
    }

        ImmutableList<File> elfs;
    if (new File(apfDir).isDirectory()) {
        elfs = ImmutableList.copyOf(new File(elfDir).listFiles());
    } else {
        elfs = FileUtils.loadFileList(new File(elfDir));
    }
    ElfLoader elfLoader = new ElfLoader();
    AnswerCollection.Builder elfAnswersBuilder = new AnswerCollection.Builder();
    for (File elf : elfs) {
      System.out.println("Loading ELF "+elf.toString()+"...");
      AnswerFactory.collectAnswers(elfLoader.loadFrom(elf), elfAnswersBuilder, elfConv);
    }
    AnswerCollection elfAnswers = elfAnswersBuilder.build();
    System.out.println("ELF answers: " + elfAnswers.getRelations().size());

        ImmutableList<File> apfs;
    if (new File(apfDir).isDirectory()) {
        apfs = ImmutableList.copyOf(new File(apfDir).listFiles());
    } else {
        apfs = FileUtils.loadFileList(new File(apfDir));
    }
    APFLoader apfLoader = new APFLoader();
    AnswerCollection.Builder apfAnswersBuilder = new AnswerCollection.Builder();
    for (File apf : apfs) {
      try {
	System.out.println("Loading APF "+apf.toString()+"...");
	AnswerFactory.collectAnswers(apfLoader.loadFrom(apf), apfAnswersBuilder, apfConv);
      } catch (Exception ex) {
	System.out.println("Failed to load "+apf);
	ex.printStackTrace();
      }
    }
    AnswerCollection apfAnswers = apfAnswersBuilder.build();
    System.out.println("APF answers: " + apfAnswers.getRelations().size());

    PrematchedEvaluationModule evalModule = new PrematchedEvaluationModule(elfAnswers, apfAnswers);

    MonolingualDocTheoryInstanceLoader docTheoryLoader = evalModule.getDocTheoryLoader(TargetFactory.fromNamedString(target));

    if (LearnItConfig.defined("test_set_serifxmls")) {
        LoaderUtils.loadFileList(FileUtils.loadFileList(new File(LearnItConfig.get("test_set_serifxmls"))), docTheoryLoader);
    } else {
        LoaderUtils.loadFileList(ImmutableList.copyOf(new File(serifxmlDir).listFiles()), docTheoryLoader);
    }

    for (RelationAnswer relA : elfAnswers.getRelations()) {
      if (!relA.isAttested()) {
	System.out.println(relA.toString());
	System.out.println();
      }
    }

    InstanceToAnswerMapping elfInstances = evalModule.getSystemAnswerMapping();
    InstanceToAnswerMapping apfInstances = evalModule.getGoldAnswerMapping();

    System.out.println("ELF answers: " + elfInstances.getGoldAnswers().size());
    System.out.println("APF answers: " + apfInstances.getGoldAnswers().size());


    int tp = 0;
    final Map<String, Set<EvalAnswer>> tpByPat = new HashMap<String, Set<EvalAnswer>>();
    final Map<String, Set<EvalAnswer>> totalByPat = new HashMap<String, Set<EvalAnswer>>();
    final Map<EvalAnswer,EvalAnswer> matched = new HashMap<EvalAnswer,EvalAnswer>();

    //This is sorta hacky....
    //Find APF answers that have corresponding answers from the ELF
    for (EvalAnswer gold : apfInstances.getGoldAnswers()) {
      for (InstanceIdentifier goldInstance : apfInstances.getInstances(gold)) {
	EvalAnswer system = elfInstances.getAnswer(goldInstance);
	if(system.isCorrect()) {
	  ++tp;
	  for (String brandy : system.getBrandyMatches()) {
	    if (!tpByPat.containsKey(brandy)) {
	      tpByPat.put(brandy, new HashSet<EvalAnswer>());
	    }
	    tpByPat.get(brandy).add(system);
	  }
	  matched.put(gold,system);
	  break;
	}
      }
    }
    System.out.println();
    for (EvalAnswer system : elfInstances.getGoldAnswers()) {
      for (String brandy : system.getBrandyMatches()) {
	if (!totalByPat.containsKey(brandy)) {
	  totalByPat.put(brandy, new HashSet<EvalAnswer>());
	}
	totalByPat.get(brandy).add(system);
      }
    }

    //Overall precision/recall scores
    int fp = elfInstances.getGoldAnswers().size() - tp;
    int fn = apfInstances.getGoldAnswers().size() - tp;

    double precision = (100.0 * tp) / (tp+fp);
    double recall    = (100.0 * tp) / (tp+fn);
    double fscore;
    if (precision+recall > 0)
      fscore = (2*precision*recall)/(precision+recall);
    else
      fscore = 0.0;

    System.out.printf("Precision: %.2f%%\nRecall: %.2f%%\nF-Score: %.2f%%\n",precision,recall,fscore);

    //Order patterns by number of matches
    List<String> orderedPatterns = new ArrayList<String>(totalByPat.keySet());
    Collections.sort(orderedPatterns, new Comparator<String>() {

      @Override
      public int compare(String o1, String o2) {
	if (totalByPat.get(o2).size() == totalByPat.get(o1).size()) {
	  int c1 = tpByPat.containsKey(o1) ? tpByPat.get(o1).size() : 0;
	  int c2 = tpByPat.containsKey(o2) ? tpByPat.get(o2).size() : 0;
	  return c2 - c1;
	}
	else
	  return totalByPat.get(o2).size() - totalByPat.get(o1).size();
      }
    });

    //Pattern-by-pattern precision/recall scores
    for (String pattern : orderedPatterns) {
      int patTP = tpByPat.containsKey(pattern) ? tpByPat.get(pattern).size() : 0;
      int patFP = totalByPat.get(pattern).size() - patTP;
      int patFN = apfInstances.getGoldAnswers().size() - patTP;

      double patPrec = (100.0 * patTP) / (patTP+patFP);
      double patRec  = (100.0 * patTP) / (patTP+patFN);
      double patFScore;
      if (patPrec+patRec > 0)
	patFScore = (2*patPrec*patRec)/(patPrec+patRec);
      else
	patFScore = 0.0;

      Pattern serifPat = PatternSetFactory.fromSexp(SexpReader.readSexp(pattern)).getFirstPattern();

      System.out.println(new BrandyToLearnItStringPatternConverter().convert(serifPat));
      System.out.println("Matches: " + totalByPat.get(pattern).size());
      System.out.printf("Precision: %.2f%%    Recall: %.2f%%    F-Score: %.2f%%\n\n",patPrec,patRec,patFScore);
    }

    //HTML output
    LearnitHtmlOutputWriter htmlWriter = new LearnitHtmlOutputWriter(new File(outputFile));
    htmlWriter.start();
    htmlWriter.writeHtmlContent(String.format("<h3>%s</h3>",target));
    htmlWriter.writeHtmlContent(String.format("<p><strong>Precision: %.2f%%&nbsp;&nbsp;&nbsp;&nbsp;Recall: %.2f%%&nbsp;&nbsp;&nbsp;&nbsp;F-Score: %.2f%%</strong></p>",
	precision,recall,fscore));
    //Show unmatched instances. TODO: Generate contexts that could match these?
    htmlWriter.startCollapsibleSection("Unmatched Instances", false);
    for (EvalAnswer relA : apfInstances.getGoldAnswers()) {
      if (!matched.containsKey(relA)) {
	htmlWriter.startCollapsibleSection(relA.toPrettyString(), true);
	for (InstanceIdentifier instId : apfInstances.getInstances(relA)) {
	  htmlWriter.writeHtmlContent(evalModule.getMatchInfoMap().getMatchInfoDisplay(instId).getLanguageMatchInfoDisplay("English").html());
	  htmlWriter.writeHtmlContent("<br/><br/>");
	}
	htmlWriter.endCollapsibleSection();
      }
    }
    htmlWriter.endCollapsibleSection();
    htmlWriter.writeHtmlContent("<br/>");
    //Show matched instances and the patterns that matched them
    htmlWriter.startCollapsibleSection("Matched Instances", false);
    for (EvalAnswer relA : matched.values()) {
      htmlWriter.startCollapsibleSection(relA.toPrettyString(), true);
      htmlWriter.startCollapsibleSection("Matched by:", false);
      for (String pattern : relA.getBrandyMatches()) {
	htmlWriter.writeContent(new BrandyToLearnItStringPatternConverter().convert(PatternSetFactory.fromSexp(SexpReader.readSexp(pattern)).getFirstPattern()));
      }
      htmlWriter.endCollapsibleSection();
      htmlWriter.writeHtmlContent("<br/><br/>");
      for (InstanceIdentifier instId : apfInstances.getInstances(relA)) {
	htmlWriter.writeHtmlContent(evalModule.getMatchInfoMap().getMatchInfoDisplay(instId).getLanguageMatchInfoDisplay("English").html());
	htmlWriter.writeHtmlContent("<br/><br/>");
      }
      htmlWriter.endCollapsibleSection();
    }
    htmlWriter.endCollapsibleSection();
    htmlWriter.writeHtmlContent("<br/><hr/><br/>");
    //Pattern per pattern results. Split good and bad matches.
    for (String pattern : orderedPatterns) {
      int numGood = tpByPat.containsKey(pattern) ? tpByPat.get(pattern).size() : 0;
      int numBad = totalByPat.get(pattern).size() - numGood;
      htmlWriter.startCollapsibleSection(new BrandyToLearnItStringPatternConverter().convert(PatternSetFactory.fromSexp(SexpReader.readSexp(pattern)).getFirstPattern()), false);
      htmlWriter.writeHtmlContent(String.format("Precision: %.2f%%<br/>",((100.0 * numGood) / (numGood + numBad))));
      htmlWriter.startCollapsibleSection(String.format("Good Matches (%d)",numGood), false);
      if (tpByPat.containsKey(pattern)) {
	for (EvalAnswer evalA : tpByPat.get(pattern)) {
	  htmlWriter.startCollapsibleSection(evalA.toPrettyString(), true);
	  for (InstanceIdentifier instId : elfInstances.getInstances(evalA)) {
	    htmlWriter.writeHtmlContent(evalModule.getMatchInfoMap().getMatchInfoDisplay(instId).getLanguageMatchInfoDisplay("English").html());
	    htmlWriter.writeHtmlContent("<br/><br/>");
	  }
	  htmlWriter.endCollapsibleSection();
	}
      } else {
	htmlWriter.writeHtmlContent("None.");
      }
      htmlWriter.endCollapsibleSection();
      htmlWriter.startCollapsibleSection(String.format("Bad Matches (%d)",numBad), false);
      if (tpByPat.containsKey(pattern) && tpByPat.get(pattern).size() == totalByPat.get(pattern).size()) {
	htmlWriter.writeHtmlContent("None.");
      } else {
	for (EvalAnswer evalA : totalByPat.get(pattern)) {
	  if (!tpByPat.containsKey(pattern) || !tpByPat.get(pattern).contains(evalA)) {
	    htmlWriter.startCollapsibleSection(evalA.toPrettyString(), true);
	    for (InstanceIdentifier instId : elfInstances.getInstances(evalA)) {
	      htmlWriter.writeHtmlContent(evalModule.getMatchInfoMap().getMatchInfoDisplay(instId).getLanguageMatchInfoDisplay("English").html());
	      htmlWriter.writeHtmlContent("<br/><br/>");
	    }
	    htmlWriter.endCollapsibleSection();
	  }
	}
      }
      htmlWriter.endCollapsibleSection();
      htmlWriter.endCollapsibleSection();
    }
    htmlWriter.close();
  }
}
