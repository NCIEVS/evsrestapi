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

public class MatchedConcept
{

// Variable declaration
	private String label;
	private String code;
	private String propertyName;
	private String propertyValue;
	private String termType;
	private int score;

// Default constructor
	public MatchedConcept() {
	}

// Constructor
	public MatchedConcept(
		String label,
		String code,
		String propertyName,
		String propertyValue,
		String termType,
		int score) {

		this.label = label;
		this.code = code;
		this.propertyName = propertyName;
		this.propertyValue = propertyValue;
		this.termType = termType;
		this.score = score;
	}

	public MatchedConcept(
		String label,
		String code,
		String propertyName,
		String propertyValue,
		int score) {

		this.label = label;
		this.code = code;
		this.propertyName = propertyName;
		this.propertyValue = propertyValue;
		this.termType = null;
		this.score = score;
	}

// Set methods
	public void setLabel(String label) {
		this.label = label;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	public void setPropertyValue(String propertyValue) {
		this.propertyValue = propertyValue;
	}

	public void setTermType(String termType) {
		this.termType = termType;
	}

	public void setScore(int score) {
		this.score = score;
	}


// Get methods
	public String getLabel() {
		return this.label;
	}

	public String getCode() {
		return this.code;
	}

	public String getPropertyName() {
		return this.propertyName;
	}

	public String getPropertyValue() {
		return this.propertyValue;
	}

	public String getTermType() {
		return this.termType;
	}

	public int getScore() {
		return this.score;
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
