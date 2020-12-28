package gov.nih.nci.evs.restapi.util;
import gov.nih.nci.evs.restapi.model.*;
import java.io.*;
import java.text.*;
import java.util.*;

public class ValueSetReportGenerator {
	String pageTitle = "Value Set QA Report";
	String tableTitle = null;
	Vector warnings = null;
    static String HYPERLINK_URL ="https://nciterms65.nci.nih.gov/ncitbrowser/pages/concept_details.jsf?dictionary=NCI_Thesaurus&ns=ncit&code=";
    String serviceUrl = null;
    String namedGraph = null;
    String username = null;
    String password = null;
    OWLSPARQLUtils owlSPARQLUtils = null;
	MetadataUtils metadataUtils = null;
    String version = null;
    String definition = null;
    Vector conditions = null;
    String description = null;
    static String NCI_THESAURUS = "NCI_Thesaurus";
    static String RETIRED_CONCEPT = "Retired_Concept";
    String headerConceptCode = null;
    String timestamp = null;
    String footer = null;
    String label = null;
    private Vector condition_data = null;
    gov.nih.nci.evs.restapi.util.ValueSetConditionValidator validator = null;
    String headerConceptLabel = null;
    boolean checkOutBoundConceptInSubset = false;
    Vector missing_vec = null;
    public static String CONCEPT_STATUS = "Concept_Status";
    Vector retired_concepts = null;
    Vector concept_status_vec = null;
    HashSet retired_concept_codes = null;

    public void setValueSetConditionValidator(gov.nih.nci.evs.restapi.util.ValueSetConditionValidator validator) {
		this.validator = validator;
	}

	public Vector getWarnings() {
		return this.warnings;
	}

	public Vector getMissings() {
		return this.missing_vec;
	}

	public void setCheckOutBoundConceptInSubset(boolean bool) {
		checkOutBoundConceptInSubset = bool;
	}


	public ValueSetReportGenerator(String serviceUrl, String namedGraph, String username, String password,
	    gov.nih.nci.evs.restapi.util.ValueSetConditionValidator validator) {
		this.serviceUrl = serviceUrl;
		this.username = username;
		this.password = password;
		this.namedGraph = namedGraph;

		this.validator = validator;
		this.headerConceptCode = validator.getHeaderConceptCode();
		this.owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, username, password);
		this.owlSPARQLUtils.set_named_graph(namedGraph);

        this.concept_status_vec = owlSPARQLUtils.getConceptsContainingProperty(this.namedGraph, CONCEPT_STATUS);
        Utils.saveToFile("CONCEPT_STATUS.txt", this.concept_status_vec);

        this.retired_concept_codes = new HashSet();
        //C155798|Chimeric Antigen Receptor T-cell Therapy|P310|Concept_Status|Retired_Concept
        for (int i=0; i<concept_status_vec.size(); i++) {
			String line = (String) concept_status_vec.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String status = (String) u.elementAt(4);
			if (status.compareTo(RETIRED_CONCEPT) == 0) {
				String code = (String) u.elementAt(0);
				this.retired_concept_codes.add(code);
			}
		}

		this.headerConceptLabel = this.owlSPARQLUtils.getLabel(this.headerConceptCode);
		this.metadataUtils = new MetadataUtils(serviceUrl, username, password);

		this.description = "";
		this.label = this.owlSPARQLUtils.getLabel(headerConceptCode);
		String hyperlinkedCode = getHyperlink(headerConceptCode);
		setTableTitle(label + " (" + hyperlinkedCode + ")");

		this.conditions = new Vector();
		this.version = getVersion();
		this.definition = getDefinition();
		this.footer = "Source: " + NCI_THESAURUS + " (version: " + this.version + ")";
		this.timestamp = "Last modified: " + StringUtils.getToday();

	}

	public boolean isRetired(String code) {
		if (retired_concept_codes.contains(code)) return true;
		return false;
	}

	private void setTableTitle(String tbl_title) {
		this.tableTitle = tbl_title;
	}

    public void set_condition_data(Vector condition_data) {
		this.condition_data = condition_data;
	}

    public void set_conditions(Vector conditions) {
		this.conditions = conditions;
	}

    private Set findCodesMeetPropertyConditions() {
		Set condition_codes = new HashSet();
        String code = null;
	    Vector propertyLabels = new Vector();
	    for (int i=0; i<condition_data.size(); i++) {
			String line = (String) condition_data.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String type = (String) u.elementAt(0);
			if ((type.compareTo("Property") == 0 && u.size() == 2) ||
			    (type.compareTo("PropertyValue") == 0 && u.size() == 3)) {
				String property_code = (String) u.elementAt(1);
                String property_label = null;
				if (validator == null) {
					System.out.println("Validator == null???");
				}
				property_label = validator.getPropertyLabel(property_code);
				propertyLabels.add(property_label);
			}
		}
	    Vector w = owlSPARQLUtils.getConceptsWithProperties(this.namedGraph, code, propertyLabels);
		if (w == null || w.size() == 0) {
			System.out.println("getConceptsWithProperties failed???");
		}
        Utils.saveToFile(headerConceptCode + "_ConceptsWithProperties.txt", w);
        for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String condition_code = (String) u.elementAt(0);
			condition_codes.add(condition_code);
		}
		return condition_codes;
	}

//w.add("Property|P322|CTDC");
    private Vector findPropertyLabelAndValueInPropertyValueConditions() {
		Vector propertyLabelAndValue = new Vector();
	    for (int i=0; i<condition_data.size(); i++) {
			String line = (String) condition_data.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String type = (String) u.elementAt(0);
			if (type.compareTo("Property") == 0 && u.size() == 3) {
				String property_code = (String) u.elementAt(1);
				String property_value = (String) u.elementAt(2);
				String property_label = owlSPARQLUtils.getPropertyLabel(property_code);
				if (!propertyLabelAndValue.contains(property_label + "|" + property_value)) {
					propertyLabelAndValue.add(property_label + "|" + property_value);
				}
			}
		}
		return propertyLabelAndValue;
	}

    private Vector findPropertyLabelsInPropertyQualifierConditions() {
		Vector propertyLabels = new Vector();
	    for (int i=0; i<condition_data.size(); i++) {
			String line = (String) condition_data.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String type = (String) u.elementAt(0);
			if (type.compareTo("PropertyQualifier") == 0) {
				String property_code = (String) u.elementAt(1);
				String property_label = validator.getPropertyLabel(property_code);
				if (!propertyLabels.contains(property_label)) {
					propertyLabels.add(property_label);
				}
			}
		}
		return propertyLabels;
	}


    private Set findCodesMeetPropertyValueConditions(String property_label, String property_value) {
		Vector w = owlSPARQLUtils.findConceptsWithPropertyMatching(namedGraph, property_label,
					   property_value);
		Set condition_2_codes = new HashSet();
		for (int i2=0; i2<w.size(); i2++) {
			String line = (String) w.elementAt(i2);
			Vector u2 = StringUtils.parseData(line, '|');
			String condition_code = (String) u2.elementAt(0);
			condition_2_codes.add(condition_code);
		}
		return condition_2_codes;
	}

    private Set findCodesMeetPropertyQualifierConditions(String propertyLabel) {
        Vector qualifierCodes = new Vector();
        Vector qualifierValues = new Vector();
        for (int i=0; i<condition_data.size(); i++) {
			String line = (String) condition_data.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String type = (String) u.elementAt(0);
			if (type.compareTo("PropertyQualifier") == 0) {
				String property_code = (String) u.elementAt(1);
				String property_label = validator.getPropertyLabel(property_code);
				if (property_label.compareTo(propertyLabel) == 0) {
					qualifierCodes.add((String) u.elementAt(2));
					qualifierValues.add((String) u.elementAt(3));
				}
			}
		}
        String code = null;
        Vector w = owlSPARQLUtils.getConceptsWithPropertyAndQualifiersMatching(
			           namedGraph, code, propertyLabel,
	                   qualifierCodes, qualifierValues);

        Utils.saveToFile(headerConceptCode + "_prop_qual_" + propertyLabel + ".txt", w);
        Set condition_codes = new HashSet();
        for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String condition_code = (String) u.elementAt(0);
			condition_codes.add(condition_code);
		}
		return condition_codes;
	}

    public void runQA() {
        Set condition_codes = findCodesMeetPropertyConditions();
        Vector propertyLabelsOfPropQualConditions = findPropertyLabelsInPropertyQualifierConditions();
        for (int i=0; i<propertyLabelsOfPropQualConditions.size(); i++) {
			String property_label = (String) propertyLabelsOfPropQualConditions.elementAt(i);
			Set condition_2_codes = findCodesMeetPropertyQualifierConditions(property_label);
			condition_codes.retainAll(condition_2_codes);
		}
		Vector v = findPropertyLabelAndValueInPropertyValueConditions();
		for (int i=0; i<v.size(); i++) {
			String labelAndValue = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(labelAndValue, '|');
			String property_label = (String) u.elementAt(0);
			String property_value = (String) u.elementAt(1);
			Set condition_2_codes = findCodesMeetPropertyValueConditions(property_label, property_value);
    		condition_codes.retainAll(condition_2_codes);
		}

		ValueSetConstructor vsc = new ValueSetConstructor(this.serviceUrl, this.namedGraph, this.username, this.password);
		Vector value_set = vsc.generate_concept_in_subset(headerConceptCode);
        Utils.saveToFile(headerConceptCode + ".txt", value_set);
        Vector warnings  = new Vector();

        Vector value_set_codes = new Vector();
        int k = 0;
        for (int i=0; i<value_set.size(); i++) {
			String line = (String) value_set.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String value_set_code = (String) u.elementAt(1);
			value_set_codes.add(value_set_code);
			if (!condition_codes.contains(value_set_code)) {
				k++;
				warnings.add(line);
			}
		}
		setWarnings(warnings);

		Vector condition_code_vec = new Vector();
		Iterator it = condition_codes.iterator();
		while (it.hasNext()) {
			String condition_code = (String) it.next();
			condition_code_vec.add(condition_code);
		}
        missing_vec  = new Vector();
        for (int i=0; i<condition_code_vec.size(); i++) {
			String condition_code = (String) condition_code_vec.get(i);
			if (!value_set_codes.contains(condition_code) && !isRetired(condition_code)) {
				String condition_label = this.owlSPARQLUtils.getLabel(condition_code);
				missing_vec.add(condition_label + "|" + condition_code);
			}
		}
		Utils.saveToFile(headerConceptCode + "_missing_concepts.txt", missing_vec);
	}

    public void dumpReportInfo() {
		System.out.println(this.label);
		System.out.println(this.definition);
		System.out.println(this.footer);
		System.out.println(this.timestamp);
		System.out.println(this.definition);
		System.out.println("Conditions:");
		for (int i=0; i<conditions.size(); i++) {
			String condition = (String) conditions.elementAt(i);
			System.out.println("\t" + condition);
		}
	}

	public String getLabel() {
		return this.label;
	}

	public String getLabel(String code) {
		return owlSPARQLUtils.getLabel(code);
	}

	public void setHeaderConceptCode(String headerConceptCode) {
		this.headerConceptCode = headerConceptCode;
	}

	public String getVersion() {
		metadataUtils.dumpNameVersion2NamedGraphMap();
		return metadataUtils.getLatestVersion(NCI_THESAURUS);
	}

	public String getDefinition() {
		Vector propertyLabels = new Vector();
		propertyLabels.add("DEFINITION");
		Vector v = owlSPARQLUtils.getConceptsWithProperties(
			namedGraph, headerConceptCode, propertyLabels);
		Vector u = StringUtils.parseData((String) v.elementAt(0), '|');
		return (String) u.elementAt(3);
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setConditions(Vector conditions) {
		this.conditions = conditions;
	}

	public void setPageTitle(String pageTitle) {
		this.pageTitle = pageTitle;
	}

	public void setWarnings(Vector warnings) {
		this.warnings = warnings;
	}

	public void setMissings(Vector missing_vec) {
		this.missing_vec = missing_vec;
	}

	public void printBanner(PrintWriter out) {
		out.println("<div>");
		out.println("  <img");
		out.println("      src=\"https://nciterms.nci.nih.gov/ncitbrowser/images/evs-logo-swapped.gif\"");
		out.println("      alt=\"EVS Logo\"");
		out.println("      width=\"100%\"");
		out.println("      height=\"26\"");
		out.println("      border=\"0\"");
		out.println("      usemap=\"#external-evs\"");
		out.println("  />");
		out.println("  <map id=\"external-evs\" name=\"external-evs\">");
		out.println("    <area");
		out.println("        shape=\"rect\"");
		out.println("        coords=\"0,0,140,26\"");
		out.println("        href=\"/ncitbrowser/start.jsf\"");
		out.println("        target=\"_self\"");
		out.println("        alt=\"NCI Term Browser\"");
		out.println("    />");
		out.println("    <area");
		out.println("        shape=\"rect\"");
		out.println("        coords=\"520,0,941,26\"");
		out.println("        href=\"http://evs.nci.nih.gov/\"");
		out.println("        target=\"_blank\"");
		out.println("        alt=\"Enterprise Vocabulary Services\"");
		out.println("    />");
		out.println("  </map>");
		out.println("</div>");
	}

    public void printHeader(PrintWriter out, String pageTitle) {
		out.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">");
		out.println("<html xmlns:c=\"http://java.sun.com/jsp/jstl/core\">");
		out.println("<head>");
		out.println("<title>" + pageTitle + "</title>");
		out.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">");
		out.println("<style>");
		out.println("table {");
		out.println("    border-collapse: collapse;");
		out.println("}");
		out.println("table, td, th {");
		out.println("    border: 1px solid black;");
		out.println("}");
		out.println("</style>");
		out.println("</head>");
		out.println("");
		out.println("<body>");
		out.println("");
		out.println("");
		out.println(" <!-- nci banner<div class=\"ncibanner\"> -->");
		out.println("<div style='clear:both;margin-top:-5px;padding:8px;height:32px;color:white;background-color:#C31F40'>");
		out.println("  <a href=\"http://www.cancer.gov\" target=\"_blank\">");
		out.println("    <img");
		out.println("        src=\"https://nciterms.nci.nih.gov/ncitbrowser/images/banner-red.png\"");
		//out.println("        src=\"https://nciterms.nci.nih.gov/ncitbrowser/images/nci-banner-1.gif\"");
		out.println("        width=\"955\"");
		out.println("        height=\"39\"");
		out.println("        border=\"0\"");
		out.println("        alt=\"National Cancer Institute\"");
		out.println("    />");
		out.println("  </a>");
		out.println("</div>");
		out.println("<!-- end nci banner -->");
		out.println("");

		printBanner(out);

		out.println("");
		out.println("");
		out.println("<center>");


		out.println("<h1>Value Set QA Report</h1>");
		out.println("<p>");
		out.println("<h2>" + validator.getLabel(headerConceptCode) + "&nbsp;(" + headerConceptCode + ")"  + "</h2>");
		//String timestamp = StringUtils.getToday("MM-dd-yyyy");
		out.println("<h3>(" + timestamp + ")</h3>");
		out.println("</p>");
		out.println("</center>");
	}

	public String getHyperlink(String value) {
		//<a href="https://nciterms65.nci.nih.gov/ncitbrowser/pages/concept_details.jsf?dictionary=NCI_Thesaurus&version=20.11e&ns=ncit&code=C118325">C118325</a>
		//String str = "<a href=\"" + HYPERLINK_URL + value + "\">" + value + "</a>";
		//System.out.println(str);
		return value;
	}

    public void printConditions(PrintWriter out) {
		out.println("");
		out.println("");
		out.println("<center>");
		out.println("<h4>Table 1. Value Set Conditions</h4>");
		out.println("<table>");
        out.println("<tr>");
        out.println("<th align='left'>Condition</th>");
		out.println("</tr>");

		for (int i=0; i<conditions.size(); i++) {
			String condition = (String) conditions.elementAt(i);
			out.println("<tr>");
			out.println("<td>" + condition + "</td>");
    		out.println("</tr>");
		}
		out.println("</table>");
		out.println("</center>");
		out.println("<p></P>");
	}


    public void printTable(PrintWriter out,
        String tableLabel,
        Vector th_vec,
        Vector data_vec) {
		out.println("");

		printConditions(out);

		out.println("<div>");
		out.println("<center>");
		out.println("<h4>Table 2. " + tableLabel + "</h4>");

		if (data_vec.size() > 0) {
			out.println("<table>");
			out.println("<tr>");
			for (int i=0; i<th_vec.size(); i++) {
				String th = (String) th_vec.elementAt(i);
				out.println("<th align='left'>"+ th + "</th>");
			}
			out.println("</tr>");
			for (int i=0; i<data_vec.size(); i++) {
				String data = (String) data_vec.elementAt(i);
				out.println("<tr>");
				Vector u = StringUtils.parseData(data, '|');
				for (int j=0; j<u.size(); j++) {
					String value = (String) u.elementAt(j);
					out.println("<td>");
					String th = (String) th_vec.elementAt(j);
					if (th.endsWith("Code")) {
						out.println("<a href=\"" + HYPERLINK_URL + value + "\">" + value + "</a>");
					} else {
						out.println(value);
					}
					out.println("</td>");
				}
				out.println("</tr>");
			}
			out.println("</table>");
		} else {
			out.println("<center><p>" +"(None found.)" + "</p></center>");
		}

		out.println("<div>");
		out.println("<center>");
		out.println("<h4>Table 3. Concepts meet conditions but not found in the value set.</h4>");

		if (missing_vec.size() > 0) {
			out.println("<table>");
			out.println("<tr>");
			for (int i=0; i<th_vec.size(); i++) {
				String th = (String) th_vec.elementAt(i);
				out.println("<th align='left'>"+ th + "</th>");
			}
			out.println("</tr>");
			for (int i=0; i<missing_vec.size(); i++) {
				String data = (String) missing_vec.elementAt(i);
				out.println("<tr>");
				Vector u = StringUtils.parseData(data, '|');
				for (int j=0; j<u.size(); j++) {
					String value = (String) u.elementAt(j);
					out.println("<td>");
					String th = (String) th_vec.elementAt(j);
					if (th.endsWith("Code")) {
						out.println("<a href=\"" + HYPERLINK_URL + value + "\">" + value + "</a>");
					} else {
						out.println(value);
					}
					out.println("</td>");
				}
				out.println("</tr>");
			}
			out.println("</table>");
		} else {
			out.println("<center><p>" +"(None found.)" + "</p></center>");
		}

		out.println("</div>");
		out.println("");
	}

    public void printFooter(PrintWriter out) {
		out.println("</div>");
		out.println("<div>");
        out.println("<p></p>");
		out.println(headerConceptLabel + " - " + this.definition);
		out.println("<p></p>");

		if (checkOutBoundConceptInSubset) {
			Vector w = owlSPARQLUtils.getPropertiesByCode(this.namedGraph, this.headerConceptCode,
			"Concept_In_Subset");
			if (w == null || w.size() == 0) {
				 String str = "(Note: The concept has no Concept_In_Subset association pointing to any other concept.)";
			     out.println("<p>" + str + "</p>");
			} else {
                 for (int i=0; i<w.size(); i++) {
					 String line = (String) w.elementAt(i);
					 Vector u = StringUtils.parseData(line, '|');
					 String sourceCode = (String) u.elementAt(0);
					 String sourceLabel = (String) u.elementAt(1);
					 String targetCode = (String) u.elementAt(4);
					 String targetLabel = (String) u.elementAt(5);
					 StringBuffer buf = new StringBuffer();
					 buf.append(sourceLabel);
					 buf.append(" (" + sourceCode);
					 buf.append(") ");
					 buf.append(" -- [Concept_In_Subset] --> ");
					 buf.append(targetLabel);
					 buf.append(" (" + targetCode);
					 buf.append(") ");
					 out.println("<p>" + buf.toString() + "</p>");
				 }
			}
		}
        out.println("<p></p>");
        out.println("<center>");
		out.println(this.footer);
		out.println("</center><p></p>");
		out.println("</div>");

		out.println("</body>");
		out.println("</html>");
    }

	public void generate() {
	    OWLSPARQLUtils test = new OWLSPARQLUtils(serviceUrl, username, password);
        test.set_named_graph(namedGraph);
        runQA();

        String pageTitle = this.tableTitle;
        String outputfile = headerConceptLabel.replace(" ", "_");
        outputfile = outputfile + "_QA_" + StringUtils.getToday() + ".html";
        long ms = System.currentTimeMillis();
		PrintWriter pw = null;
		Vector th_vec = new Vector();
		th_vec.add("Name");
		th_vec.add("Code");
		try {
			pw = new PrintWriter(outputfile, "UTF-8");
            printHeader(pw, pageTitle);
			String tableLabel = "Concepts Do Not Meet Value Set Conditions";
            printTable(pw, tableLabel, th_vec, this.warnings);
            printFooter(pw);
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
}

