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


public class UNIIDataRetriever {
	static String UNIDENTIFIABLE_SRS_UNII_CODE = "Unknown SRS UNII code";
	static String UNIDENTIFIABLE_NCIT_UNII_CODE = "UNII code not found in NCIt";
	static String MISSING_NCIT_CODE = "NCIT code not given";
	static String UNKNOWN_NCIT_CODE = "Unknown NCIt code";
	static String FDA_UNII_Subset_Code = "C63923";
	static String FDA_UNII_Subset = "FDA Established Names and Unique Ingredient Identifier Codes Terminology";
	static String FDA_UNII_Code = "FDA_UNII_Code";

    public Vector term_source_fda_vec = null;
    public Vector fda_unii_subset_vec = null;
    public Vector ncit_concepts_with_unii_code_vec = null;
    public Vector ncit_retired_concepts_vec = null;
    public Vector term_source_vec = null;
    public Vector term_source_code_vec = null;
    public Vector full_syn_vec = null;

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

	HashMap unii2ncitcode_map = new HashMap();
	HashMap ncitcode2unii_map = new HashMap();
	HashMap ncitcode2pt_map = new HashMap();

	static String term_source_fda_file = "term-source-fda.txt";
	static String subsource_name_file = "subsource-name.txt";
	static String source_code_file = "source-code.txt";

	static String obsolete_concept_file = "retired_and_obsolete_concepts.txt";
	static String ncit_unii_file = "ncit_unii.txt";
	static String FDA_UNII_Subset_file = FDA_UNII_Subset_Code + ".txt";
	static String full_syn_file = "FULL_SYN.txt";

	public Vector concepts_in_subset = null;

	public String unii_link_file = null;
	public String unii_data_file = null;

	private TermSearchUtils termSearchUtils = null;
	private HashSet ncit_code_hset = null;

	private List synonyms = null;
    private HashMap code2SynonymMap = null;
    private HashMap ncitcode2labelMap = null;


    public UNIIDataRetriever(String serviceUrl, String namedGraph) {
		this.serviceUrl = serviceUrl;
		if (!this.serviceUrl.endsWith("/")) {
			this.serviceUrl = this.serviceUrl + "/";
		}
		this.namedGraph = namedGraph;
		System.out.println(this.serviceUrl);
		owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, null, null);
    }

/*
bnode_301c03a7_663e_49c8_be4e_8726b4fc92ea_826645|Cyclophosphamide|C405|FULL_SYN|P90|CYCLOPHOSPHAMIDE|Term Source|P384|FDA
bnode_301c03a7_663e_49c8_be4e_8726b4fc92ea_914386|Fluoxymesterone|C507|FULL_SYN|P90|FLUOXYMESTERONE|Term Source|P384|FDA
*/


    public Vector get_term_source_vec(String source) {
		return retrieveTermSourceData(source);
	}

    public Vector get_term_source_fda_vec() {
		if (term_source_fda_vec == null) {
			term_source_fda_vec = retrieveTermSourceFDAdata();
		}
		return term_source_fda_vec;
	}

    public Vector get_fda_unii_subset_vec() {
		if (fda_unii_subset_vec == null) {
			fda_unii_subset_vec = retrieveUNIIConceptsInSubsetData();
		}
		return fda_unii_subset_vec;
	}

    public Vector get_ncit_concepts_with_unii_code_vec() {
		if (ncit_concepts_with_unii_code_vec == null) {
			ncit_concepts_with_unii_code_vec = retrieveConceptsWithUNIICode();
		}
		return ncit_concepts_with_unii_code_vec;
	}

    public Vector get_ncit_retired_concepts_vec() {
		if (ncit_retired_concepts_vec == null) {
			ncit_retired_concepts_vec = retrieveRetiredConceptData();
		}
		return ncit_retired_concepts_vec;
	}

    public Vector get_term_source_vec() {
		if (term_source_vec == null) {
			term_source_vec = retrieveTermSourceData();
		}
		return term_source_vec;
	}

    public Vector get_term_source_code_vec() {
		if (term_source_code_vec == null) {
			term_source_code_vec = retrieveSourceCodeData();
		}
		return term_source_code_vec;
	}

    public Vector get_full_syn_vec() {
		if (full_syn_vec == null) {
			full_syn_vec = retrievePropertyData("FULL_SYN");
		}
		return full_syn_vec;
	}

    public void retrieveUNIISupportingData() {
		Vector v = null;
		v = retrieveTermSourceFDAdata();
		Utils.saveToFile(term_source_fda_file, v);

		v = retrieveUNIIConceptsInSubsetData();
		Utils.saveToFile(FDA_UNII_Subset_Code + ".txt", v);

	    v = retrieveConceptsWithUNIICode();
	    Utils.saveToFile(ncit_unii_file, v);

		v = retrieveRetiredConceptData();
		Utils.saveToFile(obsolete_concept_file, v);

		v = retrieveTermSourceData();
		Utils.saveToFile(subsource_name_file, v);

		v = retrieveSourceCodeData();
		Utils.saveToFile(source_code_file, v);

        if (!FileUtils.fileExists(full_syn_file)) {
			v = retrievePropertyData("FULL_SYN");
			Utils.saveToFile(full_syn_file, v);
		}
	}

	public Vector retrievePropertyData(String propertyName) {
		Vector v = owlSPARQLUtils.get_property_query(this.namedGraph, propertyName);
        return v;
	}

	public Vector retrieveTermSourceFDAdata() {
		String prop_label = "FULL_SYN";
		String qualifier_label = "Term Source";
		String qualifier_value = "FDA";
		Vector v = owlSPARQLUtils.getAxiomsWithQualifierMatching(this.namedGraph, null, prop_label, qualifier_label, qualifier_value);
        return v;
	}

	public Vector retrieveTermSourceData(String qualifier_value) {
		String prop_label = "FULL_SYN";
		String qualifier_label = "Term Source";
		Vector v = owlSPARQLUtils.getAxiomsWithQualifierMatching(this.namedGraph, null, prop_label, qualifier_label, qualifier_value);
        return v;
	}

/*
bnode_301c03a7_663e_49c8_be4e_8726b4fc92ea_800112|Molecular Abnormality|C3910|FULL_SYN|P90|Molecular Alteration|Subsource Name|P386|caDSR
bnode_301c03a7_663e_49c8_be4e_8726b4fc92ea_800110|Molecular Abnormality|C3910|FULL_SYN|P90|Molecular Abnormality|Subsource Name|P386|caDSR
*/

	public Vector retrieveTermSourceData() {
		String prop_label = "FULL_SYN";
		String qualifier_label = "Subsource Name";
		String qualifier_value = null;
		Vector v = owlSPARQLUtils.getAxiomsWithQualifierMatching(this.namedGraph, null, prop_label, qualifier_label, qualifier_value);
        return v;
	}

/*
bnode_301c03a7_663e_49c8_be4e_8726b4fc92ea_800110|Molecular Abnormality|C3910|FULL_SYN|P90|Molecular Abnormality|Source Code|P385|TCGA
bnode_301c03a7_663e_49c8_be4e_8726b4fc92ea_550723|Gene|C16612|FULL_SYN|P90|Gene|Source Code|P385|TCGA
*/

	public Vector get_term_type_data(String term_type) {
        return retrieveTermTypeData(term_type);
	}

	public Vector retrieveTermTypeData(String term_type) {
		String prop_label = "FULL_SYN";
		String qualifier_label = "Term Type";
		String qualifier_value = term_type;
		Vector v = owlSPARQLUtils.getAxiomsWithQualifierMatching(this.namedGraph, null, prop_label, qualifier_label, qualifier_value);
        return v;
	}

	public Vector retrieveTermTypeData() {
		String prop_label = "FULL_SYN";
		String qualifier_label = "Term Type";
		String qualifier_value = null;
		Vector v = owlSPARQLUtils.getAxiomsWithQualifierMatching(this.namedGraph, null, prop_label, qualifier_label, qualifier_value);
        return v;
	}

	public Vector retrieveSourceCodeData() {
		String prop_label = "FULL_SYN";
		String qualifier_label = "Source Code";
		String qualifier_value = null;
		Vector v = owlSPARQLUtils.getAxiomsWithQualifierMatching(this.namedGraph, null, prop_label, qualifier_label, qualifier_value);
        return v;
	}

	public Vector retrieveUNIIConceptsInSubsetData() {
        Vector v = owlSPARQLUtils.getConceptsInSubset(this.namedGraph, FDA_UNII_Subset_Code);
	    v = new ParserUtils().getResponseValues(v);
	    v = new SortUtils().quickSort(v);
        return v;
	}

	public Vector retrieveConceptsWithUNIICode() {
		Vector v = owlSPARQLUtils.findConceptsWithProperty(this.namedGraph, FDA_UNII_Code);
        return v;
	}

/*
Cyclophosphamide/Fluoxymesterone/Mitolactol/Prednisone/Tamoxifen|C10000|Concept_Status|Obsolete_Concept
Agent Combination Indexed in Open Clinical Trials|C61007|Concept_Status|Obsolete_Concept
*/

	public Vector retrieveRetiredConceptData() {
		String property_name = "Concept_Status";
		String property_value = "Obsolete_Concept";
		Vector v = owlSPARQLUtils.findConceptsWithPropertyMatching(this.namedGraph, property_name, property_value);
		property_value = "Retired_Concept";
		Vector v2 = owlSPARQLUtils.findConceptsWithPropertyMatching(this.namedGraph, property_name, property_value);
		v.addAll(v2);
        return v;
	}

    public static void main(String[] args) {
		String serviceUrl = args[0];
		String namedGraph = args[1];
		UNIIDataRetriever test = new UNIIDataRetriever(serviceUrl, namedGraph);
		test.retrieveUNIISupportingData();
	}
}