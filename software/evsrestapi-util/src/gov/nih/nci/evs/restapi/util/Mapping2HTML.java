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


public class Mapping2HTML {
    JSONUtils jsonUtils = null;
    HTTPUtils httpUtils = null;
    String named_graph = null;
    String prefixes = null;
    String serviceUrl = null;
    String restURL = null;
    String named_graph_id = ":NHC0";
    String BASE_URI = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl";

    ParserUtils parser = new ParserUtils();
    HashMap nameVersion2NamedGraphMap = null;
    HashMap ontologyUri2LabelMap = null;
    String version = null;
    String username = null;
    String password = null;

    public Mapping2HTML() {

	}

	public void setVersion(String version) {
		this.version = version;
	}

    public void generate(String inputfile, String source, String target) {
		Vector w = new Vector();
		w.add("<title>" + source + " to " + target + " Mapping");
		w.add("<table>Mapping Entries");
		w.add("<th>Source");
		w.add("<th>Source Code");
		w.add("<th>Source Name");
		w.add("<th>REL");
		w.add("<th>Map Rank");
		w.add("<th>Target");
		w.add("<th>Target Code");
		w.add("<th>Target Name");
		w.add("<data>");

		//String REL = "SY";
		//int rank = 1;

//FDA|09936A4QSN|SIMPINICLINE [INN]|OT|4|NCI Thesaurus|C175854|Simpinicline Citrate

		Vector v = Utils.readFile(inputfile);
		for (int i=0; i<v.size(); i++) {
		    String line = (String) v.elementAt(i);
		    Vector u = StringUtils.parseData(line, '|');
		    //source = (String) u.elementAt(0);
			String sourceCode = (String) u.elementAt(1);
			String sourceTerm = (String) u.elementAt(2);
			String REL = (String) u.elementAt(3);
			String rank_str = (String) u.elementAt(4);
			String targetCode = (String) u.elementAt(6);
			targetCode = HyperlinkHelper.toHyperlink(targetCode);
			String targetLabel = (String) u.elementAt(7);
			String t = source + "|" + sourceCode + "|" + sourceTerm + "|" + REL + "|" + rank_str + "|"
			       + target + "|" + targetCode + "|" + targetLabel;
			w.add(t);
		}
		w.add("</data>");
		w.add("</table>");
		if (this.version != null) {
			w.add("<footer>(Source: NCI Thesaurus, version " + this.version + ")");
		}
		HTMLTable.generate(w);
	}

	public static void main(String[] args) {
		long ms = System.currentTimeMillis();
		Mapping2HTML test = new Mapping2HTML();
		String inputfile = args[0];
		String source = args[1];
		String target = args[2];
		test.generate(inputfile, source, target);
	}
}