package gov.nih.nci.evs.restapi.bean;

import java.io.*;
import java.util.*;
import java.net.*;

public class Property extends NameAndValue
{
	    private String source = null;
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

		public Property(
			String name,
			String value,
			String source) {
			super(name, value);
		}

		public void setSource(String source) {
			this.source = source;
		}

		public String getSource() {
			return this.source;
		}
}
