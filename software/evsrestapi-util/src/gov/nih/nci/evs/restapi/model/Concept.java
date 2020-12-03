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

public class Concept
{

// Variable declaration
	private String code;
	private String name;
	private String terminology;
	private String version;
	private List<Synonym> synonyms;
	private List<Definition> definitions;
	private List<Property> properties;

// Default constructor
	public Concept() {
	}

// Constructor
	public Concept(
		String code,
		String name,
		String terminology,
		String version,
		List<Synonym> synonyms,
		List<Definition> definitions,
		List<Property> properties) {

		this.code = code;
		this.name = name;
		this.terminology = terminology;
		this.version = version;
		this.synonyms = synonyms;
		this.definitions = definitions;
		this.properties = properties;
	}

// Set methods
	public void setCode(String code) {
		this.code = code;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setTerminology(String terminology) {
		this.terminology = terminology;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public void setSynonyms(List<Synonym> synonyms) {
		this.synonyms = synonyms;
	}

	public void setDefinitions(List<Definition> definitions) {
		this.definitions = definitions;
	}

	public void setProperties(List<Property> properties) {
		this.properties = properties;
	}


// Get methods
	public String getCode() {
		return this.code;
	}

	public String getName() {
		return this.name;
	}

	public String getTerminology() {
		return this.terminology;
	}

	public String getVersion() {
		return this.version;
	}

	public List<Synonym> getSynonyms() {
		return this.synonyms;
	}

	public List<Definition> getDefinitions() {
		return this.definitions;
	}

	public List<Property> getProperties() {
		return this.properties;
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
