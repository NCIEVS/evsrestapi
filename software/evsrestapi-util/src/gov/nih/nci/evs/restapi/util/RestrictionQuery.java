package gov.nih.nci.evs.restapi.util;
import gov.nih.nci.evs.restapi.bean.*;
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
import java.text.*;

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


public class RestrictionQuery {
    private OWLSPARQLUtils owlSPARQLUtils = null;
    String serviceUrl = null;
    String username = null;
    String password = null;
    String named_graph = null;

    public RestrictionQuery(String serviceUrl, String username, String password) {
        this.serviceUrl = serviceUrl;
        this.username = username;
        this.password = password;
        owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, username, password);
    }

    public void set_named_graph(String named_graph) {
		this.named_graph = named_graph;
		this.owlSPARQLUtils.set_named_graph(named_graph);
	}


    public String getPrefixes() {
		return owlSPARQLUtils.getPrefixes();
	}

	Vector executeQuery(String query) {
		return owlSPARQLUtils.executeQuery(query);
	}

/*

(1) owl:Class|owl:equivalentClass|owl:Class|owl:intersectionOf|owl:Class|owl:intersectionOf|owl:Class|owl:intersectionOf|owl:Restriction|owl:onProperty


owl:Class
	owl:equivalentClass
		owl:Class
			owl:intersectionOf
				owl:Class
					owl:intersectionOf
						owl:Class
							owl:intersectionOf
								owl:Restriction
									owl:onProperty
*/




	public String construct_get_roles_1(String named_graph, String code, boolean outbound, boolean codeOnly) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		if (codeOnly) {
			buf.append("SELECT distinct ?x_code ?p_code ?y_code ").append("\n");
		} else {
			buf.append("SELECT distinct ?x_label ?x_code ?p_label ?y_label ?y_code ").append("\n");
		}
		buf.append("{ ").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("	   {").append("\n");
		buf.append("		?x :NHC0 ?x_code .").append("\n");
		buf.append("		?x rdfs:label ?x_label .").append("\n");
		buf.append("		?x owl:equivalentClass ?z1 .").append("\n");
		buf.append("		?z1 a owl:Class .").append("\n");
		buf.append("		?z1 owl:intersectionOf ?z2 .").append("\n");
		buf.append("		?z2 rdf:rest*/rdf:first ?z3 .").append("\n");
		buf.append("		?z3 a owl:Class .").append("\n");
		buf.append("		?z3 owl:intersectionOf ?z4 .").append("\n");
		buf.append("		?z4 rdf:rest*/rdf:first ?z5 .").append("\n");
		buf.append("		?z5 a owl:Class .").append("\n");
		buf.append("		?z5 owl:intersectionOf ?z6 .").append("\n");
		buf.append("		?z6 rdf:rest*/rdf:first ?z7 .").append("\n");
		buf.append("		?z7 a owl:Restriction .").append("\n");
		buf.append("		?z7 owl:onProperty ?p .").append("\n");
		buf.append("		?p rdfs:label ?p_label .").append("\n");
		buf.append("		?p :NHC0 ?p_code .").append("\n");
		buf.append("		?z7 owl:someValuesFrom ?y .").append("\n");
		buf.append("		?y a owl:Class .").append("\n");
		buf.append("		?y rdfs:label ?y_label .").append("\n");
		buf.append("		?y :NHC0 ?y_code .").append("\n");
		if (code != null && outbound) {
		    buf.append("		?x :NHC0 \""+ code +"\"^^xsd:string").append("\n");
		} else {
		    buf.append("		?y :NHC0 \""+ code +"\"^^xsd:string").append("\n");
		}
		buf.append("	    }").append("\n");
		buf.append("    }").append("\n");
		buf.append("} ").append("\n");
		return buf.toString();
	}
/*

(2) owl:Class|owl:equivalentClass|owl:Class|owl:intersectionOf|owl:Class|owl:intersectionOf|owl:Restriction|owl:onProperty


owl:Class
	owl:equivalentClass
		owl:Class
			owl:intersectionOf
				owl:Class
					owl:intersectionOf
						owl:Restriction
							owl:onProperty
*/




	public String construct_get_roles_2(String named_graph, String code, boolean outbound, boolean codeOnly) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		if (codeOnly) {
			buf.append("SELECT distinct ?x_code ?p_code ?y_code ").append("\n");
		} else {
			buf.append("SELECT distinct ?x_label ?x_code ?p_label ?y_label ?y_code ").append("\n");
		}
		buf.append("{ ").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("	   {").append("\n");
		buf.append("		?x :NHC0 ?x_code .").append("\n");
		buf.append("		?x rdfs:label ?x_label .").append("\n");
		buf.append("		?x owl:equivalentClass ?z1 .").append("\n");
		buf.append("		?z1 a owl:Class .").append("\n");
		buf.append("		?z1 owl:intersectionOf ?z2 .").append("\n");
		buf.append("		?z2 rdf:rest*/rdf:first ?z3 .").append("\n");
		buf.append("		?z3 a owl:Class .").append("\n");
		buf.append("		?z3 owl:intersectionOf ?z4 .").append("\n");
		buf.append("		?z4 rdf:rest*/rdf:first ?z5 .").append("\n");
		buf.append("		?z5 a owl:Restriction .").append("\n");
		buf.append("		?z5 owl:onProperty ?p .").append("\n");
		buf.append("		?p rdfs:label ?p_label .").append("\n");
		buf.append("		?p :NHC0 ?p_code .").append("\n");
		buf.append("		?z5 owl:someValuesFrom ?y .").append("\n");
		buf.append("		?y a owl:Class .").append("\n");
		buf.append("		?y rdfs:label ?y_label .").append("\n");
		buf.append("		?y :NHC0 ?y_code .").append("\n");
		if (code != null && outbound) {
		    buf.append("		?x :NHC0 \""+ code +"\"^^xsd:string").append("\n");
		} else {
		    buf.append("		?y :NHC0 \""+ code +"\"^^xsd:string").append("\n");
		}
		buf.append("	    }").append("\n");
		buf.append("    }").append("\n");
		buf.append("} ").append("\n");
		return buf.toString();
	}
/*

(3) owl:Class|owl:equivalentClass|owl:Class|owl:intersectionOf|owl:Class|owl:unionOf|owl:Class|owl:intersectionOf|owl:Restriction|owl:onProperty


owl:Class
	owl:equivalentClass
		owl:Class
			owl:intersectionOf
				owl:Class
					owl:unionOf
						owl:Class
							owl:intersectionOf
								owl:Restriction
									owl:onProperty
*/




	public String construct_get_roles_3(String named_graph, String code, boolean outbound, boolean codeOnly) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		if (codeOnly) {
			buf.append("SELECT distinct ?x_code ?p_code ?y_code ").append("\n");
		} else {
			buf.append("SELECT distinct ?x_label ?x_code ?p_label ?y_label ?y_code ").append("\n");
		}
		buf.append("{ ").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("	   {").append("\n");
		buf.append("		?x :NHC0 ?x_code .").append("\n");
		buf.append("		?x rdfs:label ?x_label .").append("\n");
		buf.append("		?x owl:equivalentClass ?z1 .").append("\n");
		buf.append("		?z1 a owl:Class .").append("\n");
		buf.append("		?z1 owl:intersectionOf ?z2 .").append("\n");
		buf.append("		?z2 rdf:rest*/rdf:first ?z3 .").append("\n");
		buf.append("		?z3 a owl:Class .").append("\n");
		buf.append("		?z3 owl:unionOf ?z4 .").append("\n");
		buf.append("		?z4 rdf:rest*/rdf:first ?z5 .").append("\n");
		buf.append("		?z5 a owl:Class .").append("\n");
		buf.append("		?z5 owl:intersectionOf ?z6 .").append("\n");
		buf.append("		?z6 rdf:rest*/rdf:first ?z7 .").append("\n");
		buf.append("		?z7 a owl:Restriction .").append("\n");
		buf.append("		?z7 owl:onProperty ?p .").append("\n");
		buf.append("		?p rdfs:label ?p_label .").append("\n");
		buf.append("		?p :NHC0 ?p_code .").append("\n");
		buf.append("		?z7 owl:someValuesFrom ?y .").append("\n");
		buf.append("		?y a owl:Class .").append("\n");
		buf.append("		?y rdfs:label ?y_label .").append("\n");
		buf.append("		?y :NHC0 ?y_code .").append("\n");
		if (code != null && outbound) {
		    buf.append("		?x :NHC0 \""+ code +"\"^^xsd:string").append("\n");
		} else {
		    buf.append("		?y :NHC0 \""+ code +"\"^^xsd:string").append("\n");
		}
		buf.append("	    }").append("\n");
		buf.append("    }").append("\n");
		buf.append("} ").append("\n");
		return buf.toString();
	}
/*

(4) owl:Class|owl:equivalentClass|owl:Class|owl:intersectionOf|owl:Class|owl:unionOf|owl:Restriction|owl:onProperty


owl:Class
	owl:equivalentClass
		owl:Class
			owl:intersectionOf
				owl:Class
					owl:unionOf
						owl:Restriction
							owl:onProperty
*/




	public String construct_get_roles_4(String named_graph, String code, boolean outbound, boolean codeOnly) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		if (codeOnly) {
			buf.append("SELECT distinct ?x_code ?p_code ?y_code ").append("\n");
		} else {
			buf.append("SELECT distinct ?x_label ?x_code ?p_label ?y_label ?y_code ").append("\n");
		}
		buf.append("{ ").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("	   {").append("\n");
		buf.append("		?x :NHC0 ?x_code .").append("\n");
		buf.append("		?x rdfs:label ?x_label .").append("\n");
		buf.append("		?x owl:equivalentClass ?z1 .").append("\n");
		buf.append("		?z1 a owl:Class .").append("\n");
		buf.append("		?z1 owl:intersectionOf ?z2 .").append("\n");
		buf.append("		?z2 rdf:rest*/rdf:first ?z3 .").append("\n");
		buf.append("		?z3 a owl:Class .").append("\n");
		buf.append("		?z3 owl:unionOf ?z4 .").append("\n");
		buf.append("		?z4 rdf:rest*/rdf:first ?z5 .").append("\n");
		buf.append("		?z5 a owl:Restriction .").append("\n");
		buf.append("		?z5 owl:onProperty ?p .").append("\n");
		buf.append("		?p rdfs:label ?p_label .").append("\n");
		buf.append("		?p :NHC0 ?p_code .").append("\n");
		buf.append("		?z5 owl:someValuesFrom ?y .").append("\n");
		buf.append("		?y a owl:Class .").append("\n");
		buf.append("		?y rdfs:label ?y_label .").append("\n");
		buf.append("		?y :NHC0 ?y_code .").append("\n");
		if (code != null && outbound) {
		    buf.append("		?x :NHC0 \""+ code +"\"^^xsd:string").append("\n");
		} else {
		    buf.append("		?y :NHC0 \""+ code +"\"^^xsd:string").append("\n");
		}
		buf.append("	    }").append("\n");
		buf.append("    }").append("\n");
		buf.append("} ").append("\n");
		return buf.toString();
	}
/*

(5) owl:Class|owl:equivalentClass|owl:Class|owl:intersectionOf|owl:Restriction|owl:onProperty


owl:Class
	owl:equivalentClass
		owl:Class
			owl:intersectionOf
				owl:Restriction
					owl:onProperty
*/




	public String construct_get_roles_5(String named_graph, String code, boolean outbound, boolean codeOnly) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		if (codeOnly) {
			buf.append("SELECT distinct ?x_code ?p_code ?y_code ").append("\n");
		} else {
			buf.append("SELECT distinct ?x_label ?x_code ?p_label ?y_label ?y_code ").append("\n");
		}
		buf.append("{ ").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("	   {").append("\n");
		buf.append("		?x :NHC0 ?x_code .").append("\n");
		buf.append("		?x rdfs:label ?x_label .").append("\n");
		buf.append("		?x owl:equivalentClass ?z1 .").append("\n");
		buf.append("		?z1 a owl:Class .").append("\n");
		buf.append("		?z1 owl:intersectionOf ?z2 .").append("\n");
		buf.append("		?z2 rdf:rest*/rdf:first ?z3 .").append("\n");
		buf.append("		?z3 a owl:Restriction .").append("\n");
		buf.append("		?z3 owl:onProperty ?p .").append("\n");
		buf.append("		?p rdfs:label ?p_label .").append("\n");
		buf.append("		?p :NHC0 ?p_code .").append("\n");
		buf.append("		?z3 owl:someValuesFrom ?y .").append("\n");
		buf.append("		?y a owl:Class .").append("\n");
		buf.append("		?y rdfs:label ?y_label .").append("\n");
		buf.append("		?y :NHC0 ?y_code .").append("\n");
		if (code != null && outbound) {
		    buf.append("		?x :NHC0 \""+ code +"\"^^xsd:string").append("\n");
		} else {
		    buf.append("		?y :NHC0 \""+ code +"\"^^xsd:string").append("\n");
		}
		buf.append("	    }").append("\n");
		buf.append("    }").append("\n");
		buf.append("} ").append("\n");
		return buf.toString();
	}
/*

(6) owl:Class|rdfs:subClassOf|owl:Class|owl:intersectionOf|owl:Class|owl:intersectionOf|owl:Restriction|owl:onProperty


owl:Class
	rdfs:subClassOf
		owl:Class
			owl:intersectionOf
				owl:Class
					owl:intersectionOf
						owl:Restriction
							owl:onProperty
*/




	public String construct_get_roles_6(String named_graph, String code, boolean outbound, boolean codeOnly) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		if (codeOnly) {
			buf.append("SELECT distinct ?x_code ?p_code ?y_code ").append("\n");
		} else {
			buf.append("SELECT distinct ?x_label ?x_code ?p_label ?y_label ?y_code ").append("\n");
		}
		buf.append("{ ").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("	   {").append("\n");
		buf.append("		?x :NHC0 ?x_code .").append("\n");
		buf.append("		?x rdfs:label ?x_label .").append("\n");
		buf.append("		?x rdfs:subClassOf ?z1 .").append("\n");
		buf.append("		?z1 a owl:Class .").append("\n");
		buf.append("		?z1 owl:intersectionOf ?z2 .").append("\n");
		buf.append("		?z2 rdf:rest*/rdf:first ?z3 .").append("\n");
		buf.append("		?z3 a owl:Class .").append("\n");
		buf.append("		?z3 owl:intersectionOf ?z4 .").append("\n");
		buf.append("		?z4 rdf:rest*/rdf:first ?z5 .").append("\n");
		buf.append("		?z5 a owl:Restriction .").append("\n");
		buf.append("		?z5 owl:onProperty ?p .").append("\n");
		buf.append("		?p rdfs:label ?p_label .").append("\n");
		buf.append("		?p :NHC0 ?p_code .").append("\n");
		buf.append("		?z5 owl:someValuesFrom ?y .").append("\n");
		buf.append("		?y a owl:Class .").append("\n");
		buf.append("		?y rdfs:label ?y_label .").append("\n");
		buf.append("		?y :NHC0 ?y_code .").append("\n");
		if (code != null && outbound) {
		    buf.append("		?x :NHC0 \""+ code +"\"^^xsd:string").append("\n");
		} else {
		    buf.append("		?y :NHC0 \""+ code +"\"^^xsd:string").append("\n");
		}
		buf.append("	    }").append("\n");
		buf.append("    }").append("\n");
		buf.append("} ").append("\n");
		return buf.toString();
	}
/*

(7) owl:Class|rdfs:subClassOf|owl:Class|owl:intersectionOf|owl:Restriction|owl:onProperty


owl:Class
	rdfs:subClassOf
		owl:Class
			owl:intersectionOf
				owl:Restriction
					owl:onProperty
*/




	public String construct_get_roles_7(String named_graph, String code, boolean outbound, boolean codeOnly) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		if (codeOnly) {
			buf.append("SELECT distinct ?x_code ?p_code ?y_code ").append("\n");
		} else {
			buf.append("SELECT distinct ?x_label ?x_code ?p_label ?y_label ?y_code ").append("\n");
		}
		buf.append("{ ").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("	   {").append("\n");
		buf.append("		?x :NHC0 ?x_code .").append("\n");
		buf.append("		?x rdfs:label ?x_label .").append("\n");
		buf.append("		?x rdfs:subClassOf ?z1 .").append("\n");
		buf.append("		?z1 a owl:Class .").append("\n");
		buf.append("		?z1 owl:intersectionOf ?z2 .").append("\n");
		buf.append("		?z2 rdf:rest*/rdf:first ?z3 .").append("\n");
		buf.append("		?z3 a owl:Restriction .").append("\n");
		buf.append("		?z3 owl:onProperty ?p .").append("\n");
		buf.append("		?p rdfs:label ?p_label .").append("\n");
		buf.append("		?p :NHC0 ?p_code .").append("\n");
		buf.append("		?z3 owl:someValuesFrom ?y .").append("\n");
		buf.append("		?y a owl:Class .").append("\n");
		buf.append("		?y rdfs:label ?y_label .").append("\n");
		buf.append("		?y :NHC0 ?y_code .").append("\n");
		if (code != null && outbound) {
		    buf.append("		?x :NHC0 \""+ code +"\"^^xsd:string").append("\n");
		} else {
		    buf.append("		?y :NHC0 \""+ code +"\"^^xsd:string").append("\n");
		}
		buf.append("	    }").append("\n");
		buf.append("    }").append("\n");
		buf.append("} ").append("\n");
		return buf.toString();
	}
/*

(8) owl:Class|rdfs:subClassOf|owl:Class|owl:unionOf|owl:Class|owl:intersectionOf|owl:Restriction|owl:onProperty


owl:Class
	rdfs:subClassOf
		owl:Class
			owl:unionOf
				owl:Class
					owl:intersectionOf
						owl:Restriction
							owl:onProperty
*/




	public String construct_get_roles_8(String named_graph, String code, boolean outbound, boolean codeOnly) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		if (codeOnly) {
			buf.append("SELECT distinct ?x_code ?p_code ?y_code ").append("\n");
		} else {
			buf.append("SELECT distinct ?x_label ?x_code ?p_label ?y_label ?y_code ").append("\n");
		}
		buf.append("{ ").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("	   {").append("\n");
		buf.append("		?x :NHC0 ?x_code .").append("\n");
		buf.append("		?x rdfs:label ?x_label .").append("\n");
		buf.append("		?x rdfs:subClassOf ?z1 .").append("\n");
		buf.append("		?z1 a owl:Class .").append("\n");
		buf.append("		?z1 owl:unionOf ?z2 .").append("\n");
		buf.append("		?z2 rdf:rest*/rdf:first ?z3 .").append("\n");
		buf.append("		?z3 a owl:Class .").append("\n");
		buf.append("		?z3 owl:intersectionOf ?z4 .").append("\n");
		buf.append("		?z4 rdf:rest*/rdf:first ?z5 .").append("\n");
		buf.append("		?z5 a owl:Restriction .").append("\n");
		buf.append("		?z5 owl:onProperty ?p .").append("\n");
		buf.append("		?p rdfs:label ?p_label .").append("\n");
		buf.append("		?p :NHC0 ?p_code .").append("\n");
		buf.append("		?z5 owl:someValuesFrom ?y .").append("\n");
		buf.append("		?y a owl:Class .").append("\n");
		buf.append("		?y rdfs:label ?y_label .").append("\n");
		buf.append("		?y :NHC0 ?y_code .").append("\n");
		if (code != null && outbound) {
		    buf.append("		?x :NHC0 \""+ code +"\"^^xsd:string").append("\n");
		} else {
		    buf.append("		?y :NHC0 \""+ code +"\"^^xsd:string").append("\n");
		}
		buf.append("	    }").append("\n");
		buf.append("    }").append("\n");
		buf.append("} ").append("\n");
		return buf.toString();
	}
/*

(9) owl:Class|rdfs:subClassOf|owl:Class|owl:unionOf|owl:Restriction|owl:onProperty


owl:Class
	rdfs:subClassOf
		owl:Class
			owl:unionOf
				owl:Restriction
					owl:onProperty
*/




	public String construct_get_roles_9(String named_graph, String code, boolean outbound, boolean codeOnly) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		if (codeOnly) {
			buf.append("SELECT distinct ?x_code ?p_code ?y_code ").append("\n");
		} else {
			buf.append("SELECT distinct ?x_label ?x_code ?p_label ?y_label ?y_code ").append("\n");
		}
		buf.append("{ ").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("	   {").append("\n");
		buf.append("		?x :NHC0 ?x_code .").append("\n");
		buf.append("		?x rdfs:label ?x_label .").append("\n");
		buf.append("		?x rdfs:subClassOf ?z1 .").append("\n");
		buf.append("		?z1 a owl:Class .").append("\n");
		buf.append("		?z1 owl:unionOf ?z2 .").append("\n");
		buf.append("		?z2 rdf:rest*/rdf:first ?z3 .").append("\n");
		buf.append("		?z3 a owl:Restriction .").append("\n");
		buf.append("		?z3 owl:onProperty ?p .").append("\n");
		buf.append("		?p rdfs:label ?p_label .").append("\n");
		buf.append("		?p :NHC0 ?p_code .").append("\n");
		buf.append("		?z3 owl:someValuesFrom ?y .").append("\n");
		buf.append("		?y a owl:Class .").append("\n");
		buf.append("		?y rdfs:label ?y_label .").append("\n");
		buf.append("		?y :NHC0 ?y_code .").append("\n");
		if (code != null && outbound) {
		    buf.append("		?x :NHC0 \""+ code +"\"^^xsd:string").append("\n");
		} else {
		    buf.append("		?y :NHC0 \""+ code +"\"^^xsd:string").append("\n");
		}
		buf.append("	    }").append("\n");
		buf.append("    }").append("\n");
		buf.append("} ").append("\n");
		return buf.toString();
	}
/*

(10) owl:Class|rdfs:subClassOf|owl:Restriction|owl:onProperty


owl:Class
	rdfs:subClassOf
		owl:Restriction
			owl:onProperty
*/




	public String construct_get_roles_10(String named_graph, String code, boolean outbound, boolean codeOnly) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		if (codeOnly) {
			buf.append("SELECT distinct ?x_code ?p_code ?y_code ").append("\n");
		} else {
			buf.append("SELECT distinct ?x_label ?x_code ?p_label ?y_label ?y_code ").append("\n");
		}
		buf.append("{ ").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("	   {").append("\n");
		buf.append("		?x :NHC0 ?x_code .").append("\n");
		buf.append("		?x rdfs:label ?x_label .").append("\n");
		buf.append("		?x rdfs:subClassOf ?z1 .").append("\n");
		buf.append("		?z1 a owl:Restriction .").append("\n");
		buf.append("		?z1 owl:onProperty ?p .").append("\n");
		buf.append("		?p rdfs:label ?p_label .").append("\n");
		buf.append("		?p :NHC0 ?p_code .").append("\n");
		buf.append("		?z1 owl:someValuesFrom ?y .").append("\n");
		buf.append("		?y a owl:Class .").append("\n");
		buf.append("		?y rdfs:label ?y_label .").append("\n");
		buf.append("		?y :NHC0 ?y_code .").append("\n");
		if (code != null && outbound) {
		    buf.append("		?x :NHC0 \""+ code +"\"^^xsd:string").append("\n");
		} else {
		    buf.append("		?y :NHC0 \""+ code +"\"^^xsd:string").append("\n");
		}
		buf.append("	    }").append("\n");
		buf.append("    }").append("\n");
		buf.append("} ").append("\n");
		return buf.toString();
	}

	public static void main(String[] args) {
		String serviceUrl = args[0];
		String namedGraph = args[1];
		System.out.println("serviceUrl: " + serviceUrl);
		System.out.println("namedGraph: " + namedGraph);
		RestrictionQuery restrictionQuery = new RestrictionQuery(serviceUrl, null, null);
		restrictionQuery.set_named_graph(namedGraph);

		long ms = System.currentTimeMillis();
		String query = restrictionQuery.construct_get_roles_10(namedGraph, "C150023", true, false);
		System.out.println(query);

		Vector v = restrictionQuery.executeQuery(query);
		if (v != null) {
			v = new ParserUtils().getResponseValues(v);
			v = new SortUtils().quickSort(v);
		}
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
		Utils.saveToFile("role_query_10._" + StringUtils.getToday() + ".txt", v);
	}

}
