package gov.nih.nci.evs.restapi.util;

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

 public class MetathesaurusBasedMapping {
	 private static String VIRTUAL_GRAPH_NAME = "metathesaurus";
	 String serviceUrl = null;
     HTTPUtils httpUtils = null;
     JSONUtils jsonUtils = null;
     static String MAPS_TO = "mapsTo";

     MetathesaurusDataUtils metathesaurusDataUtils = null;

	 public MetathesaurusBasedMapping(String serviceUrl) {
		this.serviceUrl = serviceUrl;
        this.httpUtils = new HTTPUtils(serviceUrl, null, null);
        this.jsonUtils = new JSONUtils();
        metathesaurusDataUtils = new MetathesaurusDataUtils(serviceUrl);
	 }

	 public MetathesaurusDataUtils getMetathesaurusDataUtils() {
		 return this.metathesaurusDataUtils;
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

	 public String construct_cuis_containing_atoms_in_source_query(String sab, int maxReturn) {
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX MRCONSO: <http://ncicb.nci.nih.gov/" + VIRTUAL_GRAPH_NAME + "/mrconso/>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("SELECT distinct ?cui {").append("\n");
		buf.append("GRAPH <virtual://" + VIRTUAL_GRAPH_NAME + "> {").append("\n");
		buf.append("?s MRCONSO:cui ?cui .").append("\n");
		buf.append("?s MRCONSO:sab ?sab .").append("\n");
		buf.append("?s MRCONSO:sab \"" + sab + "\"^^xsd:string .").append("\n");
		buf.append("}").append("\n");
		buf.append("}").append("\n");
		if (maxReturn != -1 && maxReturn > 0) {
			buf.append("LIMIT " + maxReturn).append("\n");
		}
		return buf.toString();
	 }

     public Vector getCUIsContainingAtomsInSource(String sab, int maxReturn) {
        Vector v = new Vector();
        String query = construct_cuis_containing_atoms_in_source_query(sab, maxReturn);
        v = executeQuery(query);
		if (v == null || v.size() == 0) return null;
		v = new ParserUtils().getResponseValues(v);
		v = new gov.nih.nci.evs.restapi.util.SortUtils().quickSort(v);
		return v;
	 }


	public String create_in_filter_statement(String var, Vector cuis) {
		StringBuffer buf = new StringBuffer();
		buf.append("filter(str(" + var + ") in (");
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

	 public String construct_mapping_query(String cui, Vector sabs) {
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX MRCONSO: <http://ncicb.nci.nih.gov/metathesaurus/mrconso/>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("SELECT ?cui ?sab ?code ?str ?tty {").append("\n");
		buf.append("GRAPH <virtual://metathesaurus> {").append("\n");
		buf.append("?s MRCONSO:cui ?cui .").append("\n");
		buf.append("?s MRCONSO:cui \"C2262808\"^^<http://www.w3.org/2001/XMLSchema#string> .").append("\n");
		buf.append("?s MRCONSO:sab ?sab .").append("\n");
		buf.append("?s MRCONSO:code ?code .").append("\n");
		buf.append("?s MRCONSO:str ?str .").append("\n");
		buf.append("?s MRCONSO:tty ?tty .").append("\n");
		buf.append("}").append("\n");
		buf.append("   " + create_in_filter_statement("?sab", sabs)).append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		return buf.toString();
	 }

     public Vector getMappingEntryData(String cui, Vector sabs) {
        Vector v = new Vector();
        String query = construct_mapping_query(cui, sabs);
        v = executeQuery(query);
		if (v == null || v.size() == 0) return null;
		v = new ParserUtils().getResponseValues(v);
		v = new gov.nih.nci.evs.restapi.util.SortUtils().quickSort(v);
		return v;
	 }

	 public Vector setIntersection(Vector v1, Vector v2) {
		 Vector v3 = new Vector();
		 for (int i=0; i<v2.size(); i++) {
			 String t = (String) v2.elementAt(i);
			 if (v1.contains(t)) {
				 v3.add(t);
			 }
		 }
		 return v3;
	 }

     public Vector createMappingEntries(String cui, String src_sab, String target_sab, Vector v) {
		 Vector src_code_vec = new Vector();
		 Vector target_code_vec = new Vector();
		 for (int i=0; i<v.size(); i++) {
			 String line = (String) v.elementAt(i);
             Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(line, '|');
             String sab = (String) u.elementAt(1);
             if (sab.compareTo(src_sab) == 0) {
				 String src_code = (String) u.elementAt(2);
				 if (!src_code_vec.contains(src_code)) {
				 	src_code_vec.add(src_code);
				 }
             } else if (sab.compareTo(target_sab) == 0) {
				 String target_code = (String) u.elementAt(2);
				 if (!target_code_vec.contains(target_code)) {
				 	target_code_vec.add(target_code);
				 }
			 }
		 }
		 Vector w = new Vector();
		 for (int i=0; i<src_code_vec.size(); i++) {
			 String src_code = (String) src_code_vec.elementAt(i);
			 Atom src_atom = metathesaurusDataUtils.getHighestRankAtom(cui, src_sab, src_code);
			 String src_data = src_sab + "|" + src_atom.getStr() + "|" + src_atom.getCode();
			 for (int j=0; j<target_code_vec.size(); j++) {
				 String target_code = (String) target_code_vec.elementAt(j);
				 Atom target_atom = metathesaurusDataUtils.getHighestRankAtom(cui, target_sab, target_code);
				 String target_data = target_sab + "|" + target_atom.getStr() + "|" + target_atom.getCode();
				 w.add(src_data + "|" + MAPS_TO + "|" + target_data);
			 }
		 }
         return w;
	 }


     public Vector getCUIsParticipatingInMapping(String src_sab, String target_sab, int maxReturn) {
         Vector v1 = getCUIsContainingAtomsInSource(src_sab, maxReturn);
         if (v1 == null || v1.size() == 0) {
			 return new Vector();
		 }

         Vector v2 = getCUIsContainingAtomsInSource(target_sab, maxReturn);
         if (v2 == null || v2.size() == 0) {
			 return new Vector();
		 }
		 return setIntersection(v1, v2);
	 }

	public Vector create_mapping(String src_sab, String target_sab) {
        int maxReturn = -1;
        return create_mapping(src_sab, target_sab, maxReturn);
	}

	public Vector create_mapping(String src_sab, String target_sab, int maxReturn) {
		Vector mappings = new Vector();
		Vector sabs = new Vector();
		sabs.add(src_sab);
		sabs.add(target_sab);
		Vector w = getCUIsParticipatingInMapping(src_sab, target_sab, maxReturn);
		for (int i=0; i<w.size(); i++) {
			String cui = (String) w.elementAt(i);
			Vector v = getMappingEntryData(cui, sabs);
			if (v != null && v.size() > 0) {
				Vector w2 = createMappingEntries(cui, src_sab, target_sab, v);
				mappings.addAll(w2);
			}
		}
		return mappings;
	}

	 public static void main(String[] args) {
        String serviceUrl = args[0];
        MetathesaurusBasedMapping test = new MetathesaurusBasedMapping(serviceUrl);
        System.out.println("serviceUrl: " + serviceUrl);
        String src_sab = "GO";
        String target_sab = "NCI";
        int maxReturn = 20000;
        Vector v = test.create_mapping(src_sab, target_sab, maxReturn);
		if (v == null || v.size() == 0) {
			 System.out.println("create_mapping returns null???");
		} else {
			 gov.nih.nci.evs.restapi.util.StringUtils.dumpVector("v", v);
		}
	 }
}