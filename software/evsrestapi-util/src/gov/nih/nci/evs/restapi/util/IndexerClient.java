package gov.nih.nci.evs.restapi.util;
import java.io.*;
import java.util.*;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class IndexerClient {
	String serviceUrl = null;
	String namedGraph = null;
	String username = null;
	String password = null;
    private SPARQLSearchUtils searchUtils = null;
    private IndexUtils indexUtils = null;
	private int THRESHOLD = 100;

	public IndexerClient(String serviceUrl, String namedGraph, String username, String password) {
		this.serviceUrl = serviceUrl;
		this.namedGraph = namedGraph;
        searchUtils = new SPARQLSearchUtils(serviceUrl, namedGraph, username, password);
        indexUtils = new IndexUtils(serviceUrl, namedGraph, username, password);
	}

	public void set_THRESHOLD(int threshold) {
		this.THRESHOLD = threshold;
	}

	public void dumpHashMap(String label, Vector keys, HashMap hmap) {
		System.out.println(label);
		for (int i=0; i<keys.size(); i++) {
			String key = (String) keys.elementAt(i);
			Vector values = (Vector) hmap.get(key);
			Utils.dumpVector(key, values);
		}
	}

	public HashMap run(String vbtfile) {
		Vector w = Utils.readFile(vbtfile);
		return run(w, "unmatched.txt");
	}

	public HashMap run(Vector vbt_vec, String unmatched_file) {
		Vector unmatched = new Vector();
		HashMap hmap = new HashMap();
		for (int i=0; i<vbt_vec.size(); i++) {
			String term = (String) vbt_vec.elementAt(i);
			Vector v = search(namedGraph, term);
			if (v == null || v.size() > THRESHOLD) {
				unmatched.add(term);
			}
			hmap.put(term, v);
		}
		Utils.saveToFile(unmatched_file, unmatched);
		return hmap;
	}

	public Vector search(String named_graph, String term) {
		term = term.trim();
		if (term.length() == 0) return null;
		Vector w = searchUtils.search(named_graph, term, SPARQLSearchUtils.EXACT_MATCH);
		if (w != null && w.size() > 0) return w;
		w = getCodeBySignature(term);
		if (w != null && w.size() > 0) return w;
		w = indexUtils.indexNarrative(term);
		if (w != null && w.size() > 0) return w;
		return null;
	}

	public Vector getCodeBySignature(String term) {
		String signature = searchUtils.getSignature(term);
		Vector v = searchUtils.getCodeBySignature(signature);
		if (v == null) {
			return null;
		}
		Vector w1 = new Vector();
		if (v == null || v.size() == 0) return w1;
		for (int i=0; i<v.size(); i++) {
			String code = (String) v.elementAt(i);
			String label = searchUtils.get_label(code);
			w1.add(label + "|" + code);
		}
		return new SortUtils().quickSort(w1);
	}
}
