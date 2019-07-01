package com.bbn.akbc.neolearnit.evaluation.pelf;

import com.bbn.bue.common.xml.XMLUtils;
import com.bbn.serif.apf.APFException;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.common.io.Files;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import static com.bbn.bue.common.xml.XMLUtils.requiredAttribute;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class ElfLoader {

	private final Map<String, Object> idMap;

	public ElfLoader() {
		this.idMap = Maps.newHashMap();
	}

	public ElfDocument loadFrom(File f) throws IOException {
		try {
			return loadFrom(Files.toString(f, Charsets.UTF_8));
		} catch (IOException e) {
			throw e;
		} catch (Throwable t) {
			throw new IOException(String.format("Error loading PELF document %s", f.getAbsolutePath()), t);
		}
	}

	public ElfDocument loadFrom(String s) throws IOException {
		// The XML parser treats \r\n as a single character. This is problematic
		// when we are using character offsets. To avoid this, we replace
		// \r with an entity reference before parsing
		final InputSource in = new InputSource(new StringReader(s.replaceAll("\r", "&#xD;")));
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			return loadFrom(builder.parse(in));
		} catch (ParserConfigurationException e) {
			throw new PELFException("Error parsing xml", e);
		} catch (SAXException e) {
			throw new PELFException("Error parsing xml", e);
		}
	}

	public ElfDocument loadFrom(org.w3c.dom.Document  xml) {
		final Element root = xml.getDocumentElement();
		return toDocument(root);
	}

	public ElfDocument toDocument(Element xml)  {
		idMap.clear();

		String docid = XMLUtils.requiredAttribute(xml, "id");
		Optional<String> source = XMLUtils.optionalStringAttribute(xml, "source");

		ElfDocument.Builder builder;
		if (source.isPresent()) {
			builder = new ElfDocument.Builder(docid,source.get());
		} else {
			builder = new ElfDocument.Builder(docid);
		}

		//first get all individuals
		for (Node child = xml.getFirstChild(); child!=null; child = child.getNextSibling()) {
			if (child instanceof Element) {
				Element e = (Element)child;
				if (e.getTagName().equals("individual")) {
					builder.withAddIndividual(toIndividual(e));
				}
			}
		}

		//the get all relations
		for (Node child = xml.getFirstChild(); child!=null; child = child.getNextSibling()) {
			if (child instanceof Element) {
				Element e = (Element)child;
				if (e.getTagName().equals("relation")) {
					builder.withAddRelation(toRelation(e));
				}
			}
		}

		ElfDocument result = builder.build();
		idMap.put(docid, result);
		return result;
	}

	public ElfIndividual toIndividual(Element xml) {
		String id = XMLUtils.requiredAttribute(xml, "id");
		ElfIndividual.Builder builder = new ElfIndividual.Builder(id);

		for (Node child = xml.getFirstChild(); child!=null; child = child.getNextSibling()) {
			if (child instanceof Element) {
				Element e = (Element)child;
				builder.withAddMention(toIndividualMention(e));
			}
		}

		ElfIndividual result = builder.build();
		idMap.put(id, result);
		return result;
	}

	public ElfIndividualMention toIndividualMention(Element xml) {
		int start = XMLUtils.requiredIntegerAttribute(xml, "start");
		int end = XMLUtils.requiredIntegerAttribute(xml, "end");
		String text = XMLUtils.defaultStringAttribute(xml, "desc", "");

		return new ElfIndividualMention(start,end,text);
	}

	public ElfRelation toRelation(Element xml) {
		int start = XMLUtils.requiredIntegerAttribute(xml, "start");
		int end = XMLUtils.requiredIntegerAttribute(xml, "end");
		String name = XMLUtils.requiredAttribute(xml, "name");
		double p = XMLUtils.requiredDoubleAttribute(xml, "p");
		int score_group = XMLUtils.requiredIntegerAttribute(xml, "score_group");
		String source = XMLUtils.requiredAttribute(xml, "source");

		Optional<Element> textElement = XMLUtils.directChild(xml, "text");
		if (!textElement.isPresent()) {
			new PELFException("No text element present for P-ELF Relation");
		}
		String text = textElement.get().getTextContent();

		ElfRelation.Builder builder = new ElfRelation.Builder(start, end, name, p,
				score_group, source, text);

		for (Node child = xml.getFirstChild(); child!=null; child = child.getNextSibling()) {
			if (child instanceof Element) {
				Element e = (Element)child;
				if (e.getTagName().equals("arg")) {
					builder.withAddArg(toRelationArgument(e));
				}
			}
		}

		return builder.build();
	}

	public ElfRelationArgument toRelationArgument(Element xml) {
		ElfIndividual individual = fetch("id",xml);
		int start = XMLUtils.requiredIntegerAttribute(xml, "start");
		int end = XMLUtils.requiredIntegerAttribute(xml, "end");
		String text = xml.getTextContent();
		String type = XMLUtils.requiredAttribute(xml, "type");
		String role = XMLUtils.requiredAttribute(xml, "role");
		int confidence_enum = XMLUtils.requiredIntegerAttribute(xml, "confidence_enum");

		return new ElfRelationArgument(individual,start,
				end,text,type,role,confidence_enum);
	}

	@SuppressWarnings("unused")
	private <T> T fetch(String id)  {
		checkNotNull(id);
		checkArgument(!id.isEmpty());
		@SuppressWarnings("unchecked")
		final T ret = (T)idMap.get(id);
		if (ret == null) {
			throw new PELFException(String.format("Lookup failed for id %s.", id));
		}
		return ret;
	}

	@SuppressWarnings("unchecked")
	private <T> T fetch(String attribute, Element e)  {
		final String attVal = requiredAttribute(e, attribute);

		Object o = idMap.get(requiredAttribute(e, attribute));

		if (o == null) {
			throw new PELFException(String.format("Lookup failed for id %s. Known keys are %s", attVal, idMap.keySet()));
		}

		try {
			return (T)o;
		} catch (ClassCastException f) {
			throw new APFException(String.format("Didn't expect ID %s to be %s", attVal, o.getClass()));
		}
	}

}
