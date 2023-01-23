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


public class ScannerUtils {
    public static String openAxiomTag = "<owl:Axiom>";
    public static String closeAxiomTag = "</owl:Axiom>";
    public static String ns = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#";
    //Vector owl_vec = null;

    //public ScannerUtils(Vector owl_vec) {
	//	this.owl_vec = owl_vec;
	//}

    public static boolean isIDCommentLine(String line) {
		line = line.trim();
		if (line.startsWith("<!-- http") && line.endsWith(" -->")) return true;
		return false;
	}

    public static boolean isAnnotationPropertyLine(String line) {
		line = line.trim();
		if (line.startsWith("<owl:AnnotationProperty ") && line.endsWith("\">")) return true;
		return false;
	}

    public static String getTagKey(String line) {
		Vector u = StringUtils.parseData(line, '|');
		return (String) u.elementAt(0);
	}

    public static String getTagValue(String line) {
		Vector u = StringUtils.parseData(line, '|');
		return (String) u.elementAt(1);
	}

    public static String xml2Delimited(String line) {
		line = line.trim();
		if (line.length() == 0) return null;
		int n = line.indexOf(" ");
		if (n != -1) {
			String tag = line.substring(1, n);
			String endTag = "</" + tag + ">";
			if (line.endsWith(endTag)) {
				String s = line.substring(0, line.length()-endTag.length());
				int m = s.lastIndexOf(">");
				String value = s.substring(m+1, s.length());
				return tag + "|" + value;
			} else {
				endTag = "/>";
				int n1 = line.indexOf("\"");
				int n2 = line.lastIndexOf("\"");
				if (n1 != -1 && n2 != -1) {
					String value = line.substring(n1+1, n2);
					return tag + "|" + value;
				}
			}
		}
		n = line.indexOf(">");
		if (n != -1) {
			String openTag = line.substring(1, n);
			String endTag = "</" + openTag + ">";
			if (line.endsWith(endTag)) {
				int n1 = line.indexOf(">");
				int n2 = line.lastIndexOf("<");
				if (n1 != -1 && n2 != -1) {
					String value = line.substring(n1+1, n2);
					return openTag + "|" + value;
				}
			}
		}
		return null;
    }

    public static String removePrefix(String ns, String t) {
		return t.replace(ns, "");
	}

    public static Vector removePrefix(String ns, Vector v) {
		Vector w = new  Vector();
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			w.add(removePrefix(ns, t));
	    }
	    return w;
	}

//    <owl:Class rdf:about="http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#C104431">
	public static boolean isOpenClass(String line0) {
		String line = line0;
		//line = line.trim();
		if (line.indexOf("owl:Class rdf:about=") != -1 && line.endsWith(">")) {
			return true;
		}
		return false;
	}

	public static boolean isCloseClass(String line0) {
		String line = line0;
		//line = line.trim();
		if (line.compareTo("    </owl:Class>") == 0) return true;
		return false;
	}

	public static String extractIdFromOpenClassLine(String line) {
		line = line.replace(">", "/>");
		String t = xml2Delimited(line);
		Vector u = StringUtils.parseData(t, '|');
		return (String) u.elementAt(1);
	}

	public static Vector extractClassIDs(Vector owl_vec) {
		Vector w = new Vector();
		String id = null;
		for (int i=0; i<owl_vec.size(); i++) {
			String line = (String) owl_vec.elementAt(i);
			if (isOpenClass(line)) {
				id = extractIdFromOpenClassLine(line);
                w.add(id);
			}
		}
		return w;
	}

	public static Vector extractProperties(Vector owl_vec) {
		return extractProperties(null);
	}

	public static Vector extractProperties(Vector owl_vec, String propertyCode) {
		Vector v = new Vector();
		Vector w = new Vector();
		String id = null;
		for (int i=0; i<owl_vec.size(); i++) {
			String line = (String) owl_vec.elementAt(i);
			//////////////////////////////////
			if (line.indexOf("General axioms") != -1) break;
			while (!line.endsWith(">") && i < owl_vec.size()-1) {
				i++;
				String nextLine = (String) owl_vec.elementAt(i);
				nextLine = nextLine.trim();
				line = line + " " + nextLine;
			}
			//////////////////////////////////
			if (isOpenClass(line)) {
				w = new Vector();
				id = extractIdFromOpenClassLine(line);
			} else if (isCloseClass(line)) {
				if (w != null && w.size() > 0) {
					v.addAll(prefixId(id, w));
				}
			} else {
				String t = xml2Delimited(line);
				if (t != null) {
					if (propertyCode != null) {
						String tagKey = getTagKey(t);
						if (tagKey.compareTo(propertyCode) == 0) {
							w.add(t);
						}
					} else {
						w.add(t);
					}
				}
			}
		}
		return v;
	}

	public static Vector prefixId(String id, Vector v) {
		Vector w = new Vector();
        for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			w.add(id + "|" + t);
		}
		return w;
	}


    public static Vector extractAxioms(Vector owl_vec) {
		Vector v = new Vector();
		String annotatedSource = null;
		String annotatedProperty = null;
		String annotatedTarget = null;
		String onProperty = null;
		String someValuesFrom = null;
		Vector qualifiers = new Vector();
		boolean inAxiom = false;

		for (int i=0; i<owl_vec.size(); i++) {
			String line = (String) owl_vec.elementAt(i);
			if (line.indexOf("General axioms") != -1) break;
			//////////////////////////////////
			while (!line.endsWith(">") && i < owl_vec.size()-1) {
				i++;
				String nextLine = (String) owl_vec.elementAt(i);
				nextLine = nextLine.trim();
				line = line + " " + nextLine;
			}
			//////////////////////////////////
			line = line.trim();
			if (line.indexOf(openAxiomTag) != -1) {
				annotatedSource = null;
				annotatedProperty = null;
				annotatedTarget = null;
				onProperty = null;
				someValuesFrom = null;
				qualifiers = new Vector();
				inAxiom = true;

			} else if (line.indexOf(closeAxiomTag) != -1) {
				StringBuffer buf = new StringBuffer();
				buf.append(annotatedSource).append("|");
				buf.append(annotatedProperty).append("|");
				if (annotatedTarget != null) {
					buf.append(annotatedTarget).append("|");
				} else {
					buf.append(onProperty).append("$").append(someValuesFrom).append("|" );
				}
				for (int j=0; j<qualifiers.size(); j++) {
					String keyAndValue = (String) qualifiers.elementAt(j);
					buf.append(keyAndValue).append("|");
				}
				String s = buf.toString();
				s = s.substring(0, s.length()-1);
				v.add(s);
				inAxiom = false;
			} else {
				if (inAxiom) {
					String t = xml2Delimited(line);
					if (t != null && t.length() > 0) {
						Vector u = StringUtils.parseData(t, '|');
						if (u.size() == 2) {
							String key = (String) u.elementAt(0);
							String value = (String) u.elementAt(1);
							if (key.compareTo("owl:annotatedSource") == 0) {
								annotatedSource = value;
							} else if (key.compareTo("owl:annotatedProperty") == 0) {
								annotatedProperty = value;
							} else if (key.compareTo("owl:annotatedTarget") == 0) {
								annotatedTarget = value;
							} else if (key.compareTo("owl:onProperty") == 0) {
								onProperty = value;
							} else if (key.compareTo("owl:someValuesFrom") == 0) {
								someValuesFrom = value;
							} else {
								qualifiers.add(key + "$" + value);
							}
						}
					}
				}
			}
		}
		return v;
	}

    public static void test(String[] args) {
        String line = "<agr rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">HGNC:11205</agr>";
        System.out.println(line + "\n" + xml2Delimited(line));
        line = "<rdfs:label>Funding Category Code</rdfs:label>";
        System.out.println(line + "\n" + xml2Delimited(line));
        line = "<owl:AnnotationProperty rdf:about=\"http://ncicb.nci.nih.gov/genenames.org/HGNC.owl#agr\"/>";
        System.out.println(line + "\n" + xml2Delimited(line));
        line = "<P90>Nez Perce County, Idaho</P90>";
        System.out.println(line + "\n" + xml2Delimited(line));
        line = "<owl:annotatedSource rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#C108273\"/>";
        System.out.println(line + "\n" + xml2Delimited(line));
        String t = xml2Delimited(line);
        String ns = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#";
        System.out.println(line + "\n" + removePrefix(ns, t));
	}

    public static void main(String[] args) {
        String owlfile = args[0];
        Vector owl_vec = Utils.readFile(owlfile);
        Vector w = ScannerUtils.extractProperties(owl_vec);
        Utils.saveToFile("property_" + owlfile, w);
        w = ScannerUtils.extractAxioms(owl_vec);
        Utils.saveToFile("axiom_" + owlfile, w);
	}
}