package gov.nih.nci.evs.restapi.bean;

import java.io.*;
import java.util.*;
import java.net.*;

public class ResolvedConceptReference implements java.io.Serializable
{

// Variable declaration
	private String codingScheme;
	private String version;
	private String code;
	private String namespace;
	private String name;

// Default constructor
	public ResolvedConceptReference() {
	}

// Constructor
	public ResolvedConceptReference(
		String codingScheme,
		String version,
		String code,
		String namespace,
		String name) {

		this.codingScheme = codingScheme;
		this.version = version;
		this.code = code;
		this.namespace = namespace;
		this.name = name;
	}

// Set methods
	public void setCodingScheme(String codingScheme) {
		this.codingScheme = codingScheme;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public void setName(String name) {
		this.name = name;
	}


// Get methods
	public String getCodingScheme() {
		return this.codingScheme;
	}

	public String getVersion() {
		return this.version;
	}

	public String getCode() {
		return this.code;
	}

	public String getNamespace() {
		return this.namespace;
	}

	public String getName() {
		return this.name;
	}

}
