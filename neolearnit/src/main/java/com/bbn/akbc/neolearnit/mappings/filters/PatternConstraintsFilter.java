package com.bbn.akbc.neolearnit.mappings.filters;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.mappings.filters.storage.MemberStorageFilter;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;
import com.bbn.akbc.neolearnit.observations.pattern.BetweenSlotsPattern;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.pattern.PropPattern;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by bmin on 4/29/15.
 */
public class PatternConstraintsFilter implements MappingsFilter {
  private int BETWEEN_SLOTS_PATTERN_MIN_LEXICAL_ITEMS = 1;
  private int BETWEEN_SLOTS_PATTERN_MAX_LEXICAL_ITEMS = 5;

  private int PROP_PATTERN_MIN_LEXICAL_ITEMS = 1;
  private int PROP_PATTERN_MAX_LEXICAL_ITEMS = 4;


  private int PROP_PATTERN_MAX_DEPTH = 3; // TODO: make this parameterizable

  private final boolean mustHaveTwoSlots;

  private Set<LearnitPattern> patternsToKeep;
  private Set<LearnitPattern> patternsToRemove;

  public PatternConstraintsFilter(Set<LearnitPattern> patternsToKeep, Set<LearnitPattern> patternsToRemove) {
    this.patternsToKeep = patternsToKeep;
    this.patternsToRemove = patternsToRemove;
    this.mustHaveTwoSlots = false;
  }

  public PatternConstraintsFilter(Set<LearnitPattern> patternsToKeep, Set<LearnitPattern> patternsToRemove,
                                  boolean mustHaveTwoSlots) {
    this.patternsToKeep = patternsToKeep;
    this.patternsToRemove = patternsToRemove;
    this.mustHaveTwoSlots = mustHaveTwoSlots;
  }

  public void set_prop_pattern_max_depth(int PROP_PATTERN_MAX_DEPTH) {
    this.PROP_PATTERN_MAX_DEPTH = PROP_PATTERN_MAX_DEPTH;
  }

  private boolean isValidPattern(LearnitPattern pattern) {
    if(pattern.toIDString().contains(",") || pattern.toIDString().contains(" and ") || pattern.toIDString().contains(" or ") || pattern.toIDString().contains("said") || pattern.toIDString().contains("<ref>")
            || pattern.toIDString().contains("<member>") || pattern.toIDString().contains("comp:"))
      return false;

    if(pattern instanceof LabelPattern)
      return false;

    // these are lists with highest priority
    if(patternsToKeep.contains(pattern))
      return true;
    if(patternsToRemove.contains(pattern))
      return false;

    if(pattern instanceof BetweenSlotsPattern) {
      BetweenSlotsPattern p = (BetweenSlotsPattern) pattern;
      return p.getLexicalItems().size() >= BETWEEN_SLOTS_PATTERN_MIN_LEXICAL_ITEMS && p.getLexicalItems().size() <= BETWEEN_SLOTS_PATTERN_MAX_LEXICAL_ITEMS;
    }
    else if(pattern instanceof PropPattern) {
      PropPattern p = (PropPattern) pattern;

      if(p.depth()>PROP_PATTERN_MAX_DEPTH)
        return false;

      if (p.getLexicalItems().size() < PROP_PATTERN_MIN_LEXICAL_ITEMS || p.getLexicalItems().size() > PROP_PATTERN_MAX_LEXICAL_ITEMS) {
        return false;
      } else return !mustHaveTwoSlots || p.hasExactlyTwoUniqueSlots();
    }
    else {
      return false;
    }
  }

  @Override
  public Mappings makeFiltered(Mappings input) {
    Set<LearnitPattern> relevantPatterns = new HashSet<LearnitPattern>();
    // Do not confuse the 'patterns' above (which are target or user defined patterns) with
    // input.getAllPatterns() which are instances/instantiated patterns
    for (LearnitPattern pattern : input.getAllPatterns()) {
      if (pattern.isCompletePattern()) {
        if(isValidPattern(pattern)) {
          relevantPatterns.add(pattern);
          System.out.println("[KEEP]\t" + pattern.toIDString());
        }
        else {
          System.out.println("[REMOVE]\t" + pattern.toIDString());
        }
      }
      else {
       System.out.println("[REMOVE]\t" + pattern.toIDString());
      }
    }

    // now perform the filtering
    InstanceToPatternMapping newPatternMapping =
        new InstanceToPatternMapping(
            MemberStorageFilter.<InstanceIdentifier,LearnitPattern>fromRightSet(relevantPatterns)
                .filter(input.getInstance2Pattern().getStorage()));

    final Mappings relevantMapping = new RelevantInstancesFilter().makeFiltered( new Mappings(input.getInstance2Seed(), newPatternMapping) );

    //for(final LearnitPattern p : relevantMapping.getAllPatterns()) {
    //	System.out.println(p);
    //}

    return relevantMapping;
  }
}
