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
//import org.apache.commons.codec.binary.Base64;
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
    String username = null;
    String password = null;

    public AxiomUtils() {

	}

    public AxiomUtils(String serviceUrl) {
        this.serviceUrl = serviceUrl;
        this.owlSPARQLUtils = new OWLSPARQLUtils(this.serviceUrl, null, null);
    }

    public AxiomUtils(String serviceUrl, String username, String password) {
        this.serviceUrl = serviceUrl;
        this.username = username;
        this.password = password;
        this.owlSPARQLUtils = new OWLSPARQLUtils(this.serviceUrl, username, password);
    }

    public Vector getAxioms(String named_graph, String code, String propertyName) {
		return owlSPARQLUtils.getAxioms(named_graph, code, propertyName);
	}

	public List getSynonyms(String named_graph, String code, String propertyName) {
		Vector v = getAxioms(named_graph, code, propertyName);
		return getSynonyms(v);
	}

    public Vector getAxioms(String named_graph, String code, String propertyName, String qualifierName) {
		Vector w = owlSPARQLUtils.getAxioms(named_graph, code, propertyName, qualifierName);
		return w;
	}

	public List getSynonyms(String named_graph, String code, String propertyName, String qualifierName) {
		Vector v = getAxioms(named_graph, code, propertyName, qualifierName);
		return getSynonyms(v);
	}

    public List getSynonyms(Vector axiom_data) {
		if (axiom_data == null) return null;
		HashMap hmap = new HashMap();
		for (int i=0; i<axiom_data.size(); i++) {
			String t = (String) axiom_data.elementAt(i);
			Vector u = StringUtils.parseData(t, '|');
			String axiom_id = (String) u.elementAt(0); //bnode_21e4bb1f_2fc9_480b_bbdb_0d116398d610_2156686
			String label = (String) u.elementAt(1);
			String code = (String) u.elementAt(2);
			String propertyName = (String) u.elementAt(3); // FULL_SYN
			//String propertyCode = (String) u.elementAt(4); // P90
			String term_name = (String) u.elementAt(4); //P90
			String qualifier_name = (String) u.elementAt(5);
			//String qualifier_code = (String) u.elementAt(7);
			String qualifier_value = (String) u.elementAt(6);

			String key = axiom_id + "$" + code;

            Synonym syn = (Synonym) hmap.get(key);
            if (syn == null) {
				syn = new Synonym(
							code,
							label,
							term_name,
							null, //termGroup,
							null, //termSource,
							null, //sourceCode,
							null, //subSourceName,
		                    null); //subSourceCode
			}
			if (qualifier_name.compareTo("Term Type") == 0 ||
			           qualifier_name.compareTo("tem-type") == 0 ||
			           qualifier_name.compareTo("P383") == 0) {
				syn.setTermGroup(qualifier_value);
			} else if (qualifier_name.compareTo("Term Source") == 0 ||
			           qualifier_name.compareTo("tem-source") == 0 ||
			           qualifier_name.compareTo("P384") == 0) {
				syn.setTermSource(qualifier_value);
			} else if (qualifier_name.compareTo("Source Code") == 0 ||
			           qualifier_name.compareTo("source-code") == 0 ||
			           qualifier_name.compareTo("P385") == 0) {
				syn.setSourceCode(qualifier_value);
			} else if (qualifier_name.compareTo("Subsource Name") == 0 ||
			           qualifier_name.compareTo("subsource-name") == 0 ||
			           qualifier_name.compareTo("P386") == 0) {
				syn.setSubSourceName(qualifier_value);
			} else if (qualifier_name.compareTo("Subsource Code") == 0 ||
			           qualifier_name.compareTo("subsource-name") == 0) {
				syn.setSubSourceCode(qualifier_value);
			}
			hmap.put(key, syn);
		}
		List syn_list = new ArrayList();
		Iterator it = hmap.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			Synonym syn = (Synonym) hmap.get(key);
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
		String attribution = null;
		String qualifier_name = null;
		String qualifier_value = null;
		String propertyName = null;

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

/*
	(1) bnode_d28617db_50c1_4332_a067_92ecdab7e192_2912161|Sex|C28421|ALT_DEFINITION|The biologic character or quality that distinguishes male and female from one another as expressed by analysis of the person's gonadal, morphologic (internal and external), chromosomal, and hormonal characteristics.|Definition Source|PCDC
	(2) bnode_d28617db_50c1_4332_a067_92ecdab7e192_2912160|Sex|C28421|ALT_DEFINITION|Phenotypic expression of chromosomal makeup that defines a study subject as male, female, or other. Compare to gender.|Definition Source|CDISC-GLOSS
	(3) bnode_d28617db_50c1_4332_a067_92ecdab7e192_2912161|Sex|C28421|ALT_DEFINITION|The biologic character or quality that distinguishes male and female from one another as expressed by analysis of the person's gonadal, morphologic (internal and external), chromosomal, and hormonal characteristics.|attribution|AML
*/
		Iterator it = hmap.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			Vector values = (Vector) hmap.get(key);
			code = null;
			label = null;
			propertyName = null;
			desc = null;
			defSource = null;
			attribution = null;
			qualifier_name = null;
			qualifier_value = null;

			for (int i=0; i<values.size(); i++) {
				String line = (String) values.elementAt(i);
				Vector u = StringUtils.parseData(line, '|');
				label = (String) u.elementAt(1);
				code = (String) u.elementAt(2);
				propertyName = (String) u.elementAt(3);
				desc = (String) u.elementAt(4);
				qualifier_name = (String) u.elementAt(5);
				qualifier_value = (String) u.elementAt(6);
                if (qualifier_name.compareTo("P378") == 0 || qualifier_name.compareTo("def-source") == 0 || qualifier_name.compareTo("Definition Source") == 0) {
					defSource = qualifier_value;
                } else if (qualifier_name.compareTo("P381") == 0 || qualifier_name.compareTo("attribution") == 0 || qualifier_name.compareTo("attr") == 0) {
					attribution = qualifier_value;
				}

			}
			if (propertyName.compareTo("DEFINITION") == 0 || propertyName.compareTo("P97") == 0) {
				Definition def = new Definition(
					code,
					label,
					desc,
					attribution,
					defSource);
				def_list.add(def);
			} else if (propertyName.compareTo("ALT_DEFINITION") == 0 || propertyName.compareTo("P325") == 0) {
				AltDefinition def = new AltDefinition(
					code,
					label,
					desc,
					attribution,
					defSource);
				def_list.add(def);
			}
		}
		return def_list;
	}

	public List getDefinitions(String named_graph, String code, String propertyName) {
		Vector v = getAxioms(named_graph, code, propertyName);
		return getDefinitions(v);
	}


	public List getMapsTos(String named_graph, String code, String propertyName) {
		Vector v = getAxioms(named_graph, code, propertyName);
		return getMapsTos(v);
	}

    public List getMapsTos(Vector axiom_data) {
		HashMap hmap = new HashMap();
		for (int i=0; i<axiom_data.size(); i++) {
			String t = (String) axiom_data.elementAt(i);
			Vector u = StringUtils.parseData(t, '|');
			String axiom_id = (String) u.elementAt(0);
			String label = (String) u.elementAt(1);
			String code = (String) u.elementAt(2);
			String propertyName = (String) u.elementAt(3);

//bnode_301c03a7_663e_49c8_be4e_8726b4fc92ea_380278|Hepatic Infection, CTCAE|C143541|Maps_To|P375|Hepatic infection|Target_Code|P395|10056522
/*
	(1) bnode_a5affcae_8273_49eb_aa9d_d77fe34e555a_205001|Ciliary Body|C12345|Maps_To|Ciliary body|Relationship_to_Target|Has Synonym
	(2) bnode_a5affcae_8273_49eb_aa9d_d77fe34e555a_204999|Ciliary Body|C12345|Maps_To|Ciliary body|Relationship_to_Target|Has Synonym
	(3) bnode_a5affcae_8273_49eb_aa9d_d77fe34e555a_205000|Ciliary Body|C12345|Maps_To|Ciliary body|Relationship_to_Target|Has Synonym
	(4) bnode_a5affcae_8273_49eb_aa9d_d77fe34e555a_204999|Ciliary Body|C12345|Maps_To|Ciliary body|Target_Term_Type|PT
	(5) bnode_a5affcae_8273_49eb_aa9d_d77fe34e555a_205001|Ciliary Body|C12345|Maps_To|Ciliary body|Target_Term_Type|PT
	(6) bnode_a5affcae_8273_49eb_aa9d_d77fe34e555a_205000|Ciliary Body|C12345|Maps_To|Ciliary body|Target_Term_Type|PT
	(7) bnode_a5affcae_8273_49eb_aa9d_d77fe34e555a_204999|Ciliary Body|C12345|Maps_To|Ciliary body|Target_Code|PRAS
	(8) bnode_a5affcae_8273_49eb_aa9d_d77fe34e555a_205001|Ciliary Body|C12345|Maps_To|Ciliary body|Target_Code|TOO
	(9) bnode_a5affcae_8273_49eb_aa9d_d77fe34e555a_205000|Ciliary Body|C12345|Maps_To|Ciliary body|Target_Code|SRB
	(10) bnode_a5affcae_8273_49eb_aa9d_d77fe34e555a_205000|Ciliary Body|C12345|Maps_To|Ciliary body|Target_Terminology|GDC
	(11) bnode_a5affcae_8273_49eb_aa9d_d77fe34e555a_205001|Ciliary Body|C12345|Maps_To|Ciliary body|Target_Terminology|GDC
	(12) bnode_a5affcae_8273_49eb_aa9d_d77fe34e555a_204999|Ciliary Body|C12345|Maps_To|Ciliary body|Target_Terminology|GDC
*/

			//String propertyCode = (String) u.elementAt(4);
			String targetName = (String) u.elementAt(4);
			String qualifier_name = (String) u.elementAt(5);
			//String qualifier_code = (String) u.elementAt(7);
			String qualifier_value = (String) u.elementAt(6);
            MapToEntry entry = (MapToEntry) hmap.get(axiom_id);
            if (entry == null) {
				entry = new MapToEntry(
							code,
							label,
							null,
							null,
							targetName,
							null,
							null,
							null);
			}

			if (qualifier_name.compareTo("Relationship_to_Target") == 0) {
				entry.setRelationshipToTarget(qualifier_value);
			} else if (qualifier_name.compareTo("Target_Code") == 0) {
				entry.setTargetCode(qualifier_value);
			} else if (qualifier_name.compareTo("Target_Term_Type") == 0) {
				entry.setTargetTermType(qualifier_value);
			} else if (qualifier_name.compareTo("Target_Terminology") == 0) {
				entry.setTargetTerminology(qualifier_value);
			} else if (qualifier_name.compareTo("Target_Terminology_Version") == 0) {
				entry.setTargetTerminologyVersion(qualifier_value);
			}
			hmap.put(axiom_id, entry);
		}
		Vector w2 = new Vector();
		Iterator it = hmap.keySet().iterator();
		while (it.hasNext()) {
			String axiom_id = (String) it.next();
			MapToEntry entry = (MapToEntry) hmap.get(axiom_id);
			w2.add(entry);
		}
		return w2;
	}

	public List getGoAnnotations(String named_graph, String code, String propertyName) {
		Vector v = getAxioms(named_graph, code, propertyName);
		return getGoAnnotations(v);
	}

    public List getGoAnnotations(Vector axiom_data) {
		HashMap hmap = new HashMap();
		for (int i=0; i<axiom_data.size(); i++) {
			String t = (String) axiom_data.elementAt(i);
			Vector u = StringUtils.parseData(t, '|');
			String axiom_id = (String) u.elementAt(0);
			String label = (String) u.elementAt(1);
			String code = (String) u.elementAt(2);
			String propertyName = (String) u.elementAt(3);
			String targetName = (String) u.elementAt(4);
			String qualifier_name = (String) u.elementAt(5);
			String qualifier_value = (String) u.elementAt(6);

	// (1) bnode_a5affcae_8273_49eb_aa9d_d77fe34e555a_637314|DRD2 1 Allele|C19799|GO_Annotation|integral to plasma membrane|go-id|GO:0005887

			GoAnnotation go = (GoAnnotation) hmap.get(axiom_id);
			if (go == null) {
					go = new GoAnnotation(
					 code,
					 label,
					 targetName,
					 null, //goEvi,
					 null, //goId,
					 null, //goSource,
					 null); //sourceDate);
			}

			if (qualifier_name.compareTo("go-id") == 0) {
				go.setGoId(qualifier_value);
			} else if (qualifier_name.compareTo("go-evi") == 0) {
				go.setGoEvi(qualifier_value);
			} else if (qualifier_name.compareTo("go-source") == 0) {
				go.setGoSource(qualifier_value);
			} else if (qualifier_name.compareTo("source-date") == 0) {
				go.setSourceDate(qualifier_value);
			}
			hmap.put(axiom_id, go);
		}
		Vector w2 = new Vector();
		Iterator it = hmap.keySet().iterator();
		while (it.hasNext()) {
			String axiom_id = (String) it.next();
			GoAnnotation go = (GoAnnotation) hmap.get(axiom_id);
			w2.add(go);
		}
		return w2;
	}


	public Vector<Synonym> getSynonymWithQualifierMatching(String named_graph, String qualifierName, String qualifierValue) {
		Vector syn_vec = new Vector();
		String propertyName = "FULL_SYN";
		List list = getSynonyms(named_graph, null, propertyName, qualifierName);
		Vector codes = new Vector();
		for (int i=0; i<list.size(); i++) {
			Synonym syn = (Synonym) list.get(i);
			if (syn.getSubSourceName().compareTo(qualifierValue) == 0) {
		        if (!codes.contains(syn.getCode())) {
					codes.add(syn.getCode());
				}
			}
		}
		int lcv = 0;
		for (int i=0; i<codes.size(); i++) {
			lcv++;
			String code = (String) codes.elementAt(i);
			List syns = getSynonyms(named_graph, code, propertyName);
		    for (int j=0; j<syns.size(); j++) {
				Synonym syn = (Synonym) syns.get(j);
				if (qualifierName.compareTo("Subsource Name") == 0) {
				    if (syn.getSubSourceName() != null && syn.getSubSourceName().compareTo(qualifierValue) == 0) {
						syn_vec.add(syn);
					}
				} else if (qualifierName.compareTo("Term Source") == 0) {
				    if (syn.getTermSource() != null && syn.getTermSource().compareTo(qualifierValue) == 0) {
						syn_vec.add(syn);
					}
				} else if (qualifierName.compareTo("Term Type") == 0) {
				    if (syn.getTermGroup() != null && syn.getTermGroup().compareTo(qualifierValue) == 0) {
						syn_vec.add(syn);
					}
				}
			}

			if (lcv == 500) {
				System.out.println("" + i + " out of " + codes.size() + " completed.");
				lcv = 0;
			}
		}
		return syn_vec;
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


