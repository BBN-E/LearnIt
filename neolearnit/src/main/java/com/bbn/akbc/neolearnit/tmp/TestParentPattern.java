package com.bbn.akbc.neolearnit.tmp;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.pattern.SerifPattern;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class TestParentPattern {
    public static void main(String[] args) throws Exception{
        String learnitConfigPath = args[0];
        LearnItConfig.loadParams(new File(learnitConfigPath));

        Mappings mappings = Mappings.deserialize(new File(args[1]),true);
        for(LearnitPattern srcPattern1:mappings.getAllPatterns().elementSet()){
            if(srcPattern1 instanceof SerifPattern){
                SerifPattern srcSerifPattern = (SerifPattern)srcPattern1;
                Set<SerifPattern> children = new HashSet<>();
                for(LearnitPattern dstPattern1:mappings.getAllPatterns().elementSet()){
                    SerifPattern dstSerifPattern = (SerifPattern)dstPattern1;
                    if(srcSerifPattern.isParentOf(dstSerifPattern)){
                        children.add(dstSerifPattern);
                    }
                }
                if(children.size() > 0){
                    System.out.println("For pattern "+srcSerifPattern.toPrettyString()+", we found");
                    for(SerifPattern serifPattern: children){
                        System.out.println(serifPattern.toPrettyString());
                    }
                }
            }
        }
    }
}
