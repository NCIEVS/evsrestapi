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


public class ReportGenerator {
    JSONUtils jsonUtils = null;
    HTTPUtils httpUtils = null;
    String named_graph = null;
    String prefixes = null;
    String serviceUrl = null;
    String restURL = null;
    ParserUtils parser = new ParserUtils();
    HashMap nameVersion2NamedGraphMap = null;
    HashMap ontologyUri2LabelMap = null;
    String version = null;
    String username = null;
    String password = null;
    OWLSPARQLUtils owlSPARQLUtils = null;
    AxiomUtils axiomUtils = null;
    ReportGenerationHelper helper = null;

    HashMap code2LabelMap = null;
    HashMap vsMap = null;

	public static int TYPE_PARENT = 1;
	public static int TYPE_CHILDREN = 2;
	public static int TYPE_PROPERTY = 3;
	public static int TYPE_ASSOCIATION = 4;
	public static int TYPE_ROLE = 5;
	public static int TYPE_ASSOCIATED_CONCEPT_PROPERTY = 6;

    public ReportGenerator(String serviceUrl, String named_graph, String username, String password) {
		this.serviceUrl = serviceUrl;
    	this.named_graph = named_graph;
    	this.username = username;
    	this.password = password;
        this.owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, username, password);
        this.owlSPARQLUtils.set_named_graph(named_graph);
        this.axiomUtils = new AxiomUtils(serviceUrl, username, password);
        this.helper = new ReportGenerationHelper(serviceUrl, named_graph, username, password);
        createVSMap();
    }

    public HashMap getvsMap() {
		return vsMap;
	}

	public Vector getVS(String subsetCode) {
		return (Vector) vsMap.get(subsetCode);
	}

	public String getLabel(String code) {
		return (String) code2LabelMap.get(code);
	}

    private void createVSMap() {
		long ms = System.currentTimeMillis();
		code2LabelMap = new HashMap();
		vsMap = new HashMap();
        Vector v = getValueset(this.named_graph);
        for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String yesOrNo = (String) u.elementAt(5);
			if (yesOrNo.compareTo("Yes") == 0) {
				code2LabelMap.put((String) u.elementAt(1), (String) u.elementAt(0));
				code2LabelMap.put((String) u.elementAt(4), (String) u.elementAt(3));
				Vector w = new Vector();
				String code = (String) u.elementAt(1);
				String subsetCode = (String) u.elementAt(4);
				if (vsMap.containsKey(subsetCode)) {
					w = (Vector) vsMap.get(subsetCode);
				}
				if (!w.contains(code)) {
					w.add(code);
				}
				vsMap.put(subsetCode, w);
			}
		}
        v.clear();
        System.out.println("Total createVSMap run time (ms): " + (System.currentTimeMillis() - ms));
	}

    public OWLSPARQLUtils getOWLSPARQLUtils() {
		return this.owlSPARQLUtils;
	}

	public String construct_get_valueset(String named_graph, String code, int type, String data) {
		String prefixes = owlSPARQLUtils.getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);

		if (type == 0) {
		    buf.append("select distinct ?x_label ?x_code ?p1_label ?y_label ?y_code ?p2_value ").append("\n");
		} else if (type == TYPE_PARENT) {
             buf.append("select distinct ?x_label ?x_code ?p1_label ?y_label ?y_code ?p2_value ?z_label ?z_code").append("\n");
		} else if (type == TYPE_CHILDREN) {
             buf.append("select distinct ?x_label ?x_code ?p1_label ?y_label ?y_code ?p2_value ?z_label ?z_code").append("\n");
		} else if (type == TYPE_PROPERTY) {
             buf.append("select distinct ?x_label ?x_code ?p1_label ?y_label ?y_code ?p2_value ?p_label ?z").append("\n");
		} else if (type == TYPE_ASSOCIATION) {
             buf.append("select distinct ?x_label ?x_code ?p1_label ?y_label ?y_code ?p2_value ?p_label ?z_label ?z_code").append("\n");
		} else if (type == TYPE_ROLE) {
             buf.append("select distinct ?x_label ?x_code ?p1_label ?y_label ?y_code ?p2_value ?p_label ?z_label ?z_code").append("\n");
		} else if (type == TYPE_ASSOCIATED_CONCEPT_PROPERTY) {
			 Vector u = StringUtils.parseData(data, '|');
			 String prop_code = (String) u.elementAt(0);
			 if (u.size() == 2) {
             	 buf.append("select distinct ?x_label ?x_code ?p1_label ?y_label ?y_code ?p2_value ?p_label ?z_label ?z_code ?p3_value").append("\n");
			 } else {
				 buf.append("select distinct ?x_label ?x_code ?p1_label ?y_label ?y_code ?p2_value ?p_label ?z_label ?z_code ?z1_axiom ?z1_target").append("\n");
			 }
		}
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

		if (type == TYPE_PARENT) {
			buf.append("            	?z a owl:Class .").append("\n");
			buf.append("            	?z :NHC0 ?z_code .").append("\n");
			buf.append("            	?z rdfs:label ?z_label .").append("\n");
			buf.append("      ?x (rdfs:subClassOf|(owl:equivalentClass/owl:intersectionOf/rdf:rest*/rdf:first)) ?z . ").append("\n");
		} else if (type == TYPE_PARENT) {
			buf.append("            	?z a owl:Class .").append("\n");
			buf.append("            	?z :NHC0 ?z_code .").append("\n");
			buf.append("            	?z rdfs:label ?z_label .").append("\n");
			buf.append("      ?z (rdfs:subClassOf|(owl:equivalentClass/owl:intersectionOf/rdf:rest*/rdf:first)) ?x . ").append("\n");
	    } else if (type == TYPE_PROPERTY) {
			buf.append("            ?p a owl:AnnotationProperty .").append("\n");
			buf.append("            ?p rdfs:label ?p_label .").append("\n");
			buf.append("            ?x ?p ?z .").append("\n");
			if (data != null) {
				buf.append("            ?p rdfs:label \"" + data + "\"^^xsd:string .").append("\n");
			}
	    } else if (type == TYPE_ASSOCIATION) {
			buf.append("	    ?x ?p ?z .").append("\n");
			buf.append("	    ?z a owl:Class .").append("\n");
			buf.append("	    ?z rdfs:label ?z_label .").append("\n");
			buf.append("	    ?z :NHC0 ?z_code .").append("\n");
			buf.append("	    ?p rdfs:label ?p_label .").append("\n");
			buf.append("	    ?p :NHC0 ?p_code .").append("\n");

        } else if (type == TYPE_ROLE) {
			buf.append("        ?p :NHC0 ?p_code .").append("\n");
			buf.append("        ?p rdfs:label ?p_label .").append("\n");
			buf.append("        ?x (rdfs:subClassOf|owl:equivalentClass|owl:unionOf/rdf:rest*/rdf:first|owl:intersectionOf/rdf:rest*/rdf:first)* ?rs .  ").append("\n");
			buf.append("        ?rs a owl:Restriction .").append("\n");
			buf.append("        ?rs owl:onProperty ?p .").append("\n");
			buf.append("        ?rs owl:someValuesFrom ?z .").append("\n");
			buf.append("	    ?z rdfs:label ?z_label .").append("\n");
			buf.append("	    ?z :NHC0 ?z_code .").append("\n");

	    } else if (type == TYPE_ASSOCIATED_CONCEPT_PROPERTY) {
			Vector u = StringUtils.parseData(data, '|');
			String a_code = (String) u.elementAt(0);
            buf.append("\n");
			buf.append("	    	?x ?p ?z .").append("\n");
			buf.append("	    	?z a owl:Class .").append("\n");

			buf.append("	    	?z rdfs:label ?z_label .").append("\n");
			buf.append("	    	?z :NHC0 ?z_code .").append("\n");

			buf.append("	    	?p rdfs:label ?p_label .").append("\n");
			buf.append("	    	?p :NHC0 \"" + a_code + "\"^^xsd:string .").append("\n");

			String prop_code = (String) u.elementAt(1);
			if (u.size() == 2) {
				buf.append("	    ?z ?p3 ?p3_value .").append("\n");
				buf.append("	    ?p3_code :NHC0 \"" + prop_code + "\"^^xsd:string .").append("\n");
			} else if (u.size() > 2) {
				//associated concept (?z) axiom
				buf.append("            	?z1_axiom a owl:Axiom .").append("\n");
				buf.append("            	?z1_axiom owl:annotatedSource ?z .").append("\n");
				buf.append("            	?z1_axiom owl:annotatedProperty ?p3 .").append("\n");
				buf.append("            	?p3_code :NHC0 \"" + prop_code + "\"^^xsd:string .").append("\n");
				buf.append("            	?z1_axiom owl:annotatedTarget ?z1_target .").append("\n");
				// match qualifiers
                for (int j=2; j<u.size(); j++) {
					int k=j-1;
					String qual_str = (String) u.elementAt(j);
					Vector u2 = StringUtils.parseData(qual_str, '$');
					String qual_code = (String) u2.elementAt(0);
					String qual_val = (String) u2.elementAt(1);
					buf.append("            	?q" + k + " :NHC0 \"" + qual_code + "\"^^xsd:string .").append("\n");
					buf.append("            	?z1_axiom ?q" + k + " \"" + qual_val + "\"^^xsd:string ." ).append("\n");
				}
			}
		}

		buf.append("}").append("\n");
		return buf.toString();
	}

	public String construct_axiom_query(String named_graph, String subset_code, String propertyName) {
		return construct_axiom_query(named_graph, subset_code, null, null, propertyName);
	}

	public String construct_axiom_query(String named_graph, String subset_code, String code, String associationCode, String propertyName) {
		String prefixes = owlSPARQLUtils.getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("").append("\n");
		buf.append("SELECT ?z_axiom ?z0_label ?z0_code ?p_label ?z_target ?y_label ?z").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    { ").append("\n");

		buf.append("            ?x :NHC0 ?x_code .").append("\n");
		if (code != null) {
			buf.append("            ?x :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
		}
		buf.append("            ?x rdfs:label ?x_label .").append("\n");

		buf.append("            ?x ?p0 ?z0 .").append("\n");
		buf.append("            ?z0 a owl:Class .").append("\n");

		buf.append("            ?z0 rdfs:label ?z0_label .").append("\n");
		buf.append("            ?z0 :NHC0 ?z0_code .").append("\n");

		buf.append("            ?p0 rdfs:label ?p0_label .").append("\n");

		if (associationCode != null) {
			buf.append("            ?p0 :NHC0 \"" + associationCode + "\"^^xsd:string .").append("\n\n");
	    }

        if (subset_code != null) {
			buf.append("            ?y0 a owl:Class .").append("\n");
			buf.append("            ?y0 :NHC0 ?y0_code .").append("\n");
			buf.append("            ?y0 :NHC0 \"" + subset_code + "\"^^xsd:string .").append("\n");
			buf.append("").append("\n");
			buf.append("            ?y0 rdfs:label ?y0_label .").append("\n");
			buf.append("            ?p1 rdfs:label ?p1_label .").append("\n");
			buf.append("            ?p1 rdfs:label \"Concept_In_Subset\"^^xsd:string .  ").append("\n");
			buf.append("            ?x ?p1 ?y0 .").append("\n");
			buf.append("").append("\n");
			buf.append("            ?y0 ?p2 ?p2_value .").append("\n");
			buf.append("            ?p2 rdfs:label ?p2_label .").append("\n");
			buf.append("            ?p2 rdfs:label \"Publish_Value_Set\"^^xsd:string .").append("\n\n");
	    }

		buf.append("            ?z_axiom a owl:Axiom .").append("\n");
		buf.append("            ?z_axiom owl:annotatedSource ?z0 .").append("\n");
		buf.append("            ?z_axiom owl:annotatedProperty ?p .").append("\n");
		buf.append("            ?p rdfs:label ?p_label .").append("\n");

		if (propertyName != null) {
			buf.append("            ?p rdfs:label \"" + propertyName + "\"^^xsd:string .").append("\n");
		}

		buf.append("            ?z_axiom owl:annotatedTarget ?z_target .").append("\n");
		buf.append("            ?z_axiom ?y ?z . ").append("\n");
		buf.append("            ?y rdfs:label ?y_label .").append("\n");

		buf.append("     }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}


	public Vector getValueset(String named_graph, String code, int type, String data) {
		String query = construct_get_valueset(named_graph, code, type, data);
		Vector v = owlSPARQLUtils.executeQuery(query);
		if (v == null) return null;
		if (v.size() == 0) return v;
		v = new ParserUtils().getResponseValues(v);
		return new SortUtils().quickSort(v);
	}

	public Vector getAxioms(String named_graph, String code, String propertyName) {
		String query = construct_axiom_query(named_graph, code, propertyName);
		Vector v = owlSPARQLUtils.executeQuery(query);
		if (v != null) {
			v = new ParserUtils().getResponseValues(v);
		}
		return v;
	}

	public Vector getAxioms(String named_graph, String subset_code, String code, String associationCode, String propertyName) {
		String query = construct_axiom_query(named_graph, subset_code, code, associationCode, propertyName);
		Vector v = owlSPARQLUtils.executeQuery(query);
		if (v != null) {
			v = new ParserUtils().getResponseValues(v);
		}
		return v;
	}

	public List getSynonyms(String named_graph, String subset_code, String code, String associationCode) {
		Vector axiom_data = getAxioms(named_graph, subset_code, code, associationCode, "FULL_SYN");
		return axiomUtils.getSynonyms(axiom_data);
	}

	public List getSynonyms(String named_graph, String subset_code) {
		Vector axiom_data = getAxioms(named_graph, subset_code, "FULL_SYN");
		return axiomUtils.getSynonyms(axiom_data);
	}

	public List getDefinitions(String named_graph, String subset_code) {
		Vector axiom_data = getAxioms(named_graph, subset_code, "DEFINITION");
		return axiomUtils.getDefinitions(axiom_data);
	}


	public String construct_get_valueset(String named_graph) {
        String prefixes = owlSPARQLUtils.getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        buf.append("select distinct ?x_label ?x_code ?p1_label ?y_label ?y_code ?p2_value").append("\n");
        buf.append("from <" + named_graph + ">").append("\n");
        buf.append("where  { ").append("\n");
        buf.append("                    ?x a owl:Class .").append("\n");
        buf.append("                    ?x :NHC0 ?x_code .").append("\n");
        buf.append("                    ?x rdfs:label ?x_label .").append("\n");
        buf.append("                    ").append("\n");
        buf.append("                    ?y a owl:Class .").append("\n");
        buf.append("                    ?y :NHC0 ?y_code .").append("\n");
        buf.append("                    ?y rdfs:label ?y_label .").append("\n");
        buf.append("                    ").append("\n");
        buf.append("                    ?x ?p1 ?y .").append("\n");
        buf.append("                ?p1 rdfs:label ?p1_label .").append("\n");
        buf.append("                ?p1 rdfs:label \"Concept_In_Subset\"^^xsd:string .").append("\n");
        buf.append("").append("\n");
        buf.append("                ?y ?p2 ?p2_value .").append("\n");
        buf.append("                ?p2 rdfs:label ?p2_label .").append("\n");
        buf.append("                ?p2 rdfs:label \"Publish_Value_Set\"^^xsd:string .").append("\n");
        buf.append("}").append("\n");
        return buf.toString();
	}


	public Vector getValueset(String named_graph) {
        String query = construct_get_valueset(named_graph);
        Vector v = owlSPARQLUtils.executeQuery(query);
        if (v == null) return null;
        if (v.size() == 0) return v;
        v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
	}

	public Vector resolveValueSet(String subsetCode) {
		Stack stack = new Stack();
		stack.push(subsetCode);
		Vector w = new Vector();
		while (!stack.isEmpty()) {
			String code = (String) stack.pop();
			Vector v = (Vector) getVS(code);
			if (v != null) {
				w.addAll(v);
			}
			Vector sub_vec = helper.getSubclassesByCode(named_graph, code);
			if (sub_vec != null) {
				for (int k=0; k<sub_vec.size(); k++) {
					String line1 = (String) sub_vec.elementAt(k);
					Vector u2 = StringUtils.parseData(line1, '|');
					String sub = (String) u2.elementAt(1);
					stack.push(sub);
				}
			}
		}
		Vector v = new Vector();
		HashSet hset = new HashSet();
		for (int i=0; i<w.size(); i++) {
			String code = (String) w.elementAt(i);
			if (!hset.contains(code)) {
				hset.add(code);
				v.add(getLabel(code) + "|" + code);
			}
		}
		w.clear();
		return new SortUtils().quickSort(v);
	}

    public static void main(String[] args) {
		long ms = System.currentTimeMillis();
		String serviceUrl = args[0];
		String named_graph = args[1];
		String username = args[2];
		String password = args[3];
		Vector w = new Vector();
        ReportGenerator test = new ReportGenerator(serviceUrl, named_graph, username, password);

        //ICDC Terminology (Code C165451)
        String code = "C165451";
		Vector v = test.resolveValueSet(code);
		Utils.dumpVector(code, v);

		//SPL Color Terminology (Code C54453)

        int type = TYPE_PARENT;
        w = test.getValueset(named_graph, code, type, null);
        Utils.dumpVector(code, w);

        type = TYPE_CHILDREN;
        w = test.getValueset(named_graph, code, type, null);
        Utils.dumpVector(code, w);

        type = TYPE_PROPERTY;
        w = test.getValueset(named_graph, code, type, "Preferred_Name");
        Utils.dumpVector(code, w);

        w = test.getValueset(named_graph, code, type, "Semantic_Type");
        Utils.dumpVector(code, w);

        type = TYPE_ASSOCIATION;
        w = test.getValueset(named_graph, code, type, null);
        Utils.dumpVector(code, w);

        type = TYPE_ROLE;
        w = test.getValueset(named_graph, code, type, null);
        Utils.dumpVector(code, w);

        List list = test.getSynonyms(named_graph, code);
        for (int i=0; i<list.size(); i++) {
			Synonym syn = (Synonym) list.get(i);
			System.out.println(syn.toJson());
		}

        list = test.getDefinitions(named_graph, code);
        for (int i=0; i<list.size(); i++) {
			Definition def = (Definition) list.get(i);
			System.out.println(def.toJson());
		}

        String subset_code = null;
        code = "C168823";
        String associationCode = "A23";
        String propertyName = "FULL_SYN";

        list = test.getSynonyms(named_graph, subset_code, code, associationCode);
        for (int i=0; i<list.size(); i++) {
			Synonym syn = (Synonym) list.get(i);
			System.out.println(syn.toJson());
		}

		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));

    }
}
