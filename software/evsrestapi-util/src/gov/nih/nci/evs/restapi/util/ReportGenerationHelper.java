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
 *     Initial implementation kim.ong@nih.gov
 *
 */


public class ReportGenerationHelper {
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
    AxiomUtils axiomUtils = null; //(String serviceUrl, String username, String password)

    public ReportGenerationHelper(String serviceUrl, String named_graph, String username, String password) {
		this.serviceUrl = serviceUrl;
    	this.named_graph = named_graph;
    	this.username = username;
    	this.password = password;
        this.owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, username, password);
        this.owlSPARQLUtils.set_named_graph(named_graph);
        this.axiomUtils = new AxiomUtils(serviceUrl, username, password);
    }

    public OWLSPARQLUtils getOWLSPARQLUtils() {
		return this.owlSPARQLUtils;
	}

	public String construct_get_valueset(String named_graph, String code) {
		String prefixes = owlSPARQLUtils.getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("select distinct ?x_label ?x_code ?p1_label ?y_label ?y_code ?p2_value").append("\n");
		buf.append("from <http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl>").append("\n");
		buf.append("where  { ").append("\n");
		buf.append("            	?x a owl:Class .").append("\n");
		buf.append("            	?x :NHC0 ?x_code .").append("\n");
		buf.append("            	?x rdfs:label ?x_label .").append("\n");
		buf.append("").append("\n");
		buf.append("            	?y a owl:Class .").append("\n");
		buf.append("            	?y :NHC0 ?y_code .").append("\n");

		if (code != null) {
			buf.append("                ?y :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
		}
		buf.append("").append("\n");
		buf.append("            	?y rdfs:label ?y_label .").append("\n");
		buf.append("            	").append("\n");
		buf.append("                ?p1 rdfs:label ?p1_label .").append("\n");
		buf.append("                ?p1 rdfs:label \"Concept_In_Subset\"^^xsd:string .  ").append("\n");
		buf.append("                ").append("\n");
		buf.append("                ?x ?p1 ?y .").append("\n");
		buf.append("").append("\n");
		buf.append("                ?y ?p2 ?p2_value .").append("\n");
		buf.append("                ?p2 rdfs:label ?p2_label .").append("\n");
		buf.append("                ?p2 rdfs:label \"Publish_Value_Set\"^^xsd:string .").append("\n");
		buf.append("").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}
	public Vector getValueset(String named_graph, String code) {
		String query = construct_get_valueset(named_graph, code);
		Vector v = owlSPARQLUtils.executeQuery(query);
		if (v == null) return null;
		if (v.size() == 0) return v;
		v = new ParserUtils().getResponseValues(v);
		return new SortUtils().quickSort(v);
	}

	public Vector getAxioms(String named_graph, String code, String propertyName) {
		return owlSPARQLUtils.getAxioms(named_graph, code, propertyName);
	}

	public Vector getAxiomData(String named_graph, String propertyCode) {
		return owlSPARQLUtils.getAxiomData(named_graph, propertyCode);
	}

	public List getSynonyms(String named_graph) {
		Vector axiom_data = owlSPARQLUtils.getAxioms(named_graph, null, "FULL_SYN");
		System.out.println("axiom_data: " + axiom_data.size());
		return axiomUtils.getSynonyms(axiom_data);
	}

	public List getSynonyms(Vector axiom_data) {
		return axiomUtils.getSynonyms(axiom_data);
	}

	public List getDefinitions(Vector axiom_data) {
		return axiomUtils.getDefinitions(axiom_data);
	}

	public Vector getPropertiesByCode(String named_graph, String code, String propertyName) {
		return owlSPARQLUtils.getPropertiesByCode(named_graph, code, propertyName);
	}

	public Vector getAssociationsByCode(String named_graph, String code) {
		Vector v = owlSPARQLUtils.getAssociationsByCode(named_graph, code);
		v = new ParserUtils().getResponseValues(v);
		return v;
	}

	public Vector getInverseAssociationsByCode(String named_graph, String code) {
		Vector v = owlSPARQLUtils.getInverseAssociationsByCode(named_graph, code);
		v = new ParserUtils().getResponseValues(v);
		return v;
	}

	public Vector getRolesByCode(String named_graph, String code) {
		Vector v = owlSPARQLUtils.getRolesByCode(named_graph, code);
		return v;
	}

	public Vector getInverseRolesByCode(String named_graph, String code) {
		Vector v = owlSPARQLUtils.getInverseRolesByCode(named_graph, code);
		return v;
	}

	public Vector getSuperclassesByCode(String named_graph, String code) {
		Vector v = owlSPARQLUtils.getSuperclassesByCode(named_graph, code);
		return v;
	}

	public Vector getSubclassesByCode(String named_graph, String code) {
		Vector v = owlSPARQLUtils.getSubclassesByCode(named_graph, code);
		return v;
	}

    public static void main(String[] args) {
		long ms = System.currentTimeMillis();
		String serviceUrl = args[0];
		String named_graph = args[1];
		String username = args[2];
		String password = args[3];
		Vector w = new Vector();
        ReportGenerationHelper test = new ReportGenerationHelper(serviceUrl, named_graph, username, password);
		//Vector v = test.getValueset(named_graph);
		//Utils.saveToFile("getValueset.txt", v);

		//List list = test.getSynonyms(named_graph);
		String code = "C12345";
		String propertyName = "FULL_SYN";
		Vector axiom_data = test.getAxioms(named_graph, code, propertyName);

		List list = test.getSynonyms(axiom_data);
		for (int i=0; i<list.size(); i++) {
			Synonym syn = (Synonym) list.get(i);
			System.out.println(syn.toJson());
		}

		propertyName = "DEFINITION";
		axiom_data = test.getAxioms(named_graph, code, propertyName);

		list = test.getDefinitions(axiom_data);
		for (int i=0; i<list.size(); i++) {
			Definition def = (Definition) list.get(i);
			System.out.println(def.toJson());
		}

		propertyName = "Semantic_Type";
		w = test.getPropertiesByCode(named_graph, code, propertyName);
		Utils.dumpVector(code, w);

		propertyName = "Preferred_Name";
		w = test.getPropertiesByCode(named_graph, code, propertyName);
		Utils.dumpVector(code, w);

		w = test.getAssociationsByCode(named_graph, code);
		Utils.dumpVector(code, w);

		w = test.getInverseAssociationsByCode(named_graph, code);
		Utils.dumpVector(code, w);

		w = test.getRolesByCode(named_graph, code);
		Utils.dumpVector(code, w);

		w = test.getInverseRolesByCode(named_graph, code);
		Utils.dumpVector(code, w);

		w = test.getSuperclassesByCode(named_graph, code);
		Utils.dumpVector(code, w);

		w = test.getSubclassesByCode(named_graph, code);
		Utils.dumpVector(code, w);
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
    }
}
