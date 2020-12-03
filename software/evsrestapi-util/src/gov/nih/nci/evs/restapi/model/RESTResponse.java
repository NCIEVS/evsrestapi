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

public class RESTResponse
{

// Variable declaration
	private String code;
	private String terminology;
	private List list;

// Default constructor
	public RESTResponse() {
	}

// Constructor
	public RESTResponse(
		String code,
		String terminology,
		List list) {

		this.code = code;
		this.terminology = terminology;
		this.list = list;
	}

// Set methods
	public void setCode(String code) {
		this.code = code;
	}

	public void setTerminology(String terminology) {
		this.terminology = terminology;
	}

	public void setList(List list) {
		this.list = list;
	}


// Get methods
	public String getCode() {
		return this.code;
	}

	public String getTerminology() {
		return this.terminology;
	}

	public List getList() {
		return this.list;
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
