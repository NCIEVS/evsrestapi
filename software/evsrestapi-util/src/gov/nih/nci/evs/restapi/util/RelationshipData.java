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


public class RelationshipData {
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
    public RelationshipData(String serviceUrl, String named_graph, String username, String password) {
		this.serviceUrl = serviceUrl;
    	this.named_graph = named_graph;
    	this.username = username;
    	this.password = password;
        this.owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, username, password);
        this.owlSPARQLUtils.set_named_graph(named_graph);
		this.metadataUtils = new MetadataUtils(serviceUrl, username, password);
		this.version = metadataUtils.getVocabularyVersion(named_graph);
    }

    public OWLSPARQLUtils getOWLSPARQLUtils() {
		return this.owlSPARQLUtils;
	}

    public void generate(String named_graph, String code) {
		Vector w = new Vector();
		String label = owlSPARQLUtils.getLabel(code);
		w.add("<title>" + label + " (" + code + ")");

		Vector v = getHierarchicallyRrelatedConcepts(named_graph, code, true);
		w.add("<table>Superconcepts");
		w.add("<th>Label");
		w.add("<th>Code");
		w.add("<data>");
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String concept_label = (String) u.elementAt(2);
			String concept_code = (String) u.elementAt(3);
			concept_code = HyperlinkHelper.toHyperlink(concept_code);
            w.add(concept_label + "|" + concept_code);
		}
		w.add("</data>");
		w.add("</table>");

		v = getHierarchicallyRrelatedConcepts(named_graph, code, false);
		w.add("<table>Subconcepts");
		w.add("<th>Label");
		w.add("<th>Code");
		w.add("<data>");
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String concept_label = (String) u.elementAt(0);
			String concept_code = (String) u.elementAt(1);
			concept_code = HyperlinkHelper.toHyperlink(concept_code);
            w.add(concept_label + "|" + concept_code);
		}
		w.add("</data>");
		w.add("</table>");

        boolean outbound = true;
        v = getRestrictions(named_graph, code, outbound);
		w.add("<table>Outbound Roles");
		w.add("<th>Role");
		w.add("<th>Target Concept Label");
		w.add("<th>Target Concept Code");
		w.add("<data>");
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String property_name = (String) u.elementAt(3);
			String target_concept_label = (String) u.elementAt(5);
			String target_concept_code = (String) u.elementAt(4);
			target_concept_code = HyperlinkHelper.toHyperlink(target_concept_code);
            w.add(property_name + "|" + target_concept_label + "|" + target_concept_code);
		}
		w.add("</data>");
		w.add("</table>");

		outbound = false;
        v = getRestrictions(named_graph, code, outbound);
		w.add("<table>Inbound Roles");
		w.add("<th>Source Concept Label");
		w.add("<th>Source Concept Code");
		w.add("<th>Role");
		w.add("<data>");
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String property_name = (String) u.elementAt(3);
			String source_concept_label = (String) u.elementAt(1);
			String source_concept_code = (String) u.elementAt(0);
			source_concept_code = HyperlinkHelper.toHyperlink(source_concept_code);
            w.add(source_concept_label + "|" + source_concept_code + "|" + property_name);
		}
		w.add("</data>");
		w.add("</table>");

        outbound = true;
        v = getAssociation(named_graph, code, null, outbound);
		w.add("<table>Outbound Associations");
		w.add("<th>Association");
		w.add("<th>Target Concept Label");
		w.add("<th>Target Concept Code");
		w.add("<data>");
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String property_name = (String) u.elementAt(2);
			String target_concept_label = (String) u.elementAt(4);
			String target_concept_code = (String) u.elementAt(5);
			target_concept_code = HyperlinkHelper.toHyperlink(target_concept_code);
            w.add(property_name + "|" + target_concept_label + "|" + target_concept_code);
		}
		w.add("</data>");
		w.add("</table>");

		outbound = false;
        v = getAssociation(named_graph, code, null, outbound);
		w.add("<table>Inbound Associations");
		w.add("<th>Source Concept Label");
		w.add("<th>Source Concept Code");
		w.add("<th>Association");
		w.add("<data>");
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String source_concept_label = (String) u.elementAt(0);
			String source_concept_code = (String) u.elementAt(1);
			String property_name = (String) u.elementAt(2);
			source_concept_code = HyperlinkHelper.toHyperlink(source_concept_code);
            w.add(source_concept_label + "|" + source_concept_code + "|" + property_name);
		}
		w.add("</data>");
		w.add("</table>");
		w.add("<footer>(Source; NCI Thesaurus, version " + version + ")");
		HTMLTable.generate(w);
	}

	public String construct_get_association(String named_graph, String code, String propertyName, boolean outbound) {
		String prefixes = owlSPARQLUtils.getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("").append("\n");
		buf.append("SELECT distinct ?x_label ?x_code ?p_label ?p_code ?y_label ?y_code").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + "> {").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x :NHC0 ?x_code .").append("\n");
		if (outbound) {
			buf.append("            ?x :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
		}
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("").append("\n");
		buf.append("            ?y a owl:Class .").append("\n");
		buf.append("            ?y :NHC0 ?y_code .").append("\n");
		if (!outbound) {
			buf.append("            ?y :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
	    }
		buf.append("            ?y rdfs:label ?y_label .").append("\n");
		buf.append("").append("\n");
		buf.append("            ?p a owl:AnnotationProperty .").append("\n");
		buf.append("            ?x ?p ?y .").append("\n");
		buf.append("            ?p :NHC0 ?p_code .").append("\n");
		buf.append("            ?p rdfs:label ?p_label .").append("\n");
		if (propertyName != null) {
			buf.append("            ?p rdfs:label \"" + propertyName + "\"^^xsd:string .").append("\n");
		}
		buf.append("     }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}


	public Vector getAssociation(String named_graph, String code, String propertyName, boolean outbound) {
		String query = construct_get_association(named_graph, code, propertyName, outbound);
		Vector v = owlSPARQLUtils.executeQuery(query);
		if (v == null) return null;
		if (v.size() == 0) return v;
		v = new ParserUtils().getResponseValues(v);
		return new SortUtils().quickSort(v);
	}

	public Vector getSuperconcepts(String named_graph, String code) {
        Vector v = owlSPARQLUtils.getSuperclassesByCode(named_graph, code);
		if (v == null) return null;
		if (v.size() == 0) return v;
		v = new ParserUtils().getResponseValues(v);
		return new SortUtils().quickSort(v);
	}

	public Vector getSubconcepts(String named_graph, String code) {
        Vector v = owlSPARQLUtils.getSubclassesByCode(named_graph, code);
		if (v == null) return null;
		if (v.size() == 0) return v;
		v = new ParserUtils().getResponseValues(v);
		return new SortUtils().quickSort(v);
	}


	public String construct_get_restrictions(String named_graph, String code, boolean outbound) {
		String prefixes = owlSPARQLUtils.getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("").append("\n");
		buf.append("SELECT distinct ?x_code ?x_label ?p_code ?p_label ?c_code ?c_label").append("\n");

		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + "> {").append("\n");
		buf.append("            {").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x :NHC0 ?x_code .").append("\n");
		if (code != null && outbound) {
			buf.append("            ?x :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
		}
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("").append("\n");
		buf.append("            ?x owl:equivalentClass ?y .").append("\n");
		buf.append("            ?y (rdfs:subClassOf|(owl:intersectionOf/rdf:rest*/rdf:first))* ?r1 .").append("\n");
		buf.append("            ?r1 owl:onProperty ?p .").append("\n");
		buf.append("").append("\n");
		buf.append("            ?p :NHC0 ?p_code .").append("\n");
		buf.append("            ?p rdfs:label ?p_label .").append("\n");
		buf.append("").append("\n");
		buf.append("            ?r1 owl:someValuesFrom ?c .").append("\n");
		buf.append("            ?c :NHC0 ?c_code .").append("\n");

		if (code != null && !outbound) {
			buf.append("            ?c :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
		}

		buf.append("            ?c rdfs:label ?c_label .").append("\n");
		buf.append("            }").append("\n");
		buf.append("            UNION").append("\n");
		buf.append("            {").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x :NHC0 ?x_code .").append("\n");

		if (code != null && outbound) {
			buf.append("            ?x :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
		}

		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("").append("\n");
		buf.append("            ?x owl:equivalentClass ?y .").append("\n");
		buf.append("            ?y (rdfs:subClassOf|(owl:unionOf/rdf:rest*/rdf:first))* ?r1 .").append("\n");
		buf.append("            ?r1 owl:onProperty ?p .").append("\n");
		buf.append("").append("\n");
		buf.append("            ?p :NHC0 ?p_code .").append("\n");
		buf.append("            ?p rdfs:label ?p_label .").append("\n");
		buf.append("").append("\n");
		buf.append("            ?r1 owl:someValuesFrom ?c .").append("\n");
		buf.append("            ?c :NHC0 ?c_code .").append("\n");

		if (code != null && !outbound) {
			buf.append("            ?c :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
		}

		buf.append("            ?c rdfs:label ?c_label .").append("\n");
		buf.append("            }").append("\n");
		buf.append("            UNION            ").append("\n");
		buf.append("            {").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x :NHC0 ?x_code .").append("\n");

		if (code != null && outbound) {
			buf.append("            ?x :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
		}

		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("            ?x rdfs:subClassOf ?r .").append("\n");
		buf.append("").append("\n");
		buf.append("            ?r owl:onProperty ?p .").append("\n");
		buf.append("            ?p :NHC0 ?p_code .").append("\n");
		buf.append("            ?p rdfs:label ?p_label .").append("\n");
		buf.append("").append("\n");
		buf.append("            ?r owl:someValuesFrom ?c .").append("\n");
		buf.append("            ?c :NHC0 ?c_code .").append("\n");

		if (code != null && !outbound) {
			buf.append("            ?c :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
		}

		buf.append("            ?c rdfs:label ?c_label .").append("\n");
		buf.append("            }").append("\n");
		buf.append("            ").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		buf.append(" ").append("\n");
		return buf.toString();
	}

	public Vector getRestrictions(String named_graph, String code, boolean outbound) {
		String query = construct_get_restrictions(named_graph, code, outbound);
		Vector v = owlSPARQLUtils.executeQuery(query);
		if (v == null) return null;
		if (v.size() == 0) return v;
		v = new ParserUtils().getResponseValues(v);
		return new SortUtils().quickSort(v);
	}


	public String construct_get_hierarchically_related_concepts(String named_graph, String code, boolean direction) {
			String prefixes = owlSPARQLUtils.getPrefixes();
			StringBuffer buf = new StringBuffer();
			buf.append(prefixes);
			buf.append("").append("\n");
			buf.append("select ?x_label ?x_code ?c_label ?c_code  ").append("\n");
			buf.append("{").append("\n");
			buf.append("    graph <" + named_graph + "> {").append("\n");
			buf.append("          { ").append("\n");
			buf.append("          ?x a owl:Class .").append("\n");
			buf.append("          ?x :NHC0 ?x_code .").append("\n");

			if (code != null && direction) {
				buf.append("          ?x :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
			}
			buf.append("          ?x rdfs:label ?x_label .  ").append("\n");
			buf.append("          ?x rdfs:subClassOf ?c .").append("\n");


			buf.append("          ?c :NHC0 ?c_code .").append("\n");
			if (code != null && !direction) {
				buf.append("          ?c :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
			}

			buf.append("          ?c rdfs:label ?c_label .  ").append("\n");
			buf.append("          }").append("\n");
			buf.append("          union").append("\n");
			buf.append("          {").append("\n");
			buf.append("          ?x a owl:Class .").append("\n");
			buf.append("          ?x :NHC0 ?x_code .").append("\n");

			if (code != null && direction) {
				buf.append("          ?x :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
			}

			buf.append("          ?x rdfs:label ?x_label .            ").append("\n");
			buf.append("          ?x owl:equivalentClass ?y .").append("\n");
			buf.append("          ?y (rdfs:subClassOf|(owl:intersectionOf/rdf:rest*/rdf:first))* ?c .").append("\n");
			buf.append("          ?c a owl:Class .").append("\n");
			buf.append("          ?c :NHC0 ?c_code .").append("\n");

			if (code != null && !direction) {
				buf.append("          ?c :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
			}

			buf.append("          ?c rdfs:label ?c_label .  ").append("\n");
			buf.append("          }").append("\n");
			buf.append("    }").append("\n");
			buf.append("}").append("\n");
			buf.append("").append("\n");
			return buf.toString();
	}


	public Vector getHierarchicallyRrelatedConcepts(String named_graph, String code, boolean direction) {
			String query = construct_get_hierarchically_related_concepts(named_graph, code, direction);
			Vector v = owlSPARQLUtils.executeQuery(query);
			if (v == null) return null;
			if (v.size() == 0) return v;
			v = new ParserUtils().getResponseValues(v);
			return new SortUtils().quickSort(v);
	}


	public static void main(String[] args) {
		long ms = System.currentTimeMillis();
		String serviceUrl = args[0];
		String namedGraph = args[1];
		String username = args[2];
		String password = args[3];
		String code = args[4];
		RelationshipData test = new RelationshipData(serviceUrl, namedGraph, username, password);
		test.generate(namedGraph, code);
	}
}