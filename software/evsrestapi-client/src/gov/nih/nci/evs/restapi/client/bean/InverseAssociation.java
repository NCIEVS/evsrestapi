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

public class InverseAssociation
{

// Variable declaration
	private String type;
	private String relatedCode;
	private String relatedName;

// Default constructor
	public InverseAssociation() {
	}

// Constructor
	public InverseAssociation(
		String type,
		String relatedCode,
		String relatedName) {

		this.type = type;
		this.relatedCode = relatedCode;
		this.relatedName = relatedName;
	}

// Set methods
	public void setType(String type) {
		this.type = type;
	}

	public void setRelatedCode(String relatedCode) {
		this.relatedCode = relatedCode;
	}

	public void setRelatedName(String relatedName) {
		this.relatedName = relatedName;
	}


// Get methods
	public String getType() {
		return this.type;
	}

	public String getRelatedCode() {
		return this.relatedCode;
	}

	public String getRelatedName() {
		return this.relatedName;
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
