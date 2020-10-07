package com.bbn.akbc.neolearnit.util;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.bue.common.symbols.Symbol;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Set;

public class DumpPatternWithLexicalTerms {



    public static void main(String[] args) throws Exception{
        String paramPath = args[0];
        LearnItConfig.loadParams(new File(paramPath));

        String mappingPath = args[1];
        String outputPath = args[2];
        Mappings mappings = Mappings.deserialize(new File(mappingPath),true);
        GeneralUtils.loadStopWordList();
        Set<String> outputSet = new HashSet<>();

        for(LearnitPattern learnitPattern: mappings.getAllPatterns().elementSet()){
            Set<String> lexicalItems = new HashSet<>();
            for(Symbol b : learnitPattern.getLexicalItems()){
                if(!GeneralUtils.inBlackList(b.toString())){
                    lexicalItems.add(b.toString());
                }
            }
            if(lexicalItems.size()>0){
                outputSet.add(String.format("%s\t%s",learnitPattern.toIDString(),String.join("\t",lexicalItems)));
            }
        }
        BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputPath)));
        for(String out:outputSet){
            bw.write(out+"\n");
        }
        bw.close();
    }
}
