package gov.nih.nci.evs.restapi.bean;

import java.io.*;
import java.util.*;
import java.net.*;

import com.google.gson.*;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.XStream;

public class TransitiveClosure
{

// Variable declaration
	private String rootConceptCode;
	private String rootConceptName;
	private List concepts;

// Default constructor
	public TransitiveClosure() {
	}

// Constructor
	public TransitiveClosure(
		String rootConceptCode,
		String rootConceptName,
		List concepts) {

		this.rootConceptCode = rootConceptCode;
		this.rootConceptName = rootConceptName;
		this.concepts = concepts;
	}

// Set methods
	public void setRootConceptCode(String rootConceptCode) { 
		this.rootConceptCode = rootConceptCode;
	}

	public void setRootConceptName(String rootConceptName) { 
		this.rootConceptName = rootConceptName;
	}

	public void setConcepts(List concepts) { 
		this.concepts = concepts;
	}


// Get methods
	public String getRootConceptCode() { 
		return this.rootConceptCode;
	}

	public String getRootConceptName() { 
		return this.rootConceptName;
	}

	public List getConcepts() { 
		return this.concepts;
	}

	public String toXML() {
		XStream xstream_xml = new XStream(new DomDriver());
		String xml = xstream_xml.toXML(this);
		xml = escapeDoubleQuotes(xml);
		StringBuffer buf = new StringBuffer();
		String XML_DECLARATION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		buf.append(XML_DECLARATION).append("\n").append(xml);
		xml = buf.toString();
		return xml;
	}

	public String toJson() {
		JsonParser parser = new JsonParser();
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
	}

	public String escapeDoubleQuotes(String inputStr) {
		char doubleQ = '"';
		StringBuffer buf = new StringBuffer();
		for (int i=0;  i<inputStr.length(); i++) {
			char c = inputStr.charAt(i);
			if (c == doubleQ) {
				buf.append(doubleQ).append(doubleQ);
			}
			buf.append(c);
		}
		return buf.toString();
	}
}
