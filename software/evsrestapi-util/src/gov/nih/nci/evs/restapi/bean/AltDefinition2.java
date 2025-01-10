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

public class AltDefinition2
{

// Variable declaration
	private String code;
	private String label;
	private String propertyCode;
	private String propertyLabel;
	private String description;
	private String attribution;
	private String source;

// Default constructor
	public AltDefinition2() {
	}

// Constructor
	public AltDefinition2(
		String code,
		String label,
		String propertyCode,
		String propertyLabel,
		String description,
		String attribution,
		String source) {

		this.code = code;
		this.label = label;
		this.propertyCode = propertyCode;
		this.propertyLabel = propertyLabel;
		this.description = description;
		this.attribution = attribution;
		this.source = source;
	}

// Set methods
	public void setCode(String code) {
		this.code = code;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public void setPropertyCode(String propertyCode) {
		this.propertyCode = propertyCode;
	}

	public void setPropertyLabel(String propertyLabel) {
		this.propertyLabel = propertyLabel;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setAttribution(String attribution) {
		this.attribution = attribution;
	}

	public void setSource(String source) {
		this.source = source;
	}


// Get methods
	public String getCode() {
		return this.code;
	}

	public String getLabel() {
		return this.label;
	}

	public String getPropertyCode() {
		return this.propertyCode;
	}

	public String getPropertyLabel() {
		return this.propertyLabel;
	}

	public String getDescription() {
		return this.description;
	}

	public String getAttribution() {
		return this.attribution;
	}

	public String getSource() {
		return this.source;
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
