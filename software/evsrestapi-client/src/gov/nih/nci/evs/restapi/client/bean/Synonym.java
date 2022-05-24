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

public class Synonym
{

// Variable declaration
	private String type;
	private String name;
	private String termGroup;
	private String source;
	private String code;
	private String subSource;

// Default constructor
	public Synonym() {
	}

// Constructor
	public Synonym(
		String type,
		String name,
		String termGroup,
		String source,
		String code,
		String subSource) {

		this.type = type;
		this.name = name;
		this.termGroup = termGroup;
		this.source = source;
		this.code = code;
		this.subSource = subSource;
	}

// Set methods
	public void setType(String type) {
		this.type = type;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setTermGroup(String termGroup) {
		this.termGroup = termGroup;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public void setSubSource(String subSource) {
		this.subSource = subSource;
	}


// Get methods
	public String getType() {
		return this.type;
	}

	public String getName() {
		return this.name;
	}

	public String getTermGroup() {
		return this.termGroup;
	}

	public String getSource() {
		return this.source;
	}

	public String getCode() {
		return this.code;
	}

	public String getSubSource() {
		return this.subSource;
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
