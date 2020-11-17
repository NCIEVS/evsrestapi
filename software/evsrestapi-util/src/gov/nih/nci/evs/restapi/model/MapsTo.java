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

public class MapsTo
{

// Variable declaration
	private String type;
	private String targetName;
	private String targetTermGroup;
	private String targetCode;
	private String targetTerminology;
	private String targetTerminologyVersion;

// Default constructor
	public MapsTo() {
	}

// Constructor
	public MapsTo(
		String type,
		String targetName,
		String targetTermGroup,
		String targetCode,
		String targetTerminology,
		String targetTerminologyVersion) {

		this.type = type;
		this.targetName = targetName;
		this.targetTermGroup = targetTermGroup;
		this.targetCode = targetCode;
		this.targetTerminology = targetTerminology;
		this.targetTerminologyVersion = targetTerminologyVersion;
	}

// Set methods
	public void setType(String type) {
		this.type = type;
	}

	public void setTargetName(String targetName) {
		this.targetName = targetName;
	}

	public void setTargetTermGroup(String targetTermGroup) {
		this.targetTermGroup = targetTermGroup;
	}

	public void setTargetCode(String targetCode) {
		this.targetCode = targetCode;
	}

	public void setTargetTerminology(String targetTerminology) {
		this.targetTerminology = targetTerminology;
	}

	public void setTargetTerminologyVersion(String targetTerminologyVersion) {
		this.targetTerminologyVersion = targetTerminologyVersion;
	}


// Get methods
	public String getType() {
		return this.type;
	}

	public String getTargetName() {
		return this.targetName;
	}

	public String getTargetTermGroup() {
		return this.targetTermGroup;
	}

	public String getTargetCode() {
		return this.targetCode;
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
