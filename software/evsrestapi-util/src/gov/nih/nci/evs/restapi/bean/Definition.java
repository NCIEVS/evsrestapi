package gov.nih.nci.evs.restapi.bean;

import java.io.*;
import java.util.*;
import java.net.*;

public class Definition
{

// Variable declaration
	private String description;
	private String source;

// Default constructor
	public Definition() {
	}

// Constructor
	public Definition(
		String description,
		String source) {

		this.description = description;
		this.source = source;
	}

// Set methods
	public void setDescription(String description) { 
		this.description = description;
	}

	public void setSource(String source) { 
		this.source = source;
	}


// Get methods
	public String getDescription() { 
		return this.description;
	}

	public String getSource() { 
		return this.source;
	}

}
