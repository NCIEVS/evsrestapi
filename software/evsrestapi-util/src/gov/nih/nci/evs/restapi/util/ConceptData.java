package gov.nih.nci.evs.restapi.util;
import gov.nih.nci.evs.restapi.bean.*;
import gov.nih.nci.evs.restapi.common.*;

import java.io.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.*;
import org.json.*;

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


public class ConceptData {
    JSONUtils jsonUtils = null;
    HTTPUtils httpUtils = null;
    String named_graph = null;
    String prefixes = null;
    String serviceUrl = null;
    String restURL = null;
    String named_graph_id = ":NHC0";
    String BASE_URI = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl";

    ParserUtils parser = new ParserUtils();
    HashMap nameVersion2NamedGraphMap = null;
    HashMap ontologyUri2LabelMap = null;
    String version = null;
    String username = null;
    String password = null;
    OWLSPARQLUtils owlSPARQLUtils = null;
    MetadataUtils metadataUtils = null;
    AxiomParser axiomParser = null;// (String serviceUrl, String named_graph, String username, String password)

    public ConceptData(String serviceUrl, String named_graph, String username, String password) {
		this.serviceUrl = serviceUrl;
    	this.named_graph = named_graph;
    	this.username = username;
    	this.password = password;
        this.owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, username, password);
        this.owlSPARQLUtils.set_named_graph(named_graph);
		this.metadataUtils = new MetadataUtils(serviceUrl, username, password);
		this.version = metadataUtils.getVocabularyVersion(named_graph);
		this.axiomParser = new AxiomParser(serviceUrl, named_graph, username, password);
    }

    public OWLSPARQLUtils getOWLSPARQLUtils() {
		return this.owlSPARQLUtils;
	}


	public String construct_get_concept_properties(String named_graph, String code) {
		String prefixes = owlSPARQLUtils.getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("").append("\n");
		buf.append("SELECT distinct ?p_label ?p_value").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + "> {").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x :NHC0 ?x_code .").append("\n");
		buf.append("            ?x :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("").append("\n");
		buf.append("            ?p a owl:AnnotationProperty .").append("\n");
		buf.append("            ?x ?p ?p_value .").append("\n");
		buf.append("            ?p rdfs:label ?p_label .").append("\n");
		buf.append("      }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getConceptProperties(String named_graph, String code) {
		String query = construct_get_concept_properties(named_graph, code);
		Vector v = owlSPARQLUtils.executeQuery(query);
		if (v == null) return null;
		if (v.size() == 0) return v;
		v = new ParserUtils().getResponseValues(v);
		return new SortUtils().quickSort(v);
	}

    public void generate(String named_graph, String code) {
        Vector qualifiers = owlSPARQLUtils.getSupportedPropertyQualifiers(named_graph);
        Vector properties = new Vector();
        for (int i=0; i<qualifiers.size(); i++) {
			String line = (String) qualifiers.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String s = (String) u.elementAt(0);
			if (!properties.contains(s)) {
				properties.add(s);
			}
		}

		Vector v = getConceptProperties(named_graph, code);
		Vector w = new Vector();
		String label = owlSPARQLUtils.getLabel(code);
		w.add("<title>" + label + " (" + code + ")");
		w.add("<table>Terms & Properties");
		w.add("<th>Name");
		w.add("<th>Value");
		w.add("<data>");
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String name = (String) u.elementAt(0);
			if (!properties.contains(name)) {
				String value = (String) u.elementAt(1);
				if (value.indexOf(BASE_URI) != -1) {
					int n = value.lastIndexOf("#");
					value = value.substring(n+1, value.length());
					value = owlSPARQLUtils.getLabel(value) + " (" + HyperlinkHelper.toHyperlink(value) + ")";
				} else if (HTMLTable.isCode(value) && value.length() <8) {
					value = owlSPARQLUtils.getLabel(value) + " (" + HyperlinkHelper.toHyperlink(value) + ")";
				}
				w.add(name + "|" + value);
			}
		}
		w.add("</data>");
		w.add("</table>");
		Vector axiomData = axiomParser.getAxioms(named_graph, code);
		for (int i=0; i<properties.size(); i++) {
            String property = (String) properties.elementAt(i);
			w.add("<table>" + property);
			w.add("<th>Value");
			for (int j=0; j<qualifiers.size(); j++) {
				String line = (String) qualifiers.elementAt(j);
				Vector u = StringUtils.parseData(line, '|');
				String type = (String) u.elementAt(0);
				if (type.compareTo(property) == 0) {
					String s = (String) u.elementAt(2);
					w.add("<th>" + s);
				}
			}
			w.add("<data>");
			Vector w1 = filterAxiomData(axiomData, property);
			w.addAll(w1);
		    w.add("</data>");
    		w.add("</table>");
		}

		w.add("<footer>(Source; NCI Thesaurus, version " + version + ")");

		HTMLTable.generate(w);
	}

	public Vector filterAxiomData(Vector axiomData, String propertyName) {
	    Vector w = new Vector();
	    if (propertyName.compareTo("FULL_SYN") == 0) {
			for (int i=0; i<axiomData.size(); i++) {
				Object obj = axiomData.elementAt(i);
				if (obj instanceof Synonym) {
					Synonym syn = (Synonym) obj;
					String value = syn.getTermName() + "|"
					  + syn.getTermGroup() + "|"
					  + syn.getTermSource() + "|"
					  + syn.getSourceCode() + "|"
					  + syn.getSubSourceName();
					value = value.replace("null", "");
					w.add(value);
				}
			}
	    } else if (propertyName.compareTo("DEFINITION") == 0) {
			for (int i=0; i<axiomData.size(); i++) {
				Object obj = axiomData.elementAt(i);
				if (obj instanceof Definition) {
					Definition def = (Definition) obj;
					String value = def.getDescription() + "|"
					  + def.getSource() + "|"
					  + def.getAttribution();
					value = value.replace("null", "");
					w.add(value);

				}
			}

	    } else if (propertyName.compareTo("ALT_DEFINITION") == 0) {
			for (int i=0; i<axiomData.size(); i++) {
				Object obj = axiomData.elementAt(i);
				if (obj instanceof AltDefinition) {
					AltDefinition def = (AltDefinition) obj;
					String value = def.getDescription() + "|"
					  + def.getSource() + "|"
					  + def.getAttribution();
					value = value.replace("null", "");
					w.add(value);
				}
			}

	    } else if (propertyName.compareTo("Maps_To") == 0) {
			for (int i=0; i<axiomData.size(); i++) {
				Object obj = axiomData.elementAt(i);
				if (obj instanceof MapToEntry) {
					MapToEntry e = (MapToEntry) obj;
					String value = e.getPreferredName() + "|"
					  + e.getRelationshipToTarget() + "|"
					  + e.getTargetTermType() + "|"
					  + e.getTargetCode() + "|"
					  + e.getTargetTerminology() + "|"
					  + e.getTargetTerminologyVersion();
					value = value.replace("null", "");
					w.add(value);
				}
			}

	    } else if (propertyName.compareTo("GO_Annotation") == 0) {
			for (int i=0; i<axiomData.size(); i++) {
				Object obj = axiomData.elementAt(i);
				if (obj instanceof GoAnnotation) {
					GoAnnotation e = (GoAnnotation) obj;
					String value = e.getAnnotation() + "|"
					  + e.getGoEvi() + "|"
					  + e.getGoSource() + "|"
					  + e.getSourceDate();
					value = value.replace("null", "");
					w.add(value);
				}
			}
		}
		return w;
	}

    public static void main(String[] args) {
		long ms = System.currentTimeMillis();
		String serviceUrl = args[0];
		String namedGraph = args[1];
		String username = args[2];
		String password = args[3];
		String code = args[4];
        ConceptData test = new ConceptData(serviceUrl, namedGraph, username, password);
        OWLSPARQLUtils owlSPARQLUtils = test.getOWLSPARQLUtils();
        test.generate(namedGraph, code);
    }

}