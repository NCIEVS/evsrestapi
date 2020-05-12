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

public class OWLAxiom
{

// Variable declaration
	private int axiomId;
	private String label;
	private String annotatedSource;
	private String annotatedProperty;
	private String annotatedTarget;
	private String qualifierName;
	private String qualifierValue;

// Default constructor
	public OWLAxiom() {
	}

// Constructor
	public OWLAxiom(
		int axiomId,
		String label,
		String annotatedSource,
		String annotatedProperty,
		String annotatedTarget,
		String qualifierName,
		String qualifierValue) {

		this.axiomId = axiomId;
		this.label = label;
		this.annotatedSource = annotatedSource;
		this.annotatedProperty = annotatedProperty;
		this.annotatedTarget = annotatedTarget;
		this.qualifierName = qualifierName;
		this.qualifierValue = qualifierValue;
	}

// Set methods
	public void setAxiomId(int axiomId) {
		this.axiomId = axiomId;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public void setAnnotatedSource(String annotatedSource) {
		this.annotatedSource = annotatedSource;
	}

	public void setAnnotatedProperty(String annotatedProperty) {
		this.annotatedProperty = annotatedProperty;
	}

	public void setAnnotatedTarget(String annotatedTarget) {
		this.annotatedTarget = annotatedTarget;
	}

	public void setQualifierName(String qualifierName) {
		this.qualifierName = qualifierName;
	}

	public void setQualifierValue(String qualifierValue) {
		this.qualifierValue = qualifierValue;
	}


// Get methods
	public int getAxiomId() {
		return this.axiomId;
	}

	public String getLabel() {
		return this.label;
	}

	public String getAnnotatedSource() {
		return this.annotatedSource;
	}

	public String getAnnotatedProperty() {
		return this.annotatedProperty;
	}

	public String getAnnotatedTarget() {
		return this.annotatedTarget;
	}

	public String getQualifierName() {
		return this.qualifierName;
	}

	public String getQualifierValue() {
		return this.qualifierValue;
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
		return toString('|');
	}

	public String toString(char delim) {
		return "" + this.axiomId + delim
		     + this.label + delim
		     + this.annotatedSource + delim
		     + this.annotatedProperty + delim
		     + this.annotatedTarget + delim
		     + this.qualifierName + delim
		     + this.qualifierValue;
	}

}
