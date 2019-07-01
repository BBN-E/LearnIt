package com.bbn.akbc.neolearnit.mappings.filters;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.mappings.filters.storage.MemberStorageFilter;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;

import java.util.HashSet;
import java.util.Set;


public class PatternExactMatchFilter implements MappingsFilter {
  private Set<LearnitPattern> patternsToKeep;

  public PatternExactMatchFilter(Set<LearnitPattern> patternsToKeep) {
    this.patternsToKeep = patternsToKeep;
  }

  private boolean isValidPattern(LearnitPattern pattern) {
    if(patternsToKeep.contains(pattern))
      return true;

    return false;
  }

  @Override
  public Mappings makeFiltered(Mappings input) {
    Set<LearnitPattern> relevantPatterns = new HashSet<LearnitPattern>();

    for (LearnitPattern pattern : input.getAllPatterns()) {
      if (pattern.isCompletePattern()) {
        if(isValidPattern(pattern)) {
          relevantPatterns.add(pattern);
          // System.out.println("[KEEP]\t" + pattern.toIDString());
        }
        else {
          // System.out.println("[REMOVE]\t" + pattern.toIDString());
        }
      }
      else {
        // System.out.println("[REMOVE]\t" + pattern.toIDString());
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
