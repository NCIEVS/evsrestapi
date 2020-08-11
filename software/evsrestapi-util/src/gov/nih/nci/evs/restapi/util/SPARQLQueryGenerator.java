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


public class SPARQLQueryGenerator {
    private OWLSPARQLUtils owlSPARQLUtils = null;

    private String sparql_serviceUrl = "https://sparql-evs.nci.nih.gov/sparql?query=";
    private String sparql_ns = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl";

    public SPARQLQueryGenerator() {
        owlSPARQLUtils = new OWLSPARQLUtils(sparql_serviceUrl, null,null);
        owlSPARQLUtils.set_named_graph(sparql_ns);
    }

    public static void main(String[] args) {
		long ms = System.currentTimeMillis();
        String owlfile = args[0];
        String type = args[1];
		OWLScanner scanner = new OWLScanner(owlfile);
		int n = owlfile.lastIndexOf(".");
		String outputfile = owlfile.substring(0, n) + "_" + StringUtils.getToday() + ".owl";
		Vector v = scanner.scanOwlTags();

		if (type.compareToIgnoreCase("roles") == 0) {
			v = scanner.fileterTagData(v, "owl:Restriction");
			for (int i=0; i<v.size()/2; i++) {
				int i1 = i+1;
				int j = i*2;
				String path = (String) v.elementAt(j);
				System.out.println("/*");
				System.out.println("\n(" + i1 + ") " + path);
				System.out.println("\n");
				scanner.printPath(path);
                System.out.println("*/");
				scanner.print_restriction_query(path, i1);
			}
		} else {
			Vector w = new Vector();
			for (int i=0; i<v.size(); i++) {
				String t = (String) v.elementAt(i);
				if (t.endsWith("|owl:Class")) {
					w.add(t);
				}
			}
			Utils.saveToFile("superclass_scanOwlTags", w);
			v = w;
	/*
			Vector v = new Vector();
			v.add("owl:Class|rdfs:subClassOf");
			v.add("owl:Class|rdfs:subClassOf|owl:Class|owl:intersectionOf|owl:Class");
			v.add("owl:Class|owl:equivalentClass|owl:Class|owl:intersectionOf|owl:Class");
	*/
			for (int i=0; i<v.size(); i++) {
				int i1 = i+1;
				String path = (String) v.elementAt(i);
				System.out.println("/*");
				System.out.println("\n(" + i1 + ") " + path);
				System.out.println("\n");
				scanner.printPath(path);
				System.out.println("*/");
				scanner.print_superclass_query(path, i1);
			}
		}
	}
}

