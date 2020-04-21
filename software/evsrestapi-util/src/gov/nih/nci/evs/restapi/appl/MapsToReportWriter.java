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
    public static String MAPS_TO_HEADING = "Subset Code|Subset Name|Concept Code|NCIt Preferred Term|Relationship To Target|Target Code|Target Term|Target Term Type|Target Terminology|Target Terminology Version";

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

    OWLSPARQLUtils owlSPARQLUtils = null;
    String namedGraph = null;

    Vector raw_maps_to_data = null;

    public MapsToReportWriter(String serviceUrl, String namedGraph) {
		this.serviceUrl = serviceUrl;
		this.namedGraph = namedGraph;
		owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, null, null);
        long ms = System.currentTimeMillis();
        owlSPARQLUtils.set_named_graph(namedGraph);
        new MetadataUtils(serviceUrl).dumpNameVersion2NamedGraphMap();
        String propertyName = MAPS_TO;
        System.out.println("Initialization in progress. pelase wait...");
		raw_maps_to_data = retrievePropertyQualifierData(propertyName);
		System.out.println("Initialization completed.");
	}

	public Vector retrievePropertyQualifierData(String property_name) {
        long ms = System.currentTimeMillis();
        Vector v = owlSPARQLUtils.getPropertyQualifiersByCode(this.namedGraph, null, property_name);
        v = new ParserUtils().getResponseValues(v);
        return v;
	}

	public Vector retrievePropertyQualifierData(String property_name, String property_value) {
        long ms = System.currentTimeMillis();
        Vector v = owlSPARQLUtils.getPropertyQualifiersByCode(this.namedGraph, null, property_name, property_value);
        v = new ParserUtils().getResponseValues(v);
        return v;
	}

	public Vector get_concepts_in_subset(String code) {
	    return owlSPARQLUtils.get_concepts_in_subset(this.namedGraph, code);
    }

	public void compareValueSets(Vector codes) {
		String prev_str = "";
		String next_str = "";
		String prev_code = null;
		String next_code = null;
		for (int i=0; i<codes.size(); i++) {
			String code = (String) codes.elementAt(i);
		    Vector v = get_concepts_in_subset(code);
		    System.out.println("Subset " + code + ": " + v.size());
		    v = new SortUtils().quickSort(v);
		    StringBuffer buf = new StringBuffer();
			for (int j=0; j<v.size(); j++) {
				String line = (String) v.elementAt(j);
				Vector u = StringUtils.parseData(line, '|');
				String subset_member_code = (String) u.elementAt(1);
				buf.append(subset_member_code).append("|");
			}
			next_str = buf.toString();
			if (prev_str.length() == 0) {
				prev_str = next_str;
				prev_code = code;
			} else {
				if (prev_str.compareTo(next_str) != 0) {
					System.out.println("WARNING: Subsets " + code + " and " + prev_code + " are different.");
				} else {
					System.out.println("Subsets " + code + " and " + prev_code + " are the same.");
				}
				prev_code = code;
				prev_str = next_str;
			}
		}
	}

	public String getLabelByCode(String code) {
		Vector v = owlSPARQLUtils.getLabelByCode(this.namedGraph, code);
		v = new ParserUtils().getResponseValues(v);
		if (v == null || v.size() == 0) return null;
		return (String) v.elementAt(0);
	}

    public Vector sortMapsToData(String terminology_name, String terminology_version) {
		return new ParserUtils().sortMapsToData(raw_maps_to_data, terminology_name, terminology_version) ;
	}

    public Vector sortByColumn(Vector v, String columnName) {
        Vector headings = StringUtils.parseData(MAPS_TO_HEADING);
        for (int i=0; i<headings.size(); i++) {
			String heading = (String) headings.elementAt(i);
			if (heading.compareTo(columnName) == 0) {
				return sortByColumn(v, i);
			}
		}
		return sortByColumn(v, 0);
	}

    public Vector sortByColumn(Vector v, int columnNum) {
		HashMap hmap = new HashMap();
	    Vector keys = new Vector();
	    for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line);
			String key = (String) u.elementAt(columnNum);
			keys.add(key);
			hmap.put(key, line);
		}
		Vector w = new Vector();
		keys = new SortUtils().quickSort(keys);
		for (int i=0; i<keys.size(); i++) {
			String key = (String) keys.elementAt(i);
			String line = (String) hmap.get(key);
			w.add(line);
		}
		return w;
	}

    public Vector generateMapsToReport(String code, String terminology_name, String terminology_version) {
		Vector v = sortMapsToData(terminology_name, terminology_version);
		v = sortByColumn(v, "NCIt Preferred Term");
		String label = getLabelByCode(code);
		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			w.add(code + "|" + label + "|" + line);
		}
		w = sortByColumn(w, "NCIt Preferred Term");
		v = new Vector();
		v.add(MAPS_TO_HEADING);
		v.addAll(w);
        return v;
	}


    public static void main(String[] args) {
		long ms = System.currentTimeMillis();
		String serviceUrl = args[0];
		String named_graph = args[1];
		String terminology_name = args[2];
		String terminology_version = args[3];
		String codes_str = args[4];
		MapsToReportWriter mapsToReportWriter = new MapsToReportWriter(serviceUrl, named_graph);
		Vector codes = StringUtils.parseData(codes_str, '|');
		Vector datafile_vec = new Vector();
		Vector sheetLabel_vec = new Vector();

        for (int i=0; i<codes.size(); i++) {
			String code = (String) codes.elementAt(i);
			Vector v = mapsToReportWriter.generateMapsToReport(code, terminology_name, terminology_version);
			String label = mapsToReportWriter.getLabelByCode(code);
			System.out.println(label + " (" + code + ")");
			Utils.saveToFile(code + ".txt", v);
			sheetLabel_vec.add(label);
			datafile_vec.add(code + ".txt");
		}
		char delim = '|';
		String headerColor = ExcelWriter.RED;
		String excelfile = "Mapped_" + terminology_name + "_" + terminology_version + "_Terminology_" + StringUtils.getToday() + ".xlsx";
		new ExcelWriter().writeToXSSF(datafile_vec, excelfile, delim, sheetLabel_vec, headerColor);
		System.out.println(excelfile + " generated.");
        System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
    }
}




