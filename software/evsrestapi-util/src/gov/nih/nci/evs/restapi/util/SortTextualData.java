package gov.nih.nci.evs.restapi.util;
import java.io.*;
import java.util.*;

/**
 * <!-- LICENSE_TEXT_START -->
 * Copyright 2008,2009 NGIT. This software was developed in conjunction
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
 *      "This product includes software developed by NGIT and the National
 *      Cancer Institute."   If no such end-user documentation is to be
 *      included, this acknowledgment shall appear in the software itself,
 *      wherever such third-party acknowledgments normally appear.
 *   3. The names "The National Cancer Institute", "NCI" and "NGIT" must
 *      not be used to endorse or promote products derived from this software.
 *   4. This license does not authorize the incorporation of this software
 *      into any third party proprietary programs. This license does not
 *      authorize the recipient to use any trademarks owned by either NCI
 *      or NGIT
 *   5. THIS SOFTWARE IS PROVIDED "AS IS," AND ANY EXPRESSED OR IMPLIED
 *      WARRANTIES, (INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *      OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE) ARE
 *      DISCLAIMED. IN NO EVENT SHALL THE NATIONAL CANCER INSTITUTE,
 *      NGIT, OR THEIR AFFILIATES BE LIABLE FOR ANY DIRECT, INDIRECT,
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
 *          Modification history Initial implementation kim.ong@ngc.com
 *
 */

public class SortTextualData {

    public static final int SORT_BY_NAME = 1;
    public static final int SORT_BY_CODE = 2;

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

    public static String getFieldValue(String line, int index) {
		Vector u = parseData(line, '\t');
		return (String) u.elementAt(index);

	}

	 public static void saveToFile(String outputfile, String t) {
		 Vector v = new Vector();
		 v.add(t);
		 saveToFile(outputfile, v);
	 }

	 public static void saveToFile(String outputfile, Vector v) {

		PrintWriter pw = null;
		try {
			pw = new PrintWriter(outputfile, "UTF-8");
			if (v != null && v.size() > 0) {
				for (int i=0; i<v.size(); i++) {
					String t = (String) v.elementAt(i);
					pw.println(t);
				}
		    }
		} catch (Exception ex) {

		} finally {
			try {
				pw.close();
				System.out.println("Output file " + outputfile + " generated.");
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	 }

	public static void saveToFile(PrintWriter pw, Vector v) {
		if (v != null && v.size() > 0) {
			for (int i=0; i<v.size(); i++) {
				String t = (String) v.elementAt(i);
				pw.println(t);
			}
		}
	}

	public static Vector readFile(String filename)
	{
		Vector v = new Vector();
		try {
			BufferedReader in = new BufferedReader(
			   new InputStreamReader(
						  new FileInputStream(filename), "UTF8"));
			String str;
			while ((str = in.readLine()) != null) {
				v.add(str);
			}
            in.close();
		} catch (Exception ex) {
            ex.printStackTrace();
		}
		return v;
	}



    /**
     * Performs quick sort of a List by name.
     *
     * @param list an instance of List
     */
    public void quickSort(List list) {
        quickSort(list, SORT_BY_NAME);
    }

    /**
     * Performs quick sort of a List by a specified sort option.
     *
     * @param list an instance of List
     * @param sort_option, an integer; 1, if sort by name; 2: if sort by code
     */
    public void quickSort(List list, int sort_option) {
        if (list == null)
            return;
        if (list.size() <= 1)
            return;
        try {
            Collections.sort(list, new RowComparator(sort_option));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Performs quick sort of a Vector by a specified sort option.
     *
     * @param v an instance of Vector
     * @param sort_option, an integer; 1, if sort by name; 2: if sort by code
     */

    public Vector quickSort(Vector v, int sort_option) {
        if (v == null)
            return v;
        if (v.size() <= 1)
            return v;
        try {
            Collections.sort((List) v, new RowComparator(sort_option));
            return v;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Performs quick sort of a Vector by name.
     *
     * @param v an instance of Vector
     */

    public Vector quickSort(Vector v) {
        return quickSort(v, SORT_BY_NAME);
    }

    @SuppressWarnings("unchecked")
    public Enumeration<?> sort(Enumeration<?> enumeration) {
        if (enumeration == null)
            return enumeration;

        List keyList = Collections.list(enumeration);
        Collections.sort(keyList);
        enumeration = Collections.enumeration(keyList);
        return enumeration;
    }

	public Vector caseInsensitiveSort(Vector v) {
		if (v == null) return null;
		HashMap hmap = new HashMap();
		Vector keys = new Vector();
		Vector values = new Vector();
		for (int i=0; i<v.size(); i++) {
			String key = (String) v.elementAt(i);
			String key_lower_case = key.toLowerCase();
			keys.add(key_lower_case);
			hmap.put(key_lower_case, key);
		}
		keys = quickSort(keys);
		for (int i=0; i<keys.size(); i++) {
			String key = (String) keys.elementAt(i);
			String value = (String) hmap.get(key);
			values.add(value);
		}
		return values;
	}

	public static void dumpVector(String label, Vector v) {
		System.out.println(label);
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			int j = i+1;
			System.out.println("(" + j + ") " + t);
		}
	}

	public static boolean isInteger(String str) {
		try {
			Integer.parseInt(str);
			return true;
		} catch (NumberFormatException nfe) {
			return false;
		}
	}

	public static String sort(String datafile) {
		Vector v = readFile(datafile);
		Vector w = sort(v);
		saveToFile("sorted_" + datafile, w);
		return "sorted_" + datafile;
	}

    public static float[] sortFloatArray(float[] fArr) {
        Arrays.sort(fArr);
        return fArr;
    }

	public static Vector sort(Vector v) {
		String header = (String) v.elementAt(0);
		Vector u = parseData(header, '\t');
		HashMap hmap = new HashMap();

		float[] keys = new float[v.size()];
		float key = (float) 0.0;
		int knt = 0;
		for (int i=1; i<v.size(); i++) {
			try {
				String t = (String) v.elementAt(i);
				u = parseData(t, '\t');
				String str = (String) u.elementAt(u.size()-1);
				if (str.length() > 0) {
					if (isInteger(str)) {
						int n = Integer.parseInt(str);
						key = (float) n;
					} else {
						key = Float.parseFloat(str);
					}
					hmap.put(String.valueOf(key),  t);
					keys[i] = key;
				} else {
					knt++;
					str = "0." + knt;
					if (isInteger(str)) {
						int n = Integer.parseInt(str);
						key = (float) n;
					} else {
						key = Float.parseFloat(str);
					}
					hmap.put(String.valueOf(key),  t);
					keys[i] = key;
				}
			} catch(Exception ex) {
                ex.printStackTrace();
			}
		}

		keys = sortFloatArray(keys);

		Vector w = new Vector();
		w.add(header);
		for (int i=0; i<keys.length; i++) {
			try {
				float f = keys[i];
				String float_key = String.valueOf(f);
				String value = (String) hmap.get(float_key);
				if (value != null) {
					w.add(value);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		return w;
	}

}
