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

public class MapToEntry
{

// Variable declaration
	private String code;
	private String preferredName;
	private String relationshipToTarget;
	private String targetCode;
	private String targetTerm;
	private String targetTermType;
	private String targetTerminology;
	private String targetTerminologyVersion;

// Default constructor
	public MapToEntry() {
	}

// Constructor
	public MapToEntry(
		String code,
		String preferredName,
		String relationshipToTarget,
		String targetCode,
		String targetTerm,
		String targetTermType,
		String targetTerminology,
		String targetTerminologyVersion) {

		this.code = code;
		this.preferredName = preferredName;
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

	public void setPreferredName(String preferredName) {
		this.preferredName = preferredName;
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

	public String getPreferredName() {
		return this.preferredName;
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
