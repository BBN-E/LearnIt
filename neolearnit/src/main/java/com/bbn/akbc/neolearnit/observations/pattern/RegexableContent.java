package com.bbn.akbc.neolearnit.observations.pattern;

import com.bbn.serif.patterns.Pattern;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.MINIMAL_CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public interface RegexableContent {

	public Pattern getPattern();

	public String toPrettyString();

}
