package gov.nih.nci.evs.restapi.bean;

import java.io.*;
import java.util.*;
import java.net.*;

public class NameAndValue
{

// Variable declaration
	private String name;
	private String value;

// Default constructor
	public NameAndValue() {
	}

// Constructor
	public NameAndValue(
		String name,
		String value) {

		this.name = name;
		this.value = value;
	}

// Set methods
	public void setName(String name) {
		this.name = name;
	}

	public void setValue(String value) {
		this.value = value;
	}


// Get methods
	public String getName() {
		return this.name;
	}

	public String getValue() {
		return this.value;
	}

}
