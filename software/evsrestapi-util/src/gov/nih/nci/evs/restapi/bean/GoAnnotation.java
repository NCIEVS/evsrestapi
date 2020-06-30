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

public class GoAnnotation
{

// Variable declaration
	private String code;
	private String label;
	private String annotation;
	private String goEvi;
	private String goId;
	private String goSource;
	private String sourceDate;

// Default constructor
	public GoAnnotation() {
	}

// Constructor
	public GoAnnotation(
		String code,
		String label,
		String annotation,
		String goEvi,
		String goId,
		String goSource,
		String sourceDate) {

		this.code = code;
		this.label = label;
		this.annotation = annotation;
		this.goEvi = goEvi;
		this.goId = goId;
		this.goSource = goSource;
		this.sourceDate = sourceDate;
	}

// Set methods
	public void setCode(String code) {
		this.code = code;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public void setAnnotation(String annotation) {
		this.annotation = annotation;
	}

	public void setGoEvi(String goEvi) {
		this.goEvi = goEvi;
	}

	public void setGoId(String goId) {
		this.goId = goId;
	}

	public void setGoSource(String goSource) {
		this.goSource = goSource;
	}

	public void setSourceDate(String sourceDate) {
		this.sourceDate = sourceDate;
	}


// Get methods
	public String getCode() {
		return this.code;
	}

	public String getLabel() {
		return this.label;
	}

	public String getAnnotation() {
		return this.annotation;
	}

	public String getGoEvi() {
		return this.goEvi;
	}

	public String getGoId() {
		return this.goId;
	}

	public String getGoSource() {
		return this.goSource;
	}

	public String getSourceDate() {
		return this.sourceDate;
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
		return this.code + "|"
		     + this.label + "|"
		     + "P211|"
		     + this.annotation + "|"
		     + "P389|"
		     + this.goEvi + "|"
		     + "P387|"
		     + this.goId + "|"
		     + "P390|"
		     + this.goSource + "|"
		     + "P391|"
		     + this.sourceDate;
	}
}
