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

public class RelatedBiomarker
{

// Variable declaration
	private String codingScheme;
	private String version;
	private String label;
	private String code;
	private List biomarkers;

// Default constructor
	public RelatedBiomarker() {
	}

// Constructor
	public RelatedBiomarker(
		String codingScheme,
		String version,
		String label,
		String code,
		List biomarkers) {

		this.codingScheme = codingScheme;
		this.version = version;
		this.label = label;
		this.code = code;
		this.biomarkers = biomarkers;
	}

// Set methods
	public void setCodingScheme(String codingScheme) { 
		this.codingScheme = codingScheme;
	}

	public void setVersion(String version) { 
		this.version = version;
	}

	public void setLabel(String label) { 
		this.label = label;
	}

	public void setCode(String code) { 
		this.code = code;
	}

	public void setBiomarkers(List biomarkers) { 
		this.biomarkers = biomarkers;
	}


// Get methods
	public String getCodingScheme() { 
		return this.codingScheme;
	}

	public String getVersion() { 
		return this.version;
	}

	public String getLabel() { 
		return this.label;
	}

	public String getCode() { 
		return this.code;
	}

	public List getBiomarkers() { 
		return this.biomarkers;
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
