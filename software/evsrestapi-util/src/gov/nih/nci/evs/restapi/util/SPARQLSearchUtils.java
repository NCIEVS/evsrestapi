package gov.nih.nci.evs.restapi.util;

import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.XStream;
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

import org.apache.commons.text.similarity.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


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

public class SPARQLSearchUtils extends OWLSPARQLUtils {


	public static String[] fillers = new String[] {
			"a",    "about",        "again",        "all",  "almost",
			"also", "although",     "always",       "among",        "an",
			"and",  "another",      "any",  "are",  "as",
			"at",   "be",   "because",      "been", "before",
			"being",        "between",      "both", "but",  "by",
			"can",  "could",        "did",  "do",   "does",
			"done", "due",  "during",       "each", "either",
			"enough",       "especially",   "etc",  "external",     "for",
			"found",        "from", "further",      "had",  "has",
			"have", "having",       "here", "how",  "however", "human",
			"i",    "if",   "in",   "internal",     "into",
			"is",   "it",   "its",  "itself",       "just",
			"made", "mainly",       "make", "may",  "might",
			"most", "mostly",       "must", "nearly",       "neither",
			"nor",  "obtained",     "of",   "often",        "on",
			"or",   "other",        "our",  "overall",      "part",
			"parts",        "perhaps",      "pmid", "quite",        "rather",
			"really",       "regarding",    "secondary",    "seem", "seen",
			"several",      "should",       "show", "showed",       "shown",
			"shows",        "significantly",        "since",        "site", "sites",
			"so",   "some", "specification",        "specified",    "such",
			"than", "that", "the",  "their",        "theirs",
			"them", "then", "there",        "therefore",    "these",
			"they", "this", "those",        "through",      "thus",
			"to",   "unspecified",  "upon", "use",  "used",
			"using",        "various",      "very", "was",  "we",
			"were", "what", "when", "which",        "while",
			"with", "within",       "without",      "would"};

	public static String[] PRESENTATIONS = new String[] {"Preferred_Name", "Display_Name", "FULL_SYN", "Legacy_Concept_Name", "label"};
	public static List PRESENTATION_LIST = Arrays.asList(PRESENTATIONS);

    public static String EXACT_MATCH = "exactMatch";
    public static String STARTS_WITH = "startsWith";
    public static String ENDS_WITH = "endsWith";
    public static String CONTAINS = "contains";
    public static String PHRASE = "Phrase";
    public static String FUZZY = "Fuzzy";
    public static String BOOLEAN_AND = "AND";
    public static String BOOLEAN_OR = "OR";

    public static String NAMES = "names";
    public static String PROPERTIES = "properties";
    public static String ASSOCIATIONS = "associations";

    public static String[] ALGORITHMS = new String[] {EXACT_MATCH, STARTS_WITH, ENDS_WITH, CONTAINS, PHRASE, FUZZY, BOOLEAN_AND};
    public static String[] TARGETS = new String[] {NAMES, PROPERTIES, ASSOCIATIONS};

	public static int MATCH_SOURCE = 1;
	public static int MATCH_TARGET = 2;

    private HashSet filler_set = null;
	static String NAME_DATA = "name_data.txt";

	Vector name_data = null;
	HashMap name_data_hmap = new HashMap();
	HashMap term2label_hmap = new HashMap();
	HashMap signature2Codes_hmap = new HashMap();
	HashMap code2Label_hmap = new HashMap();
	HashMap kwd_freq_hmap = null;
	HashSet rareKeywords = new HashSet();
	Vector rareKeyword_vec = new Vector();

    Vector keyword_vec = null;
    HashSet keywords = null;
    Vector user_fillers = new Vector();
    Vector keyword_freq_vec = new Vector();

	public SPARQLSearchUtils() {
		super();
	}

	public SPARQLSearchUtils(String serviceUrl) {
		super(serviceUrl);
    }

	public SPARQLSearchUtils(ServerInfo serverInfo) {
		super(serverInfo);
	}

	public SPARQLSearchUtils(String serviceUrl, String named_graph, String username, String password) {
		super(serviceUrl, named_graph, username, password);
		long ms = System.currentTimeMillis();
		initialize();
        System.out.println("Total initialization run time (ms): " + (System.currentTimeMillis() - ms));
    }

    public void set_user_fillers(Vector v) {
		this.user_fillers = v;
	}

	public static HashSet toHashSet(Vector a) {
		HashSet hset = new HashSet();
		for (int i=0; i<a.size(); i++) {
			String t = (String) a.elementAt(i);
			hset.add(t);
		}
		return hset;
	}

	public static HashSet toHashSet(String[] a) {
		HashSet hset = new HashSet();
		for (int i=0; i<a.length; i++) {
			String t = a[i];
			hset.add(t);
		}
		return hset;
	}

	public boolean isFiller(String word) {
		if (filler_set.contains(word)) return true;
		if (user_fillers.contains(word)) return true;
		return false;
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

	public String getSignature(Vector words) {
		words = new SortUtils().quickSort(words);
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<words.size(); i++) {
			String word = (String) words.elementAt(i);
			if (!isFiller(word)) {
				buf.append(word);
				if (i < words.size()-1) {
					buf.append("$");
				}
			}
		}
		return buf.toString();
	}

    public void initialize() {
        code2Label_hmap = new HashMap();
        kwd_freq_hmap = new HashMap();
        rareKeywords = new HashSet();
		filler_set = toHashSet(fillers);

		if (checkIfFileExists(NAME_DATA)) {
			name_data = Utils.readFile(NAME_DATA);
		} else {
			name_data = getSearchByNameData(named_graph);
			Utils.saveToFile(NAME_DATA, name_data);
		}
	    name_data_hmap = create_name_data_hmap(name_data);

	    keyword_vec = toKeywords(name_data);
	    keywords = toHashSet(keyword_vec);

	    term2label_hmap = new HashMap();
	    signature2Codes_hmap = new HashMap();

		Iterator it = name_data_hmap.keySet().iterator();
		int lcv = 0;

		boolean debug = false;

		int knt = 0;

		while (it.hasNext()) {
			String key = (String) it.next(); //Pericardial Stripping

			Vector kwds = toKeywords(key);
			for (int k=0; k<kwds.size(); k++) {
				String keyword = (String) kwds.elementAt(k);
				Vector w1 = new Vector();
				Integer int_obj = new Integer(0);
				if (kwd_freq_hmap.containsKey(keyword)) {
					int_obj = (Integer) kwd_freq_hmap.get(keyword);
				}
				int freq = Integer.valueOf(int_obj);
				freq++;
				kwd_freq_hmap.put(keyword, new Integer(freq));
			}

			//Pericardial Stripping|C100004|PERICARDIAL STRIPPING|CDISC|PT
			Vector values = (Vector) name_data_hmap.get(key);
			for (int i=0; i<values.size(); i++) {
				String value = (String) values.elementAt(i);
				Vector w2 = StringUtils.parseData(value);
				String code = (String) w2.elementAt(1);

				code2Label_hmap.put(code, key);
				String term = (String) w2.elementAt(0);



				Vector words = toKeywords(term);
				String signature = getSignature(words);
				Vector codes = new Vector();
				if (signature2Codes_hmap.containsKey(signature)) {
					codes = (Vector) signature2Codes_hmap.get(signature);
				}
				if (!codes.contains(code))codes.add(code);
				signature2Codes_hmap.put(signature, codes);

				term = (String) w2.elementAt(2);
				words = toKeywords(term);
				signature = getSignature(words);
				codes = new Vector();
				if (signature2Codes_hmap.containsKey(signature)) {
					codes = (Vector) signature2Codes_hmap.get(signature);
				}
				if (!codes.contains(code))codes.add(code);
				signature2Codes_hmap.put(signature, codes);

				//Vector u = StringUtils.parseData(value, '|');
				String syn = (String) w2.elementAt(2);
				syn = syn.toLowerCase();
				term2label_hmap.put(syn, key);

				kwds = toKeywords(syn);
				for (int k=0; k<kwds.size(); k++) {
					String keyword = (String) kwds.elementAt(k);
					Vector w1 = new Vector();
					Integer int_obj = new Integer(0);
					if (kwd_freq_hmap.containsKey(keyword)) {
						int_obj = (Integer) kwd_freq_hmap.get(keyword);
					}
					int freq = Integer.valueOf(int_obj);
					freq++;
					kwd_freq_hmap.put(keyword, new Integer(freq));
				}
			}
	    }

		keyword_freq_vec = new Vector();
		Iterator it2 = kwd_freq_hmap.keySet().iterator();
		while (it2.hasNext()) {
			String keyword = (String) it2.next();
			Integer int_obj = (Integer) kwd_freq_hmap.get(keyword);
			keyword_freq_vec.add(keyword + "|" + Integer.valueOf(int_obj));
			if (Integer.valueOf(int_obj) == 1) {
				rareKeywords.add(keyword);
				rareKeyword_vec.add(keyword);
			}
		}
		keyword_freq_vec = new SortUtils().quickSort(keyword_freq_vec);
	}

	public Vector getKeywordFrequency() {
		return keyword_freq_vec;
	}

	public Vector getRareKeywords() {
		return rareKeyword_vec;
	}


    public String containsRareKeywords(String term) {
		Vector kwds = toKeywords(term);
		for (int k=0; k<kwds.size(); k++) {
			String kwd = (String) kwds.elementAt(k);
			if (rareKeywords.contains(kwd)) {
				return kwd;
			}
		}
		return null;
	}

	public boolean isKeyword(String word) {
		return keywords.contains(word);
	}

//3' Flank Mutation|C63431|Downstream Gene Variant|NCI|SY
    public HashMap create_name_data_hmap(Vector name_data) {
		HashMap hmap = new HashMap();
		for (int i=0; i<name_data.size(); i++) {
			String line = (String) name_data.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String key = (String) u.elementAt(0);
			key = key.toLowerCase();
			Vector w = new Vector();
			if (hmap.containsKey(key)) {
			    w = (Vector) hmap.get(key);
			}
			if (!w.contains(line)) {
				w.add(line);
			}
			hmap.put(key, w);
			//KLO, 03022021
			key = (String) u.elementAt(2);
			key = key.toLowerCase();
			w = new Vector();
			if (hmap.containsKey(key)) {
			    w = (Vector) hmap.get(key);
			}
			if (!w.contains(line)) {
				w.add(line);
			}
			hmap.put(key, w);
		}
		return hmap;
	}


    public Vector mapTo(String vbt) {
		vbt = vbt.toLowerCase();
		return (Vector) name_data_hmap.get(vbt);
	}

	public Vector keywordSearch(String word) {
		Vector w = new Vector();
		Iterator it = name_data_hmap.keySet().iterator();
		int lcv = 0;
		while (it.hasNext()) {
			String key = (String) it.next();
			Vector words = toKeywords(key);
			if (words.contains(word)) {
				w.addAll((Vector) name_data_hmap.get(key));
				return w;
			}
			Vector values = (Vector) name_data_hmap.get(key);
			for (int k=0; k<values.size(); k++) {
				String value = (String) values.elementAt(k);
				Vector u = StringUtils.parseData(value, '|');
				String syn = (String) u.elementAt(2);
				Vector v = toKeywords(syn);
				if (v.contains(word)) {
					w.addAll((Vector) name_data_hmap.get(key));
					break;
				}
			}
	    }
	    return w;
	}

	public Vector substringSearch(String vbt) {
		Vector w = new Vector();
		vbt = vbt.toLowerCase();
		vbt = vbt.replace("-", " ");
		Iterator it = name_data_hmap.keySet().iterator();
		int lcv = 0;
		while (it.hasNext()) {
			String key = (String) it.next();
			if (lcv == 0) {
				lcv++;
			}
			if (key.indexOf(vbt) != -1) {
				w.addAll((Vector) name_data_hmap.get(key));
			}
	    }
	    return w;
	}

	public Vector endsWithSearch(String vbt) {
		Vector w = new Vector();
		try {
			Iterator it = term2label_hmap.keySet().iterator();
			while (it.hasNext()) {
				String term = (String) it.next();
				if (term.endsWith(vbt)) {
					String key = (String) term2label_hmap.get(term);
					Vector w2 = (Vector) name_data_hmap.get(key);
					w.addAll(w2);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return w;
    }

	public Vector substring_search(String vbt) {
		Vector w = new Vector();
		vbt = vbt.toLowerCase();
		Iterator it = name_data_hmap.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			Vector values = (Vector) name_data_hmap.get(key);
			for (int k=0; k<values.size(); k++) {
				String value = (String) values.elementAt(k);
				Vector u = StringUtils.parseData(value, '|');
				String syn = (String) u.elementAt(2);
				Vector v = toKeywords(syn);
				if (v.contains(vbt)) {
					w.addAll((Vector) name_data_hmap.get(key));
				}
			}
	    }
	    return w;
	}


    public String getMatchFilter(String var, String algorithm, String searchString) {
		if (algorithm.compareTo(EXACT_MATCH) == 0) {
			return "    FILTER (lcase(?" + var + ") = \"" + searchString + "\")";
		} else if (algorithm.compareTo(STARTS_WITH) == 0) {
			return "    FILTER (STRSTARTS(lcase(?" + var + "), \"" + searchString + "\"))";
		} else if (algorithm.compareTo(ENDS_WITH) == 0) {
			return "    FILTER (STRENDS(lcase(?" + var + "), \"" + searchString + "\"))";
		} else if (algorithm.compareTo(CONTAINS) == 0) {
			return "    FILTER (CONTAINS(lcase(?" + var + "), \"" + searchString + "\"))";
		}
		return "";
	}

	public String getLabel(String code) {
		return (String) code2Label_hmap.get(code);
	}

	public Vector getCodeBySignature(String signature) {
		if (signature2Codes_hmap.containsKey(signature)) {
			return (Vector) signature2Codes_hmap.get(signature);
		}
		return null;
	}

	public List assignScores(List matchedConcepts, String searchString) {
		boolean matchPropertyValue = true;
		return assignScores(matchedConcepts, searchString, matchPropertyValue);
	}

	public List assignScores(List matchedConcepts, String searchString, boolean matchPropertyValue) {
		LevenshteinDistance ld = new LevenshteinDistance(null);
		searchString = searchString.toLowerCase();
		for (int i=0; i<matchedConcepts.size(); i++) {
            MatchedConcept mc_i = (MatchedConcept) matchedConcepts.get(i);
            Integer score = null;
            if (matchPropertyValue) {
                score = ld.apply(searchString, mc_i.getPropertyValue().toLowerCase());
			} else {
                score = ld.apply(searchString, mc_i.getLabel().toLowerCase());
			}
            mc_i.setScore(Integer.valueOf(score));
		}
		return matchedConcepts;
	}

    public String getKey(int score) {
		String base = "000000";
		String score_str = new Integer(score).toString();
		int len = score_str.length();
		return base.substring(0, base.length()-len) + score_str;
	}

	public List sortMatchedConcepts(List matchedConcepts, String searchString) {
		matchedConcepts = assignScores(matchedConcepts, searchString, false);
		if (matchedConcepts == null) return null;
		if (matchedConcepts.size() <= 1) return matchedConcepts;
		Vector w = new Vector();
		for (int i=1; i<matchedConcepts.size(); i++) {
			MatchedConcept mc = (MatchedConcept) matchedConcepts.get(i);
			String key = "" + getKey(mc.getScore())  + "|" + mc.getScore()  + "|" + mc.getLabel() + "|" + mc.getCode() + "|" + mc.getPropertyName()
			   + "|" + mc.getPropertyValue();
			w.add(key);
		}

		matchedConcepts = new ArrayList();
		w = new SortUtils().quickSort(w);
		for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String score_str = (String) u.elementAt(1);
			int score = Integer.parseInt(score_str);
			String x_label = (String) u.elementAt(2);
			String x_code = (String) u.elementAt(3);
			String p_label = (String) u.elementAt(4);
			String p_value = (String) u.elementAt(5);
			MatchedConcept mc = new MatchedConcept(x_label, x_code, p_label, p_value, score);
			matchedConcepts.add(mc);
		}
		return matchedConcepts;

	}

	public void dumpMatchedConcept(MatchedConcept mc) {
		StringBuffer buf = new StringBuffer();
		 buf.append("\n" + mc.getLabel() + "\n\t" + mc.getCode()
		 + "\n\t" + mc.getPropertyName()
		 + "\n\t" + mc.getPropertyValue()
		 + "\n\t" + mc.getScore()
		 );
	    System.out.println(buf.toString());
	}

    public Vector tokenize(String str) {
		Vector v = new Vector();
		StringTokenizer st = new StringTokenizer(str);
		while (st.hasMoreTokens()) {
		    v.add(st.nextToken());
		}
		return v;
	}

	public String createSearchString(String str) {
		str = str.toLowerCase();
		Vector v = tokenize(str);
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<v.size(); i++) {
			String token = (String) v.elementAt(i);
            buf.append(token);
			if (i < v.size()-1) {
				buf.append(" AND ");
			}
		}
		return "'" + buf.toString() + "'" ;
	}

	public boolean match(String algorithm, String searchString, String p_value) {
		if (algorithm.compareTo(EXACT_MATCH) == 0) {
			if (p_value.compareToIgnoreCase(searchString) == 0) {
				return true;
			}
		} else if (algorithm.compareTo(STARTS_WITH) == 0) {
			if (p_value.startsWith(searchString)) {
				return true;
			}
		} else if (algorithm.compareTo(ENDS_WITH) == 0) {
			if (p_value.endsWith(searchString)) {
				return true;
			}
		} else {
			return true;
		}
		return false;
	}


	public Vector postProcess(Vector w, String searchTarget, String algorithm, String searchString) {
		searchString = searchString.toLowerCase();
        searchString = searchString.trim();
		Vector v = new Vector();

		for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String p_label = (String) u.elementAt(2);
			String p_value = (String) u.elementAt(0);
			p_value = p_value.toLowerCase();

			if (searchTarget.compareTo(NAMES) == 0) {
				if (PRESENTATION_LIST.contains(p_label)) {
					if (match(algorithm, searchString, p_value)) {
					    if (!v.contains(line)) {
							v.add(line);
						}
					}
				}
			} else if (searchTarget.compareTo(PROPERTIES) == 0) {
				if (!PRESENTATION_LIST.contains(p_label)) {
					if (match(algorithm, searchString, p_value)) {
					    if (!v.contains(line)) {
	                        v.add(line);
						}
					}
				}
			} else {
				boolean bool = match(algorithm, searchString, p_value);
				if (bool) {
					if (!v.contains(line)) {
						v.add(line);
					}
				}
			}
		}
		return v;
	}

	public static String marshalSearchResult(SearchResult sr) throws Exception {
		XStream xstream_xml = new XStream(new DomDriver());
        String xml = Constants.XML_DECLARATION + "\n" + xstream_xml.toXML(sr);
        return xml;
	}

    public void apply_text_match(StringBuffer buf, String param, String searchString, String algorithm) {
		searchString = searchString.toLowerCase();
		if (algorithm.compareTo(EXACT_MATCH) == 0) {
			buf.append("		FILTER (lcase(str(?" + param + ")) = \"" + searchString + "\")").append("\n");
		} else if (algorithm.compareTo(STARTS_WITH) == 0) {
			buf.append("		FILTER (regex(str(?"+ param +"),'^" + searchString + "','i'))").append("\n");
		} else if (algorithm.compareTo(ENDS_WITH) == 0) {
			buf.append("		FILTER (regex(str(?"+ param +"),'" + searchString + "^','i'))").append("\n");
		} else {
			searchString = searchString.replaceAll("%20", " ");
			searchString = SPARQLSearchUtils.createSearchString(searchString, algorithm);
			buf.append("(?"+ param +") <tag:stardog:api:property:textMatch> (" + searchString + ").").append("\n");
		}
	}

	public static String createSearchString(String term, String algorithm) {
		if (term == null) return null;
		String searchTerm = term;
        if (algorithm.equalsIgnoreCase(CONTAINS)){
			searchTerm = "*" + term + "*";
		} else if (algorithm.equalsIgnoreCase(EXACT_MATCH)){
			searchTerm =term;
		} else if (algorithm.equalsIgnoreCase(STARTS_WITH)){
			searchTerm = term + "*";
		} else if (algorithm.equalsIgnoreCase(ENDS_WITH)){
			searchTerm = "*" + term;
		} else if (algorithm.equalsIgnoreCase(PHRASE)){
			searchTerm = "\"" + term + "\"";
		} else if (algorithm.equalsIgnoreCase(FUZZY)){
			searchTerm = term + "~";
		} else if (algorithm.equalsIgnoreCase(BOOLEAN_AND)){
			String[] terms = searchTerm.split(" ");
			List list = Arrays.asList(terms);
			StringBuffer buf = new StringBuffer();
			for (int i=0; i<list.size(); i++) {
				String token = (String) list.get(i);
				buf = buf.append(token);
				if (i < list.size()-1) {
					buf.append(" AND ");
			    }
			}
			searchTerm = buf.toString();
		}
		searchTerm = escapeLuceneSpecialCharacters(searchTerm);
		return "'" + searchTerm + "'";
	}

	public static String escapeLuceneSpecialCharacters(String before) {
		String patternString = "([+:!~*?/\\-/{}\\[\\]\\(\\)\\^\\\"])";
		Pattern pattern = Pattern.compile(patternString);
		Matcher matcher = pattern.matcher(before);
		StringBuffer buf = new StringBuffer();
		while(matcher.find()) {
			matcher.appendReplacement(buf,
			before.substring(matcher.start(),matcher.start(1)) +
			"\\\\" + "\\\\" + matcher.group(1)
			+ before.substring(matcher.end(1),matcher.end()));
		}
		String after = matcher.appendTail(buf).toString();
		return after;
	}

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public SearchResult search(String named_graph, String propertyName, String searchString, String searchTarget, String algorithm) {
		long ms = System.currentTimeMillis();
		long ms0 = ms;
		Vector v = null;
		try {
			if (searchTarget.compareTo(ASSOCIATIONS) == 0) {
				v = searchAssociationTargets(named_graph, propertyName, searchString, algorithm);
			} else {
				v = searchAnnotationProperties(named_graph, propertyName, searchString, algorithm);
			}
		} catch (Exception ex) {
			System.out.println("search method throws exception???");
		}
		ms = System.currentTimeMillis();
		if (v == null) return null;
		String x_label = null;
		String x_code = null;
		String p_label = null;
		String p_value = null;
		String vs_code = null;

		SearchResult sr = new SearchResult();
		List matchedConcepts = new ArrayList();
		Vector w = new Vector();
		HashSet hset = new HashSet();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			line = line.trim();
			if (!hset.contains(line)) {
				Vector u = StringUtils.parseData(line, '|');
				x_label = (String) u.elementAt(0);
				x_code = (String) u.elementAt(1);
				p_label = (String) u.elementAt(2);
				p_value = (String) u.elementAt(3);
                if (searchTarget.compareTo(ASSOCIATIONS) == 0) {
					vs_code = (String) u.elementAt(4);
					p_value = p_value + " (" + vs_code + ")";
				}
			}
		}
		ms = System.currentTimeMillis();
		matchedConcepts = sortMatchedConcepts(matchedConcepts, searchString);
		sr.setMatchedConcepts(matchedConcepts);
		return sr;
	}

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//  named_graph, propertyName, searchString, EXACT_MATCH);
	public String construct_search_annotation_properties(String named_graph, String propertyName, String searchString, String algorithm) {
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX xml:<http://www.w3.org/XML/1998/namespace>").append("\n");
		buf.append("PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>").append("\n");
		buf.append("PREFIX owl:<http://www.w3.org/2002/07/owl#>").append("\n");
		buf.append("PREFIX owl2xml:<http://www.w3.org/2006/12/owl2-xml#>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>").append("\n");
		buf.append("PREFIX dc:<http://purl.org/dc/elements/1.1/>").append("\n");

		if (named_graph == null) {
			buf.append("SELECT distinct ?g ?x_label ?x_code ?y_label ?z").append("\n");
			buf.append("{").append("\n");
			buf.append("    graph ?g").append("\n");
		} else {
			buf.append("SELECT distinct ?x_label ?x_code ?y_label ?z").append("\n");
			buf.append("{").append("\n");
			buf.append("    graph <" + named_graph + ">").append("\n");
		}

		buf.append("    {").append("\n");
		buf.append("	    ?x a owl:Class .").append("\n");
		buf.append("	    ?x rdfs:label ?x_label .").append("\n");
		buf.append("	    ?y a owl:AnnotationProperty .").append("\n");
		buf.append("	    ?x ?y ?z .").append("\n");
		buf.append("	    ?y rdfs:label ?y_label .").append("\n");
		if (propertyName != null) {
			buf.append("	    ?y rdfs:label " + "\"" + propertyName + "\"^^xsd:string .").append("\n");
		}

		apply_text_match(buf, "x_label", searchString, algorithm);
		//buf.append("	    (?x_label) <tag:stardog:api:property:textMatch> (" + searchString + ").").append("\n");

		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}


	public Vector searchAnnotationProperties(String named_graph, String propertyName, String searchString, String algorithm) {
		String query = construct_search_annotation_properties(named_graph, propertyName, searchString, algorithm);
		Vector v = null;
		try {
			v = executeQuery(query);
		} catch (Exception ex) {
			System.out.println("searchAnnotationProperties throws exception???");
		}

		if (v != null && v.size() > 0) {
			v = new ParserUtils().getResponseValues(v);
			String searchTarget = NAMES;
			if (!PRESENTATION_LIST.contains(propertyName)) {
					searchTarget = PROPERTIES;
			}
			v = postProcess(v, searchTarget, algorithm, searchString);
		}
		return v;
	}

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public String construct_search_association_targets(String named_graph, String associationName, String searchString, String algorithm) {
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX xml:<http://www.w3.org/XML/1998/namespace>").append("\n");
		buf.append("PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>").append("\n");
		buf.append("PREFIX owl:<http://www.w3.org/2002/07/owl#>").append("\n");
		buf.append("PREFIX owl2xml:<http://www.w3.org/2006/12/owl2-xml#>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>").append("\n");
		buf.append("PREFIX dc:<http://purl.org/dc/elements/1.1/>").append("\n");

		if (named_graph == null) {
			buf.append("SELECT distinct ?g ?x_label ?x ?y_label ?z_label ?z").append("\n");
			buf.append("{").append("\n");
			buf.append("    graph ?g").append("\n");
		} else {
			buf.append("SELECT distinct ?x_label ?x ?y_label ?z_label ?z").append("\n");
			buf.append("{").append("\n");
			buf.append("    graph <" + named_graph + ">").append("\n");
		}

		buf.append("    {").append("\n");
		buf.append("	    ?x rdfs:label ?x_label .").append("\n");
		buf.append("	    ?x ?y ?z .").append("\n");
		buf.append("	    ?z rdfs:label ?z_label .").append("\n");
		buf.append("	    ?y rdfs:label ?y_label .").append("\n");
		buf.append("	    ?y rdfs:label " + "\"" + associationName + "\"^^xsd:string .").append("\n");

		apply_text_match(buf, "x_label", searchString, algorithm);
		//buf.append("	    (?x_label) <tag:stardog:api:property:textMatch> (" + searchString + ").").append("\n");

		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}


	public Vector searchAssociationTargets(String named_graph, String associationName, String searchString, String algorithm) {
		String query = construct_search_association_targets(named_graph, associationName, searchString, algorithm);
		Vector v = executeQuery(query);
		v = new ParserUtils().getResponseValues(v);
		if (v.size() > 0) {
			String searchTarget = ASSOCIATIONS;
			v = postProcess(v, searchTarget, algorithm, searchString);
	    }
		return v;
	}

	public String construct_search_by_uri(String named_graph, String uri) {
		StringBuffer buf = new StringBuffer();

		if (named_graph == null) {
			buf.append("SELECT distinct ?g ?x ?x_label").append("\n");
			buf.append("{").append("\n");
			buf.append("    graph ?g").append("\n");
		} else {
			buf.append("SELECT distinct ?x ?x_label").append("\n");
			buf.append("{").append("\n");
			buf.append("    graph <" + named_graph + ">").append("\n");
		}

		buf.append("    {").append("\n");
		buf.append("	    ?x a owl:Class .").append("\n");
		buf.append("	    ?x rdfs:label ?x_label .").append("\n");
		buf.append("    }").append("\n");
		buf.append("    FILTER (str(?x) = \"" + uri + "\"^^xsd:string)").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector searchByURI(String named_graph, String uri) {
		Vector w = executeQuery(construct_search_by_uri(named_graph, uri));
		w = new ParserUtils().getResponseValues(w);
		return w;
	}

public String construct_get_property_values(String named_graph, String propertyName) {
	String prefixes = getPrefixes();
	StringBuffer buf = new StringBuffer();
	buf.append(prefixes).append("\n");
	buf.append("select distinct ?x_label ?x_code ?p_label ?p_value").append("\n");
	buf.append("from <" + named_graph + ">").append("\n");
	buf.append("where  { ").append("\n");
	buf.append("                ?x a owl:Class .").append("\n");
	buf.append("                ?x :NHC0 ?x_code .").append("\n");
	buf.append("                ?x rdfs:label ?x_label .").append("\n");
	buf.append("").append("\n");
	buf.append("                ?p :NHC0 ?p_code .").append("\n");
	buf.append("                ?p rdfs:label ?p_label .").append("\n");
	buf.append("                ?p rdfs:label \"" + propertyName + "\"^^xsd:string .").append("\n");
	buf.append("                ?p a owl:AnnotationProperty .").append("\n");
	buf.append("                ").append("\n");
	buf.append("                ?x ?p ?p_value .").append("\n");
	buf.append("}").append("\n");
	return buf.toString();
}


public Vector getPropertyValues(String named_graph, String propertyName) {
	String query = construct_get_property_values(named_graph, propertyName);
	Vector w = executeQuery(query);
	w = new ParserUtils().getResponseValues(w);
	return w;
}


	public String construct_search_by_name(String named_graph, String searchString, String algorithm) {
        searchString = searchString.toLowerCase();
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);

		buf.append("select distinct ?x_label ?x_code ?a_target ?q1_value ?q2_value ").append("\n");
		buf.append("from <" + named_graph + ">").append("\n");

        buf.append("where  { ").append("\n");
        buf.append("                ?x a owl:Class .").append("\n");
        buf.append("                ?x :NHC0 ?x_code .").append("\n");
        buf.append("                ?x rdfs:label ?x_label .").append("\n");
        buf.append("").append("\n");
        buf.append("                ?a a owl:Axiom .").append("\n");
        buf.append("                ?a owl:annotatedSource ?x .").append("\n");
        buf.append("                ?a owl:annotatedProperty ?p .").append("\n");
        buf.append("                ?a owl:annotatedTarget ?a_target .").append("\n");
        buf.append("                ?p rdfs:label ?p_label .").append("\n");
        buf.append("                ?p rdfs:label \"FULL_SYN\"^^xsd:string .").append("\n");
        buf.append("                ").append("\n");

        buf.append("                ?a ?q1 ?q1_value .").append("\n");
        buf.append("                ?q1 rdfs:label ?q1_label .").append("\n");
        buf.append("                ?q1 rdfs:label \"Term Source\"^^xsd:string .").append("\n");

        buf.append("                ?a ?q2 ?q2_value .").append("\n");
        buf.append("                ?q2 rdfs:label ?q2_label .").append("\n");
        buf.append("                ?q2 rdfs:label \"Term Type\"^^xsd:string .").append("\n");
        buf.append("                ").append("\n");
        buf.append("                FILTER (lcase(?a_target) = \"" + searchString + "\"^^xsd:string)    ").append("\n");
        buf.append("").append("\n");
        buf.append("").append("\n");
        buf.append("}").append("\n");


	return buf.toString();
}



	public Vector searchByName(String named_graph, String searchString, String algorithm) {
		String query = construct_search_by_name(named_graph, searchString, algorithm);
		Vector w = executeQuery(query);
		if (w != null && w.size() > 0) {
			w = new ParserUtils().getResponseValues(w);
		}
		return w;
	}
/////////////////////////////////////////////////////////////////////////////////////////////////////
	public String construct_search_by_name_data(String named_graph) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);

		buf.append("select distinct ?x_label ?x_code ?a_target ?q1_value ?q2_value ").append("\n");
		buf.append("from <" + named_graph + ">").append("\n");

        buf.append("where  { ").append("\n");
        buf.append("                ?x a owl:Class .").append("\n");
        buf.append("                ?x :NHC0 ?x_code .").append("\n");
        buf.append("                ?x rdfs:label ?x_label .").append("\n");
        buf.append("").append("\n");
        buf.append("                ?a a owl:Axiom .").append("\n");
        buf.append("                ?a owl:annotatedSource ?x .").append("\n");
        buf.append("                ?a owl:annotatedProperty ?p .").append("\n");
        buf.append("                ?a owl:annotatedTarget ?a_target .").append("\n");
        buf.append("                ?p rdfs:label ?p_label .").append("\n");
        buf.append("                ?p rdfs:label \"FULL_SYN\"^^xsd:string .").append("\n");
        buf.append("                ").append("\n");

        buf.append("                ?a ?q2 ?q2_value .").append("\n");
        buf.append("                ?q2 rdfs:label ?q2_label .").append("\n");
        buf.append("                ?q2 rdfs:label \"Term Type\"^^xsd:string .").append("\n");

        buf.append("                ?a ?q1 ?q1_value .").append("\n");
        buf.append("                ?q1 rdfs:label ?q1_label .").append("\n");
        buf.append("                ?q1 rdfs:label \"Term Source\"^^xsd:string .").append("\n");

        buf.append("                ").append("\n");
        buf.append("").append("\n");
        buf.append("").append("\n");
        buf.append("}").append("\n");


	return buf.toString();
}

	public Vector getSearchByNameData(String named_graph) {
		String query = construct_search_by_name_data(named_graph);
		Vector w = executeQuery(query);
		if (w != null && w.size() > 0) {
			w = new ParserUtils().getResponseValues(w);
		}
		return w;
	}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public String construct_search_by_property(String named_graph, String property_name, String property_value, String algorithm) {
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX xml:<http://www.w3.org/XML/1998/namespace>").append("\n");
		buf.append("PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>").append("\n");
        buf.append("PREFIX :<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>").append("\n");
		buf.append("PREFIX owl:<http://www.w3.org/2002/07/owl#>").append("\n");
		buf.append("PREFIX owl2xml:<http://www.w3.org/2006/12/owl2-xml#>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>").append("\n");
		buf.append("PREFIX dc:<http://purl.org/dc/elements/1.1/>").append("\n");
		buf.append("").append("\n");
		buf.append("SELECT distinct ?x_label ?x_code ?z").append("\n");
		buf.append("{").append("\n");
		buf.append("	graph <" + named_graph + "> {").append("\n");
		buf.append("		?x a owl:Class .").append("\n");
		buf.append("		?x rdfs:label ?x_label .").append("\n");
		buf.append("		?x :NHC0 ?x_code .          ").append("\n");
		buf.append("		?x ?y ?z .").append("\n");
		buf.append("		?y rdfs:label \"" + property_name + "\"^^<http://www.w3.org/2001/XMLSchema#string> .").append("\n");
		apply_text_match(buf, "z", property_value, algorithm);
		buf.append("	}").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector searchByProperty(String named_graph, String property_name, String property_value, String algorithm) {
		Vector w = executeQuery(construct_search_by_property(named_graph, property_name, property_value, algorithm));
		if (w != null && w.size() > 0) {
			w = new ParserUtils().getResponseValues(w);
		}
		return w;
	}


	public String construct_get_contains(String named_graph, String term, String boolean_operator) {
		term = term.trim();
		String term_lc = term.toLowerCase();
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("").append("\n");
		buf.append("SELECT distinct ?x_code ?x_label").append("\n");
		buf.append("{").append("\n");
		buf.append("	graph <" + named_graph + "> ").append("\n");
		buf.append("	{").append("\n");
		buf.append("		{").append("\n");
		buf.append("			?x a owl:Class .").append("\n");
		buf.append("			?x :NHC0 ?x_code .").append("\n");
		buf.append("			?x rdfs:label ?x_label .").append("\n");
		buf.append("").append("\n");
		buf.append("			FILTER (").append("\n");
		StringBuffer contains_buf = new StringBuffer();
		contains_buf.append("\t\t\t");

		String[] tokens = term_lc.split(" ");

		for (int i=0; i<tokens.length; i++) {
			String token = tokens[i];
			contains_buf.append("contains(lcase(?x_label),\"" + token + "\")");
			if (i<tokens.length-1) {
				if (boolean_operator.compareToIgnoreCase("AND") == 0) {
					contains_buf.append(" && ");
				} else {
					contains_buf.append(" || ");
				}
			}
		}
		String contains = contains_buf.toString();
		buf.append(contains).append("\n");
		buf.append("			)").append("\n");
		buf.append("		}").append("\n");
		buf.append("").append("\n");
		buf.append("	}").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}


	public Vector containsSearch(String named_graph, String term, String boolean_operator) {
		String query = construct_get_contains(named_graph, term, boolean_operator);
		Vector v = executeQuery(query);
		if (v == null) return null;
		if (v.size() == 0) return v;
		v = new ParserUtils().getResponseValues(v);
		return new SortUtils().quickSort(v);
	}

	public Vector containsSearch(String named_graph, String term) {
		String boolean_operator = "AND";
		return containsSearch(named_graph, term, boolean_operator);
	}


	public String construct_get_exactMatch(String named_graph, Vector propertyNames, String term) {
        String prefixes = getPrefixes();
        term = term.toLowerCase();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        buf.append("").append("\n");
        buf.append("SELECT distinct ?x_code ?x_label ?z_target").append("\n");
        buf.append("{").append("\n");
        buf.append("    graph <http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl> {").append("\n");
        buf.append("            {").append("\n");
        buf.append("                ?x a owl:Class .").append("\n");
        buf.append("                ?x :NHC0 ?x_code .").append("\n");
        buf.append("                ?x rdfs:label ?x_label .").append("\n");
        buf.append("").append("\n");
        buf.append("                ?z_axiom a owl:Axiom  .").append("\n");
        buf.append("                ?z_axiom owl:annotatedSource ?x .").append("\n");
        buf.append("                ?z_axiom owl:annotatedProperty ?p .").append("\n");
        buf.append("                ?p rdfs:label ?p_label .").append("\n");
        buf.append("                ?p rdfs:label \"FULL_SYN\"^^xsd:string .").append("\n");
        buf.append("                ?z_axiom owl:annotatedTarget ?z_target .").append("\n");
		buf.append("                FILTER (").append("\n");
		buf.append("                	lcase(?z_target) = \"" + term + "\"^^xsd:string ").append("\n");
		buf.append("                )").append("\n");
        buf.append("            }").append("\n");

        for (int i=0; i<propertyNames.size(); i++) {
			String propertyName = (String) propertyNames.elementAt(i);
			buf.append("            UNION ").append("\n");
			buf.append("            {").append("\n");
			buf.append("                ?x a owl:Class .").append("\n");
			buf.append("                ?x :NHC0 ?x_code .").append("\n");
			buf.append("                ?x rdfs:label ?x_label .").append("\n");
			buf.append("").append("\n");
			buf.append("                ?x ?p1 ?z_target .").append("\n");
			buf.append("                ?p1 rdfs:label ?p1_label .").append("\n");
			buf.append("                ?p1 rdfs:label \"" + propertyName + "\"^^xsd:string .").append("\n");
			buf.append("                FILTER (").append("\n");
			buf.append("                	lcase(?z_target) = \"" + term + "\"^^xsd:string ").append("\n");
			buf.append("                )").append("\n");
			buf.append("").append("\n");
			buf.append("            }").append("\n");
	    }

        buf.append("    }").append("\n");
        buf.append("}").append("\n");
        return buf.toString();
    }



	public Vector getExactMatch(String named_graph, Vector propertyNames, String term) {
        String query = construct_get_exactMatch(named_graph, propertyNames, term);
        Vector v = executeQuery(query);
        if (v == null) return null;
        if (v.size() == 0) return v;
        v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
	}

///////////////////////////////////////////////////////////////////////////////////////////////


	public static String[] discarded_phrase_values = new String[] {
			"ill-defined",
			"not elsewhere classified",
			"not otherwise classified",
			"not specified",
			"Other specified",
			"Other types of",
			"primary site unknown, so stated",
			"Unknown and unspecified causes of"};

	public static String[] specialChars = new String[] {"{", "}", "(", ")", "'", ":", ";", ".", "," ,"\""};
	public static HashSet specialCharsHashSet = toHashSet(specialChars);



	public static Vector toKeywords(String term) {
		HashSet hset = new HashSet();
		term = term.trim();
		term = term.replace("\"", "");
		term = term.trim();
		if (term.length() > 0) {
			term = term.toLowerCase();
			term = replace(term, '/', ' ');
			String[] tokens = term.split(" ");
			for (int j=0; j<tokens.length; j++) {
				String token = tokens[j];
				if (token.length() > 1) {
					char firstChar = token.charAt(0);
					char lastChar = token.charAt(token.length()-1);
					if (specialCharsHashSet.contains("" + firstChar) &&
						specialCharsHashSet.contains("" + lastChar)) {
						token = token.substring(1, token.length()-1);
					} else if (specialCharsHashSet.contains("" + lastChar)) {
						token = token.substring(0, token.length()-1);
					} else if (specialCharsHashSet.contains("" + firstChar)) {
						token = token.substring(1, token.length());
					}
					if (token.length() > 0) {
						if (!hset.contains(token)) {
							hset.add(token);
						}
					}
				} else if (token.length() == 1) {
					if (!hset.contains(token)) {
						hset.add(token);
					}
				}
			}
		}
		return new gov.nih.nci.evs.restapi.util.SortUtils().quickSort(toVector(hset));
	}

	public static Vector toVector(String[] a) {
		Vector w = new Vector();
		for (int i=0; i<a.length; i++) {
			String t = a[i];
			w.add(t);
		}
		return w;
	}

    public static Vector toVector(HashSet hset) {
		Vector v = new Vector();
		Iterator it = hset.iterator();
		while (it.hasNext()) {
			v.add((String) it.next());
		}
		return v;
	}

	public static String replace(String s, char replace, char by) {
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<s.length(); i++) {
			char c = s.charAt(i);
			if (c == replace) {// '\u002F' ) {
				c = by;
			}
			buf.append(c);
		}
		return buf.toString();
	}

	public static Vector remove_duplicates(Vector w) {
		Vector v = new Vector();
		String t = (String) w.elementAt(0);
		v.add(t);
		for (int i=1; i<w.size(); i++) {
			String s = (String) w.elementAt(i);
			if (s.compareTo(t) != 0) {
				v.add(s);
				t = s;
			}
		}
		return v;
	}

	public static Vector toKeywords(Vector term_data) {
		Vector w = new Vector();
        for (int i=0; i<term_data.size(); i++) {
			String line = (String) term_data.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String term = (String) u.elementAt(2);
			Vector v = toKeywords(term);
			w.addAll(v);
		}
		w = new gov.nih.nci.evs.restapi.util.SortUtils().quickSort(w);
		w = remove_duplicates(w);
		return w;
	}

    public static int countNumberOfMatchedConcepts(Vector v) {
		int n = 0;
		HashSet hset = new HashSet();
		if (v == null) return 0;
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(t, '|');
			String code = (String) u.elementAt(1);
			if (!hset.contains(code)) {
				hset.add(code);
			}
		}
		return hset.size();
	}
///////////////////////////////////////////////////////////////////////////////////////////////

	public String construct_search(String named_graph, String term, String algorithm) {
		term = term.toLowerCase();
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("select distinct ?x1_label ?x1_code ").append("\n");
		buf.append("from <http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl>").append("\n");
		buf.append("where  { ").append("\n");
		buf.append("                    ?x1 a owl:Class .").append("\n");
		buf.append("                    ?x1 :NHC0 ?x1_code .").append("\n");
		buf.append("                    ?x1 rdfs:label ?x1_label .").append("\n");
		buf.append(" ").append("\n");
		buf.append("                ?a1 a owl:Axiom .").append("\n");
		buf.append("                ?a1 owl:annotatedSource ?x1 .").append("\n");
		buf.append("                ?a1 owl:annotatedProperty ?p2 .").append("\n");
		buf.append("                ").append("\n");
		buf.append("                ?a1 owl:annotatedTarget ?a2_target .").append("\n");
		buf.append("                ?p2 :NHC0 \"P90\"^^xsd:string .").append("\n");
		buf.append("                ").append("\n");
		if (algorithm.compareTo(STARTS_WITH) == 0) {
			buf.append("                FILTER( REGEX( lcase(?a2_target), \"^" + term + "\" ) ) .").append("\n");
		} else if (algorithm.compareTo(ENDS_WITH) == 0) {
			buf.append("                FILTER( strends( lcase(?a2_target), \"" + term + "\" ) ) .").append("\n");
		} else if (algorithm.compareTo(EXACT_MATCH) == 0) {
			buf.append("                FILTER( lcase(?a2_target) = \"" + term + "\" )  .").append("\n");
		} else if (algorithm.compareTo(CONTAINS) == 0) {
			buf.append("                FILTER( contains( lcase(?a2_target), \"" + term + "\" ) ) .").append("\n");
		} else if (algorithm.compareTo(BOOLEAN_AND) == 0) {

			String[] terms = term.split(" ");
			List list = Arrays.asList(terms);
			buf.append("FILTER(");
			for (int i=0; i<list.size(); i++) {
				String token = (String) list.get(i);
				token = token.toLowerCase();
				buf.append("contains( lcase(?a2_target), \"" + token + "\" )");
				if (i < list.size()-1) {
					buf.append(" && ");
				}
			}
			buf.append(") .").append("\n");
		} else if (algorithm.compareTo(BOOLEAN_OR) == 0) {

			String[] terms = term.split(" ");
			List list = Arrays.asList(terms);
			buf.append("FILTER(");
			for (int i=0; i<list.size(); i++) {
				String token = (String) list.get(i);
				token = token.toLowerCase();
				buf.append("contains( lcase(?a2_target), \"" + token + "\" )");
				if (i < list.size()-1) {
					buf.append(" || ");
				}
			}
			buf.append(") .").append("\n");
		}
		buf.append("} ").append("\n");
		return buf.toString();
	}

	public Vector search(String named_graph, String term, String algorithm) {
		String query = construct_search(named_graph, term, algorithm);
		Vector v = executeQuery(query);
		if (v == null) return null;
		if (v.size() == 0) return v;
		v = new ParserUtils().getResponseValues(v);
		return new SortUtils().quickSort(v);
	}

	public static void main(String[] args) {
		long ms = System.currentTimeMillis();
		String serviceUrl = args[0];
		String namedGraph = args[1];
		String username = args[2];
		String password = args[3];
		String verbatimfile = args[4];
		String codingScheme = "NCI_Thesaurus";
		MetadataUtils test = new MetadataUtils(serviceUrl, username, password);
		String version = test.getLatestVersion(codingScheme);
		String named_graph = test.getNamedGraph(codingScheme);
		SPARQLSearchUtils searchUtils = new SPARQLSearchUtils(serviceUrl, named_graph, username, password);
        Vector verbatims = Utils.readFile(verbatimfile);
        Vector w = new Vector();
		int k = 0;

	    int num_matched = 0;
	    Vector no_match_verbatim_vec = new Vector();
		for (int i=0; i<verbatims.size(); i++) {
			boolean matched = true;
			k++;
			String verbatim = (String) verbatims.elementAt(i);
			Vector u = StringUtils.parseData(verbatim);
			String term = (String) u.elementAt(0);
			String code = (String) u.elementAt(1);

			System.out.println("(" + k + ") " + term + " (" + code + ")");
			Vector v = searchUtils.mapTo(term);
			if (v != null && v.size() > 0) {
				Vector v2 = new Vector();
				for (int j=0; j<v.size(); j++) {
					String line = (String) v.elementAt(j);
					w.add(verbatim + "|" + line);
				}
			} else{
				w.add(verbatim + "||||||No match");
				matched = false;
				no_match_verbatim_vec.add(verbatim);
			}
			if (matched) {
				num_matched++;
			}
		}
		Utils.saveToFile("results_" + verbatimfile, w);
		Utils.saveToFile("nomatches_" + verbatimfile, no_match_verbatim_vec);

		System.out.println("Number of terms: " + verbatims.size());
		System.out.println("Number of terms matched: " + num_matched);
		int no_matches = verbatims.size() - num_matched;
		System.out.println("Number of terms NOT matched: " + no_matches);

	}

}
