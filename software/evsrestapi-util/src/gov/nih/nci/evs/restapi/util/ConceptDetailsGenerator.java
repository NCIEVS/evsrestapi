package gov.nih.nci.evs.restapi.util;

import gov.nih.nci.evs.restapi.util.*;
import gov.nih.nci.evs.restapi.bean.*;
import gov.nih.nci.evs.restapi.common.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.io.*;
import java.util.*;
import java.util.Map.Entry;


public class ConceptDetailsGenerator {
	HierarchyHelper hh = null;
	Vector parent_child_vec = null;
	String TREE_VARIABLE_NAME = "demoList";
	String HYPERLINK = "https://nciterms65.nci.nih.gov/ncitbrowser/pages/concept_details.jsf?dictionary=NCI_Thesaurus&type=terminology&key=null&b=1&n=0&vse=null&code=";

	HashMap propertyHashMap = null;

	public ConceptDetailsGenerator() {
		this.propertyHashMap = null;
	}

	public ConceptDetailsGenerator(HashMap propertyHashMap) {
		this.propertyHashMap = propertyHashMap;
	}

	public void setHYPERLINK(String hyperlinkUrl) {
		this.HYPERLINK = hyperlinkUrl;
	}

	public String getHyperLink(String code) {
		if (HYPERLINK != null) {
			return HYPERLINK + code;
		} else {
			return null;
		}
	}

    public String encode(String t) {
		if (t == null) return null;
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<t.length(); i++) {
			char c = t.charAt(i);
			if (c > 126) {
				buf.append(" ");
			} else {
				String s = "" + c;
				if (s.compareTo("'") == 0) {
					buf.append("\\'");
				} else {
					buf.append(s);
				}
			}
		}
		return buf.toString();
	}

    public void writeHeader(PrintWriter out, String title) {
      out.println("<!doctype html>");
      out.println("<html lang=\"en\">");
      out.println("<head>");
      out.println("	<meta charset=\"utf-8\">");
      out.println("	<title>" + title + "</title>");
      out.println("	<link rel=\"stylesheet\" href=\"tree.css\">");
      writeFunction(out);
      out.println("</head>");
	}


    public void writeFunction(PrintWriter out) {
		out.println("	<script type=\"text/javascript\" src=\"http://ajax.googleapis.com/ajax/libs/jquery/1.4.2/jquery.min.js\"></script>");
		out.println("	<script type=\"text/javascript\" src=\"js/jquery.sapling.min.js\"></script>");
		out.println("    <link rel=\"stylesheet\" type=\"text/css\" href=\"css/tree.css\">");
		out.println("	<script type=\"text/javascript\">");
		out.println("		$(document).ready(function() {");
		out.println("			$('#demoList').sapling();");
		out.println("		});");
		out.println("	</script>");
    }


	public String getIndentation(int level) {
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<level; i++) {
			buf.append("\t");
		}
		return buf.toString();
	}


	public String propertyHashMap2HTML(HashMap propertyHashMap) {
		StringBuffer buf = new StringBuffer();
		buf.append("<table>").append("\n");
		Iterator it = propertyHashMap.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			String line = CSVFileReader.csv2Delimited(key, "|");
			Vector u = StringUtils.parseData(line, '|');
			String source_code = (String) u.elementAt(0);
			String source_term = (String) u.elementAt(1);
			String target_code = (String) u.elementAt(2);
			String target_term = (String) u.elementAt(3);
			buf.append("<tr>").append("\n");
			StringBuffer property_buf = new StringBuffer();
			HashMap hmap = (HashMap) propertyHashMap.get(key);
			Vector properties = new Vector();
			Iterator it2 = hmap.keySet().iterator();
			while (it2.hasNext()) {
				String key2 = (String) it2.next();
				properties.add(key2);
			}
			properties = new gov.nih.nci.evs.restapi.util.SortUtils().quickSort(properties);
			int line_num = 0;
			for (int k=0; k<properties.size(); k++) {
				StringBuffer line_buf = new StringBuffer();
				String property = (String) properties.elementAt(k);
				Vector values = (Vector) hmap.get(property);
				values = new gov.nih.nci.evs.restapi.util.SortUtils().quickSort(values);
			    for (int k2=0; k2<values.size(); k2++) {
					if (line_num == 0) {
						line_buf.append("<td>").append(source_code).append("</td>").append("\n");
						line_buf.append("<td>").append(source_term).append("</td>").append("\n");
						line_buf.append("<td>").append("<a href='").append(getHyperLink(target_code)).append("'>").append(target_code).append("</a>").append("</td>").append("\n");
						line_buf.append("<td>").append(target_term).append("</td>").append("\n");

					} else {
						line_buf.append("<td>").append("").append("</td>").append("\n");
						line_buf.append("<td>").append("").append("</td>").append("\n");
						line_buf.append("<td>").append("").append("</td>").append("\n");
						line_buf.append("<td>").append("").append("</td>").append("\n");
					}
					String value = (String) values.elementAt(k2);
					String col1 = property;
					String col2 = value;
					if (k2 != 0) {
						col1 = "";
					}
					line_buf.append("<td>").append(col1).append("</td>").append("\n");
					line_buf.append("<td>").append(col2).append("</td>").append("\n");

					buf.append(line_buf.toString()).append("\n");
					buf.append("</tr>").append("\n");
					line_num++;
					line_buf = new StringBuffer();
				}

			}

		}
		buf.append("</table>").append("\n");
		System.out.println(buf.toString());
		return buf.toString();
	}

	public void writeBody(PrintWriter out, String title) {
		out.println("<body>");
		out.println("	<center><h3>" + title + "</h3></center>");
		out.println("	<hr>");
		String content = null;
		try {
			content = propertyHashMap2HTML(this.propertyHashMap);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		out.println(content);
		out.println("</body>");
	}

	public void writeFooter(PrintWriter out) {
		out.println("</html>");
	}

	public Vector sortByLabel(Vector codes) {
		if (codes == null || codes.size()<=1) return codes;
		Vector w = new Vector();
		HashMap hmap = new HashMap();
		for (int i=0; i<codes.size(); i++) {
			String code = (String) codes.elementAt(i);
			String label = hh.getLabel(code);
			hmap.put(label, code);
			w.add(label);
		}
		w = new SortUtils().quickSort(w);
		Vector v = new Vector();
		for (int i=0; i<w.size(); i++) {
			String label = (String) w.elementAt(i);
			String code = (String) hmap.get(label);
			v.add(code);
		}
		return v;
	}

    public void generate(PrintWriter out, String title) {
		System.out.println("writeHeader");
        writeHeader(out, title);
        System.out.println("writeBody");
        writeBody(out, title);
        System.out.println("writeFooter");
        writeFooter(out);
	}

	public void generate(String outputfile, String title) {
		long ms = System.currentTimeMillis();
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(outputfile, "UTF-8");
			generate(pw, title);

		} catch (Exception ex) {

		} finally {
			try {
				pw.close();
				System.out.println("Output file " + outputfile + " generated.");
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}

	public static Vector filterRoots(Vector parent_child_vec) {
		Vector w = new Vector();
		for (int i=0; i<parent_child_vec.size(); i++) {
			String line = (String) parent_child_vec.elementAt(i);
			if (line.indexOf("|<Root>|") == -1) {
				w.add(line);
			}
		}
		return w;
	}


    public HashMap appendPropertiesToMappingEntries(String serviceUrl, String namedGraph, Vector mapping_entries) {
		HashMap propertyHashMap = new HashMap();
		OWLSPARQLUtils owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl);
		ParserUtils parserUtils = new ParserUtils();
        Vector v = mapping_entries;
        for (int i=1; i<mapping_entries.size(); i++) {
			String line = (String) mapping_entries.elementAt(i);
			String line1 = line;
			if (line.indexOf("|") == -1) {
				line1 = CSVFileReader.csv2Delimited(line, "|");
			}

			System.out.println(line1);
			Vector u = StringUtils.parseData(line1, '|');
			String source_code = (String) u.elementAt(0);
			String source_term = (String) u.elementAt(1);
			String target_code = (String) u.elementAt(2);
			if (target_code != null && target_code.length() > 0) {
				String query = owlSPARQLUtils.construct_get_properties_by_code(namedGraph, target_code);
				Vector w = owlSPARQLUtils.getPropertiesByCode(namedGraph, target_code);
				if (w == null || w.size() == 0) {
					System.out.println("\tgetPropertiesByCode returns null???");
				} else {
					w = parserUtils.getResponseValues(w);
					HashMap hmap = createPropertyHashMap(w);
					propertyHashMap.put(line, hmap);
				}
			}
		}
		return propertyHashMap;
	}

	public void dumpPropertyHashMap(HashMap propertyHashMap) {
		Iterator it = propertyHashMap.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();

			HashMap hmap = (HashMap) propertyHashMap.get(key);
			System.out.println(key);
			Vector properties = new Vector();
			Iterator it2 = hmap.keySet().iterator();
			while (it2.hasNext()) {
				String key2 = (String) it2.next();
				properties.add(key2);
			}
			properties = new gov.nih.nci.evs.restapi.util.SortUtils().quickSort(properties);
			for (int k=0; k<properties.size(); k++) {
				String property = (String) properties.elementAt(k);
				System.out.println(property);
				Vector values = (Vector) hmap.get(property);
				values = new gov.nih.nci.evs.restapi.util.SortUtils().quickSort(values);
			    for (int k2=0; k2<values.size(); k2++) {
					String value = (String) values.elementAt(k2);
					System.out.println("\t" + value);
				}
			}
		}
	}

	public HashMap createPropertyHashMap(Vector w) {
		HashMap hmap = null;
    	if (w == null || w.size() == 0) {
			return hmap;
		}
		hmap = new HashMap();
		for (int j=0; j<w.size(); j++) {
			String t = (String) w.elementAt(j);
			Vector u = StringUtils.parseData(t, '|');
			//Secondary Malignant Neoplasm|code|C4968
			String property_name = (String) u.elementAt(1);
			String property_value = (String) u.elementAt(2);
			Vector v = new Vector();
			if (hmap.containsKey(property_name)) {
				v = (Vector) hmap.get(property_name);
			}
			v.add(property_value);
			hmap.put(property_name, v);
		}
		return hmap;
	}

	public static void main(String[] args) {
		String serviceUrl = args[0];
		String namedGraph = args[1];
		String mappingfile = args[2];
		System.out.println("serviceUrl: " + serviceUrl);
		System.out.println("namedGraph: " + namedGraph);
		System.out.println("mappingfile: " + mappingfile);

		System.out.println("Generating property hashmap...");
		Vector mapping_entries = Utils.readFile(mappingfile);

		HashMap propertyHashMap = new ConceptDetailsGenerator().appendPropertiesToMappingEntries(serviceUrl, namedGraph, mapping_entries);
		ConceptDetailsGenerator generator = new ConceptDetailsGenerator(propertyHashMap);
		String hyperlinkUrl = "https://ncimappingtool-dev.nci.nih.gov/ncimappingtool/pages/concept_details.jsf?ng=http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl&code=";
        generator.setHYPERLINK(hyperlinkUrl);
	    String outputfile = "mapping.html";
	    String title = "Mapping Entry Concept Details";
		generator.generate(outputfile, title);
	}

}




