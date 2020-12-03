package gov.nih.nci.evs.restapi.model;

import java.io.*;
import java.util.*;
import java.net.*;

import com.google.gson.*;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.XStream;

public class Synonym
{

// Variable declaration
	private String name;
	private String type;
	private String termGroup;
	private String source;
	private String subSource;
	private String code;

// Default constructor
	public Synonym() {
	}

// Constructor
	public Synonym(
		String name,
		String type,
		String termGroup,
		String source,
		String subSource,
		String code) {

		this.name = name;
		this.type = type;
		this.termGroup = termGroup;
		this.source = source;
		this.subSource = subSource;
		this.code = code;
	}

// Set methods
	public void setName(String name) {
		this.name = name;
	}

	public void setType(String type) {
		this.type = type;
	}

	public void setTermGroup(String termGroup) {
		this.termGroup = termGroup;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public void setSubSource(String subSource) {
		this.subSource = subSource;
	}

	public void setCode(String code) {
		this.code = code;
	}


// Get methods
	public String getName() {
		return this.name;
	}

	public String getType() {
		return this.type;
	}

	public String getTermGroup() {
		return this.termGroup;
	}

	public String getSource() {
		return this.source;
	}

	public String getSubSource() {
		return this.subSource;
	}

	public String getCode() {
		return this.code;
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
