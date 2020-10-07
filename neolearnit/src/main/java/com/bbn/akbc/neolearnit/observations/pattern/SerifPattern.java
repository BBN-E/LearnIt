package com.bbn.akbc.neolearnit.observations.pattern;

import com.bbn.akbc.common.Pair;
import com.bbn.akbc.kb.facts.FactCanonicalMention;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.bue.sexp.Sexp;
import com.bbn.bue.sexp.SexpReader;
import com.bbn.serif.patterns.Pattern;
import com.bbn.serif.patterns.PatternFactory;
import com.bbn.serif.patterns.PatternSexpParsingException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class SerifPattern extends LearnitPattern{

    static Map<Symbol,Pattern> symbolPatternCache = new ConcurrentHashMap<>();

    @Override
    @JsonProperty
    public String toPrettyString() {
        return this.serifPattern.toPrettyString();
    }

    @Override
    public int hashCode() {
        return this.serifPatterhSexpString.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof SerifPattern))return false;
        SerifPattern that = (SerifPattern)obj;
        return this.serifPatterhSexpString.equals(that.serifPatterhSexpString);
    }

    @Override
    public String toString() {
        return this.toIDString();
    }

    @Override
    @JsonProperty("toIDString")
    public String toIDString() {
        return this.serifPatterhSexpString.asString();
    }

    @Override
    public boolean isInCanonicalSymmetryOrder() {
        return false;
    }

    @Override
    public Set<Symbol> getLexicalItems() {
        boolean isbinaryPattern = this.toPrettyString().contains("[1]");
        Set<Symbol> ret = new HashSet<>();
        for(String token:this.toPrettyString().split(" ")){
            token = token.trim();
            if(isbinaryPattern && (token.endsWith("[0]") || token.endsWith("[1]")))continue;
            if(token.equals("[") || token.equals("]"))continue;
            if(token.startsWith("<") || token.endsWith(">"))continue;
            if(token.length()<1)continue;
            ret.add(Symbol.from(token));
        }
        return ret;
    }

    @Override
    public boolean matchesPattern(LearnitPattern p) {
        if(!(p instanceof SerifPattern)){
            return false;
        }
        SerifPattern that = (SerifPattern)p;
        return that.equals(this);
    }

    final Pattern serifPattern;
    final Symbol serifPatterhSexpString;
    private ReentrantLock lock = new ReentrantLock();
    public Pattern getPattern(){
        return serifPattern;
    }

    public SerifPattern(Pattern serifPattern){

        this.serifPatterhSexpString = Symbol.from(serifPattern.toString());
        lock.lock();
        if(!symbolPatternCache.containsKey(this.serifPatterhSexpString)){
            symbolPatternCache.put(this.serifPatterhSexpString,serifPattern);
        }
        lock.unlock();
        this.serifPattern = symbolPatternCache.get(this.serifPatterhSexpString);
    }

    public static Pattern construcPatternFromSexpStr(String serifPatternStr) throws PatternSexpParsingException,IOException {
        SexpReader sexpReader = SexpReader.createDefault();
        PatternFactory patternFactory = new PatternFactory();
        Sexp fullSexp = sexpReader.read(serifPatternStr);
        return patternFactory.fromSexp(fullSexp);
    }

    @JsonCreator
    public SerifPattern(@JsonProperty("toIDString") String serifPatternStr) throws IOException,PatternSexpParsingException {
        this.serifPatterhSexpString = Symbol.from(serifPatternStr);
        lock.lock();
        if(!symbolPatternCache.containsKey(this.serifPatterhSexpString)){
            Pattern unSeenPattern = construcPatternFromSexpStr(serifPatternStr);
            symbolPatternCache.put(this.serifPatterhSexpString,unSeenPattern);
        }
        lock.unlock();
        this.serifPattern = symbolPatternCache.get(this.serifPatterhSexpString);
    }

    public List<Pair<Symbol,Boolean>> getLexicalItemsInSequence(){
        List<Pair<Symbol,Boolean>> ret = new ArrayList<>();
        for(String token:this.toPrettyString().split(" ")){
            token = token.trim();
            if(token.equals("[") || token.equals("]"))continue;
            if(token.length()<1)continue;
            // Handle entity type
            if(token.contains("[") && token.length()> 3 && token.lastIndexOf('[') != token.length()-3){
                if(token.startsWith("[")){
                    ret.add(new Pair<>(Symbol.from(token),true));
                }
                else{
                    ret.add(new Pair<>(Symbol.from(token),false));
                }
            }
            else{
                ret.add(new Pair<>(Symbol.from(token),false));
            }
        }
        return ret;
    }

//    public boolean isParentOf(SerifPattern otherSerifPattern){
//        Set<Symbol> otherLexicals = otherSerifPattern.getLexicalItems();
//        return otherLexicals.containsAll(this.getLexicalItems()) && !this.getLexicalItems().equals(otherLexicals);
//    }
    public boolean isParentOf(SerifPattern otherSerifPattern){
        if(otherSerifPattern == this)return false;
        List<Pair<Symbol,Boolean>> otherLexicals = otherSerifPattern.getLexicalItemsInSequence();
        List<Pair<Symbol,Boolean>> thisLexicals = this.getLexicalItemsInSequence();

        if(thisLexicals.size() > otherLexicals.size())return false;
        int currentCursor = 0;
        int earliestHitCursorInOther = otherLexicals.size();
        int latestHitCursorInOther  = 0;
        for(Pair<Symbol,Boolean> symbolBooleanPair: thisLexicals){
            int mover = currentCursor;
            boolean found = false;
            for(;mover<otherLexicals.size();++mover){
                if(symbolBooleanPair.getSecond()){ // This is a generic entity type, [FOOD]
                    String otherToken = otherLexicals.get(mover).getFirst().asString();
                    if(otherToken.contains("[")){
                        String otherEntityType = otherToken.substring(otherToken.indexOf("["));
                        if(symbolBooleanPair.getFirst().asString().equals(otherEntityType)){
                            currentCursor = mover+1;
                            found = true;
                            earliestHitCursorInOther = Math.min(earliestHitCursorInOther,mover);
                            latestHitCursorInOther = Math.max(latestHitCursorInOther,mover);
                            break;
                        }
                    }
                }
                if(otherLexicals.get(mover).equals(symbolBooleanPair)){
                    currentCursor = mover+1;
                    found = true;
                    earliestHitCursorInOther = Math.min(earliestHitCursorInOther,mover);
                    latestHitCursorInOther = Math.max(latestHitCursorInOther,mover);
                    break;
                }
            }
            if(!found)return false;
            if((latestHitCursorInOther-earliestHitCursorInOther+1) > thisLexicals.size())return false;
        }
        if((latestHitCursorInOther-earliestHitCursorInOther+1) == thisLexicals.size()){
            return true;
        }
        else{
            return false;
        }

    }
}
