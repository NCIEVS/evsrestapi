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

public class ValueSetConditionValidator {
    public static String[] CONDITION_TEMPLATES = null;//
    private static HashMap CONDITION2WordsHashMap = null;
    private static Vector KEYS = null;
    private static String DESCENDANT = "descendant";

    String serviceUrl = null;
    String namedGraph = null;
    String username = null;
    String password = null;
    String headerConceptCode = null;
    OWLSPARQLUtils owlPARQLUtils = null;
    private HashMap propertyCode2labelHashMap = null;
    private HashMap associationCode2labelHashMap = null;
    private Vector condition_data = null;
    private Vector conditions = null;

    static {
		KEYS = new Vector();
		KEYS.add("(Property_Code)");
		KEYS.add("(Property_Value)");
		KEYS.add("(Association_Code)");
		KEYS.add("(Qualifier_Code)");
		KEYS.add("(Header_Concept_Code)");
		KEYS.add("(Qualifier_Value)");

		CONDITION_TEMPLATES = new String[] {
			"Has a property Property_Name (Property_Code).",
			"Has a property Property_Name (Property_Code) with value equaling (Property_Value).",
			"Has a property Property_Name (Property_Code) with a qualifier Qualifier_Name (Qualifier_Code) and its value equaling (Qualifier_Value).",
			"Has an Association Association_Name (Association_Code) pointing to Header_Concept_Name (Header_Concept_Code) or any of its descendant."};

		CONDITION2WordsHashMap = new HashMap();
        for (int i=0; i<CONDITION_TEMPLATES.length; i++) {
			String t = CONDITION_TEMPLATES[i];
			if (t.endsWith(t)) {
				t = t.substring(0, t.length()-1);
			}
			Vector u = StringUtils.parseData(t, ' ');
			CONDITION2WordsHashMap.put(t, u);
		}
	}

	public Vector getConditionData() {
		return this.condition_data;
	}

	public Vector getConditions() {
		return this.conditions;
	}

	public Vector getSupportedProperties() {
		return this.owlPARQLUtils.getSupportedProperties(this.namedGraph);
	}

	public Vector getSupportedAssociations() {
		return this.owlPARQLUtils.getSupportedAssociations(this.namedGraph);
	}

	public String getLabel(String code) {
		return this.owlPARQLUtils.getLabel(code);
	}

	public ValueSetConditionValidator(String serviceUrl, String namedGraph, String username, String password) {
		this.serviceUrl = serviceUrl;
		this.username = username;
		this.password = password;
		this.namedGraph = namedGraph;
		this.owlPARQLUtils = new OWLSPARQLUtils(serviceUrl, username, password);
		this.owlPARQLUtils.set_named_graph(namedGraph);

		propertyCode2labelHashMap = new HashMap();
		Vector supportedProperties = getSupportedProperties();
		for (int i=0; i<supportedProperties.size(); i++) {
			String supportedProperty = (String) supportedProperties.elementAt(i);
			Vector u = StringUtils.parseData(supportedProperty, '|');
			String label = (String) u.elementAt(0);
			String code = (String) u.elementAt(1);
			propertyCode2labelHashMap.put(code, label);
		}

		associationCode2labelHashMap = new HashMap();
		Vector supportedAssociations = getSupportedAssociations();
		for (int i=0; i<supportedAssociations.size(); i++) {
			String supportedassociation = (String) supportedAssociations.elementAt(i);
			Vector u = StringUtils.parseData(supportedassociation, '|');
			String label = (String) u.elementAt(0);
			String code = (String) u.elementAt(1);
			associationCode2labelHashMap.put(code, label);
		}
	}

	public String getPropertyLabel(String code) {
		String label = (String) propertyCode2labelHashMap.get(code);
		return label;//.replace(" ", "_");
	}

	public String getAssociationLabel(String code) {
		return (String) associationCode2labelHashMap.get(code);
	}

    public static String[] get_CONDITION_TEMPLATES() {
		return CONDITION_TEMPLATES;
	}

    public static String get_CONDITION_TEMPLATE(String type) {
		if (type.compareTo("Property") == 0) {
			return CONDITION_TEMPLATES[0];
		} else if (type.compareTo("PropertyValue") == 0) {
			return CONDITION_TEMPLATES[1];
		} else if (type.compareTo("PropertyQualifier") == 0) {
			return CONDITION_TEMPLATES[2];
		} else if (type.compareTo("ValueSet") == 0) {
			return CONDITION_TEMPLATES[3];
		}
		return null;
	}

	public String substitute(String template, String header_concept_code, String property_code, String property_value, String qualifier_code,
	    String qualifier_value, String association_code) {
		if (property_code != null) {
			template = template.replace("(Property_Code)", "(" + property_code + ")");
			template = template.replace("Property_Name", getPropertyLabel(property_code));
		}

		if (property_value != null) {
			template = template.replace("(Property_Code)", "(" + property_code + ")");
			template = template.replace("(Property_Value)", property_value);
		}

		if (qualifier_code != null) {
			template = template.replace("(Qualifier_Code)", "(" + qualifier_code + ")");
			template = template.replace("Qualifier_Name", getPropertyLabel(qualifier_code));
		}

		if (association_code != null) {
			template = template.replace("(Association_Code)", "(" + association_code + ")");
			template = template.replace("Association_Name", getAssociationLabel(association_code));
		}

		if (qualifier_value != null) {
			template = template.replace("(Qualifier_Value)", qualifier_value);
		}

		if (header_concept_code != null) {
			template = template.replace("(Header_Concept_Code)", "(" + header_concept_code + ")");
			template = template.replace("Header_Concept_Name", getLabel(header_concept_code));
		}
		return template;
	}

    public String createPropertyCondition(String property_code) {
        String template = get_CONDITION_TEMPLATE("Property");
        String condition = substitute(template, null, property_code, null, null, null, null);
        return condition;
	}

    public String createPropertyValueCondition(String property_code, String property_value) {
        String template = get_CONDITION_TEMPLATE("PropertyValue");
        String condition = substitute(template, null, property_code, property_value, null, null, null);
        return condition;
	}

    public String createPropertyQualifierCondition(String property_code, String qualifier_code, String qualifier_value) {
        String template = get_CONDITION_TEMPLATE("PropertyQualifier");
        String condition = substitute(template, null, property_code, null, qualifier_code, qualifier_value, null);
        return condition;
	}

    public String createValueSetCondition(String header_concept_code, String association_code) {
        String template = get_CONDITION_TEMPLATE("ValueSet");
        String condition = substitute(template, header_concept_code, null, null, null, null, association_code);
        return condition;
	}

    public void setConditions(Vector w) {
		this.condition_data = w;
		this.conditions = new Vector();
		for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String type = (String) u.elementAt(0);
			if (type.compareTo("Property") == 0) {
				String property_code = (String) u.elementAt(1);
				String s = createPropertyCondition(property_code);
				this.conditions.add(s);
			} else if (type.compareTo("PropertyValue") == 0) {
				String property_code = (String) u.elementAt(1);
				String property_value = (String) u.elementAt(2);
				String s = createPropertyValueCondition(property_code, property_value);
				this.conditions.add(s);

			} else if (type.compareTo("PropertyQualifier") == 0) {
				String property_code = (String) u.elementAt(1);
				String qualifier_code = (String) u.elementAt(2);
				String qualifier_value = (String) u.elementAt(3);
				String s = createPropertyQualifierCondition(property_code, qualifier_code, qualifier_value);
				this.conditions.add(s);

			} else if (type.compareTo("ValueSet") == 0) {
				String association_code = (String) u.elementAt(1);
				String header_concept_code = (String) u.elementAt(2);
                String s = createValueSetCondition(header_concept_code, association_code);
                this.conditions.add(s);
                this.headerConceptCode = (String) u.elementAt(2);;
			}
		}
	}

	public String getHeaderConceptCode() {
		return headerConceptCode;
	}
}