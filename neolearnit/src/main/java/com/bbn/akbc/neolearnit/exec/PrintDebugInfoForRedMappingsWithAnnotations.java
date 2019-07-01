package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.utility.Pair;
import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import java.io.*;
import java.util.*;

public class PrintDebugInfoForRedMappingsWithAnnotations {

    static Set<String> docIdsInMappings = new HashSet<String>();
    static Set<String> uniqueInstancesInMappings = new HashSet<String>();
    static Set<String> uniqueVerbPairsAnnotated = new HashSet<String>();
    static Map<String,Set<String>> missedAnnotations = new HashMap<String,Set<String>>();
    static int numMissedAnnotations = 0;
    static Set<String> sectionsWithoutLabelDueToUnmatchedSentence = new HashSet<String>();
    static Set<String> sectionIdsWithLabels = new HashSet<String>();

    public static void main(String [] args) throws IOException {
        String mappingsDebugOutput = args[0];
        String annotationsFile = args[1];
        String outputFile = args[2];

        BufferedReader mappingsDebug = new BufferedReader(new InputStreamReader(new FileInputStream(
                mappingsDebugOutput
        )));
        BufferedReader annotations = new BufferedReader(new InputStreamReader(new FileInputStream(
                annotationsFile
        )));
        PrintWriter output = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outputFile)));

        List<String> sectionIds = new ArrayList<String>();
        Map<String,String> sections = new HashMap<String, String>();
        Map<String,String> sectionIdToDocIdVerbPair = new HashMap<String, String>();
        Map<String, String> docIdVerbPairToSections = new HashMap<String, String>();
        readMappingsOutput(mappingsDebug, sectionIds, sections, sectionIdToDocIdVerbPair, docIdVerbPairToSections);

        addAnnotationsToSections(annotations, docIdVerbPairToSections);
        //TODO: print output
        for(String sectionId : sectionIds){
            String docIdToVerbPair = sectionIdToDocIdVerbPair.get(sectionId);
            String newSection = docIdVerbPairToSections.get(docIdToVerbPair);
            output.println(newSection+"\n-----------------\n");
        }
        output.println("=======================Missed Instances="+ numMissedAnnotations+" ==================\n");
        for(Map.Entry<String,Set<String>> missedInstances : missedAnnotations.entrySet()){
            for(String instance : missedInstances.getValue()){
                output.println(instance+"\n");
            }
        }
        output.println("=======================Missed Instances (Due to Sentence-Mismatch)="+sectionsWithoutLabelDueToUnmatchedSentence.size()+"==================\n");
        for(String section : sectionsWithoutLabelDueToUnmatchedSentence){
            output.println(section+"\n");
        }
        output.println("=======================Stats===================================================");
        int numUniqueInstances = uniqueInstancesInMappings.size() - sectionsWithoutLabelDueToUnmatchedSentence.size() ;
        int numUniqueVerbPairsAnnotated = uniqueVerbPairsAnnotated.size() - sectionsWithoutLabelDueToUnmatchedSentence.size();
        int numHits = sectionIdsWithLabels.size() - sectionsWithoutLabelDueToUnmatchedSentence.size();
        output.println("Total instances extracted (after subtracting 'mismatched due to exact sentence' instances): "+numUniqueInstances);
        output.println("# instances in annotations (after subtracting 'mismatched due to exact sentence' instances): "+ numUniqueVerbPairsAnnotated);
        output.println("# TP: "+numHits+" # FP: "+
                (numUniqueInstances-numHits)+" # FN: "+(numUniqueVerbPairsAnnotated -numHits));
        output.println("Recall: "+(numHits*1.0/ numUniqueVerbPairsAnnotated)+" Precision: "+(numHits*1.0/numUniqueInstances));
        output.close();
    }

    private static void readMappingsOutput(BufferedReader mappingsFile,
    List<String> sectionIds, Map<String,String> sections, Map<String,String> sectionIdToDocIdVerbPair,
                                           Map<String,String> docIdVerbPairToSections)
    throws IOException{
        String line = null;
        String sectionId = null;
        String docId = null;
        String verb1 = null;
        String verb2 = null;

        List<String> sectionLines = new ArrayList<String>();

        while((line = mappingsFile.readLine())!=null){
            if(line.isEmpty()&&sectionLines.isEmpty()){
                continue;
            }
            if(line.startsWith("-------")){
                //end of section
                String section = Joiner.on("\n").join(sectionLines);
                sections.put(sectionId,section);
                String docIdVerbPair = docId+"<DELIM>"+verb1+"<DELIM>"+verb2;
                docIdVerbPairToSections.put(docIdVerbPair,section);
                sectionIdToDocIdVerbPair.put(sectionId,docIdVerbPair);
                sectionLines = new ArrayList<String>();
            }else if(line.startsWith("Language: ")) {
                sectionId = line;
                sectionLines.add(sectionId);
                uniqueInstancesInMappings.add(sectionId);
                docId = line.substring(line.indexOf("Document: ")+"Document: ".length(),
                        line.lastIndexOf(","));
                docIdsInMappings.add(docId);
                verb1 = sectionId.substring(sectionId.indexOf("verb0=")+"verb0=".length(),sectionId.lastIndexOf("\t"));
                verb2 = sectionId.substring(sectionId.indexOf("verb1=")+"verb1=".length());
                sectionIds.add(sectionId);
            }else{
                sectionLines.add(line);
            }
        }
    }

    private static void addAnnotationsToSections(
        BufferedReader annotationsFile, Map<String,String> docIdVerbPairToSections)
            throws IOException{
        Multimap<String,Pair<String,String>> labelToVerbPair = HashMultimap.create();
        Multimap<Pair<String,String>,String> verbPairToLabel = HashMultimap.create();
        String line = null;
        String docId = null;
        String verb1 = null;
        String verb2 = null;
        String sentence = null;
        String verb1Sentence = null;
        String verb2Sentence = null;
        String label = null;
        boolean isRelationToConsider = false;

        while((line = annotationsFile.readLine())!=null){
            if (line.startsWith("=== ")){
                line = line.substring(4);
                docId = line.substring(0,line.indexOf("\t"));
                if(docId.startsWith("PROXY_")){
                    docId = docId.substring(6);
                }
                if(docId.contains(".")){
                    docId = docId.substring(0,docId.lastIndexOf("."));
                }
                if(!docIdsInMappings.contains(docId)){
                    continue;
                }
                line = line.substring(line.indexOf("\t")+1);
                if(line.startsWith("argument 1:")){
                    try {
                        verb1 = line.substring(line.indexOf("***") + 3, line.lastIndexOf("***"));
                        verb1Sentence = line.substring(line.indexOf(":")+1).trim();
                    }catch(StringIndexOutOfBoundsException e){
                        verb1 = null;
                    }
                }else if(line.startsWith("argument 2:")){
                    if(!isRelationToConsider){
                        continue;
                    }
                    try {
                        verb2 = line.substring(line.indexOf("***") + 3, line.lastIndexOf("***"));
                        verb2Sentence = line.substring(line.indexOf(":")+1).trim();
                    }catch(StringIndexOutOfBoundsException e){
                        verb2 = null;
                    }
                    if(verb1==null || verb2 == null){
                        continue;
                    }
                    sentence = verb2Sentence.replaceAll("\\*\\*\\*","");
                    String key = docId+"<DELIM>"+verb1+"<DELIM>"+verb2;
                    //We may assume that docID with the annotated verb-pair is unique
                    uniqueVerbPairsAnnotated.add(key);
                    String section = docIdVerbPairToSections.get(key);
                    if(section==null){
                        key = docId+"<DELIM>"+verb2+"<DELIM>"+verb1;
                        section = docIdVerbPairToSections.get(key);
                    }
                    if(section==null){
                        Set<String> existingInstances = missedAnnotations.get(docId);
                        if(existingInstances==null){
                            existingInstances = new HashSet<String>();
                        }
                        existingInstances.add(key.replace("<DELIM>","\t")+"\n"+verb1Sentence+"\n"+verb2Sentence+"\n"+label);
                        missedAnnotations.put(docId,existingInstances);
                        numMissedAnnotations++;
                        continue;
                    }
                    //if we get a section for this annotation, it's a hit
                    sectionIdsWithLabels.add(key);
                    String sentenceFromMappings = section.split("\n")[1];
                    sentenceFromMappings = sentenceFromMappings.replace("<SLOT1>","").
                            replace("</SLOT1>","").replace("<SLOT0>","").
                            replace("</SLOT0>","");
                    Set<String> tokens1 = new HashSet(ImmutableList.copyOf(
                            (sentenceFromMappings.split(" "))));
                    Set<String> tokens2 = new HashSet(ImmutableList.copyOf(sentence.split(" ")));
                    if(Sets.intersection(tokens1,tokens2).size()>=0.75*tokens1.size()){
                        if(section.substring(section.lastIndexOf("\n")+1).equals("BEFORE")||
                                section.substring(section.lastIndexOf("\n")+1).equals("CAUSES")||
                                section.substring(section.lastIndexOf("\n")+1).equals("PRECONDITION")) {
                            System.out.println("=====More than one labels===="+section.substring(section.lastIndexOf("\n")+1)
                            +" "+label);
                            System.out.println(sentenceFromMappings);
                            System.out.println(sentence);
                            System.out.println("==========");
                        }
                        section+=("\nLabel: "+label);
                        labelToVerbPair.put(label,new Pair(verb1,verb2));
                        verbPairToLabel.put(new Pair<String, String>(verb1,verb2),label);
                        docIdVerbPairToSections.put(key,section);
                    }else{
//                        System.out.println("====Non matching sentences=====");
//                        System.out.println(sentenceFromMappings);
//                        System.out.println(sentence);
//                        System.out.println("=========");
                        sectionsWithoutLabelDueToUnmatchedSentence.add(section);
                    }
                }else if(line.startsWith("relation type:")){
                    if(line.endsWith("\tTrue")){
                        if(line.contains("CAUSES")||line.contains("BEFORE")||
                                line.contains("PRECONDITION")) {
                            isRelationToConsider = true;
                            label = line.split("\t")[1];
                            continue;
                        }
                    }
                    isRelationToConsider = false;
                }
            }
        }
//        for(String l : labelToVerbPair.keySet()){
//            System.out.println(l+"\t:");
//            for(Pair verbs : labelToVerbPair.get(l)){
//                System.out.println("\t\t"+verbs.key+"\t"+verbs.value);
//            }
//        }
//        for(Pair<String,String> p : verbPairToLabel.keySet()){
//            String out = p.key+"\t"+p.value+"\t\t";
//            for(String lab : verbPairToLabel.get(p)){
//                out+=lab+", ";
//            }
//            out = out.substring(0,out.length()-2);
//            System.out.println(out);
//        }

    }
}
