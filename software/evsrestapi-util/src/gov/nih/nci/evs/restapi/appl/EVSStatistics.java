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
	int table_number = 0;

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
    static String NCIT_OWL = "ThesaurusInferred_forTS.owl";
	static String PARENT_CHILD_FILE = "parent_child.txt";
	static String RESTRICTION_FILE = "roles.txt";

	HashSet retired_concepts = new HashSet();
	HashMap valueset2ContributingSourceMap = new HashMap();
	HashMap valuesetCode2NameMap = new HashMap();
	HashMap valuesetName2CodeMap = new HashMap();

	static HashMap propertyCode2CountMap = null;

    static {
		ANNOTATED_TARGETS = new String[] {"term-name", "go-term", "TARGET TERM"};
		ANNOTATED_TARGET_CODES = new String[] {"P382", "P388", "P392"};
		ANNOTATED_TARGET_LIST = Arrays.asList(ANNOTATED_TARGET_CODES);
		ANNOTATED_TARGET_HASHMAP = new HashMap();
		ANNOTATED_TARGET_HASHMAP.put("P382", "P90");
		ANNOTATED_TARGET_HASHMAP.put("P388", "P211");
		ANNOTATED_TARGET_HASHMAP.put("P392", "P375");

		OWLScanner scanner = new OWLScanner(NCIT_OWL);
		propertyCode2CountMap = scanner.getPropertyCode2CountMap();
		Vector roles = scanner.extractOWLRestrictions(scanner.get_owl_vec());
		Utils.saveToFile(RESTRICTION_FILE, roles);
		scanner.get_owl_vec().clear();
	}

	public String get_ncit_version() {
		Vector v = owlSPARQLUtils.get_ontology_info(named_graph);
		Utils.dumpVector("get_ncit_version", v);
		String line = (String) v.elementAt(0);
		Vector u = StringUtils.parseData(line, '|');
		String ncit_version = (String) u.elementAt(0);
		System.out.println(ncit_version);
		return ncit_version;
	}

	public EVSStatistics(String serviceUrl, String named_graph, String username, String password) {
		this.serviceUrl = serviceUrl;
		this.named_graph = named_graph;
		this.username = username;
		this.password = password;
		this.table_number = 0;

		this.metadataUtils = new MetadataUtils(serviceUrl, username, password);

		this.owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, username, password);
		this.owlSPARQLUtils.set_named_graph(named_graph);
		this.version  = get_ncit_version();//metadataUtils.getVocabularyVersion(named_graph);
        System.out.println("NCI Thesaurus version: " + version);
        try {
			httpUtils = new HTTPUtils();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		System.out.println("httpUtils instantiated.");
		System.out.println("getSupportedProperties ...");
		properties = getSupportedProperties(named_graph);

		System.out.println("properties: " + properties.size());
		Utils.saveToFile("properties.txt", properties);

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

        System.out.println("getObjectProperties ...");
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

		//retired_concepts
		retired_concepts = new HashSet();
	    String property_name = "Concept_Status";
	    String property_value = "Retired_Concept";
	    System.out.println("findConceptsWithProperty property_value: " + property_value);

		Vector w = owlSPARQLUtils.findConceptsWithProperty(named_graph, property_name);
		for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String status = (String) u.elementAt(u.size()-1);
			if (status.compareTo(property_value) == 0) {
				String code = (String) u.elementAt(1);
				retired_concepts.add(code);
			}
		}
		System.out.println("Number of retired concepts: " + retired_concepts.size());

		valuesetCode2NameMap = new HashMap();
		valuesetName2CodeMap = new HashMap();
		valueset2ContributingSourceMap = new HashMap();

		System.out.println("getValueSets ...");
		boolean publishedOnly = false;
	    w = getValueSets(named_graph, publishedOnly);
		for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String code = (String) u.elementAt(1);
			String name = (String) u.elementAt(0);
			valueset2ContributingSourceMap.put(code, "No External Source");
			valuesetCode2NameMap.put(code, name);
			valuesetName2CodeMap.put(name, code);
		}

        System.out.println("getValueSetsWithContributingSource ...");
	    w = getValueSetsWithContributingSource(named_graph, publishedOnly);

		Utils.saveToFile("value_set_w_cs.txt", w);

		for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String code = (String) u.elementAt(1);
			String source = (String) u.elementAt(3);
			valueset2ContributingSourceMap.put(code, source);
		}
		System.out.println("EVSStatistics instantiated.");
	}

	public int getPropertyCount(String propertyCode) {
		if (!propertyCode2CountMap.containsKey(propertyCode)) {
			return 0;
		}
		Integer int_obj = (Integer) propertyCode2CountMap.get(propertyCode);
		return int_obj.intValue();
	}

	public void addTitle(String title) {
		table_data.add("<title>" + title);
	}

	public boolean is_retired(String code) {
		return retired_concepts.contains(code);
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

		//Vector w = HTTPUtils.runQuery(serviceUrl, username, password, query);
		Vector w = owlSPARQLUtils.executeQuery(query);

		return w;
	}

    public void run(String queryfile) {
		String query = HTTPUtils.loadQuery(queryfile);
		//boolean parsevalues = true;
		//Vector w = httpUtils.execute(serviceUrl, username, password, query, parsevalues);
		Vector w = HTTPUtils.runQuery(serviceUrl,  username, password, query);
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
        	Utils.saveToFile(RESTRICTION_FILE, v);
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
        buf.append("from <" + named_graph + ">").append("\n");
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
        buf.append("from <" + named_graph + ">").append("\n");
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
        buf.append("from <" + named_graph + ">").append("\n");
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
	public static HashMap getPropertyCountHashMap(String owlfile) {
		OWLScanner owlscanner = new OWLScanner(owlfile);
		Vector v = owlscanner.extractProperties(owlscanner.get_owl_vec());
		HashMap propertyCountHashMap = new HashMap();
		for (int i=0; i<v.size(); i++) {
			String line = (String)v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String propertyCode = (String) u.elementAt(1);
			Integer int_obj = Integer.valueOf(0);
			if (propertyCountHashMap.containsKey(propertyCode)) {
				int_obj = (Integer) propertyCountHashMap.get(propertyCode);
			}
			int count = int_obj.intValue();
            int_obj = Integer.valueOf(count+1);
            propertyCountHashMap.put(propertyCode, int_obj);
		}
		return propertyCountHashMap;
	}

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
				System.out.println("(*)" + property_name + " (" + property_code + ")" + "|" + knt);

			} else if (property_code.startsWith("P") && !hmap.containsKey(property_code)) {
				Vector w = null;
				try {
					/*
					w = getConceptsWithProperty(named_graph, property_code);
					if (w == null) {
						w = new Vector();
					}
					count = count + w.size();
					v.add(property_name + " (" + property_code + ")" + "|" +  w.size());
					*/
					Integer prop_count_obj = (Integer) propertyCode2CountMap.get(property_code);
					int prop_count = prop_count_obj.intValue();
					count = count + prop_count;
					v.add(property_name + " (" + property_code + ")" + "|" +  prop_count);

					System.out.println(property_name + " (" + property_code + ")" + "|" + prop_count);
				} catch (Exception ex) {
					int knt = getPropertyCount(property_code);
					count = count + knt;
					v.add(property_name + " (" + property_code + ")" + "|" +  knt);
					System.out.println("(**)" + property_name + " (" + property_code + ")" + "|" + knt);
				}

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

	public String construct_get_value_sets(String named_graph) {
		boolean publishedOnly = true;
		return construct_get_value_sets(named_graph, publishedOnly);
	}

	public String construct_get_value_sets(String named_graph, boolean publishedOnly) {
        String prefixes = getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        buf.append("select distinct ?x_label ?x_code ").append("\n");
        buf.append("from <" + named_graph + ">").append("\n");
        buf.append("where  { ").append("\n");
        buf.append("                ?x a owl:Class .").append("\n");
        buf.append("                ?x :NHC0 ?x_code .").append("\n");
        buf.append("                ?x rdfs:label ?x_label .").append("\n");
        buf.append("").append("\n");
        buf.append("                ?p2 a owl:AnnotationProperty .").append("\n");
        buf.append("                ?p2 :NHC0 ?p2_code .").append("\n");
        buf.append("                ?p2 rdfs:label ?p2_label .").append("\n");
        buf.append("                ?p2 rdfs:label \"Publish_Value_Set\"^^xsd:string .").append("\n");
        buf.append("                ").append("\n");
        buf.append("                ?x ?p2 ?p2_value .").append("\n");
        if (publishedOnly) {
       	    buf.append("                ?x ?p2 \"Yes\"^^xsd:string .").append("\n");
		}
        buf.append("}").append("\n");
        return buf.toString();
	}

	public Vector getValueSets(String named_graph) {
		boolean publishedOnly = true;
		String query = construct_get_value_sets(named_graph, publishedOnly);
		Vector v = executeQuery(query);
		return v;
	}

	public Vector getValueSets(String named_graph, boolean publishedOnly) {
		String query = construct_get_value_sets(named_graph, publishedOnly);
		Vector v = executeQuery(query);
		return v;
	}

	public String construct_get_value_sets_with_contributing_source(String named_graph) {
		boolean publishedOnly = true;
		return construct_get_value_sets_with_contributing_source(named_graph, publishedOnly);
	}

	public String construct_get_value_sets_with_contributing_source(String named_graph, boolean publishedOnly) {
        String prefixes = getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        buf.append("select distinct ?x_label ?x_code ?p1_label ?p1_value ").append("\n");
        //buf.append("select distinct ?x_label ?x_code ?p1_value ?p2_value ").append("\n");
        buf.append("from <" + named_graph + ">").append("\n");
        buf.append("where  { ").append("\n");
        buf.append("                ?x a owl:Class .").append("\n");
        buf.append("                ?x :NHC0 ?x_code .").append("\n");
        buf.append("                ?x rdfs:label ?x_label .").append("\n");
        buf.append("").append("\n");
        buf.append("                ?p1 a owl:AnnotationProperty .").append("\n");
        buf.append("                ?p1 :NHC0 ?p1_code .").append("\n");
        buf.append("                ?p1 rdfs:label ?p1_label .").append("\n");
        buf.append("                ?p1 rdfs:label \"Contributing_Source\"^^xsd:string .").append("\n");
        buf.append("                ?x  ?p1 ?p1_value .").append("\n");
        buf.append("").append("\n");
        buf.append("                ?p2 a owl:AnnotationProperty .").append("\n");
        buf.append("                ?p2 :NHC0 ?p2_code .").append("\n");
        buf.append("                ?p2 rdfs:label ?p2_label .").append("\n");
        buf.append("                ?p2 rdfs:label \"Publish_Value_Set\"^^xsd:string .").append("\n");
        buf.append("                ").append("\n");
        buf.append("                ?x ?p2 ?p2_value .").append("\n");
        if (publishedOnly) {
        	buf.append("                ?x ?p2 \"Yes\"^^xsd:string .").append("\n");
		}
        buf.append("}").append("\n");
        return buf.toString();
	}

	public Vector getValueSetsWithContributingSource(String named_graph) {
		boolean publishedOnly = true;
		String query = construct_get_value_sets_with_contributing_source(named_graph, publishedOnly);
		Vector v = executeQuery(query);
		return v;
	}

	public Vector getValueSetsWithContributingSource(String named_graph, boolean publishedOnly) {
		String query = construct_get_value_sets_with_contributing_source(named_graph, publishedOnly);
		Vector v = executeQuery(query);
		return v;
	}

	public String construct_get_concepts_in_subset(String named_graph, String subset_code, boolean codeOnly) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		if (codeOnly) {
			buf.append("SELECT ?x_code").append("\n");
		} else {
			buf.append("SELECT ?x_label ?x_code").append("\n");
	    }
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("            ?x " + named_graph_id + " ?x_code .").append("\n");
		buf.append("            ?y a owl:AnnotationProperty .").append("\n");
		buf.append("            ?x ?y ?z .").append("\n");
		buf.append("            ?z " + named_graph_id + " \"" + subset_code + "\"^^xsd:string .").append("\n");
		buf.append("            ?y rdfs:label " + "\"" + "Concept_In_Subset" + "\"^^xsd:string ").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getConceptsInSubset(String named_graph, String code, boolean codeOnly) {
		return executeQuery(construct_get_concepts_in_subset(named_graph, code, codeOnly));
	}

	public String construct_get_value_set_data(String named_graph) {
		boolean publishedOnly = true;
		return construct_get_value_set_data(named_graph, publishedOnly);
	}

	public String construct_get_value_set_data(String named_graph, boolean publishedOnly) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("select distinct ?y_label ?y_code ?p0_label ?x_label ?x_code ?p1_label ?p1_value ?p2_label ?p2_value ").append("\n");
		buf.append("from <" + named_graph + ">").append("\n");
		buf.append("where  { ").append("\n");
		buf.append("                ?y a owl:Class .").append("\n");
		buf.append("                ?y :NHC0 ?y_code .").append("\n");
		buf.append("                ?y rdfs:label ?y_label .").append("\n");
		buf.append("").append("\n");
		buf.append("                ?p0 a owl:AnnotationProperty .").append("\n");
		buf.append("                ?p0 :NHC0 ?p0_code .").append("\n");
		buf.append("                ?p0 rdfs:label ?p0_label .").append("\n");
		buf.append("                ?p0 rdfs:label \"Concept_In_Subset\"^^xsd:string .  ").append("\n");
		buf.append(" ").append("\n");
		buf.append("                ?x a owl:Class .").append("\n");
		buf.append("                ?x :NHC0 ?x_code .").append("\n");
		buf.append("                ?x rdfs:label ?x_label .").append("\n");
		buf.append("").append("\n");
		buf.append("                ?y ?p0 ?x .   ").append("\n");
		buf.append("").append("\n");
		buf.append("                ?p1 a owl:AnnotationProperty .").append("\n");
		buf.append("                ?p1 :NHC0 ?p1_code .").append("\n");
		buf.append("                ?p1 rdfs:label ?p1_label .").append("\n");
		buf.append("                ?p1 rdfs:label \"Contributing_Source\"^^xsd:string .").append("\n");

		buf.append("                OPTIONAL {").append("\n");
		buf.append("                ?x  ?p1 ?p1_value .").append("\n");
		buf.append("                }").append("\n");
		buf.append("").append("\n");
		buf.append("                ?p2 a owl:AnnotationProperty .").append("\n");
		buf.append("                ?p2 :NHC0 ?p2_code .").append("\n");
		buf.append("                ?p2 rdfs:label ?p2_label .").append("\n");
		buf.append("                ?p2 rdfs:label \"Publish_Value_Set\"^^xsd:string .").append("\n");
		buf.append("                ").append("\n");
		buf.append("                ?x ?p2 ?p2_value .").append("\n");
		if (publishedOnly) {
			buf.append("                ?x ?p2 \"Yes\"^^xsd:string .").append("\n");
		}
		buf.append("}").append("\n");
		return buf.toString();
	}


//	(221972) CTRP Disease Finding|C173902|Concept_In_Subset|CTS-API Disease Broad Category Terminology|C138189|Contributing_Source|Publish_Value_Set|null|Yes

	public Vector getValueSetData(String named_graph) {
		boolean publishedOnly = true;
		return getValueSetData(named_graph, publishedOnly);
	}

	public Vector getValueSetData(String named_graph, boolean publishedOnly) {
		Vector v = executeQuery(construct_get_value_set_data(named_graph, publishedOnly));

		Utils.saveToFile("value_set_data.txt", v);

		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
////COVID-19 Infection|C171133|Concept_In_Subset|CTS-API Disease Main Type Terminology|C138190|Contributing_Source|Publish_Value_Set|null|Yes

//Contributing_Source|Publish_Value_Set|null

line = line.replace("Contributing_Source|Publish_Value_Set|null", "Contributing_Source|null|Publish_Value_Set");


			Vector u = StringUtils.parseData(line, '|');
			String code = (String) u.elementAt(1);
			boolean include = true;
			boolean is_retired = is_retired(code);

			String subset_code = (String) u.elementAt(4);
			boolean is_subset_code_retired = is_retired(subset_code);

			if (publishedOnly) {
				if (!line.endsWith("Publish_Value_Set|Yes")) {
					include = false;
				}
			}
			if (include && !is_retired && !is_subset_code_retired) {
				String name = (String) u.elementAt(0);
				//String code = (String) u.elementAt(1);
				String Concept_In_Subset = (String) u.elementAt(2);
				String subset_name = (String) u.elementAt(3);
				//String subset_code = (String) u.elementAt(4);
				String Contributing_Source = (String) u.elementAt(5);
				String Contributing_Source_value = (String) u.elementAt(6);

				if (Contributing_Source_value.compareTo("null") == 0) {
					Contributing_Source_value = "No External Source";
				}

				String Publish_Value_Set = (String) u.elementAt(7);
				String Publish_Value_Set_value = (String) u.elementAt(8);
				String t = name + "|" + code + "|" + Concept_In_Subset + "|" + subset_name + "|" + subset_code
				    + "|Contributing_Source|" + Contributing_Source_value + "|"
				    + Publish_Value_Set + "|" + Publish_Value_Set_value;

				w.add(t);

			}
		}
		return w;
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
			Integer int_obj = Integer.valueOf(0);
			if (hmap.containsKey(key)) {
				int_obj = (Integer) hmap.get(key);
			}
			int count = Integer.valueOf(int_obj);
			hmap.put(key, Integer.valueOf(count+1));
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
			Integer int_obj = Integer.valueOf(0);
			if (hmap.containsKey(key)) {
			    int_obj = (Integer) hmap.get(key);
			}
			int knt = Integer.valueOf(int_obj);
			knt++;

			hmap.put(key, Integer.valueOf(knt));
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
					hmap.put(value, Integer.valueOf(0));
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
		return generate_branch_size_table_data();
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

    public Vector generate_branch_size_table_data() {
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
        int total = 0; // nodes
        int total2 = 0; // concepts excl retired
        int total3 = 0;
        int total4 = 0;
        Vector active_retired_concepts = new Vector();
        HashSet active_retired_concepts_hset = new HashSet();
		for (int i=0; i<roots.size(); i++) {
			String root = (String) roots.elementAt(i);
			String root_label = hh.getLabel(root);
			String label = hh.getLabel(root);
			//System.out.println("Processing " + label + " (" + root + ")");
			int count = hh.get_transitive_closure(root); //number of nodes
			Vector v3 = hh.get_transitive_closure_v3(root);
			HashSet hset = new HashSet();
			int retired = 0;
			for (int j=0; j<v3.size(); j++) {
				String t = (String) v3.elementAt(j);
				if (is_retired(t)) {
					if (root.compareTo("C28428") != 0) {
						String s = hh.getLabel(t);
						String str = root + "|" + root_label + "|" + t + "|" + s;
						if (!active_retired_concepts_hset.contains(str)) {
							active_retired_concepts.add(str);
							active_retired_concepts_hset.add(str);
						}
					}
					retired++;
				}
				hset.add(t);
			}
			int count2 = hset.size() - retired;
			int count3 = retired;
			int count4 = hset.size();

			total2 = total2 + count2;
			total3 = total3 + count3;
			total4 = total4 + count4;

			total = total + count;
			v0.add(hh.getLabel(root) + " (" + root + ")|" + count + "|" + count2 + "|" + count3 + "|" + count4);
		}
		v0 = new SortUtils().quickSort(v0);
		v0.add("Total|" + total + "|" + total2 + "|" + total3 + "|" + total4);
		active_retired_concepts = new SortUtils().quickSort(active_retired_concepts);
		Utils.dumpVector("active_retired_concepts.txt", active_retired_concepts);
		return v0;
	}

    public Vector generateValueSetTableData() {
		Vector ret_vec = new Vector();
		boolean publishedOnly = false;
		Vector v = getValueSets(named_graph, publishedOnly);
		int total = 0;
		for (int i=0 ; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String label = (String) u.elementAt(0);
			String code = (String) u.elementAt(1);
			if (!is_retired(code)) {
				String contributing_source = (String) valueset2ContributingSourceMap.get(code);
				Vector w = getConceptsInSubset(named_graph, code, true);
				int count = 0;
				if (w != null) {
					count = w.size();
				}
				total = total + count;
				String key = label + " (" + code + ")";
				ret_vec.add(key + "|" + contributing_source + "|" + count);
			}
		}
		ret_vec = new SortUtils().quickSort(ret_vec);
		ret_vec.add("Total||" + total);
        return ret_vec;
	}

    public Vector generateSubBranchData(Vector roots) {
		Vector v0 = new Vector();
		for (int i=0; i<roots.size(); i++) {
		    String root = (String) roots.elementAt(i);
		    String label = hh.getLabel(root);
			int count = hh.get_transitive_closure(root); //number of nodes
			Vector v3 = hh.get_transitive_closure_v3(root);
			HashSet hset = new HashSet();
			int retired = 0;
			for (int j=0; j<v3.size(); j++) {
				String t = (String) v3.elementAt(j);
				if (is_retired(t)) {
					retired++;
				}
                hset.add(t);
			}
			int count2 = hset.size() - retired;
			int count3 = retired;
			int count4 = hset.size();
			v0.add(hh.getLabel(root) + " (" + root + ")|" + count + "|" + count2 + "|" + count3 + "|" + count4);
		}
		v0 = new SortUtils().quickSort(v0);
		return v0;
	}

    public static Vector createValueSetTableData(HashMap valuesetCountHashMap, HashMap hmap) {
        Vector w0 = new Vector();
        Vector w = new Vector();
        Vector keys = new Vector();
        Iterator it = hmap.keySet().iterator();
        while (it.hasNext()) {
			String t = (String) it.next();
			keys.add(t);
		}
		int total = 0;
		int sub_total = 0;
		keys = new SortUtils().quickSort(keys);
		for (int i=0; i<keys.size(); i++) {
			String key = (String) keys.elementAt(i);
			w = new Vector();
			Vector values = (Vector) hmap.get(key);
			values = new SortUtils().quickSort(values);
			sub_total = 0;
			for (int k=0; k<values.size(); k++) {
				String value = (String) values.elementAt(k);
				Integer int_obj = (Integer) valuesetCountHashMap.get(value);
				int count = Integer.valueOf(int_obj);
				sub_total = sub_total + count;
				int k1 = k+1;
				total = total + count;
				w.add(key + "|" + value + "|" + count);
			}
			w = new SortUtils().quickSort(w);
			w.add("Subtotal (" + key + ")|" + "" + "|" + sub_total);
			w0.addAll(w);
		}
		w0.add("Total||" + total);
		return w0;
    }

    public static Vector createValueSetSourceTableData(
            HashMap source2ValueSetCountHashMap,
            HashMap source2ConceptCountHashMap,
            HashMap source2UniqueConceptCountHashMap) {
        Vector w = new Vector();
        Vector keys = new Vector();
        Iterator it = source2ValueSetCountHashMap.keySet().iterator();
        while (it.hasNext()) {
			String t = (String) it.next();
			keys.add(t);
		}
		int total_valueset_count = 0;
		int total_concept_count = 0;
		int total_unique_concept_count = 0;

		HashMap countMap = new HashMap();
		keys = new SortUtils().quickSort(keys);
		for (int i=0; i<keys.size(); i++) {
			String key = (String) keys.elementAt(i);
			int valueset_count = ((Integer) source2ValueSetCountHashMap.get(key)).intValue();
			int concept_count = ((Integer) source2ConceptCountHashMap.get(key)).intValue();
			int unique_concept_count = ((Integer) source2UniqueConceptCountHashMap.get(key)).intValue();
            total_valueset_count = total_valueset_count +  valueset_count;
            total_concept_count = total_concept_count +  concept_count;
            total_unique_concept_count = total_unique_concept_count +  unique_concept_count;
			w.add(key + "|" + valueset_count + "|" + concept_count + "|" + unique_concept_count);
		}
		w = new SortUtils().quickSort(w);
		w.add("Total|" + total_valueset_count + "|" + total_concept_count + "|" + total_unique_concept_count);
		return w;
    }

    public HashMap getSource2valueSetCountHashMap(Vector v) {
        HashMap hmap = new HashMap();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String source = (String) u.elementAt(6);
			HashSet hset = new HashSet();
			if (hmap.containsKey(source)) {
				hset = (HashSet) hmap.get(source);
			}
			String code = (String) u.elementAt(4);
			if (!hset.contains(code)) {
				hset.add(code);
			}
			hmap.put(source, hset);
		}
		HashMap source2valueSetCountHashMap = new HashMap();
		Iterator it = hmap.keySet().iterator();
		while (it.hasNext()) {
			String source = (String) it.next();
			HashSet hset = (HashSet) hmap.get(source);
			source2valueSetCountHashMap.put(source, Integer.valueOf(hset.size()));
		}
		hmap.clear();
        return source2valueSetCountHashMap;
	}

    public HashMap getSource2UniqueConceptCountHashMap(Vector v) {
        HashMap hmap = new HashMap();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String source = (String) u.elementAt(6);
			HashSet hset = new HashSet();
			if (hmap.containsKey(source)) {
				hset = (HashSet) hmap.get(source);
			}
			String code = (String) u.elementAt(1);
			if (!hset.contains(code)) {
				hset.add(code);
			}
			hmap.put(source, hset);
		}
		HashMap source2UniqueConceptCountHashMap = new HashMap();
		Iterator it = hmap.keySet().iterator();
		while (it.hasNext()) {
			String source = (String) it.next();
			HashSet hset = (HashSet) hmap.get(source);
			source2UniqueConceptCountHashMap.put(source, Integer.valueOf(hset.size()));
		}
		hmap.clear();
        return source2UniqueConceptCountHashMap;
	}

	public void run_valuse_set() {
		boolean publishedOnly = false;
		Vector v = getValueSetData(named_graph, publishedOnly);

		int number_of_valueses = 0;
		int number_of_sources = 0;
		HashSet sourceHashSet = new HashSet();
		HashSet valuesets = new HashSet();
		HashMap valuesetCountHashMap = new HashMap();
		HashMap source2valueSetHashMap = new HashMap();
		HashMap source2ConceptCountHashMap = new HashMap();
		HashMap source2ValueSetCountHashMap = getSource2valueSetCountHashMap(v);
		HashMap source2UniqueConceptCountHashMap = getSource2UniqueConceptCountHashMap(v);

		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String source = (String) u.elementAt(6);

			String valueset = (String) u.elementAt(3) + " (" +
			                  (String) u.elementAt(4) + ")";

            String code = (String)u.elementAt(1);
            Integer int_obj = Integer.valueOf(0);
            if (source2ConceptCountHashMap.containsKey(source)) {
				int_obj = (Integer) source2ConceptCountHashMap.get(source);
			}
			int count = Integer.valueOf(int_obj);
			int_obj = Integer.valueOf(count+1);
			source2ConceptCountHashMap.put(source, int_obj);

			if (!valuesets.contains(valueset)) {
				valuesets.add(valueset);
			}

			if (!sourceHashSet.contains(source)) {
				sourceHashSet.add(source);
			}

			Vector w = new Vector();
			if (source2valueSetHashMap.containsKey(source)) {
				w = (Vector) source2valueSetHashMap.get(source);
			}
			if (!w.contains(valueset)) {
				w.add(valueset);
			}
			source2valueSetHashMap.put(source, w);

            int_obj = Integer.valueOf(0);
            if (valuesetCountHashMap.containsKey(valueset)) {
				int_obj = (Integer) valuesetCountHashMap.get(valueset);
			}
			count = Integer.valueOf(int_obj);
			int_obj = Integer.valueOf(count+1);
			valuesetCountHashMap.put(valueset, int_obj);
		}

        Vector v1 = createValueSetTableData(valuesetCountHashMap, source2valueSetHashMap);
	    String tableName = addTableNumber("Concepts In Value Set Grouped by Contributing Source");
	    Vector th_vec = new Vector();

	    th_vec.add("Contributing Source");
	    th_vec.add("Value Set");
	    th_vec.add("Count");

	    addTable(tableName, th_vec, v1);

        Vector v2 = createValueSetSourceTableData(
            source2ValueSetCountHashMap,
            source2ConceptCountHashMap,
            source2UniqueConceptCountHashMap);

	    tableName = addTableNumber("Concepts In Value Set Grouped by Contributing Source Summary");
	    th_vec = new Vector();
	    th_vec.add("Contributing Source");
		th_vec.add("Value Set Count");
		th_vec.add("Concept Count");
        th_vec.add("Unique Concept Count");

	    addTable(tableName, th_vec, v2);
	}

	public String addTableNumber(String tableName) {
		table_number++;
		String str = "Table " + table_number + ". " + tableName;
		System.out.println("Generating " + str);
		return str;
	}

    public void generateTableData() {
		System.out.println("generateTableData...");



		Vector v = new Vector();
		String tableName = null;
		Vector th_vec = null;
		addTitle("NCI Thesaurus Statistics");

		System.out.println("generateBranchSizeTableData ...");
        v = generateBranchSizeTableData();
	    tableName = addTableNumber("Branch Size");
	    th_vec = new Vector();
	    th_vec.add("Root");
	    th_vec.add("Node Count");
	    th_vec.add("Concepts Excl. Retired");
	    th_vec.add("Retired");
	    th_vec.add("Total");
	    addTable(tableName, th_vec, v);

		if (checkIfFileExists(RESTRICTION_FILE)) {
			System.out.println("Loading " + RESTRICTION_FILE);
			v = Utils.readFile(RESTRICTION_FILE);
		} else{
			System.out.println("getRoleTargets...");
			v = getRoleTargets(named_graph);
		}

		System.out.println("getRoleTargets... v.size(): " + v.size());
		String firstLine = (String) v.elementAt(0);
		Vector u = StringUtils.parseData(firstLine, '|');
		if (u.size() == 3) {
			v = convert(v);
			Utils.saveToFile(RESTRICTION_FILE, v);
		}

        Vector spec_roots = new Vector();
        spec_roots.add("C2991");
        spec_roots.add("C3262");
        spec_roots.add("C8278");
        spec_roots.add("C4873");
        spec_roots.add("C89328");
        spec_roots.add("C3367");
        spec_roots.add("C16203");
        spec_roots.add("C25218");
        spec_roots.add("C1909");

		spec_roots.add("C62633");
		spec_roots.add("C62634");
		spec_roots.add("C16342");
		spec_roots.add("C192880");
		spec_roots.add("C192883");

        System.out.println("generateSubBranchData ...");
        Vector subbranchdata = generateSubBranchData(spec_roots);
        subbranchdata = new SortUtils().quickSort(subbranchdata);
	    tableName = addTableNumber("Subtrees");
	    th_vec = new Vector();
	    th_vec.add("Root");
	    th_vec.add("Node Count");
	    th_vec.add("Concepts Excl. Retired");
	    th_vec.add("Retired");
	    th_vec.add("Total");
	    addTable(tableName, th_vec, subbranchdata);

        System.out.println("generateValueSetTableData ...");
		Vector ret_vec = generateValueSetTableData();
	    tableName = addTableNumber("Tallies of Concepts_In_Subset Associations");
	    th_vec = new Vector();
	    th_vec.add("Value Set");
	    th_vec.add("Contributing Source");
	    th_vec.add("Count");
	    addTable(tableName, th_vec, ret_vec);

	    run_valuse_set();

        System.out.println("getPropertyValueCounts ...");
	    v = getPropertyValueCounts(named_graph, "Semantic_Type");
	    tableName = addTableNumber("Semantic_Type");
	    th_vec = new Vector();
	    th_vec.add("Semantic Type");
	    th_vec.add("Count");
	    addTable(tableName, th_vec, v);
	    addFooter();

        System.out.println("getPropertyValueCounts (Contributing_Source) ...");
	    v = getPropertyValueCounts(named_graph, "Contributing_Source");
	    tableName = addTableNumber("Contributing_Source");
	    th_vec = new Vector();
	    th_vec.add("Contributing Source");
	    th_vec.add("Count");
	    addTable(tableName, th_vec, v);
	    addFooter();

        System.out.println("getPropertyValueCounts (Concept_Status) ...");
	    v = getPropertyValueCounts(named_graph, "Concept_Status");
	    tableName = addTableNumber("Concept_Status");
	    th_vec = new Vector();
	    th_vec.add("Concept Status");
	    th_vec.add("Count");
	    addTable(tableName, th_vec, v);
	    addFooter();

        System.out.println("getPropertyCounts ...");
        v = getPropertyCounts(named_graph);
	    tableName = addTableNumber("Properties");
	    th_vec = new Vector();
	    th_vec.add("Property");
	    th_vec.add("Count");
	    addTable(tableName, th_vec, v);
	    addFooter();

        System.out.println("getAssociations ...");
        /*
        v = getAssociations(named_graph);
        Vector association_knt_vec = getRelationsipCounts(v);
        */
        Vector association_knt_vec = getAssociationCountData();

	    tableName = addTableNumber("Associations");
	    th_vec = new Vector();
	    th_vec.add("Association");
	    th_vec.add("Count");
	    addTable(tableName, th_vec, association_knt_vec);

		v = Utils.readFile(RESTRICTION_FILE);
        System.out.println("getRelationsipCounts ...");

		Vector role_vec = getRelationsipCounts(v);
		tableName = addTableNumber("Roles");
		th_vec = new Vector();
		th_vec.add("Role");
		th_vec.add("Count");
		addTable(tableName, th_vec, role_vec);

	    addFooter();
	    System.out.println("Done.");
	}


    public Vector getRelationsipCounts(Vector asso_vec) {
		HashMap hmap = new HashMap();
		Vector keys = new Vector();
		for (int i=0; i<asso_vec.size(); i++) {
			String line = (String) asso_vec.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String asso_label = (String) u.elementAt(2);
			Integer int_obj = Integer.valueOf(0);
			if (hmap.containsKey(asso_label)) {
				int_obj = (Integer) hmap.get(asso_label);
			}
			int count = Integer.valueOf(int_obj);
			count++;
			hmap.put(asso_label, Integer.valueOf(count));
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
		Utils.saveToFile("table_data_" + StringUtils.getToday() + ".txt", table_data);
		String outputfile = new HTMLTable().generate(table_data);
		System.out.println(outputfile + " generated.");
	}

	public Vector get_hierarchical_relationships(String named_graph) {
		return owlSPARQLUtils.getHierarchicalRelationships(named_graph);
	}

    public Vector findConceptsWithPropertyMatching(String named_graph, String property_name, String property_value) {
		return owlSPARQLUtils.findConceptsWithPropertyMatching(named_graph, property_name, property_value);
	}

	public Vector getAssociationCountData() {
		Vector v = Utils.readFile("properties.txt");
		Vector w = new Vector();
		int total = 0;
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String prop_code = (String) u.elementAt(1);
			if (prop_code.startsWith("A")) {
				Integer knt_obj = (Integer) propertyCode2CountMap.get(prop_code);
				if (knt_obj == null) {
					//System.out.println("WARNING: " + prop_code + " count not found.");
				} else {
					int knt = knt_obj.intValue();
					w.add((String) u.elementAt(0) + " (" + (String) u.elementAt(1) + ")|" + knt);
					total = total + knt;
				}
			}
		}
		w = new SortUtils().quickSort(w);
		w.add("Total| " + total);
		return w;
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

