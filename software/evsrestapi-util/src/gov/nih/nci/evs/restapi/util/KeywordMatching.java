package gov.nih.nci.evs.restapi.util;

import java.io.*;
import java.text.*;
import java.net.*;
import java.util.*;
import java.nio.file.*;
import java.nio.charset.Charset;

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


public class KeywordMatching {

	static String TERMFILE = "axiom_data.txt";
	static Vector TERMDATA = null;
	static HashMap hmap = null;
	static HashSet keywords = null;
	static HashMap code2TermMap = null;
	static HashMap code2LabelMap = null;
    static HashMap code2NormalizedTermMap = null;
    static HashMap code2WordSeqMap = null;

	static {
		TERMDATA = Utils.readFile(TERMFILE);
		keywords = createKeywordSet();
		code2TermMap = createCode2TermMap();
		code2LabelMap = createCode2LabelMap();
        code2NormalizedTermMap = createCode2SynMap(false);

		code2WordSeqMap = new HashMap();

        Iterator it = code2NormalizedTermMap.keySet().iterator();
        int knt = 0;
        while (it.hasNext()) {
			knt++;
			String code = (String) it.next();
            String label = (String) code2LabelMap.get(code);
			Vector w = (Vector) code2NormalizedTermMap.get(code);
			String displayLabel = label + " (" + code + ")";
			//Utils.dumpVector("(" + knt + ") " + displayLabel, w);

			HashMap map = getFrequenciesOfWordsInConcept(w);
			//dumpFreqHashMap("HashMap of Freq " + displayLabel, map);

			w = sortWordsByFrequency(map);
			//Utils.dumpVector("Freq " + displayLabel, w);

			String str = listWordsByFreq(w);
			//System.out.println("\n" + str);

			code2WordSeqMap.put(code, StringUtils.parseData(str, ' '));
		}


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

    public static void dumpHashMap(String label, HashMap hmap) {
		System.out.println("\n" + label + ":");
		if (hmap == null) {
			System.out.println("\tNone");
			return;
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
			String value = (String) (String) hmap.get(key);
			System.out.println(key + " --> " + value);
		}
		System.out.println("\n");
	}

    public static void dumpMultiValuedHashMap(String label, HashMap hmap) {
   	    System.out.println("\n" + label + ":");
		if (hmap == null) {
			System.out.println("\tNone");
			return;
		}
		Vector key_vec = new Vector();
		Iterator it = hmap.keySet().iterator();
		while (it.hasNext()) {
			String nv = (String) it.next();
			key_vec.add(nv);
		}
		key_vec = new SortUtils().quickSort(key_vec);
		for (int k=0; k<key_vec.size(); k++) {
			String nv = (String) key_vec.elementAt(k);
			System.out.println("\n");
			Vector v = (Vector) hmap.get(nv);
			for (int i=0; i<v.size(); i++) {
				String q = (String) v.elementAt(i);
				System.out.println(nv + " --> " + q);
			}
		}
		System.out.println("\n");
	}

    public static void dumpVector(String label, Vector v) {
		System.out.println("\n" + label + ":");
		if (v == null) return;
		if (v.size() == 0) {
			System.out.println("\tNone");
			return;
		}
        for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			int j = i+1;
			System.out.println("\t(" + j + ") " + t);
		}
		System.out.println("\n");
	}

    public static void dumpVector(String label, Vector v, boolean display_label, boolean display_index) {
		if (display_label) {
			System.out.println("\n" + label + ":");
		}
		if (v == null || v.size() == 0) {
			System.out.println("\tNone");
			return;
		}
        for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			int j = i+1;
			if (display_index) {
				System.out.println("\t(" + j + ") " + t);
			} else {
				System.out.println("\t" + t);
			}
		}
		System.out.println("\n");
	}

    public static void dumpArrayList(String label, ArrayList list) {
		System.out.println("\n" + label + ":");
		if (list == null || list.size() == 0) {
			System.out.println("\tNone");
			return;
		}
        for (int i=0; i<list.size(); i++) {
			String t = (String) list.get(i);
			int j = i+1;
			System.out.println("\t(" + j + ") " + t);
		}
		System.out.println("\n");
	}

    public static void dumpList(String label, List list) {
		System.out.println("\n" + label + ":");
		if (list == null || list.size() == 0) {
			System.out.println("\tNone");
			return;
		}
        for (int i=0; i<list.size(); i++) {
			String t = (String) list.get(i);
			int j = i+1;
			System.out.println("\t(" + j + ") " + t);
		}
		System.out.println("\n");
	}


	 public static String replaceFilename(String filename) {
	    return filename.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
	 }


	public static List<String> findFilesInDirectory(String directory) {
		return findFilesInDirectory(new File(directory));
	}

	public static List<String> findFilesInDirectory(File dir) {
		List<String> list = new ArrayList<String>();
		for (File file : dir.listFiles()) {
			if (file.isDirectory()) {
				List<String> list2 = findFilesInDirectory(file);
				list.addAll(list2);
			} else {
				list.add(file.getAbsolutePath());
			}
		}
		return list;
	}


	 public static void saveToFile(String outputfile, String t) {
		 Vector v = new Vector();
		 v.add(t);
		 saveToFile(outputfile, v);
	 }

	public static void saveToFile(PrintWriter pw, Vector v) {
		if (v != null && v.size() > 0) {
			for (int i=0; i<v.size(); i++) {
				String t = (String) v.elementAt(i);
				pw.println(t);
			}
		}
	}

	public static Vector readFile(String datafile) {
		Vector v = new Vector();
        try {
			File file = new File(datafile);
			FileInputStream fis = new FileInputStream(file);
			BufferedInputStream bis = new BufferedInputStream(fis);
			BufferedReader br = null;
			try {
				br = new BufferedReader(new InputStreamReader(bis));
			} catch (Exception ex) {
				return null;
			}

            while (true) {
                String line = br.readLine();
				if (line == null) {
					break;
				}
				v.add(line);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return v;
	}

	public static HashSet vector2HashSet(Vector v) {
		if (v == null) return null;
		HashSet hset = new HashSet();
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			hset.add(t);
		}
		return hset;
	}

	public static Vector hashSet2Vector(HashSet hset) {
		if (hset == null) return null;
		Vector v = new Vector();
		Iterator it = hset.iterator();
		while (it.hasNext()) {
			String t = (String) it.next();
			v.add(t);
		}
		v = new SortUtils().quickSort(v);
		return v;
	}

    public static String changeFileExtension(String filename, String ext) {
		int n = filename.lastIndexOf(".");
		if (n != -1) {
			return filename.substring(0, n) + "." + ext;
		}
		return filename;
	}

	public static HashMap getInverseHashMap(HashMap hmap) {
		HashMap inv_hmap = new HashMap();
		Iterator it = hmap.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			String value = (String) hmap.get(key);
			Vector v = new Vector();
			if (inv_hmap.containsKey(value)) {
				v = (Vector) inv_hmap.get(value);
			}
			v.add(key);
			inv_hmap.put(value, v);
		}
		return inv_hmap;
	}

    public static Vector listFiles(String directory) {
		Vector w = new Vector();
		Collection<File> c = listFileTree(new File(directory));
		int k = 0;
		Iterator it = c.iterator();
		while (it.hasNext()) {
			File t = (File) it.next();
			k++;
			w.add(t.getName());
		}
		w = new SortUtils().quickSort(w);
		return w;
	}


	public static Collection<File> listFileTree(File dir) {
		Set<File> fileTree = new HashSet<File>();
		if(dir==null||dir.listFiles()==null){
			return fileTree;
		}
		for (File entry : dir.listFiles()) {
			if (entry.isFile()) fileTree.add(entry);
			else fileTree.addAll(listFileTree(entry));
		}
		return fileTree;
	}

    public static boolean checkIfFileExists(String filename) {
		String currentDir = System.getProperty("user.dir");
		File f = new File(currentDir + "\\" + filename);
		if(f.exists() && !f.isDirectory()) {
			return true;
		} else {
			return false;
		}
	}

    public static boolean deleteFile(String filename) {
        File file = new File(filename);
        if(file.delete())
        {
            return true;
        }
        else
        {
            return false;
        }
    }


	public static void copyDirectory(String sourceDirectoryLocation, String destinationDirectoryLocation)
	  throws IOException {
	    Files.walk(java.nio.file.Paths.get(sourceDirectoryLocation))
	      .forEach(source -> {
		  java.nio.file.Path destination = java.nio.file.Paths.get(destinationDirectoryLocation, source.toString()
		    .substring(sourceDirectoryLocation.length()));
		  try {
		      Files.copy(source, destination);
		  } catch (IOException e) {
		      e.printStackTrace();
		  }
	      });
	}


	public static String encode(String toEncode) {
		try {
			return java.net.URLEncoder.encode(toEncode.trim(), "UTF-8");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

    public static String generateHyperlink(String name, String propertyName) {
		String propertyName_0 = propertyName;
		if (propertyName.compareTo("Relationships") == 0) {
			propertyName = "list_relationships";
		}
		//propertyName = propertyName.replace(" ", "+");
		String hyperlink = "https://aapmbdsc.azurewebsites.net/?KeyElementName=" + name + "&PropertyName=" + propertyName;
		//hyperlink = hyperlink.replace("�", "&ndash;");
		//hyperlink = encode(hyperlink);
		hyperlink = "<a href=\"" + hyperlink + "\">" + propertyName_0 + "</a>";
		return hyperlink;
	}


	public static boolean isPureAscii(String v) {
		return Charset.forName("US-ASCII").newEncoder().canEncode(v);
	// or "ISO-8859-1" for ISO Latin 1
	// or StandardCharsets.US_ASCII with JDK1.7+
	}

	public String xmlEscapeText(String t) {
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < t.length(); i++){
			char c = t.charAt(i);
			switch(c){
				case '<': sb.append("&lt;"); break;
				case '>': sb.append("&gt;"); break;
				case '\"': sb.append("&quot;"); break;
				case '&': sb.append("&amp;"); break;
				case '\'': sb.append("&apos;"); break;
				default:
					if (c>0x7e) {
						sb.append("&#"+((int)c)+";");
					} else {
						sb.append(c);
					}
			}
		}
		return sb.toString();
	}

	public static Vector loadFileLineByLine(String filename) {
		Vector w = new Vector();
		StringBuffer buf = new StringBuffer();
		try {
			InputStream input = new FileInputStream(filename);
			int data = input.read();
			while(data != -1) {
				char c = (char) data;
				if (c == '\n') {
					String s = buf.toString();
					s = s.trim();
					w.add(s);
					buf = new StringBuffer();
				} else {
					buf.append("" + c);
				}
				data = input.read();
			}
			String s = buf.toString();
			s = s.trim();
			w.add(s);
			input.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return w;
	}

    public static void saveToFile(String outputfile, Vector v) {
        try {
            FileOutputStream output = new FileOutputStream(outputfile);
            for (int i=0; i<v.size(); i++) {
				String data = (String) v.elementAt(i);
				if (i < v.size()) {
					data = data + "\n";
				}
				byte[] array = data.getBytes();
				output.write(array);
			}
            output.close();
        } catch(Exception e) {
            e.getStackTrace();
        }
    }

	public static String extractTagValue(String line, String tag) {
		String t0 = line;
		String t = t0.trim();
		int n = t.indexOf(tag);
		if (n == -1) return null;
		t = t.substring(n+tag.length(), t.length());
		n = t.indexOf("<");
		if (n == -1) return null;
		t = t.substring(1, n);
		t = t.trim();
		return t;
	}

	public static HashSet extractColumnData(String filename, int col, char delim) {
		Vector v = readFile(filename);
		HashSet hset = new HashSet();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, delim);
			String value = (String) u.elementAt(col);
			if (!hset.contains(value)) {
				hset.add(value);
			}
		}
		return hset;
	}

    public static void exploreContent(String filename, char delim) {
        Vector v = readFile(filename);
        String firstLine = (String) v.elementAt(0);
        Vector u = StringUtils.parseData(firstLine, delim);
        Utils.dumpVector(firstLine, u);
	}

	public static void mergeFiles(String sourcefile, int keyCol_1, String targetfile, int keyCol_2, String heading) {
		long ms = System.currentTimeMillis();
        Vector v1 = readFile(sourcefile);
        HashMap hmap = new HashMap();
        for (int i=1; i<v1.size(); i++) {
			String line = (String) v1.elementAt(i);
			Vector u = StringUtils.parseData(line, '\t');
			String t =(String) u.elementAt(keyCol_1);
			hmap.put(t, line);
		}
        Vector v2 = readFile(targetfile);
        Vector w = new Vector();
        w.add(heading);//"Term\tNCIt PT\tNCIt Code");
        int knt1 = 0;
        int knt2 = 0;
        for (int i=0; i<v2.size(); i++) {
			String line = (String) v2.elementAt(i);
			Vector u = StringUtils.parseData(line, '\t');
			String t = (String) u.elementAt(keyCol_2);
			if (hmap.containsKey(t)) {
				w.add(t + "\t" + (String) hmap.get(t));
				knt1++;
			} else {
				w.add(t + "\tNot found\tNot found");
				knt2++;
			}
		}
		Utils.saveToFile("mod_" + targetfile, w);
		System.out.println("No match: " + knt2);
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}

    public Vector extractColumnValues(String filename, int columnIndex, char delim) {
		Vector v = readFile(filename);
		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, delim);
			String value = (String) u.elementAt(columnIndex);
			w.add(value);
		}
		return w;
	}
    public static String removeSpecialCharacters(String t) {
		t = t.replace("(", " ");
		t = t.replace(")", " ");
		t = t.replace(",", " ");
		t = t.replace(";", " ");
		t = t.replace("'s", " ");
		return t;
	}

    public static Vector tokenize(String term) {
		term = term.toLowerCase();
		term = removeSpecialCharacters(term);
		Vector words = new Vector();
		StringTokenizer st = new StringTokenizer(term);
		while (st.hasMoreTokens()) {
		     words.add(st.nextToken());
		}
		return words;
	}

	public static String normalize(String term) {
		Vector words = tokenize(term);
		words = new SortUtils().quickSort(words);
		StringBuffer buf = new StringBuffer();
		for (int j=0; j<words.size(); j++) {
			String word = (String) words.elementAt(j);
			String stemmed_word = LexicalMatching.stemTerm(word);
			buf.append(stemmed_word).append(" ");
		}
		String t = buf.toString();
		return t.trim();
	}

	public static String normalize(String term, boolean stem) {
		Vector words = tokenize(term);
		words = new SortUtils().quickSort(words);
		StringBuffer buf = new StringBuffer();
		for (int j=0; j<words.size(); j++) {
			String word = (String) words.elementAt(j);
			if (stem) {
				String stemmed_word = LexicalMatching.stemTerm(word);
				buf.append(stemmed_word).append(" ");
			} else {
				buf.append(word).append(" ");
			}
		}

		String t = buf.toString();
		return t.trim();
	}

	public static HashMap createCode2SynMap() {
		return createCode2SynMap(true);
	}


	public static HashMap createCode2SynMap(boolean stem) {
		HashMap hmap = new HashMap();
		for (int i=0; i<TERMDATA.size(); i++) {
			String line = (String) TERMDATA.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String code = (String) u.elementAt(1);
			String label = (String) u.elementAt(0);
			String syn = (String) u.elementAt(3);
			Vector w = new Vector();
			if (hmap.containsKey(code)) {
				w = (Vector) hmap.get(code);
			} else {
				w.add(label);
			}

			syn = normalize(syn, stem);
			if (!w.contains(syn)) {
				w.add(syn);
			}

			hmap.put(code, w);
		}
		return hmap;
	}

	public static HashMap createCode2TermMap() {
		HashMap hmap = new HashMap();
		for (int i=0; i<TERMDATA.size(); i++) {
			String line = (String) TERMDATA.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String code = (String) u.elementAt(1);
			String label = (String) u.elementAt(0);
			String syn = (String) u.elementAt(3);
			Vector w = new Vector();
			if (hmap.containsKey(code)) {
				w = (Vector) hmap.get(code);
			} else {
				w.add(label);
			}
    		if (!w.contains(syn)) {
				w.add(syn);
			}
			hmap.put(code, w);
		}
		return hmap;
	}

    public static HashMap createCode2LabelMap() {
		HashMap hmap = new HashMap();
		for (int i=0; i<TERMDATA.size(); i++) {
			String line = (String) TERMDATA.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String code = (String) u.elementAt(1);
			String label = (String) u.elementAt(0);
			hmap.put(code, label);
		}
		return hmap;
	}

	public static HashSet createKeywordSet() {
		HashSet hset = new HashSet();
        for (int i=0; i<TERMDATA.size(); i++) {
			String line = (String) TERMDATA.elementAt(i);
			Vector values = StringUtils.parseData(line, '|');
			String term = (String) values.elementAt(3);
			Vector w = tokenize(term);
			for (int j=0; j<w.size(); j++) {
				String word = (String) w.elementAt(j);
				if (!hset.contains(word)) {
					hset.add(word);
				}
			}
		}
		System.out.println("Number of keywords: " + hset.size());
		return hset;
	}

    public static HashMap getFrequenciesOfWordsInConcept(Vector terms) {
		HashMap hmap = new HashMap();
		for (int i=0; i<terms.size(); i++) {
			String term = (String) terms.elementAt(i);
			Vector words = tokenize(term);
			//Utils.dumpVector(term, words);

			for (int j=0; j<words.size(); j++) {
				String word = (String) words.elementAt(j);
				Integer int_knt = Integer.valueOf(0);
				if (hmap.containsKey(word)) {
					int_knt = (Integer) hmap.get(word);
				}
				int_knt = Integer.valueOf(int_knt.intValue()+1);
				hmap.put(word, int_knt);
			}
		}

		return hmap;
	}

    public static void dumpIntArray(int[] ints) {
		for (int i=0; i<ints.length; i++) {
			System.out.println(ints[i]);
		}
	}

	public static Vector sortWordsByFrequency(HashMap hmap) {
        Vector w = new Vector();
		Iterator it = hmap.keySet().iterator();
		int knt = 0;
		while (it.hasNext()) {
			String key = (String) it.next();
			Integer value = (Integer) hmap.get(key);
			w.add(value + "|" + key);
		}
        w = new SortUtils().quickSort(w);
		return w;
	}

	public static void dumpFreqHashMap(String label, HashMap hmap) {
		System.out.println(label);
        Iterator it = hmap.keySet().iterator();
        while (it.hasNext()) {
			String key = (String) it.next();
            Integer value = (Integer) hmap.get(key);
            System.out.println(key + ": " + value.intValue());
		}
	}

	public static String listWordsByFreq(Vector w) {
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<w.size(); i++) {
			int j = w.size()-i-1;
			String line = (String) w.elementAt(j);
			Vector u = StringUtils.parseData(line, '|');
			String word = (String) u.elementAt(1);
			buf.append(word).append(" ");
		}
		String t = buf.toString();
		t = t.trim();
		return t;
	}

	public static Vector match(String s) {
		Vector w = new Vector();
		String normalized_term = normalize(s, false);
		Vector u1 = StringUtils.parseData(normalized_term, ' ');
		Vector u0 = new Vector();
		for (int i=0; i<u1.size(); i++) {
			String word = (String) u1.elementAt(i);
			if (keywords.contains(word)) {
				u0.add(word);
			}
		}

        Iterator it = code2WordSeqMap.keySet().iterator();
        while (it.hasNext()) {
			String key = (String) it.next();
			Vector u = (Vector) code2WordSeqMap.get(key);
			boolean match = true;
			for (int j=0; j<u0.size(); j++) {
				String word = (String) u0.elementAt(j);
				if (!u.contains(word)) {
					match = false;
					break;
				}
			}
			if (match) {
				Vector terms = (Vector) code2TermMap.get(key);
				String label = (String) code2LabelMap.get(key);
				w.add(label + "|" + key);
			}
		}
		return w;
	}

	public static void main(String[] args) {
		long ms = System.currentTimeMillis();

		String s = "Anal Canal and Anal Margin Squamous Intraepithelial Neoplasia";
		System.out.println("\ns: " + s);
        Vector w = match(s);
        Utils.dumpVector("matches", w);
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}
}

