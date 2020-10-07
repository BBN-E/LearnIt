package com.bbn.akbc.neolearnit.tmp;

import com.bbn.akbc.neolearnit.common.BBNInternalOntology;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.scoring.scores.PatternScore;
import com.bbn.akbc.neolearnit.scoring.tables.AbstractScoreTable;
import com.bbn.bue.common.symbols.Symbol;
import com.google.common.collect.Multimap;

import java.io.File;

public class PrintAllLexicalItemsOfExtractor {
    public static void main(String[] args)throws Exception{
        LearnItConfig.loadParams(new File("/home/hqiu/ld100/learnit/params/learnit/runs/cx_16_rdf_sentences.params"));
        String yamlPath = "/home/hqiu/ld100/learnit/inputs/domains/CX_ICM/ontology/binary_event_ontology.yaml";
        String targetPathDir = "/home/hqiu/ld100/learnit/inputs/domains/CX_ICM/extractors/";

        final File ontologyFile = new File(yamlPath);
        BBNInternalOntology.BBNInternalOntologyNode root =  BBNInternalOntology.BBNInternalOntologyNode.fromInternalOntologyFile(ontologyFile);
        Multimap<String, BBNInternalOntology.BBNInternalOntologyNode> childrenNodeMap = root.getPropToNodeMultiMap(new BBNInternalOntology.BBNInternalOntologyNode.PropStringGetter("originalKey"));
        for(String targetName:childrenNodeMap.keySet()){
            if(new File(targetPathDir + targetName+".json").exists()){
                TargetAndScoreTables targetAndScoreTables = TargetAndScoreTables.deserialize(new File(targetPathDir + targetName+".json"));
                for(AbstractScoreTable.ObjectWithScore<LearnitPattern, PatternScore> patternPatternScoreObjectWithScore:targetAndScoreTables.getPatternScores().getObjectsWithScores()){
                    if(patternPatternScoreObjectWithScore.getScore().isGood() && patternPatternScoreObjectWithScore.getScore().isFrozen()){
                        for(Symbol symbol: patternPatternScoreObjectWithScore.getObject().getLexicalItems()){
                            if (symbol.toString().endsWith("[0]") || symbol.toString().endsWith("[1]")) {
                                String s = symbol.toString().substring(0, symbol.toString().length() - 3);
                                if(s.length()>0){
                                    System.out.println(s);
                                }
                            }
                            else{
                                if(symbol.asString().length()>0){
                                    System.out.println(symbol.asString());
                                }
                            }

                        }
//                        System.out.println(patternPatternScoreObjectWithScore.getObject().toPrettyString());
                    }
                }
            }
        }
    }
}
