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

public class MapToEntry2
{

// Variable declaration
	private String code;
	private String label;
	private String propertyCode;
	private String propertyLabel;
	private String relationshipToTarget;
	private String targetCode;
	private String targetTerm;
	private String targetTermType;
	private String targetTerminology;
	private String targetTerminologyVersion;

// Default constructor
	public MapToEntry2() {
	}

// Constructor
	public MapToEntry2(
		String code,
		String label,
		String propertyCode,
		String propertyLabel,
		String relationshipToTarget,
		String targetCode,
		String targetTerm,
		String targetTermType,
		String targetTerminology,
		String targetTerminologyVersion) {

		this.code = code;
		this.label = label;
		this.propertyCode = propertyCode;
		this.propertyLabel = propertyLabel;
		this.relationshipToTarget = relationshipToTarget;
		this.targetCode = targetCode;
		this.targetTerm = targetTerm;
		this.targetTermType = targetTermType;
		this.targetTerminology = targetTerminology;
		this.targetTerminologyVersion = targetTerminologyVersion;
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

	public void setRelationshipToTarget(String relationshipToTarget) {
		this.relationshipToTarget = relationshipToTarget;
	}

	public void setTargetCode(String targetCode) {
		this.targetCode = targetCode;
	}

	public void setTargetTerm(String targetTerm) {
		this.targetTerm = targetTerm;
	}

	public void setTargetTermType(String targetTermType) {
		this.targetTermType = targetTermType;
	}

	public void setTargetTerminology(String targetTerminology) {
		this.targetTerminology = targetTerminology;
	}

	public void setTargetTerminologyVersion(String targetTerminologyVersion) {
		this.targetTerminologyVersion = targetTerminologyVersion;
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

	public String getRelationshipToTarget() {
		return this.relationshipToTarget;
	}

	public String getTargetCode() {
		return this.targetCode;
	}

	public String getTargetTerm() {
		return this.targetTerm;
	}

	public String getTargetTermType() {
		return this.targetTermType;
	}

	public String getTargetTerminology() {
		return this.targetTerminology;
	}

	public String getTargetTerminologyVersion() {
		return this.targetTerminologyVersion;
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
