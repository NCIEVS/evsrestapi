package gov.nih.nci.evs.restapi.appl;

import gov.nih.nci.evs.restapi.util.*;
import gov.nih.nci.evs.restapi.bean.*;
import gov.nih.nci.evs.restapi.common.*;

import java.io.*;
//import java.math.*;
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


public class CTRPProcessor {
	static String CTRP_TERMINOLOGY_CODE = "C116977";
	static String CTRP_TERMINOLOGY_LABEL = "CTRP terminology";

    JSONUtils jsonUtils = null;
    HTTPUtils httpUtils = null;
    String named_graph = null;
    String prefixes = null;
    String sparql_endpoint = null;
    String serviceUrl = null;
    String named_graph_id = ":NHC0";
    String base_uri = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl";

    OWLSPARQLUtils owlSPARQLUtils = null;

	private static String NCI_THESAURUS = "NCI_Thesaurus";
	private String namedGraph = null;

	public Vector concepts_in_subset = null;

	private TermSearchUtils termSearchUtils = null;
	private HashSet ncit_code_hset = null;

	private List synonyms = null;
    private HashMap code2SynonymMap = null;
    private HashMap ncitcode2labelMap = null;

    private UNIIDataRetriever uniiDataRetriever = null;

	public CTRPProcessor(String serviceUrl, String namedGraph) {
		this.serviceUrl = serviceUrl;
		this.namedGraph = namedGraph;
		System.out.println("serviceUrl: " + this.serviceUrl);
		System.out.println("namedGraph: " + this.namedGraph);

		owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, null, null);
		new MetadataUtils(serviceUrl).dumpNameVersion2NamedGraphMap();
		uniiDataRetriever = new UNIIDataRetriever(this.serviceUrl, this.namedGraph);
/*
		if (!fileExists(FDA_UNII_Subset_Code + ".txt")) {
			System.out.println("Retrieving supporting data from triple store server ...");
			uniiDataRetriever = new UNIIDataRetriever(this.serviceUrl, this.namedGraph);
			uniiDataRetriever.retrieveUNIISupportingData();
			System.out.println("Completed retrieving supporting data.");
		}

		System.out.println("Initialized TermSearchUtils ...");
*/
		//termSearchUtils = new TermSearchUtils(serviceUrl, this.namedGraph);
		synonyms = loadSynonyms();
		System.out.println("createCode2SynonymMap ...");
		code2SynonymMap = createCode2SynonymMap(synonyms);
		System.out.println("createCode2SynonymMap done. " + code2SynonymMap.keySet().size());
	}

	public void dumpSynonymData() {
		for (int i=0; i<synonyms.size(); i++) {
			Synonym syn = (Synonym) synonyms.get(i);
			System.out.println(syn.toJson());
		}
	}

    public boolean fileExists(String filename) {
		File file = new File(filename);
		return file.exists();
	}

/*
Name	TYPE	UNII	Display Name
(R)-2-(5-CYANO-2-(6-(METHOXYCARBONYL)-7-METHYL-3-OXO-8-(3-(TRIFLUOROMETHYL)PHENYL)-2,3,5,8-TETRAHYDRO-(1,2,4)TRIAZOLO(4,3-A)PYRIMIDIN-5-YL)PHENYL)-N,N,N-TRIMETHYLETHANAMINIUM	sys	00174624E2	CHF-6333 CATION
(R)-2-(5-CYANO-2-(6-(METHOXYCARBONYL)-7-METHYL-3-OXO-8-(3-(TRIFLUOROMETHYL)PHENYL)-2,3,5,8-TETRAHYDRO-(1,2,4)TRIAZOLO(4,3-A)PYRIMIDIN-5-YL)PHENYL)-N,N,N-TRIMETHYLETHANAMINIUM ION	cn	00174624E2	CHF-6333 CATION
*/

	public Vector getOWLClasses() {
		return owlSPARQLUtils.getOWLClasses(this.namedGraph);
	}

	public Vector getConceptsInSubset(String code) {
        boolean codeOnly = false;
        Vector v = owlSPARQLUtils.getConceptsInSubset(this.namedGraph, code, codeOnly);
 	    v = new ParserUtils().getResponseValues(v);
	    v = new SortUtils().quickSort(v);
	    return v;
	}

    public Vector getTerminologyVersion(String terminology) {
		HashMap hmap = new MetadataUtils(serviceUrl).getNameVersion2NamedGraphMap();
		Vector w = new Vector();
		Iterator it = hmap.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			Vector u = StringUtils.parseData(key, '|');
			String codingSchemeName = (String) u.elementAt(0);
			String codingSchemeVersion = (String) u.elementAt(1);
			if (codingSchemeName.compareTo(terminology) == 0) {
				if (!w.contains(codingSchemeVersion)) {
					w.add(codingSchemeVersion);
				}
			}
		}
		return w;
	}

	public Vector getPropertyQualifierValuesByCode(String code, String prop_label, String qualifier_label) {
		return owlSPARQLUtils.getPropertyQualifierValuesByCode(this.namedGraph, code, prop_label, qualifier_label);
	}

	public Vector getAxioms(String code, String propertyName) {
		Vector v = owlSPARQLUtils.getAxioms(this.namedGraph, code, propertyName);
		//v = new ParserUtils().getResponseValues(v);
		return v;
	}

    public Vector getAxiomsWithQualifierMatching(String code, String prop_label, String qualifier_label, String qualifier_value) {
        return owlSPARQLUtils.getAxiomsWithQualifierMatching(this.namedGraph, code, prop_label, qualifier_label, qualifier_value);
    }

	public HashMap createSubsourceNameHashMap(Vector subsource_name_data) {
		HashMap hmap = new HashMap();
		for (int i=0; i<subsource_name_data.size(); i++) {
			String line = (String) subsource_name_data.elementAt(i);
			Vector u = parseData(line, '|');
			String code = (String) u.elementAt(2);
			String sourcename = (String) u.elementAt(8);
			Vector v = new Vector();
			if (hmap.containsKey(code)) {
				v = (Vector) hmap.get(code);
			}
			v.add(sourcename);
			hmap.put(code, v);
		}
		return hmap;
	}

		/*
		bnode_301c03a7_663e_49c8_be4e_8726b4fc92ea_826645|Cyclophosphamide|C405|FULL_SYN|P90|CYCLOPHOSPHAMIDE|Term Source|P384|FDA
		bnode_301c03a7_663e_49c8_be4e_8726b4fc92ea_914386|Fluoxymesterone|C507|FULL_SYN|P90|FLUOXYMESTERONE|Term Source|P384|FDA
        */
	public HashMap createTermSourceHashMap(Vector term_source_data) {
		HashMap hmap = new HashMap();
		for (int i=0; i<term_source_data.size(); i++) {
			String line = (String) term_source_data.elementAt(i);
			Vector u = parseData(line, '|');
			String code = (String) u.elementAt(2);
			String sourcename = (String) u.elementAt(8);
			Vector v = new Vector();
			if (hmap.containsKey(code)) {
				v = (Vector) hmap.get(code);
			}
			v.add(sourcename);
			hmap.put(code, v);
		}
		return hmap;
	}

	public HashMap createSourceCodeHashMap(Vector source_code_data) {
		HashMap hmap = new HashMap();
		for (int i=0; i<source_code_data.size(); i++) {
			String line = (String) source_code_data.elementAt(i);
			Vector u = parseData(line, '|');
			String code = (String) u.elementAt(2);
			String sourcename = (String) u.elementAt(8);
			Vector v = new Vector();
			if (hmap.containsKey(code)) {
				v = (Vector) hmap.get(code);
			}
			v.add(sourcename);
			hmap.put(code, v);
		}
		return hmap;
	}

	public HashMap createSynonymHashMap() {
		Vector v = new Vector();
		Vector subsource_name_data = Utils.readFile("subsource-name.txt");
		v.addAll(subsource_name_data);
		Vector term_source_data = Utils.readFile("term-source-fda.txt");
		v.addAll(term_source_data);
		Vector source_code_data = Utils.readFile("source-code.txt");
		v.addAll(source_code_data);
		v = new SortUtils().quickSort(v);

		List list = new ParserUtils().getSynonyms(v);
        HashMap hmap = new HashMap();
        ncit_code_hset = new HashSet();
		for (int i=0; i<list.size(); i++) {
			int j = i+1;
			Synonym syn = (Synonym) list.get(i);
			String code = syn.getCode();
			ncit_code_hset.add(code);
			Vector w = new Vector();
			if (hmap.containsKey(code)) {
				w = (Vector) hmap.get(code);
			}
			w.add(syn);
			hmap.put(code, w);
		}
		return hmap;
	}


	public Vector retrievePropertyData(String propertyName) {
		String prop_label = "FULL_SYN";
		Vector v = owlSPARQLUtils.get_property_query(this.namedGraph, propertyName);
        return v;
	}

	public Vector retrieveTermSourceData(String qualifier_value) {
		String prop_label = "FULL_SYN";
		String qualifier_label = "Term Source";
		//String qualifier_value = "FDA";
		Vector v = owlSPARQLUtils.getAxiomsWithQualifierMatching(this.namedGraph, null, prop_label, qualifier_label, qualifier_value);
        return v;
	}

	public Vector retrieveTermTypeData(String qualifier_value) {
		String prop_label = "FULL_SYN";
		String qualifier_label = "Term Type";
		Vector v = owlSPARQLUtils.getAxiomsWithQualifierMatching(this.namedGraph, null, prop_label, qualifier_label, qualifier_value);
        return v;
	}

    public HashMap findCTRPConcepts() {
		HashMap hmap = new HashMap();
		Vector v = new Vector();
		for (int i=0; i<synonyms.size(); i++) {
			int j = i+1;
			Synonym syn = (Synonym) synonyms.get(i);
			if (syn.getTermSource() != null && syn.getTermSource().compareTo("CTRP") == 0) {
				hmap.put(syn.getCode(), syn.getLabel());
			}
		}
		return hmap;
	}

    public Vector searchFullSynWithCTRP() {
		Vector w = new Vector();
		for (int i=0; i<synonyms.size(); i++) {
			int j = i+1;
			Synonym syn = (Synonym) synonyms.get(i);
			if ((syn.getTermSource() != null && syn.getTermSource().compareTo("CTRP") == 0)) {
				w.add(syn.getCode());
			}
		}
		return w;
	}

    public Vector searchFullSynWithCTRPND() {
		Vector w = new Vector();
		for (int i=0; i<synonyms.size(); i++) {
			int j = i+1;
			Synonym syn = (Synonym) synonyms.get(i);
			if ((syn.getTermSource() != null && syn.getTermSource().compareTo("CTRP") == 0) &&
				(syn.getTermGroup() != null && syn.getTermGroup().compareTo("DN") == 0)) {
				w.add(syn.getCode());
			}
		}
		return w;
	}


    public boolean searchFullSyn(String code, String source, String type) {
		for (int i=0; i<synonyms.size(); i++) {
			int j = i+1;
			Synonym syn = (Synonym) synonyms.get(i);
			if (syn.getCode().compareTo(code) == 0) {
				if ((syn.getTermSource() != null && syn.getTermSource().compareTo("CTRP") == 0) &&
				    (syn.getTermGroup() != null && syn.getTermGroup().compareTo("DN") == 0)) {
				    return true;
				}
			}
		}
		return false;
	}

	public Vector searchSynonyms(String code) {
		return (Vector) code2SynonymMap.get(code);
	}


	public Vector searchForMissingDNInCTRPTerms() { // = "ncit_unii.txt";) {
		HashMap hmap = findCTRPConcepts();
		Vector w = new Vector();
		Vector missing_dn_vec = new Vector();
		Iterator it = hmap.keySet().iterator();
		int i = 0;
		Vector error_vec = new Vector();
		while (it.hasNext()) {
			i++;
			String code = (String) it.next();
			String label = (String) hmap.get(code);
			boolean retval = searchFullSyn(code, "CTRP", "DN");
			if (!retval) {
				missing_dn_vec.add(code);
			}
		}

		for (i=0; i<missing_dn_vec.size(); i++) {
			String code = (String) missing_dn_vec.elementAt(i);
			String label = (String) hmap.get(code);
			Vector syns = searchSynonyms(code);
			w.add("(" + i + ") " + label + "(" + code + ")");
			for (int j=0; j<syns.size(); i++) {
				Synonym syn = (Synonym) syns.elementAt(j);
				w.add(syn.toJson());
			}
		}
		return w;
	}

	public void runCTRPQA() {
		Vector w = new Vector();
		Vector v1 = searchFullSynWithCTRPND();
		Vector v2 = searchFullSynWithCTRP();

		System.out.println("Number of FULL_SYN with CTRP source and DN type: " + v1.size());
		System.out.println("Number of FULL_SYN with CTRP source: " + v2.size());

        for (int i=0; i<v2.size(); i++) {
			String code = (String) v2.elementAt(i);
			if (!v1.contains(code)) {
				Vector u = (Vector) code2SynonymMap.get(code);
				Synonym syn0 = (Synonym) u.elementAt(0);
				String label = syn0.getLabel();
				w.add(label + " (" + code + ")");
				for (int j=0; j<u.size(); j++) {
					Synonym syn = (Synonym) u.elementAt(j);
					w.add(syn.toJson());
				}
				w.add("\n");
			}
		}

        String property_name = "Display_Name";
        String property_value = null;
		Vector display_name_vec = findConceptsWithPropertyMatching(property_name, property_value);
		HashMap display_name_hmap = new HashMap();
		for (int i=0; i<display_name_vec.size(); i++) {
			String line = (String) display_name_vec.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			//Therapeutic Procedure|C49236|Display_Name|Therapeutic Procedure
			String code = (String) u.elementAt(1);
			String label = (String) u.elementAt(0);
			display_name_hmap.put(code, label);
		}

		for (int i=0; i<v1.size(); i++) {
			String code = (String) v1.elementAt(i);
			if (!display_name_hmap.containsKey(code)) {
				Vector u = (Vector) code2SynonymMap.get(code);
				Synonym syn0 = (Synonym) u.elementAt(0);
				String label = syn0.getLabel();
				w.add("WARNING: " + label + "(" + code + ") " + " does not have a Display_Name property.");
			}
		}
		w.add("\n");

        property_name = "Contributing_Source";
        property_value = "CTRP";
		Vector contributing_source_vec = findConceptsWithPropertyMatching(property_name, property_value);
		HashMap contributing_source_hmap = new HashMap();
		for (int i=0; i<contributing_source_vec.size(); i++) {
			String line = (String) contributing_source_vec.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String code = (String) u.elementAt(1);
			String label = (String) u.elementAt(0);
			contributing_source_hmap.put(code, label);
		}

		for (int i=0; i<v1.size(); i++) {
			String code = (String) v1.elementAt(i);
			if (!contributing_source_hmap.containsKey(code)) {
				Vector u = (Vector) code2SynonymMap.get(code);
				Synonym syn0 = (Synonym) u.elementAt(0);
				String label = syn0.getLabel();
				w.add("WARNING: " + label + "(" + code + ") " + " does not have a CTRP Contributing_Source.");
			}
		}

		w.add("\n");
		Vector concept_in_subset_vec = get_concepts_in_subset(CTRP_TERMINOLOGY_CODE);
		HashMap concept_in_subset_hmap = new HashMap();
		for (int i=0; i<concept_in_subset_vec.size(); i++) {
			String line = (String) concept_in_subset_vec.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String code = (String) u.elementAt(1);
			String label = (String) u.elementAt(0);
			concept_in_subset_hmap.put(code, label);
		}

		for (int i=0; i<v1.size(); i++) {
			String code = (String) v1.elementAt(i);
			if (!concept_in_subset_hmap.containsKey(code)) {
				Vector u = (Vector) code2SynonymMap.get(code);
				Synonym syn0 = (Synonym) u.elementAt(0);
				String label = syn0.getLabel();
				w.add("WARNING: " + label + "(" + code + ") " + " does not belong to " + CTRP_TERMINOLOGY_LABEL + " (" + CTRP_TERMINOLOGY_CODE + ")");
			}
		}
        String outputfile = "CTRP_QA_" + StringUtils.getToday() + ".txt";
		Utils.saveToFile(outputfile, w);
	}

    public static Vector parseData(String line, char delimiter) {
		if(line == null) return null;
		Vector w = new Vector();
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<line.length(); i++) {
			char c = line.charAt(i);
			if (c == delimiter) {
				w.add(buf.toString());
				buf = new StringBuffer();
			} else {
				buf.append(c);
			}
		}
		w.add(buf.toString());
		return w;
	}

    public static Vector<String> parseData(String line) {
		if (line == null) return null;
        char tab = '|';
        return parseData(line, tab);
    }

	public void saveSynonyms() {
		saveSynonyms(synonyms);
	}


	public void saveSynonyms(List synonyms) {
	    Vector w = new Vector();
	    for (int i=0; i<synonyms.size(); i++) {
			Synonym syn = (Synonym) synonyms.get(i);
			int j = i+1;
			w.add("(" + j + ") " + syn.toJson());
		}
		Utils.saveToFile("synonyms.txt", w);
	}

    public List loadSynonyms() {
		Vector v = new Vector();
		Vector subsource_name_data = uniiDataRetriever.get_term_source_vec();
		v.addAll(subsource_name_data);
		Vector term_source_data = uniiDataRetriever.get_term_source_vec("CTRP");
		v.addAll(term_source_data);
		Vector source_code_data = uniiDataRetriever.get_term_source_code_vec();
		v.addAll(source_code_data);

		Vector term_type_data = uniiDataRetriever.get_term_type_data("DN");
		v.addAll(term_type_data);

		v = new SortUtils().quickSort(v);
		List list = new ParserUtils().getSynonyms(v);
		return list;
	}

    public HashMap createCode2SynonymMap(List synonyms) {
        HashMap hmap = new HashMap();
		for (int i=0; i<synonyms.size(); i++) {
			Synonym syn = (Synonym) synonyms.get(i);
			String code = syn.getCode();
			Vector w = new Vector();
			if (hmap.containsKey(code)) {
				w = (Vector) hmap.get(code);
			}
			w.add(syn);
			hmap.put(code, w);
		}
		return hmap;
	}

    public Synonym searchSynonym(String code) {
		if (code2SynonymMap == null) {
			System.out.println("code2SynonymMap == NULL???");
			return null;
		}

		if (!code2SynonymMap.containsKey(code)) {
			return null;
		}
		Vector w = (Vector) code2SynonymMap.get(code);
		for (int i=0; i<w.size(); i++) {
			Synonym syn = (Synonym) w.get(i);
			if (syn.getTermName() != null && syn.getTermSource() != null && syn.getSubSourceName() != null && syn.getSourceCode() != null) {
			    if (syn.getTermSource().compareTo("FDA") == 0 && syn.getSubSourceName().compareTo("UNII") == 0) {
					return syn;
				}
			}
		}
        return null;
	}

	public Vector findConceptsWithPropertyMatching(String property_name, String property_value) {
	    return owlSPARQLUtils.findConceptsWithPropertyMatching(this.namedGraph, property_name, property_value);
	}

    public Vector get_concepts_in_subset(String code) {
		return owlSPARQLUtils.get_concepts_in_subset(this.namedGraph, code);
	}


    public static void main(String[] args) {
		long ms = System.currentTimeMillis();
		String serviceUrl = args[0];
		String namedGraph = args[1];
		CTRPProcessor processor = new CTRPProcessor(serviceUrl, namedGraph);
		System.out.println("runCTRPQA -- please wait...");
	    processor.runCTRPQA();
        System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}
}

