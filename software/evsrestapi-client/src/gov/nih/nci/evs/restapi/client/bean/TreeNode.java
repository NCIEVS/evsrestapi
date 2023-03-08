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

public class TreeNode
{

// Variable declaration
	private String code;
	private String label;
	private boolean leaf;
	private boolean expanded;
	private List<TreeNode> children;

// Default constructor
	public TreeNode() {
	}

// Constructor
	public TreeNode(
		String code,
		String label,
		boolean leaf,
		boolean expanded,
		List<TreeNode> children) {

		this.code = code;
		this.label = label;
		this.leaf = leaf;
		this.expanded = expanded;
		this.children = children;
	}

// Set methods
	public void setCode(String code) { 
		this.code = code;
	}

	public void setLabel(String label) { 
		this.label = label;
	}

	public void setLeaf(boolean leaf) { 
		this.leaf = leaf;
	}

	public void setExpanded(boolean expanded) { 
		this.expanded = expanded;
	}

	public void setChildren(List<TreeNode> children) { 
		this.children = children;
	}


// Get methods
	public String getCode() { 
		return this.code;
	}

	public String getLabel() { 
		return this.label;
	}

	public boolean getLeaf() { 
		return this.leaf;
	}

	public boolean getExpanded() { 
		return this.expanded;
	}

	public List<TreeNode> getChildren() { 
		return this.children;
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
