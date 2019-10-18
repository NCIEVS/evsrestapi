import gov.nih.nci.evs.restapi.util.*;

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


public class AxiomUtils {
	String serviceUrl = null;
    OWLSPARQLUtils owlSPARQLUtils = null;

    public AxiomUtils(String serviceUrl) {
        this.serviceUrl = serviceUrl;
        this.owlSPARQLUtils = new OWLSPARQLUtils(this.serviceUrl, null, null);
    }

    public Vector getAxioms(String named_graph, String code, String propertyName) {
		return owlSPARQLUtils.getAxioms(named_graph, code, propertyName);
	}

	public List getSynonyms(String named_graph, String code, String propertyName) {
		Vector v = getAxioms(named_graph, code, propertyName);
		return getSynonyms(v);
	}

	public List getSynonyms(Vector v) {
		if (v == null) return null;
		List syn_list = new ArrayList();
        String z_axiom = null;
		String code = null;
		String label = null;
		String termName = null;
		String termGroup = null;
		String termSource = null;
		String sourceCode = null;
		String subSourceName = null;
		String subSourceCode = null;

		String qualifier_name = null;
		String qualifier_value = null;

		HashMap hmap = new HashMap();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
		    Vector u = StringUtils.parseData(line, '|');
		    String key = (String) u.elementAt(0);
		    Vector values = new Vector();
		    if (hmap.containsKey(key)) {
				values = (Vector) hmap.get(key);
			}
			values.add(line);
			hmap.put(key, values);
		}

		Iterator it = hmap.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			Vector values = (Vector) hmap.get(key);

			code = null;
			label = null;
			termName = null;
			termGroup = null;
			termSource = null;
			sourceCode = null;
			subSourceName = null;
			subSourceCode = null;

			for (int i=0; i<values.size(); i++) {
				String line = (String) values.elementAt(i);
				Vector u = StringUtils.parseData(line, '|');
				label = (String) u.elementAt(1);
				code = (String) u.elementAt(2);
				termName = (String) u.elementAt(4);
				qualifier_name = (String) u.elementAt(5);
				qualifier_value = (String) u.elementAt(6);

				if (qualifier_name.compareTo("term-source") == 0 || qualifier_name.compareTo("Term Source") == 0) {
					termSource = qualifier_value;
				} else if (qualifier_name.compareTo("P385") == 0) {
					sourceCode = qualifier_value;
				} else if (qualifier_name.compareTo("term-group") == 0 || qualifier_name.compareTo("Term Type") == 0) {
					termGroup = qualifier_value;
				} else if (qualifier_name.compareTo("source-code") == 0 || qualifier_name.compareTo("Source Code") == 0) {
					sourceCode = qualifier_value;
				} else if (qualifier_name.compareTo("subsource-name") == 0 || qualifier_name.compareTo("Subsource Name") == 0) {
					subSourceName = qualifier_value;
				} else if (qualifier_name.compareTo("subsource-code") == 0 || qualifier_name.compareTo("Subsource Code") == 0) {
					subSourceCode = qualifier_value;
				}
			}

			Synonym syn = new Synonym(
				code,
				label,
				termName,
				termGroup,
				termSource,
				sourceCode,
				subSourceName,
				subSourceCode);
			syn_list.add(syn);
		}
		return syn_list;
	}

	public List getDefinitions(Vector v) {
		if (v == null) return null;
		List def_list = new ArrayList();
        String z_axiom = null;
		String code = null;
		String label = null;
		String desc = null;
		String defSource = null;

		String qualifier_name = null;
		String qualifier_value = null;

		HashMap hmap = new HashMap();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
		    Vector u = StringUtils.parseData(line, '|');
		    String key = (String) u.elementAt(0);
		    Vector values = new Vector();
		    if (hmap.containsKey(key)) {
				values = (Vector) hmap.get(key);
			}
			values.add(line);
			hmap.put(key, values);
		}

		Iterator it = hmap.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			Vector values = (Vector) hmap.get(key);

			code = null;
			label = null;
			desc = null;
			defSource = null;

			qualifier_name = null;
			qualifier_value = null;

			for (int i=0; i<values.size(); i++) {
				String line = (String) values.elementAt(i);
				Vector u = StringUtils.parseData(line, '|');
				label = (String) u.elementAt(1);
				code = (String) u.elementAt(2);
				desc = (String) u.elementAt(4);
				qualifier_name = (String) u.elementAt(5);
				qualifier_value = (String) u.elementAt(6);
                if (qualifier_name.compareTo("P378") == 0 || qualifier_name.compareTo("def-source") == 0 || qualifier_name.compareTo("Definition Source") == 0) {
					defSource = qualifier_value;
				}
			}
			Definition def = new Definition(
				desc,
				defSource);
			def_list.add(def);
		}
		return def_list;
	}

	public List getDefinitions(String named_graph, String code, String propertyName) {
		Vector v = getAxioms(named_graph, code, propertyName);
		return getDefinitions(v);
	}

    public static void main(String[] args) {
		String serviceUrl = args[0];
		String named_graph = args[1];
		AxiomUtils axiomUtils = new AxiomUtils(serviceUrl);
		String code = "C12345";
		String propertyName = "FULL_SYN";
		List list = axiomUtils.getSynonyms(named_graph, code, propertyName);
		for (int i=0; i<list.size(); i++) {
			Synonym syn = (Synonym) list.get(i);
		    System.out.println(syn.toJson());
		}
		propertyName = "DEFINITION";
		list = axiomUtils.getDefinitions(named_graph, code, propertyName);
		for (int i=0; i<list.size(); i++) {
			Definition def = (Definition) list.get(i);
		    System.out.println(def.toJson());
		}

		propertyName = "ALT_DEFINITION";
		list = axiomUtils.getDefinitions(named_graph, code, propertyName);
		for (int i=0; i<list.size(); i++) {
			Definition def = (Definition) list.get(i);
		    System.out.println(def.toJson());
		}

	}
}