package com.bbn.akbc.neolearnit.evaluation.pelf;

public class ElfIndividualMention {

	private final boolean isType;
	private final int start;
	private final int end;
	private final String text;
	private final String type;

	public ElfIndividualMention(int start, int end,
			String text, String type) {
		this.isType = true;
		this.start = start;
		this.end = end;
		this.text = text;
		this.type = type;
	}

	public ElfIndividualMention(int start, int end,
			String text) {
		this.isType = false;
		this.start = start;
		this.end = end;
		this.text = text;
		this.type = text;
	}

	public boolean isType() {
		return isType;
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

}
