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


public class HistoryUtilsExt {
	String owlfile = null;
	String diff_file = null;
    Vector edit_vec = null;
    Vector editAction_vec = null;
    HashMap code2LabelMap = null;
    OWLScanner owlScanner = null;
    HashSet codes = null;
    Vector edited_concept_codes = null;

    public HistoryUtilsExt() {
        this.owlScanner = new OWLScanner();
        edit_vec = new Vector();
    }

	public void dumpVector(String label, Vector v) {
		System.out.println(label);
		for (int i=0; i<v.size(); i++) {
			Object obj = v.elementAt(i);
			System.out.println(obj.toString());
		}
	}

	public String getPropertyType(String tabDimitedAxiomData) {
		Vector u = StringUtils.parseData(tabDimitedAxiomData);
		String property_code = (String) u.elementAt(2);
		if (property_code.compareTo("P90") == 0) {
			return "FULL_SYN";
		} else if (property_code.compareTo("P375") == 0) {
			return "Maps_To";
		} else if (property_code.compareTo("P211") == 0) {
			return "GO_Annotation";
		} else if (property_code.compareTo("P97") == 0) {
			return "DEFINITION";
		} else if (property_code.compareTo("P325") == 0) {
			return "ALT_DEFINITION";
		} else {
			return "axiom";
		}
	}

	public void dump_vector(String type, Vector v) {
		String property = null;
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			if (type.indexOf("|axiom") != -1) {
				String s = getPropertyType(t);
				Vector u = StringUtils.parseData(type, '|');
				property = (String) u.elementAt(0) + "|" + s;
			} else {
				property = type;
			}
			edit_vec.add(property + "|" + t);
		}
	}

    public void compareVector(String type, Vector vec1, Vector vec2) {
		HashSet hset1 = new HashSet();
		for (int i=0; i<vec1.size(); i++) {
			String t = (String) vec1.elementAt(i);
			hset1.add(t);
		}
		HashSet hset2 = new HashSet();
		for (int i=0; i<vec2.size(); i++) {
			String t = (String) vec2.elementAt(i);
			hset2.add(t);
		}
		Vector v1_minus_v2 = new Vector();
		for (int i=0; i<vec2.size(); i++) {
			String t = (String) vec2.elementAt(i);
			if (!hset1.contains(t)) {
				v1_minus_v2.add(t);
			}
		}
		dump_vector("added" + "|" + type, v1_minus_v2);

		Vector v2_minus_v1 = new Vector();
		for (int i=0; i<vec1.size(); i++) {
			String t = (String) vec1.elementAt(i);
			if (!hset2.contains(t)) {
				v2_minus_v1.add(t);
			}
		}
		dump_vector("deleted" + "|" + type, v2_minus_v1);
	}

	public Vector removeObjectValuedProperties(Vector v) {
		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(t, '|');
			String property = (String) u.elementAt(1);
			if (!property.startsWith("A")) {
				w.add(t);
			}
		}
		return w;
	}

    public void compareClass(Vector v1, Vector v2) {
		OWLScanner owlScanner = new OWLScanner();
        String t1 = owlScanner.get_owl_class_hashcode(v1);
        String t2 = owlScanner.get_owl_class_hashcode(v2);

        Vector w1 = removeObjectValuedProperties(owlScanner.extract_properties(v1));
        Vector w2 = removeObjectValuedProperties(owlScanner.extract_properties(v2));
        compareVector("property", w1, w2);

        w1 = owlScanner.extract_superclasses(v1);
        w2 = owlScanner.extract_superclasses(v2);
        compareVector("superclass", w1, w2);

        w1 = new Vector();
        w2 = new Vector();
        Vector w3 = owlScanner.extract_owlrestrictions(v1);
        Vector w4 = owlScanner.extract_owlrestrictions(v2);

        for (int i=0; i<w3.size(); i++) {
            OWLRestriction r = (OWLRestriction) w3.elementAt(i);
            w1.add(r.toString());
		}
        for (int i=0; i<w4.size(); i++) {
            OWLRestriction r = (OWLRestriction) w4.elementAt(i);
            w2.add(r.toString());
		}
        compareVector("role", w1, w2);

        w1 = owlScanner.extract_associations(v1);
        w2 = owlScanner.extract_associations(v2);
        compareVector("association", w1, w2);

        w3 = owlScanner.extractPropertiesWithQualifiers(v1);
        w4 = owlScanner.extractPropertiesWithQualifiers(v2);

        w3 = owlScanner.axioms2Strings(w3);
        w4 = owlScanner.axioms2Strings(w4);

        Vector w5 = set_difference(w3, w4);
        dump_vector("added|axiom", w5);

        w5 = set_difference(w4, w3);
        dump_vector("deleted|axiom", w5);
	}

	public Object delimitedString2Object(String str) {
		Vector u = StringUtils.parseData(str, '|');
		String code = (String) u.elementAt(0);
		String label = (String) u.elementAt(1);

		String property = (String) u.elementAt(2);
		if (property.compareTo("P90") == 0) {
			String termName = (String) u.elementAt(3);
			String termGroup = (String) u.elementAt(5);
			String termSource = (String) u.elementAt(7);
			String sourceCode = (String) u.elementAt(9);
			String subSourceName = (String) u.elementAt(11);
			String subSourceCode = null;
			return new Synonym(code, label, termName, termGroup, termSource, sourceCode, subSourceName, subSourceCode);

		} else if (property.compareTo("P375") == 0) {
			String preferredName = (String) u.elementAt(1);
			String relationshipToTarget = (String) u.elementAt(3);
			String targetCode = (String) u.elementAt(5);
			String targetTerm = (String) u.elementAt(7);
			String targetTermType = (String) u.elementAt(9);
			String targetTerminology = (String) u.elementAt(11);
			String targetTerminologyVersion = (String) u.elementAt(13);
			return new MapToEntry(code, preferredName, relationshipToTarget, targetCode, targetTerm, targetTermType, targetTerminology, targetTerminologyVersion);

		} else if (property.compareTo("P211") == 0) {
			String annotation = (String) u.elementAt(3);
			String goEvi = (String) u.elementAt(5);
			String goId = (String) u.elementAt(7);
			String goSource = (String) u.elementAt(9);
			String sourceDate = (String) u.elementAt(11);
			return new GoAnnotation(code, label, annotation, goEvi, goId, goSource, sourceDate);

		} else if (property.compareTo("P97") == 0) {
			String description = (String) u.elementAt(3);
			String attribution = (String) u.elementAt(5);
			String source = (String) u.elementAt(7);
			return new Definition(code, label, description, attribution, source);

		} else if (property.compareTo("P325") == 0) {
			String description = (String) u.elementAt(3);
			String attribution = (String) u.elementAt(5);
			String source = (String) u.elementAt(7);
			AltDefinition altDef =  new AltDefinition(code, label, description, attribution, source);
			return altDef;
		}
		return null;
	}


	public String indentJson(String json) {
		String t = json;
		t = t.replace("\n", "\n\t");
		return "\t" + t;
	}

    public Synonym2 transform(Synonym syn) {
		return new Synonym2(
			syn.getCode(),
			syn.getLabel(),
			"P90",
			"FULL_SYN",
			syn.getTermName(),
			syn.getTermGroup(),
			syn.getTermSource(),
			syn.getSourceCode(),
			syn.getSubSourceName(),
			syn.getSubSourceCode());
	}

    public Definition2 transform(Definition def) {
		return new Definition2(
			def.getCode(),
			def.getLabel(),
			"P97",
			"DEFINITION",
			def.getDescription(),
			def.getAttribution(),
			def.getSource());
	}

    public AltDefinition2 transform(AltDefinition def) {
		return new AltDefinition2(
			def.getCode(),
			def.getLabel(),
			"P325",
			"ALT_DEFINITION",
			def.getDescription(),
			def.getAttribution(),
			def.getSource());
	}

    public GoAnnotation2 transform(GoAnnotation go) {
		return new GoAnnotation2(
			go.getCode(),
			go.getLabel(),
			"P211",
			"GO_Annotation",
			go.getAnnotation(),
			go.getGoEvi(),
			go.getGoId(),
			go.getGoSource(),
			go.getSourceDate());
	}

    public MapToEntry2 transform(MapToEntry entry) {
		return new MapToEntry2(
			entry.getCode(),
			entry.getPreferredName(),
			"P375",
			"Maps_To",
			entry.getRelationshipToTarget(),
			entry.getTargetCode(),
			entry.getTargetTerm(),
			entry.getTargetTermType(),
			entry.getTargetTerminology(),
			entry.getTargetTerminologyVersion());
	}

	public boolean isComplexProperty(String prop_code) {
		if (prop_code.compareTo("P90") == 0 ||
		    prop_code.compareTo("P375") == 0 ||
		    prop_code.compareTo("P211") == 0 ||
		    prop_code.compareTo("P97") == 0 ||
		    prop_code.compareTo("P325") == 0) return true;
		return false;
	}


	public Vector get_edit_vec(Vector v1) {
		try {
			edit_vec = new Vector();
			//Vector v1 = owlScanner.getOWLClassDataByCode(code);
			//System.out.println("new code: " + code + " " + v1.size());
			Vector w1 = removeObjectValuedProperties(owlScanner.extract_properties(v1));

			Vector w2 = new Vector();
			compareVector("property", w2, w1);

			w1 = owlScanner.extract_superclasses(v1);
			w2 = new Vector();
			compareVector("superclass", w2, w1);

			w1 = new Vector();
			w2 = new Vector();
			Vector w3 = owlScanner.extract_owlrestrictions(v1);
			w2 = new Vector();

			for (int i=0; i<w3.size(); i++) {
				OWLRestriction r = (OWLRestriction) w3.elementAt(i);
				w1.add(r.toString());
			}
			compareVector("role",  w2, w1);

			w1 = owlScanner.extract_associations(v1);
			w2 = new Vector();
			compareVector("association",  w2, w1);

			w3 = owlScanner.extractPropertiesWithQualifiers(v1);
			Vector w4 = new Vector();

			w3 = owlScanner.axioms2Strings(w3);
			w4 = new Vector();

			Vector w5 = set_difference(w4, w3);

			dump_vector("added|axiom", w5);

	    } catch (Exception ex) {
			ex.printStackTrace();
		}

		return edit_vec;
	}

    public Vector set_difference(Vector vec1, Vector vec2) {
		HashSet hset1 = new HashSet();
		for (int i=0; i<vec1.size(); i++) {
			String t = (String) vec1.elementAt(i);
			hset1.add(t);
		}
		HashSet hset2 = new HashSet();
		for (int i=0; i<vec2.size(); i++) {
			String t = (String) vec2.elementAt(i);
			hset2.add(t);
		}
		Vector v1_minus_v2 = new Vector();
		for (int i=0; i<vec2.size(); i++) {
			String t = (String) vec2.elementAt(i);
			if (!hset1.contains(t)) {
				v1_minus_v2.add(t);
			}
		}
		return v1_minus_v2;
	}


//added|property|C40074|rdfs:label|25-Hydroxyvitamin D-1 Alpha-Hydroxylase, Mitochondrial
    public String find_rdfs_label(Vector edit_vec) {
		for (int i=0; i<edit_vec.size(); i++) {
			String t = (String) edit_vec.elementAt(i);
			if (t.indexOf("|rdfs:label|") != -1) {
				Vector u = StringUtils.parseData(t, '|');
			    return (String) u.elementAt(4);
			}
		}
		return null;
	}

    public Vector construct_editaction_vec(Vector edit_vec) {
		String label = find_rdfs_label(edit_vec);
		Vector editAction_vec = new Vector();
		for (int i=0; i<edit_vec.size(); i++) {
			String t = (String) edit_vec.elementAt(i);
			Vector u = StringUtils.parseData(t, '|');
	        String action = (String) u.elementAt(0);
	        String type = (String) u.elementAt(1);
	        String code = (String) u.elementAt(2);
	        String value = t.substring(action.length()+1+type.length()+1, t.length());
	        EditAction editAction = new EditAction(label, code, action, type, value);
	        editAction_vec.add(editAction);
		}
		return editAction_vec;
	}


	public static void main(String[] args) {
        long ms = System.currentTimeMillis();
        String owlfile = args[0];
        HistoryUtilsExt hist = new HistoryUtilsExt();

		Vector v = Utils.readFile(owlfile);
		Vector edit_vec = hist.get_edit_vec(v);
		Utils.saveToFile("edit_vec_" + owlfile, edit_vec);

        Vector editaction_vec = hist.construct_editaction_vec(edit_vec);
        Vector w = new Vector();
		for (int i=0; i<editaction_vec.size(); i++) {
			EditAction t = (EditAction) editaction_vec.elementAt(i);
			w.add(t.toJson());
		}

        Utils.saveToFile("editaction_" + owlfile, w);
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}
}





















































