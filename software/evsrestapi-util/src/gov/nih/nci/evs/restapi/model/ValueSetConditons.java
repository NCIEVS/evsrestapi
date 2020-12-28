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

public class ValueSetConditons
{

// Variable declaration
	private String code;
	private String label;
	private String propertyValue;
	private Vector qualifierCodes;
	private Vector qualifierValues;

// Default constructor
	public ValueSetConditons() {
	}

// Constructor
	public ValueSetConditons(
		String code,
		String label,
		String propertyValue,
		Vector qualifierCodes,
		Vector qualifierValues) {

		this.code = code;
		this.label = label;
		this.propertyValue = propertyValue;
		this.qualifierCodes = qualifierCodes;
		this.qualifierValues = qualifierValues;
	}

// Set methods
	public void setCode(String code) {
		this.code = code;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public void setPropertyValue(String propertyValue) {
		this.propertyValue = propertyValue;
	}

	public void setQualifierCodes(Vector qualifierCodes) {
		this.qualifierCodes = qualifierCodes;
	}

	public void setQualifierValues(Vector qualifierValues) {
		this.qualifierValues = qualifierValues;
	}


// Get methods
	public String getCode() {
		return this.code;
	}

	public String getLabel() {
		return this.label;
	}

	public String getPropertyValue() {
		return this.propertyValue;
	}

	public Vector getQualifierCodes() {
		return this.qualifierCodes;
	}

	public Vector getQualifierValues() {
		return this.qualifierValues;
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
