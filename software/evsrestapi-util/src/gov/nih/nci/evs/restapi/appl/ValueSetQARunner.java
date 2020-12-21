package gov.nih.nci.evs.restapi.util;

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

public class ValueSetQARunner {
	String serviceUrl = null;
	String namedGraph = null;
	String username = null;
	String password = null;
    ValueSetQA valueSetQA = null;
	OWLSPARQLUtils owlSPARQLUtils = null;
	HashMap conditionHashMap = null;

	public ValueSetQARunner(String serviceUrl, String namedGraph, String username, String password) {
		this.serviceUrl = serviceUrl;
		this.namedGraph = namedGraph;
		this.username = username;
		this.password = password;
		this.owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, username, password);
		this.owlSPARQLUtils.set_named_graph(namedGraph);
		this.valueSetQA = new ValueSetQA(serviceUrl, namedGraph, username, password);
	}

    public void loadConditionHashMap(String filename) {
		conditionHashMap = new HashMap();
		Vector w = Utils.readFile(filename);
		String key = null;
		Vector values = null;
		for (int i=0; i<w.size(); i++) {
			String t = (String) w.elementAt(i);
			if (t.indexOf("$") != -1) {
				if (key != null) {
					conditionHashMap.put(key, values);
				}
				key = t;
				values = new Vector();
			} else {
				values.add(t);
			}
		}
		conditionHashMap.put(key, values);
	}

    public void dumpConditionHashMap() {
		if (conditionHashMap == null) return;
		Iterator it = conditionHashMap.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			Vector values = (Vector) conditionHashMap.get(key);
			Vector u = StringUtils.parseData(key, '$');
			String headerConceptCode = (String) u.elementAt(0);
			boolean boolval = false;
			if(u.size() == 2) {
				String boolstr = (String) u.elementAt(1);
				Boolean boolObj = Boolean.parseBoolean(boolstr);
				boolval = Boolean.valueOf(boolObj);
			}
			System.out.println("\nheaderConceptCode: " + headerConceptCode);
			System.out.println("boolval: " + boolval);
			Utils.dumpVector(key, values);
		}
	}

	public void run() {
		if (conditionHashMap == null) return;
		String outputfile = "ValuSetQA_" + StringUtils.getToday() + ".log";
		Vector warning_vec = new Vector();
		Iterator it = conditionHashMap.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			Vector values = (Vector) conditionHashMap.get(key);
			Vector u = StringUtils.parseData(key, '$');
			String headerConceptCode = (String) u.elementAt(0);
			boolean boolval = false;
			if(u.size() == 2) {
				String boolstr = (String) u.elementAt(1);
				Boolean boolObj = Boolean.parseBoolean(boolstr);
				boolval = Boolean.valueOf(boolObj);
			}
			valueSetQA.setCheckOutBoundConceptInSubset(boolval);
			valueSetQA.run(values);
			Vector v = new Vector();
			v.add(headerConceptCode);
			v.addAll(valueSetQA.getWarnings());
			v.addAll(valueSetQA.getMissings());
			warning_vec.addAll(v);
		}
		Utils.saveToFile(outputfile, warning_vec);
	}

	public static void main(String[] args) {
		long ms = System.currentTimeMillis();
		String serviceUrl = args[0];
		String namedGraph = args[1];
		String username = args[2];
		String password = args[3];
		String datafile = args[4];
		ValueSetQARunner test = new ValueSetQARunner(serviceUrl, namedGraph, username, password);
		test.loadConditionHashMap(datafile);
		test.run();
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}
}

