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
import org.apache.commons.codec.binary.Base64;
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
 *     Initial implementation kim.ong@ngc.com
 *
 */


/*
		ontologyUri2LabelMap.put("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl", "NCI_Thesaurus");
		ontologyUri2LabelMap.put("http://purl.obolibrary.org/obo/go.owl", "GO");
		ontologyUri2LabelMap.put("http://purl.obolibrary.org/obo/obi.owl", "obi");
		ontologyUri2LabelMap.put("http://purl.obolibrary.org/obo/chebi.owl", "chebi");
		ontologyUri2LabelMap.put("http://id.nlm.nih.gov/mesh/2017-11-07", "mesh");
		ontologyUri2LabelMap.put("http://cbiit.nci.nih.gov/caDSR/20171108", "caDSR");
*/

public class PrefixUtils {
    public static String NCIT_URI = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl";
    public static String NCIT_URI_2 = "http://ncicb.nci.nih.gov/xml/owl/EVS/NCI_Thesaurus.owl";
    public static String GO_URI = "http://purl.obolibrary.org/obo/go.owl";
    public static String obi_URI = "http://purl.obolibrary.org/obo/obi.owl";
    public static String chebi_URI = "http://purl.obolibrary.org/obo/chebi.owl";

    public static String[] URIs = new String[] {NCIT_URI, NCIT_URI_2, GO_URI, obi_URI, chebi_URI};

    public static String codingScheme2URI(String codingScheme) {
		for (int i=0; i<URIs.length; i++) {
			String uri = URIs[i];
			if (uri.indexOf(codingScheme + ".owl") != -1) {
				return uri;
			}
		}
		return NCIT_URI;
	}

    public static String getBasicPrefixes() {
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX xml:<http://www.w3.org/XML/1998/namespace>").append("\n");
		buf.append("PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>").append("\n");
		buf.append("PREFIX owl:<http://www.w3.org/2002/07/owl#>").append("\n");
		buf.append("PREFIX owl2xml:<http://www.w3.org/2006/12/owl2-xml#>").append("\n");
		buf.append("PREFIX protege:<http://protege.stanford.edu/plugins/owl/protege#>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>").append("\n");
		buf.append("PREFIX dc:<http://purl.org/dc/elements/1.1/>").append("\n");
		return buf.toString();
	}

    public static String getPrefixes(String ontology_uri) {
		StringBuffer buf = new StringBuffer();
		if (ontology_uri == null) return buf.toString();
		if (ontology_uri.compareTo(NCIT_URI) == 0) {
			buf.append("PREFIX :<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>").append("\n");
			buf.append("PREFIX base:<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl>").append("\n");
			buf.append("PREFIX Thesaurus:<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>").append("\n");
		} else if (ontology_uri.compareTo(NCIT_URI_2) == 0) {
			buf.append("PREFIX :<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>").append("\n");
			buf.append("PREFIX base:<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl>").append("\n");
			buf.append("PREFIX Thesaurus:<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>").append("\n");
		} else if (ontology_uri.compareTo(GO_URI) == 0 || ontology_uri.compareTo(chebi_URI) == 0 || ontology_uri.compareTo(obi_URI) == 0) {
			buf.append("PREFIX :<http://purl.obolibrary.org/obo/go.owl#>").append("\n");
			buf.append("PREFIX base:<http://purl.obolibrary.org/obo/go.owl>").append("\n");
			buf.append("PREFIX oboInOwl:<http://www.geneontology.org/formats/oboInOwl#>").append("\n");
			buf.append("PREFIX terms:<http://www.geneontology.org/formats/oboInOwl#http://purl.org/dc/terms/>").append("\n");
			buf.append("PREFIX go:<http://purl.obolibrary.org/obo/go#>").append("\n");
			buf.append("PREFIX obo:<http://purl.obolibrary.org/obo/>").append("\n");
		}
		buf.append(getBasicPrefixes());
		return buf.toString();
	}
}
