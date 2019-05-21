package gov.nih.nci.evs.restapi.util;

import gov.nih.nci.evs.restapi.ui.*;
import gov.nih.nci.evs.restapi.bean.*;

import java.io.*;
import java.util.*;
import java.text.*;
import java.sql.*;
import java.net.*;

import org.apache.commons.lang.*;
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

 public class MetathesaurusSearchUtils {
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

	 public MetathesaurusSearchUtils(String serviceUrl) {
		this.serviceUrl = verifyServiceUrl(serviceUrl);
        this.httpUtils = new HTTPUtils(serviceUrl, null, null);
        this.jsonUtils = new JSONUtils();
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

    public void runQuery(String query_file) {
		new OWLSPARQLUtils(serviceUrl).runQuery(query_file);
	}


//////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public String create_in_filter_statement(String var, Vector cuis) {
		StringBuffer buf = new StringBuffer();
		buf.append("filter(?cui in (");
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
        System.out.println(query);
        v = executeQuery(query);
		if (v == null || v.size() == 0) return null;
		v = new ParserUtils().getResponseValues(v);
        return v;
	}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // searchConceptsByAtomName
	public String construct_search_by_code_query(String sab, String matchText) {
		String ts = null;
		String stt = null;
		String ispref = null;
		String lat = "ENG";
	    return construct_search_by_code_query(sab, matchText, ts,
	        stt, ispref, lat);
	}

        // searchConceptsByAtomName
	public String construct_search_by_code_query(String sab, String matchText, String ts,
	    String stt, String ispref, String lat) {
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX MRCONSO: <http://ncicb.nci.nih.gov/" + VIRTUAL_GRAPH_NAME + "/mrconso/>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("SELECT distinct ?str ?cui ?aui ?code {").append("\n");
		buf.append("   GRAPH <virtual://" + VIRTUAL_GRAPH_NAME + "> {").append("\n");
        buf.append("      {").append("\n");
		buf.append("      ?t MRCONSO:cui ?cui .").append("\n");
		buf.append("      ?t MRCONSO:cui \"" + matchText + "\"^^xsd:string .").append("\n");
		buf.append("      ?t MRCONSO:code ?code .").append("\n");
		buf.append("      ?t MRCONSO:aui ?aui .").append("\n");
		buf.append("      ?t MRCONSO:lat \"" + lat + "\"^^xsd:string .").append("\n");
		buf.append("      ?t MRCONSO:str ?str .").append("\n");
		buf.append("      ?t MRCONSO:sab ?sab .").append("\n");
		buf.append("      ?t MRCONSO:ispref ?ispref .").append("\n");
		if (sab != null) {
			buf.append("      ?t MRCONSO:sab \"" + sab + "\"^^xsd:string .").append("\n");
		}
		if (ispref != null) {
			buf.append("      ?t MRCONSO:ispref \"" + ispref + "\"^^xsd:string .").append("\n");
		}
		buf.append("      ?t MRCONSO:tty ?tty .").append("\n");
		if (stt != null) {
			buf.append("      ?t MRCONSO:stt \"" + stt + "\"^^xsd:string .").append("\n");
	    }
	    if (ts != null) {
			buf.append("      ?t MRCONSO:ts \"" + ts + "\"^^xsd:string .").append("\n");
	    }
		buf.append("      }").append("\n");

		buf.append("      UNION ").append("\n");

        buf.append("      {").append("\n");
		buf.append("      ?t MRCONSO:cui ?cui .").append("\n");
		buf.append("      ?t MRCONSO:code ?code .").append("\n");
		buf.append("      ?t MRCONSO:code \"" + matchText + "\"^^xsd:string .").append("\n");

		buf.append("      ?t MRCONSO:aui ?aui .").append("\n");
		buf.append("      ?t MRCONSO:lat \"" + lat + "\"^^xsd:string .").append("\n");
		buf.append("      ?t MRCONSO:str ?str .").append("\n");
		buf.append("      ?t MRCONSO:sab ?sab .").append("\n");
		buf.append("      ?t MRCONSO:ispref ?ispref .").append("\n");
		if (sab != null) {
			buf.append("      ?t MRCONSO:sab \"" + sab + "\"^^xsd:string .").append("\n");
		}
		if (ispref != null) {
			buf.append("      ?t MRCONSO:ispref \"" + ispref + "\"^^xsd:string .").append("\n");
		}
		buf.append("      ?t MRCONSO:tty ?tty .").append("\n");
		if (stt != null) {
			buf.append("      ?t MRCONSO:stt \"" + stt + "\"^^xsd:string .").append("\n");
	    }
	    if (ts != null) {
			buf.append("      ?t MRCONSO:ts \"" + ts + "\"^^xsd:string .").append("\n");
	    }
		buf.append("      }").append("\n");



		buf.append("   }").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		return buf.toString();
	}

    public Vector<Atom> searchConceptsByCode(String sab, String matchText) {
		Vector w = new Vector();
        Vector v = new Vector();
        String query = construct_search_by_code_query(sab, matchText);
        System.out.println(query);
        v = executeQuery(query);
		if (v == null || v.size() == 0) return null;
		v = new ParserUtils().getResponseValues(v);
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(line, '|');
			Atom atom = new Atom((String) u.elementAt(3),
			                     (String) u.elementAt(0),
			                     (String) u.elementAt(2),
			                     sab, (String) u.elementAt(1));
			w.add(atom);
		}
        return w;
    }

	public Vector getCUIsFromAtoms(Vector<Atom> atoms) {
		Vector cuis = new Vector();
		for (int i=0; i<atoms.size(); i++) {
			Atom atom = (Atom) atoms.elementAt(i);
			String cui = atom.getCui();
			if (!cuis.contains(cui)) {
				cuis.add(cui);
			}
		}
		return new gov.nih.nci.evs.restapi.util.SortUtils().quickSort(cuis);
	}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public String construct_search_by_name_query(String algorithm, String matchText) {
		return construct_search_by_name_query(null, algorithm, matchText);
	}

	public String construct_search_by_name_query(String sab, String algorithm, String matchText) {
		String ts = null;
		String stt = null;//"PF";
		String ispref = null;//"Y";
		String lat = "ENG";
	    return construct_search_by_name_query(sab, algorithm, matchText, ts,
	        stt, ispref, lat);
	}

        // searchConceptsByAtomName
	public String construct_search_by_name_query(String sab, String algorithm, String matchText, String ts,
	    String stt, String ispref, String lat) {
		StringBuffer buf = new StringBuffer();
		//buf.append("PREFIX sm: <tag:stardog:api:mapping:>").append("\n");
		buf.append("PREFIX MRCONSO: <http://ncicb.nci.nih.gov/" + VIRTUAL_GRAPH_NAME + "/mrconso/>").append("\n");
		//buf.append("PREFIX dc:<http://purl.org/dc/elements/1.1/>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("SELECT distinct ?str ?cui ?aui ?code {").append("\n");
		buf.append("   GRAPH <virtual://" + VIRTUAL_GRAPH_NAME + "> {").append("\n");
		//buf.append("      ?t a :MRCONSO .").append("\n");
		buf.append("      ?t MRCONSO:cui ?cui .").append("\n");
		buf.append("      ?t MRCONSO:aui ?aui .").append("\n");
		buf.append("      ?t MRCONSO:ispref ?ispref .").append("\n");
		if (ispref != null) {
			buf.append("      ?t MRCONSO:ispref \"" + ispref + "\"^^xsd:string .").append("\n");
		}
		buf.append("      ?t MRCONSO:lat \"" + lat + "\"^^xsd:string .").append("\n");
		buf.append("      ?t MRCONSO:code ?code .").append("\n");
		buf.append("      ?t MRCONSO:str ?str .").append("\n");
		buf.append("      ?t MRCONSO:sab ?sab .").append("\n");
		if (sab != null) {
			buf.append("      ?t MRCONSO:sab \"" + sab + "\"^^xsd:string .").append("\n");
		}
		buf.append("      ?t MRCONSO:tty ?tty .").append("\n");
		if (stt != null) {
			buf.append("      ?t MRCONSO:stt \"" + stt + "\"^^xsd:string .").append("\n");
		}
		if (ts != null) {
			buf.append("      ?t MRCONSO:ts \"" + ts + "\"^^xsd:string .").append("\n");
		}

        String s = getMatchFilter("str", algorithm, matchText);
        buf.append(s).append("\n");
		buf.append("   }").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");

		System.out.println(buf.toString());
		return buf.toString();
	}


    public Vector<Atom> searchConceptsByName(String sab, String algorithm, String matchText) {
		Vector w = new Vector();
        Vector v = new Vector();
        String query = construct_search_by_name_query(sab, algorithm, matchText);
        v = executeQuery(query);
		if (v == null || v.size() == 0) return null;
		v = new ParserUtils().getResponseValues(v);
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(line, '|');
			Atom atom = new Atom((String) u.elementAt(3),
			                     (String) u.elementAt(0),
			                     (String) u.elementAt(2),
			                     sab,
			                     (String) u.elementAt(1));
			w.add(atom);
		}
        return w;
    }

///////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static void main(String[] args) {
        String serviceUrl = args[0];
        MetathesaurusSearchUtils test = new MetathesaurusSearchUtils(serviceUrl);
        System.out.println("serviceUrl: " + serviceUrl);
        String named_graph = args[1];
        String queryfile = args[2];
        //test.runQuery(queryfile);

/*
        String sab = "NCI";
        String matchText = "C0007581";

        System.out.println("Calling searchConceptsByAtomCode ...");
        Vector<Atom> atoms = test.searchConceptsByCode(sab, matchText);
        if (atoms == null) {
			System.out.println("atoms == null???");
		} else if (atoms.size() == 0) {
			System.out.println("atoms.size() == 0???");
		} else {
            System.out.println("atoms.size() = " + atoms.size());
            for (int i=0; i<atoms.size(); i++) {
				Atom atom = (Atom) atoms.elementAt(i);
				System.out.println(atom.toJson());
			}
			Vector cuis = test.getCUIsFromAtoms(atoms);
            gov.nih.nci.evs.restapi.util.StringUtils.dumpVector("cuis", cuis);
		}

        long ms = System.currentTimeMillis();
		Vector cuis = new Vector();
        cuis.add("C1140168");
        cuis.add("C1512759");
        cuis.add("C1512762");
        cuis.add("C1254372");
		Vector w = test.getSemanticTypesInBatch(cuis);
        gov.nih.nci.evs.restapi.util.StringUtils.dumpVector("w", w);
        System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
*/

        String sab = "NCI";
        String matchText = "rash";
        String algorithm = "contains";

        Vector<Atom> atoms = test.searchConceptsByName(sab, algorithm, matchText);
        if (atoms == null) {
			System.out.println("atoms == null???");
		} else if (atoms.size() == 0) {
			System.out.println("atoms.size() == 0???");
		} else {
            System.out.println("atoms.size() = " + atoms.size());
            for (int i=0; i<atoms.size(); i++) {
				Atom atom = (Atom) atoms.elementAt(i);
				System.out.println(atom.toJson());
			}
			Vector cuis = test.getCUIsFromAtoms(atoms);
            gov.nih.nci.evs.restapi.util.StringUtils.dumpVector("cuis", cuis);

			long ms = System.currentTimeMillis();
			Vector w = test.getSemanticTypesInBatch(cuis);
			gov.nih.nci.evs.restapi.util.StringUtils.dumpVector("w", w);
			System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
		}
	}

}
