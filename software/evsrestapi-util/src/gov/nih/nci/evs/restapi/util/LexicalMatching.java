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
import opennlp.tools.stemmer.PorterStemmer;


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
 *     Initial implementation kim.ong@nih.gov
 *
 */


public class LexicalMatching {
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

    static PorterStemmer stemmer = null;

    static HashSet STOP_WORDS = null;
    static HashSet KEYWORDS = null;
    static HashMap signatureMap = null;
    static HashMap id2LabelMap = null;

    static String FULL_SYN_FILE = "FULL_SYN.txt";
    static String STOPWORD_FILE = "stop_words.txt";

    static {
		long ms = System.currentTimeMillis();
		STOP_WORDS = createStopWordSet(STOPWORD_FILE);
		stemmer = new PorterStemmer();
		signatureMap = createSignatureMap();
		id2LabelMap = createId2LabelMap();
		KEYWORDS = createKeywordSet(FULL_SYN_FILE);
		System.out.println("Total initialization run time (ms): " + (System.currentTimeMillis() - ms));
	}

	public static String getLabel(String id) {
		return (String) id2LabelMap.get(id);
	}

	public static boolean isFiller(String word) {
		return STOP_WORDS.contains(word);
	}

	public static boolean isKeyword(String word) {
		return KEYWORDS.contains(word);
	}

    public LexicalMatching(String serviceUrl, String named_graph, String username, String password) {
		this.serviceUrl = serviceUrl;
    	this.named_graph = named_graph;
    	this.username = username;
    	this.password = password;
        this.owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, username, password);
        this.owlSPARQLUtils.set_named_graph(named_graph);
    }

    public OWLSPARQLUtils getOWLSPARQLUtils() {
		return this.owlSPARQLUtils;
	}

	public String construct_get_constains_search(String named_graph, String term) {
		String prefixes = owlSPARQLUtils.getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		term = term.toLowerCase();
		buf.append("select distinct ?x_label ?x_code ?a_target ").append("\n");
		buf.append("from <" + named_graph + ">").append("\n");
		buf.append("where  { ").append("\n");
		buf.append("            	?x a owl:Class .").append("\n");
		buf.append("            	?x :NHC0 ?x_code .").append("\n");
		buf.append("             	?x rdfs:label ?x_label .").append("\n");
		buf.append("").append("\n");
		buf.append("                ?p2 a owl:AnnotationProperty .").append("\n");
		buf.append(" ").append("\n");
		buf.append("                ?a1 a owl:Axiom .").append("\n");
		buf.append("                ?a1 owl:annotatedSource ?x .").append("\n");
		buf.append("                ?a1 owl:annotatedProperty ?p2 .").append("\n");
		buf.append("                ?p2 :NHC0 \"P90\"^^xsd:string .").append("\n");
		buf.append("                ?a1 owl:annotatedTarget ?a_target .").append("\n");
		buf.append("").append("\n");
		buf.append("                FILTER(contains(lcase(?a_target), \"" + term + "\"))").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getConstainsSearch(String named_graph, String term) {
		String query = construct_get_constains_search(named_graph, term);
		Vector v = owlSPARQLUtils.executeQuery(query);
		if (v == null) return null;
		if (v.size() == 0) return v;
		v = new ParserUtils().getResponseValues(v);
		return new SortUtils().quickSort(v);
	}

	public static String getVariant(String t) {
		String s = t.replace("neoplasms", "neoplasm");
		s = s.replace("disorders", "disorder");
		s = s.replace(" and unspecified", "");
		s = s.replace(" NEC", "");
		s = s.replace("'s", "");
		s = s.replace("type 1", "type I");
		s = s.replace("sarcomas", "sarcoma");

		return s;
	}

//(E3-Independent) E2 Ubiquitin-Conjugating Enzyme|C150023|FULL_SYN|(E3-Independent) E2 Ubiquitin-Conjugating Enzyme|PT|NCI||

    public static String removeSpecialCharacters(String t) {
		t = t.replace("(", " ");
		t = t.replace(")", " ");
		t = t.replace(",", " ");
		t = t.replace(";", " ");
		t = t.replace("'s", " ");
		return t;
	}

	public static HashSet createKeywordSet(String filename) {
		HashSet hset = new HashSet();
		Vector v = Utils.readFile(filename);
        for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
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

    public static String getSignature(String term) {
		Vector words = tokenize(term);
		Vector stemmed_words = new Vector();
		for (int i=0; i<words.size(); i++) {
			String word = (String) words.elementAt(i);
			if (!isFiller(word)) {
				String stemmed_word = stemTerm(word);
				stemmed_words.add(stemmed_word);
			}
		}
		stemmed_words = new SortUtils().quickSort(stemmed_words);
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<stemmed_words.size(); i++) {
			String stemmed_word = (String) stemmed_words.elementAt(i);
			buf.append(stemmed_word).append("$");
		}
		String t = buf.toString();
		if (t.length() == 0) return "";
		return t.substring(0, t.length()-1);
	}

	public static HashSet createStopWordSet(String filename) {
		HashSet hset = new HashSet();
		Vector v = Utils.readFile(filename);
        for (int i=0; i<v.size(); i++) {
			String word = (String) v.elementAt(i);
			if (!hset.contains(word)) {
				hset.add(word);
			}
		}
		System.out.println("Number of stop words: " + hset.size());
		return hset;
	}


	public static String stemTerm (String term) {
	    return stemmer.stem(term);
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

    public static void containsSearch(String[] args) {
		long ms = System.currentTimeMillis();
		String serviceUrl = args[0];
		String named_graph = args[1];
		String username = args[2];
		String password = args[3];
		String termfile = args[4];

		Vector w = new Vector();
        LexicalMatching test = new LexicalMatching(serviceUrl, named_graph, username, password);
        Vector v = Utils.readFile(termfile);
        Vector res_vec = new Vector();
        System.out.println(v.size());
        int knt = 0;
        int total = 0;
        for (int i=1; i<v.size(); i++) {
			int j = i+1;
			String line = (String) v.elementAt(i);
			line = line.trim();
			Vector u = StringUtils.parseData(line, '\t');
			String term = (String) u.elementAt(0);
			String term0 = term;
			if (term.length() > 0) {
				total++;
				System.out.println("(" + j + ") " + term);
				w = test.getConstainsSearch(named_graph, term);
				if (w == null || w.size() == 0) {
					String variant = getVariant(term);
					if (variant.compareTo(term) != 0) {
						w = test.getConstainsSearch(named_graph, variant);
					}
				}

				if (w != null && w.size() > 0) {
					knt++;
					for (int k=0; k<w.size(); k++) {
						String s = (String) w.elementAt(k);
						res_vec.add(term0 + "\t" + s);
					}
					Utils.dumpVector(term0, w);
				}
			}
		}
		System.out.println("" + knt + " out of " + total + " matches.");
		Utils.saveToFile("result_" + termfile, res_vec);
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
    }

    public static void sort(String filename) {
		HashMap hmap = new HashMap();
		Vector v = Utils.readFile(filename);
        for (int i=1; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			line = line.trim();
			Vector u = StringUtils.parseData(line, '\t');
			String term = (String) u.elementAt(0);
			String match = (String) u.elementAt(1);
			term = term.trim();
			if (term.length() > 0) {
				Vector w = new Vector();
				if (hmap.containsKey(term)) {
					w = (Vector) hmap.get(term);
				}
				w.add(match);
				hmap.put(term, w);
			}
		}
        System.out.println(hmap.keySet().size());
        Iterator it = hmap.keySet().iterator();
        int lcv = 0;
        Vector keys = new Vector();
        while (it.hasNext()) {
			String key = (String) it.next();
			keys.add(key);
		}
		keys = new SortUtils().quickSort(keys);

        for (int i=0; i<keys.size(); i++) {
			String key = (String) keys.elementAt(i);
			lcv++;
			Vector values = (Vector) hmap.get(key);
			Utils.dumpVector("(" + lcv + ") " + key, values);
		}
	}

	public static HashMap createId2LabelMap() {
		HashMap hmap = new HashMap();
		Vector v = Utils.readFile(FULL_SYN_FILE);
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String label = (String) u.elementAt(0);
			String code = (String) u.elementAt(1);
			hmap.put(code, label);
		}
		return hmap;
	}

	public static HashMap createSignatureMap() {
		HashMap hmap = new HashMap();
		Vector v = Utils.readFile(FULL_SYN_FILE);
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String term = (String) u.elementAt(3);
			String code = (String) u.elementAt(1);
			String signature = getSignature(term);
			Vector w = new Vector();
			if (hmap.containsKey(signature)) {
				w = (Vector) hmap.get(signature);
			}
			if (!w.contains(code)) {
				w.add(code);
			}
			hmap.put(signature, w);
		}
		return hmap;
	}

    public static void run(String termfile) {
		int total = 0;
		Vector v = Utils.readFile(termfile);
		int num_matches = 0;
        for (int i=1; i<v.size(); i++) {
			String term = (String) v.elementAt(i);
			term = term.trim();
			if (term.length() > 0) {
				total++;
				int j = i+1;
				System.out.println("\n(" + j + ") " + term);
				Vector w = tokenize(term);
				for (int k=0; k<w.size(); k++) {
					String word = (String) w.elementAt(k);
					if (isKeyword(word)) {
						if (isFiller(word)) {
							System.out.println("\t" + word + " (*)");
						} else {
							System.out.println("\t" + word);
						}
					} else {
						System.out.println("\t" + word + " (?)");
					}
				}
				String signature = getSignature(term);
				if (signatureMap.containsKey(signature)) {
					System.out.println("\n\tMatches: ");
					w = (Vector) signatureMap.get(signature);
					for (int k=0; k<w.size(); k++) {
						String code = (String) w.elementAt(k);
						String label = (String) getLabel(code);
						System.out.println("\t" + label + " (" + code + ")");
					}
					num_matches++;
				}
			}
		}
        System.out.println("" + num_matches + " out of " + total + " matches.");
	}

    public static void main(String[] args) {
		String termfile = args[0];
		run(termfile);
	}
}
