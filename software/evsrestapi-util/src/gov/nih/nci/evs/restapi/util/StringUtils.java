package gov.nih.nci.evs.restapi.util;

import gov.nih.nci.evs.restapi.bean.*;

import java.io.*;
import java.net.URI;
import java.text.*;
import java.util.*;
import org.apache.commons.lang.StringEscapeUtils;


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


public class StringUtils {

	public static Vector parseData(String t, String delimStr) {
		String delim_vec = "|$%#&*@^!?<>-+=:;\"";
		for (int i=0; i<delim_vec.length(); i++) {
			char c = delim_vec.charAt(i);
			String ch = "" + c;
			if (t.indexOf(ch) == -1) {
				t = replace(t, delimStr, ch);
				return parseData(t, c);
			}
		}
		return null;
	}

	public static String replace(String s, String from, String to) {
		return s.replace(from, to);
	}

    public static Vector parseData(String line, char delimiter) {
		if(line == null) return null;
		Vector w = new Vector();
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<line.length(); i++) {
			char c = line.charAt(i);
			if (c == delimiter) {
				w.add(buf.toString());
				buf = new StringBuffer();
			} else {
				buf.append(c);
			}
		}
		w.add(buf.toString());
		return w;
	}

    public static Vector<String> parseData(String line) {
		if (line == null) return null;
        String delim = "|";
        return parseData(line, delim);
    }


    public static String encode(String line) {
		line = line.replaceAll("<", "&lt;");
		line = line.replaceAll(">", "&gt;");
		return line;
	}

    public static boolean isAlphanumeric(String str) {
        for (int i=0; i<str.length(); i++) {
            char c = str.charAt(i);
            if (!Character.isDigit(c) && !Character.isLetter(c))
                return false;
        }
        return true;
    }

	public static boolean isInteger( String input )
	{
	   if (input == null) return false;
	   try {
		  Integer.parseInt( input );
		  return true;
	   } catch( Exception e) {
		  return false;
	   }
	}


    public static boolean isNull(String value) {
		if (value == null || value.compareToIgnoreCase("null") == 0 || value.compareToIgnoreCase("undefined") == 0) return true;
		return false;
	}

    public static boolean isNullOrBlank(String value) {
		if (value == null || value.compareToIgnoreCase("null") == 0 || value.compareTo("") == 0 || value.compareTo("undefined") == 0) return true;
		return false;
	}

    public static boolean isNullOrEmpty(Vector v) {
		if (v == null || v.size() == 0) return true;
		return false;
	}
/*
    public static String replaceAll(String s, String t1, String t2) {
		if (s == null) return s;
        s = s.replaceAll(t1, t2);
        return s;
    }
*/
	public static String getStringComponent(String bar_delimed_str, int index) {
		if (bar_delimed_str == null) return null;
		Vector u = StringUtils.parseData(bar_delimed_str);
		if (index >= u.size()) return null;
		return (String) u.elementAt(index);
	}

	public static String getStringComponent(String bar_delimed_str, String delim, int index) {
		if (bar_delimed_str == null) return null;
		Vector u = StringUtils.parseData(bar_delimed_str, delim);
		if (index >= u.size()) return null;
		return (String) u.elementAt(index);
	}


	public static void dumpVector(Vector v) {
		if (v == null) return;
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			System.out.println(t);
		}
	}

	public static void dumpVector(String label, Vector v) {
		if (v == null) return;
		System.out.println(label + ":");
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			int j=i+1;
			System.out.println("\t(" + j + ") " + t);
		}
	}

	public static void dumpVector(PrintWriter pw, String label, Vector v) {
		if (v == null) return;
		pw.println(label + ":");
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			int j=i+1;
			pw.println("\t(" + j + ") " + t);
		}
	}

	public static void dumpHashSet(String label, HashSet hset) {
		if (hset == null) return;
		System.out.println(label + ":");
		Iterator it = hset.iterator();
		int i = 0;
		while (it.hasNext()) {
			String t = (String) it.next();
			i++;
			System.out.println("\t(" + i + ") " + t);
		}
	}
/*
	public static void dumpHashMap(String label, HashMap hmap) {
		if (hmap == null) return;
		System.out.println(label + ":");
		Iterator it = hmap.keySet().iterator();
		while (it.hasNext()) {
			String t = (String) it.next();
			System.out.println("\tkey: " + t);
			Vector v = (Vector) hmap.get(t);
			if (v != null && v.size() > 0) {
				for (int j=0; j<v.size(); j++) {
					String s = (String) v.elementAt(j);
					System.out.println("\t\t" + s);
				}
			}
		}
	}
*/

	public static void dumpHashMap(String label, HashMap hmap) {
		if (hmap == null) return;
		System.out.println(label + ":");
		Iterator it = hmap.keySet().iterator();
		while (it.hasNext()) {
			String t = (String) it.next();
			System.out.println("\tkey: " + t);
			Object obj = hmap.get(t);
			if (obj instanceof Vector) {
				Vector v = (Vector) obj;
				if (v != null && v.size() > 0) {
					for (int j=0; j<v.size(); j++) {
						String s = (String) v.elementAt(j);
						System.out.println("\t\t" + s);
					}
				}
			} else if (obj instanceof String) {
				String s = (String) obj;
				System.out.println("\t\t" + s);
			} else if (obj instanceof Atom) {
				Atom s = (Atom) obj;
				System.out.println("\t\t" + s.toJson());
			}
		}
	}

    public static void outputHashMap(HashMap hmap) {
        PrintWriter pw = new PrintWriter(System.out, true);
        outputHashMap(pw, null, hmap);
	}

    public static void outputHashMap(String label, HashMap hmap) {
        PrintWriter pw = new PrintWriter(System.out, true);
        outputHashMap(pw, label, hmap);
	}

    public static void outputHashMap(PrintWriter pw, String label, HashMap hmap) {
		if (label != null) {
			pw.println(label + ":");
		}
		Vector key_vec = new Vector();
		Iterator it = hmap.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			key_vec.add(key);
		}
		key_vec = new SortUtils().quickSort(key_vec);
		for (int i=0; i<key_vec.size(); i++) {
			String key = (String) key_vec.elementAt(i);
			Vector v = (Vector) hmap.get(key);
			for (int k=0; k<v.size(); k++) {
				String value = (String) v.elementAt(k);
				pw.println(key + " --> " + value);
			}
		}
	}

    public static String convertToCommaSeparatedValue(Vector v) {
        if (v == null)
            return null;
        String s = "";
        if (v.size() == 0)
            return s;

        StringBuffer buf = new StringBuffer();
        buf.append((String) v.elementAt(0));
        for (int i = 1; i < v.size(); i++) {
            String next = (String) v.elementAt(i);
            buf.append("; " + next);
        }
        return buf.toString();
    }

    public static String int2String(Integer int_obj) {
        if (int_obj == null) {
            return null;
        }
        String retstr = Integer.toString(int_obj);
        return retstr;
    }

    public static String encodeTerm(String s) {
		if (s == null) return null;
		if (StringUtils.isAlphanumeric(s)) return s;

        StringBuilder buf = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
			if (Character.isLetterOrDigit(c)) {
                buf.append(c);
            } else {
                buf.append("&#").append((int) c).append(";");
            }
        }
        return buf.toString();
    }


// Reference: http://www.walterzorn.de/en/tooltip_old/tooltip_e.htm
// (whilespace after &lt; is intentional)
    public static String encode_term(String s) {
		if (s == null) return null;
		if (StringUtils.isAlphanumeric(s)) return s;
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == 60) {
				buf.append("&lt; ");
			} else if (c == 62) {
				buf.append("&gt;");
			} else if (c == 38) {
				buf.append("&amp;");
			} else if (c == 32) {
				buf.append("&#32;");
			} else {
				buf.append(c);
			}
        }
        String t = buf.toString();
        return t;
    }



    public static String decode_term(String t) {
	    t = t.replaceAll("&amp;", "&");
	    t = t.replaceAll("&lt;", "<");
	    t = t.replaceAll("&gt;", ">");
	    t = t.replaceAll("&quot;", "\"");
	    t = t.replaceAll("&apos;", "'");
	    return t;
    }



    /*
     * To convert a string to the URL-encoded form suitable for transmission as
     * a query string (or, generally speaking, as part of a URL), use the escape
     * function. This function works as follows: digits, Latin letters and the
     * characters + - * / . _ @ remain unchanged; all other characters in the
     * original string are replaced by escape-sequences %XX, where XX is the
     * ASCII code of the original character. Example: escape("It's me!") //
     * result: It%27s%20me%21
     */

    public static String htmlEntityEncode(String s) {
        StringBuilder buf = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0'
                && c <= '9') {
                buf.append(c);
            } else {
                buf.append("&#").append((int) c).append(";");
            }
        }
        return buf.toString();
    }


    public static String escape(String s) {
		if (s == null) return null;
		if (StringUtils.isAlphanumeric(s)) return s;

        StringBuilder buf = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\') {
				buf.append("\\").append("\\");
            } else {
                buf.append(c);
            }
        }
        return buf.toString();
	}

   public static String removeWhiteSpaces(String str) {
	   if (str == null) return null;
	   StringBuffer buf = new StringBuffer();
	   for (int i=0; i<str.length(); i++) {
		   char c = str.charAt(i);
		   if (c != ' ') {
			   buf.append(c);
		   }
	   }
	   return buf.toString();
   }

   public static String getFieldValue(String line, int index) {
		if (line == null) return null;
        Vector u = parseData(line);
        if (u.size() <= index) return null;
        return (String) u.elementAt(index);
   }

   public static String getFieldValue(String line, String delim, int index) {
		if (line == null) return null;
        Vector u = parseData(line, delim);
        if (u.size() <= index) return null;
        return (String) u.elementAt(index);
   }

   public static String createPrefixString(String prefixes) {
		Vector w = StringUtils.parseData(prefixes, "$");
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<w.size(); i++) {
			String t = (String) w.elementAt(i);
			t = t.replaceAll("&lt;", "<");
			t = t.replaceAll("&gt;", ">");
			buf.append(t).append("\n");
		}
		return buf.toString();
   }

   public static String replaceAll(String str, String target, String to) {
	   if (str == null) return null;
		int n = str.indexOf(target);
		while (n != -1) {
			String s1 = str.substring(0, n);
			String s2 = str.substring(n + target.length(), str.length());
			str = s1 + to + s2;
			n = str.indexOf(target);
		}
		return str;
   }

	public static String fixBlankFields(String t) {
		if (t == null) return null;
		if (t.length() <= 1) return t;
		StringBuffer buf = new StringBuffer();
		char c0 = t.charAt(0);
		buf.append(c0);
		for (int i=1; i<t.length(); i++) {
			char c1 = t.charAt(i);
			if (c1 == '|' && c0 == '|') {
				buf.append(" ");
			}
			buf.append(c1);
			c0 = c1;
		}
		return buf.toString();
	}

    public String removeNamespace(String t, String ns) {
		int n = t.indexOf(ns);
		if (n != -1) {
		    return t.substring(n + ns.length(), t.length());
		}
		return t;
	}

    public Vector removeNamespace(Vector v, String ns) {
		if (v == null) return null;
		if (v.size() == 0) return v;
		Vector u = new Vector();
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			String s = removeNamespace(t, ns);
			u.add(s);
		}
		return u;
	}

	public static String getToday() {
		return getToday("MM-dd-yyyy");
	}

	public static String getToday(String format) {
		java.util.Date date = Calendar.getInstance().getTime();
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		return sdf.format(date);
	}

    public static String escapeJava(String inputStr) {
	   return StringEscapeUtils.escapeJava(inputStr);
    }

    public static String escapeXML(String inputStr) {
	   return StringEscapeUtils.escapeXml(inputStr);
    }

	public static String removePackageNames(String packageName, String str) {
		if (str == null) return null;
		str = str.replaceAll(packageName + ".", "");
		return str;
	}

	public static String escapeDoubleQuotes(String inputStr) {
		StringBuffer buf = new StringBuffer();
		for (int i=0;  i<inputStr.length(); i++) {
			char c = inputStr.charAt(i);
			if (c == '\"') {
				buf.append("\\");
			}
			buf.append(c);
		}
		return buf.toString();
	}

    public static HashMap createHashMap(Vector v, int key_index, int value_index) {
		if (v == null) return null;
		HashMap hmap = new HashMap();
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(t, '|');
			String key = (String) u.elementAt(key_index);
			String value = null;
			if (value_index == -1) {
				value = t;
			} else {
			    value = (String) u.elementAt(value_index);
			}
			hmap.put(key, value);
		}
		return hmap;
	}

	public static Vector removeDuplicates(Vector v) {
		if (v == null) return v;
		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			if (!w.contains(t)) {
				w.add(t);
			}
		}
		return w;
	}

	public static HashMap createHashMap(Vector w) {
		return createHashMap(w, 0, 1);
	}

    public static String trimLeadingBlanksOrTabs(String line) {
		if (line == null) return null;
		if (line.length() == 0) return line;
		char c = line.charAt(0);
		while (c == ' ' || c == '\t') {
			line = line.substring(1, line.length());
			if (line.length() == 0) return line;
		    c = line.charAt(0);
		}
		return line;
	}
/*
	public String vector2HTMLTable(Vector v) {
		if (v == null || v.size() == 0) return null;
		StringBuffer buf = new StringBuffer();
		String line = (String) v.elementAt(0);
		Vector u = StringUtils.parseData(line, '|');
		int n = u.size();
		buf.append("<table border=\"1\">").append("\n");
		for (int i=0; i<v.size(); i++) {
			line = (String) v.elementAt(i);
			buf.append("<tr>").append("\n");
			u = StringUtils.parseData(line, '|');
			n = u.size();
			for (int j=0; j<n; j++) {
				String field = (String) u.elementAt(j);
				buf.append("<td>").append(field).append("</td>").append("\n");
			}
			buf.append("</tr>").append("\n");
		}
		buf.append("</table>").append("\n");
		return buf.toString();

	}

	public String vector2XML(Vector v) {
		if (v == null || v.size() == 0) return null;
		List<String> headings = null;
		List<Row> rows = new ArrayList();
		Vector u = null;
		String line = null;
		for (int i=0; i<v.size(); i++) {
			List<String> list = new ArrayList();
			line = (String) v.elementAt(i);
			u = StringUtils.parseData(line, '|');
			int n = u.size();
			for (int j=0; j<n; j++) {
				String field = (String) u.elementAt(j);
				list.add(field);
			}
			Row row = new Row(list);
			rows.add(row);
		}
        Table table = new Table("Query Response", headings, rows);
		return table.toXML();
	}
*/

	public String toCSV(String str) {
		if (str == null) return null;
		StringBuffer buf = new StringBuffer();
		buf.append("\"");
		for (int i=0; i<str.length(); i++) {
			char c = str.charAt(i);
			buf.append(c);
			if (c == '\"') {
				buf.append(c);
			}
		}
		buf.append("\"");
		return buf.toString();
	}

	public String vector2CSV(Vector v) {
		if (v == null || v.size() == 0) return null;
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			int n = u.size();
			for (int j=0; j<n; j++) {
				String field = (String) u.elementAt(j);
				String str = toCSV(field);
				buf.append(str);
				if (j < n-1) {
					buf.append(",");
				}
			}
			buf.append("\n");
		}
		return buf.toString();
	}

	public String vector2XMLString(Vector v) {
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			line = line.replaceAll("<", "&lt;");
			line = line.replaceAll(">", "&gt;");
			buf.append(line).append("\n");

		}
        return buf.toString();
	}

	public String vector2Text(Vector v) {
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			buf.append(line).append("\n");
		}
        return buf.toString();
	}


	public String encodeXML(String xml) {
		xml = xml.replaceAll("<", "&lt;");
		xml = xml.replaceAll(">", "&gt;");
		return xml;
	}

    public Vector trim_namespaces(Vector v) {
		if (v == null) return null;
		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			StringBuffer  buf = new StringBuffer();
			Vector u = StringUtils.parseData(line, '|');
			for (int j=0; j<u.size(); j++) {
				String field = (String) u.elementAt(j);
				int n = field.lastIndexOf("#");
				if (n != -1) {
					field = field.substring(n+1, field.length());
				}
				buf.append(field);
				if (j<u.size()-1) {
					buf.append("|");
				}
			}
			w.add(buf.toString());

		}
		return w;
	}

	public static Vector getKeys(HashMap hmap) {
        Vector keys = new Vector();
        Iterator it = hmap.keySet().iterator();
        while (it.hasNext()) {
			String key = (String) it.next();
			keys.add(key);
		}
		keys = new gov.nih.nci.evs.restapi.util.SortUtils().quickSort(keys);
		return keys;
	}

    public static HashMap constructHashMap(Vector v, int index_key, int index_value) {
		char delim = '|';
		return constructHashMap(v, index_key, index_value, false, delim);
	}

    public static HashMap constructHashMap(Vector v, int index_key, int index_value, char delim) {
		return constructHashMap(v, index_key, index_value, false, delim);
	}

    public static HashMap constructHashMap(Vector v, int index_key, int index_value, boolean skip_heading, char delim) {
        HashMap hmap = new HashMap();
        int istart = 0;
        if (skip_heading) istart = 1;
        for (int i=istart; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, delim);

			if (index_key < u.size() && index_value < u.size()) {
				String key = (String) u.elementAt(index_key);
				String value = (String) u.elementAt(index_value);
				hmap.put(key, value);
			}
		}
		return hmap;
	}

    public static HashMap constructMultiValuedHashMap(Vector v, int index_key, int index_value) {
		char delim = '|';
		return constructMultiValuedHashMap(v, index_key, index_value, false, delim);
	}


    public static HashMap constructMultiValuedHashMap(Vector v, int index_key, int index_value, char delim) {
		return constructMultiValuedHashMap(v, index_key, index_value, false, delim);
	}

    public static HashMap constructMultiValuedHashMap(Vector v, int index_key, int index_value, boolean skip_heading, char delim) {
		HashMap hmap = new HashMap();
        int istart = 0;
        if (skip_heading) istart = 1;
        for (int i=istart; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(t, delim);
			String label = (String) u.elementAt(0);
			String code = (String) u.elementAt(1);
			String value = (String) u.elementAt(3);
			Vector w = new Vector();
			if (hmap.containsKey(code)) {
				w = (Vector) hmap.get(code);
			}
			if (!w.contains(value)) {
				w.add(value);
			}
			hmap.put(code, w);
		}
		return hmap;
	}
}
