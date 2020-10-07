package com.bbn.akbc.neolearnit.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ExternalDictionary {
    private String dicPath;
    private Map<String, Map> dictionary;
    public ExternalDictionary(String dicPath){
        this.dicPath = dicPath;
    }

    public void readExternalDictionary() throws Exception{
        this.dictionary = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        dictionary = objectMapper.readValue(new File(dicPath),dictionary.getClass());
    }

    public Map lookupDictionary(String keyword){
        return this.dictionary.getOrDefault(keyword,new HashMap());
    }

    public static void main(String[] args) throws Exception{
        String dicPath = "/nfs/raid88/u10/users/hqiu/learnit_data/learnit_bi_dev.010920/tokens_resolved.json";
        ExternalDictionary externalDictionary = new ExternalDictionary(dicPath);
        externalDictionary.readExternalDictionary();
        System.out.println(externalDictionary.lookupDictionary("مسئولي"));
    }
}
