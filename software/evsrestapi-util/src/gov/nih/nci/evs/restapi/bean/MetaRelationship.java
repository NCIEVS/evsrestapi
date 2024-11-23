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

public class MetaRelationship
{

// Variable declaration
	private String rel;
	private String rela;
	private String cui1;
	private String cui2;
	private String str;
	private String sab;
	private String code;
	private String tty;

// Default constructor
	public MetaRelationship() {
	}

// Constructor
	public MetaRelationship(
		String rel,
		String rela,
		String cui1,
		String cui2,
		String str,
		String sab,
		String code,
		String tty) {

		this.rel = rel;
		this.rela = rela;
		this.cui1 = cui1;
		this.cui2 = cui2;
		this.str = str;
		this.sab = sab;
		this.code = code;
		this.tty = tty;
	}

// Set methods
	public void setRel(String rel) {
		this.rel = rel;
	}

	public void setRela(String rela) {
		this.rela = rela;
	}

	public void setCui1(String cui1) {
		this.cui1 = cui1;
	}

	public void setCui2(String cui2) {
		this.cui2 = cui2;
	}

	public void setStr(String str) {
		this.str = str;
	}

	public void setSab(String sab) {
		this.sab = sab;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public void setTty(String tty) {
		this.tty = tty;
	}


// Get methods
	public String getRel() {
		return this.rel;
	}

	public String getRela() {
		return this.rela;
	}

	public String getCui1() {
		return this.cui1;
	}

	public String getCui2() {
		return this.cui2;
	}

	public String getStr() {
		return this.str;
	}

	public String getSab() {
		return this.sab;
	}

	public String getCode() {
		return this.code;
	}

	public String getTty() {
		return this.tty;
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
