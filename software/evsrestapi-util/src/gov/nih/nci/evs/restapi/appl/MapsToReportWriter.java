package gov.nih.nci.evs.restapi.appl;

import gov.nih.nci.evs.restapi.util.*;
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


public class MapsToReportWriter {
	public static String MAPS_TO_HEADING = "CODE|PT|RELATIONSHIP_TO_TARGET|TARGET_CODE|TARGET_TERM|TARGET_TERM_TYPE|TARGET_TERMINOLOGY|TARGET_TERMINOLOGY_VERSION";
	public static String MAPS_TO = "Maps_To";
	public static int NUMER_OF_FIELDS = 8;
    JSONUtils jsonUtils = null;
    HTTPUtils httpUtils = null;
    String named_graph = null;
    String prefixes = null;
    String serviceUrl = null;
    String named_graph_id = ":NHC0";
    String BASE_URI = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl";

    int pos_z_axiom = 0;
    int pos_x_label = 1;
    int pos_x_code = 2;
    int pos_p_label = 3;
    int pos_p_code = 4;
    int pos_z_target = 5;
    int pos_y_label = 6;
    int pos_y_code = 7;
    int pos_z = 8;


    public MapsToReportWriter() {

	}


	public static void generateMapsToReport(Vector v) {
		generateMapsToReport(v, null);
	}

    public static void generateMapsToReport(Vector v, String target_terminology) {
		//Vector v = Utils.readFile(inputfile);
		Vector w = new Vector();
		System.out.println(v.size());
		HashMap hmap = new HashMap();
		Vector axiomIds = new Vector();

		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line);
			int n = u.size();
			String lastValue = (String) u.elementAt(n-1);
			if (target_terminology != null) {
				if (lastValue.compareTo(target_terminology) == 0) {
					w.add(line);
					String axiomId = (String) u.elementAt(0);
					axiomIds.add(axiomId);
					Vector values = new Vector();
					for (int k=0; k<NUMER_OF_FIELDS; k++) {
						values.add("N/A");
					}
					hmap.put(axiomId, values);
				}
			} else {
				w.add(line);
				String axiomId = (String) u.elementAt(0);
				if (!hmap.containsKey(axiomId)) {
					axiomIds.add(axiomId);
					Vector values = new Vector();
					for (int k=0; k<NUMER_OF_FIELDS; k++) {
						values.add("N/A");
					}
					hmap.put(axiomId, values);
				}
			}
		}

		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line);
			String axiomId = (String) u.elementAt(0);
			String field_name = (String) u.elementAt(6);
			String value = (String) u.elementAt(8);
			String code = (String) u.elementAt(2);
			String label = (String) u.elementAt(1);
			String term_name = (String) u.elementAt(5);
			Vector values = (Vector) hmap.get(axiomId);

			if (values != null) {
				if (field_name.compareTo("Relationship_to_Target") == 0) {
					values.setElementAt(value, 2);
				} else if (field_name.compareTo("Target_Code") == 0) {
					values.setElementAt(value, 3);
				} else if (field_name.compareTo("Target_Term_Type") == 0) {
					values.setElementAt(value, 5);
				} else if (field_name.compareTo("Target_Terminology") == 0) {
					values.setElementAt(value, 6);
				} else if (field_name.compareTo("Target_Terminology_Version") == 0) {
					values.setElementAt(value, 7);
				}
				values.setElementAt(code, 0);
				values.setElementAt(label, 1);
				values.setElementAt(term_name, 4);
				hmap.put(axiomId, values);
			}
		}

		Vector lines = new Vector();

		for (int i=0; i<axiomIds.size(); i++) {
			String axiomId = (String) axiomIds.elementAt(i);
			Vector values = (Vector) hmap.get(axiomId);
			StringBuffer buf = new StringBuffer();
			for (int j=0; j<NUMER_OF_FIELDS; j++) {
				String value = (String) values.elementAt(j);
				buf.append(value);
				if (j<NUMER_OF_FIELDS-1) {
					buf.append("|");
				}
			}
			lines.add(buf.toString());
		}
		if (target_terminology == null) {
			target_terminology = "Maps_To";
		}
		lines = new SortUtils().quickSort(lines);
		Vector w2 = new Vector();
		w2.add(MAPS_TO_HEADING);
		w2.addAll(lines);
		Utils.saveToFile(target_terminology + "_Report" + "_" + StringUtils.getToday() + ".txt", w2);
	}

    public static void findFullSynForSourceCodes(OWLSPARQLUtils owlSPARQLUtils, String named_graph, String mapsToFile) { // "mapsToGDC.txt"
		Vector v = Utils.readFile(mapsToFile);
		System.out.println("Number of lines: " + v.size());
		HashSet hset = new HashSet();
		Vector fullSynVec = new Vector();
		int lcv = 0;
		for (int i=1; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line);
			String code = (String) u.elementAt(0);
			if (!hset.contains(code)) {
				hset.add(code);
				lcv++;
				System.out.println("(" + lcv + ") " + code);
				Vector v2 = owlSPARQLUtils.getPropertyQualifiersByCode(named_graph, code, "FULL_SYN");
				v2 = new ParserUtils().getResponseValues(v2);
				fullSynVec.addAll(v2);
			}
		}
		Utils.saveToFile("fullSyn.txt", fullSynVec);
	}

	public static Vector retrievePropertyQualifierData(String serviceUrl, String named_graph, String property_name) {
		OWLSPARQLUtils owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, null, null);
        long ms = System.currentTimeMillis();
        owlSPARQLUtils.set_named_graph(named_graph);
        Vector v = owlSPARQLUtils.getPropertyQualifiersByCode(named_graph, null, property_name);
        v = new ParserUtils().getResponseValues(v);
        return v;
	}

	public static Vector retrievePropertyQualifierData(String serviceUrl, String named_graph, String property_name, String property_value) {
		OWLSPARQLUtils owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, null, null);
        long ms = System.currentTimeMillis();
        owlSPARQLUtils.set_named_graph(named_graph);
        Vector v = owlSPARQLUtils.getPropertyQualifiersByCode(named_graph, null, property_name, property_value);
        v = new ParserUtils().getResponseValues(v);
        return v;
	}

    public static void generateMapsToReport(String serviceUrl, String named_graph) {
		String propertyName = MAPS_TO;
		Vector v = retrievePropertyQualifierData(serviceUrl, named_graph, propertyName);
		generateMapsToReport(v, null);
	}

    public static void main(String[] args) {
		long ms = System.currentTimeMillis();
		String serviceUrl = args[0];
		String named_graph = args[1];
		OWLSPARQLUtils owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, null, null);
		String propertyName = MAPS_TO;
		String propertyValue = "GDC";
        generateMapsToReport(retrievePropertyQualifierData(serviceUrl, named_graph, "Maps_To"));
        System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
    }
}

