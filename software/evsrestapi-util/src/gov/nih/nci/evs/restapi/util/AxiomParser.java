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


public class AxiomParser {
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
    OWLSPARQLUtils owlSPARQLUtils = null;

    public AxiomParser() {

	}

    public AxiomParser(String serviceUrl, String named_graph, String username, String password) {
    	this.named_graph = named_graph;
        this.owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, username, password);
        this.owlSPARQLUtils.set_named_graph(named_graph);
    }

    public OWLSPARQLUtils getOWLSPARQLUtils() {
		return this.owlSPARQLUtils;
	}


	public static HashSet getDistinctAxiomIds(Vector v) {
		HashSet hset = new HashSet();
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(t, '|');
			String axiom_id = (String) u.elementAt(0);
			if (!hset.contains(axiom_id)) {
				hset.add(axiom_id);
			}
		}
		return hset;
	}

	public static Vector getAxiomsById(Vector v, String id) {
		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(t, '|');
			String axiom_id = (String) u.elementAt(0);
			if (axiom_id.compareTo(id) == 0) {
				w.add(t);
			}
		}
		return w;
	}


	public static Vector getHashSetKeys(HashSet hset) {
        Vector keys = new Vector();
        if (hset == null) {
			return new Vector();
		}
        Iterator it = hset.iterator();
        while (it.hasNext()) {
			String key = (String) it.next();
			keys.add(key);
		}
		return keys;
	}

	public static String getAxiomAnnotatedSourceCode(Vector w) {
		return getAxiomAnnotatedSourceCode((String) w.elementAt(0));
	}

	public static String getAxiomAnnotatedSourceCode(String axiom_line) {
		Vector u = StringUtils.parseData(axiom_line, '|');
		return (String) u.elementAt(1);
	}

	public static String getAxiomAnnotatedSourceLabel(Vector w) {
		return getAxiomAnnotatedSourceLabel((String) w.elementAt(0));
	}

	public static String getAxiomAnnotatedSourceLabel(String axiom_line) {
		Vector u = StringUtils.parseData(axiom_line, '|');
		return (String) u.elementAt(2);
	}

	public static String getAxiomAnnotatedProperty(Vector w) {
		return getAxiomAnnotatedProperty((String) w.elementAt(0));
	}

	public static String getAxiomAnnotatedProperty(String axiom_line) {
		Vector u = StringUtils.parseData(axiom_line, '|');
		return (String) u.elementAt(4);	}

	public static String getAxiomAnnotatedTarget(Vector w) {
		return getAxiomAnnotatedTarget((String) w.elementAt(0));
	}

	public static String getAxiomAnnotatedTarget(String axiom_line) {
		Vector u = StringUtils.parseData(axiom_line, '|');
		return (String) u.elementAt(5);
	}

	public static HashMap getAxiomQualifiers(Vector w) {
		HashMap hmap = new HashMap();
//	(2) bnode_f2caede0_a728_4542_84bd_b5c447ff981a_8862453|C12345|Ciliary Body|P325|ALT_DEFINITION|Circumferential tissue located behind the iris and composed of muscle and epithelium.|P378|Definition Source|CDISC
		for (int i=0; i<w.size(); i++) {
			String axiom_line = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(axiom_line, '|');
			String qualifierName = (String) u.elementAt(7);
			String qualifierValue = (String) u.elementAt(8);
			hmap.put(qualifierName, qualifierValue);
		}
		return hmap;
	}

	public static Object axiomData2Object(Vector w) {
		String type = getAxiomAnnotatedProperty(w);
	    String code = getAxiomAnnotatedSourceCode(w);
	    String label = getAxiomAnnotatedSourceLabel(w);
        String target = getAxiomAnnotatedTarget(w);
        HashMap hmap = getAxiomQualifiers(w);

        if (type.compareTo("FULL_SYN") == 0) {
			Synonym syn = new Synonym(
				code,
				label,
				target,
				null,
				null,
				null,
				null,
				null);
			Iterator it = hmap.keySet().iterator();
			while (it.hasNext()) {
				String key = (String) it.next();
                if (key.compareTo("Term Source") == 0) {
					syn.setTermSource((String) hmap.get(key));
                } else if (key.compareTo("Term Type") == 0) {
					syn.setTermGroup((String) hmap.get(key));
                } else if (key.compareTo("Source Code") == 0) {
					syn.setSourceCode((String) hmap.get(key));
                } else if (key.compareTo("Subsource Name") == 0) {
					syn.setSubSourceName((String) hmap.get(key));
				}
			}
            return syn;

         } else if (type.compareTo("DEFINITION") == 0) {
			Definition def = new Definition(
				code,
				label,
				target,
				null,
				null);
			Iterator it = hmap.keySet().iterator();
			while (it.hasNext()) {
				String key = (String) it.next();
                if (key.compareTo("Definition Source") == 0) {
					def.setSource((String) hmap.get(key));
                } else if (key.compareTo("attribution") == 0) {
					def.setAttribution((String) hmap.get(key));
                }
			}
            return def;

         } else if (type.compareTo("ALT_DEFINITION") == 0) {
			AltDefinition def = new AltDefinition(
				code,
				label,
				target,
				null,
				null);
			Iterator it = hmap.keySet().iterator();
			while (it.hasNext()) {
				String key = (String) it.next();
                if (key.compareTo("Definition Source") == 0) {
					def.setSource((String) hmap.get(key));
                } else if (key.compareTo("attribution") == 0) {
					def.setAttribution((String) hmap.get(key));
                }
			}
            return def;

         } else if (type.compareTo("Maps_To") == 0) {
			MapToEntry entry = new MapToEntry(
				code,
				label,
				null,
				null,
				null,
				null,
				null,
				null);

			Iterator it = hmap.keySet().iterator();
			while (it.hasNext()) {
				String key = (String) it.next();
                if (key.compareTo("Relationship_to_Target") == 0) {
					entry.setRelationshipToTarget((String) hmap.get(key));
                } else if (key.compareTo("Target_Term_Type") == 0) {
					entry.setTargetTermType((String) hmap.get(key));
                } else if (key.compareTo("Target_Code") == 0) {
					entry.setTargetCode((String) hmap.get(key));
				} else if (key.compareTo("Target_Terminology") == 0) {
					entry.setTargetTerminology((String) hmap.get(key));
				} else if (key.compareTo("Target_Terminology_Version") == 0) {
					entry.setTargetTerminologyVersion((String) hmap.get(key));
				}
			}
            return entry;

         } else if (type.compareTo("GO_Annotation") == 0) {
			GoAnnotation go = new GoAnnotation(
				code,
				label,
				target,
				null,
				null,
				null,
				null);

			Iterator it = hmap.keySet().iterator();
			while (it.hasNext()) {
				String key = (String) it.next();
                if (key.compareTo("go-id") == 0) {
					go.setGoId((String) hmap.get(key));
                } else if (key.compareTo("go-evi") == 0) {
					go.setGoEvi((String) hmap.get(key));
 				} else if (key.compareTo("go-source") == 0) {
					go.setGoSource((String) hmap.get(key));
				} else if (key.compareTo("source-date") == 0) {
					go.setSourceDate((String) hmap.get(key));
				}
			}
            return go;

		}
        return null;

	}

    public static void printAxiomObject(Object obj) {
		if (obj != null) {
			if (obj instanceof Synonym) {
				Synonym syn = (Synonym) obj;
				System.out.println(syn.toJson());
			} else if (obj instanceof Definition) {
				Definition def = (Definition) obj;
				System.out.println(def.toJson());
			} else if (obj instanceof AltDefinition) {
				AltDefinition def = (AltDefinition) obj;
				System.out.println(def.toJson());
			} else if (obj instanceof MapToEntry) {
				MapToEntry entry = (MapToEntry) obj;
				System.out.println(entry.toJson());
			} else if (obj instanceof GoAnnotation) {
				GoAnnotation go = (GoAnnotation) obj;
				System.out.println(go.toJson());
			}
		}
	}

	public static HashMap getAxiomId2DataHashMap(Vector v) {
		HashMap hmap = new HashMap();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String key = (String) u.elementAt(0);
			Vector w = new Vector();
			if (hmap.containsKey(key)) {
				w = (Vector) hmap.get(key);
			}
			w.add(line);
			hmap.put(key, w);
		}
		return hmap;
	}

	public Vector getAxioms(String named_graph, String code) {
		Vector w = new Vector();
		Vector v = getAxiomData(named_graph, code);
		HashMap hmap = getAxiomId2DataHashMap(v);
		Iterator it = hmap.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			Vector data = (Vector) hmap.get(key);
			Object obj = axiomData2Object(data);
			w.add(obj);
		}
		return w;
	}

	public Vector getAxiomData(String named_graph, String code) {
		return owlSPARQLUtils.get_axioms_by_code(named_graph, code);
	}

    public static void main(String[] args) {
		long ms = System.currentTimeMillis();
		String serviceUrl = args[0];
		String named_graph = args[1];
		String username = args[2];
		String password = args[3];
        AxiomParser test = new AxiomParser(serviceUrl, named_graph, username, password);
        String code = "C12345";
        Vector w = test.getAxioms(named_graph, code);
        for (int i=0; i<w.size(); i++) {
			Object obj = w.elementAt(i);
			printAxiomObject(obj);
		}
    }
}