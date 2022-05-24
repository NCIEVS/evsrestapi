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

public class Parameter
{

// Variable declaration
	private String term;
	private String type;
	private String include;
	private int fromRecord;
	private int pageSize;
	private List<String> terminology;

// Default constructor
	public Parameter() {
	}

// Constructor
	public Parameter(
		String term,
		String type,
		String include,
		int fromRecord,
		int pageSize,
		List<String> terminology) {

		this.term = term;
		this.type = type;
		this.include = include;
		this.fromRecord = fromRecord;
		this.pageSize = pageSize;
		this.terminology = terminology;
	}

// Set methods
	public void setTerm(String term) {
		this.term = term;
	}

	public void setType(String type) {
		this.type = type;
	}

	public void setInclude(String include) {
		this.include = include;
	}

	public void setFromRecord(int fromRecord) {
		this.fromRecord = fromRecord;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public void setTerminology(List<String> terminology) {
		this.terminology = terminology;
	}


// Get methods
	public String getTerm() {
		return this.term;
	}

	public String getType() {
		return this.type;
	}

	public String getInclude() {
		return this.include;
	}

	public int getFromRecord() {
		return this.fromRecord;
	}

	public int getPageSize() {
		return this.pageSize;
	}

	public List<String> getTerminology() {
		return this.terminology;
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
