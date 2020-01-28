package gov.nih.nci.evs.restapi.bean;


import java.io.*;
import java.net.*;
import java.util.*;

import com.google.gson.*;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.XStream;

/**
 * <!-- LICENSE_TEXT_START -->
 * Copyright 2008-2017 NGIS. This software was developed in conjunction
 * with the National Cancer Institute, and so to the extent government
 * employees are co-authors, any rights in such works shall be subject
 * to Title 17 of the United States Code, section 105.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *   1. Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the disclaimer of Article 3,
 *      below. Redistributions in binary form must reproduce the above
 *      copyright notice, this list of conditions and the following
 *      disclaimer in the documentation and/or other materials provided
 *      with the distribution.
 *   2. The end-user documentation included with the redistribution,
 *      if any, must include the following acknowledgment:
 *      "This product includes software developed by NGIS and the National
 *      Cancer Institute."   If no such end-user documentation is to be
 *      included, this acknowledgment shall appear in the software itself,
 *      wherever such third-party acknowledgments normally appear.
 *   3. The names "The National Cancer Institute", "NCI" and "NGIS" must
 *      not be used to endorse or promote products derived from this software.
 *   4. This license does not authorize the incorporation of this software
 *      into any third party proprietary programs. This license does not
 *      authorize the recipient to use any trademarks owned by either NCI
 *      or NGIS
 *   5. THIS SOFTWARE IS PROVIDED "AS IS," AND ANY EXPRESSED OR IMPLIED
 *      WARRANTIES, (INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *      OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE) ARE
 *      DISCLAIMED. IN NO EVENT SHALL THE NATIONAL CANCER INSTITUTE,
 *      NGIS, OR THEIR AFFILIATES BE LIABLE FOR ANY DIRECT, INDIRECT,
 *      INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *      BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *      LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *      CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *      LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *      ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *      POSSIBILITY OF SUCH DAMAGE.
 * <!-- LICENSE_TEXT_END -->
 */

/**
 * @author EVS Team
 * @version 1.0
 *
 * Modification history:
 *     Initial implementation kim.ong@ngc.com
 *
 */


public class Synonym
{

// Variable declaration
	private String code;
	private String label;
	private String termName;
	private String termGroup;
	private String termSource;
	private String sourceCode;
	private String subSourceName;
	private String subSourceCode;

// Default constructor
	public Synonym() {
	}

// Constructor
	public Synonym(
		String code,
		String label,
		String termName,
		String termGroup,
		String termSource,
		String sourceCode,
		String subSourceName,
		String subSourceCode) {

		this.code = code;
		this.label = label;
		this.termName = termName;
		this.termGroup = termGroup;
		this.termSource = termSource;
		this.sourceCode = sourceCode;
		this.subSourceName = subSourceName;
		this.subSourceCode = subSourceCode;
	}

	public Synonym(
		String termName,
		String termGroup,
		String termSource,
		String sourceCode) {
		this.code = null;
		this.label = null;
		this.termName = termName;
		this.termGroup = termGroup;
		this.termSource = termSource;
		this.sourceCode = sourceCode;
		this.subSourceName = null;
		this.subSourceCode = null;
	}

// Set methods
	public void setCode(String code) {
		this.code = code;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public void setTermName(String termName) {
		this.termName = termName;
	}

	public void setTermGroup(String termGroup) {
		this.termGroup = termGroup;
	}

	public void setTermSource(String termSource) {
		this.termSource = termSource;
	}

	public void setSourceCode(String sourceCode) {
		this.sourceCode = sourceCode;
	}

	public void setSubSourceName(String subSourceName) {
		this.subSourceName = subSourceName;
	}

	public void setSubSourceCode(String subSourceCode) {
		this.subSourceCode = subSourceCode;
	}


// Get methods
	public String getCode() {
		return this.code;
	}

	public String getLabel() {
		return this.label;
	}

	public String getTermName() {
		return this.termName;
	}

	public String getTermGroup() {
		return this.termGroup;
	}

	public String getTermSource() {
		return this.termSource;
	}

	public String getSourceCode() {
		return this.sourceCode;
	}

	public String getSubSourceName() {
		return this.subSourceName;
	}

	public String getSubSourceCode() {
		return this.subSourceCode;
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

	public String toString() {
		StringBuffer buf = new StringBuffer();
		//buf.append("code: ").append(code).append("\n");
		//buf.append("label: ").append(label).append("\n");
		buf.append("termName:").append(termName).append("\n");
		buf.append("\t").append("termGroup: ").append(termGroup).append("\n");
		buf.append("\t").append("termSource: ").append(termSource).append("\n");
		buf.append("\t").append("sourceCode: ").append(sourceCode).append("\n");
		buf.append("\t").append("subSourceName: ").append(subSourceName).append("\n");
		buf.append("\t").append("subSourceCode: ").append(subSourceCode);
        return buf.toString() + "\n";
	}
}
