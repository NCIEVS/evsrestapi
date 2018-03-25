package gov.nih.nci.evs.restapi.bean;

import java.io.*;
import java.util.*;
import java.net.*;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

@XStreamAlias("ComplexDefinition")
public class ComplexDefinition implements java.io.Serializable
{

// Variable declaration
    @XStreamImplicit
	private String definition;
	private String source;

// Default constructor
	public ComplexDefinition() {
	}

// Constructor
	public ComplexDefinition(
		String definition,
		String source) {

		this.definition = definition;
		this.source = source;
	}

// Set methods
	public void setDefinition(String definition) {
		this.definition = definition;
	}

	public void setSource(String source) {
		this.source = source;
	}


// Get methods
	public String getDefinition() {
		return this.definition;
	}

	public String getSource() {
		return this.source;
	}


	public String getDisplayForm() {
		return definition + " (source: " + source + ")";
	}

    @Override
    public String toString() {
        return "ComplexDefinition [definition=" + definition + "]" + "[source=" + source + "]";
    }
}
