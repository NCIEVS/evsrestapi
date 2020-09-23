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

public class Synonym2
{

// Variable declaration
	private String code;
	private String label;
	private String propertyCode;
	private String propertyLabel;
	private String termName;
	private String termGroup;
	private String termSource;
	private String sourceCode;
	private String subSourceName;
	private String subSourceCode;

// Default constructor
	public Synonym2() {
	}

// Constructor
	public Synonym2(
		String code,
		String label,
		String propertyCode,
		String propertyLabel,
		String termName,
		String termGroup,
		String termSource,
		String sourceCode,
		String subSourceName,
		String subSourceCode) {

		this.code = code;
		this.label = label;
		this.propertyCode = propertyCode;
		this.propertyLabel = propertyLabel;
		this.termName = termName;
		this.termGroup = termGroup;
		this.termSource = termSource;
		this.sourceCode = sourceCode;
		this.subSourceName = subSourceName;
		this.subSourceCode = subSourceCode;
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

	public void setTermName(String termName) {
		this.termName = termName;
	}

	public void setTermGroup(String termGroup) {
		this.termGroup = termGroup;
	}

	public void setTermSource(String termSource) {
		this.termSource = termSource;
	}

	public void setSourceCode(String sourceCode) {
		this.sourceCode = sourceCode;
	}

	public void setSubSourceName(String subSourceName) {
		this.subSourceName = subSourceName;
	}

	public void setSubSourceCode(String subSourceCode) {
		this.subSourceCode = subSourceCode;
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

	public String getTermName() {
		return this.termName;
	}

	public String getTermGroup() {
		return this.termGroup;
	}

	public String getTermSource() {
		return this.termSource;
	}

	public String getSourceCode() {
		return this.sourceCode;
	}

	public String getSubSourceName() {
		return this.subSourceName;
	}

	public String getSubSourceCode() {
		return this.subSourceCode;
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
