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

public class ConceptDetails extends Concept
{

// Variable declaration
	private List<Superclass> superclasses;
	private List<Role> roles;
	private List<InverseRole> inverseRoles;
	private List<Association> associations;
	private List<InverseAssociation> inverseAssociations;

// Default constructor
	public ConceptDetails() {
	}

// Constructor
	public ConceptDetails(
		String code,
		String name,
		String terminology,
		String version,
		List<Synonym> synonyms,
		List<Definition> definitions,
		List<Property> properties,
		List<Superclass> superclasses,
		List<Role> roles,
		List<InverseRole> inverseRoles,
		List<Association> associations,
		List<InverseAssociation> inverseAssociations) {

		super(code, name, terminology, version, synonyms, definitions, properties);

		this.superclasses = superclasses;
		this.roles = roles;
		this.inverseRoles = inverseRoles;
		this.associations = associations;
		this.inverseAssociations = inverseAssociations;
	}

// Set methods
	public void setSuperclasses(List<Superclass> superclasses) {
		this.superclasses = superclasses;
	}

	public void setRoles(List<Role> roles) {
		this.roles = roles;
	}

	public void setInverseRoles(List<InverseRole> inverseRoles) {
		this.inverseRoles = inverseRoles;
	}

	public void setAssociations(List<Association> associations) {
		this.associations = associations;
	}

	public void setInverseAssociations(List<InverseAssociation> inverseAssociations) {
		this.inverseAssociations = inverseAssociations;
	}


// Get methods
	public List<Superclass> getSuperclasses() {
		return this.superclasses;
	}

	public List<Role> getRoles() {
		return this.roles;
	}

	public List<InverseRole> getInverseRoles() {
		return this.inverseRoles;
	}

	public List<Association> getAssociations() {
		return this.associations;
	}

	public List<InverseAssociation> getInverseAssociations() {
		return this.inverseAssociations;
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
