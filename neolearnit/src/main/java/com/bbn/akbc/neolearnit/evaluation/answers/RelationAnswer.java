package com.bbn.akbc.neolearnit.evaluation.answers;

import com.bbn.akbc.neolearnit.evaluation.pelf.ElfIndividual;
import com.bbn.akbc.neolearnit.evaluation.pelf.ElfRelation;
import com.bbn.akbc.neolearnit.observations.evaluation.EvalAnswer;
import com.bbn.serif.apf.APFEntityArgument;
import com.bbn.serif.apf.APFRelation;
import com.bbn.serif.apf.APFRelationMention;
import com.bbn.serif.apf.APFSpanning;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RelationAnswer {

	@JsonProperty
	private final String relationType;
	@JsonProperty
	private final SpanningAnswer arg0;
	@JsonProperty
	private final SpanningAnswer arg1;
	@JsonProperty
	private final String text;
	private final Optional<String> brandyMatch; //for evaluating against P-ELFs

	@JsonProperty
	private boolean attested; //whether this answer has been seen in the evaluation process

	private final Set<EvalAnswer> attestedBy;

	@JsonCreator
	private RelationAnswer(
			@JsonProperty("relationType") String relationType,
			@JsonProperty("arg0") SpanningAnswer arg0,
			@JsonProperty("arg1") SpanningAnswer arg1,
			@JsonProperty("text") String text,
			@JsonProperty("attested") boolean attested) {
		this.relationType = relationType;
		this.arg0 = arg0;
		this.arg1 = arg1;
		this.text = text;
		this.brandyMatch = Optional.<String>absent();
		attested = false;
		attestedBy = new HashSet<EvalAnswer>();
	}

	public RelationAnswer(String relationType, SpanningAnswer arg0,
			SpanningAnswer arg1, String text) {
		this.relationType = relationType;
		this.arg0 = arg0;
		this.arg1 = arg1;
		this.text = text;
		attested = false;
		attestedBy = new HashSet<EvalAnswer>();
		brandyMatch = Optional.absent();
	}

	public RelationAnswer(String relationType, SpanningAnswer arg0,
			SpanningAnswer arg1, String text, String brandyMatch) {
		this.relationType = relationType;
		this.arg0 = arg0;
		this.arg1 = arg1;
		this.text = text;
		attested = false;
		attestedBy = new HashSet<EvalAnswer>();
		this.brandyMatch = Optional.of(brandyMatch);
	}

	public String getRelationType() {
		return relationType;
	}

	public Set<EvalAnswer> getAttestedBy() {
		return attestedBy;
	}

	public SpanningAnswer getArg0() {
		return arg0;
	}

	public SpanningAnswer getArg1() {
		return arg1;
	}

	public Optional<String> getBrandyMatch() {
		return brandyMatch;
	}

	public synchronized void setAttested(EvalAnswer evalAnswer) {
		attested = true;
		attestedBy.add(evalAnswer);
	}

	public boolean isAttested() {
		return attested;
	}

	public static RelationAnswer fromAPFRelationMention(APFRelation rel,
			APFRelationMention relMention, Map<String, SpanningAnswer> lookup) {

		// APFSpanning arg1Spanning = relMention.getArgument("Arg-1").get().getMention();
		// APFSpanning arg2Spanning = relMention.getArgument("Arg-2").get().getMention();

	  APFSpanning arg1Spanning = null;
	  if(relMention.getArgument("Arg-1").get() instanceof APFEntityArgument) {
	    arg1Spanning = ((APFEntityArgument)relMention.getArgument("Arg-1").get()).entityMention();
	  }
	  APFSpanning arg2Spanning = null;
	  if(relMention.getArgument("Arg-2").get() instanceof APFEntityArgument) {
	    arg1Spanning = ((APFEntityArgument)relMention.getArgument("Arg-2").get()).entityMention();
	  }

	  SpanningAnswer arg1 = lookup.get(arg1Spanning.getID());
		SpanningAnswer arg2 = lookup.get(arg2Spanning.getID());

		String type = rel.getSubtype();
		return new RelationAnswer(type, arg1, arg2, relMention.getExtent().isPresent() ? relMention.getExtent().get().text : "NO TEXT");
	}

	public static RelationAnswer fromElfRelation(ElfRelation relation, Map<String, SpanningAnswer> lookup) {

		ElfIndividual arg1Individual = relation.getArgs().get(0).getIndividual();
		ElfIndividual arg2Individual = relation.getArgs().get(1).getIndividual();

		SpanningAnswer arg1 = lookup.get(arg1Individual.getId());
		SpanningAnswer arg2 = lookup.get(arg2Individual.getId());

		String type = relation.getName();
		if (!relation.getSource().isEmpty())
			return new RelationAnswer(type, arg1, arg2, relation.getText(), relation.getSource());
		else
			return new RelationAnswer(type, arg1, arg2, relation.getText());
	}

	@Override
	public String toString() {
		return "RelationAnswer [relationType=" + relationType + ", arg0="
				+ arg0 + ", arg1=" + arg1 + ", text=" + text + ", attested=" + attested + "]";
	}



}
