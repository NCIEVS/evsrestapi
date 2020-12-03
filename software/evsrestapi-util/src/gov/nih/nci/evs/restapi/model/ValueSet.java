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

public class ValueSet
{
// Variable declaration
	private String code;
	private String name;
	private String terminology;
	private String version;
	private List<String> codes;

// Default constructor
	public ValueSet() {
	}

// Constructor
	public ValueSet(
		String code,
		String name,
		String terminology,
		String version,
		List<String> codes) {

		this.code = code;
		this.name = name;
		this.terminology = terminology;
		this.version = version;
		this.codes = codes;
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

	public void setCodes(List<String> codes) {
		this.codes = codes;
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

	public List<String> getCodes() {
		return this.codes;
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
