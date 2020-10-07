package com.bbn.akbc.neolearnit.util;

import com.bbn.bue.common.symbols.Symbol;

import java.util.HashSet;
import java.util.Set;

public class TextNormalizer {


    public static String defaultTextNormalizerForKeywordSearch(String input) {
        return input.toLowerCase();
    }


    public static Set<Symbol> normalizeASet(Set<Symbol> input) {
        Set<Symbol> ret = new HashSet<>();
        for (Symbol s : input) {
            ret.add(Symbol.from(defaultTextNormalizerForKeywordSearch(s.asString())));
        }
        return ret;
    }


}
