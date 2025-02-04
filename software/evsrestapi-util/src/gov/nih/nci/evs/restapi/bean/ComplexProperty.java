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

public class ComplexProperty extends Property
{

// Variable declaration
	private String name;
	private String value;
	private List<PropertyQualifier> qualifiers;

// Default constructor
	public ComplexProperty() {
	}

// Constructor
	public ComplexProperty(
		String name,
		String value,
		List<PropertyQualifier> qualifiers) {
		this.name = name;
		this.value = value;
		this.qualifiers = qualifiers;
	}

// Set methods
	public void setName(String name) {
		this.name = name;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public void setQualifiers(List<PropertyQualifier> qualifiers) {
		this.qualifiers = qualifiers;
	}


// Get methods
	public String getName() {
		return this.name;
	}

	public String getValue() {
		return this.value;
	}

	public List<PropertyQualifier> getQualifiers() {
		return this.qualifiers;
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
