package com.bbn.akbc.neolearnit.common.util;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.utility.FileUtil;
import com.bbn.bue.common.parameters.exceptions.MissingRequiredParameter;
import com.google.common.collect.ImmutableMap;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TabularPathListsConverter {
    private static Map<String, Map<String, String>> docIdToEntries;

    public static Map<String, Map<String, String>> parseSingleTabularList(String fileName) throws IOException {
        Map<String, Map<String, String>> ret = new HashMap<>();
        File listFile = new File(fileName);
        if (listFile.isDirectory()) {
            throw new IOException(fileName + " must be a list file and not a directory");
        }
        List<String> lines = FileUtil.readLinesIntoList(listFile);
        for (String line : lines) {
            String docId = "";
            String[] pieces = line.trim().split(" ");
            Map<String, String> entries = new HashMap<>();
            for (String piece : pieces) {
                String fieldName = piece.split(":")[0];
                String fieldValue = piece.split(":")[1];
                if (fieldName.toLowerCase().equals("docid")) {
                    docId = fieldValue;
                } else {
                    entries.put(fieldName, fieldValue);
                }
            }
            if (docId.length() > 0) {
                ret.put(docId, entries);
            }
        }
        return ret;
    }

    private synchronized static void loadFromListsDirectory(String sourceListsDir) throws IOException {
        if (docIdToEntries != null) {
            return;
        }
        docIdToEntries = new HashMap<>();
        File dir = new File(sourceListsDir);
        if (dir.isFile()) {
            throw new IOException(sourceListsDir + " must be a directory of source list files");
        }
        String[] fileNames = dir.list();
        for (String fileName : fileNames) {
            String filePath = sourceListsDir + File.separator + fileName;
            Map<String, Map<String, String>> currentEntry = parseSingleTabularList(filePath);
            docIdToEntries.putAll(currentEntry);
        }
    }

    public static Map<String, String> getPathEntries(String docId) throws IOException {
        String sourceListsDir = LearnItConfig.get("source_lists");
        loadFromListsDirectory(sourceListsDir);
        return docIdToEntries.get(docId);
    }

    public static Map<String, Map<String, String>> getTabularPathList() {
        ImmutableMap.Builder<String, Map<String, String>> ret = new ImmutableMap.Builder<>();
        for (Map.Entry<String, Map<String, String>> kv : docIdToEntries.entrySet()) {
            ret.put(kv.getKey(), ImmutableMap.copyOf(kv.getValue()));
        }
        return ret.build();
    }
}
