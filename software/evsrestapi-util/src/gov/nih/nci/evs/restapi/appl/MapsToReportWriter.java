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
    String OWLFILE = "ThesaurusInferred_forTS.owl";

    OWLSPARQLUtils owlSPARQLUtils = null;
    MetadataUtils metadataUtils = null;
    String namedGraph = null;

	String username = null;
	String password = null;

    Vector raw_maps_to_data = null;
    Vector<MapToEntry> mapsToEntries = null;
    String ncit_version = null;
    HashSet retired = null;

    public MapsToReportWriter(String serviceUrl, String namedGraph, String username, String password) {
		this.serviceUrl = serviceUrl;
		this.namedGraph = namedGraph;
		this.username = username;
		this.password = password;

		System.out.println("serviceUrl: " + serviceUrl);
		System.out.println("namedGraph: " + namedGraph);

		owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, username, password);
		if (owlSPARQLUtils == null) {
			System.out.println("WARNING: unable to instantiate owlSPARQLUtils???");
		}

        long ms = System.currentTimeMillis();
        owlSPARQLUtils.set_named_graph(namedGraph);
        Vector concept_status_vec = owlSPARQLUtils.getPropertyValues(namedGraph, "Concept_Status");
        if (concept_status_vec == null) {
			System.out.println("namedGraph: " + namedGraph);
			System.out.println("WARNING: concept_status_vec == null???");
		} else {
			//concept_status_vec = new ParserUtils().getResponseValues(concept_status_vec);
			retired = new HashSet();
			for (int i=0; i<concept_status_vec.size(); i++) {
				String line = (String) concept_status_vec.elementAt(i);
				Vector u = StringUtils.parseData(line, '|');
				String code = (String) u.elementAt(1);
				String status = (String) u.elementAt(3);
				if (status.compareTo("Retired_Concept") == 0) {
					retired.add(code);
				}
			}
		}
        metadataUtils = new MetadataUtils(serviceUrl, username, password);
        ncit_version = get_ncit_version();
        System.out.println("NCI Thesaurus version: " + ncit_version);

        String propertyName = MAPS_TO;
        System.out.println("Initialization in progress. Please wait...");
		raw_maps_to_data = retrievePropertyQualifierData(propertyName);
		mapsToEntries = new ParserUtils().parseMapsToData(raw_maps_to_data);

		System.out.println("Initialization completed.");
	}


    public Vector removeRetired(Vector v) {
		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String code = (String) u.elementAt(2);
			if (!isRetired(code)) {
				w.add(line);
			}
		}
		return w;
	}

	public Vector retrievePropertyQualifierData(String property_name) {
        long ms = System.currentTimeMillis();
        Vector v = owlSPARQLUtils.getPropertyQualifiersByCode(this.namedGraph, null, property_name);
        //v = new ParserUtils().getResponseValues(v);
        v = removeRetired(v);
        return v;
	}

	public Vector retrievePropertyQualifierData(String property_name, String property_value) {
        long ms = System.currentTimeMillis();
        Vector v = owlSPARQLUtils.getPropertyQualifiersByCode(this.namedGraph, null, property_name, property_value);
        //v = new ParserUtils().getResponseValues(v);
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
		//v = new ParserUtils().getResponseValues(v);
		if (v == null || v.size() == 0) return null;
		return (String) v.elementAt(0);
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

			for (int k=0; k<u.size(); k++) {
				String value = (String) u.elementAt(k);
				if (k != columnNum) {
					key = key + "|" + value;
				}
			}

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


    public void dumpMapsToEntries() {
		Vector json_vec = new Vector();
		for (int i=0; i<mapsToEntries.size(); i++) {
			MapToEntry entry = (MapToEntry) mapsToEntries.elementAt(i);
			json_vec.add(entry.toJson());
		}
		Utils.saveToFile("MapToEntries_" + StringUtils.getToday() + ".txt", json_vec);
	}

	public Vector getMapsToData(String terminology_name, String terminology_version) {

		Vector v = new Vector();
		HashSet hset = new HashSet();

		for (int i=0; i<mapsToEntries.size(); i++) {
			MapToEntry entry = (MapToEntry) mapsToEntries.elementAt(i);
			if ((entry.getTargetTerminology() != null && entry.getTargetTerminology().compareTo(terminology_name) == 0) &&
		        (entry.getTargetTerminologyVersion() != null && entry.getTargetTerminologyVersion().compareTo(terminology_version) == 0)) {

				String line = entry.getCode()
				      + "|" + entry.getPreferredName()
				      + "|" + entry.getRelationshipToTarget()
				      + "|" + entry.getTargetCode()
				      + "|" + entry.getTargetTerm()
				      + "|" + entry.getTargetTermType()
				      + "|" + entry.getTargetTerminology()
				      + "|" + entry.getTargetTerminologyVersion();
				if (!hset.contains(line)) {
					v.add(line);
					hset.add(line);
				} else {
					System.out.println("WARNING: dupicated entry " + line);
				}
			}
		}

		return v;
	}


	public Vector getMapsToData(String terminology_name, String terminology_version, String sourceCode) {
		Vector v = new Vector();
		HashSet hset = new HashSet();

		for (int i=0; i<mapsToEntries.size(); i++) {
			MapToEntry entry = (MapToEntry) mapsToEntries.elementAt(i);
			if ((entry.getTargetTerminology() != null && entry.getTargetTerminology().compareTo(terminology_name) == 0) &&
		        (entry.getTargetTerminologyVersion() != null && entry.getTargetTerminologyVersion().compareTo(terminology_version) == 0)) {
				if (entry.getCode().compareTo(sourceCode) == 0) {
					String line = entry.getCode()
						  + "|" + entry.getPreferredName()
						  + "|" + entry.getRelationshipToTarget()
						  + "|" + entry.getTargetCode()
						  + "|" + entry.getTargetTerm()
						  + "|" + entry.getTargetTermType()
						  + "|" + entry.getTargetTerminology()
						  + "|" + entry.getTargetTerminologyVersion();
					if (!hset.contains(line)) {
						v.add(line);
						hset.add(line);
					} else {
						System.out.println("WARNING: dupicated entry " + line);
					}
				}
			}
		}
		return v;
	}

    public Vector generateMapsToReport(String code, String terminology_name, String terminology_version) {
		Vector v = getMapsToData(terminology_name, terminology_version);
		Utils.saveToFile(terminology_name + "_" + terminology_version + ".txt", v);
		boolean codeOnly = true;
		Vector members = owlSPARQLUtils.getSubsetMembership(namedGraph, code, codeOnly);
		v = sortByColumn(v, "NCIt Preferred Term");
		String label = getLabelByCode(code);
		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String member_code = (String) u.elementAt(0);
			if (members.contains(member_code)) {
				w.add(code + "|" + label + "|" + line);
			}
		}
		w = sortByColumn(w, "NCIt Preferred Term");
		v = new Vector();
		v.add(MAPS_TO_HEADING);
		v.addAll(w);
        return v;
	}

	public String get_ncit_version() {
		Vector v = owlSPARQLUtils.get_ontology_info(namedGraph);
		Utils.dumpVector("get_ncit_version", v);
		String line = (String) v.elementAt(0);
		Vector u = StringUtils.parseData(line, '|');
		ncit_version = (String) u.elementAt(0);
		System.out.println(ncit_version);
		/*
		if(ncit_version == null) {
			HashMap hmap = metadataUtils.getNameVersion2NamedGraphMap();
			String version = null;
			Iterator it = hmap.keySet().iterator();
			while (it.hasNext()) {
				String key = (String) it.next();
				Vector values = (Vector) hmap.get(key);
				String value = (String) values.elementAt(0);
				if (value.compareTo(this.namedGraph) == 0) {
					Vector u = StringUtils.parseData(key, '|');
					ncit_version = (String) u.elementAt(1);
					break;
				}
			}
		}
		*/

		return ncit_version;
	}

    public Vector getRootCodes(String version) {
		Vector w = new Vector();
        System.out.println("ICDO version: " + version);
        String label = "Mapped ICDO" + version + " Terminology";
        Vector v = owlSPARQLUtils.getCodeByLabel(this.namedGraph, label);
        if (v == null) {
			System.out.println("ERROR: getCodeByLabel return null???");
		} else {
             //v = new ParserUtils().getResponseValues(v);
             w.add((String) v.elementAt(0));
		}
        label = "Mapped ICDO" + version + " Morphology Terminology";
        v = owlSPARQLUtils.getCodeByLabel(this.namedGraph, label);
        //v = new ParserUtils().getResponseValues(v);
        w.add((String) v.elementAt(0));

        label = "Mapped ICDO" + version + " Morphology PT Terminology";
        v = owlSPARQLUtils.getCodeByLabel(this.namedGraph, label);
        //v = new ParserUtils().getResponseValues(v);
        w.add((String) v.elementAt(0));

/*
Mapped ICDO Terminology (C168654)
Mapped ICDO3.1 Terminology (C168655)
Mapped ICDO3.2 Terminology (C168656)
Mapped ICDO3.2 Morphology Terminology (C168661)
Mapped ICDO3.2 Topography Terminology (C168663)
Mapped ICDO3.2 Topography PT Terminology (C168664)
Mapped ICDO3.2 Morphology PT Terminology (C168662)
Mapped ICDO3.1 Morphology Terminology (C168657)
Mapped ICDO3.1 Topography Terminology (C168659)
Mapped ICDO3.1 Topography PT Terminology (C168660)
Mapped ICDO3.1 Morphology PT Terminology (C168658)
*/
        //To be modified:
        label = "Mapped ICDO" + version + " Topography Terminology";
        v = owlSPARQLUtils.getCodeByLabel(this.namedGraph, label);
        //v = new ParserUtils().getResponseValues(v);
        w.add((String) v.elementAt(0));

        label = "Mapped ICDO" + version + " Topography PT Terminology";
        v = owlSPARQLUtils.getCodeByLabel(this.namedGraph, label);
        //v = new ParserUtils().getResponseValues(v);
        w.add((String) v.elementAt(0));

        StringUtils.dumpVector("codes", w);
        return w;
	}

	public boolean isRetired(String code) {
		return retired.contains(code);
	}


    public void run(String terminology_name, String terminology_version) {
		Vector codes = getRootCodes(terminology_version);
		run(terminology_name, terminology_version, codes);
	}

	//We don’t want the SY entries to show up in the following two sets C168658 Mapped ICDO3.1 Morphology PT Terminology and C168662 Mapped ICDO3.2 Morphology PT Terminology.
	//C168663|Mapped ICDO3.2 Topography Terminology|C12252|Abdominal Esophagus|Related To|C15.2|Abdominal esophagus|PT|ICDO3|3.2
	public Vector filterTargetTermType(Vector v, String code, String term_type) {
		if (code.compareTo("C168658") != 0 && code.compareTo("C168662") != 0 &&
		    code.compareTo("C168660") != 0 && code.compareTo("C168664") != 0) return v;
		Vector w = new Vector();
		w.add((String) v.elementAt(0));
		for (int i=1; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String type = (String) u.elementAt(7);
			if (type.compareTo(term_type) == 0) {
				w.add(line);
			}
		}
		return w;
	}

    public void run(String terminology_name, String terminology_version, Vector codes) {
		Vector datafile_vec = new Vector();
		Vector sheetLabel_vec = new Vector();
		String version = get_ncit_version();
		String label0 = null;
		dumpMapsToEntries();
        for (int i=0; i<codes.size(); i++) {
			String code = (String) codes.elementAt(i);
			Vector v = generateMapsToReport(code, terminology_name, terminology_version);
			v = filterTargetTermType(v, code, "PT");
			String label = getLabelByCode(code);
			System.out.println(label + " (" + code + ")");
			Utils.saveToFile(code + ".txt", v);
			sheetLabel_vec.add(label);
			datafile_vec.add(code + ".txt");
			if (i == 0) {
				label0 = label;
			}
		}
		label0 = label0.replaceAll(" ", "_");
		char delim = '|';
		//String headerColor = ExcelWriter.RED;
		String excelfile = label0 + "_(" + version + ")_" + StringUtils.getToday() + ".xlsx";
		new ExcelWriter().writeToXSSF(datafile_vec, excelfile, delim, sheetLabel_vec, null);
		System.out.println(excelfile + " generated.");
	}
/*
    public static void main(String[] args) {
		String serviceUrl = ConfigurationController.serviceUrl;
		String namedGraph = ConfigurationController.namedGraph;
		String username = ConfigurationController.username;
		String password = ConfigurationController.password;

		String terminology_name = args[0];
		String terminology_version = args[1];

		long ms = System.currentTimeMillis();
		MapsToReportWriter mapsToReportWriter = new MapsToReportWriter(serviceUrl, namedGraph, username, password);
		mapsToReportWriter.run(terminology_name, terminology_version);
        System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
    }
*/
}





