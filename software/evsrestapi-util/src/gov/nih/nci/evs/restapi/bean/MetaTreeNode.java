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

public class MetaTreeNode
{

// Variable declaration
	public String _cui;
	public String _text;
	public String _sab;
	public String _aui;
	public String _code;
	public Iterator children;
	public boolean _expandable;
	public HashMap asso2ChildNodeMap;

// Default constructor
	public MetaTreeNode() {
		asso2ChildNodeMap = new HashMap();
	}

// Constructor
	public MetaTreeNode(
		String cui,
		String name,
		Iterator children,
		boolean expanded) {

		this._cui = cui;
		this._text = name;
		this.children = children;
		this._expandable = expanded;
		this.asso2ChildNodeMap = new HashMap();
	}

	public MetaTreeNode(
		String cui,
		String name) {
		this._cui = cui;
		this._text = name;
		this.asso2ChildNodeMap = new HashMap();
	}

// Set methods
	public void setCui(String cui) {
		this._cui = cui;
	}

	public void setCode(String code) {
		this._code = code;
	}

	public void setSab(String sab) {
		this._sab = sab;
	}

	public void setAui(String aui) {
		this._aui = aui;
	}

	public void setText(String name) {
		this._text = name;
	}

	public void setChildren(Iterator children) {
		this.children = children;
	}

	public void setExpanded(boolean expanded) {
		this._expandable = expanded;
	}


    public void addChild(String assoc, MetaTreeNode childItem) {
        Vector v = (Vector) asso2ChildNodeMap.get(assoc);
        v.add(childItem);
        asso2ChildNodeMap.put(assoc, v);
	}


// Get methods
	public String getCui() {
		return this._cui;
	}

	public String getCode() {
		return this._code;
	}

	public String getSab() {
		return this._sab;
	}

	public String getAui() {
		return this._aui;
	}

	public String getText() {
		return this._text;
	}

	public Iterator getChildren() {
		return this.children;
	}

	public boolean getExpanded() {
		return this._expandable;
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
