package com.bbn.akbc.neolearnit.preprocessing.corpus;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import com.bbn.akbc.utility.FileUtil;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

public class GenerateInputForMTfromWMS {
	static String strDirSegment = "/nfs/mercury-04/u42/bmin/d2d/corpus/2/raw_segments/";

	static String getDocIdFromWMSxmlPath(String strFileXml) {
		String [] items = strFileXml.split("/");
		return items[items.length-2] + "_" + items[items.length-1].replace(".xml", "").replace(" ", "-");
	}

	static String getSegmentPathFromWMSxmlPath(String strFileXml) {
		return strDirSegment + getDocIdFromWMSxmlPath(strFileXml) + ".segment";
	}


	/*
	 <source_text_page>
		 <passages>
			 <passage>
			 	<text_passage>
			 		<text_token token_id="24" token_type="lexeme" token_print_form="生" />
			 	</text_passage>
			 </passage>
		 </passages>
	 </source_text_page>
	 */
	static List<String> getTextsOfSentences(String strFileWMSxml) throws Exception {
		List<String> listTexts = new ArrayList<String>();

		File f = new File(strFileWMSxml);
		if(!f.exists())
			throw new Exception("No file: " + strFileWMSxml);

		String contents = Files.toString(f, Charsets.UTF_8);

		// deal with BOM
		if(!contents.startsWith("<?xml version="))
			contents = contents.substring(contents.indexOf("<?xml version="));

		final InputSource in = new InputSource(new StringReader(contents));

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		org.w3c.dom.Document xml = builder.parse(in);

		final Element root = xml.getDocumentElement();

		String rootTagName = root.getTagName();
		if(!rootTagName.equals("source_text_page")) {
			throw new Exception("Exception reading XML: " + strFileWMSxml);
		}

		for(Node child=root.getFirstChild(); child!=null; child=child.getNextSibling()) {
			if(child instanceof Element) {
				Element pasagesNode = (Element)child;
				String passagesTagName = pasagesNode.getTagName();

				if(passagesTagName.equals("passages")) {
					for(Node passageNode=pasagesNode.getFirstChild(); passageNode!=null; passageNode=passageNode.getNextSibling()) {
						if(passageNode instanceof Element) {
							Element passageElement = (Element)passageNode;
							String passageTagName = passageElement.getTagName();

							if(!passageTagName.equals("passage"))
								throw new Exception("Exception eading XML: " + strFileWMSxml);

							for(Node textPassageNode=passageNode.getFirstChild(); textPassageNode!=null; textPassageNode=textPassageNode.getNextSibling()) {
								if(textPassageNode instanceof Element) {
									Element textPassageElement = (Element)textPassageNode;
									String textPassageTagName = textPassageElement.getTagName();

									if(textPassageTagName.equals("text_passage")) {
										StringBuilder sb = new StringBuilder();
										for(Node tokenNode=textPassageElement.getFirstChild(); tokenNode!=null; tokenNode=tokenNode.getNextSibling()) {
											if(tokenNode instanceof Element) {
												Element tokenElement =(Element) tokenNode;
												if(tokenElement.getTagName().equals("text_token")) {
													String strTokenText = tokenElement.getAttribute("token_print_form");
													sb.append(strTokenText);
												}
											}
										}
										listTexts.add(sb.toString());
									}
								}
							}
						}
					}
				}
			}
		}

		return listTexts;
	}

	// strFileXml: /nfs/mercury-04/u10/ChineseWMSData/run_2014-09-19/0001/CD News.37.1018.0.xml
	static void convertWMSxmlToSegmentForMT(String strFileXml) throws Exception {
		String strFileSegment = getSegmentPathFromWMSxmlPath(strFileXml);
		String docID = getDocIdFromWMSxmlPath(strFileXml);

		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(strFileSegment), "UTF-8"));

		List<String> sentTexts = getTextsOfSentences(strFileXml);
		for(int sid=0; sid<sentTexts.size(); sid++) {
			String sentText = sentTexts.get(sid);
			String strSentID = Util.getSentenceIDstr(sid+1); // start from 00001

			bw.write("<SEGMENT>\n");
			bw.write("   <GUID>" + "[" + docID + "]" + "[" + docID + "]" + "[" + strSentID + "]</GUID>\n");
			bw.write("   <RAW_TEXT>" + sentText + "</RAW_TEXT>\n");
			bw.write("</SEGMENT>\n");
		}

		bw.close();
	}

	public static void main(String [] argv) throws Exception {
		String xmlFileList = "/nfs/mercury-04/u10/ChineseWMSData/all_files.txt";
		List<String> listXmlFiles = FileUtil.readLinesIntoList(xmlFileList);

		for(String strXmlFile : listXmlFiles) {
			System.out.println("== Doc: " + strXmlFile + "\n");
			convertWMSxmlToSegmentForMT(strXmlFile);
		}
	}
}
