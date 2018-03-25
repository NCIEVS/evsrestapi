package gov.nih.nci.evs.restapi.bean;

import java.io.*;
import java.util.*;
import java.net.*;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

@XStreamAlias("ComplexTerm")
public class ComplexTerm implements java.io.Serializable
{

// Variable declaration
    @XStreamImplicit
	private String name = "";
	private String group = "";
	private String source = "";
	private String code = "";
	private String subsource_name = "";

// Default constructor
	public ComplexTerm() {
	}

// Constructor
	public ComplexTerm(
		String name,
		String group,
		String source,

		String code,

		String subsource_name) {

		this.name = name;
		this.group = group;
		this.source = source;
		this.code = code;
		this.subsource_name = subsource_name;
	}

// Set methods
	public void setName(String name) {
		this.name = name;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public void setSource(String source) {
		this.source = source;
	}


	public void setCode(String code) {
		this.code = code;
	}

	public void setsubsource_name(String subsource_name) {
		this.subsource_name = subsource_name;
	}
// Get methods
	public String getName() {
		return this.name;
	}

	public String getGroup() {
		return this.group;
	}

	public String getSource() {
		return this.source;
	}


	public String getCode() {
		return this.code;
	}

	public String getsubsource_name() {
		return this.subsource_name;
	}

	public String getDisplayForm() {
		StringBuffer buf = new StringBuffer();
		buf.append(name + " (group:" + group + ", " + "source: " + source);
		if (code != null && code.compareTo("") != 0) {
			buf.append(", " + "code: " + code);
		}
		if (subsource_name != null && subsource_name.compareTo("") != 0) {
			buf.append(", " + "subsource_name: " + subsource_name);
		}
		buf.append(")");
		return buf.toString();
	}

    @Override
    public String toString() {
        return "ComplexTerm [name=" + name + "]" + "[group=" + group + "]" + "[source=" + source + "]" + "[code=" + code + "]" + "[subsource_name=" + subsource_name + "]";
    }
}

/*
<ncicp:ComplexTerm xmlns:ncicp="http://ncicb.nci.nih.gov/xml/owl/EVS/ComplexProperties.xsd#"><ncicp:term-name>Acinar Cell</ncicp:term-name><ncicp:term-group>SY</ncicp:term-group><ncicp:term-source>NCI</ncicp:term-source><ncicp:source-code>TCGA</ncicp:source-code></ncicp:ComplexTerm>


<ncicp:ComplexTerm xmlns:ncicp="http://ncicb.nci.nih.gov/xml/owl/EVS/ComplexProp
erties.xsd#"><ncicp:term-name>Acinar Cell</ncicp:term-name><ncicp:term-group>SY<
/ncicp:term-group><ncicp:term-source>NCI</ncicp:term-source><ncicp:source-code>T
CGA</ncicp:source-code></ncicp:ComplexTerm>
<ncicp:ComplexTerm xmlns:ncicp="http://ncicb.nci.nih.gov/xml/owl/EVS/ComplexProp
erties.xsd#"><ncicp:term-name>Acinar Cell</ncicp:term-name><ncicp:term-group>SY<
/ncicp:term-group><ncicp:term-source>NCI</ncicp:term-source><ncicp:source-code>T
CGA</ncicp:source-code><ncicp:subsource_name-name>caDSR</ncicp:subsource_name-name></ncicp
:ComplexTerm>
<ncicp:ComplexTerm xmlns:ncicp="http://ncicb.nci.nih.gov/xml/owl/EVS/ComplexProperties.xsd#"><ncicp:term-name>Acinar Cell</ncicp:term-name><ncicp:term-group>SY</ncicp:term-group><ncicp:term-source>NCI</ncicp:term-source><ncicp:source-code>TCGA</ncicp:source-code><ncicp:subsource_name-name>caDSR</ncicp:subsource_name-name></ncicp:ComplexTerm>
*/
