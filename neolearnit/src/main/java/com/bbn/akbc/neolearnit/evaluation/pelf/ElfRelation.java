package com.bbn.akbc.neolearnit.evaluation.pelf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.common.collect.ImmutableList;

public class ElfRelation {

	private final int start;
	private final int end;
	private final String name;
	private final Double p;
	private final int scoreGroup;
	private final String source;
	private final String text;
	private final List<ElfRelationArgument> args;

	private ElfRelation(int start, int end, String name, Double p,
			int score_group, String source, String text,
			List<ElfRelationArgument> args) {
		this.start = start;
		this.end = end;
		this.name = name;
		this.p = p;
		this.scoreGroup = score_group;
		this.source = source;
		this.text = text;
		this.args = ImmutableList.copyOf(args);
	}

	public static class Builder {

		private final int start;
		private final int end;
		private final String name;
		private final Double p;
		private final int score_group;
		private final String source;
		private final String text;
		private final List<ElfRelationArgument> args;

		public Builder(int start, int end, String name, Double p,
				int score_group, String source, String text) {
			this.start = start;
			this.end = end;
			this.name = name;
			this.p = p;
			this.score_group = score_group;
			this.source = source;
			this.text = text;
			this.args = new ArrayList<ElfRelationArgument>();
		}

		public ElfRelation build() {
			Collections.sort(args, new Comparator<ElfRelationArgument>() {

				@Override
				public int compare(ElfRelationArgument arg0,
						ElfRelationArgument arg1) {
					return arg0.getRole().compareTo(arg1.getRole());
				}

			});

			return new ElfRelation(start,end,name,p,score_group,source,text,args);
		}

		public Builder withAddArg(ElfRelationArgument arg) {
			this.args.add(arg);
			return this;
		}
	}

	public int getStart() {
		return start;
	}

	public int getEnd() {
		return end;
	}

	public String getName() {
		return name;
	}

	public Double getP() {
		return p;
	}

	public int getScoreGroup() {
		return scoreGroup;
	}

	public String getSource() {
		return source;
	}

	public String getText() {
		return text;
	}

	public List<ElfRelationArgument> getArgs() {
		return args;
	}

}
