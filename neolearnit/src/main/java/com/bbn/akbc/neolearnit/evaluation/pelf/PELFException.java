package com.bbn.akbc.neolearnit.evaluation.pelf;

import org.w3c.dom.Element;

import com.bbn.bue.common.xml.XMLUtils;
import com.bbn.serif.common.SerifException;

public class PELFException extends SerifException {
	private static final long serialVersionUID = 1L;

	public PELFException(String msg) { super(msg); }
	public PELFException(String msg, Throwable t) { super(msg, t);}
	public PELFException(Element e, Throwable t) {
		super(String.format("While processing element %s", XMLUtils.dumpXMLElement(e)), t);
	}
	public PELFException(String msg, Element e, Throwable t) {
		super(String.format("While processing element %s, %s", XMLUtils.dumpXMLElement(e), msg), t);
	}
}
