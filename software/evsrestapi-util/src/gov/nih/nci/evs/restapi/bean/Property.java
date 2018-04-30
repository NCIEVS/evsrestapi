package gov.nih.nci.evs.restapi.bean;

import java.io.*;
import java.util.*;
import java.net.*;

public class Property extends NameAndValue
{
	// Default constructor
		public Property() {
			super();
		}
	// Constructor
		public Property(
			String name,
			String value) {
			super(name, value);
		}
}
