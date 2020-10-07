package com.bbn.akbc.neolearnit.mappings.filters;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.bue.common.symbols.Symbol;
import com.google.common.collect.Sets;

import java.util.HashSet;
import java.util.Set;

public class SubPatternFilter implements MappingsFilter {

    @Override
    public Mappings makeFiltered(Mappings input) {
        for(LearnitPattern srcPattern:input.getAllPatterns().elementSet()){
            Set<Symbol> srcKeywords = srcPattern.getLexicalItems();
            Set<InstanceIdentifier> srcInstanceIdentifier = new HashSet<>(input.getInstancesForPattern(srcPattern));
            for(LearnitPattern dstPattern:input.getAllPatterns().elementSet()){
                if(srcPattern.equals(dstPattern))continue;
                Set<Symbol> dstKeywords = dstPattern.getLexicalItems();
                if(Sets.intersection(srcKeywords,dstKeywords).size()<1){
                    continue;
                }
                Set<InstanceIdentifier> dstInstanceIdentifier = new HashSet<>(input.getInstancesForPattern(dstPattern));
                Set<InstanceIdentifier> instanceIdentifierBoth = Sets.intersection(srcInstanceIdentifier,dstInstanceIdentifier);
                System.out.println(srcPattern.toPrettyString()+"\t"+dstPattern.toPrettyString());
            }
        }
        return input;
    }
}
