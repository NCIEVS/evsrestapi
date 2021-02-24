package gov.nih.nci.evs.restapi.util;

import java.io.*;
import java.text.*;
import java.util.*;

public class MetadataTableGenerator {
	OWLSPARQLUtils owlSPARQLUtils = null;
    MetadataUtils metadataUtils = null;
    String version = null;
    String serviceUrl = null;
    String named_graph = null;
    String username = null;
    String password = null;

    public MetadataTableGenerator(String serviceUrl, String named_graph, String username, String password) {
		this.serviceUrl = serviceUrl;
		this.named_graph = named_graph;
		this.username = username;
		this.password = password;

	    this.owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, username, password);
        this.owlSPARQLUtils.set_named_graph(named_graph);
	    this.metadataUtils = new MetadataUtils(serviceUrl, username, password);
	    this.version = metadataUtils.getLatestVersion("NCI_Thesaurus");
	    System.out.println(this.version);

	}

	public void generateHTML(String filename) {
		Vector v = Utils.readFile(filename);
		HTMLTable.generate(v);
	}

	public void generate(String outputfile) {
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(outputfile, "UTF-8");
			pw.println("<title>NCIt_Properties_and_Relationships");
			pw.println("<table>Supported Properties");
			pw.println("<th>Name");
			pw.println("<th>Code");
			pw.println("<data>");
			Vector v = owlSPARQLUtils.getSupportedProperties(named_graph);
			for (int i=0; i<v.size(); i++) {
				String t = (String) v.elementAt(i);
				pw.println(t);
			}
			pw.println("</data>");
			pw.println("</table>");

			pw.println("<table>Supported Roles");
			pw.println("<th>Name");
			pw.println("<th>Code");
			pw.println("<data>");
			v = owlSPARQLUtils.getSupportedRoles(named_graph);
			for (int i=0; i<v.size(); i++) {
				String t = (String) v.elementAt(i);
				pw.println(t);
			}
			pw.println("</data>");
			pw.println("</table>");

			pw.println("<table>Supported Associations");
			pw.println("<th>Name");
			pw.println("<th>Code");
			pw.println("<data>");
			v = owlSPARQLUtils.getSupportedAssociations(named_graph);
			for (int i=0; i<v.size(); i++) {
				String t = (String) v.elementAt(i);
				pw.println(t);
			}
			pw.println("</data>");
			pw.println("</table>");

			pw.println("<table>Supported Property Qualifiers");
			pw.println("<th>Property Name");
			pw.println("<th>Property Code");
			pw.println("<th>Qualifier Name");
			pw.println("<th>QualifierCode");
			pw.println("<data>");
			v = owlSPARQLUtils.getSupportedPropertyQualifiers(named_graph);
			for (int i=0; i<v.size(); i++) {
				String t = (String) v.elementAt(i);
				pw.println(t);
			}
			pw.println("</data>");
			pw.println("</table>");

			pw.println("<footer>(Source; NCI Thesaurus, version " + this.version + ")");
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			try {
				pw.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
		String serviceUrl = args[0];
		String named_graph = args[1];
		String username = args[2];
		String password = args[3];
        MetadataTableGenerator MetadataTableGenerator = new MetadataTableGenerator(serviceUrl, named_graph, username, password);
        String filename = "metadata.txt";
        MetadataTableGenerator.generate(filename);
        MetadataTableGenerator.generateHTML(filename);
	}
}