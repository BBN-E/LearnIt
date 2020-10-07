package com.bbn.akbc.neolearnit.common.matchinfo;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.bilingual.TokenAlignmentTable;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo.LanguageMatchInfo;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.bue.common.StringUtils;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.Token;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.*;

public class MatchInfoDisplay {

	@JsonProperty
	private final Target target;
	@JsonProperty
	private final String primaryLanguage;
	@JsonProperty
	private final Map<String,LanguageMatchInfoDisplay> langDisplays;

    @JsonProperty
    private final TokenAlignmentTable tokenAlignmentTable;

    @JsonProperty
	private final String id(){
    	return "MatchInfoDisplay_" + Integer.toString(this.hashCode());
	}

	private final boolean isValidTransliteration;	// defaults to true

	private static final ImmutableSet<String> capitalizeExclusions;

	static {
		capitalizeExclusions = ImmutableSet.of("of", "the", "a", "at", "by", "for", "in", "from", "into", "on", "to", "up");
	}

	@JsonCreator
	private MatchInfoDisplay(
			@JsonProperty("target")Target target,
                             @JsonProperty("primaryLanguage") String primaryLanguage,
                             @JsonProperty("langDisplays") Map<String, LanguageMatchInfoDisplay> langDisplays,
                             @JsonProperty("isValidTransliteration") int isValidTransliteration,
                             @JsonProperty("tokenAlignmentTable") TokenAlignmentTable tokenAlignmentTable
    ) {
		this.target = target;
		this.primaryLanguage = primaryLanguage;
		this.langDisplays = langDisplays;
        this.tokenAlignmentTable = tokenAlignmentTable;
        this.isValidTransliteration = isValidTransliteration == 1;
	}

	@JsonProperty
    public int isValidTransliteration() {		// we do integer as JSON might have a hard time with serializing boolean
    	return isValidTransliteration ? 1 : 0;
    }

	public static MatchInfoDisplay fromMatchInfo(MatchInfo info, Optional<Map<Symbol, Symbol>> chiEngNameMapping) {
	  try {
	    if (LearnItConfig.optionalParamTrue("bilingual")) {

	      Map<String, LanguageMatchInfoDisplay> langDisplays =
		  new HashMap<String, LanguageMatchInfoDisplay>();
	      for (String language : info.getAllLanguages()) {
		final LanguageMatchInfoDisplay languageInfoDisplay = LanguageMatchInfoDisplay
		    .fromBilingualLanguageMatchInfo(info.getLanguageMatch(language));
		langDisplays.put(language, languageInfoDisplay);
	      }

	      // if we're dealing with chinese-english, then we want to use the chi-eng canonical mapping, to add an eng canonical seed
	      if (langDisplays.containsKey("english") && langDisplays.containsKey("chinese")) {
		final LanguageMatchInfoDisplay chiDisplay = langDisplays.get("chinese");
		final LanguageMatchInfoDisplay engDisplay = langDisplays.get("english");

		if (chiEngNameMapping.isPresent()) {
		  if (chiDisplay.getSeed().isPresent() && engDisplay.getSeed().isPresent()) {
		    final Seed engSeed = engDisplay.getSeed().get();
		    String engSlot0String = engSeed.getSlot(0)
			.toString();        // not final because of the potential use of canonical string below
		    String engSlot1String = engSeed.getSlot(1).toString();

		    final Seed chiSeed = chiDisplay.getSeed().get();
		    final Symbol chiSlot0 = chiSeed.getSlot(0);
		    final Symbol chiSlot1 = chiSeed.getSlot(1);

		    final Symbol chiSlot0Trimmed =
			Symbol.from(chiSlot0.toString().replaceAll("\\s", ""));
		    final Symbol chiSlot1Trimmed =
			Symbol.from(chiSlot1.toString().replaceAll("\\s", ""));

		    // find the canonical strings
		    if (chiEngNameMapping.get().containsKey(chiSlot0Trimmed)) {
		      engSlot0String = chiEngNameMapping.get().get(chiSlot0Trimmed).toString();
		    }
		    if (chiEngNameMapping.get().containsKey(chiSlot1Trimmed)) {
		      engSlot1String = chiEngNameMapping.get().get(chiSlot1Trimmed).toString();
		    }

		    final Seed engCanonicalSeed =
			Seed.from(engSeed.getLanguage(), engSlot0String, engSlot1String);
		    langDisplays.put("english", engDisplay.copyWithCanonicalSeed(engCanonicalSeed));
		  }
		}

		// right now, this does nothing useful. But we can imagine having a chi-chi mapping here to normalize a given chi name string
		if (chiDisplay.getSeed().isPresent()) {
		  final Seed chiSeed = chiDisplay.getSeed().get();
		  final Seed chiCanonicalSeed =
		      Seed.from(chiSeed.getLanguage(), chiSeed.getSlot(0).toString(),
			  chiSeed.getSlot(1).toString());
		  langDisplays.put("chinese", chiDisplay.copyWithCanonicalSeed(chiCanonicalSeed));
		}
	      }

	      return new MatchInfoDisplay(info.getTarget(), info.getPrimaryLanguage(), langDisplays,
                  1, info.getAlignments());

	    } else {

	      Map<String, LanguageMatchInfoDisplay> langDisplays =
		  new HashMap<String, LanguageMatchInfoDisplay>();

	      for (String language : info.getAllLanguages()) {
		langDisplays.put(language, LanguageMatchInfoDisplay
		    .fromMonolingualLanguageMatchInfo(info.getLanguageMatch(language)));
	      }
	      return new MatchInfoDisplay(info.getTarget(), info.getPrimaryLanguage(), langDisplays,
                  1, info.getAlignments());

	    }
	  } catch (Exception e) {
	    e.printStackTrace();
	  }

	  return null; // TODO: bad fix
	}

    public LanguageMatchInfoDisplay getPrimaryLanguageMatch() {
        return langDisplays.get(primaryLanguage);
    }

    @JsonProperty
    public String getPrimaryLanguage() {
        return primaryLanguage;
    }

    public MatchInfoDisplay copyWithTransliteration(int validTransliteration) {
        return new MatchInfoDisplay(target, primaryLanguage, langDisplays, validTransliteration, tokenAlignmentTable);
    }

	public static String sanitize(String input) {
		return input.replace("&", "&amp;").replace(">", "&gt;").replace("<", "&lt;").replace("\n", "<br />").replace("\t", "&nbsp;&nbsp;&nbsp;")
				.replace("@-@", "-").replace("-LRB-","&#40;").replace("-RRB-","&#41;").replace("numbercommontoken", "&#35;");
	}

	public LanguageMatchInfoDisplay getLanguageMatchInfoDisplay(String language) {
		return langDisplays.get(language);
	}

	public Set<String> getAvailableLanguages() {
		return langDisplays.keySet();
	}

	public Target getTarget() {
		return target;
	}

	public String html() {
		StringBuilder builder = new StringBuilder();
		for (String language : langDisplays.keySet()) {
			if (langDisplays.keySet().size() > 1) {
				builder.append("<b>"+language+"</b><br />");
			}
			builder.append(langDisplays.get(language).html()+"<br />");
		}
		return builder.toString();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("<MatchInfoDisplay>\n");
		sb.append(target.toString() + "\n");
		sb.append("primaryLanguage:" + primaryLanguage + "\n");
		for(Map.Entry<String, LanguageMatchInfoDisplay> entry : langDisplays.entrySet()) {
			sb.append(entry.getValue() + "\n");
		}
		sb.append("</MatchInfoDisplay>\n\n");

		return sb.toString();
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class LanguageMatchInfoDisplay {
		@JsonProperty
		private final String language;
		@JsonProperty
		private final String docId;
        @JsonProperty
        private final int sentId;
		@JsonProperty
		private final String visualizationLink;
		@JsonProperty
		private final List<String> sentenceTokens;

		private final Optional<InstanceIdentifier> instance;
		private final Optional<Seed> seed;
		private final Optional<Seed> canonicalSeed;

        @JsonProperty
        private InstanceIdentifier instance() {
            return instance.isPresent() ? instance.get() : null;
        }
        @JsonProperty
        public Seed seed() {
            return seed.isPresent() ? seed.get() : null;
        }
        @JsonProperty
        public Seed canonicalSeed() {
            return canonicalSeed.isPresent() ? canonicalSeed.get() : null;
        }

		@JsonCreator
		private LanguageMatchInfoDisplay(
				@JsonProperty("language") String language,
				@JsonProperty("docId") String docId,
				@JsonProperty("sentId") int sentId,
				@JsonProperty("visualizationLink") String visualizationLink,
				@JsonProperty("sentenceTokens") List<String> sentenceTokens,
				@JsonProperty("instance") InstanceIdentifier instance,
				@JsonProperty("seed") Seed seed,
				@JsonProperty("canonicalSeed") Seed canonicalSeed) {
			this.language = language;
			this.docId = docId;
			this.sentId = sentId;
			this.visualizationLink = visualizationLink;
			this.sentenceTokens = sentenceTokens;
            this.instance = instance == null ? Optional.absent() : Optional.of(instance);

            if (seed == null) {
                this.seed = Optional.absent();
            } else {
				Seed newSeed;
            	if(seed.getStringSlots().size()<2){
					newSeed = Seed.from(seed.getLanguage(),seed.getStringSlots().get(0),"");
				}
				else{
					newSeed = Seed.from(seed.getLanguage(), seed.getStringSlots().get(0), seed.getStringSlots().get(1));

				}
				this.seed = Optional.of(newSeed);
            }

            if (canonicalSeed == null) {
                this.canonicalSeed = Optional.absent();
            } else {
				Seed newSeed;
				if(canonicalSeed.getStringSlots().size()<2){
					newSeed = Seed.from(canonicalSeed.getLanguage(),canonicalSeed.getStringSlots().get(0),"");
				}
				else{
					newSeed = Seed.from(canonicalSeed.getLanguage(), canonicalSeed.getStringSlots().get(0), canonicalSeed.getStringSlots().get(1));

				}
                this.canonicalSeed = Optional.of(newSeed);
            }
		}

        private LanguageMatchInfoDisplay(String language, String docId,int sentId, String visualizationLink, List<String> sentenceTokens) {
            this.language = language;
            this.docId = docId;
            this.sentId = sentId;
            this.visualizationLink = visualizationLink;
            this.sentenceTokens = sentenceTokens;
            this.instance = Optional.absent();
            this.seed = Optional.absent();
            this.canonicalSeed = Optional.absent();
        }

        public LanguageMatchInfoDisplay copyWithCanonicalSeed(final Seed canonicalSeed) {
        	if(!instance.isPresent() && !seed.isPresent()) {
        		return new LanguageMatchInfoDisplay(language, docId, sentId , visualizationLink, sentenceTokens, null, null, canonicalSeed);
        	}
        	else if(!instance.isPresent()) {
        		return new LanguageMatchInfoDisplay(language, docId,sentId, visualizationLink, sentenceTokens, null, seed.get(), canonicalSeed);
        	}
        	else if(!seed.isPresent()) {
        		return new LanguageMatchInfoDisplay(language, docId,sentId, visualizationLink, sentenceTokens, instance.get(), null, canonicalSeed);
        	}
        	else {
        		return new LanguageMatchInfoDisplay(language, docId,sentId, visualizationLink, sentenceTokens, instance.get(), seed.get(), canonicalSeed);
        	}
        }

        private boolean hasInstance() {
            return instance.isPresent();
        }

        public String getSentence() {
            return StringUtils.SpaceJoin.apply(this.sentenceTokens);
        }

        public Optional<Seed> getSeed() {
            return seed;
        }

        public Optional<Seed> getCanonicalSeed() {
            return canonicalSeed;
        }

        public Optional<InstanceIdentifier> getInstance() {
            return instance;
        }

        // use '[' and ']' to denote boundaries of slot0, slot1
        public String getSentenceWithSlotBoundaryMarkups() {
        	if(hasInstance()) {
        		StringBuilder sb = new StringBuilder();

        		for(int i=0; i<sentenceTokens.size(); i++) {
        			if(i>0) {
        				sb.append(" ");
        			}

                    //Special case if the slots have the same start point
                    if (i == instance.get().getSlot0Start() && i == instance.get().getSlot1Start()) {
                        if (instance.get().getSlot0End() > instance.get().getSlot1End()) {
                            sb.append("<s0>");
                            sb.append("<s1>");
                        } else {
                            sb.append("<s1>");
                            sb.append("<s0>");
                        }
                    } else if (i == instance.get().getSlot0Start()) {
                        sb.append("<s0>");
                    } else if (i == instance.get().getSlot1Start()) {
                        sb.append("<s1>");
                    }

                    sb.append(sentenceTokens.get(i));

                    if (i == instance.get().getSlot1End()) {
                        sb.append("</s1>");
                    }
                    if (i == instance.get().getSlot0End()) {
                        sb.append("</s0>");
                    }
        		}

                return sb.toString();
			}

        	else {
        		return getSentence();
        	}
        }

		public static LanguageMatchInfoDisplay fromMonolingualLanguageMatchInfo(LanguageMatchInfo mi) {
			final String docId = mi.getDocTheory().docid().toString();

			String link;
			if (LearnItConfig.defined("eval_dir")) {
				link = LearnItConfig.get("eval_dir")+"/serifxml_vis_sent/" +
					 docId+
					 ".xml-sent-"+mi.getSentTheory().index()+"-details.html";
			} else {
				link = "";
			}

			List<String> tokens = new ArrayList<String>();
			for (Token t : mi.getSentTheory().tokenSequence()) {
				tokens.add(sanitize(t.tokenizedText().utf16CodeUnits()));
			}

            if (mi.hasSlots()) {
                InstanceIdentifier id = InstanceIdentifier.from(mi, false);
                Seed seed = Seed.from(mi, false);

                return new LanguageMatchInfoDisplay(mi.getLanguage(), docId,mi.getSentTheory().sentenceNumber(), link, tokens, id, seed, null);
            } else {
                return new LanguageMatchInfoDisplay(mi.getLanguage(), docId,mi.getSentTheory().sentenceNumber(), link, tokens);
            }
		}

		public static LanguageMatchInfoDisplay fromBilingualLanguageMatchInfo(LanguageMatchInfo mi) {

			final String docId = mi.getDocTheory().docid().toString();

			String link;
			if (LearnItConfig.defined("eval_dir")) {
				link = LearnItConfig.get("eval_dir")+"/serifxml_vis_sent/" +
						docId+
						".xml-sent-"+mi.getSentTheory().index()+"-details.html";
			} else {
				link = "";
			}

			List<String> tokens = new ArrayList<String>();
			for (Token t : mi.getSentTheory().tokenSequence()) {
				tokens.add(sanitize(t.tokenizedText().utf16CodeUnits()));
			}

            if (mi.hasSlots()) {
                InstanceIdentifier id = InstanceIdentifier.from(mi, false);
                Seed seed = Seed.from(mi, false);

                // capitalize english seed if it is of some particular entity type
                if(mi.getLanguage().compareTo("english")==0) {
//                	final ImmutableList<Symbol> newSlots = capitalizeSlots(id, seed);
//                	final Seed newSeed = Seed.from(seed.getLanguage(), newSlots.get(0).toString(), newSlots.get(1).toString());
                	final Seed newSeed = seed;
					return new LanguageMatchInfoDisplay(mi.getLanguage(), docId,mi.getSentTheory().sentenceNumber(), link, tokens, id, newSeed, null);
                }
                else {
                	return new LanguageMatchInfoDisplay(mi.getLanguage(), docId,mi.getSentTheory().sentenceNumber(), link, tokens, id, seed, null);
                }
            } else {
                return new LanguageMatchInfoDisplay(mi.getLanguage(), docId,mi.getSentTheory().sentenceNumber(), link, tokens);
            }
		}

		private static ImmutableList<Symbol> capitalizeSlots(final InstanceIdentifier id, final Seed seed) {
			final ImmutableList.Builder<Symbol> ret = ImmutableList.builder();

			final List<Symbol> slots = seed.getSlots();
			final String slot0EntityType = id.getSlotEntityType(0);
            final String slot1EntityType = id.getSlotEntityType(1);

            if(slot0EntityType.compareTo("PER")==0 || slot0EntityType.compareTo("ORG")==0 || slot0EntityType.compareTo("GPE")==0) {
            	ret.add(Symbol.from(capitalize(slots.get(0).toString(), capitalizeExclusions)));
            }
            else {
            	ret.add(slots.get(0));
            }
            if(slot1EntityType.compareTo("PER")==0 || slot1EntityType.compareTo("ORG")==0 || slot1EntityType.compareTo("GPE")==0) {
            	ret.add(Symbol.from(capitalize(slots.get(1).toString(), capitalizeExclusions)));
            }
            else {
            	ret.add(slots.get(1));
            }

            return ret.build();
		}

		private static String capitalize(final String s, final ImmutableSet<String> exclusions) {
            StringBuffer buffer = new StringBuffer();
			final String[] tokens = s.split(" ");
			for(int i=0; i<tokens.length; i++) {
				if(i>0) {
					buffer.append(" ");
				}
				if(!exclusions.contains(tokens[i]) && Character.isLetter(tokens[i].charAt(0))) {
					final String w = Character.toUpperCase(tokens[i].charAt(0)) + tokens[i].substring(1);
					buffer.append(w);
				}
				else {
					buffer.append(tokens[i]);
				}
			}

			return buffer.toString();
		}

		public String link() {
			return visualizationLink;
		}

		@JsonProperty
		public String html() {
			StringBuilder builder = new StringBuilder();
			boolean capitalize = false;
			for (int i=0;i<sentenceTokens.size();i++) {
			    builder.append("<span class=\"token\">");
                if (hasInstance()) {
                    //Special case if the slots have the same start point
                    if (i == instance.get().getSlot0Start() && i == instance.get().getSlot1Start()) {
                        if (instance.get().getSlot0End() > instance.get().getSlot1End()) {
                            builder.append("<span class=\"slot0\">");
                            builder.append("<span class=\"slot1\">");
                        } else {
                            builder.append("<span class=\"slot1\">");
                            builder.append("<span class=\"slot0\">");
                        }
                        if(	language.compareTo("english")==0 &&
                        	((instance.get().getSlotMentionType(0).isPresent() && instance.get().getSlotMentionType(0).get()==Mention.Type.NAME) ||
                        	 (instance.get().getSlotMentionType(1).isPresent() && instance.get().getSlotMentionType(1).get()==Mention.Type.NAME)) &&
                        	(instance.get().getSlotEntityType(0).compareTo("PER")==0 ||
                        	 instance.get().getSlotEntityType(0).compareTo("ORG")==0 ||
                        	 instance.get().getSlotEntityType(0).compareTo("GPE")==0 ||
                        	 instance.get().getSlotEntityType(1).compareTo("PER")==0 ||
                        	 instance.get().getSlotEntityType(1).compareTo("ORG")==0 ||
                        	 instance.get().getSlotEntityType(1).compareTo("GPE")==0)) {
                        	capitalize = true;
                        }
                    } else if (i == instance.get().getSlot0Start()) {
                        builder.append("<span class=\"slot0\">");
                        if(language.compareTo("english")==0 &&
                        	(instance.get().getSlotMentionType(0).isPresent() && instance.get().getSlotMentionType(0).get()==Mention.Type.NAME) &&
                        	(instance.get().getSlotEntityType(0).compareTo("PER")==0 ||
                        	 instance.get().getSlotEntityType(0).compareTo("ORG")==0) ||
                        	 instance.get().getSlotEntityType(0).compareTo("GPE")==0) {
                        	capitalize = true;
                        }
                    } else if (i == instance.get().getSlot1Start()) {
                        builder.append("<span class=\"slot1\">");
                        if(language.compareTo("english")==0 &&
                        	(instance.get().getSlotMentionType(1).isPresent() && instance.get().getSlotMentionType(1).get()==Mention.Type.NAME) &&
                        	(instance.get().getSlotEntityType(1).compareTo("PER")==0 ||
                        	 instance.get().getSlotEntityType(1).compareTo("ORG")==0 ||
                        	 instance.get().getSlotEntityType(1).compareTo("GPE")==0)) {
                        	capitalize = true;
                        }
                    }
                }
                if(capitalize) {
                	builder.append(capitalize(sentenceTokens.get(i), capitalizeExclusions));
                }
                else if(i==0) {
                    builder.append(capitalize(sentenceTokens.get(i), ImmutableSet.of()));
                }
                else {
                	builder.append(sentenceTokens.get(i));
                }
                if (hasInstance()) {
                    if (i == instance.get().getSlot1End()) {
                        builder.append("</span>");
                        capitalize = false;
                    }
                    if (i == instance.get().getSlot0End()) {
                        builder.append("</span>");
                        capitalize = false;
                    }
                }
                builder.append("</span>");
                builder.append(" ");
			}
            if (hasInstance()) {
                builder.append("<br/>");
                builder.append(String.format("(<span class=\"slot0\">%s</span>, <span class=\"slot1\">%s</span>)",
                        seed.get().getSlot(0), seed.get().getSlot(1)));
            }

            final String s = builder.toString();
            return s;

		}

		public String toString() {
            StringBuilder sb = new StringBuilder();

			sb.append("LanguageMatchInfoDisplay {\n");
			sb.append("  language:" + language + "\n");
			sb.append("  docId:" + docId + "\n");
			sb.append("  visualizationLink:" + visualizationLink + "\n");
			sb.append("  sentenceTokens:" + getSentenceWithSlotBoundaryMarkups() + "\n");
			sb.append("  " + (instance.isPresent()? instance.get().toString() : "ABSENT") + "\n");
			sb.append("  seed:" + (seed.isPresent()? seed.get().toString() : "ABSENT") + "\n");
			sb.append("  canonicalSeed:" + (canonicalSeed.isPresent()? canonicalSeed.get().toString() : "ABSENT") + "\n");
			sb.append("}");

			return sb.toString();
		}

	}



}
