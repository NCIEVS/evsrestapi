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

public class Revision
{

// Variable declaration
	private String codingScheme;
	private String version;
	private String date;
	private List<EditAction> editActions;

// Default constructor
	public Revision() {
	}

// Constructor
	public Revision(
		String codingScheme,
		String version,
		String date,
		List<EditAction> editActions) {

		this.codingScheme = codingScheme;
		this.version = version;
		this.date = date;
		this.editActions = editActions;
	}

// Set methods
	public void setCodingScheme(String codingScheme) {
		this.codingScheme = codingScheme;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public void setEditActions(List<EditAction> editActions) {
		this.editActions = editActions;
	}


// Get methods
	public String getCodingScheme() {
		return this.codingScheme;
	}

	public String getVersion() {
		return this.version;
	}

	public String getDate() {
		return this.date;
	}

	public List<EditAction> getEditActions() {
		return this.editActions;
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
