package com.bbn.akbc.neolearnit.evaluation;

import com.bbn.akbc.neolearnit.evaluation.answers.AnswerCollection;
import com.bbn.akbc.neolearnit.evaluation.answers.EntityAnswer;
import com.bbn.akbc.neolearnit.evaluation.answers.MentionAnswer;
import com.bbn.akbc.neolearnit.evaluation.answers.RelationAnswer;
import com.bbn.akbc.neolearnit.evaluation.answers.SpanningAnswer;
import com.bbn.akbc.neolearnit.evaluation.answers.ValueAnswer;
import com.bbn.akbc.neolearnit.evaluation.answers.ValueMentionAnswer;
import com.bbn.akbc.neolearnit.evaluation.offsets.OffsetConverter;
import com.bbn.akbc.neolearnit.evaluation.pelf.ElfDocument;
import com.bbn.akbc.neolearnit.evaluation.pelf.ElfIndividual;
import com.bbn.akbc.neolearnit.evaluation.pelf.ElfIndividualMention;
import com.bbn.akbc.neolearnit.evaluation.pelf.ElfRelation;
import com.bbn.serif.apf.APFDocument;
import com.bbn.serif.apf.APFEntity;
import com.bbn.serif.apf.APFEntityMention;
import com.bbn.serif.apf.APFRelation;
import com.bbn.serif.apf.APFRelationMention;
import com.bbn.serif.apf.APFSourceFile;
import com.bbn.serif.apf.APFValue;
import com.bbn.serif.apf.APFValueMention;

import java.util.HashMap;
import java.util.Map;

public class AnswerFactory {

	public static void collectAnswers(APFSourceFile source, AnswerCollection.Builder builder, OffsetConverter conv) {
		Map<String, SpanningAnswer> answerLookups = new HashMap<String, SpanningAnswer>();

		for (APFDocument doc : source) {
			String docid = doc.getDocId();


			// Let's put together the coreferences and store mentions in answerLookups
			for (APFEntity entity : doc.getEntities()) {
				EntityAnswer ea = EntityAnswer.fromAPFEntity(entity);


				// for (APFEntityMention mention : entity) {
				for (APFEntityMention mention : entity.getMentions()) {
			    MentionAnswer ma = MentionAnswer.fromAPFMention(docid, conv, mention, ea);
					builder.withAddedCoreference(ea, ma);

					answerLookups.put(ma.getMentionId(), ma);
				}
			}

			for (APFValue value : doc.getValues()) {
				ValueAnswer va = ValueAnswer.fromAPFValue(value);

				// for (APFValueMention mention : value) {
				for (APFValueMention mention : value.getMentions()) {

			    ValueMentionAnswer vma = ValueMentionAnswer.fromAPFValueMention(docid, conv, mention, va);
					builder.withAddedValueGroup(va, vma);

					answerLookups.put(vma.getValueMentionId(), vma);
				}
			}

			// Put together relations
			for (APFRelation relation : doc.getRelations()) {
				for (APFRelationMention relMention : relation.getRelationMentions()) {
					builder.withAddedRelation(RelationAnswer.fromAPFRelationMention(relation, relMention, answerLookups));

				}
			}
		}
	}

	public static void collectAnswers(ElfDocument doc, AnswerCollection.Builder builder, OffsetConverter conv) {

		Map<String, SpanningAnswer> answerLookups = new HashMap<String, SpanningAnswer>();

		String docid = doc.getDocid();


		// Let's put together the coreferences and store mentions in answerLookups
		for (ElfIndividual individual : doc.getIndividuals()) {
			EntityAnswer ea = EntityAnswer.fromElfIndividual(individual);

			for (ElfIndividualMention mention : individual.getMentions()) {
				MentionAnswer ma = MentionAnswer.fromElfIndividualMention(docid, conv, mention, ea);
				builder.withAddedCoreference(ea, ma);

				//answerLookups.put(ma.getMentionId(), ma);
				answerLookups.put(individual.getId(), ma);
			}
		}

		/*
		 *
		 *    MIKE C: THIS IS FOR YOU TO FIGURE OUT!!!
		 *
		for (APFValue value : doc.getValues()) {
			ValueAnswer va = ValueAnswer.fromAPFValue(value);

			for (APFValueMention mention : value) {
				ValueMentionAnswer vma = ValueMentionAnswer.fromAPFValueMention(docid, conv, mention, va);
				builder.withAddedValueGroup(va, vma);

				answerLookups.put(vma.getValueMentionId(), vma);
			}
		}
		*/

		// Put together relations
		for (ElfRelation relation : doc.getRelations()) {

			builder.withAddedRelation(RelationAnswer.fromElfRelation(relation, answerLookups));
		}

	}

}
