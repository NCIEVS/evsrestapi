package gov.nih.nci.evs.restapi.client.bean;

import java.io.*;
import java.util.*;
import java.net.*;

import com.google.gson.*;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.XStream;

public class SearchResult
{

// Variable declaration
	private int total;
	private int timeTaken;
	private Parameter parameters;
	private List<Concept> concepts;

// Default constructor
	public SearchResult() {
	}

// Constructor
	public SearchResult(
		int total,
		int timeTaken,
		Parameter parameters,
		List<Concept> concepts) {

		this.total = total;
		this.timeTaken = timeTaken;
		this.parameters = parameters;
		this.concepts = concepts;
	}

// Set methods
	public void setTotal(int total) {
		this.total = total;
	}

	public void setTimeTaken(int timeTaken) {
		this.timeTaken = timeTaken;
	}

	public void setParameters(Parameter parameters) {
		this.parameters = parameters;
	}

	public void setConcepts(List<Concept> concepts) {
		this.concepts = concepts;
	}


// Get methods
	public int getTotal() {
		return this.total;
	}

	public int getTimeTaken() {
		return this.timeTaken;
	}

	public Parameter getParameters() {
		return this.parameters;
	}

	public List<Concept> getConcepts() {
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
