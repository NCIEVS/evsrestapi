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

public class ValueSetDefinition
{

// Variable declaration
	private String uri;
	private String defaultCodingScheme;
	private String conceptDomain;
	private String name;
	private String code;
	private List sources;

// Default constructor
	public ValueSetDefinition() {
	}

// Constructor
	public ValueSetDefinition(
		String uri,
		String defaultCodingScheme,
		String conceptDomain,
		String name,
		String code,
		List sources) {

		this.uri = uri;
		this.defaultCodingScheme = defaultCodingScheme;
		this.conceptDomain = conceptDomain;
		this.name = name;
		this.code = code;
		this.sources = sources;
	}

// Set methods
	public void setUri(String uri) { 
		this.uri = uri;
	}

	public void setDefaultCodingScheme(String defaultCodingScheme) { 
		this.defaultCodingScheme = defaultCodingScheme;
	}

	public void setConceptDomain(String conceptDomain) { 
		this.conceptDomain = conceptDomain;
	}

	public void setName(String name) { 
		this.name = name;
	}

	public void setCode(String code) { 
		this.code = code;
	}

	public void setSources(List sources) { 
		this.sources = sources;
	}


// Get methods
	public String getUri() { 
		return this.uri;
	}

	public String getDefaultCodingScheme() { 
		return this.defaultCodingScheme;
	}

	public String getConceptDomain() { 
		return this.conceptDomain;
	}

	public String getName() { 
		return this.name;
	}

	public String getCode() { 
		return this.code;
	}

	public List getSources() { 
		return this.sources;
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
