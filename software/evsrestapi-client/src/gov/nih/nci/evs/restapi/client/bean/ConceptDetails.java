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

public class ConceptDetails
{

// Variable declaration
	private String code;
	private String name;
	private String terminology;
	private String version;
	private boolean leaf;
	private List<Synonym> synonyms;
	private List<Definition> definitions;
	private List<Property> properties;
	private List<Parent> parents;
	private List<Child> children;
	private List<Association> associations;
	private List<InverseAssociation> inverseAssociations;
	private List<Role> roles;
	private List<InverseRole> inverseRoles;
	private List<Map> maps;

// Default constructor
	public ConceptDetails() {
	}

// Constructor
	public ConceptDetails(
		String code,
		String name,
		String terminology,
		String version,
		boolean leaf,
		List<Synonym> synonyms,
		List<Definition> definitions,
		List<Property> properties,
		List<Parent> parents,
		List<Child> children,
		List<Association> associations,
		List<InverseAssociation> inverseAssociations,
		List<Role> roles,
		List<InverseRole> inverseRoles,
		List<Map> maps) {

		this.code = code;
		this.name = name;
		this.terminology = terminology;
		this.version = version;
		this.leaf = leaf;
		this.synonyms = synonyms;
		this.definitions = definitions;
		this.properties = properties;
		this.parents = parents;
		this.children = children;
		this.associations = associations;
		this.inverseAssociations = inverseAssociations;
		this.roles = roles;
		this.inverseRoles = inverseRoles;
		this.maps = maps;
	}

// Set methods
	public void setCode(String code) {
		this.code = code;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setTerminology(String terminology) {
		this.terminology = terminology;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public void setLeaf(boolean leaf) {
		this.leaf = leaf;
	}

	public void setSynonyms(List<Synonym> synonyms) {
		this.synonyms = synonyms;
	}

	public void setDefinitions(List<Definition> definitions) {
		this.definitions = definitions;
	}

	public void setProperties(List<Property> properties) {
		this.properties = properties;
	}

	public void setParents(List<Parent> parents) {
		this.parents = parents;
	}

	public void setChildren(List<Child> children) {
		this.children = children;
	}

	public void setAssociations(List<Association> associations) {
		this.associations = associations;
	}

	public void setInverseAssociations(List<InverseAssociation> inverseAssociations) {
		this.inverseAssociations = inverseAssociations;
	}

	public void setRoles(List<Role> roles) {
		this.roles = roles;
	}

	public void setInverseRoles(List<InverseRole> inverseRoles) {
		this.inverseRoles = inverseRoles;
	}

	public void setMaps(List<Map> maps) {
		this.maps = maps;
	}


// Get methods
	public String getCode() {
		return this.code;
	}

	public String getName() {
		return this.name;
	}

	public String getTerminology() {
		return this.terminology;
	}

	public String getVersion() {
		return this.version;
	}

	public boolean getLeaf() {
		return this.leaf;
	}

	public List<Synonym> getSynonyms() {
		return this.synonyms;
	}

	public List<Definition> getDefinitions() {
		return this.definitions;
	}

	public List<Property> getProperties() {
		return this.properties;
	}

	public List<Parent> getParents() {
		return this.parents;
	}

	public List<Child> getChildren() {
		return this.children;
	}

	public List<Association> getAssociations() {
		return this.associations;
	}

	public List<InverseAssociation> getInverseAssociations() {
		return this.inverseAssociations;
	}

	public List<Role> getRoles() {
		return this.roles;
	}

	public List<InverseRole> getInverseRoles() {
		return this.inverseRoles;
	}

	public List<Map> getMaps() {
		return this.maps;
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
