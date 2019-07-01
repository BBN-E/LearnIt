package com.bbn.akbc.neolearnit.mappings.filters;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.mappings.filters.storage.MemberStorageFilter;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.pattern.PropPattern;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by bmin on 11/11/16.
 */
public class PatternInformativePropFilter implements MappingsFilter {
    static private int PROP_PATTERN_MIN_LEXICAL_ITEMS = 1;

    private Set<LearnitPattern> patternsToKeep;
    private Set<LearnitPattern> patternsToRemove;

    public PatternInformativePropFilter(Set<LearnitPattern> patternsToKeep, Set<LearnitPattern> patternsToRemove) {
      this.patternsToKeep = patternsToKeep;
      this.patternsToRemove = patternsToRemove;
    }

    private boolean isValidPattern(LearnitPattern pattern) {
      // these are lists with highest priority
      if(patternsToKeep.contains(pattern))
        return true;
      if(patternsToRemove.contains(pattern))
        return false;

      if(pattern instanceof PropPattern) {
        PropPattern p = (PropPattern) pattern;
        if(p.getLexicalItemsWithContent().size()>=PROP_PATTERN_MIN_LEXICAL_ITEMS && !p.getPredicates().isEmpty())
          return true;
        else
          return false;
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
            System.out.println("[keep]\t" + pattern.toIDString());
          }
          else {
            System.out.println("[remove-not-valid-prop]\t" + pattern.toIDString());
          }
        }
        else {
          System.out.println("[remove-not-prop]\t" + pattern.toIDString());
        }
      }

      // now perform the filtering
      InstanceToPatternMapping newPatternMapping =
          new InstanceToPatternMapping(
              MemberStorageFilter.<InstanceIdentifier,LearnitPattern>fromRightSet(relevantPatterns)
                  .filter(input.getInstance2Pattern().getStorage()));

      final Mappings relevantMapping = new RelevantInstancesFilter().makeFiltered( new Mappings(input.getInstance2Seed(), newPatternMapping) );

      return relevantMapping;
    }
  }
