package com.bbn.akbc.neolearnit.common.targets;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.common.targets.constraints.MatchConstraint;
import com.bbn.akbc.neolearnit.common.targets.constraints.impl.*;
import com.bbn.akbc.neolearnit.common.targets.properties.SlotProperty;
import com.bbn.akbc.neolearnit.common.targets.properties.TargetProperty;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.storage.StorageUtils;
import com.bbn.akbc.utility.Pair;
import com.bbn.bue.common.xml.XMLUtils;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class TargetFactory {

	/**
	 * Takes as input an ambiguous string: either a relation "named string"
	 * like "family" or a path to an old style (learnit) target definition
	 * xml file and returns the corresonding target
	 *
	 * TODO: this will need to be changed when we automate the processing of
	 * multiple targets.
	 * @param relationNameOrPath
	 * @return
	 * @throws IOException
	 */
	public static Target fromString(String relationNameOrPath) throws IOException {
		// if relation is specified as a valid file path, load target from xml
		File file = new File(relationNameOrPath);
		if (file.exists()) {
			return TargetFactory.fromTargetXMLFile(relationNameOrPath).get(0);
		} else {
            //Being able to do this here was causing a lot of mischief. Should really only read the JSON when directed
            //to it from a TargetAndScoreTables object

			File xml = new File(LearnItConfig.get("learnit_root")+"/inputs/targets/"+relationNameOrPath+".target.xml");
			//File xml = new File(LearnItConfig.get("learnit_root")+"/inputs/targets/kbp_"+relationNameOrPath+".target.xml");

			File json  = new File(LearnItConfig.get("learnit_root")+"/inputs/targets/json/"+relationNameOrPath+".json");

            if (xml.exists()) {
                if (!json.exists() || json.lastModified() < xml.lastModified()) {
                    Target target = TargetFactory.fromTargetXMLFile(xml.toString()).get(0);
//                  target.serialize(json.getAbsolutePath());
                    return target;
                }
            }

			if (json.exists()) {
				return StorageUtils.deserialize(json, Target.class, false);
			}
			else {
				return TargetFactory.fromNamedString(relationNameOrPath);
			}
		}
	}

	/**
	 * Builds a list of Target objects from a single old style (learnit) target
	 * definition xml file.
	 *
	 * Currently returns a list of targets (despite our convention of only defining
	 * target per xml file) in case we want to (TODO) automate processing of multiple
	 * targets.
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public static List<Target> fromTargetXMLFile(String file) throws IOException {
		List<Target> targets = new ArrayList<Target>();
		String contents = Files.toString(new File(file), Charsets.UTF_8);
		final InputSource in = new InputSource(new StringReader(contents.replaceAll("[\r\n]+", " ")));
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			org.w3c.dom.Document xml = builder.parse(in);

			final Element root = xml.getDocumentElement();

			if (root.getTagName().equals("targets")) {
				for (Node child = root.getFirstChild(); child!=null; child = child.getNextSibling()) {
					if (child instanceof Element) {
						targets.add(TargetFactory.fromElement((Element)child));
					}
				}

			} else {
				System.err.println(String.format("Don't know what to do with %s. Skipping file.",root.getTagName()));
			}
		} catch (ParserConfigurationException e) {
			throw new IOException("Error parsing xml", e);
		} catch (SAXException e) {
			throw new IOException("Error parsing xml", e);
		}

		if (targets.size() == 0) {
			throw new IOException("Error parsing xml: no target nodes found");
		}

		return targets;
	}

	/**
	 * A helper function for fromTargetXMLFile: returns a single Target object
	 * from a Target node in an old style (learnit) target definition xml file.
	 * @param target
	 * @return
	 */
	public static Target fromElement(Element target) {
		Target.Builder builder = new Target.Builder(XMLUtils.requiredAttribute(target, "name"));
		builder.setDescription(XMLUtils.requiredAttribute(target, "description"));

        Optional<Integer> emptySets = XMLUtils.optionalIntegerAttribute(target,TargetProperty.EMPTY_SETS);
        if (emptySets.isPresent() && emptySets.get() == 1)
		    builder.withAddedProperty(new TargetProperty(TargetProperty.EMPTY_SETS));
        Optional<Integer> stopwordPats = XMLUtils.optionalIntegerAttribute(target,TargetProperty.STOPWORD_PATS);
        if (stopwordPats.isPresent() && stopwordPats.get() == 1)
            builder.withAddedProperty(new TargetProperty(TargetProperty.STOPWORD_PATS));
        Optional<Integer> lexicalExpansion = XMLUtils.optionalIntegerAttribute(target,TargetProperty.LEX_EXPANSION);
        if (lexicalExpansion.isPresent() && lexicalExpansion.get() == 1)
            builder.withAddedProperty(new TargetProperty(TargetProperty.LEX_EXPANSION));
        Optional<Integer> simpleProps = XMLUtils.optionalIntegerAttribute(target,TargetProperty.SIMPLE_PROPS);
        if (simpleProps.isPresent() && simpleProps.get() == 1)
            builder.withAddedProperty(new TargetProperty(TargetProperty.SIMPLE_PROPS));

        Optional<Double> patConf = XMLUtils.optionalDoubleAttribute(target, TargetProperty.PATTERN_CONFIDENCE);
        if (patConf.isPresent())
            builder.withPatternConfidenceCutoff(patConf.get());
        else if (LearnItConfig.defined("pattern_confidence_threshold"))
            builder.withPatternConfidenceCutoff(LearnItConfig.getDouble("pattern_confidence_threshold"));
        else
            builder.withPatternConfidenceCutoff(0.4);

		List<Element> slots = new ArrayList<Element>();
		List<Element> slotPairs = new ArrayList<Element>();

		// build up lists of slot xml elements and slot pairs elements
		for (Node child = target.getFirstChild(); child!=null; child = child.getNextSibling()) {
			if (child instanceof Element) {
				String tag = ((Element)child).getTagName();
				if (tag.equals("slot"))
					slots.add((Element)child);
				else if (tag.equals("slot_pair"))
					slotPairs.add((Element)child);
			}
		}

		// add slot specific constraints / properties
		for (Element slot : slots) {
			TargetSlotDefinition targetSlotDef = TargetSlotDefinition.fromElement(slot);
			Integer slotNum = targetSlotDef.slotnum;

			TargetSlot.Builder slotBuilder = new TargetSlot.Builder(slotNum, targetSlotDef.type);

			if (!targetSlotDef.use_best_name) {
				slotBuilder.withAddedProperty(new SlotProperty(slotNum, SlotProperty.USE_HEAD_TEXT));
			}
			if (targetSlotDef.allow_desc_training) {
				slotBuilder.withAddedProperty(new SlotProperty(slotNum, SlotProperty.ALLOW_DESC_TRAINING));
			}

			// TODO: make atomic mention constraint configurable
			slotBuilder.withAddedConstraint(new AtomicMentionConstraint(slotNum));

			List<String> types = targetSlotDef.getTypes();
			if (types.size() >= 1) {
				if (targetSlotDef.type.equals("mention")) {
					slotBuilder.withAddedConstraint(new EntityTypeConstraint(slotNum, ImmutableList.copyOf(types)));
				} else {
					slotBuilder.withAddedConstraint(new ValueTypeConstraint(slotNum, ImmutableList.copyOf(types)));
				}
			}

			if (targetSlotDef.getMentionTypes().size() > 0) {
				if (targetSlotDef.type.equals("mention")) {
					slotBuilder.withAddedConstraint(new MentionTypeConstraint(slotNum, ImmutableList.copyOf(targetSlotDef.getMentionTypes())));
				} else {
					throw new RuntimeException("Cannot define mention type constraint for a value slot!");
				}
			}

			// TODO: make language configurable
			String language = "ALL";
			if (!targetSlotDef.getMinEntityLevel().isEmpty()) {
				if (targetSlotDef.type.equals("mention")) {
					slotBuilder.withAddedConstraint(new MinEntityLevelConstraint(language, slotNum, targetSlotDef.getMinEntityLevel()));
				} else {
					throw new RuntimeException("Cannot define min-entitylevel constraint for a value slot!");
				}
			}

			builder.withTargetSlot(slotBuilder.build());
		}

		// TODO: generalize to case where there is more than one slot pair per target
		if (!slotPairs.isEmpty()) {
			TargetSlotPairDefinition targetSlotPair = TargetSlotPairDefinition.fromElement(slotPairs.get(0));
			if (targetSlotPair.symmetric) {
				builder.withAddedProperty(new TargetProperty(TargetProperty.SYMMETRIC));
			}
			if (targetSlotPair.must_not_corefer) {
				builder.withAddedConstraint(new MustNotCoreferConstraint());
			} else if (targetSlotPair.must_corefer) {
				builder.withAddedConstraint(new MustCoreferConstraint());
			}
			if (targetSlotPair.no_overlap_setting.length() > 0) {
				builder.withAddedConstraint(new NoOverlapConstraint(targetSlotPair.no_overlap_setting));
			}
			if (targetSlotPair.allowed_type_pairs != null) {
				ValidTypePairsConstraint.Builder typePairsBuilder = new ValidTypePairsConstraint.Builder();
				for (Pair<String,String> typePair : targetSlotPair.allowed_type_pairs) {
					typePairsBuilder.withAddedPair(typePair);
				}
				builder.withAddedConstraint(typePairsBuilder.build());
			}
		}

		return builder.build();
	}

	public static Target fromNamedString(String name) {
		if (name.toLowerCase().equals("all")){
			return makeEverythingTargetWithNoConstraints();
		} else if (name.toLowerCase().equals("unary_entity")){
			return makeUnaryEntityTarget(); // TODO: implement this
		} else if(name.toLowerCase().equals("unary_event")) {
			return makeUnaryEventTarget();
		} else if (name.toLowerCase().equals("binary_event_event")) {
			return makeBinaryEventEventTarget();
		} else if(name.toLowerCase().equals("binary_entity_entity")){
			return makeBinaryEntityEntityTarget(); // TODO: implement this
		} else if(name.toLowerCase().equals("binary_event_entity")){
			return makeBinaryEventEntityTarget(); // TODO: implement this
		}
		// Below are not actively used
		else if (name.toLowerCase().equals("all_mention_no_coref_pairs")) {
			return makeAllEntityTypesNoCorefTarget();
		} else if (name.toLowerCase().equals("all_mention_coref_pairs")) {
			return makeAllEntityTypesCorefTarget();
		} else if (name.toLowerCase().equals("all_mention_value_pairs")) {
			return makeAllEntityValueTypesTarget();
		}  else if (name.toLowerCase().equals("everything_no_constraints")) {
			return makeEverythingTargetWithNoConstraints();
		} else if (name.toLowerCase().equals("everything")) {
			return makeEverythingTarget();
		} else if (name.toLowerCase().equals("family")) {
			return makeFamilyTarget();
		} else if (name.toLowerCase().equals("user")) {
			return makeUserOwnerInventor();
		} else if (name.toLowerCase().equals("membership")) {
			return makeMembershipTarget();
		} else if (name.toLowerCase().equals("geographical")) {
			return makePartWholeGeographicalTarget();
		} else if (name.toLowerCase().equals("org-location")) {
			return makeOrgLocation();

		} else if (name.toLowerCase().equals("cause")) {
			return makeEverythingTarget();
		} else if (name.toLowerCase().equals("occurs_before")) {
			return makeEverythingTarget();

//		} else if (name.equals("providesProduct")) {
//			return makeProvidesProduct();
//		} else if (name.equals("providesTo")) {
//			return makeProvidesTo();
//		} else if (name.equals("agreementBetween")) {
//			return makeAgreementBetween();
		}
		else if(name.toLowerCase().equals("all_event_or_entity_pairs")){
			return makeEventEntityTarget();
		}

		else if(name.toLowerCase().equals("unary_and_binary_event")){
			return makeUnaryAndBinaryEventTarget();
		}
		else {
			throw new RuntimeException("Unknown named string "+name);
		}
	}

	public static Target makeFakeTarget() {
		return new Target.Builder("FAKE")
			.withAddedConstraint(new MatchConstraint() {

				@Override
				public boolean valid(MatchInfo match) {
					return false;
				}

				@Override
				public boolean offForEvaluation() {
					return false;
				}

				@Override
				public boolean valid(InstanceIdentifier instanceId,
						Collection<Seed> seeds, Target t) {
					return false;
				}

			})
			.build();
	}

	public static Target makeEverythingTarget() {
		//List<String> allTypes = ImmutableList.of("PER","ORG","GPE","LOC","FAC","WEA","VEH","PRN","TPN");
		List<String> allTypes = ImmutableList.of("PER","ORG","GPE","LOC","FAC","WEA","VEH","PRN","TPN");

		TargetSlot slot0 = new TargetSlot.Builder(0,  "all")
				.build();
		TargetSlot slot1 = new TargetSlot.Builder(1, "all")
				.build();

		return new Target.Builder("EVERYTHING")
				.withTargetSlot(slot0).withTargetSlot(slot1)
				.withAddedConstraint(new AtomicMentionConstraint(0))
				.withAddedConstraint(new AtomicMentionConstraint(1))
				.withAddedConstraint(new EntityTypeOrValueOrEventConstraint(0,allTypes))
				.withAddedConstraint(new EntityTypeOrValueOrEventConstraint(1,allTypes))
				.withAddedConstraint(new MinEntityLevelConstraint("ALL",0,"DESC"))
				.withAddedConstraint(new MinEntityLevelConstraint("ALL", 1, "DESC"))

				// Bonan: make it truely be "everything" at the moment
		    /*
			.withAddedConstraint(new EntityTypeConstraint(0,allTypes))
			.withAddedConstraint(new EntityTypeOrValueConstraint(1,allTypes))
			*/
			.build();
	}

	public static Target makeEverythingTargetWithNoConstraints() {
		TargetSlot slot0 = new TargetSlot.Builder(0,  "all")
				.build();
		TargetSlot slot1 = new TargetSlot.Builder(1, "all")
				.build();
		return new Target.Builder("EVERYTHING_NO_CONSTRAINTS")
				.withTargetSlot(slot0).withTargetSlot(slot1).build();
	}

	public static Target makeBinaryEventEventTarget() {
		TargetSlot slot0 = new TargetSlot.Builder(0,  "all")
				.build();
		TargetSlot slot1 = new TargetSlot.Builder(1, "all")
				.build();

		return new Target.Builder("EventEvent")
				.withTargetSlot(slot0).withTargetSlot(slot1)
				.withAddedConstraint(new EventMentionOnlyConstraint(0))
				.withAddedConstraint(new EventMentionOnlyConstraint(1))
				.build();
	}

	public static Target makeBinaryEntityEntityTarget() {
		throw new NotImplementedException();
	}

	public static Target makeBinaryEventEntityTarget() {
		throw new NotImplementedException();
	}

	public static Target makeEventEntityTarget(){
		TargetSlot slot0 = new TargetSlot.Builder(0,  "all")
				.build();
		TargetSlot slot1 = new TargetSlot.Builder(1, "all")
				.build();

		List<String> entityTypes = ImmutableList.of("PER","ORG","GPE","LOC","FAC","WEA","VEH","PRN","TPN");

		return new Target.Builder("EventEntity")
				.withTargetSlot(slot0).withTargetSlot(slot1)
				.withAddedConstraint(new EventOrEntityConstraint(0,entityTypes))
				.withAddedConstraint(new EventOrEntityConstraint(1,entityTypes))
				.build();
	}

	public static Target makeUnaryAndBinaryEventTarget(){
		TargetSlot slot0 = new TargetSlot.Builder(0,"all").build();
		TargetSlot slot1 = new TargetSlot.Builder(1,"all").build();
		return new Target.Builder("UnaryAndBinaryEvent").withTargetSlot(slot0).withTargetSlot(slot1)
				.withAddedConstraint(new EventMentionOnlyConstraint(0,false))
				.withAddedConstraint(new EventMentionOnlyConstraint(1,true))
				.build();
	}

	public static Target makeUnaryEventTarget(){
		TargetSlot slot0 = new TargetSlot.Builder(0,"all").build();
		TargetSlot slot1 = new TargetSlot.Builder(1,"all").build();
		return new Target.Builder("UnaryEvent").
                withTargetSlot(slot0).
                withTargetSlot(slot1)
				.withAddedConstraint(new EventMentionOnlyConstraint(0))
				.withAddedConstraint(new EmptySlotConstraint(1)).build()
                ;
	}

	public static Target makeUnaryEntityTarget(){
		throw new NotImplementedException();
	}

	public static Target makeEverythingExceptEventTarget() {
    List<String> allTypes = ImmutableList.of("PER","ORG","GPE","LOC","FAC","WEA","VEH","PRN","TPN");

    return new Target.Builder("EVERYTHING")
			.withAddedConstraint(new AtomicMentionConstraint(0))
			.withAddedConstraint(new AtomicMentionConstraint(1))
			.withAddedConstraint(new EntityTypeConstraint(0,allTypes))
			.withAddedConstraint(new EntityTypeOrValueConstraint(1,allTypes))
			.withAddedConstraint(new MinEntityLevelConstraint("ALL",0,"DESC"))
			.withAddedConstraint(new MinEntityLevelConstraint("ALL", 1, "DESC"))
	.build();
  }

	public static Target makeNaiveEverythingTarget() {
	  List<String> allTypes = ImmutableList.of("PER","ORG","GPE","LOC","FAC","WEA","VEH","PRN","TPN");

	  return new Target.Builder("EVERYTHING")
	      // Bonan: make it truely be "everything"
			      /**/
	      .build();
	}


  public static Target makeAllEntityTypesTarget() {
		List<String> allTypes = ImmutableList.of("PER","ORG","GPE","LOC","FAC","WEA","VEH","PRN","TPN");

		return new Target.Builder("ALL_MENTION_PAIRS")
			.withAddedConstraint(new AtomicMentionConstraint(0))
			.withAddedConstraint(new AtomicMentionConstraint(1))
			.withAddedConstraint(new EntityTypeConstraint(0,allTypes))
			.withAddedConstraint(new EntityTypeConstraint(1,allTypes))
			.withAddedConstraint(new MinEntityLevelConstraint("ALL",0,"DESC"))
			.withAddedConstraint(new MinEntityLevelConstraint("ALL",1,"DESC"))
			.build();
	}

	public static Target makeAllEntityTypesNoCorefTarget() {
		List<String> allTypes = ImmutableList.of("PER","ORG","GPE","LOC","FAC","WEA","VEH","PRN","TPN");

		return new Target.Builder("ALL_MENTION_NO_COREF_PAIRS")
			.withAddedConstraint(new AtomicMentionConstraint(0))
			.withAddedConstraint(new AtomicMentionConstraint(1))
			.withAddedConstraint(new EntityTypeConstraint(0,allTypes))
			.withAddedConstraint(new EntityTypeConstraint(1,allTypes))
			.withAddedConstraint(new MinEntityLevelConstraint("ALL",0,"DESC"))
			.withAddedConstraint(new MinEntityLevelConstraint("ALL",1,"DESC"))
			.withAddedConstraint(new MustNotCoreferConstraint())
			.build();
	}

	public static Target makeAllEntityTypesCorefTarget() {
		List<String> allTypes = ImmutableList.of("PER","ORG","GPE","LOC","FAC","WEA","VEH","PRN","TPN");

		return new Target.Builder("ALL_MENTION_COREF_PAIRS")
			.withAddedConstraint(new AtomicMentionConstraint(0))
			.withAddedConstraint(new AtomicMentionConstraint(1))
			.withAddedConstraint(new EntityTypeConstraint(0,allTypes))
			.withAddedConstraint(new EntityTypeConstraint(1,allTypes))
			.withAddedConstraint(new MinEntityLevelConstraint("ALL",0,"DESC"))
			.withAddedConstraint(new MinEntityLevelConstraint("ALL",1,"DESC"))
			.withAddedConstraint(new MustCoreferConstraint())
			.build();
	}

	public static Target makeAllEntityValueTypesTarget() {
		List<String> allTypes = ImmutableList.of("PER","ORG","GPE","LOC","FAC","WEA","VEH","PRN","TPN");

		return new Target.Builder("ALL_MENTION_VALUE_PAIRS")
			.withAddedConstraint(new AtomicMentionConstraint(0))
			.withAddedConstraint(new AtomicMentionConstraint(1))
			.withAddedConstraint(new EntityTypeConstraint(0,allTypes))
			.withAddedConstraint(new ValueTypeConstraint(1,ImmutableList.of("all")))
			.withAddedConstraint(new MinEntityLevelConstraint("ALL",0,"DESC"))
			.withAddedConstraint(new MustNotCoreferConstraint())
			.build();
	}

	public static Target makeFamilyTarget() {

		TargetSlot slot0 = new TargetSlot.Builder(0, "mention")
			.withAddedConstraint(new AtomicMentionConstraint(0))
			.withAddedConstraint(new EntityTypeConstraint(0,"PER"))
			.withAddedConstraint(new MinEntityLevelConstraint("ALL",0,"DESC"))
			.build();
		TargetSlot slot1 = new TargetSlot.Builder(1, "mention")
			.withAddedConstraint(new AtomicMentionConstraint(1))
			.withAddedConstraint(new EntityTypeConstraint(1,"PER"))
			.withAddedConstraint(new MinEntityLevelConstraint("ALL",1,"DESC"))
			.build();

		return new Target.Builder("Family")
			.withAddedProperty(TargetProperty.makeSymmetric())
			.withTargetSlot(slot0).withTargetSlot(slot1)
			.withAddedConstraint(new MustNotCoreferConstraint())
			.build();
	}


	public static Target makeAllTacSlotsNoCorefTarget() {
		List<String> slot0Types = ImmutableList.of("PER","ORG");
		List<String> slot1Types = ImmutableList.of("PER","ORG","GPE","LOC");

		TargetSlot slot0 = new TargetSlot.Builder(0, "mention")
				.withAddedConstraint(new AtomicMentionConstraint(0))
				.withAddedConstraint(new EntityTypeConstraint(0,slot0Types))
				.withAddedConstraint(new MinEntityLevelConstraint("ALL",0,"DESC"))
				.build();
		TargetSlot slot1 = new TargetSlot.Builder(1, "mention")
				.withAddedConstraint(new AtomicMentionConstraint(1))
				.withAddedConstraint(new EntityTypeConstraint(1,slot1Types))
				.withAddedConstraint(new MinEntityLevelConstraint("ALL",1,"DESC"))
				.build();

		return new Target.Builder("AllTacSlotsNoCoref")
				.withTargetSlot(slot0).withTargetSlot(slot1)
				.withAddedConstraint(new MustNotCoreferConstraint())
				.build();
	}

	public static Target makeAllTacSlotsCorefTarget() {
		List<String> slot0Types = ImmutableList.of("PER","ORG");
		List<String> slot1Types = ImmutableList.of("PER","ORG");

		TargetSlot slot0 = new TargetSlot.Builder(0, "mention")
				.withAddedConstraint(new AtomicMentionConstraint(0))
				.withAddedConstraint(new EntityTypeConstraint(0,slot0Types))
				.withAddedConstraint(new MinEntityLevelConstraint("ALL",0,"DESC"))
				.build();
		TargetSlot slot1 = new TargetSlot.Builder(1, "mention")
				.withAddedConstraint(new AtomicMentionConstraint(1))
				.withAddedConstraint(new EntityTypeConstraint(1,slot1Types))
				.withAddedConstraint(new MinEntityLevelConstraint("ALL",1,"DESC"))
				.build();

		return new Target.Builder("AllTacSlotsCoref")
				//.withAddedProperty(TargetProperty.makeSymmetric())
				.withTargetSlot(slot0).withTargetSlot(slot1)
				.withAddedConstraint(new MustCoreferConstraint())
				.build();
	}


	public static Target makeAllTacSlotsValueTarget() {

		List<String> slot0Types = ImmutableList.of("PER","ORG");
		List<String> slot1Types = ImmutableList.of("all");

		TargetSlot slot0 = new TargetSlot.Builder(0, "mention")
				.withAddedConstraint(new AtomicMentionConstraint(0))
				.withAddedConstraint(new EntityTypeConstraint(0,slot0Types))
				.withAddedConstraint(new MinEntityLevelConstraint("ALL",0,"DESC"))
				.build();
		TargetSlot slot1 = new TargetSlot.Builder(1, "value")
				//.withAddedConstraint(new AtomicMentionConstraint(1))
				.withAddedConstraint(new ValueTypeConstraint(1,slot1Types))
				.build();

		return new Target.Builder("AllTacSlotsValue")
				//.withAddedProperty(TargetProperty.makeSymmetric())
				.withTargetSlot(slot0).withTargetSlot(slot1)
				//.withAddedConstraint(new MustNotCoreferConstraint())
				.build();
	}

	public static Target makeUserOwnerInventor() {

		TargetSlot slot0 = new TargetSlot.Builder(0, "mention")
			.withAddedConstraint(new AtomicMentionConstraint(0))
			.withAddedConstraint(new EntityTypeConstraint(0,ImmutableList.of("PER","ORG","GPE")))
			.withAddedConstraint(new MinEntityLevelConstraint("ALL",0,"DESC"))
			.build();
		TargetSlot slot1 = new TargetSlot.Builder(1, "mention")
			.withAddedConstraint(new AtomicMentionConstraint(1))
			.withAddedConstraint(new EntityTypeConstraint(1,ImmutableList.of("WEA","VEH","FAC")))
			.withAddedConstraint(new MinEntityLevelConstraint("ALL",1,"DESC"))
			.build();

		return new Target.Builder("User-Owner-Inventor-Manufacturer")
			.withTargetSlot(slot0).withTargetSlot(slot1)
			.withAddedConstraint(new MustNotCoreferConstraint())
			.build();
	}

	public static Target makeMembershipTarget() {

		TargetSlot slot0 = new TargetSlot.Builder(0, "mention")
			.withAddedConstraint(new AtomicMentionConstraint(0))
			.withAddedConstraint(new EntityTypeConstraint(0,ImmutableList.of("PER","ORG","GPE")))
			.withAddedConstraint(new MinEntityLevelConstraint("ALL",0,"DESC"))
			.build();
		TargetSlot slot1 = new TargetSlot.Builder(1, "mention")
			.withAddedConstraint(new AtomicMentionConstraint(1))
			.withAddedConstraint(new EntityTypeConstraint(1,ImmutableList.of("ORG")))
			.withAddedConstraint(new MinEntityLevelConstraint("ALL",1,"DESC"))
			.build();

		return new Target.Builder("Membership")
			.withTargetSlot(slot0).withTargetSlot(slot1)
			.withAddedConstraint(new MustNotCoreferConstraint())
			.build();
	}

	public static Target makePartWholeGeographicalTarget() {

		TargetSlot slot0 = new TargetSlot.Builder(0, "mention")
			.withAddedConstraint(new AtomicMentionConstraint(0))
			.withAddedConstraint(new EntityTypeConstraint(0,ImmutableList.of("FAC","LOC","GPE")))
			.withAddedConstraint(new MinEntityLevelConstraint("ALL",0,"DESC"))
			.build();
		TargetSlot slot1 = new TargetSlot.Builder(1, "mention")
			.withAddedConstraint(new AtomicMentionConstraint(1))
			.withAddedConstraint(new EntityTypeConstraint(1,ImmutableList.of("FAC","LOC","GPE")))
			.withAddedConstraint(new MinEntityLevelConstraint("ALL",1,"DESC"))
			.build();

		return new Target.Builder("Geographical")
			.withTargetSlot(slot0).withTargetSlot(slot1)
			.withAddedConstraint(new MustNotCoreferConstraint())
			.build();
	}

	public static Target makeOrgLocation() {

		TargetSlot slot0 = new TargetSlot.Builder(0, "mention")
			.withAddedConstraint(new AtomicMentionConstraint(0))
			.withAddedConstraint(new EntityTypeConstraint(0,"ORG"))
			.withAddedConstraint(new MinEntityLevelConstraint("ALL",0,"DESC"))
			.build();
		TargetSlot slot1 = new TargetSlot.Builder(1, "mention")
			.withAddedConstraint(new AtomicMentionConstraint(1))
			.withAddedConstraint(new EntityTypeConstraint(1,ImmutableList.of("LOC", "GPE")))
			.withAddedConstraint(new MinEntityLevelConstraint("ALL",1,"DESC"))
			.build();

		return new Target.Builder("Org-Location")
			.withTargetSlot(slot0).withTargetSlot(slot1)
			.withAddedConstraint(new MustNotCoreferConstraint())
			.build();
	}

//	/*************************************
//	 *  SEC RELATION CLASSES
//	 *************************************/
//
//	public static Target makeProvidesTo() {
//
//		TargetSlot slot0 = new TargetSlot.Builder(0, "mention")
//			.withAddedConstraint(new AtomicMentionConstraint(0))
//			.withAddedConstraint(new EntityTypeConstraint(0,"ORG"))
//			.withAddedConstraint(new MinEntityLevelConstraint("ALL",0,"NAME"))
//			.build();
//		TargetSlot slot1 = new TargetSlot.Builder(1, "mention")
//			.withAddedConstraint(new AtomicMentionConstraint(1))
//			.withAddedConstraint(new EntityTypeConstraint(1,ImmutableList.of("ORG","GPE","PER")))
//			.withAddedConstraint(new MinEntityLevelConstraint("ALL",1,"DESC"))
//			.build();
//
//		return new Target.Builder("providesTo")
//			.withTargetSlot(slot0).withTargetSlot(slot1)
//			.withAddedConstraint(new MustNotCoreferConstraint())
//			.build();
//	}
//
//	public static Target makeProvidesProduct() {
//
//		TargetSlot slot0 = new TargetSlot.Builder(0, "mention")
//			.withAddedConstraint(new AtomicMentionConstraint(0))
//			.withAddedConstraint(new EntityTypeConstraint(0,"ORG"))
//			.withAddedConstraint(new MinEntityLevelConstraint("ALL",0,"NAME"))
//			.build();
//		TargetSlot slot1 = new TargetSlot.Builder(1, "mention")
//			.withAddedConstraint(new AtomicMentionConstraint(1))
//			.withAddedConstraint(new EntityTypeConstraint(1,ImmutableList.of("WEA","VEH","PRN")))
//			.withAddedConstraint(new MinEntityLevelConstraint("ALL",1,"DESC"))
//			.build();
//
//		return new Target.Builder("providesProduct")
//			.withTargetSlot(slot0).withTargetSlot(slot1)
//			.withAddedConstraint(new MustNotCoreferConstraint())
//			.build();
//	}
//
//	public static Target makeAgreementBetween() {
//
//		TargetSlot slot0 = new TargetSlot.Builder(0, "mention")
//			.withAddedConstraint(new AtomicMentionConstraint(0))
//			.withAddedConstraint(new EntityTypeConstraint(0,ImmutableList.of("ORG","GPE","PER")))
//			.withAddedConstraint(new MinEntityLevelConstraint("ALL",0,"NAME"))
//			.build();
//		TargetSlot slot1 = new TargetSlot.Builder(1, "mention")
//			.withAddedConstraint(new AtomicMentionConstraint(1))
//			.withAddedConstraint(new EntityTypeConstraint(1,ImmutableList.of("ORG","GPE","PER")))
//			.withAddedConstraint(new MinEntityLevelConstraint("ALL",1,"NAME"))
//			.build();
//
//		return new Target.Builder("agreementBetween")
//			.withAddedProperty(new TargetProperty("symmetric"))
//			.withTargetSlot(slot0).withTargetSlot(slot1)
//			.withAddedConstraint(new MustNotCoreferConstraint())
//			.build();
//	}
}
