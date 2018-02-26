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

public class ResolvedValueSet
{

// Variable declaration
	private String uri;
	private String name;
	private List concepts;

// Default constructor
	public ResolvedValueSet() {
	}

// Constructor
	public ResolvedValueSet(
		String uri,
		String name,
		List concepts) {

		this.uri = uri;
		this.name = name;
		this.concepts = concepts;
	}

// Set methods
	public void setUri(String uri) { 
		this.uri = uri;
	}

	public void setName(String name) { 
		this.name = name;
	}

	public void setConcepts(List concepts) { 
		this.concepts = concepts;
	}


// Get methods
	public String getUri() { 
		return this.uri;
	}

	public String getName() { 
		return this.name;
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
