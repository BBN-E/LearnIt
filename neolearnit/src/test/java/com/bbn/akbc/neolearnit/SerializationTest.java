package com.bbn.akbc.neolearnit;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.common.targets.TargetFactory;
import com.bbn.akbc.neolearnit.common.targets.TargetSlot;
import com.bbn.akbc.neolearnit.common.targets.constraints.impl.AtomicMentionConstraint;
import com.bbn.akbc.neolearnit.common.targets.constraints.impl.EntityTypeConstraint;
import com.bbn.akbc.neolearnit.common.targets.constraints.impl.MinEntityLevelConstraint;
import com.bbn.akbc.neolearnit.common.targets.constraints.impl.MustCoreferConstraint;
import com.bbn.akbc.neolearnit.common.targets.constraints.impl.MustNotCoreferConstraint;
import com.bbn.akbc.neolearnit.common.targets.constraints.impl.ValueTypeConstraint;
import com.bbn.akbc.neolearnit.common.targets.properties.TargetProperty;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.storage.StorageUtils;
import com.bbn.serif.theories.Mention;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Unit tests for serialization of different learnit objects
 * @author mshafir
 *
 */
public class SerializationTest {

	@Test
	public void testSeedSerialization() {

		Seed s = Seed.from("english","john","mary");
		testSerialization(s,Seed.class);
		System.out.println();
	}

	@Test
	public void testInstanceSerialization() {

		InstanceIdentifier inst = new InstanceIdentifier("sample_doc", 1,
				2, 5,InstanceIdentifier.SpanningType.Mention, Optional.of(Mention.Type.NAME), "ORG", true,
				11, 17, InstanceIdentifier.SpanningType.Mention,Optional.of(Mention.Type.DESC), "PER", false);

		testSerialization(inst,InstanceIdentifier.class);

		InstanceIdentifier inst2 = new InstanceIdentifier("sample_doc", 1,
				2, 5,InstanceIdentifier.SpanningType.Mention, Optional.of(Mention.Type.DESC), "ORG", true,
				11, 17,InstanceIdentifier.SpanningType.ValueMention, Optional.<Mention.Type>absent(), "Numeric.Money", false);

		testSerialization(inst2,InstanceIdentifier.class);

		System.out.println();
	}

	@Test
	public void testTargetCreation() {
		String dir = "/nfs/mercury-04/u24/mcrivaro/source/trunk/Active/Projects/neolearnit/";

		// TODO: bonan: hard-coded path leads to exception, fixit
		return;

		/*
		try {

			TargetSlot slot0 = new TargetSlot.Builder(0, "mention")
				.withAddedConstraint(new AtomicMentionConstraint(0))
				.withAddedConstraint(new EntityTypeConstraint(0,ImmutableList.of("GPE")))
				.withAddedConstraint(new MinEntityLevelConstraint("ALL",0,"NAME"))
				.build();
			TargetSlot slot1 = new TargetSlot.Builder(1, "mention")
				.withAddedConstraint(new AtomicMentionConstraint(1))
				.withAddedConstraint(new EntityTypeConstraint(1,ImmutableList.of("GPE")))
				.withAddedConstraint(new MinEntityLevelConstraint("ALL",1,"DESC"))
				.withAddedProperty(TargetProperty.makeUseHeadText(1))
				.build();

			Target sample = new Target.Builder("sample")
				.setDescription("{0} samples with {1}")
				.withTargetSlot(slot0).withTargetSlot(slot1)
				.withAddedConstraint(new MustCoreferConstraint())
				.withAddedProperty(TargetProperty.makeSymmetric())
                .withPatternConfidenceCutoff(.4)
				.build();

			Target sampleRead = TargetFactory.fromTargetXMLFile(dir+"sample.target.xml").get(0);
			assertEquals(sample, sampleRead);
			System.out.println("Target \"sample\" correctly parsed.");


			slot0 = new TargetSlot.Builder(0, "mention")
				.withAddedConstraint(new AtomicMentionConstraint(0))
				.withAddedConstraint(new EntityTypeConstraint(0,ImmutableList.of("PER","ORG")))
				.withAddedConstraint(new MinEntityLevelConstraint("ALL",0,"DESC"))
				.withAddedProperty(TargetProperty.makeAllowDescTraining(0))
				.build();
			slot1 = new TargetSlot.Builder(1, "value")
				.withAddedConstraint(new AtomicMentionConstraint(1))
				.withAddedConstraint(new ValueTypeConstraint(1,ImmutableList.of("TIMEX2.TIME")))
				.build();

			Target sample2 = new Target.Builder("sample2")
			.setDescription("{0} samples {1} too")
			.withTargetSlot(slot0).withTargetSlot(slot1)
			.withAddedConstraint(new MustNotCoreferConstraint())
            .withPatternConfidenceCutoff(.4)
			.build();

			Target sample2Read = TargetFactory.fromTargetXMLFile(dir+"sample2.target.xml").get(0);
			assertEquals(sample2, sample2Read);
			System.out.println("Target \"sample2\" correctly parsed.");

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		*/
	}

	private static final String tempFilename = "tmp";

	public <T> void testSerialization(T obj, Class<T> type) {

		try {
			StorageUtils.serialize(new File(tempFilename), obj, false);
			T newObj = StorageUtils.deserialize(new File(tempFilename), type, false);
			assertEquals(obj,newObj);

			StorageUtils.serialize(new File(tempFilename), obj, true);
			T newObj2 = StorageUtils.deserialize(new File(tempFilename), type, true);
			assertEquals(obj,newObj2);

		} catch (JsonParseException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (JsonMappingException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

		System.out.println(obj.toString());

	}


}
