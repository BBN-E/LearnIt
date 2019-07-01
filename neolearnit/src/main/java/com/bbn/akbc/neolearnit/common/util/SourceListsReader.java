package com.bbn.akbc.neolearnit.common.util;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.utility.FileUtil;
import com.bbn.bue.common.parameters.exceptions.MissingRequiredParameter;
import com.google.common.collect.Lists;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class SourceListsReader {

	private static Map<String,String> docIdToFullPath;

	private synchronized static void loadFromListsDirectory(String sourceListsDir) throws IOException {
		if (docIdToFullPath!=null){
			return;
		}
		docIdToFullPath = new HashMap<>();
		File dir = new File(sourceListsDir);
		if(dir.isFile()){
			throw new IOException(sourceListsDir+" must be a directory of source list files");
		}
		String[] fileNames = dir.list();
		for(String fileName : fileNames){
			String filePath = sourceListsDir+File.separator+fileName;
			File listFile = new File(filePath);
			if (listFile.isDirectory()){
				throw new IOException(fileName+" in "+sourceListsDir+" must be a list file and not a directory");
			}
			List<String> sourceFilePaths = FileUtil.readLinesIntoList(listFile);
			for(String sourceFilePath : sourceFilePaths){
				String docId = sourceFilePath.substring(sourceFilePath.lastIndexOf(File.separator)+1,sourceFilePath.lastIndexOf("."));
				docIdToFullPath.put(docId,sourceFilePath);
			}
		}
		return;
	}

	public static String getFullPath(String docId) throws MissingRequiredParameter, IOException, SourceDocumentNotFoundException {
		String sourceListsDir = LearnItConfig.get("source_lists");
		loadFromListsDirectory(sourceListsDir);
		String fullPath = docIdToFullPath.get(docId);
		if (fullPath==null){
			throw new SourceDocumentNotFoundException(docId,fullPath);
		}
		return fullPath;
	}

	public static class SourceDocumentNotFoundException extends Exception{
	    public SourceDocumentNotFoundException(String docId, String sourceListsDir) {
			super(String.format("Could not find docId %s among those listed in list files in %s", new Object[]{docId,sourceListsDir}));
		}
	}

}
