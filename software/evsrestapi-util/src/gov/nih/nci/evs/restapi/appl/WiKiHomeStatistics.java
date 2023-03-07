package gov.nih.nci.evs.restapi.appl;
import gov.nih.nci.evs.restapi.util.*;

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
import java.nio.charset.Charset;

import java.time.Duration;
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


public class WiKiHomeStatistics {
	String serviceUrl = null;
	String named_graph = null;
	String username = null;
	String password = null;

	int councept_cocunt= -1;
	int def_count = -1;
	int relationship_count = -1;

	OWLScanner owlScanner = null;
	static String NCI_THESAURUS_OWL = "ThesaurusInferred_forTS.owl";

	public WiKiHomeStatistics(String owlfile) {
        owlScanner = new OWLScanner(owlfile);
	}

    public String wikiStat() {
        HashMap hmap = owlScanner.getCode2LabelMap();
        int n1 = hmap.keySet().size();
        System.out.println("Number of concepts: " + n1);

        Vector v1 = owlScanner.extractProperties(owlScanner.get_owl_vec(), "P97");
        int n2 = v1.size();
        System.out.println("Number of DEFINITION: " + n2);
        Vector v2 = owlScanner.extractProperties(owlScanner.get_owl_vec(), "P325");
        int n3 = v2.size();
        System.out.println("Number of ALT_DEFINITION: " + n3);
        int total = v1.size() + v2.size();
        int n4 = n2 + n3;
        System.out.println("Total number of textual definitions: " + n4);

        Vector v3 = owlScanner.extractAssociations(owlScanner.get_owl_vec());
        int n5 = v3.size();
        System.out.println("Number of associations: " + n5);
        Vector v4 = owlScanner.extractOWLRestrictions(owlScanner.get_owl_vec());
        int n6 = v4.size();
        total = v3.size() + v4.size();
        int n7 = n5 + n6;
        System.out.println("Number of roles: " + n6);
        System.out.println("Total number of inter-concept relationships: " + n7);

        StringBuffer buf = new StringBuffer();

 String stat = "NCI Thesaurus (NCIt) is NCI's reference terminology and core biomedical ontology, covering some "
 + n1
 + " key biomedical concepts with a rich set of terms, codes, "
 + n4
 + " textual definitions, and over "
 + n7 +
 " inter-concept relationships. NCIt combined and extended core NCI terminologies within a scientifically and technically rigorous framework. NCIt is now a broadly shared coding and semantic infrastructure resource - over half of NCIt concepts include content explicitly tagged by one or more EVS partners (see the shared terminology development wiki page ).";
         return stat;
	}

	public static void main(String[] args) {
		String owlfile = NCI_THESAURUS_OWL;
		if (args.length == 1) {
			owlfile = args[0];
		}
		WiKiHomeStatistics test = new WiKiHomeStatistics(owlfile);
		String stat = test.wikiStat();
		System.out.println("\n" + stat);
	}
}

