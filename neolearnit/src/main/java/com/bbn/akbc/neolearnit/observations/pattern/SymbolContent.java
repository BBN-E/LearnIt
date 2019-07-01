package com.bbn.akbc.neolearnit.observations.pattern;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.patterns.Pattern;
import com.bbn.serif.patterns.TextPattern;
import com.bbn.serif.theories.TokenSequence.Span;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SymbolContent implements RegexableContent {

	@JsonProperty
	private final Symbol text;

	@JsonCreator
	private static SymbolContent from(@JsonProperty("text") Symbol text) {
		return new SymbolContent(text);
	}

	public SymbolContent(Symbol text) {
		this.text = text;
	}

	public SymbolContent(String text) {
		this.text = Symbol.from(text);
	}

	public SymbolContent(Span span) {
		this.text = Symbol.from(span.tokenizedText());
	}

	@Override
	public Pattern getPattern() {
		return (new TextPattern.Builder()).withText(text.toString()).build();
	}

	public Symbol getSymbol() {
		return text;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((text == null) ? 0 : text.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SymbolContent other = (SymbolContent) obj;
		if (text == null) {
			if (other.text != null)
				return false;
		} else if (!text.equals(other.text))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getPattern().toString();
	}

	@Override
	public String toPrettyString() {
		return text.toString();
	}

}
