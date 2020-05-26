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

public class GraphData
{

// Variable declaration
	private Vector nodes;
	private Vector edges;
	private Vector selectedNodes;
	private String format;
	private float ranksep;
	private float nodesep;
	private String filename;

// Default constructor
	public GraphData() {
	}

// Constructor
	public GraphData(
		Vector nodes,
		Vector edges,
		Vector selectedNodes,
		String format,
		float ranksep,
		float nodesep,
		String filename) {

		this.nodes = nodes;
		this.edges = edges;
		this.selectedNodes = selectedNodes;
		this.format = format;
		this.ranksep = ranksep;
		this.nodesep = nodesep;
		this.filename = filename;
	}

// Set methods
	public void setNodes(Vector nodes) {
		this.nodes = nodes;
	}

	public void setEdges(Vector edges) {
		this.edges = edges;
	}

	public void setSelectedNodes(Vector selectedNodes) {
		this.selectedNodes = selectedNodes;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public void setRanksep(float ranksep) {
		this.ranksep = ranksep;
	}

	public void setNodesep(float nodesep) {
		this.nodesep = nodesep;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}


// Get methods
	public Vector getNodes() {
		return this.nodes;
	}

	public Vector getEdges() {
		return this.edges;
	}

	public Vector getSelectedNodes() {
		return this.selectedNodes;
	}

	public String getFormat() {
		return this.format;
	}

	public float getRanksep() {
		return this.ranksep;
	}

	public float getNodesep() {
		return this.nodesep;
	}

	public String getFilename() {
		return this.filename;
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
