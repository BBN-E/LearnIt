package com.bbn.akbc.neolearnit.exec;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.File;
import java.io.FileInputStream;

public class ConvertIntoSeedFile {
	public static void main(String [] argv) throws IOException {
		String fileIn = argv[0];
		String fileOut = fileIn + ".out";

        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(fileIn)), "UTF-8"));
		PrintStream ps = new PrintStream(new FileOutputStream(new File(fileOut)), true, "UTF-8");

        String sline;
        while ((sline = br.readLine()) != null) {
        	if(sline.trim().isEmpty())
        		continue;

        	String strLearnItSeedFileFormat = convertOneLineIntoLearnItSeedFileFormat(sline);
        	ps.println(strLearnItSeedFileFormat);
        }

        br.close();
		ps.close();
	}

	public static String convertOneLineIntoLearnItSeedFileFormat(String sline) {
	  	String lang = sline.substring(0, sline.indexOf(":"));
	  	lang = lang.substring(lang.lastIndexOf(" ")+1
		);
		lang = lang.substring(0, 1).toUpperCase() + lang.substring(1);

		String strPair = sline.substring(sline.indexOf("(")+1, sline.lastIndexOf(")")).trim();
		String slot0str = strPair.substring(0, strPair.indexOf(",")).trim();
		String slot1str = strPair.substring(strPair.indexOf(",")+1).trim();

		return "<seed slot0=\"" + slot0str + "\" " +
		"slot1=\"" + slot1str + "\" " +
		"language=\"" + lang + "\" />";
	}
}
