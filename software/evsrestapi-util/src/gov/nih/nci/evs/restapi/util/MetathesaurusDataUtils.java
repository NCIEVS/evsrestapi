package gov.nih.nci.evs.restapi.util;

import gov.nih.nci.evs.restapi.ui.*;

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

 public class MetathesaurusDataUtils {
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
     HashMap rel2LabelHashMap = null;
     String bookmarkUrl = "https://localhost:8080/sparqlncim/ConceptReport.jsp?dictionary=NCI Metathesaurus&code=";
     String applicationName = "sparqlncim";
     HashMap rankMap = null;
     HashMap sab2RootCUIHashMap = null;
     MetathesaurusSearchUtils metathesaurusSearchUtils = null;

	 public MetathesaurusDataUtils(String serviceUrl) {
		this.serviceUrl = verifyServiceUrl(serviceUrl);
        this.httpUtils = new HTTPUtils(serviceUrl, null, null);
        this.jsonUtils = new JSONUtils();
		generator = new UICodeGenerator();
		generator.set_application_name(applicationName);
		generator.set_bookmark_url(bookmarkUrl);
		rel2LabelHashMap = createRel2LabelHashMap();
		rankMap = createRankMap();
        sab2RootCUIHashMap = getSAB2RootCUIHashMap();
        metathesaurusSearchUtils = new MetathesaurusSearchUtils(serviceUrl);
	 }

	 public int getRank(String sab, String tty) {
	    Integer int_obj = (Integer) rankMap.get(sab + "|" + tty);
	    return int_obj.intValue();
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

	 public HashMap createRel2LabelHashMap() {
		 HashMap hmap = new HashMap();
		 hmap.put("PAR", "Parent");
		 hmap.put("CHD", "Child");
		 hmap.put("RB", "Broader");
		 hmap.put("RN", "Narrower");
		 hmap.put("SIB", "Sibling");
		 hmap.put("OTH", "Other");
		 return hmap;
	 }

	 public String getRelLabel(String rel) {
		 if (rel2LabelHashMap.containsKey(rel)) {
			 return (String) rel2LabelHashMap.get(rel);
		 }
		 return "Other";
	 }

     public String getMatchFilter(String var, String algorithm, String searchString) {
		searchString = searchString.toLowerCase();
		if (algorithm.compareTo(EXACT_MATCH) == 0) {
			return "    FILTER (lcase(?" + var + ") = \"" + searchString + "\")";
		} else if (algorithm.compareTo(STARTS_WITH) == 0) {
			return "    FILTER (strstarts(lcase(?" + var + "), \"" + searchString + "\"))";
		} else if (algorithm.compareTo(ENDS_WITH) == 0) {
			return "    FILTER (strends(lcase(?" + var + "), \"" + searchString + "\"))";
		} else if (algorithm.compareTo(CONTAINS) == 0) {
			return "    FILTER (contains(lcase(?" + var + "), \"" + searchString + "\"))";
		}
		return "";
	 }

     public void apply_text_match(StringBuffer buf, String param, String searchString, String algorithm) {
		searchString = searchString.toLowerCase();
		if (algorithm.compareTo(EXACT_MATCH) == 0) {
			buf.append("		FILTER (lcase(str(?" + param + ")) = \"" + searchString + "\")").append("\n");
		} else if (algorithm.compareTo(STARTS_WITH) == 0) {
			buf.append("		FILTER (regex(str(?"+ param +"),'^" + searchString + "','i'))").append("\n");
		} else if (algorithm.compareTo(ENDS_WITH) == 0) {
			buf.append("		FILTER (regex(str(?"+ param +"),'" + searchString + "^','i'))").append("\n");
		} else {
			searchString = searchString.replaceAll("%20", " ");
			searchString = SPARQLSearchUtils.createSearchString(searchString, algorithm);
			buf.append("(?"+ param +") <tag:stardog:api:property:textMatch> (" + searchString + ").").append("\n");
		}
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

	public static String construct_semantic_type_query() {
		StringBuffer buf = new StringBuffer();
		//buf.append("PREFIX sm: <tag:stardog:api:mapping:>").append("\n");
		buf.append("PREFIX MRSTY: <http://ncicb.nci.nih.gov/" + VIRTUAL_GRAPH_NAME + "/mrsty/>").append("\n");
		//buf.append("PREFIX dc:<http://purl.org/dc/elements/1.1/>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("SELECT distinct ?sty {").append("\n");
		buf.append("   GRAPH <virtual://" + VIRTUAL_GRAPH_NAME + "> {").append("\n");
		//buf.append("      ?s a :MRSTY .").append("\n");
		buf.append("      ?s MRSTY:sty ?sty .").append("\n");
		buf.append("   }").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		return buf.toString();
	}

	public static String construct_mrrank_query() {
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX MRRANK: <http://ncicb.nci.nih.gov/" + VIRTUAL_GRAPH_NAME + "/mrrank/>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("SELECT distinct ?rank2 ?sab ?tty ?suppress {").append("\n");
		buf.append("   GRAPH <virtual://" + VIRTUAL_GRAPH_NAME + "> {").append("\n");
		buf.append("      ?s MRRANK:sab ?sab .").append("\n");
		buf.append("      ?s MRRANK:tty ?tty .").append("\n");
		buf.append("      ?s MRRANK:rank2 ?rank2 .").append("\n");
		buf.append("      ?s MRRANK:suppress ?suppress .").append("\n");
		buf.append("   }").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		return buf.toString();
	}

    public Vector getRankData() {
        Vector v = new Vector();
        String query = construct_mrrank_query();
        v = executeQuery(query);
		if (v == null || v.size() == 0) return null;
		v = new ParserUtils().getResponseValues(v);
        return v;
	}


	public static String construct_term_type_query() {
		StringBuffer buf = new StringBuffer();
		//buf.append("PREFIX sm: <tag:stardog:api:mapping:>").append("\n");
		buf.append("PREFIX MRRANK: <http://ncicb.nci.nih.gov/" + VIRTUAL_GRAPH_NAME + "/mrrank/>").append("\n");
		//buf.append("PREFIX dc:<http://purl.org/dc/elements/1.1/>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("SELECT distinct ?tty {").append("\n");
		buf.append("   GRAPH <virtual://" + VIRTUAL_GRAPH_NAME + "> {").append("\n");
		//buf.append("      ?s a :MRRANK .").append("\n");
		//buf.append("      ?s MRRANK:rank ?rank .").append("\n");
		//buf.append("      ?s MRRANK:sab ?sab .").append("\n");
		//buf.append("      ?s MRRANK:suppress ?suppress .").append("\n");
		buf.append("      ?s MRRANK:tty ?tty .").append("\n");
		buf.append("   }").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		return buf.toString();
	}

	public static String construct_sab_query() {
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX MRRANK: <http://ncicb.nci.nih.gov/" + VIRTUAL_GRAPH_NAME + "/mrrank/>").append("\n");
		buf.append("SELECT distinct ?sab {").append("\n");
		buf.append("   GRAPH <virtual://" + VIRTUAL_GRAPH_NAME + "> {").append("\n");
		buf.append("      ?s MRRANK:sab ?sab .").append("\n");
		buf.append("   }").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		return buf.toString();
	}


	public static String construct_rel_query(String sab) {
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX MRREL: <http://ncicb.nci.nih.gov/" + VIRTUAL_GRAPH_NAME + "/mrrel/>").append("\n");
		//buf.append("PREFIX dc:<http://purl.org/dc/elements/1.1/>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("SELECT distinct ?rel {").append("\n");
		buf.append("   GRAPH <virtual://" + VIRTUAL_GRAPH_NAME + "> {").append("\n");
		//buf.append("      ?s a :MRREL .").append("\n");
		buf.append("      ?s MRREL:rel ?rel .").append("\n");
		buf.append("      ?s MRREL:sab \"" + sab + "\"^^xsd:string .").append("\n");

		buf.append("   }").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		return buf.toString();
	}

	public static String construct_rela_query(String sab) {
		StringBuffer buf = new StringBuffer();
		//buf.append("PREFIX sm: <tag:stardog:api:mapping:>").append("\n");
		buf.append("PREFIX MRREL: <http://ncicb.nci.nih.gov/" + VIRTUAL_GRAPH_NAME + "/mrrel/>").append("\n");
		//buf.append("PREFIX dc:<http://purl.org/dc/elements/1.1/>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("SELECT distinct ?rela {").append("\n");
		buf.append("   GRAPH <virtual://" + VIRTUAL_GRAPH_NAME + "> {").append("\n");
		//buf.append("      ?s a :MRREL .").append("\n");
		buf.append("      ?s MRREL:rela ?rela .").append("\n");
		buf.append("      ?s MRREL:sab \"" + sab + "\"^^xsd:string .").append("\n");
		buf.append("   }").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		return buf.toString();
	}

	public static String construct_atn_query(String sab) {
		StringBuffer buf = new StringBuffer();
		//buf.append("PREFIX sm: <tag:stardog:api:mapping:>").append("\n");
		buf.append("PREFIX MRSAT: <http://ncicb.nci.nih.gov/" + VIRTUAL_GRAPH_NAME + "/mrsat/>").append("\n");
		//buf.append("PREFIX dc:<http://purl.org/dc/elements/1.1/>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("SELECT distinct ?atn {").append("\n");
		buf.append("   GRAPH <virtual://" + VIRTUAL_GRAPH_NAME + "> {").append("\n");
		//buf.append("      ?s a :MRSAT .").append("\n");
		buf.append("      ?s MRSAT:atn ?atn .").append("\n");
		buf.append("      ?s MRSAT:sab \"" + sab + "\"^^xsd:string .").append("\n");
		buf.append("   }").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		return buf.toString();
	}



//	 ==================================================================================================================

	 public String generatePropertyTable(String cui, HashMap property_hashmap) {
		 if (generator == null) {
			 return null;
		 }
		 return generator.generatePropertyTable(cui, property_hashmap);
	 }

	 public String generateSynonymTable(String cui, Vector syn_vec) {
		//syn_vec = new gov.nih.nci.evs.restapi.util.SortUtils().quickSort();
		return generator.generateSynonymTable(cui, syn_vec, null);
	 }

	 public String generateSynonymTable(String cui, Vector syn_vec, Vector sourceWithHierarchy_vec) {
		syn_vec = new gov.nih.nci.evs.restapi.util.SortUtils().quickSort(syn_vec);
		return generator.generateSynonymTable(cui, syn_vec, sourceWithHierarchy_vec);
	 }

	 public String generateSynonymTable(String cui, Vector syn_vec, Vector sourceWithHierarchy_vec, Vector codingSchemes) {
		syn_vec = new gov.nih.nci.evs.restapi.util.SortUtils().quickSort(syn_vec);
		return generator.generateSynonymTable(cui, syn_vec, sourceWithHierarchy_vec, codingSchemes);
	 }

	 public String generateRelationshipTable(HashMap hmap) {
		return generator.generateRelationshipTable(hmap);
	 }

	 public String generateSourceTable(String cui, String source, Vector source_vec) {
		return generator.generateSourceTable(cui, source, source_vec);
	 }

	 public String generateSourceSelection(String code, Vector v) {
		 Vector sab_vec = new Vector();
		 for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(t, '|');
			String sab = (String) u.elementAt(4);
			if (!sab_vec.contains(sab)) {
				sab_vec.add(sab);
			}
		 }
		 sab_vec = new gov.nih.nci.evs.restapi.util.SortUtils().quickSort(sab_vec);
		 String label = getConceptName(code);
		 return generator.generateSourceSelection(code, label, (String) sab_vec.elementAt(0), sab_vec);
	 }


	 public String generateSourceSelection(String code, String sab, Vector v) {
		 Vector sab_vec = new Vector();
		 for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(t, '|');
			String sab2 = (String) u.elementAt(4);
			if (!sab_vec.contains(sab2)) {
				sab_vec.add(sab2);
			}
		 }
		 sab_vec = new gov.nih.nci.evs.restapi.util.SortUtils().quickSort(sab_vec);
		 String label = getConceptName(code);
		 if (sab == null) {
			 sab = "NCI";//(String) sab_vec.elementAt(0);
		 }

		 return generator.generateSourceSelection(code, label, sab, sab_vec);
	 }

//===========================================================================================================================================

	public static String construct_mrconso_query(String cui) {
		return construct_mrconso_query(cui, null);
	}

	public static String construct_mrconso_query(String cui, String sab) {
		return construct_mrconso_query(cui, sab, null, null);
	}

	public static String construct_mrconso_query(String cui, String sab, String code) {
		return construct_mrconso_query(cui, sab, code, null, null);
	}

	public static String construct_mrconso_query(String cui, String sab, String suppress, String tty) {
		return construct_mrconso_query(cui, sab, null, suppress, tty);
	}

	public static String construct_mrconso_query(String cui, String sab, String code, String suppress, String tty) {
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX MRCONSO: <http://ncicb.nci.nih.gov/" + VIRTUAL_GRAPH_NAME + "/mrconso/>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("SELECT ?aui ?code ?cui ?ispref ?lat ?lui ?sab ?str ?stt ?suppress ?tty {").append("\n");
		buf.append("GRAPH <virtual://" + VIRTUAL_GRAPH_NAME + "> {").append("\n");
        buf.append("?s MRCONSO:aui ?aui .").append("\n");
        buf.append("?s MRCONSO:code ?code .").append("\n");
		if (code != null) {
			buf.append("?s MRCONSO:code \"" + code + "\"^^xsd:string .").append("\n");
		}

		buf.append("?s MRCONSO:cui ?cui .").append("\n");
		buf.append("?s MRCONSO:cui \"" + cui + "\"^^xsd:string .").append("\n");

		buf.append("?s MRCONSO:ispref ?ispref .").append("\n");
		buf.append("?s MRCONSO:lat ?lat .").append("\n");
		buf.append("?s MRCONSO:lui ?lui .").append("\n");
		buf.append("?s MRCONSO:str ?str .").append("\n");
		buf.append("?s MRCONSO:stt ?stt .").append("\n");

		buf.append("?s MRCONSO:suppress ?suppress .").append("\n");
		if (suppress != null) {
			buf.append("?s MRCONSO:suppress \"" + suppress + "\"^^xsd:string .").append("\n");
		}

		buf.append("?s MRCONSO:tty ?tty .").append("\n");
		if (tty != null) {
			buf.append("?s MRCONSO:tty \"" + tty + "\"^^xsd:string .").append("\n");
		}

		buf.append("?s MRCONSO:sab ?sab .").append("\n");
		if (sab != null) {
			buf.append("?s MRCONSO:sab \"" + sab + "\"^^xsd:string .").append("\n");
		}

		buf.append("}").append("\n");
		buf.append("}").append("\n");

		return buf.toString();
	}

	public static String construct_aui_query(String sab, String cui, String code) {
		StringBuffer buf = new StringBuffer();
		//buf.append("PREFIX sm: <tag:stardog:api:mapping:>").append("\n");
		buf.append("PREFIX MRCONSO: <http://ncicb.nci.nih.gov/" + VIRTUAL_GRAPH_NAME + "/mrconso/>").append("\n");
		//buf.append("PREFIX dc:<http://purl.org/dc/elements/1.1/>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("SELECT distinct ?aui {").append("\n");
		buf.append("   GRAPH <virtual://" + VIRTUAL_GRAPH_NAME + "> {").append("\n");
		//buf.append("      ?s a :MRCONSO .").append("\n");
		buf.append("      ?s MRCONSO:cui \"" + cui + "\"^^xsd:string .").append("\n");
		buf.append("      ?s MRCONSO:aui ?aui .").append("\n");
		buf.append("      ?s MRCONSO:code \"" + code + "\"^^xsd:string .").append("\n");
		buf.append("      ?s MRCONSO:sab \"" + sab + "\"^^xsd:string .").append("\n");
		buf.append("   }").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		return buf.toString();
	}

    public String getAUIByCode(String sab, String cui, String code) {
        Vector v = new Vector();
        String query = construct_aui_query(sab, cui, code);
        v = executeQuery(query);
		if (v == null || v.size() == 0) return null;
		v = new ParserUtils().getResponseValues(v);
        return (String) v.elementAt(0);
	}


    //Find all atoms of a UMLS concept.
    public Vector getAtomData(String cui) {
        Vector v = new Vector();
        String query = construct_mrconso_query(cui);
        v = executeQuery(query);
		if (v == null || v.size() == 0) return null;
		v = new ParserUtils().getResponseValues(v);
        return v;
	}

    public Vector getAtomData(String cui, String sab) {
        Vector v = new Vector();
        String query = construct_mrconso_query(cui, sab);
        v = executeQuery(query);
		if (v == null || v.size() == 0) return null;
		v = new ParserUtils().getResponseValues(v);
        return v;
	}

    public Vector getAtomData(String cui, String sab, String code) {
        Vector v = new Vector();
        String query = construct_mrconso_query(cui, sab, code);
        v = executeQuery(query);
		if (v == null || v.size() == 0) return null;
		v = new ParserUtils().getResponseValues(v);
        return v;
	}

    public Vector getAtomData(String cui, String sab, String suppress, String tty) {
        Vector v = new Vector();
        String query = construct_mrconso_query(cui, sab, suppress, tty);
        v = executeQuery(query);
		if (v == null || v.size() == 0) return null;
		v = new ParserUtils().getResponseValues(v);
        return v;
	}

    //Find all source definitions associated with a UMLS concept.
	public static String construct_mrdef_query(String cui) {
		StringBuffer buf = new StringBuffer();
		//buf.append("PREFIX sm: <tag:stardog:api:mapping:>").append("\n");
		buf.append("PREFIX MRDEF: <http://ncicb.nci.nih.gov/" + VIRTUAL_GRAPH_NAME + "/mrdef/>").append("\n");
		//buf.append("PREFIX dc:<http://purl.org/dc/elements/1.1/>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		//buf.append("SELECT ?atui ?aui ?cui ?cvf ?def ?sab ?satui ?suppress {").append("\n");

		buf.append("SELECT ?cui ?def ?sab {").append("\n");

		buf.append("   GRAPH <virtual://" + VIRTUAL_GRAPH_NAME + "> {").append("\n");
		//buf.append("      ?s a :MRDEF .").append("\n");
		//buf.append("      ?s MRDEF:atui ?atui .").append("\n");
		//buf.append("      ?s MRDEF:aui ?aui .").append("\n");
		buf.append("      ?s MRDEF:cui ?cui .").append("\n");
		buf.append("      ?s MRDEF:cui \"" + cui + "\"^^xsd:string .").append("\n");
		//buf.append("      ?s MRDEF:cvf ?cvf .").append("\n");
		buf.append("      ?s MRDEF:def ?def .").append("\n");
		buf.append("      ?s MRDEF:sab ?sab .").append("\n");
		//buf.append("      ?s MRDEF:satui ?satui .").append("\n");
		//buf.append("      ?s MRDEF:suppress ?suppress .").append("\n");
		buf.append("   }").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		return buf.toString();
	}

    public Vector getDefinitions(String cui) {
        Vector v = new Vector();
        String query = construct_mrdef_query(cui);
        v = executeQuery(query);
		if (v == null || v.size() == 0) return null;
		v = new ParserUtils().getResponseValues(v);
        return v;
	}


    public static String construct_mrsat_query(String cui) {
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX MRSAT: <http://ncicb.nci.nih.gov/" + VIRTUAL_GRAPH_NAME + "/mrsat/>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("SELECT ?atn ?atui ?atv ?code ?cui  ?lui ?metaui ?sab ?satui ?stype ?sui ?suppress ?cvf ?satui {").append("\n");
		buf.append("GRAPH <virtual://" + VIRTUAL_GRAPH_NAME + "> {").append("\n");
		buf.append("?s MRSAT:atn ?atn .").append("\n");
		buf.append("?s MRSAT:atui ?atui .").append("\n");
		buf.append("?s MRSAT:atv ?atv .").append("\n");
		buf.append("?s MRSAT:code ?code .").append("\n");
		buf.append("?s MRSAT:cui ?cui .").append("\n");
		buf.append("?s MRSAT:cui \"" + cui + "\"^^xsd:string .").append("\n");
		buf.append("?s MRSAT:lui ?lui .").append("\n");
		buf.append("?s MRSAT:metaui ?metaui .").append("\n");
		buf.append("?s MRSAT:sab ?sab .").append("\n");
		buf.append("?s MRSAT:stype ?stype .").append("\n");
		buf.append("?s MRSAT:sui ?sui .").append("\n");
		buf.append("?s MRSAT:suppress ?suppress .").append("\n");
		buf.append("").append("\n");
		buf.append("OPTIONAL {").append("\n");
		buf.append("?s MRSAT:cvf ?cvf .").append("\n");
		buf.append("?s MRSAT:satui ?satui .").append("\n");
		buf.append("}").append("\n");
		buf.append("}").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	// Find all attributes for a UMLS concept.
    public Vector getAttributes(String cui) {
        Vector v = new Vector();
        String query = construct_mrsat_query(cui);
        v = executeQuery(query);
		if (v == null || v.size() == 0) {
			return null;
		}
		v = new ParserUtils().getResponseValues(v);
		v = new gov.nih.nci.evs.restapi.util.SortUtils().quickSort(v);
        return v;
	}

	public static String construct_mrsty_query(String cui) {
		StringBuffer buf = new StringBuffer();
		//buf.append("PREFIX sm: <tag:stardog:api:mapping:>").append("\n");
		buf.append("PREFIX MRSTY: <http://ncicb.nci.nih.gov/" + VIRTUAL_GRAPH_NAME + "/mrsty/>").append("\n");
		//buf.append("PREFIX dc:<http://purl.org/dc/elements/1.1/>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("SELECT ?atui ?cui ?cvf ?stn ?sty ?tui {").append("\n");
		buf.append("   GRAPH <virtual://" + VIRTUAL_GRAPH_NAME + "> {").append("\n");
		//buf.append("      ?s a :MRSTY .").append("\n");
		buf.append("      ?s MRSTY:atui ?atui .").append("\n");
		buf.append("      ?s MRSTY:cui \"" + cui + "\"^^xsd:string .").append("\n");
		buf.append("      ?s MRSTY:cvf ?cvf .").append("\n");
		buf.append("      ?s MRSTY:stn ?stn .").append("\n");
		buf.append("      ?s MRSTY:sty ?sty .").append("\n");
		buf.append("      ?s MRSTY:tui ?tui .").append("\n");
		buf.append("   }").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		return buf.toString();
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

	public static String construct_label_query(String cui, String sab, String aui) {
		//String tty = "PT";
		//String tty = null;
		//String stt = "PF";

		String tty = null;
		String stt = null;

		String ispref = "Y";
		String lat = "ENG";
		return construct_label_query(cui, sab, aui, tty, stt, ispref, lat);
	}


    public String getConceptName(String cui) {
        Vector v = getSortedAtomData(cui, null);
        return ((AtomData) v.elementAt(v.size()-1)).getStr();
	}

    public String getConceptName(String cui, String sab) {
        Vector v = getSortedAtomData(cui, sab);
        return ((AtomData) v.elementAt(v.size()-1)).getStr();
	}

	public static String construct_label_query(String cui, String sab, String aui, String tty, String stt, String ispref, String lat) {
		StringBuffer buf = new StringBuffer();
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

	public static String construct_related_concepts_query(String cui) {
		String ts = "P";
		String stt = "PF";
		String ispref = "Y";
		String lat = "ENG";
		return construct_related_concepts_query(cui, ts, stt, ispref, lat);
	}

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

		buf.append("      FILTER (?cui2 = ?cui)").append("\n");

		buf.append("   }").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		return buf.toString();
	}

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

     public gov.nih.nci.evs.restapi.bean.Definition toDefinition(String t) {
		 Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(t, '|');
		 return new gov.nih.nci.evs.restapi.bean.Definition((String) u.elementAt(4),
		                       (String) u.elementAt(5));
	 }

     public gov.nih.nci.evs.restapi.bean.Synonym toSynonym(String t) {

         //buf.append("SELECT ?aui ?code ?cui ?ispref ?lat ?lui ?sab ?str ?stt ?suppress ?tty {").append("\n");     public gov.nih.nci.evs.restapi.bean.Synonym toSynonym(String t) {
		 Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(t, '|');
		 return new gov.nih.nci.evs.restapi.bean.Synonym(
			                   (String) u.elementAt(2),
			                   null,
			                   (String) u.elementAt(7),
			                   (String) u.elementAt(10),
			                   (String) u.elementAt(6),
		                       (String) u.elementAt(1),
		                        null,
		                        null
		                       );
	 }

	 //SELECT ?atn ?atui ?atv ?code ?cui ?cvf ?lui ?metaui ?sab ?satui ?stype ?sui ?suppress {
     public gov.nih.nci.evs.restapi.bean.Property toProperty(String t) {
		 Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(t, '|');
		 return new gov.nih.nci.evs.restapi.bean.Property(
			                   (String) u.elementAt(0),
		                       (String) u.elementAt(2),
		                       (String) u.elementAt(8)
		                       );
	 }


///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	 public String generatePropertyTable(String cui) {
	    gov.nih.nci.evs.restapi.bean.Property prop = null;
	    gov.nih.nci.evs.restapi.bean.Definition def = null;
	    gov.nih.nci.evs.restapi.bean.Synonym syn = null;

	    HashMap property_hashmap = new HashMap();
        Vector v = null;
	    Vector w = getAttributes(cui);
//		buf.append("SELECT ?atn ?atui ?atv ?code ?cui  ?lui ?metaui ?sab ?satui ?stype ?sui ?suppress ?cvf ?satui {").append("\n");

	    if (w != null && w.size() > 0) {
			v = new Vector();
			for (int i=0; i<w.size(); i++) {
				String t = (String) w.elementAt(i);
				Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(t, '|');
				String name = (String) u.elementAt(0);
				String value = (String) u.elementAt(2);
				String source = (String) u.elementAt(7);
				prop = new gov.nih.nci.evs.restapi.bean.Property(name, value, source);
				v.add(prop);
			}
			property_hashmap.put("Property", v);

			v = new Vector();
			for (int i=0; i<w.size(); i++) {
				String t = (String) w.elementAt(i);
				Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(t, '|');
				String name = (String) u.elementAt(0);
				if (name.compareTo("Definition") == 0) {
					String value = (String) u.elementAt(2);
					String sab = (String) u.elementAt(8);
					def = new gov.nih.nci.evs.restapi.bean.Definition(value, sab);
					v.add(def);
				}
			}
			property_hashmap.put("Definition", v);
		}
        String label = getConceptName(cui);
		w = getAtomData(cui);

		if (w != null && w.size() > 0) {
			v = new Vector();
			for (int i=0; i<w.size(); i++) {
				String t = (String) w.elementAt(i);
				syn = toSynonym(t);
				syn.setLabel(label);
				v.add(syn);
			}
			v = new gov.nih.nci.evs.restapi.util.SortUtils().quickSort(v);
			property_hashmap.put("Synonym", v);

		}
		return generatePropertyTable(cui, property_hashmap);
	 }

	 public String generateRelationshipTable(String cui) {
		Vector v =  getRelatedConcepts(cui);
		if (v == null || v.size() == 0) {
			return null;
		}
	    HashMap hmap = new HashMap();
	    //String[] rels = new String[] {"Parent", "Child", "Broader", "Narrower", "Sibling", "Other"};
	    String[] rels = new String[] {"PAR", "CHD", "RB", "RN", "SIB"};
	    List<String> list = Arrays.asList(rels);

	    for (int k=0; k<rels.length; k++) {
			String relationship = rels[k];
			Vector w = new Vector();
			for (int i=0; i<v.size(); i++) {
				MetaRelationship t = (MetaRelationship) v.elementAt(i);
				String rel = t.getRel();
				if (rel.compareToIgnoreCase(relationship) == 0) {
					String rela = t.getRela();
					String rel_cui = t.getCui2();
					String term = t.getStr();
					String sab = t.getSab();
					String type = t.getTty();
					String code = t.getCode();
				    w.add(rel + "|" + rela + "|" + rel_cui + "|" + term + "|" + sab + "|" + type + "|" + code);
				}
			}
			hmap.put(relationship, w);
		}
        Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			MetaRelationship t = (MetaRelationship) v.elementAt(i);
			String rel = t.getRel();
			if (!list.contains(rel)) {
				String rela = t.getRela();
				String rel_cui = t.getCui2();
				String term = t.getStr();
				String sab = t.getSab();
				String type = t.getTty();
				String code = t.getCode();
				w.add(rel + "|" + rela + "|" + rel_cui + "|" + term + "|" + sab + "|" + type + "|" + code);
			}
		}
		hmap.put("OTH", w);
		return generateRelationshipTable(hmap);
	 }


	 public String generateSourceTable(String cui, Vector v) {
	    Vector source_vec = new Vector();
	    String[] rels = new String[] {"PAR", "CHD", "RB", "RN", "SIB"};
	    List<String> list = Arrays.asList(rels);

	    for (int k=0; k<rels.length; k++) {
			String relationship = rels[k];
			//Vector w = new Vector();
			for (int i=0; i<v.size(); i++) {
				String t = (String) v.elementAt(i);
				Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(t, '|');
				String rel = (String) u.elementAt(0);
				if (rel.compareToIgnoreCase(relationship) == 0) {
					String rela = (String) u.elementAt(1);
					String rel_cui = (String) u.elementAt(2);
					String term = (String) u.elementAt(3);
					String sab = (String) u.elementAt(4);
					String type = (String) u.elementAt(5);
					String code = (String) u.elementAt(6);
				    source_vec.add(rel + "|" + rela + "|" + rel_cui + "|" + term + "|" + sab + "|" + type + "|" + code);
				}
			}
		}

		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(t, '|');
			String rel = (String) u.elementAt(0);
			if (!list.contains(rel)) {
				String rela = (String) u.elementAt(1);
				String rel_cui = (String) u.elementAt(2);
				String term = (String) u.elementAt(3);
				String sab = (String) u.elementAt(4);
				String type = (String) u.elementAt(5);
				String code = (String) u.elementAt(6);
				source_vec.add(rel + "|" + rela + "|" + rel_cui + "|" + term + "|" + sab + "|" + type + "|" + code);
			}
		}
		return generateSourceTable(cui, "NCI", source_vec);
	 }

	 public String generateSourceTable(String cui) {
	    Vector v = getAtomsInRelatedConcepts(cui);
	    return generateSourceTable(cui, v);
	 }


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

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static String construct_mrconso_query(String cui, String sab, String ts, String stt, String ispref, String lat) {
		StringBuffer buf = new StringBuffer();
		//buf.append("PREFIX sm: <tag:stardog:api:mapping:>").append("\n");
		buf.append("PREFIX MRCONSO: <http://ncicb.nci.nih.gov/" + VIRTUAL_GRAPH_NAME + "/mrconso/>").append("\n");
		//buf.append("PREFIX dc:<http://purl.org/dc/elements/1.1/>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
        buf.append("SELECT ?aui, ?str ?code ?sab ?tty {").append("\n");
        buf.append("   GRAPH <virtual://" + VIRTUAL_GRAPH_NAME + "> {").append("\n");
		//buf.append("      ?s a :MRCONSO .").append("\n");
		buf.append("      ?s MRCONSO:aui ?aui .").append("\n");
		buf.append("      ?s MRCONSO:code ?code .").append("\n");
		buf.append("      ?s MRCONSO:cui ?cui .").append("\n");
		buf.append("      ?s MRCONSO:tty ?tty .").append("\n");
		buf.append("      ?s MRCONSO:cui \"" + cui + "\"^^xsd:string .").append("\n");
		buf.append("      ?s MRCONSO:ispref \"" + ispref + "\"^^xsd:string .").append("\n");
		buf.append("      ?s MRCONSO:lat \"" + lat + "\"^^xsd:string .").append("\n");
		buf.append("      ?s MRCONSO:sab ?sab .").append("\n");
		if (sab != null) {
			buf.append("      ?s MRCONSO:sab \"" + sab + "\"^^xsd:string .").append("\n");
		}
		buf.append("      ?s MRCONSO:str ?str .").append("\n");
		buf.append("      ?s MRCONSO:stt \"" + stt + "\"^^xsd:string .").append("\n");
		buf.append("      ?s MRCONSO:ts \"" + ts + "\"^^xsd:string .").append("\n");
		buf.append("   }").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		return buf.toString();
	}

    public Atom createAtom(String cui, String t) {
		Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(t, '|');
		return new Atom((String) u.elementAt(2),
		                (String) u.elementAt(1),
		                (String) u.elementAt(0),
		                (String) u.elementAt(3),
		                cui,
		                (String) u.elementAt(4));
	}


    public Vector getAtomsInCUI(String cui, String sab) {
        Vector w = new Vector();
        String query = construct_mrconso_query(cui, sab);
        Vector v = executeQuery(query);
		if (v == null || v.size() == 0) return null;
		v = new ParserUtils().getResponseValues(v);
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
            Atom atom = createAtom(cui, t);
            w.add(atom);
		}
		w = new gov.nih.nci.evs.restapi.util.SortUtils().quickSort(w);
		return w;
	}

//buf.append("SELECT ?aui, ?str ?code ?sab {").append("\n");
    public Vector getAtomsInCUI(String cui) {
		return getAtomsInCUI(cui, null);
	}


	public static String construct_mrconso_query(String cui, String sab, String aui, String ts, String stt, String ispref, String lat) {
		StringBuffer buf = new StringBuffer();
		//buf.append("PREFIX sm: <tag:stardog:api:mapping:>").append("\n");
		buf.append("PREFIX MRCONSO: <http://ncicb.nci.nih.gov/" + VIRTUAL_GRAPH_NAME + "/mrconso/>").append("\n");
		//buf.append("PREFIX dc:<http://purl.org/dc/elements/1.1/>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("SELECT ?str ?code ?tty {").append("\n");
		buf.append("   GRAPH <virtual://" + VIRTUAL_GRAPH_NAME + "> {").append("\n");
		//buf.append("      ?s a :MRCONSO .").append("\n");
		buf.append("      ?s MRCONSO:aui ?aui .").append("\n");
		buf.append("      ?s MRCONSO:aui \"" + cui + "\"^^xsd:string .").append("\n");
		buf.append("      ?s MRCONSO:code ?code .").append("\n");
		buf.append("      ?s MRCONSO:tty ?tty .").append("\n");
		buf.append("      ?s MRCONSO:cui ?cui .").append("\n");
		buf.append("      ?s MRCONSO:cui \"" + cui + "\"^^xsd:string .").append("\n");
		buf.append("      ?s MRCONSO:ispref \"" + ispref + "\"^^xsd:string .").append("\n");
		buf.append("      ?s MRCONSO:lat \"" + lat + "\"^^xsd:string .").append("\n");
		buf.append("      ?s MRCONSO:sab ?sab .").append("\n");
		buf.append("      ?s MRCONSO:sab \"" + sab + "\"^^xsd:string .").append("\n");
		buf.append("      ?s MRCONSO:str ?str .").append("\n");
		buf.append("      ?s MRCONSO:stt \"" + stt + "\"^^xsd:string .").append("\n");
		buf.append("      ?s MRCONSO:ts \"" + ts + "\"^^xsd:string .").append("\n");

		buf.append("   }").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		return buf.toString();
	}


	public static String construct_atom_label_and_code_query(String cui, String sab, String aui) {
		String ts = "P";
		String stt = "PF";
		String ispref = "Y";
		String lat = "ENG";
		return construct_mrconso_query(cui, sab, aui, ts, stt, ispref, lat);
	}

    public Vector getAtomLabelAndCode(String cui, String sab, String aui) {
        Vector v = new Vector();
        String query = construct_atom_label_and_code_query(cui, sab, aui);
        v = executeQuery(query);
		if (v == null || v.size() == 0) return null;
		v = new ParserUtils().getResponseValues(v);
		return v;
	}

	public static String construct_mrrel_query(String cui, String rel, String sab) {
		return construct_mrrel_query(cui, rel, sab, null);
	}

	public static String construct_mrrel_query(String cui, String rel, String sab, String aui) {
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX MRREL: <http://ncicb.nci.nih.gov/" + VIRTUAL_GRAPH_NAME + "/mrrel/>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("SELECT ?aui1 ?aui2 ?cui1 ?cui2 ?rel ?rela ?sab {").append("\n");
		buf.append("   GRAPH <virtual://" + VIRTUAL_GRAPH_NAME + "> {").append("\n");
		buf.append("      ?s MRREL:aui1 ?aui1 .").append("\n");
		if (aui != null) {
		    buf.append("      ?s MRREL:aui1 \"" + aui + "\"^^xsd:string .").append("\n");
	    }
		buf.append("      ?s MRREL:aui2 ?aui2 .").append("\n");
		buf.append("      ?s MRREL:cui1 ?cui1 .").append("\n");
		buf.append("      ?s MRREL:cui1 \"" + cui + "\"^^xsd:string .").append("\n");
		buf.append("      ?s MRREL:cui2 ?cui2 .").append("\n");
		buf.append("      ?s MRREL:rel ?rel .").append("\n");

		if (rel != null) {
		    buf.append("      ?s MRREL:rel \"" + rel + "\"^^xsd:string .").append("\n");
	    }
		buf.append("      ?s MRREL:sab ?sab .").append("\n");
		if (sab != null) {
			buf.append("      ?s MRREL:sab \"" + sab + "\"^^xsd:string .").append("\n");
		}
		buf.append("   }").append("\n");
 		buf.append("OPTIONAL {").append("\n");
        buf.append("      ?s MRREL:rela ?rela .").append("\n");
		buf.append("}").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		return buf.toString();
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
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(t, '|');
			String u0 = (String) u.elementAt(0);
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

    public Vector getChildRelationships(String cui, String sab) {
		return getRelationships(cui, "CHD", sab);
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


	public boolean isExpandable(String cui, String sab) {
		Vector w = getRelationships(cui, "CHD", sab);
		if (w == null || w.size() == 0) return false;
		return true;
	}


    public MetaTreeNode expandSourceTree(Atom atom) {
		return expandSourceTree(atom.getSab(), atom.getCui(), atom.getAui(), atom.getCode(), atom.getStr());
	}


    public MetaTreeNode expandSourceTree(String sab, String cui, String aui, String code, String name) {
		//boolean searchInactive = true;
		//Vector v = getSubconceptsInSource(sab, cui, code);
		String assoc = "CHD";
		//buf.append("SELECT distinct ?cui2, ?aui2 {
		Vector v = getNextLevelAtomsBySource(cui, sab, aui, assoc);
		MetaTreeNode root = new MetaTreeNode(code, name);
		root._cui = cui;
		root._sab = sab;
		root._expandable = false;
		for (int i=0; i<v.size();i++) {
			String t = (String) v.elementAt(i);
			Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(t);
			String cui2 = (String) u.elementAt(0);
			String aui2 = (String) u.elementAt(1);
			String nameAndCode = getAtomNameAndCodeByAUI(cui2, sab, aui2);
			Vector u2 = gov.nih.nci.evs.restapi.util.StringUtils.parseData(nameAndCode);
			String str2 = (String) u2.elementAt(0);
			String code2 = (String) u2.elementAt(1);
			MetaTreeNode childItem = new MetaTreeNode(code2, str2);
			childItem._cui = cui2;
			childItem._sab = sab;
			childItem.setAui(aui2);
			childItem._expandable = isExpandable(cui2, sab);
			root.addChild(assoc, childItem);
			root._expandable = true;
     	}
        return root;
    }


    public HashMap getChildrenExt(String cui, String sab) {
		Atom atom = getPreferredAtom(cui, sab);
		MetaTreeNode metaTreeNode = expandSourceTree(atom);
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
	public MetaTreeNode concept2MetaTreeNode(Concept c) {
		if (c == null) return null;
		return new MetaTreeNode(c.getCode(),
		                        null,
		                        c.getLabel(),
		                        null,
		                        null,
		                        c.getCui(),
		                        c.getSab(),
		                        c.getAui());
	}

	public MetaTreeNode path2MetaTreeNode(Path path) {
		String asso = "CHD";
		List conceptList = path.getConcepts();
		HashMap hmap = new HashMap();
		for (int k2=0; k2<conceptList.size(); k2++) {
			Concept c = (Concept) conceptList.get(k2);
            MetaTreeNode node = concept2MetaTreeNode(c);
            hmap.put(new Integer(c.getIdx()), node);
		}
		MetaTreeNode root = (MetaTreeNode) hmap.get( new Integer(0));
		for (int k2=0; k2<conceptList.size()-1; k2++) {
            MetaTreeNode node = (MetaTreeNode) hmap.get(new Integer(k2));
            MetaTreeNode childnode = (MetaTreeNode) hmap.get(new Integer(k2+1));
            node.addChild(asso, childnode);
		}
        return (MetaTreeNode) hmap.get(new Integer(0));
	}


	public MetaTreeNode searchTree(MetaTreeNode node, String sab, String cui, String aui) {
		if (node.getSab().compareTo(sab) == 0 && node.getCui().compareTo(cui) == 0 && node.getAui().compareTo(aui) == 0) {
			return node;
		}
		for (String assoc : node._assocToChildMap.keySet()) {
			List<MetaTreeNode> children = node._assocToChildMap.get(assoc);
			for (int i=0; i<children.size(); i++) {
				MetaTreeNode child = (MetaTreeNode) children.get(i);
				MetaTreeNode ti_next = searchTree(child,  sab, cui, aui);
				if (ti_next != null) return ti_next;
			}
		}
		return null;
	}
*/
    // getAtomsByCode(String sab, String code)
	public static String construct_aui_query(String sab, String code) {
		StringBuffer buf = new StringBuffer();
		//buf.append("PREFIX sm: <tag:stardog:api:mapping:>").append("\n");
		buf.append("PREFIX MRCONSO: <http://ncicb.nci.nih.gov/" + VIRTUAL_GRAPH_NAME + "/mrconso/>").append("\n");
		//buf.append("PREFIX dc:<http://purl.org/dc/elements/1.1/>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("SELECT distinct ?aui ?cui {").append("\n");
		buf.append("   GRAPH <virtual://" + VIRTUAL_GRAPH_NAME + "> {").append("\n");
		//buf.append("      ?s a :MRCONSO .").append("\n");
		buf.append("      ?s MRCONSO:cui ?cui .").append("\n");
		buf.append("      ?s MRCONSO:aui ?aui .").append("\n");
		buf.append("      ?s MRCONSO:code \"" + code + "\"^^xsd:string .").append("\n");
		buf.append("      ?s MRCONSO:sab \"" + sab + "\"^^xsd:string .").append("\n");
		buf.append("   }").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		return buf.toString();
	}

    public Vector getAtomsByCode(String sab, String code) {
        Vector w = new Vector();
        String query = construct_aui_query(sab, code);
        Vector v = executeQuery(query);
		if (v == null || v.size() == 0) return null;
		v = new ParserUtils().getResponseValues(v);
		//buf.append("SELECT distinct ?aui ?cui {").append("\n");
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(line, '|');
			String aui = (String) u.elementAt(0);
			String cui = (String) u.elementAt(1);
			String str_and_code = getAtomNameAndCodeByAUI(cui, sab, aui);
			Vector u2 = gov.nih.nci.evs.restapi.util.StringUtils.parseData(str_and_code, '|');
			Atom atom = new Atom((String) u2.elementAt(1),
			                     (String) u2.elementAt(0),
			                     aui,
			                     sab, cui);
			w.add(atom);
		}
        return w;
	}

    public String getAtomNameAndCodeByAUI(String cui, String sab, String aui) {
        Vector v = new Vector();
        String query = construct_label_query(cui, sab, aui);
        v = executeQuery(query);
		if (v == null || v.size() == 0) {
			Atom atom = getHighestRankAtom(cui, sab);
			if (atom == null) return null;
			return atom.getStr() + "|" + atom.getCode();
		}
		v = new ParserUtils().getResponseValues(v);
		String first_line = (String) v.elementAt(0);
		Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(first_line);
		return (String) u.elementAt(0) + "|" + (String) u.elementAt(1);
	}

        // getAtomsByAUI(String sab, String aui)
	public static String construct_atom_code_query(String sab, String aui) {
		StringBuffer buf = new StringBuffer();
		//buf.append("PREFIX sm: <tag:stardog:api:mapping:>").append("\n");
		buf.append("PREFIX MRCONSO: <http://ncicb.nci.nih.gov/" + VIRTUAL_GRAPH_NAME + "/mrconso/>").append("\n");
		//buf.append("PREFIX dc:<http://purl.org/dc/elements/1.1/>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("SELECT distinct ?code ?cui {").append("\n");
		buf.append("   GRAPH <virtual://" + VIRTUAL_GRAPH_NAME + "> {").append("\n");
		//buf.append("      ?s a :MRCONSO .").append("\n");
		buf.append("      ?s MRCONSO:cui ?cui .").append("\n");
		buf.append("      ?s MRCONSO:aui \"" + aui + "\"^^xsd:string .").append("\n");
		buf.append("      ?s MRCONSO:code ?code .").append("\n");
		buf.append("      ?s MRCONSO:sab \"" + sab + "\"^^xsd:string .").append("\n");
		buf.append("   }").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		return buf.toString();
	}

    public Vector getAtomsByAUI(String sab, String aui) {
        Vector w = new Vector();
        String query = construct_atom_code_query(sab, aui);
        Vector v = executeQuery(query);
		if (v == null || v.size() == 0) return null;
		v = new ParserUtils().getResponseValues(v);
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(line, '|');
			String code = (String) u.elementAt(0);
			String cui = (String) u.elementAt(1);
			String str_and_code = getAtomNameAndCodeByAUI(cui, sab, aui);
			Vector u2 = gov.nih.nci.evs.restapi.util.StringUtils.parseData(str_and_code, '|');
			Atom atom = new Atom((String) u2.elementAt(1),
			                     (String) u2.elementAt(0),
			                     aui,
			                     sab, cui);
			w.add(atom);
		}
        return w;
	}
        // getCUIsContainingAtom(String sab, String code)
    public Vector getCUIsContainingAtom(String sab, String code) {
        Vector v = new Vector();
        String query = construct_cui_query(sab, code);
        //buf.append("SELECT distinct ?cui {").append("\n");
        v = executeQuery(query);
		if (v == null || v.size() == 0) return null;
		v = new ParserUtils().getResponseValues(v);
		return v;
	}

        // isExpandable(Atom atom)
	 boolean isExpandable(Atom atom) {
		 return isExpandable(atom.getCui(), atom.getSab());
	 }

        // getSubconcepts(Atom atom)
    //public Vector getSubconcepts(String cui1, String sab) {
	public Vector<Atom> getSubconcepts(Atom atom) {
		String sab = atom.getSab();
		Vector w = new Vector();
		Vector v = getRelationships(atom.getCui(), "CHD", atom.getSab());
		if (v == null) return null;
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(line, '|');
			String aui2 = (String) u.elementAt(1);
			String cui2 = (String) u.elementAt(3);
			String str_and_code = getAtomNameAndCodeByAUI(cui2, sab, aui2);
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
			String str_and_code = getAtomNameAndCodeByAUI(cui1, sab, aui1);
			Vector u2 = gov.nih.nci.evs.restapi.util.StringUtils.parseData(str_and_code, '|');
			Atom super_atom = new Atom((String) u2.elementAt(1),
			                     (String) u2.elementAt(0),
			                     aui1,
			                     sab, cui1);
			w.add(super_atom);
		}
		return w;
	}

        // getSupportedSources
    public Vector getSupportedSources() {
        String query = construct_sab_query();
        Vector v = executeQuery(query);
		if (v == null || v.size() == 0) {
			return null;
		}
		v = new ParserUtils().getResponseValues(v);
		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(t);
			String sab = (String) u.elementAt(0);
			if (!w.contains(sab)) {
				w.add(sab);
			}
		}
		w = new gov.nih.nci.evs.restapi.util.SortUtils().quickSort(w);
		return w;
	}

        // getSupportedRelationships
    public Vector getSupportedRelationships(String sab) {
        Vector v = new Vector();
        String query = construct_rel_query(sab);
        v = executeQuery(query);
		if (v == null || v.size() == 0) return null;
		v = new ParserUtils().getResponseValues(v);
		v = new gov.nih.nci.evs.restapi.util.SortUtils().quickSort(v);
		return v;
    }

        // getSupportedRelationshipAttributes
    public Vector getSupportedRelationshipAttributes(String sab) {
        Vector v = new Vector();
        String query = construct_rela_query(sab);
        v = executeQuery(query);
		if (v == null || v.size() == 0) return null;
		v = new ParserUtils().getResponseValues(v);
		v = new gov.nih.nci.evs.restapi.util.SortUtils().quickSort(v);
		return v;
	}

        // getSupportedProperties
    public Vector getSupportedProperties(String sab) {
        Vector v = new Vector();
        String query = construct_atn_query(sab);
        v = executeQuery(query);
		if (v == null || v.size() == 0) return null;
		v = new ParserUtils().getResponseValues(v);
		v = new gov.nih.nci.evs.restapi.util.SortUtils().quickSort(v);
		return v;
	}

       // getSupportedTermTypes
    public Vector getSupportedTermTypes() {
        Vector v = new Vector();
        String query = construct_term_type_query();
        v = executeQuery(query);
		if (v == null || v.size() == 0) return null;
		v = new ParserUtils().getResponseValues(v);
		v = new gov.nih.nci.evs.restapi.util.SortUtils().quickSort(v);
		return v;
	}

        // getSemanticTypes
    public Vector getSemanticTypes() {
        Vector v = new Vector();
        String query = construct_semantic_type_query();
        v = executeQuery(query);
		if (v == null || v.size() == 0) return null;
		v = new ParserUtils().getResponseValues(v);
        return v;
	}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public Vector<Atom> searchConceptsByCode(String sab, String matchText) {
		return metathesaurusSearchUtils.searchConceptsByCode(sab, matchText);
    }

//////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public Vector<Atom> searchConceptsByAtomName(String sab, String algorithm, String matchText) {
		return metathesaurusSearchUtils.searchConceptsByName(sab, algorithm, matchText);
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

	public String construct_batch_semantic_type_query(Vector cuis) {
		StringBuffer buf = new StringBuffer();
		//buf.append("PREFIX sm: <tag:stardog:api:mapping:>").append("\n");
		buf.append("PREFIX MRSTY: <http://ncicb.nci.nih.gov/" + VIRTUAL_GRAPH_NAME + "/mrsty/>").append("\n");
		//buf.append("PREFIX dc:<http://purl.org/dc/elements/1.1/>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("SELECT ?cui ?sty {").append("\n");
		buf.append("   GRAPH <virtual://" + VIRTUAL_GRAPH_NAME + "> {").append("\n");
		buf.append("      ?s MRSTY:cui ?cui .").append("\n");
		buf.append("      ?s MRSTY:sty ?sty .").append("\n");
		buf.append("   }").append("\n");
		buf.append("   " + create_in_filter_statement("?cui", cuis)).append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
	    return buf.toString();
	}

    public Vector getSemanticTypesInBatch(Vector cuis) {
        Vector v = new Vector();
        String query = construct_batch_semantic_type_query(cuis);
        v = executeQuery(query);
		if (v == null || v.size() == 0) return null;
		v = new ParserUtils().getResponseValues(v);
        return v;
	}

	public String construct_get_named_graphs() {
		StringBuffer buf = new StringBuffer();
		//buf.append("PREFIX sm: <tag:stardog:api:mapping:>").append("\n");
		buf.append("SELECT distinct ?g {").append("\n");
		buf.append("  { ?s ?p ?o } ").append("\n");
		buf.append("UNION ").append("\n");
		buf.append("    { graph ?g { ?s ?p ?o } }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}


    public Vector getGraphs() {
        Vector v = new Vector();
        String query = construct_get_named_graphs();
        v = executeQuery(query);
		if (v == null || v.size() == 0) return null;
		v = new ParserUtils().getResponseValues(v);
        return v;
	}

	public HashMap createRankMap() {
		Vector v = getRankData();
        if (v == null) {
			return null;
		}
		HashMap hmap = new HashMap();
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(t, '|');
			String rank = (String) u.elementAt(0);
			String sab = (String) u.elementAt(1);
			String tty = (String) u.elementAt(2);
			String suppress = (String) u.elementAt(3);
			Integer int_obj = new Integer(Integer.parseInt(rank));
			hmap.put(sab + "|" + tty, int_obj);
		}
		return hmap;
	}

    public Vector sortAtomData(Vector w) {
		 AtomData temp = null;
		 for (int i=1; i<w.size(); i++) {
			 AtomData a1 = (AtomData) w.elementAt(i);
			 for (int j=0; j<i; j++) {
 			     AtomData a2 = (AtomData) w.elementAt(j);
 			     if (a1.getRank() < a2.getRank()) {
					 temp = a1;
					 w.set(i, a2);
					 w.set(j, a1);
				 }
			 }
		 }
         return w;
	}

    public Vector getSortedAtomData(Vector v) {
		 if (v == null) {
			 return null;
		 }
		 Vector w = new Vector();
		 for (int i=0; i<v.size(); i++) {
			 String line = (String) v.elementAt(i);
			 Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(line, '|');
			 AtomData atomData = new AtomData((String) u.elementAt(0),
			                                  (String) u.elementAt(1),
			                                  (String) u.elementAt(2),
			                                  (String) u.elementAt(3),
			                                  (String) u.elementAt(4),
			                                  (String) u.elementAt(5),
			                                  (String) u.elementAt(6),
			                                  (String) u.elementAt(7),
			                                  (String) u.elementAt(8),
			                                  (String) u.elementAt(9),
			                                  (String) u.elementAt(10),
			                                  0);
            int rank = getRank(atomData.getSab(), atomData.getTty());
            atomData.setRank(rank);
            w.add(atomData);

		 }
		 w = sortAtomData(w);
		 return w;
	 }

    public Vector getSortedAtomData(String cui, String sab) {
		 Vector v = getAtomData(cui, sab);
		 if (v == null) {
			 return null;
		 }
		 return getSortedAtomData(v);
	 }

    public Vector getSortedAtomData(String cui, String sab, String code) {
		 Vector w = new Vector();
		 Vector v = getAtomData(cui, sab, code);
		 if (v == null) {
			 return null;
		 }
		 return getSortedAtomData(v);
	 }

/////////////////////////////////////////////////////////////////////////////////////////////////////
     public Atom getHighestRankAtom(Vector v) {
		 Vector w = getSortedAtomData(v);
		 AtomData a = (AtomData) w.elementAt(w.size()-1);
		 return atomData2Atom(a);
	 }

     public Atom getHighestRankAtom(String cui, String sab) {
		 Vector v = getSortedAtomData(cui, sab);
		 //gov.nih.nci.evs.restapi.util.StringUtils.dumpVector("v", v);
		 AtomData a = (AtomData) v.elementAt(v.size()-1);
		 return atomData2Atom(a);
	 }

     public Atom getHighestRankAtom(String cui, String sab, String code) {
		 Vector v = getSortedAtomData(cui, sab, code);
		 AtomData a = (AtomData) v.elementAt(v.size()-1);
		 return atomData2Atom(a);
	 }

     public Atom getPreferredAtom(String cui, String sab) {
         return getHighestRankAtom(cui, sab);
	 }

     public Atom getPreferredAtom(String cui, String sab, String code) {
         return getHighestRankAtom(cui, sab, code);
	 }

     public String getPreferredAtomName(String cui, String sab) {
		 Atom atom = getPreferredAtom(cui, sab);
		 return atom.getStr();
	 }

     public String getPreferredAtomName(String cui, String sab, String code) {
		 Atom atom = getPreferredAtom(cui, sab, code);
		 return atom.getStr();
	 }

     public String getPreferredAtomNameAndCode(String cui, String sab) {
		 Atom atom = getPreferredAtom(cui, sab);
		 return atom.getStr() + "|" + atom.getCode();
	 }

///////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static String construct_codes_in_sab_query(String sab) {
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX MRCONSO: <http://ncicb.nci.nih.gov/" + VIRTUAL_GRAPH_NAME + "/mrconso/>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("SELECT distinct ?cui ?sab ?code {").append("\n");
		buf.append("   GRAPH <virtual://" + VIRTUAL_GRAPH_NAME + "> {").append("\n");
		buf.append("      ?s MRCONSO:cui ?cui .").append("\n");
		buf.append("      ?s MRCONSO:sab \"" + sab + "\"^^xsd:string .").append("\n");
		buf.append("      ?s MRCONSO:code ?code .").append("\n");
		buf.append("      ?t MRCONSO:str ?str .").append("\n");
		buf.append("      ?t MRCONSO:tty ?tty .").append("\n");
		buf.append("   }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

    public Vector getCodesInSAB(String sab) {
        Vector v = new Vector();
        String query = construct_codes_in_sab_query(sab);
        v = executeQuery(query);
		if (v == null || v.size() == 0) return null;
		v = new ParserUtils().getResponseValues(v);
        return v;
	}

	//buf.append("SELECT distinct ?cui ?sab ?code ?str{").append("\n");
	public Vector getMappingEntries(String source_sab, String target_sab) {
		HashMap src_cui2codes_map = new HashMap();
		Vector v1 = getCodesInSAB(source_sab);
		for (int i=0; i<v1.size(); i++) {
			String t = (String) v1.elementAt(i);
			Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(t, '|');
			String cui = (String) u.elementAt(0);
			String code = (String) u.elementAt(2);
			Vector w = new Vector();
			if (src_cui2codes_map.containsKey(cui)) {
				w = (Vector) src_cui2codes_map.get(cui);
			}
			if (!w.contains(code)) {
				w.add(code);
			}
			src_cui2codes_map.put(cui, w);
		}

		HashMap target_cui2codes_map = new HashMap();
		Vector v2 = getCodesInSAB(target_sab);
		for (int i=0; i<v2.size(); i++) {
			String t = (String) v2.elementAt(i);
			Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(t, '|');
			String cui = (String) u.elementAt(0);
			String code = (String) u.elementAt(2);
			Vector w = new Vector();
			if (target_cui2codes_map.containsKey(cui)) {
				w = (Vector) target_cui2codes_map.get(cui);
			}
			if (!w.contains(code)) {
				w.add(code);
			}
			target_cui2codes_map.put(cui, w);
		}

		Vector mapping_entries = new Vector();
		Iterator it = src_cui2codes_map.keySet().iterator();
		while (it.hasNext()) {
			String cui = (String) it.next();
			if (target_cui2codes_map.containsKey(cui)) {
				Vector code_vec_1 = (Vector) src_cui2codes_map.get(cui);
				Vector code_vec_2 = (Vector) target_cui2codes_map.get(cui);
				Vector entries = createMappingEntries(cui, source_sab, code_vec_1, target_sab, code_vec_2);
				mapping_entries.addAll(entries);
			}
		}
		return mapping_entries;
	}

	public Vector createMappingEntries(String cui, String source_sab, Vector code_vec_1, String target_sab, Vector code_vec_2) {
		Vector w = new Vector();
		for (int i=0; i<code_vec_1.size(); i++) {
			String code_1 = (String) code_vec_1.elementAt(i);
			String name_1 = getPreferredAtomName(cui, source_sab, code_1);
			for (int j=0; j<code_vec_2.size(); j++) {
				String code_2 = (String) code_vec_2.elementAt(j);
				String name_2 = getPreferredAtomName(cui, target_sab, code_2);
				w.add(source_sab + "|" + code_1 + "|" + name_1 + "|" + target_sab + "|" + code_2 + "|" + name_2);
			}
		}
		return w;
	}


    public Atom atomData2Atom(AtomData a) {
		if (a == null) return null;
		return new Atom(a.getCode(),
		                a.getStr(),
		                a.getAui(),
		                a.getSab(),
		                a.getCui(),
		                a.getIspref(),
		                a.getLat(),
		                a.getStt(),
		                null,
                        a.getTty());
	}


     public Vector getAvailableCodingSchemes() {
         OWLSPARQLUtils owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl);
         HashMap nameVersion2NamedGraphMap = owlSPARQLUtils.getNameVersion2NamedGraphMap();
         Iterator it = nameVersion2NamedGraphMap.keySet().iterator();
         Vector v = new Vector();
         while (it.hasNext()) {
			 String key = (String) it.next();
			 Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(key, '|');
			 String sab = (String) u.elementAt(0);
			 if (sab.compareTo("MEDDRA") == 0) {
				 sab = "MDR";
			 } else if (sab.compareTo("NCI_Thesaurus") == 0) {
				 sab = "NCI";
			 } else if (sab.compareTo("SNOMEDCT") == 0) {
				 sab = "SNOMEDCT_US";
			 }
			 if (!v.contains(sab)) {
				 v.add(sab);
			 }
		 }
		 return new gov.nih.nci.evs.restapi.util.SortUtils().quickSort(v);
	 }


///////////////////////////////////////////////////////////////////////////////////////////////////////////
//C4550278|C1140284|RXNORM_17AB_180305F|RXNORM|RxNorm Vocabulary,

	public static String construct_rsab_query() {
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX MRSAB: <http://ncicb.nci.nih.gov/" + VIRTUAL_GRAPH_NAME + "/mrsab/>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("SELECT distinct ?vcui ?rcui ?vsab ?rsab {").append("\n");
		buf.append("   GRAPH <virtual://" + VIRTUAL_GRAPH_NAME + "> {").append("\n");
		buf.append("      ?s MRSAB:vcui ?vcui .").append("\n");
		buf.append("      ?s MRSAB:rcui ?rcui .").append("\n");
		buf.append("      ?s MRSAB:vsab ?vsab .").append("\n");
		buf.append("      ?s MRSAB:rsab ?rsab .").append("\n");
		buf.append("   }").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		return buf.toString();
	}

    public Vector getSABData() {
        Vector v = new Vector();
        String query = construct_rsab_query();
        v = executeQuery(query);
		if (v == null || v.size() == 0) {
			System.out.println("WARNING: getSABData returns nothing");
			return null;
		}
		v = new ParserUtils().getResponseValues(v);
        return v;
	}


	public HashMap getSAB2RootCUIHashMap() {
		HashMap hmap = new HashMap();
		Vector v = getSABData();
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(t, '|');
			String sab = (String) u.elementAt(3);
			String rcui = (String) u.elementAt(1);
			hmap.put(sab, rcui);
		}
		return hmap;
	}

	public String getRootCUI(String sab) {
		if (sab2RootCUIHashMap.containsKey(sab)) {
			return (String) sab2RootCUIHashMap.get(sab);
		}
		return null;
	}

	//buf.append("SELECT ?aui1 ?aui2 ?cui1 ?cui2 ?rel ?rela ?sab {").append("\n");
	public Vector getSABRoots(String sab) {
		String cui = getRootCUI(sab);
        return getChildRelationships(cui, sab);
	}

	public static String construct_relationship_query(String cui1, String rel, String sab) {
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX MRREL: <http://ncicb.nci.nih.gov/" + VIRTUAL_GRAPH_NAME + "/mrrel/>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("SELECT distinct ?aui1 ?aui2 ?cui1 ?cui2 ?rel ?rela ?sab {").append("\n");
		buf.append("   GRAPH <virtual://" + VIRTUAL_GRAPH_NAME + "> {").append("\n");
		buf.append("      ?s MRREL:aui1 ?aui1 .").append("\n");
		buf.append("      ?s MRREL:aui2 ?aui2 .").append("\n");
		buf.append("      ?s MRREL:cui1 ?cui1 .").append("\n");
		if (cui1 != null) {
			buf.append("      ?s MRREL:cui1 \"" + cui1 + "\"^^xsd:string .").append("\n");
		}

		buf.append("      ?s MRREL:cui2 ?cui2 .").append("\n");
		buf.append("      ?s MRREL:rel  ?rel .").append("\n");
		if (rel != null) {
			buf.append("      ?s MRREL:rel \"" + rel + "\"^^xsd:string .").append("\n");
		}

		buf.append("      ?s MRREL:rela ?rela .").append("\n");

		buf.append("      ?s MRREL:sab  ?sab .").append("\n");
		if (sab != null) {
			buf.append("      ?s MRREL:sab \"" + sab + "\"^^xsd:string .").append("\n");
		}


		buf.append("   }").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		return buf.toString();
	}

    public Vector getRelationshipTarget(String cui1, String rel, String sab) {
        Vector v = new Vector();
        String query = construct_relationship_query(cui1, rel, sab);
        v = executeQuery(query);
		if (v == null || v.size() == 0) return null;
		v = new ParserUtils().getResponseValues(v);
        return v;
	}

	public static String construct_mrhier_query(String aui, String cui, String sab) {
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX MRHIER: <http://ncicb.nci.nih.gov/" + VIRTUAL_GRAPH_NAME + "/mrhier/>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("SELECT distinct ?aui ?cui ?cvf ?cxn ?hcd ?paui ?ptr ?rela ?sab {").append("\n");
		buf.append("   GRAPH <virtual://" + VIRTUAL_GRAPH_NAME + "> {").append("\n");
		buf.append("      ?s MRHIER:aui ?aui .").append("\n");
		if (aui != null) {
			buf.append("      ?s MRHIER:aui \"" + aui + "\"^^xsd:string .").append("\n");
		}
		buf.append("      ?s MRHIER:cui ?cui .").append("\n");
		if (cui != null) {
			buf.append("      ?s MRHIER:cui \"" + cui + "\"^^xsd:string .").append("\n");
		}
		buf.append("      ?s MRHIER:cvf ?cvf .").append("\n");
		buf.append("      ?s MRHIER:cxn ?cxn .").append("\n");
		buf.append("      ?s MRHIER:hcd ?hcd .").append("\n");
		buf.append("      ?s MRHIER:paui ?paui .").append("\n");
		buf.append("      ?s MRHIER:ptr ?ptr .").append("\n");
		buf.append("      ?s MRHIER:rela ?rela .").append("\n");

		buf.append("      ?s MRHIER:sab ?sab .").append("\n");
		if (sab != null) {
			buf.append("      ?s MRHIER:sab \"" + sab + "\"^^xsd:string .").append("\n");
		}
		buf.append("   }").append("\n");
		buf.append("}").append("\n");
		buf.append("LIMIT 10").append("\n");
		return buf.toString();
	}

    public Vector getHIERData(String aui, String cui, String sab) {
        Vector v = new Vector();
        String query = construct_mrhier_query(aui, cui, sab);
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


	public static String construct_atom_query(String sab, String aui) {
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX MRCONSO: <http://ncicb.nci.nih.gov/" + VIRTUAL_GRAPH_NAME + "/mrconso/>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("SELECT ?cui ?code ?str ?tty {").append("\n");
		buf.append("GRAPH <virtual://" + VIRTUAL_GRAPH_NAME + "> {").append("\n");
        buf.append("?s MRCONSO:aui ?aui .").append("\n");
        buf.append("?s MRCONSO:aui \"" + aui + "\"^^xsd:string .").append("\n");
        buf.append("?s MRCONSO:code ?code .").append("\n");
		buf.append("?s MRCONSO:cui ?cui .").append("\n");
		buf.append("?s MRCONSO:str ?str .").append("\n");
		buf.append("?s MRCONSO:tty ?tty .").append("\n");
		buf.append("?s MRCONSO:sab ?sab .").append("\n");
		buf.append("?s MRCONSO:sab \"" + sab + "\"^^xsd:string .").append("\n");
		buf.append("}").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}


    public Vector getAtomDataByAUI(String sab, String aui) {
        Vector v = new Vector();
        String query = construct_atom_query(sab, aui);
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

	public String construct_semantic_type_query(String cui) {
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX MRSTY: <http://ncicb.nci.nih.gov/" + VIRTUAL_GRAPH_NAME + "/mrsty/>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("SELECT ?sty {").append("\n");
		buf.append("   GRAPH <virtual://" + VIRTUAL_GRAPH_NAME + "> {").append("\n");
		buf.append("      ?s MRSTY:cui ?cui .").append("\n");
		buf.append("      ?s MRSTY:cui \"" + cui + "\"^^xsd:string .").append("\n");
		buf.append("      ?s MRSTY:sty ?sty .").append("\n");
		buf.append("   }").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
	    return buf.toString();
	}

    public Vector getSemanticTypes(String cui) {
        Vector v = new Vector();
        String query = construct_semantic_type_query(cui);
        v = executeQuery(query);
		if (v == null || v.size() == 0) return null;
		v = new ParserUtils().getResponseValues(v);
		return new gov.nih.nci.evs.restapi.util.SortUtils().quickSort(v);
	}

	public String onstruct_sab_definition_query() {
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX MRSAB: <http://ncicb.nci.nih.gov/" + VIRTUAL_GRAPH_NAME + "/mrsab/>").append("\n");
		buf.append("SELECT ?rsab ?son {").append("\n");
		buf.append("GRAPH <virtual://" + VIRTUAL_GRAPH_NAME + "> {").append("\n");
		buf.append("?s MRSAB:rsab ?rsab .").append("\n");
		buf.append("?s MRSAB:son ?son .").append("\n");
		buf.append("} ").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		buf.append("").append("\n");
		return buf.toString();
	}

    public Vector getSABDefinitions() {
        Vector v = new Vector();
        String query = onstruct_sab_definition_query();
        v = executeQuery(query);
		if (v == null || v.size() == 0) return null;
		v = new ParserUtils().getResponseValues(v);
		return new gov.nih.nci.evs.restapi.util.SortUtils().quickSort(v);
	}


	public String construct_term_type_definition_query() {
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX MRDOC: <http://ncicb.nci.nih.gov/" + VIRTUAL_GRAPH_NAME + "/mrdoc/>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("SELECT distinct ?value ?expl{").append("\n");
		buf.append("GRAPH <virtual://" + VIRTUAL_GRAPH_NAME + "> {").append("\n");
		buf.append("?s MRDOC:dockey ?dockey .").append("\n");
		buf.append("?s MRDOC:dockey \"TTY\"^^xsd:string  .").append("\n");
		buf.append("?s MRDOC:valuestr ?value .").append("\n");
		buf.append("?s MRDOC:type ?type .").append("\n");
		buf.append("?s MRDOC:type \"expanded_form\"^^xsd:string  .").append("\n");
		buf.append("?s MRDOC:expl ?expl .").append("\n");
		buf.append("} ").append("\n");
		buf.append("}").append("\n");
		buf.append("ORDER BY ?value").append("\n");
		buf.append("").append("\n");
		buf.append("").append("\n");
		return buf.toString();
	}

    public Vector getTermTypeDefinitions() {
        Vector v = new Vector();
        String query = construct_term_type_definition_query();
        v = executeQuery(query);
		if (v == null || v.size() == 0) return null;
		v = new ParserUtils().getResponseValues(v);
		return new gov.nih.nci.evs.restapi.util.SortUtils().quickSort(v);
	}

	public static void main(String[] args) {
        String serviceUrl = args[0];
        MetathesaurusDataUtils test = new MetathesaurusDataUtils(serviceUrl);
        System.out.println("serviceUrl: " + serviceUrl);
        String named_graph = args[1];
        String queryfile = args[2];
        test.runQuery(queryfile);
	}

}
