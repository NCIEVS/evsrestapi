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

public class OWLRestriction
{

// Variable declaration
	private String classId;
	private String onProperty;
	private String someValuesFrom;

// Default constructor
	public OWLRestriction() {
	}

// Constructor
	public OWLRestriction(
		String classId,
		String onProperty,
		String someValuesFrom) {

		this.classId = classId;
		this.onProperty = onProperty;
		this.someValuesFrom = someValuesFrom;
	}

// Set methods
	public void setClassId(String classId) {
		this.classId = classId;
	}

	public void setOnProperty(String onProperty) {
		this.onProperty = onProperty;
	}

	public void setSomeValuesFrom(String someValuesFrom) {
		this.someValuesFrom = someValuesFrom;
	}


// Get methods
	public String getClassId() {
		return this.classId;
	}

	public String getOnProperty() {
		return this.onProperty;
	}

	public String getSomeValuesFrom() {
		return this.someValuesFrom;
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

 	public String toString() {
		return this.classId + "|" +  this.onProperty + "|" + this.someValuesFrom;
	}
}
