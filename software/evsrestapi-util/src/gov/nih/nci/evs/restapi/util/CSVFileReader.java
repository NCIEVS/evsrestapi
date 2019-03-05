package gov.nih.nci.evs.restapi.util;

import com.opencsv.CSVReader;
import java.io.*;
import java.util.*;


/**
 * <!-- LICENSE_TEXT_START -->
 * Copyright 2008-2016 NGIS. This software was developed in conjunction
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


public class CSVFileReader {

/*

    public static void sortCSV(String inputfile, int[] sort_options, boolean skip_heading) {
		Vector v = readCSV(inputfile, skip_heading, "$");
		String header = null;
		if (skip_heading) {
			String[] a = extractHeadings(inputfile);
			header = convertToDelimitedValue(a, "$");
		}
        v = quickSort(v, sort_options, "$");
        if (skip_heading) {
            v.add(0, header);
		}
        PrintWriter pw = null;
        try {
 			pw = new PrintWriter(inputfile, "UTF-8");
 			for (int i=0; i<v.size(); i++) {
 				String t = (String) v.elementAt(i);
 				pw.println(t);
 			}
  		} catch (Exception ex) {
            ex.printStackTrace();
 		} finally {
 			try {
 				pw.close();
 				System.out.println("Output file " + inputfile + " generated.");
 		    } catch (Exception ex) {
 				ex.printStackTrace();
 			}
 		}
	}
*/

    public static List readCSV(String csvfile, boolean skip_heading) {
		ArrayList list = new ArrayList();
		int n = csvfile.lastIndexOf(".");
		String nextLine = null;
		Vector w = new Vector();
		String[] values = null;
		try {
			CSVReader reader = new CSVReader(new FileReader(csvfile));
			if (skip_heading) {
				values = reader.readNext();
			}
			while ((values = reader.readNext()) != null) {
				list.add(values);
			}

		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
		return list;
	}

    public static Vector readCSV(String csvfile, boolean skip_heading, String delim) {
		List list_a = readCSV(csvfile, skip_heading);
        Vector v = convertToDelimitedValues(list_a, delim);
        return v;
	}

    public static Vector convertToDelimitedValues(List list_a, String delim) {
		Vector v = new Vector();
		int istart = 0;
		for (int i=0; i<list_a.size(); i++) {
			String[] a = (String[]) list_a.get(i);
            System.out.println("(1) ");
			for (int k=0; k<a.length; k++) {
				String t = (String) a[k];
				System.out.println("\t" + t);
			}

			String s = convertToDelimitedValue(a, delim);
			s = s.trim();
			if (s.length() > 0) {
				v.add(s);
			}
		}
		return v;
	}

    public static String convertToDelimitedValue(String[] a, String delim) {
		StringBuffer buf = new StringBuffer();
		String s = null;
		for (int j=0; j<a.length; j++) {
			s = (String) a[j];

			System.out.println("(2) " + s);

			buf.append(s);
			if (j<a.length-1) {
				buf.append(delim);
			}
		}
		s = buf.toString();
		return s;
	}

	public static String[] extractHeadings(String filename) {
		Vector v = readFile(filename);
		String heading = (String) v.elementAt(0);
		Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(heading, ',');
		String[] headings = new String[u.size()];
		for (int i=0; i<u.size(); i++) {
			String t = (String) u.elementAt(i);
			t = t.substring(1, t.length()-1);
			headings[i] = t;
		}
		return headings;
	}

	 public static void saveToFile(String outputfile, String t) {
		 Vector v = new Vector();
		 v.add(t);
		 saveToFile(outputfile, v);
	 }

	 public static void saveToFile(String outputfile, Vector v) {
		if (outputfile == null) return;
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(outputfile, "UTF-8");
			if (pw != null) {
				if (v != null && v.size() > 0) {
					for (int i=0; i<v.size(); i++) {
						String t = (String) v.elementAt(i);
						pw.println(t);
					}
				}
			}
		} catch (Exception ex) {

		} finally {
			try {
				if (pw != null) pw.close();
				System.out.println("Output file " + outputfile + " generated.");
			} catch (Exception ex) {
				ex.printStackTrace();
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

    public static String csv2Delimited(String line, String delim) {
		//line = line.substring(1, line.length());
		System.out.println(line);
		line = line.replaceAll("\",\"", delim);
		line = line.replaceAll(",,", ",");
		line = line.substring(1, line.length()-1);
		System.out.println(line);
		return line;
	}

    public static Vector csv2Delimited(Vector v, boolean skip_heading, String delim) {
		Vector w = new Vector();
		int istart = 0;
		if (skip_heading) {
			istart = 1;
		}
		for (int i=istart; i<v.size(); i++) {
			String line = (String) v.elementAt(i);

			line = csv2Delimited(line, delim);
			w.add(line);
		}
        return w;
	}


	public static void main(String[] args) {
		String csvfile = args[0];
		/*
		boolean skip_heading = true;
		String delim = "|";
        Vector v = readCSV(csvfile, skip_heading, delim);
        int n = csvfile.lastIndexOf(".");
        String outputfile = csvfile.substring(0, n) + ".txt";
        saveToFile(outputfile, v);
        */
        Vector v = Utils.readFile(csvfile);
        csv2Delimited(v, true, "|");
	}
}



