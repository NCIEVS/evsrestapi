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

public class Atom
{

// Variable declaration
	private String code;
	private String str;
	private String aui;
	private String sab;
	private String cui;
	private String ispref;
	private String lat;
	private String stt;
	private String ts;
	private String tty;

// Default constructor
	public Atom() {
	}

// Constructor
	public Atom(
		String code,
		String str,
		String aui,
		String sab,
		String cui,
		String ispref,
		String lat,
		String stt,
		String ts,
		String tty) {

		this.code = code;
		this.str = str;
		this.aui = aui;
		this.sab = sab;
		this.cui = cui;
		this.ispref = ispref;
		this.lat = lat;
		this.stt = stt;
		this.ts = ts;
		this.tty = tty;
	}

	public Atom(
		String code,
		String str,
		String aui,
		String sab,
		String cui
        ) {
		this.code = code;
		this.str = str;
		this.aui = aui;
		this.sab = sab;
		this.cui = cui;
		this.ispref = "Y";
		this.lat = "ENG";
		this.stt = "PF";
		this.ts = "P";
		this.tty = null;
	}

	public Atom(
		String code,
		String str,
		String aui,
		String sab,
		String cui,
		String tty) {
		this.code = code;
		this.str = str;
		this.aui = aui;
		this.sab = sab;
		this.cui = cui;
		this.ispref = "Y";
		this.lat = "ENG";
		this.stt = "PF";
		this.ts = "P";
		this.tty = tty;
	}

// Set methods
	public void setCode(String code) {
		this.code = code;
	}

	public void setStr(String str) {
		this.str = str;
	}

	public void setAui(String aui) {
		this.aui = aui;
	}

	public void setSab(String sab) {
		this.sab = sab;
	}

	public void setCui(String cui) {
		this.cui = cui;
	}

	public void setIspref(String ispref) {
		this.ispref = ispref;
	}

	public void setLat(String lat) {
		this.lat = lat;
	}

	public void setStt(String stt) {
		this.stt = stt;
	}

	public void setTs(String ts) {
		this.ts = ts;
	}

	public void setTty(String tty) {
		this.tty = tty;
	}


// Get methods
	public String getCode() {
		return this.code;
	}

	public String getStr() {
		return this.str;
	}

	public String getAui() {
		return this.aui;
	}

	public String getSab() {
		return this.sab;
	}

	public String getCui() {
		return this.cui;
	}

	public String getIspref() {
		return this.ispref;
	}

	public String getLat() {
		return this.lat;
	}

	public String getStt() {
		return this.stt;
	}

	public String getTs() {
		return this.ts;
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
