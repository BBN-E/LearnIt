package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.common.targets.TargetFactory;
import com.bbn.akbc.neolearnit.observations.pattern.LearnItPatternFactory;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Initialize {

	public static List<Seed> loadSeedsXML(File seedFile) {
		List<Seed> result = new ArrayList<Seed>();
		try {
			String contents = Files.toString(seedFile, Charsets.UTF_8);
			final InputSource in = new InputSource(new StringReader(contents.replaceAll("[\r\n]+", " ")));

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			org.w3c.dom.Document xml = builder.parse(in);

			final Element root = xml.getDocumentElement();

			if (root.getTagName().equals("seeds")) {
				for (Node child = root.getFirstChild(); child!=null; child = child.getNextSibling()) {
					if (child instanceof Element) {
						result.add(Seed.from((Element)child));
					}
				}
			} else {
				throw new RuntimeException("Error: invalid seed file");
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException("Error reading seed file");
		}
		return result;
	}

	public static void main(String[] args) throws IOException {
		String params = args[0];
		String seedFilename = args[1];
		String patternFilename = args[2];
		String output = args[3];
		String relation = args[4];

		LearnItConfig.loadParams(new File(params));

		File seedFile = new File(seedFilename);
		List<Seed> initialSeeds;
		if (seedFile.exists() && LearnItConfig.optionalParamTrue("use_seeds_to_initialize")) {
			System.out.println("Loading seeds...");
			initialSeeds = loadSeedsXML(seedFile);
		} else {
			System.out.println("No seeds found. Initializing empty extractor...");
			initialSeeds = new ArrayList<Seed>();
		}

		Target target = TargetFactory.fromXMLPathString(relation);

		File patternFile = new File(patternFilename);
		Set<LearnitPattern> initialPatterns = new HashSet<LearnitPattern>();
		if (patternFile.exists() && LearnItConfig.optionalParamTrue("use_patterns_to_initialize")) {
			System.out.println("Loading patterns...");
			initialPatterns = LearnItPatternFactory.fromFile(patternFile, target);
		}

		String targetPathRel  = String.format("inputs/targets/json/%s.json",relation);
		String targetPathFull = String.format("%s/%s",LearnItConfig.get("learnit_root"),targetPathRel);
		target.serialize(targetPathFull);


		TargetAndScoreTables data = new TargetAndScoreTables(relation);
		data.setInitialSeeds(new HashSet<Seed>(initialSeeds));
		data.setInitialPatterns(initialPatterns);

		//starts off as 0
		data.incrementIteration();
//		data.updateStage("pattern-proposer");
//		data.updateStage("seed-proposer");
		data.updateStage("initializing-stats");

		data.serialize(new File(output));
	}

}
