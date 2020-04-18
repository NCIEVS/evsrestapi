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
import org.apache.commons.codec.binary.Base64;
import org.json.*;

/**
 * <!-- LICENSE_TEXT_START -->
 * Copyright Copyright 2020 MSC.. This software was developed in conjunction
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
 *     Initial implementation kim.ong@ngc.com
 *
 */


public class UNIIProcessor {
	static String UNIDENTIFIABLE_SRS_UNII_CODE = "Unknown SRS UNII code";
	static String UNIDENTIFIABLE_NCIT_UNII_CODE = "UNII code not found in NCIt";
	static String MISSING_NCIT_CODE = "NCIT code not given";
	static String UNKNOWN_NCIT_CODE = "Unknown NCIt code";

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
	private Vector unii_link = null;
	private Vector unii_data = null;
	private String username = null;
	private String password = null;
	private String namedGraph = null;
	private static String ncit_unii_txt_file = "ncit_unii.txt";

	HashMap unii2ncitcode_map = new HashMap();
	HashMap ncitcode2unii_map = new HashMap();
	HashMap ncitcode2pt_map = new HashMap();
	public static String FDA_UNII_Subset_Code = "C63923";
	public static String FDA_UNII_Subset = "FDA Established Names and Unique Ingredient Identifier Codes Terminology";

	public Vector concepts_in_subset = null;

	public String unii_link_file = null;
	public String unii_data_file = null;

	private TermSearchUtils termSearchUtils = null;
	private HashSet ncit_code_hset = null;

	private List synonyms = null;
    private HashMap code2SynonymMap = null;
    private HashMap ncitcode2labelMap = null;

    private UNIIDataRetriever uniiDataRetriever = null;

	public UNIIProcessor(String serviceUrl, String namedGraph) {
		this.serviceUrl = serviceUrl;
		this.namedGraph = namedGraph;
		System.out.println("serviceUrl: " + this.serviceUrl);
		System.out.println("namedGraph: " + this.namedGraph);

		owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, null, null);
		new MetadataUtils(serviceUrl).dumpNameVersion2NamedGraphMap();

		if (!fileExists(FDA_UNII_Subset_Code + ".txt")) {
			System.out.println("Retrieving supporting data from triple store server ...");
			uniiDataRetriever = new UNIIDataRetriever(this.serviceUrl, this.namedGraph);
			uniiDataRetriever.retrieveUNIISupportingData();
			System.out.println("Completed retrieving supporting data.");
		}

		System.out.println("Initialized TermSearchUtils ...");
		termSearchUtils = new TermSearchUtils(serviceUrl, this.namedGraph);
		synonyms = loadSynonyms();
		code2SynonymMap = createCode2SynonymMap(synonyms);
		System.out.println("TermSearchUtils initialized.");
	}

    public boolean fileExists(String filename) {
		File file = new File(filename);
		return file.exists();
	}

	public void initialize() {
		concepts_in_subset = Utils.readFile(FDA_UNII_Subset_Code + ".txt");
        ncitcode2labelMap = createNCItCode2LabelMap();
	}

	public void loadSRSData(String unii_link_file, String unii_data_file) {
		this.unii_link_file = unii_link_file;
		this.unii_data_file = unii_data_file;
        unii_link = Utils.readFile(unii_link_file);
        System.out.println(unii_link_file + ": " + unii_link.size());
        unii_data = Utils.readFile(unii_data_file);
        System.out.println(unii_data_file + ": " + unii_data.size());
        create_unii_hashmaps();
	}

	public void retrieveConceptsWithUNIICode() {
		Vector concepts = owlSPARQLUtils.findConceptsWithProperty(this.namedGraph, "FDA_UNII_Code");
        Utils.saveToFile(ncit_unii_txt_file, concepts);
        create_unii_hashmaps();
	}

/*
Name	TYPE	UNII	Display Name
(R)-2-(5-CYANO-2-(6-(METHOXYCARBONYL)-7-METHYL-3-OXO-8-(3-(TRIFLUOROMETHYL)PHENYL)-2,3,5,8-TETRAHYDRO-(1,2,4)TRIAZOLO(4,3-A)PYRIMIDIN-5-YL)PHENYL)-N,N,N-TRIMETHYLETHANAMINIUM	sys	00174624E2	CHF-6333 CATION
(R)-2-(5-CYANO-2-(6-(METHOXYCARBONYL)-7-METHYL-3-OXO-8-(3-(TRIFLUOROMETHYL)PHENYL)-2,3,5,8-TETRAHYDRO-(1,2,4)TRIAZOLO(4,3-A)PYRIMIDIN-5-YL)PHENYL)-N,N,N-TRIMETHYLETHANAMINIUM ION	cn	00174624E2	CHF-6333 CATION
*/

	public void create_unii_hashmaps() {
		Vector v  = Utils.readFile(ncit_unii_txt_file);
        unii2ncitcode_map = new HashMap();
        ncitcode2unii_map = new HashMap();
        ncitcode2pt_map = new HashMap();
//	//Sulfaclozine|C72849|FDA_UNII_Code|69YP7Z48CW
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = parseData(line, '|');
			String pt = (String) u.elementAt(0);
			String ncitcode = (String) u.elementAt(1);
			String unii_code = (String) u.elementAt(3);
			unii2ncitcode_map.put(unii_code, ncitcode);
			ncitcode2unii_map.put(ncitcode, unii_code);
			ncitcode2pt_map.put(ncitcode, pt);
		}
	}

	//[USAN] or [INN] as part of the Name
	private boolean contain_string_in_file_unii_name(String name, String target) {
		if (name == null) return false;
		if (name.indexOf(target) != -1) return true;
		return false;
	}


    public String findMatchedConcepts(String target) {
		if (target.endsWith("[USAN]")) {
			int n = target.lastIndexOf("[USAN]");
			target = target.substring(0, n-1);
		}

		if (target.endsWith("[INN]")) {
			int n = target.lastIndexOf("[INN]");
			target = target.substring(0, n-1);
		}
		target = target.trim();

		Vector v = owlSPARQLUtils.findMatchedConcepts(this.namedGraph, target);
		if (v == null) return "";
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = parseData(line, '|');
			String label = (String) u.elementAt(0);
			String code = (String) u.elementAt(1);
			buf.append(label + " (" + code + ")");
			if (i < v.size()-1) {
				buf.append('\t');
			}
		}
		return buf.toString();
	}

	public Vector getOWLClasses() {
		return owlSPARQLUtils.getOWLClasses(this.namedGraph);
	}

	public HashMap createNCItCode2LabelMap() {
		Vector w = getOWLClasses();
		HashMap ncitcode2labelMap = new HashMap();
		for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			Vector u = parseData(line, '|');
			String label = (String) u.elementAt(0);
			String code = (String) u.elementAt(1);
			ncitcode2labelMap.put(code, label);
		}
		return ncitcode2labelMap;
    }

	public Vector getConceptsInSubset(String code) {
        boolean codeOnly = false;
        Vector v = owlSPARQLUtils.getConceptsInSubset(this.namedGraph, code, codeOnly);
 	    v = new ParserUtils().getResponseValues(v);
	    v = new SortUtils().quickSort(v);
	    return v;
	}

/*
o	Monthly QA
o	The UNIII terms should have the following attributes:
?	Subsource Name (P386) :UNII
?	Source Code (P385) :UNII code
?	Term source (term-source) :  FDA
?	FDA_UNII_CODE (P319)  :UNII code
?	Should be Concept In Subset (A8) to C63923 (UNII Codes)
*/

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

/*
(81138) {
  "code": "C3113",
  "label": "Hyperplasia",
  "termName": "Hyperplasia",
  "termSource": "FDA",
  "sourceCode": "1906",
  "subSourceName": "CDRH"
}
*/

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


	public void runNCItUNIIQA(String filename) { // = "ncit_unii.txt";) {
		Vector w = new Vector();
		Vector vs_label_and_code_vec = Utils.readFile("C63923.txt");
		for (int i=0; i<vs_label_and_code_vec.size(); i++) {
			int j = i+1;
			String line = (String) vs_label_and_code_vec.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String label = (String) u.elementAt(0);
			String code = (String) u.elementAt(1);
			Synonym syn = searchSynonym(code);
			if (syn == null) {
				w.add("(" + j + ") " + label + "(" + code + ")" + " -- WARNING: Matched synonym not found.");
			} else {
				w.add("(" + j + ") " + label + "(" + code + ")" + syn.toJson());
			}
		}

		w.add("\n\n");
		Vector nci_unii_vec = Utils.readFile(filename);
		for (int i=0; i<nci_unii_vec.size(); i++) {
			String line = (String) nci_unii_vec.elementAt(i);
			//System.out.println(line);

            //Cyclophosphamide|C405|FDA_UNII_Code|8N3DW7272P
			Vector u = StringUtils.parseData(line, '|');
			String label = (String) u.elementAt(0);
			String code = (String) u.elementAt(1);
			String unii_code = (String) u.elementAt(3);
			Synonym syn = searchSynonym(code);
			if (syn == null) {
				String note = "";
				if (termSearchUtils.isRetired(code)) {
					note = "\t(Note: this concept is a retired/obsolete concept.)";
				}
				w.add("WARNING: " + label + " (" + code + ") " + "has a FDA_UNII_Code property but does not belong to " +
				FDA_UNII_Subset + "." + "\n" + note);
			} else {
				String note = "";
				if (termSearchUtils.isRetired(code)) {
					note = "\t(Note: this concept is a retired/obsolete concept.)";
				}
				if (syn.getSourceCode().compareTo(unii_code) != 0) {
					w.add("WARNING: " + label + " (" + code + ")\nUnmatched UNII values -- " + unii_code + " (FDA_UNII_Code)" + " versus " + syn.toJson() + "\n" + note);
				}
			}
		}
		int n = filename.lastIndexOf(".");

        String outputfile = filename.substring(0, n) + "_QA_" + StringUtils.getToday() + ".txt";
		Utils.saveToFile(outputfile, w);
	}

    public String removeSuffix(String str) {
		if (str.endsWith("[USAN]")) {
			int n = str.lastIndexOf("[USAN]");
			str = str.substring(0, n-1);
		}

		if (str.endsWith("[INN]")) {
			int n = str.lastIndexOf("[INN]");
			str = str.substring(0, n-1);
		}
		str = str.trim();
		return str;
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


	public Vector findConceptsWithPropertyMatching(String property_name, String property_value) {
		return owlSPARQLUtils.findConceptsWithPropertyMatching(this.namedGraph, property_name, property_value);
	}



    public void runFDASubstanceRegistrationSystemFilesQA(String unii_link_file, String unii_data_file) {
		if (ncit_code_hset == null) {
			HashMap hmap = createSynonymHashMap();
		}

		this.unii_link_file = unii_link_file;
		this.unii_data_file = unii_data_file;
		loadSRSData(unii_link_file, unii_data_file);

		System.out.println("Processing " + unii_link_file + " ...");

        initialize();
        char demiliter = '|';
        char comma = ',';

        // STEP 1: Name	TYPE	UNII	Display Name
        String unii_link_file_heading = null;
        String line = (String) unii_link.elementAt(0);
        //System.out.println(line);
		Vector u = StringUtils.parseData(line, '\t');
		String name = (String) u.elementAt(0);
		String unii = (String) u.elementAt(2);
		String display_name = (String) u.elementAt(3);

		Vector headings = new Vector();
		headings.add(name);
		headings.add(unii);
		headings.add(display_name);

		headings.add("Comment (" + unii + ")");
		headings.add("Matched NCIt Concepts");

		String new_unii_link_file_heading = FormatHelper.toCSVLine(headings);

		int count = 0;

        Vector w = new Vector();
        w.add(new_unii_link_file_heading);
        HashSet hset = new HashSet();
		for (int i=1; i<unii_link.size(); i++) {
			line = (String) unii_link.elementAt(i);
			Vector fields = new Vector();
			Vector name_msgs = new Vector();
			Vector unii_msgs = new Vector();

			u = StringUtils.parseData(line, '\t');
			name = (String) u.elementAt(0);
			unii = (String) u.elementAt(2);
			display_name = (String) u.elementAt(3);

			String trmmed_name = removeSuffix(name);
            if (trmmed_name.compareTo(name) != 0) {
				name = trmmed_name;
				Vector key_vec = new Vector();
				key_vec.add(name);
				key_vec.add(unii);
				key_vec.add(display_name);
				String key = FormatHelper.toDelimitedLine(key_vec, '$');
				if (!hset.contains(key)) {
					hset.add(key);
					fields = new Vector();
					fields.add(name);
					fields.add(unii);
					fields.add(display_name);

					if (!ncitcode2pt_map.containsKey(unii)) {
						unii_msgs.add(UNIDENTIFIABLE_SRS_UNII_CODE);
					}
					String name_msg_line = FormatHelper.toDelimitedLine(name_msgs, ';');
					String unii_msg_line = FormatHelper.toDelimitedLine(unii_msgs, ';');

					fields.add(unii_msg_line);

					name_msg_line = termSearchUtils.findMatchedConcepts(name);
					fields.add(name_msg_line);

					String csvLine = FormatHelper.toCSVLine(fields);
					w.add(csvLine);

					if (w.size() % 50000 == 0) {
						System.out.println("" + w.size() + " processed.");
					}
				}
			}

		}

		System.out.println("" + w.size() + " processed.");
		//w = FormatHelper.toCSVLines(w);
		int n = unii_link_file.lastIndexOf(".");
		//System.out.println("unii_link_file n: " + n);

		String outputfile = unii_link_file.substring(0, n) + "_" + StringUtils.getToday() + ".csv";
		//System.out.println("outputfile " + outputfile);
		//System.out.println("Calling saveToFile: " + w.size());
		try {
			Utils.saveToFile(outputfile, w);
		} catch (Exception ex) {
			System.out.println("Unable to save -- please close your xls application.");
		}

		//if (test_mode) return;

		// STEP 2:

//UNII	PT	RN	EC	NCIT	RXCUI	PUBCHEM	ITIS	NCBI	PLANTS	GRIN	MPNS	INN_ID	MF	INCHIKEY	SMILES	INGREDIENT_TYPE
//00174624E2	CHF-6333 CATION	1613620-10-2				76285164							C27H28F3N6O3	IHTRPSMRGYWUIM-HSZRJFAPSA-O	COC(=O)C1=C(C)N(C2=NNC(=O)N2[C@@H]1C3=CC=C(C=C3CC[N+](C)(C)C)C#N)C4=CC=CC(=C4)C(F)(F)F	IONIC MOIETY
//0129526470	5,8-DIMETHOXY(1,2,4)TRIAZOLO(1,5-C)PYRIMIDIN-2-AMINE	219715-62-5				11446888							C7H9N5O2	DBJPBHJHAPAUQU-UHFFFAOYSA-N	COC1=CN=C(OC)N2N=C(N)N=C12	INGREDIENT SUBSTANCE

		System.out.println("Processing " + unii_data_file + " ...");

        line = (String) unii_data.elementAt(0);

        String unii_data_file_heading = null;
        line = (String) unii_data.elementAt(0);
		u = StringUtils.parseData(line, '\t');
		unii = (String) u.elementAt(0);
		String pt = (String) u.elementAt(1);
		String ncit = (String) u.elementAt(4);

		headings = new Vector();
		headings.add(unii);
		headings.add(pt);
		headings.add(ncit);

		headings.add("Comment (" + unii + ")");
		headings.add("Comment (" + ncit + ")");
		headings.add("Matched NCIt Concepts");

		String new_unii_data_file_heading = FormatHelper.toCSVLine(headings);
        w = new Vector();
        w.add(new_unii_data_file_heading);

        hset = new HashSet();
		for (int i=1; i<unii_data.size(); i++) {
			line = (String) unii_data.elementAt(i);

			Vector fields = new Vector();
			Vector unii_msgs = new Vector();
			Vector ncit_msgs = new Vector();
			Vector pt_msgs = new Vector();

			u = parseData(line, '\t');
			for (int j=0; j<u.size(); j++) {
				unii = (String) u.elementAt(0);
				pt = (String) u.elementAt(1);
				ncit = "";
				if (u.size() <= 4) {
  					Vector key_vec = new Vector();
 					key_vec.add(unii);
 					key_vec.add(pt);
 					key_vec.add(ncit);
 					String key = FormatHelper.toDelimitedLine(key_vec, '$');
 					if (!hset.contains(key)) {
 						hset.add(key);
 						if (!unii2ncitcode_map.containsKey(unii)) {
 							unii_msgs.add(UNIDENTIFIABLE_SRS_UNII_CODE);
 						}
 						fields = new Vector();
 						fields.add(unii);
 						fields.add(pt);
 						fields.add(ncit);
 						String unii_msg_line = FormatHelper.toDelimitedLine(unii_msgs, ';');

 						ncit_msgs.add(MISSING_NCIT_CODE);
 						String ncit_msg_line = FormatHelper.toDelimitedLine(ncit_msgs, ';');

 						String pt_msg_line = termSearchUtils.findMatchedConcepts(pt);
 						pt_msgs.add(pt_msg_line);
 						pt_msg_line = FormatHelper.toDelimitedLine(pt_msgs, ';');

 						fields.add(unii_msg_line);
 						fields.add(ncit_msg_line);
 						fields.add(pt_msg_line);

 						String csvLine = FormatHelper.toCSVLine(fields);
 						w.add(csvLine);

 						if (w.size() % 50000 == 0) {
 							System.out.println("" + w.size() + " processed.");
 						}
					}

				} else {
					ncit = (String) u.elementAt(4);
					if (ncit.length() == 0) {
 						ncit_msgs.add(MISSING_NCIT_CODE);
					} else if (!ncit_code_hset.contains(ncit)) {
						ncit_msgs.add(UNKNOWN_NCIT_CODE);
					}
					Vector key_vec = new Vector();
					key_vec.add(unii);
					key_vec.add(pt);
					key_vec.add(ncit);
					String key = FormatHelper.toDelimitedLine(key_vec, '$');
					if (!hset.contains(key)) {
						hset.add(key);
						if (!unii2ncitcode_map.containsKey(unii)) {
							unii_msgs.add(UNIDENTIFIABLE_SRS_UNII_CODE);
						}
						fields = new Vector();
						fields.add(unii);
						fields.add(pt);
						fields.add(ncit);
						String unii_msg_line = FormatHelper.toDelimitedLine(unii_msgs, ';');
						String ncit_msg_line = FormatHelper.toDelimitedLine(ncit_msgs, ';');

						String pt_msg_line = termSearchUtils.findMatchedConcepts(pt);
						pt_msgs.add(pt_msg_line);
						pt_msg_line = FormatHelper.toDelimitedLine(pt_msgs, ';');

						fields.add(unii_msg_line);
						fields.add(ncit_msg_line);
						fields.add(pt_msg_line);

						String csvLine = FormatHelper.toCSVLine(fields);
						w.add(csvLine);

						if (w.size() % 50000 == 0) {
							System.out.println("" + w.size() + " processed.");
						}
					}
				}
			}
		}

		System.out.println("" + w.size() + " processed.");
		n = unii_data_file.lastIndexOf(".");
		outputfile = unii_data_file.substring(0, n) + "_" + StringUtils.getToday() + ".csv";
		try {
			Utils.saveToFile(outputfile, w);
		} catch (Exception ex) {
			System.out.println("Unable to save -- please close your xls application.");
		}
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
		Vector subsource_name_data = Utils.readFile("subsource-name.txt");
		v.addAll(subsource_name_data);
		Vector term_source_data = Utils.readFile("term-source-fda.txt");
		v.addAll(term_source_data);
		Vector source_code_data = Utils.readFile("source-code.txt");
		v.addAll(source_code_data);
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


    public static void main(String[] args) {
		long ms = System.currentTimeMillis();
		String serviceUrl = args[0];
		String namedGraph = args[1];
		UNIIProcessor processor = new UNIIProcessor(serviceUrl, namedGraph);
		System.out.println("runNCItUNIIQA -- please wait...");
        processor.runNCItUNIIQA(ncit_unii_txt_file);

		String unii_link_file = "UNII_Names_27Mar2020.txt";
		String unii_data_file = "UNII_Records_27Mar2020.txt";

		if (args.length == 4) {
			unii_link_file = "UNII_Names_27Mar2020.txt";
			unii_data_file = "UNII_Records_27Mar2020.txt";
		}

		System.out.println("runFDASubstanceRegistrationSystemFilesQA -- please wait...");
		processor.runFDASubstanceRegistrationSystemFilesQA(unii_link_file, unii_data_file);
        System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}
}

