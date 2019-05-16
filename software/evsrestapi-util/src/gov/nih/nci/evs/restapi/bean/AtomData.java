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

public class AtomData
{

// Variable declaration
	private String aui;
	private String code;
	private String cui;
	private String ispref;
	private String lat;
	private String lui;
	private String sab;
	private String str;
	private String stt;
	private String suppress;
	private String tty;
	private int rank;

// Default constructor
	public AtomData() {
	}

// Constructor
	public AtomData(
		String aui,
		String code,
		String cui,
		String ispref,
		String lat,
		String lui,
		String sab,
		String str,
		String stt,
		String suppress,
		String tty,
		int rank) {

		this.aui = aui;
		this.code = code;
		this.cui = cui;
		this.ispref = ispref;
		this.lat = lat;
		this.lui = lui;
		this.sab = sab;
		this.str = str;
		this.stt = stt;
		this.suppress = suppress;
		this.tty = tty;
		this.rank = rank;
	}

// Set methods
	public void setAui(String aui) { 
		this.aui = aui;
	}

	public void setCode(String code) { 
		this.code = code;
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

	public void setLui(String lui) { 
		this.lui = lui;
	}

	public void setSab(String sab) { 
		this.sab = sab;
	}

	public void setStr(String str) { 
		this.str = str;
	}

	public void setStt(String stt) { 
		this.stt = stt;
	}

	public void setSuppress(String suppress) { 
		this.suppress = suppress;
	}

	public void setTty(String tty) { 
		this.tty = tty;
	}

	public void setRank(int rank) { 
		this.rank = rank;
	}


// Get methods
	public String getAui() { 
		return this.aui;
	}

	public String getCode() { 
		return this.code;
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

	public String getLui() { 
		return this.lui;
	}

	public String getSab() { 
		return this.sab;
	}

	public String getStr() { 
		return this.str;
	}

	public String getStt() { 
		return this.stt;
	}

	public String getSuppress() { 
		return this.suppress;
	}

	public String getTty() { 
		return this.tty;
	}

	public int getRank() { 
		return this.rank;
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
