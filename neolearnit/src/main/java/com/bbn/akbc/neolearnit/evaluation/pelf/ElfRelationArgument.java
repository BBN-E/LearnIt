package com.bbn.akbc.neolearnit.evaluation.pelf;

public class ElfRelationArgument {
	private final ElfIndividual individual;
	private final int start;
	private final int end;
	private final String text;
	private final String type;
	private final String role;
	private final int confidenceEnum;

	public ElfRelationArgument(ElfIndividual individual, int start, int end, String text,
			String type, String role, int confidence_enum) {
		this.individual = individual;
		this.start = start;
		this.end = end;
		this.text = text;
		this.type = type;
		this.role = role;
		this.confidenceEnum = confidence_enum;
	}

	public ElfIndividual getIndividual() {
		return individual;
	}

	public int getStart() {
		return start;
	}

	public int getEnd() {
		return end;
	}

	public String getText() {
		return text;
	}

	public String getType() {
		return type;
	}

	public String getRole() {
		return role;
	}

	public int getConfidenceEnum() {
		return confidenceEnum;
	}




}
