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


public class ParserUtils {
	public ParserUtils() {

	}

    public Vector getValues(Vector v, int k) {
		if (v == null) return null;
		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String value = (String) u.elementAt(k);
			w.add(value);
		}
		return w;
	}

    public String getValue(String t) {
		if (t == null) return null;
		Vector v = StringUtils.parseData(t, '|');
		if (v.size() == 0) return null;
		return (String) v.elementAt(v.size()-1);
	}

	public String parseLabel(Vector v) {
		if (v == null) return null;
		String t = (String) v.elementAt(0);
		return getValue(t);
	}

	public HashMap parseSuperclasses(Vector v) {
		if (v == null) return null;
		return parse(v, 2, 1, 0);
	}

	public HashMap parseSubclasses(Vector v) {
		if (v == null) return null;
		return parse(v, 2, 1, 0);
	}

	public HashMap parseProperties(Vector v) {
		if (v == null) return null;
		return parse(v, 2, 0, 1);
	}

	public HashMap parse(Vector v, int m, int d1, int d2) {
		if (v == null) return null;
		HashMap hmap = new HashMap();
		if (v == null) return hmap;
		if (m == 0) return hmap;
		int n = v.size()/m;
		for (int i=0; i<n; i++) {
			int i0 = i*m;
			int i1 = i0+d1;
			int i2 = i0+d2;
			String t1 = (String) v.elementAt(i1);
			String t2 = (String) v.elementAt(i2);
			String key = getValue(t1);
			String value = getValue(t2);
			Vector u = new Vector();
			if (hmap.containsKey(key)) {
				u = (Vector) hmap.get(key);
			}
			if (!u.contains(value)) {
				u.add(value);
				u = new SortUtils().quickSort(u);
			}
			hmap.put(key, u);
		}
        return hmap;
	}

	public int findNumberOfVariables(Vector v) {
		if (v == null) return 0;
		HashSet hset = new HashSet();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String var = (String) u.elementAt(0);
			if (!hset.contains(var)) {
				hset.add(var);
			}
		}
		return hset.size();
	}


	public Vector parse(Vector v) {
		if (v == null) return null;
		int m = findNumberOfVariables(v);
		return parse(v, m);
	}


	public Vector parse(Vector v, int m) {
		if (v == null) return null;
		Vector w = new Vector();
		if (w == null) return w;
		int n = v.size()/m;
		for (int i=0; i<n; i++) {
			StringBuffer buf = new StringBuffer();
			for (int j=0; j<m; j++) {
				int i0 = i*m+j;
				String t = (String) v.elementAt(i0);
				t = getValue(t);
				buf.append(t);
				if (j < m-1) {
					buf.append("|");
				}
			}
			String s = buf.toString();
			w.add(s);
		}
		return w;
	}

    public Vector sortAxiomData(Vector v) {
	    if (v == null) return null;
		Vector w = new Vector();
		int n = v.size()/6;
		Vector key_vec = new Vector();
		for (int i=0; i<n; i++) {
			String t = (String) v.elementAt(i*6);
			if (!key_vec.contains(t)) {
				key_vec.add(t);
			}
		}

		key_vec = new SortUtils().quickSort(key_vec);
		for (int i=0; i<key_vec.size(); i++) {
		    String key = (String) key_vec.elementAt(i);
		    for (int j=0; j<v.size(); j++) {
				String value = (String) v.elementAt(j);
				if (value.compareTo(key) == 0) {
					w.add(value);
					w.add((String) v.elementAt(j+1));
					w.add((String) v.elementAt(j+2));
					w.add((String) v.elementAt(j+3));
					w.add((String) v.elementAt(j+4));
					w.add((String) v.elementAt(j+5));
				}
			}
		}
		return w;
	}


/*
        (1) bnode_3cb4419c_bd44_4b0d_af55_94fcc08da3b2_492050|Cell Aging|C16394|FULL_SYN|Aging, Cellular|Term Type|SY
        (2) bnode_3cb4419c_bd44_4b0d_af55_94fcc08da3b2_492050|Cell Aging|C16394|FULL_SYN|Aging, Cellular|Term Source|NCI
        (3) bnode_3cb4419c_bd44_4b0d_af55_94fcc08da3b2_492051|Cell Aging|C16394|FULL_SYN|Cell Aging|Term Type|PT
        (4) bnode_3cb4419c_bd44_4b0d_af55_94fcc08da3b2_492051|Cell Aging|C16394|FULL_SYN|Cell Aging|Term Source|NCI
        (5) bnode_3cb4419c_bd44_4b0d_af55_94fcc08da3b2_492052|Cell Aging|C16394|FULL_SYN|Cell Senescence|Term Type|SY
        (6) bnode_3cb4419c_bd44_4b0d_af55_94fcc08da3b2_492052|Cell Aging|C16394|FULL_SYN|Cell Senescence|Term Source|NCI
        (7) bnode_3cb4419c_bd44_4b0d_af55_94fcc08da3b2_492053|Cell Aging|C16394|FULL_SYN|Cellular Aging|Term Type|SY
        (8) bnode_3cb4419c_bd44_4b0d_af55_94fcc08da3b2_492053|Cell Aging|C16394|FULL_SYN|Cellular Aging|Term Source|NCI
        (9) bnode_3cb4419c_bd44_4b0d_af55_94fcc08da3b2_492054|Cell Aging|C16394|FULL_SYN|Cellular Senescence|Term Type|SY
        (10) bnode_3cb4419c_bd44_4b0d_af55_94fcc08da3b2_492054|Cell Aging|C16394|FULL_SYN|Cellular Senescence|Term Source|NCI
*/

    public Vector parseSynonymData(Vector axiom_data) {
		HashMap hmap = new HashMap();
		for (int i=0; i<axiom_data.size(); i++) {
			String t = (String) axiom_data.elementAt(i);
			Vector u = StringUtils.parseData(t, '|');
			String axiom_id = (String) u.elementAt(0);
			String label = (String) u.elementAt(1);
			String code = (String) u.elementAt(2);
			String propertyName = (String) u.elementAt(3);
			String term_name = (String) u.elementAt(4);
			String qualifier_name = (String) u.elementAt(5);
			String qualifier_value = (String) u.elementAt(6);
            Synonym syn = (Synonym) hmap.get(axiom_id);
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
			if (qualifier_name.compareTo("Term Type") == 0) {
				syn.setTermGroup(qualifier_value);
			} else if (qualifier_name.compareTo("Term Source") == 0) {
				syn.setTermSource(qualifier_value);
			} else if (qualifier_name.compareTo("Source Code") == 0) {
				syn.setSourceCode(qualifier_value);
			} else if (qualifier_name.compareTo("Subsource Name") == 0) {
				syn.setSubSourceName(qualifier_value);
			} else if (qualifier_name.compareTo("Subsource Code") == 0) {
				syn.setSubSourceCode(qualifier_value);
			} else if (qualifier_name.compareTo("Subsource Name") == 0) {
				syn.setSubSourceName(qualifier_value);
			}
			hmap.put(axiom_id, syn);
		}
		Vector w2 = new Vector();
		Iterator it = hmap.keySet().iterator();
		while (it.hasNext()) {
			String axiom_id = (String) it.next();
			Synonym syn = (Synonym) hmap.get(axiom_id);
			w2.add(syn);
		}
		return w2;
	}

	public List getSynonyms(Vector v) {
		if (v == null) return null;
		List syn_list = new ArrayList();
		Vector w = parseSynonymData(v);
		for (int i=0; i<v.size(); i++) {
			Synonym syn = (Synonym) v.elementAt(i);
			syn_list.add(syn);
		}
		return syn_list;
	}

/*
	public List getSynonyms(Vector v) {
		if (v == null) return null;
	    //v = sortAxiomData(v);
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
*/

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

	public Vector generateRelationships(Vector v, String type, String name) {
		if (v == null) return null;
        ParserUtils parser = new ParserUtils();
        Vector w = new Vector();
        int n = v.size()/4;
		String sourceCode = null;
		String sourceLabel = null;
		String targetCode = null;
		String targetLabel = null;

        for (int i=0; i<n; i++) {
			int k = i*4;
			sourceCode = parser.getValue((String) v.elementAt(k+1));
			sourceLabel = parser.getValue((String) v.elementAt(k));
			targetCode = parser.getValue((String) v.elementAt(k+3));
			targetLabel = parser.getValue((String) v.elementAt(k+2));
	        Relationship r = new Relationship(
				type,
				name,
				sourceCode,
				sourceLabel,
				targetCode,
				targetLabel);
			w.add(r);
		}
		return w;
	}

	public HashMap getCode2LabelHashMap(Vector v) {
		if (v == null) return null;
		HashMap hmap = new HashMap();
		int n = v.size()/2;
		for (int i=0; i<n; i++) {
			String label = getValue((String) v.elementAt(i*2));
			String code = getValue((String) v.elementAt(i*2+1));
			hmap.put(code, label);
		}
		return hmap;
	}

	public Vector toDelimited(Vector v, int m, char delim) {
		if (v == null) return null;
        Vector w = new Vector();
        int n = v.size()/m;
        for (int i=0; i<n; i++) {
			StringBuffer buf = new StringBuffer();
			for (int k=0; k<m; k++) {
				int j = i*m+k;
				buf.append(getValue((String) v.elementAt(j)));
				if (k<m-1) {
					buf.append(delim);
				}
			}
			String line = buf.toString();
			w.add(line);
		}
		return w;
	}

/*
	public List getAdditionalProperties(Vector v) {
		List additionalProperties = new ArrayList();
		List commonProperties = Arrays.asList(Constants.COMMON_PROPERTIES);
		if (v == null) return null;
		int n = v.size()/2;
		for (int i=0; i<n; i++) {
			String y_label = getValue((String) v.elementAt(i*2));
			String z_label = getValue((String) v.elementAt(i*2+1));
			String z = getValue(z_label);
			if (!commonProperties.contains(y_label)) {
				Property property = new Property(y_label, z);
				additionalProperties.add(property);
			}
		}
		return additionalProperties;
	}
*/
	public List getAdditionalProperties(Vector v) {
		if (v == null) return null;
		List additionalProperties = new ArrayList();
		List commonProperties = Arrays.asList(Constants.COMMON_PROPERTIES);
		//v = getResponseValues(v);
		if (v == null) return null;
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(t, '|');
			String prop_name = (String) u.elementAt(1);
			if (!commonProperties.contains(prop_name)) {
				String prop_value = (String) u.elementAt(2);
				Property property = new Property(prop_name, prop_value);
				additionalProperties.add(property);
			}
		}
		return additionalProperties;
	}

	/*
	public Path(
		int direction,
		List concepts) {
		this.direction = direction;
		this.concepts = concepts;
	}
	*/


	public Paths trimPaths(Paths paths, String code) {
		if (paths == null) return null;
		Paths new_paths = new Paths();
		List path_list = paths.getPaths();
		for (int i=0; i<path_list.size(); i++) {
			Path path = (Path) path_list.get(i);
			int direction = path.getDirection();
			List concepts = path.getConcepts();

			int idx = -1;
			for (int j=0; j<concepts.size(); j++) {
				Concept concept = (Concept) concepts.get(j);
				if (concept.getCode().compareTo(code) == 0) {
					idx = concept.getIdx();
				}
			}
			List list = new ArrayList();
			if (idx == -1) {
				idx = concepts.size()-1;
			}
			for (int k=0; k<=idx; k++) {
				Concept c = (Concept) concepts.get(k);
				list.add(c);
			}
			new_paths.add(new Path(direction, list));
		}
		return new_paths;
	}

	public Vector getResponseVariables(Vector v) {
		if (v == null) return null;
		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String var = (String) u.elementAt(0);
			if (!w.contains(var)) {
				w.add(var);
			}
		}
		return w;
	}

	public String getVariableName(String line) {
		Vector u = StringUtils.parseData(line, '|');
		return (String) u.elementAt(0);
	}


	public Vector getResponseValues(Vector v) {
		if (v == null || v.size() == 0) return null;
		ParserUtils parser = new ParserUtils();
		Vector w = new Vector();
		Vector vars = getResponseVariables(v);
		String firstVar = (String) vars.elementAt(0);
		String[] values = new String[vars.size()];
		for (int i=0; i<vars.size(); i++) {
			values[i] = null;
		}
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			String var = getVariableName(line);
			if (var.compareTo(firstVar) == 0 && values[0] != null) {
				StringBuffer buf = new StringBuffer();
				for (int j=0; j<vars.size(); j++) {
					String t = values[j];
					if (t == null) {
						t = "null";
					}
					buf.append(t);
					if (j < vars.size()-1) {
						buf.append("|");
					}
				}
				String s = buf.toString();
				w.add(s);

				for (int k=0; k<vars.size(); k++) {
					values[k] = null;
			    }
		    }
		    String value = parser.getValue(line);
			for (int k=0; k<vars.size(); k++) {
				if (var.compareTo((String) vars.elementAt(k)) == 0) {
					values[k] = value;
				}
			}

		}
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<vars.size(); i++) {
			String t = values[i];
			if (t == null) {
				t = "null";
			}
			buf.append(t);
			if (i < vars.size()-1) {
				buf.append("|");
			}
		}
		String s = buf.toString();
		w.add(s);
		return w;
	}



	public Vector filterPropertyQualifiers(Vector v, String type) { //FULL_SYN, DEFINITION, ALT_DEFINITION
	    if (v == null) return null;
		Vector w = new Vector();
		int n = v.size()/9;
		Vector key_vec = new Vector();
		for (int i=0; i<n; i++) {
			String t0 = (String) v.elementAt(i*9);
			String t3 = (String) v.elementAt(i*9+3);
			t3 = getValue(t3);
			if (t3.compareTo(type) == 0) {
				if (!key_vec.contains(t0)) {
					key_vec.add(t0);
				}
			}
		}

		key_vec = new SortUtils().quickSort(key_vec);
		for (int i=0; i<key_vec.size(); i++) {
		    String key = (String) key_vec.elementAt(i);
		    for (int j=0; j<v.size(); j++) {
				String value = (String) v.elementAt(j);
				if (value.compareTo(key) == 0) {
					w.add(value);
					w.add((String) v.elementAt(j+1));
					w.add((String) v.elementAt(j+2));
					w.add((String) v.elementAt(j+5));
					w.add((String) v.elementAt(j+6));
					w.add((String) v.elementAt(j+8));
				}
			}
		}
		return w;
	}

    public String extractLabel(String line) {
		if (line == null) return null;
		int n = line.lastIndexOf("#");
		if (n == -1) return line;
		return line.substring(n+1, line.length());
	}



	public static Vector formatOutput(Vector v) {
		if (v == null) return null;
		if (v.size() == 0) return new Vector();
		v = new ParserUtils().getResponseValues(v);
		v = new SortUtils().quickSort(v);
		return v;
	}

//       (10) Wireless Communication Problem|http://www.w3.org/2000/01/rdf-schema#label|label|Wireless Communication Problem

	public static Vector excludePropertyType(Vector v, String exclusion) {
		if (v == null) return null;
		Vector target_vec = StringUtils.parseData(exclusion, '|');
		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(t, '|');
			String x = (String) u.elementAt(1);
			boolean retval = true;
			for (int k=0; k<target_vec.size(); k++) {
				String target = (String) target_vec.elementAt(k);
				if (x.contains(target)) {
					retval = false;
					break;
				}
			}
			if (retval) {
				String s0 =  (String) u.elementAt(0);
				String s2 =  (String) u.elementAt(2);
				String s3 =  (String) u.elementAt(3);
				String s = s0 + "|" + s2 + "|" + s3;
				w.add(s);
			}
		}
		return w;
	}

	public static Vector axioms2ComplexProperties(Vector v) {
		HashMap hmap = new HashMap();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String name = (String) u.elementAt(2);
			String value = (String) u.elementAt(3);
			String n_v = name + "|" + value;
			Vector qualifiers = new Vector();
			if (hmap.containsKey(n_v)) {
				qualifiers = (Vector) hmap.get(n_v);
			}
			String qualifier_name = (String) u.elementAt(4);
			String qualifier_value = (String) u.elementAt(5);
			String q_n_v = qualifier_name + "|" + qualifier_value;
			if (!qualifiers.contains(q_n_v)) {
				qualifiers.add(q_n_v);
			}
			hmap.put(n_v, qualifiers);
		}
		Iterator it = hmap.keySet().iterator();
		Vector keys = new Vector();
		while (it.hasNext()) {
			String key = (String) it.next();
			keys.add(key);
		}
		keys = new SortUtils().quickSort(keys);
		Vector complex_properties = new Vector();
		for (int i=0; i<keys.size(); i++) {
			String key = (String) keys.elementAt(i);
			Vector u = StringUtils.parseData(key, '|');
			String name = (String) u.elementAt(0);
			String value = (String) u.elementAt(1);
			List list = new ArrayList();
			Vector q_n_v_vec = (Vector) hmap.get(key);
			for (int j=0; j<q_n_v_vec.size(); j++) {
				String q_n_v = (String) q_n_v_vec.elementAt(j);
				Vector u2 = StringUtils.parseData(q_n_v, '|');
				String q_name = (String) u2.elementAt(0);
				String q_value = (String) u2.elementAt(1);
				list.add(new PropertyQualifier(q_name, q_value));
			}
			ComplexProperty cp = new ComplexProperty(name, value, list);
			complex_properties.add(cp);
		}
		return complex_properties;
	}

	public static void main(String[] args) {
		String filename = args[0];
        filename = "filterPropertyQualifiers.txt";
		Vector w = Utils.readFile(filename);
        Vector v = new ParserUtils().filterPropertyQualifiers(w, "FULL_SYN");
	}

}
