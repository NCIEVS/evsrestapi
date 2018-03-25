package gov.nih.nci.evs.restapi.util;


import gov.nih.nci.evs.restapi.bean.*;
import java.io.*;
import java.util.*;
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


public class JSONUtils {
	public JSONUtils() {

	}

    public static Vector<String> parseData(String line) {
		if (line == null) return null;
        String tab = "|";
        return parseData(line, tab);
    }

    public static Vector<String> parseData(String line, String tab) {
		if (line == null) return null;
        Vector data_vec = new Vector();
        StringTokenizer st = new StringTokenizer(line, tab);
        while (st.hasMoreTokens()) {
            String value = st.nextToken();
            if (value.compareTo("null") == 0)
                value = "";
            data_vec.add(value);
        }
        return data_vec;
    }

	public Vector findVars(String json) {
		String vars = null;
		Vector v = null;
		Vector u = new Vector();
		try {
			JSONObject obj = new JSONObject(json);
			vars = obj.getJSONObject("head").getString("vars");
			if (vars.startsWith("[")) {
				vars = vars.substring(1, vars.length()-1);
			}
			v = parseData(vars, ",");
			for (int i=0; i<v.size(); i++) {
				String s = (String) v.elementAt(i);
				if (s.startsWith("\"")) {
					s = s.substring(1, s.length()-1);
				}
				u.add(s);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return u;

	}

	public Vector parseJSON(String json) {
		if (json == null) {
			return null;
		}
		Vector u = new Vector();
        Vector vars = findVars(json);
		try {
			JSONObject obj = new JSONObject(json);
			obj = obj.getJSONObject("results");
			JSONArray arr = obj.getJSONArray("bindings");
			for (int i = 0; i < arr.length(); i++) {
				JSONObject o = arr.getJSONObject(i);
				StringBuffer buf = new StringBuffer();
				for (int j=0; j<vars.size(); j++) {
					String s = (String) vars.elementAt(j);
					buf.append(s).append("|");
					try {
						String type = o.getJSONObject(s).getString("type");
						String value = o.getJSONObject(s).getString("value");
						buf.append(type + "|" + value + "$");
						String t = buf.toString();
						t = t.substring(0, t.length()-1);
						u.add(t);
						buf = new StringBuffer();
					} catch (Exception ex) {
                        //ex.printStackTrace();
                        //System.out.println("parseJSON throws exception -- variable:" + s);
					}
			    }
			}
		} catch (Exception ex) {
			//ex.printStackTrace();
			System.out.println("parseJSON throws exception???");
		}
		return u;
	}

	public HashMap line2HashMap(String line) {
		HashMap hmap = new HashMap();
		Vector u = parseData(line, "$");
        for (int i=0; i<u.size(); i++) {
			String t = (String) u.elementAt(i);
			Vector w = JSONUtils.parseData(t, "|");
			String key = (String) w.elementAt(0);
			String type = (String) w.elementAt(1);
			String value = (String) w.elementAt(2);
			TypeAndValue typeAndValue = new TypeAndValue(type, value);
			hmap.put(key, typeAndValue);
		}
		return hmap;
	}


	public void dumpJsonHashMap(HashMap jsonHashMap) {
		Iterator it = jsonHashMap.keySet().iterator();
		while (it.hasNext()) {
			String line = (String) it.next();
			System.out.println(line);
			HashMap hmap = (HashMap) jsonHashMap.get(line);
			Iterator it2 = hmap.keySet().iterator();
			while (it2.hasNext()) {
				String key = (String) it2.next();
				TypeAndValue tv = (TypeAndValue) hmap.get(key);
				System.out.println("\t" + key);
				System.out.println("\t\t" + tv.getType());
				System.out.println("\t\t" + tv.getValue());
			}
		}
	}


	public HashMap json2HashMap(String json) {
		HashMap jsonHashMap = new HashMap();
		Vector v = parseJSON(json);
		for (int k=0; k<v.size(); k++) {
			String line = (String) v.elementAt(k);
			HashMap hmap = new HashMap();
			Vector u = parseData(line, "$");
			for (int i=0; i<u.size(); i++) {
				String t = (String) u.elementAt(i);
				Vector w = JSONUtils.parseData(t, "|");
				String key = (String) w.elementAt(0);
				String type = (String) w.elementAt(1);
				String value = (String) w.elementAt(2);
				TypeAndValue typeAndValue = new TypeAndValue(type, value);
				hmap.put(key, typeAndValue);
			}
			jsonHashMap.put(line, hmap);
		}
		return jsonHashMap;
	}

    public Vector extractValuesFromJSON(String json) {
		Vector vars = findVars(json);
		Vector w = new Vector();
		HashMap jsonHashMap = json2HashMap(json);
		Iterator it = jsonHashMap.keySet().iterator();
		while (it.hasNext()) {
			String line = (String) it.next();
			HashMap hmap = (HashMap) jsonHashMap.get(line);
			Iterator it2 = hmap.keySet().iterator();
			String[] values = new String[vars.size()];
			for (int i=0; i<vars.size(); i++) {
				values[i] = null;
			}
			while (it2.hasNext()) {
				String key = (String) it2.next();
				for (int i=0; i<vars.size(); i++) {
					String var = (String) vars.elementAt(i);
					if (key.compareTo(var) == 0) {
						TypeAndValue tv = (TypeAndValue) hmap.get(key);
						String value = tv.getValue();
						values[i] = value;
						break;
					}
				}
			}
			boolean valid = true;
			StringBuffer buf = new StringBuffer();
			for (int i=0; i<values.length; i++) {
				String value = values[i];
			    if (value == null) {
					valid = false;
					break;
				} else {
					buf.append(value);
					if (i < values.length-1) {
						buf.append("|");
					}
				}
			}
			if (valid) {
				w.add(buf.toString());
			}
		}
		return w;
	}


}

