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


public class ValueSetConfigurationFileGenerator {
	private static String CACORE = "ftp://ftp1.nci.nih.gov/pub/cacore/";
	private static String VALUESET_URI = "http://evs.nci.nih.gov/valueset/";

	private String owlfile = null;
    private OWLScanner owlScanner = null;
    Vector owl_vec = null;

    private String PUBLISH_VALUE_SET_CODE = "P372";

    public ValueSetConfigurationFileGenerator(String owlfile) {
        owlScanner = new OWLScanner(owlfile);
        owl_vec = owlScanner.get_owl_vec();
        System.out.println("owl_vec.size: " + owl_vec.size());
    }

    public Vector split(String t) {
		return StringUtils.parseData(t, '|');
	}

    public String parseProperty(String t) {
		t = t.trim();
		if (t.indexOf("rdf:resource") != -1) {
			int n = t.indexOf("rdf:resource");
			String propertyCode = t.substring(1, n-1);
		    int m = t.lastIndexOf("#");
			String s = t.substring(m, t.length());
			m = s.indexOf("\"");
			String target = s.substring(1, m);
			return propertyCode + "|" + target;
		} else {
			int n = t.indexOf(">");
			String propertyCode = t.substring(1, n);

			String end_tag = "</" + propertyCode + ">";
			if(!t.endsWith(end_tag)) {
				return("ERROR parsing " + t);
			}
			String s = t.substring(n, t.length());
			int m = s.lastIndexOf("<");
			String propertyValue = s.substring(1, m);
        	return propertyCode + "|" + propertyValue;
		}
	}

    public Vector extractValueSetConfig() {
        Vector w = new Vector();
        boolean istart = false;
        boolean istart0 = false;
        String classId = null;
        boolean switch_off = false;
        ValueSetConfig vsc = null;
        String published = null;

        for (int i=0; i<owl_vec.size(); i++) {
			String t = (String) owl_vec.elementAt(i);
			if (t.indexOf("// Classes") != -1) {
				istart0 = true;
			}
		    if (t.indexOf("</rdf:RDF>") != -1) {
				break;
			}

			if (t.indexOf("<owl:Axiom>") != -1) {
				switch_off = true;
			}
			if (t.indexOf("</owl:Axiom>") != -1) {
				switch_off = false;
			}

			if (t.indexOf("<!-- http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#") != -1 && t.endsWith("-->")) {
				int n = t.lastIndexOf("#");
				t = t.substring(n, t.length());
				n = t.lastIndexOf(" ");
				classId = t.substring(1, n);
				if (vsc != null && published != null && published.compareTo("Yes") == 0) {
					try {
						if (vsc.getUri().indexOf("http") == -1) {
							vsc.setUri(VALUESET_URI + vsc.getUri());
						}
						w.add(vsc);
					} catch (Exception ex) {

					}
				}

				vsc = new ValueSetConfig();
				vsc.setUri(classId);
				published = null;
				if (istart0) {
					istart = true;
				}
			}
			if (istart) {

/*
	private String name;
	private String uri;
	private String reportURI;
	private String extractionRule;
*/

				t = t.trim();
				if (t.startsWith("<") && t.indexOf("rdf:resource=") != -1 && t.indexOf("owl:") == -1 && t.indexOf("rdfs:subClassOf") == -1) {

					int n = t.indexOf(">");
                    if (n != -1) {
						if (!switch_off) {
							//w.add(classId + "|" + parseProperty(t));
					    }
					}

				} else if (t.startsWith("<") && t.indexOf("rdf:resource=") == -1 && t.indexOf("owl:") == -1 && t.indexOf("rdfs:subClassOf") == -1
				    && t.indexOf("rdf:Description") == -1 && t.indexOf("rdfs:subClassOf") == -1) {
					int n = t.indexOf(">");
                    if (n != -1) {
						if (!switch_off) {
							String retstr = parseProperty(t);
							Vector u = split(retstr);
							String code = (String) u.elementAt(0);
							if (code.compareTo("P108") == 0) {
								vsc.setName((String) u.elementAt(1));
							} else if (code.compareTo("P374") == 0) {
								String value = (String) u.elementAt(1);
								if (value.compareTo("null") == 0) {
									vsc.setReportURI(null);
								} else {
									vsc.setReportURI(CACORE + value);
								}
								vsc.setExtractionRule((String) u.elementAt(2));
							} else if (code.compareTo("P322") == 0) {
								vsc.setUri(VALUESET_URI +
								    (String) u.elementAt(1) + "/" + vsc.getUri());
							} else if (code.compareTo("P372") == 0) {
								published = (String) u.elementAt(1);
							}
						}
					}
				}
		    }
		}
		if (vsc != null && published != null && published.compareTo("Yes") == 0) {
			try {
				if (vsc.getUri().indexOf("http") == -1) {
					vsc.setUri(VALUESET_URI + vsc.getUri());
				}
				w.add(vsc);
			} catch (Exception ex) {

			}
		}
		return w;
	}

    public static void main(String[] args) {
        String owlfile = args[0];
        ValueSetConfigurationFileGenerator vscfg = new ValueSetConfigurationFileGenerator(owlfile);
        Vector w = vscfg.extractValueSetConfig();
        Vector w0 = new Vector();
        for (int i=0; i<w.size(); i++) {
		    ValueSetConfig vsc = (ValueSetConfig) w.elementAt(i);
		    if (vsc != null) {
	   	    	w0.add(vsc.getName() + "|" + vsc.getUri() + "|" + vsc.getReportURI() + "|" + vsc.getExtractionRule());
		    }
		}
		w0 = new SortUtils().quickSort(w0);
        Utils.saveToFile("vscfg.txt", w0);
    }
}
