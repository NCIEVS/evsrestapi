package gov.nih.nci.evs.restapi.util;

import ca.rmen.porterstemmer.*;
import java.util.*;
import java.util.Locale;


public class LexicalMatching {

    public static HashSet NON_FILLERS = null;
    public static HashMap normalizedTerm2CodeHashmap = null;
    public static HashMap code2LabelHashMap = null;

/*
	public static String[] FILLERS = new String[] {
			"a",    "about",        "again",        "all",  "almost",
			"also", "although",     "always",       "among",        "an",
			"and",  "another",      "any",  "are",  "as",
			"at",   "be",   "because",      "been", "before",
			"being",        "between",      "both", "but",  "by",
			"can",  "could",        "did",  "do",   "does",
			"done", "due",  "during",       "each", "either",
			"enough",       "especially",   "etc",  "external",     "for",
			"found",        "from", "further",      "had",  "has",
			"have", "having",       "here", "how",  "however",
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
*/

	public static String[] FILLERS = new String[] {
        "about","again","also","although","among",
        "an","and","are","as","at",
        "be","because","been","before","being",
        "between","both","but","could","did",
        "do","does","done","during","either",
        "enough","especially","etc","found","from",
        "further","had","have","having","here",
        "however","in","internal","into","it",
        "its","itself","just","made","mainly",
        "make","may","might","mostly","must",
        "nearly","obtained","on","our","parts",
        "perhaps","pmid","rather","really","regarding",
        "secondary","seem","seen","several","should",
        "show","showed","shown","shows","significantly",
        "since","so","some","specification","specified",
        "such","their","theirs","them","then",
        "there","therefore","these","they","this",
        "those","through","thus","upon","used",
        "using","various","we","were","when",
        "which","while","within","without","would"};


    public static HashSet filler_set = null;
    static {
		filler_set = new HashSet();
		for (int i=0; i<FILLERS.length; i++) {
			String filler = (String) FILLERS[i];
			filler_set.add(filler);
		}
	}

	public static boolean isFiller(String word) {
		word = word.toLowerCase();
		return filler_set.contains(word);
	}

    public static HashSet identifyNonFillers(String filename) {
		HashSet hset = new HashSet();
		Vector v = Utils.readFile(filename);
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String term = (String) u.elementAt(3);
			String code = (String) u.elementAt(1);
			try {
				String t = getNormalizedForm(term);
				if (t.length() == 0) {
					Vector u2 = StringUtils.parseData(line, ' ');
					for (int k=0; k<u2.size(); k++) {
						String s = (String) u2.elementAt(k);
						s = s.toLowerCase();
						if (!hset.contains(s)) {
							hset.add(s);
						}
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		return hset;
	}

    static PorterStemmer stemmer = new PorterStemmer();

    static {
		//NON_FILLERS = identifyNonFillers("FULL_SYN.txt");
		//String t = resetFillers();
		//System.out.println(t);

		normalizedTerm2CodeHashmap = load("FULL_SYN.txt");
		System.out.println(normalizedTerm2CodeHashmap.keySet().size());

	}

	private static String resetFillers() {
        StringBuffer buf = new StringBuffer();
        buf.append("\t").append("public static String[] FILLERS = new String[] {").append("\n");
        StringBuffer b = new StringBuffer();
        String t = null;
        int knt = 0;
        for (int i=0; i<FILLERS.length; i++) {
			String word = (String) FILLERS[i];
			if (!NON_FILLERS.contains(word)) {
				b.append("\"").append(word).append("\",");
				knt++;
				if (knt == 5) {
					t = b.toString();
					buf.append("\t\t").append(t).append("\n");
					b = new StringBuffer();
					knt = 0;
				}
			}
		}
		t = b.toString();
		if (t.length() > 0) {
			buf.append("\t\t").append(t);
		}
		t = buf.toString();
		t = t.substring(0, t.length()-2);
		t = t + "};";
		return t;
	}

    public static String getNormalizedForm(String term) {
		Vector w = new Vector();
		String s = "";
		term = term.replace("-", " ");
		term = term.replace(",", " ");
		Vector u = StringUtils.parseData(term, ' ');
		for (int j=0; j<u.size(); j++) {
			String word = (String) u.elementAt(j);
			if (word.length() > 0 && !isFiller(word)) {
				word = word.trim();
				String s0 = stemmer.stemWord(word);
				w.add(s0);
			}
		}
		if (w.size() > 0) {
			w = new SortUtils().quickSort(w);
			StringBuffer buf = new StringBuffer();
			for (int k=0; k<w.size(); k++) {
				String t = (String) w.elementAt(k);
				buf.append(t).append("$");
			}
			s = buf.toString();
			s = s.substring(0, s.length()-1);
		}
		return s;
	}

    public static HashMap load(String filename) {
		code2LabelHashMap = new HashMap();
		HashMap hmap = new HashMap();
		Vector v = Utils.readFile(filename);
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String term = (String) u.elementAt(3);
			String code = (String) u.elementAt(1);
			String label = (String) u.elementAt(0);
			code2LabelHashMap.put(code, label);
			try {
				String t = getNormalizedForm(term);
				if (t.length() > 0) {
					Vector w = new Vector();
					if (hmap.containsKey(t)) {
						w = (Vector) hmap.get(t);
					}
					if (!w.contains(code)) {
						w.add(code);
					}
					hmap.put(t, w);
				}
			} catch (Exception ex) {
				System.out.println("Line: " + line);
			}
		}
		return hmap;
	}

	public static Vector lexialMatching(String vbt) {
		String t = getNormalizedForm(vbt);
		if (normalizedTerm2CodeHashmap.containsKey(t)) {
			return (Vector) normalizedTerm2CodeHashmap.get(t);
		}
		return null;
	}

	public static void dumpVector(String label, Vector codes) {
		System.out.println(label);
		for (int i=0; i<codes.size(); i++) {
			String code = (String) codes.elementAt(i);
			String name = (String) code2LabelHashMap.get(code);
			System.out.println("\t" + name + " (" + code + ")");
		}
	}

    public static void test() {
		String vbt = "pM0 Uterine therefore Carcinoma TNM Finding internal";
		Vector w = lexialMatching(vbt);
		dumpVector(vbt, w);

		vbt = "Uterine therefore carcinoma finding pM0 internal TNM";
		w = lexialMatching(vbt);
		dumpVector(vbt, w);

		vbt = "Trial Arms Supplement Qualifier Dataset";
		w = lexialMatching(vbt);
		dumpVector(vbt, w);

	}

    public static void main(String[] args) {
		long ms = System.currentTimeMillis();
		test();
        System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}

}