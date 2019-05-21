package gov.nih.nci.evs.restapi.util;

import gov.nih.nci.evs.restapi.ui.*;
//import gov.nih.nci.evs.restapi.util.*;
import gov.nih.nci.evs.restapi.bean.*;

import java.io.*;
import java.util.*;
import java.text.*;
import java.sql.*;
import java.net.*;

import org.apache.commons.lang.*;
//import org.apache.log4j.*;
import java.util.Map;
import java.util.Map.Entry;

/**
 * <!-- LICENSE_TEXT_START -->
 * Copyright 2008,2009 NGIT. This software was developed in conjunction
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
 *      "This product includes software developed by NGIT and the National
 *      Cancer Institute."   If no such end-user documentation is to be
 *      included, this acknowledgment shall appear in the software itself,
 *      wherever such third-party acknowledgments normally appear.
 *   3. The names "The National Cancer Institute", "NCI" and "NGIT" must
 *      not be used to endorse or promote products derived from this software.
 *   4. This license does not authorize the incorporation of this software
 *      into any third party proprietary programs. This license does not
 *      authorize the recipient to use any trademarks owned by either NCI
 *      or NGIT
 *   5. THIS SOFTWARE IS PROVIDED "AS IS," AND ANY EXPRESSED OR IMPLIED
 *      WARRANTIES, (INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *      OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE) ARE
 *      DISCLAIMED. IN NO EVENT SHALL THE NATIONAL CANCER INSTITUTE,
 *      NGIT, OR THEIR AFFILIATES BE LIABLE FOR ANY DIRECT, INDIRECT,
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
 *          Modification history Initial implementation kim.ong@ngc.com
 *
 */

 public class MetathesaurusTreeUtils {

	 public static String EXACT_MATCH = "exactMatch";
	 public static String STARTS_WITH = "startsWith";
	 public static String ENDS_WITH = "endsWith";
	 public static String CONTAINS = "contains";

	 public static String CODING_SCHEME_NAME = "NCI MetaThesaurus";

	 /** The NC i_ source. */
	 private static String NCI_SOURCE = "NCI";

	 /** The NC i_ root. */
	 private static String NCI_ROOT = "C1140168";

	 private static String VIRTUAL_GRAPH_NAME = "metathesaurus";

     int size = 5;
     UICodeGenerator generator = null;
	 String serviceUrl = null;
     HTTPUtils httpUtils = null;
     JSONUtils jsonUtils = null;

     static String PAR = "PAR";
     static String CHD = "CHD";

     static String ROOT_SAB = "SRC";
     static String DEFAULT_LAT = "ENG";

     static String YES = "Y";
     static String TS_P = "P";
     static String VPT = "VPT";
     static String NCI = "NCI";


	 static final int TRAVERSE_UP = 1;
	 static final int TRAVERSE_DOWN = 0;

     private String[] RELATIONSHIP_TYPE = new String[] {"PAR", "CHD", "RB", "RN", "SIB"};

     HashMap hierarchyRoots = null;
     String bookmarkUrl = "https://localhost:8080/sparqlncim/ConceptReport.jsp?dictionary=NCI Metathesaurus&code=";
     String applicationName = "sparqlncim";
     MetathesaurusDataUtils metathesaurusDataUtils = null;

	 public MetathesaurusTreeUtils(String serviceUrl) {
		this.serviceUrl = verifyServiceUrl(serviceUrl);
        this.httpUtils = new HTTPUtils(serviceUrl, null, null);
        this.jsonUtils = new JSONUtils();
        this.metathesaurusDataUtils = new MetathesaurusDataUtils(serviceUrl);
        hierarchyRoots = createSAB2HierarchyRootHashMap();
        Iterator it = hierarchyRoots.keySet().iterator();
	 }

	 public void setApplicationName(String applicationName) {
		 this.applicationName = applicationName;
		 generator.set_application_name(applicationName);
	 }

	 public void setBookmarkUrl(String bookmarkUrl) {
		 this.bookmarkUrl = bookmarkUrl;
		 generator.set_bookmark_url(bookmarkUrl);
	 }

	 public String loadQuery(String query_file) {
		 try {
			 String query = httpUtils.loadQuery(query_file, false);
			 return query;
		 } catch (Exception ex) {
			 ex.printStackTrace();
			 return null;
		 }
	 }

     public Vector execute(String query_file) {
		 try {
			 String query = httpUtils.loadQuery(query_file, false);
			 return executeQuery(query);
		 } catch (Exception ex) {
			 ex.printStackTrace();
			 return null;
		 }
	 }

	 public HashMap getHierarchyRoots() {
		 if (hierarchyRoots == null) {
			 hierarchyRoots = createSAB2HierarchyRootHashMap();
		 }
		 return hierarchyRoots;
	 }

	 public Vector getSourcesWithSupportedHierarchy() {
		 HashMap hmap = getHierarchyRoots();
		 Vector keys = new Vector();
		 Iterator it = hmap.keySet().iterator();
		 while (it.hasNext()) {
			 String sab = (String) it.next();
			 keys.add(sab);
		 }
		 keys = new gov.nih.nci.evs.restapi.util.SortUtils().quickSort(keys);
		 return keys;
	 }


	public String verifyServiceUrl(String serviceUrl) {
		if (serviceUrl.indexOf("?query=?query=") != -1) {
			int n = serviceUrl.lastIndexOf("?");
			serviceUrl = serviceUrl.substring(0, n);
		} else if (serviceUrl.indexOf("?") == -1) {
			serviceUrl = serviceUrl + "?query=";
		}
		return serviceUrl;
	}

    public Vector executeQuery(String query) {
        Vector v = null;
        try {
			query = httpUtils.encode(query);
            String json = httpUtils.executeQuery(query);
			v = jsonUtils.parseJSON(json);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return v;
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	public String get_hierarchy_roots(String sab) {
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX MRCONSO: <http://ncicb.nci.nih.gov/" + VIRTUAL_GRAPH_NAME + "/mrconso/>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("SELECT distinct ?cui ?sab ?code {").append("\n");
		buf.append("   GRAPH <virtual://" + VIRTUAL_GRAPH_NAME + "> {").append("\n");
		buf.append("?s MRCONSO:cui ?cui .").append("\n");
		buf.append("?s MRCONSO:sab ?sab .").append("\n");
		buf.append("?s MRCONSO:code ?code .").append("\n");
		buf.append("?s MRCONSO:sab \"SRC\"^^xsd:string .").append("\n");
		buf.append("?s MRCONSO:code \"V-" + sab + "\"^^xsd:string .").append("\n");
		buf.append("}").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

    public Vector getHierarchyRoots(String sab) {
        Vector v = new Vector();
        String query = get_hierarchy_roots(sab);
        v = executeQuery(query);
		if (v == null || v.size() == 0) return null;
		v = new ParserUtils().getResponseValues(v);
        return v;
	}

	public static String construct_mrhier_query(String cui) {
		StringBuffer buf = new StringBuffer();
		//buf.append("PREFIX sm: <tag:stardog:api:mapping:>").append("\n");
		buf.append("PREFIX MRHIER: <http://ncicb.nci.nih.gov/" + VIRTUAL_GRAPH_NAME + "/mrhier/>").append("\n");
		//buf.append("PREFIX dc:<http://purl.org/dc/elements/1.1/>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("SELECT ?aui ?cui ?cvf ?cxn ?hcd ?paui ?ptr ?rela ?sab {").append("\n");
		buf.append("   GRAPH <virtual://" + VIRTUAL_GRAPH_NAME + "> {").append("\n");
		//buf.append("      ?s a :MRHIER .").append("\n");
		buf.append("      ?s MRHIER:aui ?aui .").append("\n");
		buf.append("      ?s MRHIER:cui ?cui .").append("\n");
		buf.append("      ?s MRHIER:cui \"" + cui + "\"^^xsd:string .").append("\n");
		buf.append("      ?s MRHIER:cvf ?cvf .").append("\n");
		buf.append("      ?s MRHIER:cxn ?cxn .").append("\n");
		buf.append("      ?s MRHIER:hcd ?hcd .").append("\n");
		buf.append("      ?s MRHIER:paui ?paui .").append("\n");
		buf.append("      ?s MRHIER:ptr ?ptr .").append("\n");
		buf.append("      ?s MRHIER:rela ?rela .").append("\n");
		buf.append("      ?s MRHIER:sab ?sab .").append("\n");
		buf.append("   }").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		return buf.toString();
	}

	public static String construct_mrrel_query(String cui) {
		return construct_mrrel_query(cui, null, null);
	}


	// Find all relationships for a UMLS concept.
    public Vector getRelationships(String cui) {
		return getRelationships(cui, null, null);
	}

    public Vector getParentRelationships(String cui) {
		return getRelationships(cui, "PAR");
	}

    public Vector getChildRelationships(String cui) {
		return getRelationships(cui, "CHD");
	}

    public Vector getRelationships(String cui, String rel) {
		return getRelationships(cui, rel, null);
	}

    public Vector getParentRelationships(String cui, String sab) {
		return getRelationships(cui, "PAR", sab);
	}

    public Vector getChildRelationships(String cui, String sab) {
		return getRelationships(cui, "CHD", sab);
	}

	public Vector removeAUINamespace(Vector v) {
		if (v == null) return v;
		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(line);
			StringBuffer buf = new StringBuffer();
			for (int j=0; j<u.size(); j++) {
				String t = (String) u.elementAt(j);
				if (j < 2) {
					int n = t.lastIndexOf("A");
					t = t.substring(n, t.length());
				}
				buf.append(t);
				if (j < u.size()-1) {
					buf.append("|");
				}
			}
            w.add(buf.toString());
		}
		return w;
	}

    public Vector getRelationships(String cui, String rel, String sab) {
        Vector v = new Vector();
        String query = construct_mrrel_query(cui, rel, sab);
        v = executeQuery(query);
		if (v == null || v.size() == 0) return null;
		v = new ParserUtils().getResponseValues(v);
		v = new gov.nih.nci.evs.restapi.util.SortUtils().quickSort(v);

        v = removeAUINamespace(v);

        return v;
	}


	public static String construct_mrrel_query(String cui, String rel) {
		return construct_mrrel_query(cui, rel, null);
	}

	public static String construct_mrrel_query(String cui, String rel, String sab) {
		return construct_mrrel_query(cui, rel, sab, null);
	}

	public static String construct_mrrel_query(String cui, String rel, String sab, String aui) {
		StringBuffer buf = new StringBuffer();
		//buf.append("PREFIX sm: <tag:stardog:api:mapping:>").append("\n");
		buf.append("PREFIX MRREL: <http://ncicb.nci.nih.gov/" + VIRTUAL_GRAPH_NAME + "/mrrel/>").append("\n");
		//buf.append("PREFIX dc:<http://purl.org/dc/elements/1.1/>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		//buf.append("SELECT ?aui1 ?aui2 ?cui1 ?cui2 ?cvf ?dir ?rel ?rela ?rg ?rui ?sab ?sl ?srui ?stype1 ?stype2 ?suppress {").append("\n");

		buf.append("SELECT ?aui1 ?aui2 ?cui1 ?cui2 ?rel ?rela ?sab {").append("\n");

		buf.append("   GRAPH <virtual://" + VIRTUAL_GRAPH_NAME + "> {").append("\n");
		//buf.append("      ?s a :MRREL .").append("\n");
		buf.append("      ?s MRREL:aui1 ?aui1 .").append("\n");
		if (aui != null) {
		    buf.append("      ?s MRREL:aui1 \"" + aui + "\"^^xsd:string .").append("\n");
	    }

		buf.append("      ?s MRREL:aui2 ?aui2 .").append("\n");
		buf.append("      ?s MRREL:cui1 ?cui1 .").append("\n");
		buf.append("      ?s MRREL:cui1 \"" + cui + "\"^^xsd:string .").append("\n");
		buf.append("      ?s MRREL:cui2 ?cui2 .").append("\n");
		//buf.append("      ?s MRREL:cvf ?cvf .").append("\n");
		//buf.append("      ?s MRREL:dir ?dir .").append("\n");
		buf.append("      ?s MRREL:rel ?rel .").append("\n");

		if (rel != null) {
		    buf.append("      ?s MRREL:rel \"" + rel + "\"^^xsd:string .").append("\n");
	    }

		//buf.append("      ?s MRREL:rela ?rela .").append("\n");
		//buf.append("      ?s MRREL:rg ?rg .").append("\n");
		//buf.append("      ?s MRREL:rui ?rui .").append("\n");
		buf.append("      ?s MRREL:sab ?sab .").append("\n");

		if (sab != null) {
			buf.append("      ?s MRREL:sab \"" + sab + "\"^^xsd:string .").append("\n");
		}
		//buf.append("      ?s MRREL:sl ?sl .").append("\n");
		//buf.append("      ?s MRREL:srui ?srui .").append("\n");
		//buf.append("      ?s MRREL:stype1 ?stype1 .").append("\n");
		//buf.append("      ?s MRREL:stype2 ?stype2 .").append("\n");
		//buf.append("      ?s MRREL:suppress ?suppress .").append("\n");
		buf.append("   }").append("\n");

 		buf.append("OPTIONAL {").append("\n");
        buf.append("      ?s MRREL:rela ?rela .").append("\n");
		buf.append("}").append("\n");



		buf.append("}").append("\n");
		buf.append("").append("\n");
		return buf.toString();
	}


	public static String construct_label_query(String cui) {
		return construct_label_query(cui, null);
	}

	public static String construct_label_query(String cui, String sab) {
		//String ts = "P";
		String tty = "PT";
		String stt = "PF";
		String ispref = "Y";
		String lat = "ENG";
		//return construct_label_query(cui, sab, tty, stt, ispref, lat);
		return construct_label_query(cui, sab, null, tty, stt, ispref, lat);

	}

	public static String construct_label_query(String cui, String sab, String aui) {
		String tty = "PT";
		String stt = "PF";
		String ispref = "Y";
		String lat = "ENG";
		return construct_label_query(cui, sab, aui, tty, stt, ispref, lat);
	}

	public static String construct_label_query(String cui, String sab, String aui, String tty, String stt, String ispref, String lat) {
		StringBuffer buf = new StringBuffer();
		//buf.append("PREFIX sm: <tag:stardog:api:mapping:>").append("\n");
		buf.append("PREFIX MRCONSO: <http://ncicb.nci.nih.gov/" + VIRTUAL_GRAPH_NAME + "/mrconso/>").append("\n");
		////buf.append("PREFIX dc:<http://purl.org/dc/elements/1.1/>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("SELECT ?str ?code ?aui ?tty ?ispref ?lat ?stt ?ts{").append("\n");
		buf.append("   GRAPH <virtual://" + VIRTUAL_GRAPH_NAME + "> {").append("\n");
		//buf.append("      ?s a :MRCONSO .").append("\n");
		buf.append("      ?s MRCONSO:cui ?cui .").append("\n");
		buf.append("      ?s MRCONSO:aui ?aui .").append("\n");
		//buf.append("      ?s MRCONSO:aui \"" + aui + "\"^^xsd:string .").append("\n");
		buf.append("      ?s MRCONSO:cui \"" + cui + "\"^^xsd:string .").append("\n");

		if (sab != null) {
			buf.append("      ?s MRCONSO:sab \"" + sab + "\"^^xsd:string .").append("\n");
		}
        buf.append("      ?s MRCONSO:code ?code .").append("\n");
        buf.append("      ?s MRCONSO:str ?str .").append("\n");
        buf.append("      ?s MRCONSO:sab ?sab .").append("\n");
        buf.append("      ?s MRCONSO:tty ?tty .").append("\n");

        if (tty != null) {
			buf.append("      ?s MRCONSO:tty \"" + tty + "\"^^xsd:string .").append("\n");
		}

        buf.append("      ?s MRCONSO:ispref ?ispref .").append("\n");
        buf.append("      ?s MRCONSO:lat ?lat .").append("\n");
        buf.append("      ?s MRCONSO:stt ?stt .").append("\n");
        buf.append("      ?s MRCONSO:ts ?ts .").append("\n");

		buf.append("      ?s MRCONSO:ispref \"" + ispref + "\"^^xsd:string .").append("\n");
		buf.append("      ?s MRCONSO:lat \"" + lat + "\"^^xsd:string .").append("\n");
		buf.append("      ?s MRCONSO:stt \"" + stt + "\"^^xsd:string .").append("\n");

		buf.append("   }").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		return buf.toString();
	}
/*
    public String getConceptName(String cui) {
        Vector v = new Vector();
        String query = construct_label_query(cui);
        v = executeQuery(query);
		if (v == null || v.size() == 0) return null;
		v = new ParserUtils().getResponseValues(v);
		String first_line = (String) v.elementAt(0);
		Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(first_line);
		return (String) u.elementAt(0);
	}
*/

    public Atom getPreferredAtom(String cui, String sab) {
        Vector v = new Vector();
        String query = construct_label_query(cui, sab);
        v = executeQuery(query);
		if (v == null || v.size() == 0) return null;
		v = new ParserUtils().getResponseValues(v);
		String first_line = (String) v.elementAt(0);
		Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(first_line);
		String str = (String) u.elementAt(0);
		String code = (String) u.elementAt(1);
		String aui = (String) u.elementAt(2);
		return new Atom(code, str, aui, sab, cui);
	}


    public String getPreferredAtomName(String cui, String sab) {
        Vector v = new Vector();
        String query = construct_label_query(cui, sab);
        v = executeQuery(query);
		if (v == null || v.size() == 0) return null;
		v = new ParserUtils().getResponseValues(v);
		String first_line = (String) v.elementAt(0);
		Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(first_line);
		return (String) u.elementAt(0);
	}

    public String getPreferredAtomNameAndCode(String cui, String sab) {
        Vector v = new Vector();
        String query = construct_label_query(cui, sab);
        v = executeQuery(query);
		if (v == null || v.size() == 0) return null;
		v = new ParserUtils().getResponseValues(v);
		return (String) v.elementAt(0);
	}

	public static String construct_related_concepts_query(String cui) {
		String ts = "P";
		String stt = "PF";
		String ispref = "Y";
		String lat = "ENG";
		return construct_related_concepts_query(cui, ts, stt, ispref, lat);
	}


//	buf.append("SELECT ?rel ?rela ?cui1 ?cui2 ?str ?sab ?code {").append("\n");


	public static String construct_related_concepts_query(String cui, String ts, String stt, String ispref, String lat) {
		StringBuffer buf = new StringBuffer();
		//buf.append("PREFIX sm: <tag:stardog:api:mapping:>").append("\n");
		buf.append("PREFIX MRREL: <http://ncicb.nci.nih.gov/" + VIRTUAL_GRAPH_NAME + "/mrrel/>").append("\n");
		buf.append("PREFIX MRCONSO: <http://ncicb.nci.nih.gov/" + VIRTUAL_GRAPH_NAME + "/mrconso/>").append("\n");

		//buf.append("PREFIX dc:<http://purl.org/dc/elements/1.1/>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("SELECT ?rel ?rela ?cui1 ?cui2 ?str ?sab ?code ?tty {").append("\n");

		buf.append("   GRAPH <virtual://" + VIRTUAL_GRAPH_NAME + "> {").append("\n");
		//buf.append("      ?s a :MRREL .").append("\n");
		buf.append("      ?s MRREL:cui1 ?cui1 .").append("\n");
		buf.append("      ?s MRREL:cui1 \"" + cui + "\"^^xsd:string .").append("\n");
		buf.append("      ?s MRREL:cui2 ?cui2 .").append("\n");

		buf.append("      ?s MRREL:rel ?rel .").append("\n");
		buf.append("      ?s MRREL:rela ?rela .").append("\n");

		//buf.append("      ?t a :MRCONSO .").append("\n");
		buf.append("      ?t MRCONSO:cui ?cui .").append("\n");
		buf.append("      ?t MRCONSO:ispref \"" + ispref + "\"^^xsd:string .").append("\n");
		buf.append("      ?t MRCONSO:lat \"" + lat + "\"^^xsd:string .").append("\n");
		buf.append("      ?t MRCONSO:code ?code .").append("\n");
		buf.append("      ?t MRCONSO:str ?str .").append("\n");
		buf.append("      ?t MRCONSO:sab ?sab .").append("\n");
		buf.append("      ?t MRCONSO:tty ?tty .").append("\n");
		buf.append("      ?t MRCONSO:stt \"" + stt + "\"^^xsd:string .").append("\n");
		buf.append("      ?t MRCONSO:ts \"" + ts + "\"^^xsd:string .").append("\n");

		buf.append("      FILTER (str(?cui2) = str(?cui))").append("\n");

		buf.append("   }").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		return buf.toString();
	}

	//rel + "|" + rela + "|" + rel_cui + "|" + term + "|" + sab + "|" + type + "|" + code
	/*
		private String rel;
		private String rela;
		private String cui1;
		private String cui2;
		private String str;
		private String sab;
	    private String code;
	*/

	public MetaRelationship toMetaRelationship(String cui, String t) {
		 Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(t, '|');
		 return new MetaRelationship(
		   (String) u.elementAt(0),
		   (String) u.elementAt(1),
		   cui,
		   (String) u.elementAt(2),
		   (String) u.elementAt(3),
		   (String) u.elementAt(4),
		   (String) u.elementAt(6),
		   (String) u.elementAt(7)
		   );
	}

    // Find all relationships for a concept and the preferred (English) name of the CUI2.
    public Vector getRelatedConcepts(String cui) {
        Vector w = new Vector();
        String query = construct_related_concepts_query(cui);

        Vector v = executeQuery(query);
		if (v == null || v.size() == 0) return null;
		v = new ParserUtils().getResponseValues(v);
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			MetaRelationship rel = toMetaRelationship(cui, t);
			w.add(rel);
		}
		return w;
	}

//	 //w.add(rel + "|" + rela + "|" + rel_cui + "|" + term + "|" + sab + "|" + type + "|" + code);
	public static String construct_atoms_in_related_concepts_query(String cui) {
		StringBuffer buf = new StringBuffer();
		//buf.append("PREFIX sm: <tag:stardog:api:mapping:>").append("\n");
		buf.append("PREFIX MRREL: <http://ncicb.nci.nih.gov/" + VIRTUAL_GRAPH_NAME + "/mrrel/>").append("\n");
		buf.append("PREFIX MRCONSO: <http://ncicb.nci.nih.gov/" + VIRTUAL_GRAPH_NAME + "/mrconso/>").append("\n");

		//buf.append("PREFIX dc:<http://purl.org/dc/elements/1.1/>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("SELECT ?rel ?rela ?cui2 ?str ?sab ?tty ?code {").append("\n");

		buf.append("   GRAPH <virtual://" + VIRTUAL_GRAPH_NAME + "> {").append("\n");
		//buf.append("      ?s a :MRREL .").append("\n");
		buf.append("      ?s MRREL:cui1 ?cui1 .").append("\n");
		buf.append("      ?s MRREL:cui1 \"" + cui + "\"^^xsd:string .").append("\n");
		buf.append("      ?s MRREL:cui2 ?cui2 .").append("\n");
		buf.append("      ?s MRREL:rel ?rel .").append("\n");
		buf.append("      ?s MRREL:rela ?rela .").append("\n");

		//buf.append("      ?t a :MRCONSO .").append("\n");
		buf.append("      ?t MRCONSO:code ?code .").append("\n");
		buf.append("      ?t MRCONSO:cui ?cui .").append("\n");
		//buf.append("      ?s MRCONSO:cui \"" + cui + "\"^^xsd:string .").append("\n");
		buf.append("      ?t MRCONSO:sab ?sab .").append("\n");
		buf.append("      ?t MRCONSO:str ?str .").append("\n");
		buf.append("      ?t MRCONSO:tty ?tty .").append("\n");

		buf.append("      FILTER (str(?cui2) = str(?cui))").append("\n");

		buf.append("   }").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		return buf.toString();
	}

    // Find all relationships for a concept and the preferred (English) name of the CUI2.
    public Vector getAtomsInRelatedConcepts(String cui) {
        Vector v = new Vector();
        String query = construct_atoms_in_related_concepts_query(cui);
        v = executeQuery(query);
		if (v == null || v.size() == 0) return null;
		v = new ParserUtils().getResponseValues(v);
		return v;
	}

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static String construct_related_concept_query(String rel, String sab, String cui, String code) {
		StringBuffer buf = new StringBuffer();
		//buf.append("PREFIX sm: <tag:stardog:api:mapping:>").append("\n");
		buf.append("PREFIX MRREL: <http://ncicb.nci.nih.gov/" + VIRTUAL_GRAPH_NAME + "/mrrel/>").append("\n");
		buf.append("PREFIX MRCONSO: <http://ncicb.nci.nih.gov/" + VIRTUAL_GRAPH_NAME + "/mrconso/>").append("\n");

		//buf.append("PREFIX dc:<http://purl.org/dc/elements/1.1/>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("SELECT distinct ?cui1 ?rel ?cui2 ?sab {").append("\n");

		buf.append("   GRAPH <virtual://" + VIRTUAL_GRAPH_NAME + "> {").append("\n");

		//buf.append("      ?t a :MRCONSO .").append("\n");
		buf.append("      ?t MRCONSO:code \"" + code + "\"^^xsd:string .").append("\n");
		if (cui != null) {
		    buf.append("      ?t MRCONSO:cui \"" + sab + "\"^^xsd:string .").append("\n");
		}
		buf.append("      ?t MRCONSO:cui ?cui .").append("\n");
		buf.append("      ?t MRCONSO:sab \"" + sab + "\"^^xsd:string .").append("\n");
		buf.append("      ?t MRCONSO:str ?str .").append("\n");

		//buf.append("      ?s a :MRREL .").append("\n");
		if (cui != null) {
			buf.append("      ?s MRREL:cui1 \"" + sab + "\"^^xsd:string .").append("\n");
		}
		buf.append("      ?s MRREL:cui1 ?cui1 .").append("\n");
		buf.append("      ?s MRREL:cui2 ?cui2 .").append("\n");
		buf.append("      ?s MRREL:rel \"" + rel + "\"^^xsd:string .").append("\n");
        buf.append("      ?s MRREL:sab \"" + sab + "\"^^xsd:string .").append("\n");

		buf.append("      FILTER (str(?cui1) = str(?cui))").append("\n");

		buf.append("   }").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		return buf.toString();
	}

    //buf.append("SELECT ?cui1 ?rel ?cui2 ?sab {").append("\n");
    public Vector getRelatedconceptsInSource(String rel, String sab, String cui, String code) {
        Vector v = new Vector();
        String query = construct_related_concept_query(rel, sab, cui, code);
        v = executeQuery(query);
		if (v == null || v.size() == 0) return null;
		v = new ParserUtils().getResponseValues(v);
        return v;
	}

    public Vector getSubconceptsInSource(String sab, String cui, String code) {
		return getRelatedconceptsInSource(CHD, sab, cui, code);
	}

    public Vector getSuperconceptsInSource(String sab, String cui, String code) {
		return getRelatedconceptsInSource(PAR, sab, cui, code);
	}

    public String getRootSourceCode(String source) {
		return "V-" + source;
	}

    public String getRootSource() {
		return ROOT_SAB;
	}


    public String construct_cui_query(String sab, String code) {
		StringBuffer buf = new StringBuffer();
		//buf.append("PREFIX sm: <tag:stardog:api:mapping:>").append("\n");
		buf.append("PREFIX MRCONSO: <http://ncicb.nci.nih.gov/" + VIRTUAL_GRAPH_NAME + "/mrconso/>").append("\n");
		//buf.append("PREFIX dc:<http://purl.org/dc/elements/1.1/>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("SELECT distinct ?cui {").append("\n");
		buf.append("   GRAPH <virtual://" + VIRTUAL_GRAPH_NAME + "> {").append("\n");
		//buf.append("      ?s a :MRCONSO .").append("\n");
		buf.append("      ?s MRCONSO:cui ?cui .").append("\n");
		buf.append("      ?s MRCONSO:sab \"" + sab + "\"^^xsd:string .").append("\n");
		buf.append("      ?s MRCONSO:code \"" + code + "\"^^xsd:string .").append("\n");
		buf.append("   }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}


    public Vector getCUIs(String sab, String code) {
        Vector v = new Vector();
        String query = construct_cui_query(sab, code);
        v = executeQuery(query);
		if (v == null || v.size() == 0) return null;
		v = new ParserUtils().getResponseValues(v);
		return v;
	}

	public String getSourceHierarchyRoot(String sab) {
		String code = getRootSourceCode(sab);
		Vector v = getCUIs("SRC", code);
		if (v != null && v.size() > 0) {
			return (String) v.elementAt(0);
		}
		return null;
	}

	public boolean sourceHierarchyAvailable(String sab) {
		String root = getSourceHierarchyRoot(sab);
		Vector subs = getChildRelationships(root, sab);
		if (subs == null || subs.size() == 0) {
			return false;
		}
		return true;
	}

    public Vector getSourcesWithHierarchy() {
        Vector v = new Vector();
        String query = construct_source_with_hierarcy_query();
        v = executeQuery(query);
		if (v == null || v.size() == 0) return null;
		v = new ParserUtils().getResponseValues(v);
		return new gov.nih.nci.evs.restapi.util.SortUtils().quickSort(v);
	}

    public String construct_atom_name_query(String cui, String sab, String aui) {
		StringBuffer buf = new StringBuffer();
		//buf.append("PREFIX sm: <tag:stardog:api:mapping:>").append("\n");
		buf.append("PREFIX MRCONSO: <http://ncicb.nci.nih.gov/" + VIRTUAL_GRAPH_NAME + "/mrconso/>").append("\n");
		//buf.append("PREFIX dc:<http://purl.org/dc/elements/1.1/>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("SELECT ?str {").append("\n");
		buf.append("   GRAPH <virtual://" + VIRTUAL_GRAPH_NAME + "> {").append("\n");
		//buf.append("      ?s a :MRCONSO .").append("\n");
		buf.append("      ?s MRCONSO:cui \"" + cui + "\"^^xsd:string .").append("\n");
		buf.append("      ?s MRCONSO:sab \"" + sab + "\"^^xsd:string .").append("\n");
		buf.append("      ?s MRCONSO:aui \"" + aui + "\"^^xsd:string .").append("\n");
		buf.append("      ?s MRCONSO:str ?str .").append("\n");
		buf.append("   }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

    public String getAtomNameByAUI(String cui, String sab, String aui) {
        Vector v = new Vector();
        String query = construct_atom_name_query(cui, sab, aui);
        v = executeQuery(query);
		if (v == null || v.size() == 0) return null;
		v = new ParserUtils().getResponseValues(v);
		String first_line = (String) v.elementAt(0);
		Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(first_line);
		return (String) u.elementAt(0);
	}


    public String construct_atom_name_by_code_query(String cui, String sab, String code) {
		StringBuffer buf = new StringBuffer();
		//buf.append("PREFIX sm: <tag:stardog:api:mapping:>").append("\n");
		buf.append("PREFIX MRCONSO: <http://ncicb.nci.nih.gov/" + VIRTUAL_GRAPH_NAME + "/mrconso/>").append("\n");
		//buf.append("PREFIX dc:<http://purl.org/dc/elements/1.1/>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("SELECT ?str {").append("\n");
		buf.append("   GRAPH <virtual://" + VIRTUAL_GRAPH_NAME + "> {").append("\n");
		//buf.append("      ?s a :MRCONSO .").append("\n");
		buf.append("      ?s MRCONSO:cui \"" + cui + "\"^^xsd:string .").append("\n");
		buf.append("      ?s MRCONSO:code \"" + code + "\"^^xsd:string .").append("\n");
		buf.append("      ?s MRCONSO:sab \"" + sab + "\"^^xsd:string .").append("\n");
		buf.append("      ?s MRCONSO:str ?str .").append("\n");
		buf.append("   }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

    public String getAtomNameByCode(String cui, String sab, String code) {
        Vector v = new Vector();
        String query = construct_atom_name_by_code_query(cui, sab, code);
        v = executeQuery(query);
		if (v == null || v.size() == 0) return null;
		v = new ParserUtils().getResponseValues(v);
		String first_line = (String) v.elementAt(0);
		Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(first_line);
		return (String) u.elementAt(0);
	}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static String construct_intra_cui_relationships_query(String cui, String sab, String rel, String rela) {
		StringBuffer buf = new StringBuffer();
		//buf.append("PREFIX sm: <tag:stardog:api:mapping:>").append("\n");
		buf.append("PREFIX MRREL: <http://ncicb.nci.nih.gov/" + VIRTUAL_GRAPH_NAME + "/mrrel/>").append("\n");
		//buf.append("PREFIX dc:<http://purl.org/dc/elements/1.1/>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("SELECT ?cui1 ?aui1 ?cui2 ?aui2 ?rel ?rela {").append("\n");
		buf.append("   GRAPH <virtual://" + VIRTUAL_GRAPH_NAME + "> {").append("\n");
		//buf.append("      ?t a :MRREL .").append("\n");
		buf.append("      ?t MRREL:sab \"" + sab + "\"^^xsd:string .").append("\n");
		buf.append("      ?t MRREL:aui1 ?aui1 .").append("\n");
		buf.append("      ?t MRREL:aui2 ?aui2 .").append("\n");

		buf.append("      ?t MRREL:cui1 ?cui1 .").append("\n");
		buf.append("      ?t MRREL:cui2 ?cui2 .").append("\n");

		buf.append("      ?t MRREL:rel ?rel .").append("\n");
		buf.append("      ?t MRREL:rela ?rela .").append("\n");

		buf.append("      ?t MRREL:cui1  \"" + cui + "\"^^xsd:string .").append("\n");
		buf.append("      ?t MRREL:cui2  \"" + cui + "\"^^xsd:string .").append("\n");
		if (rel != null) {
			buf.append("      ?t MRREL:rel  \"" + rel + "\"^^xsd:string .").append("\n");
		}
		if (rela != null) {
			buf.append("      ?t MRREL:rela ?rela .").append("\n");
		}
		buf.append("   }").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		return buf.toString();
	}

    public Vector getIntraCUIRelationships(String cui, String sab, String rel, String rela) {
        Vector v = new Vector();
        String query = construct_intra_cui_relationships_query(cui, sab, rel, rela);
        v = executeQuery(query);
		if (v == null || v.size() == 0) return null;
		v = new ParserUtils().getResponseValues(v);
		return v;
	}

    public Vector getIntraCUIRelationships(String cui, String sab, String rel) {
		return getIntraCUIRelationships(cui, sab, rel, null);
	}

    public Vector getSelfReferentialRelationship(String rela, String cui, String sab) {
		String name = metathesaurusDataUtils.getConceptName(cui);
		Vector w = new Vector();

		MetathesaurusDataUtils metathesaurusDataUtils = new MetathesaurusDataUtils(serviceUrl);

		//buf.append("SELECT ?aui1 ?rela ?aui2 {").append("\n");
        Vector v = getIntraCUIRelationships(cui, sab, rela);
        for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(t);
			String aui1 = (String) u.elementAt(0);
			String aui2 = (String) u.elementAt(2);
			String nameAndCode = metathesaurusDataUtils.getAtomNameAndCodeByAUI(cui, sab, aui1);
			Vector u2 = gov.nih.nci.evs.restapi.util.StringUtils.parseData(nameAndCode);
			String source_aui = aui1;
			String source_aui_name = (String) u2.elementAt(0);
			nameAndCode = metathesaurusDataUtils.getAtomNameAndCodeByAUI(cui, sab, aui2);
			u2 = gov.nih.nci.evs.restapi.util.StringUtils.parseData(nameAndCode);
			String target_aui = aui1;
			String target_aui_name = (String) u2.elementAt(0);
            w.add(source_aui + "$" + source_aui_name + "$" + rela + "$"
                + target_aui + "$" + target_aui_name + "$" + sab + "$"
                + cui + "$" + name);
		}
		return w;
	}

    public Vector getNextLevelAtomsBySource(String cui, String sab, String aui, String rel) {
        Vector v = new Vector();
        String query = construct_mrrel_query(cui, rel, sab, aui);
        v = executeQuery(query);
		if (v == null || v.size() == 0) return null;
		v = new ParserUtils().getResponseValues(v);
		return v;
	}

	// Find all atoms in SRC with source-code (i.e., atom code) matching "V-<source)"
	public static String construct_meta_root_query() {
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX MRCONSO: <http://ncicb.nci.nih.gov/" + VIRTUAL_GRAPH_NAME + "/mrconso/>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("SELECT ?code ?str ?aui ?sab ?cui ?ispref ?lat ?stt ?ts ?tty {").append("\n");
		buf.append("   GRAPH <virtual://" + VIRTUAL_GRAPH_NAME + "> {").append("\n");
		buf.append("      ?s MRCONSO:aui ?aui .").append("\n");
		buf.append("      ?s MRCONSO:code ?code .").append("\n");
		buf.append("      ?s MRCONSO:cui ?cui .").append("\n");
		//buf.append("      ?s MRCONSO:cvf ?cvf .").append("\n");
		buf.append("      ?s MRCONSO:ispref ?ispref .").append("\n");
		buf.append("      ?s MRCONSO:ispref \"" + YES + "\"^^xsd:string .").append("\n");

		buf.append("      ?s MRCONSO:lat ?lat .").append("\n");
		buf.append("      ?s MRCONSO:lat \"" + DEFAULT_LAT + "\"^^xsd:string .").append("\n");
		//buf.append("      ?s MRCONSO:lui ?lui .").append("\n");
		buf.append("      ?s MRCONSO:sab \"" + ROOT_SAB + "\"^^xsd:string .").append("\n");
		//buf.append("      ?s MRCONSO:saui ?saui .").append("\n");
		//buf.append("      ?s MRCONSO:scui ?scui .").append("\n");
		//buf.append("      ?s MRCONSO:sdui ?sdui .").append("\n");
		//buf.append("      ?s MRCONSO:srl ?srl .").append("\n");
		buf.append("      ?s MRCONSO:sab ?sab .").append("\n");
		buf.append("      ?s MRCONSO:str ?str .").append("\n");
		buf.append("      ?s MRCONSO:stt ?stt .").append("\n");
		//buf.append("      ?s MRCONSO:sui ?sui .").append("\n");
		//buf.append("      ?s MRCONSO:suppress ?suppress .").append("\n");
		buf.append("      ?s MRCONSO:ts ?ts .").append("\n");
		buf.append("      ?s MRCONSO:ts \"" + TS_P + "\"^^xsd:string .").append("\n");

		buf.append("      ?s MRCONSO:tty ?tty .").append("\n");
		buf.append("      ?s MRCONSO:tty \"" + VPT + "\"^^xsd:string .").append("\n");

		buf.append("      FILTER (strStarts(str(?code), \"V-\"))").append("\n");
		buf.append("   }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

    public HashMap createSAB2HierarchyRootHashMap() {
		Atom atom = null;
		HashMap hmap = new HashMap();
        Vector v = new Vector();
        String query = construct_meta_root_query();
        v = executeQuery(query);
		if (v == null) {
			return null;
		}
		v = new ParserUtils().getResponseValues(v);
		v = new gov.nih.nci.evs.restapi.util.SortUtils().quickSort(v);


//gov.nih.nci.evs.restapi.util.StringUtils.dumpVector("v: ", v);
/*
        (83) V-RENI_2018_07E|Registry Nomenclature Information System, 2018_07E|A15565966|SRC|CL552553|Y|ENG|PF|P|VPT
        (84) V-RXNORM_2016AA_2016_09_06F|RxNorm Vocabulary, 16AA_160906F|A15304075|SRC|C4275285|Y|ENG|PF|P|VPT
        (85) V-SNOMEDCT_US_2018_03_01|US Edition of SNOMED CT, 2018_03_01|A15524469|SRC|CL546059|Y|ENG|PF|P|VPT
        (86) V-SOP_6|Source of Payment Typology, 6|A15099928|SRC|C4224058|Y|ENG|PF|P|VPT
        (87) V-SPN_2003|Standard Product Nomenclature, 2003|A13875063|SRC|C1262315|Y|ENG|PF|P|VPT
        (88) V-UCUM_2018_07E|Unified Code for Units of Measure, 2018_07E|A15565968|SRC|CL552554|Y|ENG|PF|P|VPT
*/
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(t, '|');
			String u0 = (String) u.elementAt(0);
			/*
			String sab = u0.substring(2, u0.length());
            int n = sab.indexOf("_");
            if (n != -1) {
				sab = sab.substring(0, n);
			}
			*/
			atom = new Atom((String) u.elementAt(0),
			                (String) u.elementAt(1),
			                (String) u.elementAt(2),
			                (String) u.elementAt(3),
			                (String) u.elementAt(4),
			                (String) u.elementAt(5),
			                (String) u.elementAt(6),
			                (String) u.elementAt(7),
			                (String) u.elementAt(8),
			                (String) u.elementAt(9));

           hmap.put(u0, atom);
		}
		return hmap;
	}

    public TreeItem buildSourceTree(String code, String sab) {
		Atom atom = null;
		if (code == null) {
			Vector cuis = getCUIs("SRC", "V-" + sab);
			code = (String) cuis.elementAt(0);
		}
        Vector v = getChildRelationships(code, sab);
		String assoc = "CHD";
		TreeItem root = new TreeItem("<Root>", "Root node");
		root._expandable = false;
		for (int i=0; i<v.size();i++) {
			String t = (String) v.elementAt(i);
			Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(t);
			String cui2 = (String) u.elementAt(3);
			String aui2 = (String) u.elementAt(1);
			Vector atoms = metathesaurusDataUtils.getSortedAtomData(cui2, sab);
			if (atoms == null && sab.compareTo("SRC") != 0) {
				atoms = metathesaurusDataUtils.getSortedAtomData(cui2, "SRC");
			}
			AtomData first_atom = (AtomData) atoms.elementAt(0);
			String str2 = first_atom.getStr();
			String code2 = first_atom.getCode();
			if (code != null && code2.compareTo(code) != 0) {
				TreeItem childItem = new TreeItem(code2, str2);
				childItem._code = cui2;
				childItem._text = str2;
				childItem._auis = aui2;
				childItem._expandable = isExpandable(cui2, sab);
				root.addChild(assoc, childItem);
				root._expandable = true;
			}
     	}
        return root;
	}

    public TreeItem buildSourceTree(String sab) {
		return buildSourceTree(null, sab);
	}

	public List getRootConcepts(String sab) {
		List list = new ArrayList();
		TreeItem node = null;
		try {
			node = buildSourceTree(sab);
			if (node == null) {
				node = buildSourceTree("SRC");
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		if (node == null) {
			return null;
		}
		for (String assoc : node._assocToChildMap.keySet()) {
			List<TreeItem> children = node._assocToChildMap.get(assoc);
			for (int i=0; i<children.size(); i++) {
				TreeItem child = (TreeItem) children.get(i);
                ResolvedConceptReference rcr = new ResolvedConceptReference(
					sab, null, child._code, sab, child._text);
			    list.add(rcr);
			}
		}
        new gov.nih.nci.evs.restapi.util.SortUtils().quickSort(list);
        return list;
	}

    public HashMap getSourceRoots(String code, String sab) {
		TreeItem node = buildSourceTree(code, sab);
		HashMap hmap = new HashMap();
		hmap.put(node._code, node);
		return hmap;
	}

	boolean isExpandable(String cui, String sab) {
		Vector w = getRelationships(cui, "CHD", sab);
		if (w == null || w.size() == 0) return false;
		return true;
	}

    public TreeItem expandSourceTree(Atom atom) {
		return expandSourceTree(atom.getSab(), atom.getCui(), atom.getAui(), atom.getCode(), atom.getStr());
	}


    public TreeItem expandSourceTree(String sab, String cui, String aui, String code, String name) {
		String assoc = "CHD";
		//buf.append("SELECT distinct ?cui2, ?aui2 {
		Vector v = getNextLevelAtomsBySource(cui, sab, aui, assoc);
		TreeItem root = new TreeItem(code, name);
		//root._code = cui;
		root._auis = sab;
		root._expandable = false;
		for (int i=0; i<v.size();i++) {
			String t = (String) v.elementAt(i);
			Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(t);
			String cui2 = (String) u.elementAt(0);
			String aui2 = (String) u.elementAt(1);
			String nameAndCode = metathesaurusDataUtils.getAtomNameAndCodeByAUI(cui2, sab, aui2);
			Vector u2 = gov.nih.nci.evs.restapi.util.StringUtils.parseData(nameAndCode);
			String str2 = (String) u2.elementAt(0);
			String code2 = (String) u2.elementAt(1);
			TreeItem childItem = new TreeItem(code2, str2);
			childItem._code = cui2;
			childItem._auis = sab;
			childItem._expandable = isExpandable(cui2, sab);
			root.addChild(assoc, childItem);
			root._expandable = true;
     	}
        return root;
    }


    public HashMap getChildrenExt(String cui, String sab) {
		Atom atom = getPreferredAtom(cui, sab);
		TreeItem metaTreeNode = expandSourceTree(atom);
		HashMap hmap = new HashMap();
		hmap.put(atom.getCode(), metaTreeNode);
		return hmap;
	}


    public Path createPath(String path) {
		Path p = new Path();
		List concepts = new ArrayList();
        Vector atom_data_points = gov.nih.nci.evs.restapi.util.StringUtils.parseData(path);
        for (int i=0; i<atom_data_points.size(); i++) {
			String atom_data_point = (String) atom_data_points.elementAt(i);
			Vector u2 = gov.nih.nci.evs.restapi.util.StringUtils.parseData(atom_data_point, '$');
			//cui + "$" + aui + "$" + str + "$" + code + "$" + sab
			Concept concept = new Concept(
				i,
				(String) u2.elementAt(2),
				(String) u2.elementAt(3),
				(String) u2.elementAt(0),
				(String) u2.elementAt(1),
				(String) u2.elementAt(4));
			concepts.add(concept);
		}
		p.setDirection(TRAVERSE_UP);
		p.setConcepts(concepts);
		return p;
	}

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// View in Hierarchy
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/*
    public TreeItem buildViewInHierarchyTree(String sab, String cui, String aui) {
		Paths paths = findPathsToRoots(sab, cui, aui);
        return paths2MetaTreeNode(sab, paths);
	}
*/
	public TreeItem concept2TreeItem(Concept c) {
		if (c == null) return null;
		return new TreeItem(c.getCode(),
		                        c.getLabel(),
		                        c.getSab());
	}

	public TreeItem path2MetaTreeNode(Path path) {
		String asso = "CHD";
		List conceptList = path.getConcepts();
		HashMap hmap = new HashMap();
		for (int k2=0; k2<conceptList.size(); k2++) {
			Concept c = (Concept) conceptList.get(k2);
            TreeItem node = concept2TreeItem(c);
            hmap.put(new Integer(c.getIdx()), node);
		}
		TreeItem root = (TreeItem) hmap.get( new Integer(0));
		for (int k2=0; k2<conceptList.size()-1; k2++) {
            TreeItem node = (TreeItem) hmap.get(new Integer(k2));
            TreeItem childnode = (TreeItem) hmap.get(new Integer(k2+1));
            node.addChild(asso, childnode);
		}
        return (TreeItem) hmap.get(new Integer(0));
	}


	public TreeItem searchTree(TreeItem node, String sab, String cui) {
		if (node._auis.compareTo(sab) == 0 && node._code.compareTo(cui) == 0) {
			return node;
		}
		for (String assoc : node._assocToChildMap.keySet()) {
			List<TreeItem> children = node._assocToChildMap.get(assoc);
			for (int i=0; i<children.size(); i++) {
				TreeItem child = (TreeItem) children.get(i);
				TreeItem ti_next = searchTree(child,  sab, cui);
				if (ti_next != null) return ti_next;
			}
		}
		return null;
	}

	public TreeItem paths2MetaTreeNode(String sab, Paths paths) {
        TreeItem root = buildSourceTree(sab);
		List list = paths.getPaths();
		for (int i=0; i<list.size(); i++) {
			Path path = (Path) list.get(i);
            TreeItem node = path2MetaTreeNode(path);
            TreeItem root_node = searchTree(root, sab, node._code);
            if (root_node != null) {
				for (String assoc : node._assocToChildMap.keySet()) {
					List<TreeItem> children = node._assocToChildMap.get(assoc);
					for (int k=0; k<children.size(); k++) {
						TreeItem child = (TreeItem) children.get(k);
				        root_node.addChild(assoc, child);
					}
				}
			}
		}
		return root;
	}

    public ResolvedConceptReference getRootInSRC(String sab) {
		Atom atom = getHierarchyRoot(sab);
		return new ResolvedConceptReference(
			null,
			null,
			atom.getCode(),
			null,
			atom.getStr());
	}

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // getHierarchyRoots(String sab)
	 public Atom getHierarchyRoot(String sab) {
		 String cui = getSourceHierarchyRoot(sab);
		 Atom atom = metathesaurusDataUtils.getPreferredAtom(cui, sab);
		 return atom;
	 }

        // isExpandable(Atom atom)
	 boolean isExpandable(Atom atom) {
		 return isExpandable(atom.getCui(), atom.getSab());
	 }

	 public Vector<Atom> getSubconcepts(Atom atom) {
		String sab = atom.getSab();
		Vector w = new Vector();

		//buf.append("SELECT ?cui1 ?cui2 ?rel ?rela ?sab {").append("\n");
		Vector v = getRelationships(atom.getCui(), "CHD", atom.getSab());
		if (v == null) return null;
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(line, '|');
			String aui2 = (String) u.elementAt(1);
			String cui2 = (String) u.elementAt(3);
			String str_and_code = metathesaurusDataUtils.getAtomNameAndCodeByAUI(cui2, sab, aui2);
			Vector u2 = gov.nih.nci.evs.restapi.util.StringUtils.parseData(str_and_code, '|');
			Atom sub_atom = new Atom((String) u2.elementAt(1),
			                     (String) u2.elementAt(0),
			                     aui2,
			                     sab, cui2);
			w.add(sub_atom);
		}
		return w;
	}

        // getSuperconcepts(Atom atom)
	public Vector<Atom> getSuperconcepts(Atom atom) {
		String sab = atom.getSab();
		Vector w = new Vector();
		//buf.append("SELECT ?aui1 ?aui2 ?cui1 ?cui2 ?rel ?rela ?sab {").append("\n");
		Vector v = getRelationships(atom.getCui(), "PAR", atom.getSab());
		if (v == null) return null;
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(line, '|');
			String aui1 = (String) u.elementAt(0);
			String cui1 = (String) u.elementAt(2);
			String str_and_code = metathesaurusDataUtils.getAtomNameAndCodeByAUI(cui1, sab, aui1);
			Vector u2 = gov.nih.nci.evs.restapi.util.StringUtils.parseData(str_and_code, '|');
			Atom super_atom = new Atom((String) u2.elementAt(1),
			                     (String) u2.elementAt(0),
			                     aui1,
			                     sab, cui1);
			w.add(super_atom);
		}
		return w;
	}

        // getPathsToRoots(Atom atom)
	public Paths findPathsToRoots(String sab, String cui, String aui) {
		Paths paths = new Paths();
		Stack stack = new Stack();
		String nameAndCode = metathesaurusDataUtils.getAtomNameAndCodeByAUI(cui, sab, aui);
		Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(nameAndCode);
		String str = (String) u.elementAt(0);
		String code = (String) u.elementAt(1);
		stack.push(cui + "$" + aui + "$" + str + "$" + code + "$" + sab);
		while (!stack.isEmpty()) {
			String path = (String) stack.pop();
			u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(path, '|');
			String atom_data_point = (String) u.elementAt(u.size()-1);
			Vector u2 = gov.nih.nci.evs.restapi.util.StringUtils.parseData(atom_data_point, '$');
			Vector v = getNextLevelAtomsBySource((String) u2.elementAt(0),
			                                     (String) u2.elementAt(2),
			                                     sab,
			                                     (String) u2.elementAt(1));
			if (v == null || v.size() == 0) {
				paths.add(createPath(path));
			} else {
				for (int i=0; i<v.size();i++) {
					String t = (String) v.elementAt(i);
					Vector u3 = gov.nih.nci.evs.restapi.util.StringUtils.parseData(t);
					String cui2 = (String) u3.elementAt(0);
					String aui2 = (String) u3.elementAt(1);
					nameAndCode = metathesaurusDataUtils.getAtomNameAndCodeByAUI(cui2, sab, aui2);
					u3 = gov.nih.nci.evs.restapi.util.StringUtils.parseData(nameAndCode);
					String str2 = (String) u3.elementAt(0);
					String code2 = (String) u3.elementAt(1);
					stack.push(t + "|" + cui2 + "$" + aui2 + "$" + str2 + "$" + code2 + "$" + sab);
				}
			}
		}
        return paths;
	}

	public TreeItem metaTreeNode2TreeItem(MetaTreeNode node) {
		String auis = null;
		return new TreeItem(node._code, node._text, auis);
	}

    public Vector<MetaRelationship> sortMetaRelationships(Vector<MetaRelationship> rel_vec, String key) {
		Vector<ExtendedMetaRelationship> v = new Vector<ExtendedMetaRelationship>();
		for (int i=0; i<rel_vec.size(); i++) {
			MetaRelationship meta_rel = (MetaRelationship) rel_vec.elementAt(i);
			String sortKey = meta_rel.getSab();
			if (key.compareTo("cui2") == 0) {
			    sortKey = meta_rel.getCui2();
			} else if (key.compareTo("str") == 0) {
			    sortKey = meta_rel.getStr();
			} else if (key.compareTo("code") == 0) {
			    sortKey = meta_rel.getCode();
			} else if (key.compareTo("tty") == 0) {
			    sortKey = meta_rel.getTty();
			}
			ExtendedMetaRelationship extendedMetaRelationship = new ExtendedMetaRelationship(meta_rel, sortKey);
            v.add(extendedMetaRelationship);
		}
		v = new gov.nih.nci.evs.restapi.util.SortUtils().quickSort(v);
		Vector<MetaRelationship> w = new Vector<MetaRelationship>();
		for (int i=0; i<v.size(); i++) {
			ExtendedMetaRelationship ext_meta_rel = (ExtendedMetaRelationship) v.elementAt(i);
			w.add(ext_meta_rel.getMetaRelationship());
		}
		return w;
	}

    public void runQuery(String query_file) {
		new OWLSPARQLUtils(serviceUrl).runQuery(query_file);
	}

	public String create_in_filter_statement(String var, Vector cuis) {
		StringBuffer buf = new StringBuffer();
		buf.append("filter(str(?cui) in (");
		for (int i=0; i<cuis.size(); i++) {
			String cui = (String) cuis.elementAt(i);
			buf.append("\"" + cui + "\"");
			if (i < cuis.size()-1) {
				buf.append(",");
			}
		}
		buf.append("))");
		return buf.toString();
	}

     public TreeItem clone(TreeItem ti) {
		 TreeItem item = new TreeItem(ti._code, ti._text, ti._auis);
		 List<TreeItem> children = ti._assocToChildMap.get("CHD");
		 if (children != null) {
			 for (int i=0; i<children.size(); i++) {
				 TreeItem existingTreeItem = children.get(i);
				 TreeItem ti_child = clone(existingTreeItem);
				 item.addChild("CHD", ti_child);
				 item._expandable = true;
			 }
		 }
		 return item;
	 }

	 public TreeItem buildViewInHierarchyTree(String cui, String sab) {
		 Vector root_cui_vec = new Vector();
         List list = getRootConcepts(sab);
         for (int i=0; i<list.size(); i++) {
			 ResolvedConceptReference rcr = (ResolvedConceptReference) list.get(i);
			 root_cui_vec.add(rcr.getCode());
		 }
		 HashSet visit_links = new HashSet();
		 TreeItem root = new TreeItem("<Root>", "Root node");
		 Atom atom = metathesaurusDataUtils.getPreferredAtom(cui, sab);
		 TreeItem ti = new TreeItem(cui, atom.getStr(), sab);
         ti._expandable = metathesaurusDataUtils.isExpandable(cui, sab);
		 Vector v = new Vector();
         String parent_code = "root";
		 Stack stack = new Stack();
		 stack.push(ti);
		 while (!stack.isEmpty()) {
			 TreeItem next_ti = (TreeItem) stack.pop();
			 v = getParentRelationships(next_ti._code, sab);
			 if (v != null && v.size() > 0) {
				 for (int i=0; i<v.size(); i++) {
					 String line = (String) v.elementAt(i);
					 Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(line);
					 String cui2 = (String) u.elementAt(3);
					 if (cui2.compareTo(next_ti._code) != 0) {
						 if (!visit_links.contains(next_ti._code + "|" + cui2)) {
							 visit_links.add(next_ti._code + "|" + cui2);
							 atom = metathesaurusDataUtils.getPreferredAtom(cui2, sab);
							 TreeItem parent_ti = new TreeItem(cui2, atom.getStr(), sab);
							 parent_ti.addChild("CHD", next_ti);
							 parent_ti._expandable = true;
							 if (root_cui_vec.contains(cui2)) {
								 root.addChild("CHD", parent_ti);
								 root._expandable = true;
							 } else {
								 stack.push(parent_ti);
							 }
						 } else {
							 //System.out.println("WARNING: Possible loop encountered -- " + next_ti._code + "|" + cui2);
						 }
					 }
				 }
			 } else {
				 root.addChild("CHD", next_ti);
				 root._expandable = true;
			 }
		 }
		 return clone(root);
	 }

     public Vector getParentTreeItems(TreeItem ti) {
		 String assocText = "CHD";
		 String cui = ti._code;
		 String sab = ti._auis;
		 Vector w = new Vector();
         Vector v = getParentRelationships(cui, sab);
         HashSet hset = new HashSet();
         for (int i=0;i<v.size(); i++) {
			 String line = (String) v.elementAt(i);
			 Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(line, '|');
			 String cui2 = (String) u.elementAt(3);
			 String source = (String) u.elementAt(5);
			 if (!hset.contains(cui2 + "|" + source) && cui2.compareTo(cui) != 0) {
				 hset.add(cui2 + "|" + source);
				 String text = metathesaurusDataUtils.getConceptName(cui2, source);
				 TreeItem parent_ti = new TreeItem(cui2, text, source);
				 Vector v2 = getChildRelationships(cui2, source);
				 HashSet hset2 = new HashSet();
				 for (int j=0;j<v2.size(); j++) {
					 String line2 = (String) v2.elementAt(j);
					 Vector u2 = gov.nih.nci.evs.restapi.util.StringUtils.parseData(line2, '|');
					 String sib_cui2 = (String) u2.elementAt(3);
					 String sib_source = (String) u2.elementAt(5);
					 if (!hset2.contains(sib_cui2 + "|" + sib_source) && sib_cui2.compareTo(cui2) != 0) {
						 hset2.add(sib_cui2 + "|" + sib_source);
						 String sib_text = metathesaurusDataUtils.getConceptName(sib_cui2, sib_source);
						 TreeItem sib_ti = new TreeItem(sib_cui2, sib_text, sib_source);
						 sib_ti._expandable = metathesaurusDataUtils.isExpandable(sib_cui2, sib_source);
						 parent_ti.addChild(assocText, sib_ti);
						 parent_ti._expandable = true;
					 }
				 }
				w.add(parent_ti);
			 }
		 }
		 return new gov.nih.nci.evs.restapi.util.SortUtils().quickSort(w);
	 }

	public String construct_source_with_hierarcy_query() {
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX MRHIER: <http://ncicb.nci.nih.gov/" + VIRTUAL_GRAPH_NAME + "/mrhier/>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("SELECT distinct ?sab {").append("\n");
		buf.append("GRAPH <virtual://" + VIRTUAL_GRAPH_NAME + "> {").append("\n");
		buf.append("?s MRHIER:sab ?sab .").append("\n");
		buf.append("}").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}


	public static String construct_distance_one_parents_query(String aui, String cui, String sab) {
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX MRHIER: <http://ncicb.nci.nih.gov/" + VIRTUAL_GRAPH_NAME + "/mrhier/>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("SELECT distinct ?ptr {").append("\n");
		buf.append("   GRAPH <virtual://" + VIRTUAL_GRAPH_NAME + "> {").append("\n");
		buf.append("      ?s MRHIER:ptr ?ptr .").append("\n");
		buf.append("      ?s MRHIER:aui \"" + aui + "\"^^xsd:string .").append("\n");
		buf.append("      ?s MRHIER:cui ?cui .").append("\n");
		buf.append("      ?s MRHIER:cui \"" + cui + "\"^^xsd:string .").append("\n");
		buf.append("      ?s MRHIER:sab ?sab .").append("\n");
		buf.append("      ?s MRHIER:sab \"" + sab + "\"^^xsd:string .").append("\n");
		buf.append("   }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}


    public Vector getDistanceOneParents(String aui, String cui, String sab) {
        Vector v = new Vector();
        String query = construct_distance_one_parents_query(aui, cui, sab);
        v = executeQuery(query);
		if (v == null) {
			System.out.println("v == null");
			return null;
		}
		if (v.size() == 0) {
			System.out.println("v.size() == 0");
			return null;
		}
		v = new ParserUtils().getResponseValues(v);
        return v;
	}

	public Vector getRootAtomData(String sab) {
		Vector roots = getHierarchyRoots(sab);
		String line = (String) roots.elementAt(0);
		Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(line, '|');
		String root_cui = (String) u.elementAt(0);
		Vector root_vec = getChildRelationships(root_cui, sab);
		root_vec = new SortUtils().quickSort(root_vec);
		return root_vec;
	}

	public Vector<Atom> getRootAtoms(String sab) {
		Vector root_vec = getRootAtomData(sab);
		if (root_vec == null) return null;
		Vector<Atom> w = new Vector();
		for (int i=0; i<root_vec.size(); i++) {
			String t = (String) root_vec.elementAt(i);
			Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(t, '|');
			String aui = (String) u.elementAt(1);
			String cui = (String) u.elementAt(3);
			String nameAndCode = metathesaurusDataUtils.getAtomNameAndCodeByAUI(cui, sab, aui);
			Vector u2 = gov.nih.nci.evs.restapi.util.StringUtils.parseData(nameAndCode);
			String str = (String) u2.elementAt(0);
			String code = (String) u2.elementAt(1);
			Atom atom = new Atom(code, str, aui, sab, cui);
			w.add(atom);
		}
		w = new gov.nih.nci.evs.restapi.util.SortUtils().quickSort(w);
		return w;
	}

	public static void main(String[] args) {
        String serviceUrl = args[0];
        MetathesaurusTreeUtils test = new MetathesaurusTreeUtils(serviceUrl);
        System.out.println("serviceUrl: " + serviceUrl);
        String named_graph = args[1];
        String queryfile = args[2];
        test.runQuery(queryfile);
	}

}
