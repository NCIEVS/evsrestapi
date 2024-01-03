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
		//System.out.println(line);

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
		return extractProperties(owl_vec, null);
	}

	public static Vector extractProperties(Vector owl_vec, String propertyCode) {
		Vector v = new Vector();
		Vector w = new Vector();
		String id = null;
		boolean include = false;
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

			//System.out.println("START " + line);
			if (line.indexOf("<owl:Axiom>") != -1) {
                include = false;
			}

			if (isOpenClass(line)) {
				w = new Vector();
				id = extractIdFromOpenClassLine(line);
				//System.out.println("id: " + id);
				include = true;

			} else if (isCloseClass(line)) {
				if (w != null && w.size() > 0) {
					v.addAll(prefixId(id, w));
				}
				include = false;
				//System.out.println("isCloseClass: " + line);

			} else {
				try {
					/*
					String t = xml2Delimited(line);
					if (t != null) {
						if (propertyCode != null) {
							String tagKey = getTagKey(t);
							if (tagKey.compareTo(propertyCode) == 0) {
								w.add(t);
							}
						} else {
							//System.out.println("t: " + t);
							w.add(t);
						}
					}
					*/
					String t = getPropertyNameAndValue(line);
					if (t != null && include) {
						if (propertyCode != null) {
							String tagKey = getTagKey(t);
							if (tagKey.compareTo(propertyCode) == 0) {
								w.add(t);
							}
						} else {
							w.add(t);
						}
					}
				} catch (Exception ex) {
					//System.out.println(line);
					ex.printStackTrace();
				}
			}
		}
		//removePrefix
		//return v;
		return removePrefix(ns, v);
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
		return removePrefix(ns, v);
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

    public static String getPropertyNameAndValue(String line) {
       line = line.trim();
       int n1 = line.indexOf(">");
       if (n1 != -1) {
		   String open_tag = line.substring(1, n1);
		   int n2 = line.lastIndexOf("</");
		   if (n2 != -1) {
			   String close_tag = line.substring(n2+2, line.length()-1);
			   if (open_tag.compareTo(close_tag) == 0) {
				   String t = xml2Delimited(line);
				   return t;
			   }
		   }
	   }
	   return null;
	}

	public static Vector extractAssociations(Vector owl_vec) {
		String associationCode = null;
		return extractAssociations(owl_vec, associationCode);
	}


	public static Vector extractAssociations(Vector owl_vec, String associationCode) {
		Vector v = new Vector();
		Vector w = new Vector();
		String id = null;
		boolean include = false;
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

			//System.out.println("START " + line);
			if (line.indexOf("<owl:Axiom>") != -1) {
                include = false;
			}

			if (isOpenClass(line)) {
				w = new Vector();
				id = extractIdFromOpenClassLine(line);
				include = true;

			} else if (isCloseClass(line)) {
				if (w != null && w.size() > 0) {
					v.addAll(prefixId(id, w));
				}
				include = false;
			} else {
				try {
					String t = getAssociationNameAndValue(line);
					if (t != null && include) {
						if (associationCode != null) {
							int n = t.indexOf("|");
							String assoCode = t.substring(0, n);
							if (assoCode.compareTo(associationCode) == 0) {
								w.add(t);
							}
						} else {
							int n = t.indexOf("|");
							String assoCode = t.substring(0, n);
							if (assoCode.indexOf(":") == -1) {
								w.add(t);
							}
						}
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
		return removePrefix(ns, v);
	}

    public static String getAssociationNameAndValue(String line) {
       line = line.trim();
       String t = null;
       String rdf_resource = "rdf:resource";
       int n1 = line.indexOf(rdf_resource);
       if (n1 != -1) {
		   String associationName = line.substring(1, n1-1);
		   int n2 = line.lastIndexOf("/>");
		   if (n2 != -1) {
			   String associationValue = line.substring(n1+rdf_resource.length()+2, line.length()-3);
			   t = associationName + "|" + associationValue;
		   }
	   }
	   return t;
	}

	public static Vector extractAllProperties(Vector owl_vec) {
		Vector v1 = extractProperties(owl_vec);
		Vector v2 = extractAssociations(owl_vec);
		v1.addAll(v2);
		return v1;
	}

	public static boolean isInteger(String str) {
		try {
			Integer.parseInt(str);
			return true;
		} catch (NumberFormatException nfe) {
			return false;
		}
	}

	public static boolean isConceptCode(String s) {
		if (s.length() < 2) return false;
		char c = s.charAt(0);
		if (c != 'C') return false;

		String t = s.substring(1, s.length());
		return isInteger(t);
	}

/*
C99999|P90|PERCUTANEOUS CORONARY INTERVENTION (PCI) FOR HIGH RISK NON-ST ELEVATION MYOCARDIAL INFARCTION OR UNSTABLE ANGINA|P383$PT|P384$CDISC
C99999|P90|Percutaneous Coronary Intervention for High Risk Non-ST Elevation Myocardial Infarction or Unstable Angina|P383$PT|P384$NCI
*/
/*
    public static HashMap getPropertyQualifier2CountMap(Vector owl_vec) {
        Vector w = ScannerUtils.extractAxioms(owl_vec);
        HashMap hmap = new HashMap();
        for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String s0 = (String) u.elementAt(0);
			if (isConceptCode(s0)) {
				String propertyCode = (String) u.elementAt(1);
				if (u.size() > 3) {
					for (int j=3; j<u.size(); j++) {
						String q = (String) u.elementAt(j);
						Vector u2 = StringUtils.parseData(q, '$');
						String q_code = (String) u2.elementAt(0);
						int count = 0;
						if (hmap.containsKey(propertyCode + "|" + q_code)) {
							Integer int_obj = (Integer) hmap.get(propertyCode + "|" + q_code);
							count = int_obj.intValue();
						}
						count++;
						hmap.put(propertyCode + "|" + q_code, new Integer(count));
					}
			    }
			}
		}
		return hmap;
	}
/*
    public static HashMap createPropertyQualifier2CountMap(Vector w) {
        HashMap hmap = new HashMap();
        for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String s0 = (String) u.elementAt(0);
			if (isConceptCode(s0)) {
				String propertyCode = (String) u.elementAt(1);
				if (u.size() > 3) {
					for (int j=3; j<u.size(); j++) {
						String q = (String) u.elementAt(j);
						Vector u2 = StringUtils.parseData(q, '$');
						String q_code = (String) u2.elementAt(0);
						int count = 0;
						if (hmap.containsKey(propertyCode + "|" + q_code)) {
							Integer int_obj = (Integer) hmap.get(propertyCode + "|" + q_code);
							count = int_obj.intValue();
						}
						count++;
						hmap.put(propertyCode + "|" + q_code, new Integer(count));
					}
			    }
			}
		}
		return hmap;
	}
*/


    public static void dumpPropertyQualifier2CountMap(HashMap hmap) {
		Iterator it = hmap.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			Integer int_obj = (Integer) hmap.get(key);
			System.out.println(key + " " + int_obj.intValue());
		}
	}
}

