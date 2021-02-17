package gov.nih.nci.evs.restapi.appl;
import gov.nih.nci.evs.restapi.util.*;

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
import org.apache.commons.codec.binary.Base64;
import java.nio.charset.Charset;

import java.time.Duration;
/**
 * <!-- LICENSE_TEXT_START -->
 * Copyright 2020 MSC. This software was developed in conjunction
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
 *      "This product includes software developed by MSC and the National
 *      Cancer Institute."   If no such end-user documentation is to be
 *      included, this acknowledgment shall appear in the software itself,
 *      wherever such third-party acknowledgments normally appear.
 *   3. The names "The National Cancer Institute", "NCI" and "MSC" must
 *      not be used to endorse or promote products derived from this software.
 *   4. This license does not authorize the incorporation of this software
 *      into any third party proprietary programs. This license does not
 *      authorize the recipient to use any trademarks owned by either NCI
 *      or MSC
 *   5. THIS SOFTWARE IS PROVIDED "AS IS," AND ANY EXPRESSED OR IMPLIED
 *      WARRANTIES, (INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *      OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE) ARE
 *      DISCLAIMED. IN NO EVENT SHALL THE NATIONAL CANCER INSTITUTE,
 *      MSC, OR THEIR AFFILIATES BE LIABLE FOR ANY DIRECT, INDIRECT,
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


public class EVSStatistics {
	String serviceUrl = null;
	String named_graph = null;
	String username = null;
	String password = null;
	MetadataUtils metadataUtils = null;
	OWLSPARQLUtils owlSPARQLUtils = null;

	HTTPUtils httpUtils = null;
	String prefixes = null;
	static String PUBLISH_VALUE_SET = "Publish_Value_Set";

	static String[] ANNOTATED_TARGETS = null;
	static String[] ANNOTATED_TARGET_CODES = null;
	static List ANNOTATED_TARGET_LIST = null;
	static HashMap ANNOTATED_TARGET_HASHMAP = null;

	Vector properties = null;
	HashMap propertyCode2NameHashMap = new HashMap();
	HashMap propertyName2CodeHashMap = new HashMap();

	HashMap roleCode2NameHashMap = new HashMap();
	HashMap roleName2CodeHashMap = new HashMap();

	Vector table_data = null;
	String version = null;

	String named_graph_id = ":NHC0";
	Vector roots = null;

	HierarchyHelper hh = null;

	static String PARENT_CHILD_FILE = "parent_child.txt";
	static String RESTRICTION_FILE = "roles.txt";

    static {
		ANNOTATED_TARGETS = new String[] {"term-name", "go-term", "TARGET TERM"};
		ANNOTATED_TARGET_CODES = new String[] {"P382", "P388", "P392"};
		ANNOTATED_TARGET_LIST = Arrays.asList(ANNOTATED_TARGET_CODES);
		ANNOTATED_TARGET_HASHMAP = new HashMap();
		ANNOTATED_TARGET_HASHMAP.put("P382", "P90");
		ANNOTATED_TARGET_HASHMAP.put("P388", "P211");
		ANNOTATED_TARGET_HASHMAP.put("P392", "P375");
	}


	public EVSStatistics(String serviceUrl, String named_graph, String username, String password) {
		this.serviceUrl = serviceUrl;
		this.named_graph = named_graph;
		this.username = username;
		this.password = password;

		this.metadataUtils = new MetadataUtils(serviceUrl, username, password);

		this.owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, username, password);
		this.owlSPARQLUtils.set_named_graph(named_graph);
		this.version  = metadataUtils.getVocabularyVersion(named_graph);
        System.out.println("NCI Thesaurus version: " + version);
		httpUtils = new HTTPUtils();
		properties = getSupportedProperties(named_graph);
		propertyCode2NameHashMap = new HashMap();
		propertyName2CodeHashMap = new HashMap();
        for (int i=0; i<properties.size(); i++) {
			String line = (String) properties.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String property_name = (String) u.elementAt(0);
			String property_code = (String) u.elementAt(1);
			propertyCode2NameHashMap.put(property_code, property_name);
			propertyName2CodeHashMap.put(property_name, property_code);
		}
		this.table_data = new Vector();
		this.roots = getRoots(named_graph, true);

		Vector supported_roles = getObjectProperties(named_graph);
		roleCode2NameHashMap = new HashMap();
		roleName2CodeHashMap = new HashMap();
        for (int i=0; i<supported_roles.size(); i++) {
			String line = (String) supported_roles.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String role_name = (String) u.elementAt(0);
			String role_code = (String) u.elementAt(1);
			roleCode2NameHashMap.put(role_code, role_name);
			roleName2CodeHashMap.put(role_name, role_code);
		}
	}

	public void addTitle(String title) {
		table_data.add("<title>" + title);
	}

	public void addTable(String tableName, Vector th_vec, Vector data) {
		table_data.add("<table>" + tableName);
		for (int i=0; i<th_vec.size(); i++) {
			String th = (String) th_vec.elementAt(i);
			table_data.add("<th>" + th);
		}
		table_data.add("<data>");
		table_data.addAll(data);
		table_data.add("</data>");
        table_data.add("</table>");
	}

    public void addFooter() {
		table_data.add("<footer>(Source; NCI Thesaurus, version " + this.version + ")");
	}

    public boolean is_ANNOTATED_TARGET_CODES(String propertyCode) {
		return ANNOTATED_TARGET_LIST.contains(propertyCode);
	}

	public static void dumpVector(String label, Vector v) {
		System.out.println(label);
		for (int i=0; i<v.size(); i++) {
			System.out.println((String) v.elementAt(i));
		}
	}

    public String getPrefixes() {
		if (prefixes != null) return prefixes;
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX :<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>").append("\n");
		buf.append("PREFIX base:<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl>").append("\n");
		buf.append("PREFIX Thesaurus:<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>").append("\n");
		buf.append("PREFIX xml:<http://www.w3.org/XML/1998/namespace>").append("\n");
		buf.append("PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>").append("\n");
		buf.append("PREFIX owl:<http://www.w3.org/2002/07/owl#>").append("\n");
		buf.append("PREFIX owl2xml:<http://www.w3.org/2006/12/owl2-xml#>").append("\n");
		buf.append("PREFIX protege:<http://protege.stanford.edu/plugins/owl/protege#>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>").append("\n");
		buf.append("PREFIX ncicp:<http://ncicb.nci.nih.gov/xml/owl/EVS/ComplexProperties.xsd#>").append("\n");
		buf.append("PREFIX dc:<http://purl.org/dc/elements/1.1/>").append("\n");
		return buf.toString();
	}

	public String loadQuery(String queryfile) {
		return httpUtils.loadQuery(queryfile, false);
	}

    public Vector executeQuery(String query) {
		//boolean parsevalues = true;
		//Vector w = httpUtils.execute(serviceUrl, username, password, query, parsevalues);

		Vector w = HTTPUtils.runQuery(serviceUrl, username, password, query);

		return w;
	}

    public void run(String queryfile) {
		String query = loadQuery(queryfile);
		boolean parsevalues = true;
		Vector w = httpUtils.execute(serviceUrl, username, password, query, parsevalues);
		if (w != null && w.size() > 0) {
			int n = queryfile.lastIndexOf("_query");
			if(n ==  -1) {
				System.out.println("ERROR: Wrong query file naming.");
				System.exit(1);
			}
			String t = queryfile.substring(0, n);
			String methodSignature = "construct_get_" + t + "(String named_graph)";
			SPARQLQueryGenerator.generateCode(queryfile, methodSignature);
			Utils.dumpVector("w", w);
		} else {
			System.out.println("ERROR: Query failed.");
            int n = queryfile.lastIndexOf("_query");
			String t = queryfile.substring(0, n);
			String methodSignature = "construct_get_" + t + "(String named_graph)";
			SPARQLQueryGenerator.generateCode(queryfile, methodSignature);
		}
	}

    public String construct_root_query(String named_graph) {
        String prefixes = getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        buf.append("select ?s_label ?s_code").append("\n");
        buf.append("from <" + named_graph + ">").append("\n");
        buf.append("where  { ").append("\n");
        buf.append("?s a owl:Class .").append("\n");
        buf.append("?s rdfs:label ?s_label .").append("\n");
        buf.append("?s :NHC0 ?s_code . ").append("\n");
        buf.append("filter not exists { ?s rdfs:subClassOf|owl:equivalentClass ?o } ").append("\n");
        buf.append("}").append("\n");
        return buf.toString();
    }

	public Vector getRoots(String named_graph, boolean code_only) {
		Vector w = new Vector();
        String query = construct_root_query(named_graph);
        Vector v = executeQuery(query);
        if (!code_only) return v;
        for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			w.add((String) u.elementAt(1));
		}
		return w;
	}

	public String construct_get_object_properties(String named_graph) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("").append("\n");
		buf.append("SELECT distinct ?p_label ?p_code ").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + "> {").append("\n");
		buf.append("            ?p a owl:ObjectProperty.").append("\n");
		buf.append("            ?p :NHC0 ?p_code .").append("\n");
		buf.append("            ?p rdfs:label ?p_label ").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getObjectProperties(String named_graph) {
		return executeQuery(construct_get_object_properties(named_graph));
	}

	public String construct_get_object_properties_domain_range(String named_graph) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT distinct ?x_pt ?x_code ?x_domain_label ?x_range_label ").append("\n");
		buf.append("{ ").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("	    {").append("\n");
		buf.append("		?x a owl:ObjectProperty .").append("\n");
		buf.append("		?x :NHC0 ?x_code .").append("\n");
		buf.append("		?x rdfs:domain ?x_domain .").append("\n");
		buf.append("		?x_domain rdfs:label ?x_domain_label .").append("\n");
		buf.append("		?x rdfs:range ?x_range .").append("\n");
		buf.append("		?x_range rdfs:label ?x_range_label .").append("\n");
		buf.append("        ?x :P108 ?x_pt .").append("\n");
		buf.append("	    }").append("\n");
		buf.append("	    ").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}


	public String construct_get_object_valued_annotation_properties(String named_graph) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT distinct ?x_label ?x_code ").append("\n");
		buf.append("{ ").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("        ?x a owl:AnnotationProperty .").append("\n");
		buf.append("        ?x :NHC0 ?x_code .").append("\n");
		buf.append("        ?x rdfs:label ?x_label .").append("\n");
		buf.append("        ?x rdfs:range ?x_range").append("\n");
		buf.append("    }").append("\n");
		buf.append("    FILTER (str(?x_range)=\"http://www.w3.org/2001/XMLSchema#anyURI\")").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getObjectValuedProperty(String named_graph) {
        String query = construct_get_object_valued_annotation_properties(named_graph);
        return executeQuery(query);
	}

	public String construct_get_supported_properties(String named_graph, String code, String propertyName, String propertyValue, boolean concept_only) {
        String prefixes = getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        if (concept_only) {
			buf.append("select distinct ?x_label ?x_code").append("\n");
		} else {
        	buf.append("select distinct ?x_label ?x_code ?p_label ?p_value").append("\n");
		}
        buf.append("from <" + named_graph + ">").append("\n");
        buf.append("where  { ").append("\n");
        buf.append("").append("\n");
        buf.append("                ?x a owl:Class .").append("\n");
        buf.append("                ?x :NHC0 ?x_code .").append("\n");
        if (code != null) {
        	buf.append("                ?x :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
		}
        buf.append("                ?x rdfs:label ?x_label .").append("\n");
        buf.append("").append("\n");
        buf.append("                ?x ?p ?p_value .").append("\n");
        if (propertyValue != null) {
        	buf.append("                ?x ?p \"" + propertyValue + "\"^^xsd:string .").append("\n");
	    }

        buf.append("                ?p rdfs:label ?p_label .").append("\n");
        buf.append("                ?p :NHC0 ?p_code .").append("\n");
        buf.append("                ?p rdfs:label \"" + propertyName + "\"^^xsd:string .").append("\n");
        buf.append("").append("\n");
        buf.append("}").append("\n");
        return buf.toString();
	}

	public Vector getSupportedProperties(String named_graph, String code, String propertyName, String propertyValue, boolean concept_only) {
        String query = construct_get_supported_properties(named_graph, code, propertyName, propertyValue, concept_only);
        return executeQuery(query);
	}

	public String construct_get_property_qualifier(String named_graph, String code, String propertyCode,
	                                               String qualifierCode, String qualifierValue, boolean concept_only) {
        String prefixes = getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);

        if (concept_only) {
			buf.append("select distinct ?x_label ?x_code").append("\n");
	    } else {
        	buf.append("select ?x_label ?x_code ?p_label ?a_target ?q1_label ?q1_value").append("\n");
		}

        buf.append("from <" + named_graph + ">").append("\n");
        buf.append("where { ").append("\n");
        buf.append("                ?x a owl:Class .").append("\n");
        buf.append("                ?x :NHC0 ?x_code .").append("\n");
        if (code != null) {
        	buf.append("                ?x :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
		}
        buf.append("                ?x rdfs:label ?x_label .").append("\n");
        buf.append("").append("\n");
        buf.append("                ?a a owl:Axiom .").append("\n");
        buf.append("                ?a owl:annotatedSource ?x .").append("\n");
        buf.append("                ?a owl:annotatedProperty ?p .").append("\n");
        buf.append("                ?a owl:annotatedTarget ?a_target .").append("\n");
        buf.append("                ?p :NHC0 ?p_code .").append("\n");
        buf.append("                ?p :NHC0 \"" + propertyCode + "\"^^xsd:string .").append("\n");
        buf.append("                ?p rdfs:label ?p_label .").append("\n");
        buf.append("").append("\n");
        buf.append("                ?a ?q1 ?q1_value .").append("\n");
        buf.append("                ?a ?q1 \"" + qualifierValue + "\"^^xsd:string .").append("\n");
        buf.append("                ?q1 :NHC0 ?q1_code .").append("\n");
        buf.append("                ?q1 :NHC0 \"" + qualifierCode + "\"^^xsd:string .").append("\n");
        buf.append("                ?q1 rdfs:label ?q1_label .").append("\n");
        buf.append("}").append("\n");
        return buf.toString();
	}


	public Vector getPropertyQualifier(String named_graph, String code, String propertyCode,
	    String qualifierCode, String qualifierValue, boolean concept_only) {
        String query = construct_get_property_qualifier(named_graph, code, propertyCode, qualifierCode, qualifierValue, concept_only);
        return executeQuery(query);
	}

	public String construct_get_associations(String named_graph) {
		return construct_get_associations(named_graph, null);
	}

	public String construct_get_associations(String named_graph, String code) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT distinct ?x_label ?x_code ?y_label ?y_code ?z_label ?z_code").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("	    ?x a owl:Class .").append("\n");
		buf.append("	    ?x :NHC0 ?x_code .").append("\n");
        if (code != null) {
        	buf.append("                ?x :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
		}
		buf.append("	    ?x rdfs:label ?x_label .").append("\n");
		buf.append("	    ?y a owl:AnnotationProperty .").append("\n");
		buf.append("	    ?x ?y ?z .").append("\n");
		buf.append("	    ?z a owl:Class .").append("\n");
		buf.append("	    ?z rdfs:label ?z_label .").append("\n");
		buf.append("	    ?z :NHC0 ?z_code .").append("\n");
		buf.append("	    ?y rdfs:label ?y_label .").append("\n");
		buf.append("	    ?y :NHC0 ?y_code .").append("\n");
		buf.append("	    ?y rdfs:range ?y_range").append("\n");
		buf.append("    }").append("\n");
		buf.append("    FILTER (str(?y_range)=\"http://www.w3.org/2001/XMLSchema#anyURI\")").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getAssociations(String named_graph) {
        String query = construct_get_associations(named_graph);
        return executeQuery(query);
	}

	public Vector getAssociations(String named_graph, String code) {
        String query = construct_get_associations(named_graph, code);
        return executeQuery(query);
	}


	public String construct_get_role_targets(String named_graph, String code, String roleName) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("").append("\n");
		buf.append("SELECT distinct ?x_label ?x_code ?p_label ?p_code ?c_label ?c_code").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + "> {").append("\n");
		buf.append("            {").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x :NHC0 ?x_code .").append("\n");
		if (code != null) {
			buf.append("                ?x :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
		}
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("").append("\n");
		buf.append("            ?x owl:equivalentClass ?y .").append("\n");
		buf.append("            ?y (rdfs:subClassOf|(owl:intersectionOf/rdf:rest*/rdf:first))* ?r1 .").append("\n");
		buf.append("            ?r1 owl:onProperty ?p .").append("\n");
		buf.append("").append("\n");
		buf.append("            ?p :NHC0 ?p_code .").append("\n");
		buf.append("            ?p rdfs:label ?p_label .").append("\n");
		if (roleName != null) {
			buf.append("            ?p rdfs:label \"" + roleName + "\"^^xsd:string .").append("\n");
		}
		buf.append("").append("\n");
		buf.append("            ?r1 owl:someValuesFrom ?c .").append("\n");
		buf.append("            ?c :NHC0 ?c_code .").append("\n");

		buf.append("            ?c rdfs:label ?c_label .").append("\n");
		buf.append("            }").append("\n");
		buf.append("            UNION").append("\n");
		buf.append("            {").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x :NHC0 ?x_code .").append("\n");

		if (code != null) {
			buf.append("                ?x :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
		}

		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("").append("\n");
		buf.append("            ?x owl:equivalentClass ?y .").append("\n");
		buf.append("            ?y (rdfs:subClassOf|(owl:unionOf/rdf:rest*/rdf:first))* ?r1 .").append("\n");
		buf.append("            ?r1 owl:onProperty ?p .").append("\n");
		buf.append("").append("\n");
		buf.append("            ?p :NHC0 ?p_code .").append("\n");
		buf.append("            ?p rdfs:label ?p_label .").append("\n");

		if (roleName != null) {
			buf.append("            ?p rdfs:label \"" + roleName + "\"^^xsd:string .").append("\n");
		}

		buf.append("").append("\n");
		buf.append("").append("\n");
		buf.append("            ?r1 owl:someValuesFrom ?c .").append("\n");
		buf.append("            ?c :NHC0 ?c_code .").append("\n");
		buf.append("            ?c rdfs:label ?c_label .").append("\n");
		buf.append("").append("\n");
		buf.append("            }").append("\n");
		buf.append("            UNION").append("\n");
		buf.append("            {").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x :NHC0 ?x_code .").append("\n");

		if (code != null) {
			buf.append("                ?x :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
		}
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("            ?x rdfs:subClassOf ?r .").append("\n");
		buf.append("").append("\n");
		buf.append("            ?r owl:onProperty ?p .").append("\n");
		buf.append("            ?p :NHC0 ?p_code .").append("\n");
		buf.append("            ?p rdfs:label ?p_label .").append("\n");

		if (roleName != null) {
			buf.append("            ?p rdfs:label \"" + roleName + "\"^^xsd:string .").append("\n");
		}
		buf.append("").append("\n");
		buf.append("            ?r owl:someValuesFrom ?c .").append("\n");
		buf.append("            ?c :NHC0 ?c_code .").append("\n");
		buf.append("            ?c rdfs:label ?c_label .").append("\n");
		buf.append("            }").append("\n");
		buf.append("    }").append("\n");
		buf.append("").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getRoleTargets(String named_graph) {
		return getRoleTargets(named_graph, true);
	}

	public Vector getRoleTargets(String named_graph, boolean savefile) {
        Vector v = getRoleTargets(named_graph, null, null);
        if (savefile) {
        	Utils.saveToFile("role_data.txt", v);
		}
        return v;
	}

	public Vector getRoleTargets(String named_graph, String code, String roleName) {
        String query = construct_get_role_targets(named_graph, code, roleName);
        return executeQuery(query);
	}


	public String construct_get_hierarchical_relationships(String named_graph) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT ?z_label ?z_code ?x_label ?x_code").append("\n");
		buf.append("{").append("\n");
		buf.append("  graph <" + named_graph + ">").append("\n");
		buf.append("  {").append("\n");
		buf.append("	  {").append("\n");
		buf.append("		  {").append("\n");
		buf.append("		    ?x a owl:Class .").append("\n");
		buf.append("		    ?x rdfs:label ?x_label .").append("\n");
		buf.append("		    ?x " + named_graph_id + " ?x_code .").append("\n");
		buf.append("		    ?x rdfs:subClassOf ?z .").append("\n");
		buf.append("		    ?z a owl:Class .").append("\n");
		buf.append("		    ?z rdfs:label ?z_label .").append("\n");
		buf.append("		    ?z " + named_graph_id + " ?z_code").append("\n");
		buf.append("		  }").append("\n");
		buf.append("		  FILTER (?x != ?z)").append("\n");
		buf.append("	  }").append("\n");
		buf.append("  	  UNION").append("\n");
		buf.append("	  {").append("\n");
		buf.append("		  {").append("\n");
		buf.append("		    ?x a owl:Class .").append("\n");
		buf.append("		    ?x rdfs:label ?x_label .").append("\n");
		buf.append("		    ?x " + named_graph_id + " ?x_code .").append("\n");
		buf.append("		    ?x owl:equivalentClass ?y .").append("\n");
		buf.append("		    ?y owl:intersectionOf ?list .").append("\n");
		buf.append("		    ?list rdf:rest*/rdf:first ?z .").append("\n");
		buf.append("		    ?z a owl:Class .").append("\n");
		buf.append("		    ?z rdfs:label ?z_label .").append("\n");
		buf.append("		    ?z " + named_graph_id + " ?z_code").append("\n");
		buf.append("		  }").append("\n");
		buf.append("		  FILTER (?x != ?z)").append("\n");
		buf.append("	  }").append("\n");
		buf.append("  }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getHierarchicalRelationships(String named_graph) {
		return executeQuery(construct_get_hierarchical_relationships(named_graph));
	}


	public Vector get_roots(String named_graph) {
		return executeQuery(construct_root_query(named_graph));
	}

	public String construct_get_concepts_with_annotation_property(String named_graph, String propertyName) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT ?x_label ?x_code ?y_label ?z").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("		    ?x :NHC0 ?x_code .").append("\n");
		buf.append("            ?y a owl:AnnotationProperty .").append("\n");
		buf.append("            ?x ?y ?z .").append("\n");
		buf.append("            ?y rdfs:label ?y_label .").append("\n");
		buf.append("            ?y rdfs:label " + "\"" + propertyName + "\"^^xsd:string ").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getConceptsWithAnnotationProperty(String named_graph, String propertyName) {
		String query = construct_get_concepts_with_annotation_property(named_graph, propertyName);
		Vector v = executeQuery(query);
		return v;
	}

	public String construct_get_concepts_with_annotation_property_matching(String named_graph,
	    String propertyName, String propertyValue) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT ?x_label ?x_code ?y_label ?z").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("            ?x :NHC0 ?x_code .").append("\n");
		buf.append("            ?y a owl:AnnotationProperty .").append("\n");
		buf.append("            ?x ?y ?z .").append("\n");
		buf.append("            ?y rdfs:label ?y_label .").append("\n");
		buf.append("            ?y rdfs:label " + "\"" + propertyName + "\"^^xsd:string .").append("\n");
		if (propertyValue != null) {
		    buf.append("            ?x ?y " + "\"" + propertyValue + "\"^^xsd:string .").append("\n");
		}

		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getConceptsWithAnnotationPropertyMatching(String named_graph, String propertyName, String propertyValue) {
		String query = construct_get_concepts_with_annotation_property_matching(named_graph, propertyName, propertyValue);
		Vector v = executeQuery(query);
		return v;
	}


	public String construct_get_supported_properties(String named_graph) {
        String prefixes = getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        buf.append("select distinct ?p_label ?p_code ").append("\n");
        buf.append("from <http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl>").append("\n");
        buf.append("where  { ").append("\n");
        buf.append("                ?p :NHC0 ?p_code .").append("\n");
        buf.append("                ?p rdfs:label ?p_label .").append("\n");
        buf.append("                ?p a owl:AnnotationProperty .").append("\n");
        buf.append("}").append("\n");
        return buf.toString();
	}


	public Vector getSupportedProperties(String named_graph) {
		String query = construct_get_supported_properties(named_graph);
		Vector v = executeQuery(query);
		return v;
	}

	public String construct_get_concepts_with_property(String named_graph, String property_code) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("select distinct ?x_label ?x_code ?p_label ?p_value").append("\n");
		buf.append("from <" + named_graph + ">").append("\n");
		buf.append("where  { ").append("\n");
		buf.append("                ?x a owl:Class .").append("\n");
		buf.append("                ?x :NHC0 ?x_code .").append("\n");
		buf.append("                ?x rdfs:label ?x_label .").append("\n");
		buf.append("").append("\n");
		buf.append("                ?p a owl:AnnotationProperty .").append("\n");
		buf.append("                ?p :NHC0 ?p_code .").append("\n");
		buf.append("                ?p rdfs:label ?p_label .").append("\n");
		buf.append("                ?p :NHC0 \"" + property_code + "\"^^xsd:string .").append("\n");
		buf.append("").append("\n");
		buf.append("                ?x ?p ?p_value .").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getConceptsWithProperty(String named_graph, String property_code) {
		String query = construct_get_concepts_with_property(named_graph, property_code);
		Vector v = executeQuery(query);
		v = removeDuplicates(v);
		v = new SortUtils().quickSort(v);
		return v;
	}

	public String construct_get_supported_qualifiers(String named_graph) {
        String prefixes = getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        buf.append("select distinct ?p_label ?p_code ?q1_label ?q1_code").append("\n");
        buf.append("from <http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl>").append("\n");
        buf.append("where  { ").append("\n");
        buf.append("                ?x a owl:Class .").append("\n");
        buf.append("                ?x :NHC0 ?x_code .").append("\n");
        buf.append("                ?x rdfs:label ?x_label .").append("\n");
        buf.append("").append("\n");
        buf.append("                ?a a owl:Axiom .").append("\n");
        buf.append("                ?a owl:annotatedSource ?x .").append("\n");
        buf.append("                ?a owl:annotatedProperty ?p .").append("\n");
        buf.append("                ?a owl:annotatedTarget ?a_target .").append("\n");
        buf.append("                ").append("\n");
        buf.append("                ?p :NHC0 ?p_code .").append("\n");
        buf.append("                ?p rdfs:label ?p_label .").append("\n");
        buf.append("").append("\n");
        buf.append("                ?a ?q1 ?q1_value .").append("\n");
        buf.append("                ?q1 :NHC0 ?q1_code .").append("\n");
        buf.append("                ?q1 rdfs:label ?q1_label .").append("\n");
        buf.append("}").append("\n");
        return buf.toString();
	}

    public HashMap createQualifierCode2PropertyCodeHashMap(String named_graph) {
		Vector v = getSupportedQualifiers(named_graph);
		HashMap hmap = new HashMap();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			//        (1) FULL_SYN|P90|Term Type|P383
			String q_code = (String) u.elementAt(3);
			String p_code = (String) u.elementAt(1);
			Vector w = new Vector();
			if (hmap.containsKey(q_code)) {
				w = (Vector) hmap.get(q_code);
			}
			w.add(p_code);
			hmap.put(q_code, w);
		}
		return hmap;
	}


	public Vector getSupportedQualifiers(String named_graph) {
		String query = construct_get_supported_qualifiers(named_graph);
		Vector v = executeQuery(query);
		v = new SortUtils().quickSort(v);
		return v;
	}

	public String construct_get_annotated_target_count(String named_graph, String propertyCode, String qualifierCode) {
        String prefixes = getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        buf.append("SELECT ?x_label ?x_code ?q1_label ?q1_value ").append("\n");
        buf.append("from <http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl>").append("\n");
        buf.append("where  { ").append("\n");
        buf.append("                ?x a owl:Class .").append("\n");
        buf.append("                ?x :NHC0 ?x_code .").append("\n");
        buf.append("                ?x rdfs:label ?x_label .").append("\n");
        buf.append("").append("\n");
        buf.append("                ?a a owl:Axiom .").append("\n");
        buf.append("                ?a owl:annotatedSource ?x .").append("\n");
        buf.append("                ?a owl:annotatedProperty ?p .").append("\n");
        buf.append("                ?a owl:annotatedTarget ?a_target .").append("\n");
        buf.append("                ").append("\n");
        buf.append("                ?p :NHC0 ?p_code .").append("\n");
        buf.append("                ?p :NHC0 \"" + propertyCode + "\"^^xsd:string .").append("\n");
        buf.append("                ?p rdfs:label ?p_label .").append("\n");
        buf.append("").append("\n");
        buf.append("                ?a ?q1 ?q1_value .").append("\n");
        buf.append("                ?q1 :NHC0 ?q1_code .").append("\n");
        buf.append("                ?q1 :NHC0 \"" + qualifierCode + "\"^^xsd:string .").append("\n");
        buf.append("                ?q1 rdfs:label ?q1_label .").append("\n");
        buf.append("}").append("\n");
        return buf.toString();
	}


	public int getAnnotatedTargetCount(String named_graph, String propertyCode, String qualifierCode) {
		String query = construct_get_annotated_target_count(named_graph, propertyCode, qualifierCode);
		Vector v = executeQuery(query);
		if (v == null || v.size() == 0) return 0;
		return v.size();
	}


	public String construct_get_annotated_target_count(String named_graph, String propertyCode) {
        String prefixes = getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        buf.append("SELECT ?a_target").append("\n");
        buf.append("from <" + named_graph + ">").append("\n");
        buf.append("where  { ").append("\n");
        buf.append("                ?a a owl:Axiom .").append("\n");
        buf.append("                ?a owl:annotatedSource ?x .").append("\n");
        buf.append("                ?a owl:annotatedProperty ?p .").append("\n");
        buf.append("                ?a owl:annotatedTarget ?a_target .").append("\n");
        buf.append("                ").append("\n");
        buf.append("                ?p :NHC0 ?p_code .").append("\n");
        buf.append("                ?p :NHC0 \"" + propertyCode + "\"^^xsd:string .").append("\n");
        buf.append("                ?p rdfs:label ?p_label .").append("\n");
        buf.append("}").append("\n");
        return buf.toString();
	}

	public int getAnnotatedTargetCount(String named_graph, String propertyCode) {
		String query = construct_get_annotated_target_count(named_graph, propertyCode);
		Vector v = executeQuery(query);
		if (v == null || v.size() == 0) return 0;
		return v.size();
	}


    public String construct_get_distinct_property_qualifier_values(String named_graph,
        String code,
        String propertyCode,
        String qualifierCode) {
        String prefixes = getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        buf.append("select distinct ?q1_value").append("\n");
        buf.append("from <" + named_graph + ">").append("\n");
        buf.append("where  { ").append("\n");
        buf.append("                ?x a owl:Class .").append("\n");
        buf.append("                ?x :NHC0 ?x_code .").append("\n");
        if (code != null) {
        	buf.append("                ?x :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
	    }
        buf.append("                ?x rdfs:label ?x_label .").append("\n");
        buf.append("").append("\n");
        buf.append("                ?a a owl:Axiom .").append("\n");
        buf.append("                ?a owl:annotatedSource ?x .").append("\n");
        buf.append("                ?a owl:annotatedProperty ?p .").append("\n");
        buf.append("                ?a owl:annotatedTarget ?a_target .").append("\n");
        buf.append("                ?p :NHC0 ?p_code .").append("\n");
        buf.append("                ?p :NHC0 \"" + propertyCode + "\"^^xsd:string .").append("\n");
        buf.append("                ?p rdfs:label ?p_label .").append("\n");
        buf.append("").append("\n");
        buf.append("                ?a ?q1 ?q1_value .").append("\n");
        buf.append("                ?q1 :NHC0 ?q1_code .").append("\n");
        buf.append("                ?q1 :NHC0 \"" + qualifierCode + "\"^^xsd:string .").append("\n");
        buf.append("                ?q1 rdfs:label ?q1_label .").append("\n");
        buf.append("}").append("\n");
        return buf.toString();
	}


	public Vector getDistinctPropertyQualifierValues(String named_graph, String code, String propertyCode, String qualifierCode) {
		String query = construct_get_annotated_target_count(named_graph, propertyCode);
		Vector v = executeQuery(query);
		v = new SortUtils().quickSort(v);
		return v;
	}

	public String construct_get_distinct_property_values(String named_graph, String propertyName) {
        String prefixes = getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        buf.append("select distinct ?p_value").append("\n");
        buf.append("from <" + named_graph + ">").append("\n");
        buf.append("where  { ").append("\n");
        buf.append("                ?x a owl:Class .").append("\n");
        buf.append("                ?x :NHC0 ?x_code .").append("\n");
        buf.append("                ?x rdfs:label ?x_label .").append("\n");
        buf.append("").append("\n");
        buf.append("                ?p :NHC0 ?p_code .").append("\n");
        buf.append("                ?p rdfs:label ?p_label .").append("\n");
        buf.append("                ?p rdfs:label \"" + propertyName + "\"^^xsd:string .").append("\n");
        buf.append("                ?p a owl:AnnotationProperty .").append("\n");
        buf.append("                ").append("\n");
        buf.append("                ?x ?p ?p_value .").append("\n");
        buf.append("}").append("\n");
        return buf.toString();
	}

	public Vector getDistinctPropertyValues(String named_graph, String propertyName) {
        String query = construct_get_distinct_property_values(named_graph, propertyName);
        Vector v = executeQuery(query);
        v = new SortUtils().quickSort(v);
        return v;
	}


	public String construct_get_concepts_with_property_value(String named_graph, String propertyName, String propertyValue) {
        String prefixes = getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        buf.append("select distinct ?x_label ?x_code ?p_label ?p_value").append("\n");
        buf.append("from <" + named_graph + ">").append("\n");
        buf.append("where  { ").append("\n");
        buf.append("                ?x a owl:Class .").append("\n");
        buf.append("                ?x :NHC0 ?x_code .").append("\n");
        buf.append("                ?x rdfs:label ?x_label .").append("\n");
        buf.append("").append("\n");
        buf.append("                ?p :NHC0 ?p_code .").append("\n");
        buf.append("                ?p rdfs:label ?p_label .").append("\n");
        buf.append("                ?p rdfs:label \"" + propertyName + "\"^^xsd:string .").append("\n");
        buf.append("                ?p a owl:AnnotationProperty .").append("\n");
        buf.append("                ").append("\n");
        buf.append("                ?x ?p ?p_value .").append("\n");
        buf.append("                ?x ?p  \"" + propertyValue + "\"^^xsd:string .").append("\n");
        buf.append("}").append("\n");
        return buf.toString();
	}


	public Vector getConceptsWithPropertyValue(String named_graph, String propertyName, String propertyValue) {
		String query = construct_get_concepts_with_property_value(named_graph, propertyName, propertyValue);
		Vector v = executeQuery(query);
		v = removeDuplicates(v);
		return v;
	}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Vector getPropertyCounts(String named_graph) {
		HashMap hmap = createQualifierCode2PropertyCodeHashMap(named_graph);
		int count = 0;
        Vector v = new Vector();
        for (int i=0; i<properties.size(); i++) {

			String line = (String) properties.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String property_name = (String) u.elementAt(0);
			String property_code = (String) u.elementAt(1);

            if (is_ANNOTATED_TARGET_CODES(property_code)) {
				String prop_code = (String) ANNOTATED_TARGET_HASHMAP.get(property_code);
				int knt = getAnnotatedTargetCount(named_graph, prop_code);
				count = count + knt;
				v.add(property_name + " (" + property_code + ")" + "|" + knt);
				System.out.println(property_name + " (" + property_code + ")" + "|" + knt);
			} else if (property_code.startsWith("P") && !hmap.containsKey(property_code)) {
				Vector w = getConceptsWithProperty(named_graph, property_code);
				if (w == null) {
					w = new Vector();
				}
				count = count + w.size();
				v.add(property_name + " (" + property_code + ")" + "|" +  w.size());
				System.out.println(property_name + " (" + property_code + ")" + "|" + w.size());
			} else if (property_code.startsWith("P") && hmap.containsKey(property_code)) {
				Vector v2 = (Vector) hmap.get(property_code);
				for (int k=0; k<v2.size(); k++) {
					String prop_code = (String) v2.elementAt(k);
					int knt = getAnnotatedTargetCount(named_graph, prop_code, property_code);
					count = count + knt;
					String label1 = property_name + " (" + property_code + ")";
					String prop_name = (String) propertyCode2NameHashMap.get(prop_code);
					String label2 = prop_name + " (" + prop_code + ")";
					v.add(label1 + " of " + label2 + "|" + knt);
					System.out.println(label1 + " of " + label2 + "|" + knt);
				}
			}
		}
		v = new SortUtils().quickSort(v);
		v.add("Total" + "|" + count);
		return v;
	}

	public static Vector findDuplicates(Vector v) {
		Vector w = new Vector();
		HashSet hset = new HashSet();
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			if (!hset.contains(t)) {
				hset.add(t);
			} else {
				w.add(t);
			}
		}
		return w;
	}

	public Vector getPropertyValueCounts(String named_graph, String propertyName) {
		String propertyCode = (String) propertyName2CodeHashMap.get(propertyName);
		return getPropertyValueTableData(named_graph, propertyCode);
	}


	public String construct_get_hierarchy_part1(String named_graph) {
        String prefixes = getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        buf.append("SELECT ?z_label ?z_code ?x_label ?x_code").append("\n");
        buf.append("{").append("\n");
        buf.append("  graph <http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl>").append("\n");
        buf.append("  {").append("\n");
        buf.append("          {").append("\n");
        buf.append("                  {").append("\n");
        buf.append("                    ?x a owl:Class .").append("\n");
        buf.append("                    ?x rdfs:label ?x_label .").append("\n");
        buf.append("                    ?x :NHC0 ?x_code .").append("\n");
        buf.append("                    ?x rdfs:subClassOf ?z .").append("\n");
        buf.append("                    ?z a owl:Class .").append("\n");
        buf.append("                    ?z rdfs:label ?z_label .").append("\n");
        buf.append("                    ?z :NHC0 ?z_code").append("\n");
        buf.append("                  }").append("\n");
        buf.append("                  FILTER (?x != ?z)").append("\n");
        buf.append("          }").append("\n");
        buf.append("   }").append("\n");
        buf.append("}").append("\n");
        return buf.toString();
	}


	public static Vector dumpTallies(HashMap hmap) {
		Vector v = new Vector();
		int count = 0;
		Vector keys = new Vector();
		Iterator it = hmap.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			keys.add(key);
		}
		keys = new SortUtils().quickSort(keys);
		it = hmap.keySet().iterator();
		for (int i=0; i<keys.size(); i++) {
			String key = (String) keys.elementAt(i);
			Integer int_obj = (Integer) hmap.get(key);
			v.add(key + "|" + Integer.valueOf(int_obj));
			count = count + Integer.valueOf(int_obj);
		}
		v.add("Total|" + count);
		return v;
	}

	public static HashMap getTallyHashMap(String datafile) {
		HashMap hmap = new HashMap();
		Vector w = Utils.readFile(datafile);
		for (int i=0; i<w.size(); i++) {
			String t = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(t, '|');
			String roleName = (String) u.elementAt(2);
			String roleCode = (String) u.elementAt(3);
			String key = roleName + " (" + roleCode + ")";
			Integer int_obj = new Integer(0);
			if (hmap.containsKey(key)) {
				int_obj = (Integer) hmap.get(key);
			}
			int count = Integer.valueOf(int_obj);
			hmap.put(key, new Integer(count+1));
		}
		return hmap;
	}


    public Vector removeDuplicates(Vector v) {
		if (v == null) return v;
		HashSet hset = new HashSet();
		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			if (!hset.contains(t)) {
				hset.add(t);
			}
		}
		Iterator it = hset.iterator();
		while (it.hasNext()) {
			String t = (String) it.next();
			w.add(t);
		}
		return w;
	}

	public Vector getPropertyValueTableData(String named_graph, String propertyCode) {
		Vector v = getConceptsWithProperty(named_graph, propertyCode);
		return getPropertyValueTableData(v);
	}

	public Vector getPropertyValueTableData(Vector v) {
		return generatePropertyValuetableData(getPropertyValueHashMap(v));
	}

	public void dumpHashMap(HashMap hmap) {
		Iterator it = hmap.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			Integer value = (Integer) hmap.get(key);
			System.out.println(key + " --> " + Integer.valueOf(value));
        }
	}


	public HashMap getPropertyValueHashMap(Vector v) {
		HashMap hmap = new HashMap();
		if (v == null) return hmap;
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String key = (String) u.elementAt(u.size()-1);
			Integer int_obj = new Integer(0);
			if (hmap.containsKey(key)) {
			    int_obj = (Integer) hmap.get(key);
			}
			int knt = Integer.valueOf(int_obj);
			knt++;

			hmap.put(key, new Integer(knt));
		}
		return hmap;
	}

	public Vector generatePropertyValuetableData(HashMap hmap) {
		return generatePropertyValuetableData(null, hmap);
	}

	public Vector generatePropertyValuetableData(Vector values, HashMap hmap) {
		Vector w = new Vector();
		if (hmap == null) return null;
		if (values != null) {
			for (int k=0; k<values.size(); k++) {
				String value = (String) values.elementAt(k);
				if (!hmap.containsKey(value)) {
					hmap.put(value, new Integer(0));
				}
			}
		}
		Vector keys = new Vector();
		Iterator it = hmap.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			keys.add(key);
		}
		keys = new SortUtils().quickSort(keys);
		int total = 0;
		for (int i=0; i<keys.size(); i++) {
			String key = (String) keys.elementAt(i);
			Integer int_obj = (Integer) hmap.get(key);
			int count = Integer.valueOf(int_obj);
			total = total + count;
			w.add(key + "|" + count);
		}
		w.add("Total|" + total);
		return w;
	}

    public static boolean checkIfFileExists(String filename) {
		String currentDir = System.getProperty("user.dir");
		File f = new File(currentDir + "\\" + filename);
		if(f.exists() && !f.isDirectory()) {
			return true;
		} else {
			return false;
		}
	}

    public Vector generateBranchSizeTableData() {
		return generateBranchSizeTableData(false);
	}

    public Vector generateBranchSizeTableData(boolean savedata) {
		Vector v = new Vector();
		if (checkIfFileExists(PARENT_CHILD_FILE)) {
			v = Utils.readFile(PARENT_CHILD_FILE);
			savedata = false;
		} else {
			v = getHierarchicalRelationships(named_graph);
		}
		if (savedata) {
		    Utils.saveToFile(PARENT_CHILD_FILE, v);
		}
		hh = new HierarchyHelper(v, 1);
		return generateBranchSizeTableData(v);
	}

    public Vector convert(Vector v) {
		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String src_code = (String) u.elementAt(0);
			String role_code = (String) u.elementAt(1);
			String target_code = (String) u.elementAt(2);

			String role_label = (String) roleCode2NameHashMap.get(role_code);
			String src_label = hh.getLabel(src_code);
			String target_label = hh.getLabel(target_code);
			w.add(src_label + "|" + src_code + "|" + role_label + "|" + role_code + "|" + target_label + "|" + target_code);
		}
		return w;
	}

    public Vector generateBranchSizeTableData(Vector v) {
		Vector roots = hh.getRoots();
		Vector w = new Vector();
		for (int i=0; i<roots.size(); i++) {
		    String root = (String) roots.elementAt(i);
		    String label = hh.getLabel(root);
		    w.add(label + " (" + root + ")");
		}
		Vector v0 = new Vector();
		w = new SortUtils().quickSort(w);
		StringUtils.dumpVector("roots", w);
        int total = 0;
		for (int i=0; i<roots.size(); i++) {
		    String root = (String) roots.elementAt(i);
		    String label = hh.getLabel(root);
			int count = hh.get_transitive_closure(root);
			v0.add(hh.getLabel(root) + " (" + root + ")|" + count);
			total = total + count;
		}
		v0 = new SortUtils().quickSort(v0);
		v0.add("Total|" + total);
		return v0;
	}

    public void generateTableData() {
		Vector v = new Vector();
		addTitle("NCI Thesaurus Statistics");

        v = generateBranchSizeTableData();
	    String tableName = "Branch Size";
	    Vector th_vec = new Vector();
	    th_vec.add("Root");
	    th_vec.add("Count");
	    addTable(tableName, th_vec, v);

	    v = getPropertyValueCounts(named_graph, "Semantic_Type");
	    tableName = "Semantic_Type";
	    th_vec = new Vector();
	    th_vec.add("Semantic Type");
	    th_vec.add("Count");
	    addTable(tableName, th_vec, v);
	    addFooter();

	    v = getPropertyValueCounts(named_graph, "Contributing_Source");
	    tableName = "Contributing_Source";
	    th_vec = new Vector();
	    th_vec.add("Contributing Source");
	    th_vec.add("Count");
	    addTable(tableName, th_vec, v);
	    addFooter();

	    v = getPropertyValueCounts(named_graph, "Concept_Status");
	    tableName = "Concept_Status";
	    th_vec = new Vector();
	    th_vec.add("Concept Status");
	    th_vec.add("Count");
	    addTable(tableName, th_vec, v);
	    addFooter();

        v = getPropertyCounts(named_graph);
	    tableName = "Properties";
	    th_vec = new Vector();
	    th_vec.add("Property");
	    th_vec.add("Count");
	    addTable(tableName, th_vec, v);
	    addFooter();

        v = getAssociations(named_graph);
        Vector association_knt_vec = getRelationsipCounts(v);

	    tableName = "Associations";
	    th_vec = new Vector();
	    th_vec.add("Association");
	    th_vec.add("Count");
	    addTable(tableName, th_vec, association_knt_vec);

        if (checkIfFileExists(RESTRICTION_FILE)) {
			v = Utils.readFile(RESTRICTION_FILE);
		} else{
        	v = getRoleTargets(named_graph);
		}

		String firstLine = (String) v.elementAt(0);
		Vector u = StringUtils.parseData(firstLine, '|');
		if (u.size() == 3) {
			v = convert(v);
			Utils.saveToFile(RESTRICTION_FILE, v);
		}

		Vector role_vec = getRelationsipCounts(v);
	    tableName = "Roles";
	    th_vec = new Vector();
	    th_vec.add("Role");
	    th_vec.add("Count");
	    addTable(tableName, th_vec, role_vec);

	    addFooter();
	}


    public Vector getRelationsipCounts(Vector asso_vec) {
		HashMap hmap = new HashMap();
		Vector keys = new Vector();
		for (int i=0; i<asso_vec.size(); i++) {
			String line = (String) asso_vec.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String asso_label = (String) u.elementAt(2);
			Integer int_obj = new Integer(0);
			if (hmap.containsKey(asso_label)) {
				int_obj = (Integer) hmap.get(asso_label);
			}
			int count = Integer.valueOf(int_obj);
			count++;
			hmap.put(asso_label, new Integer(count));
			if (!keys.contains(asso_label)) {
				keys.add(asso_label);
			}
		}
		keys = new SortUtils().quickSort(keys);
		Vector v = new Vector();
		int total = 0;
		for (int i=0; i<keys.size(); i++) {
			String key = (String) keys.elementAt(i);
			Integer int_obj = (Integer) hmap.get(key);
			int count = Integer.valueOf(int_obj);
			total = total + count;
			v.add(key + "|" + count);
		}
		v.add("Total|" + total);
		return v;
	}

    public int get_branch_size(String code) {
		int count = 0;
		Vector v = null;
		Stack stack = new Stack();
		stack.push(code);
		while (!stack.isEmpty()) {
			String next_code = (String) stack.pop();
			count++;
			v = owlSPARQLUtils.getSubclassesByCode(named_graph, next_code);
			v = new ParserUtils().getResponseValues(v);
			if (v != null) {
				for (int i=0; i<v.size(); i++) {
					String line = (String) v.elementAt(i);
					Vector u = StringUtils.parseData(line, '|');
					String child_code = (String) u.elementAt(1);
					stack.push(child_code);
				}
			}
		}
		return count;
	}

	public void generate() {
		generateTableData();
		String outputfile = new HTMLTable().generate(table_data);
		System.out.println(outputfile + " generated.");
	}

	public Vector get_hierarchical_relationships(String named_graph) {
		return owlSPARQLUtils.getHierarchicalRelationships(named_graph);
	}

	public static void main(String[] args) {
		long ms = System.currentTimeMillis();
		String serviceUrl = args[0];
		String named_graph = args[1];
		String username = args[2];
		String password = args[3];
		EVSStatistics evsStatistics = new EVSStatistics(serviceUrl, named_graph, username, password);
	    evsStatistics.generate();
        System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}
}

