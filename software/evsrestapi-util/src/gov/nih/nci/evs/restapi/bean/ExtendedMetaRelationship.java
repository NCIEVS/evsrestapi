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

public class ExtendedMetaRelationship
{

// Variable declaration
	private MetaRelationship metaRelationship;
	private String sortKey;

// Default constructor
	public ExtendedMetaRelationship() {
	}

// Constructor
	public ExtendedMetaRelationship(
		MetaRelationship metaRelationship,
		String sortKey) {

		this.metaRelationship = metaRelationship;
		this.sortKey = sortKey;
	}

// Set methods
	public void setMetaRelationship(MetaRelationship metaRelationship) { 
		this.metaRelationship = metaRelationship;
	}

	public void setSortKey(String sortKey) { 
		this.sortKey = sortKey;
	}


// Get methods
	public MetaRelationship getMetaRelationship() { 
		return this.metaRelationship;
	}

	public String getSortKey() { 
		return this.sortKey;
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
