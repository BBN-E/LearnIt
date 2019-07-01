package com.bbn.akbc.neolearnit.evaluation.pelf;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.bbn.serif.common.SerifException;

public class ElfWriter {


	public void saveTo(ElfDocument elfdoc, String filename) {
		saveTo(elfdoc,new File(filename));
	}

	public void saveTo(ElfDocument elfdoc, File file) {
		try {
			final Document xmldoc = toXMLDocument(elfdoc);

			// write the content into xml file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			DOMSource source = new DOMSource(xmldoc);
			StreamResult result = new StreamResult(file);

			transformer.transform(source, result);

		} catch (TransformerException tfe) {
			throw new SerifException("Error transforming XML to file", tfe);
		}
	}

	public Document toXMLDocument(ElfDocument elfdoc) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder builder;

			builder = factory.newDocumentBuilder();

			Document xmldoc = builder.newDocument();
			Element rootElement = xmldoc.createElement("doc");
			rootElement.setAttribute("version", ElfDocument.getVersion());
			rootElement.setAttribute("contents", ElfDocument.getContents());
			rootElement.setAttribute("xmlns", ElfDocument.getXmlns());
			rootElement.setAttribute("source", elfdoc.getSource());
			rootElement.setAttribute("id", elfdoc.getDocid());

			for (ElfRelation rel : elfdoc.getRelations()) {
				buildRelation(xmldoc, rootElement, rel);
			}

			for (ElfIndividual ind : elfdoc.getIndividuals()) {
				buildIndividual(xmldoc, rootElement, ind);
			}

			xmldoc.appendChild(rootElement);
			return xmldoc;
		} catch (ParserConfigurationException e) {
			throw new SerifException("XML parser configuration error", e);
		}
	}

	private void buildRelation(Document xmldoc,
			Element parentElement, ElfRelation relation) {

		final Element relationElement = xmldoc.createElement("relation");

		relationElement.setAttribute("start", Integer.toString(relation.getStart()));
		relationElement.setAttribute("end", Integer.toString(relation.getEnd()));
		relationElement.setAttribute("name", relation.getName());
		relationElement.setAttribute("p", Double.toString(relation.getP()));
		relationElement.setAttribute("score_group", Integer.toString(relation.getScoreGroup()));
		relationElement.setAttribute("source",relation.getSource());

		final Element contentElement = xmldoc.createElement("text");
		contentElement.setTextContent(relation.getText());
		relationElement.appendChild(contentElement);

		for (ElfRelationArgument arg : relation.getArgs()) {
			buildRelationArg(xmldoc, relationElement, arg);
		}

		parentElement.appendChild(relationElement);
	}

	private void buildRelationArg(Document xmlDoc,
			Element parentElement, ElfRelationArgument arg) {

		final Element argElement = xmlDoc.createElement("arg");

		argElement.setAttribute("start", Integer.toString(arg.getStart()));
		argElement.setAttribute("end", Integer.toString(arg.getEnd()));
		argElement.setAttribute("confidence_enum", Integer.toString(arg.getConfidenceEnum()));
		argElement.setAttribute("role", arg.getRole());
		argElement.setAttribute("type", arg.getType());
		argElement.setAttribute("id", arg.getIndividual().getId());
		argElement.setTextContent(arg.getText());

		parentElement.appendChild(argElement);
	}

	private void buildIndividual(Document xmldoc,
			Element parentElement, ElfIndividual individual) {

		final Element indElement = xmldoc.createElement("individual");

		indElement.setAttribute("id", individual.getId());
		for (ElfIndividualMention ment : individual.getMentions()) {
			buildIndividualMention(xmldoc, indElement, ment);
		}

		parentElement.appendChild(indElement);
	}

	private void buildIndividualMention(Document xmldoc,
			Element parentElement, ElfIndividualMention individualMention) {

		// Bonan: fix null desc XIN20001008.0200.0020.segment.xml
		if(individualMention.getText()==null)
			return;
		if(individualMention.getText().isEmpty())
			return;
		if(individualMention.getType()==null)
			return;
		if(individualMention.getType().isEmpty())
			return;
		//

		final Element indElement;
		if (individualMention.isType()) {
			indElement = xmldoc.createElement("type");
			indElement.setAttribute("type", individualMention.getText());
		} else {
			indElement = xmldoc.createElement("desc");
			indElement.setAttribute("desc", individualMention.getType());
		}
		indElement.setAttribute("start", Integer.toString(individualMention.getStart()));
		indElement.setAttribute("end", Integer.toString(individualMention.getEnd()));

		parentElement.appendChild(indElement);
	}

}
