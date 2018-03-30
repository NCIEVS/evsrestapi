package gov.nih.nci.evs.restapi.bean;

import java.io.*;
import java.util.*;
import java.net.*;

public class PropertyQualifier extends NameAndValue
{
// Default constructor
	public PropertyQualifier() {
		super();
	}

// Constructor
	public PropertyQualifier(
		String name,
		String value) {
		super(name, value);
	}
}
