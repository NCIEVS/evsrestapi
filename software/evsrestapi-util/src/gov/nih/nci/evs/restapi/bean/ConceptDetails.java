package gov.nih.nci.evs.restapi.bean;

import gov.nih.nci.evs.restapi.common.*;

import gov.nih.nci.evs.restapi.util.*;

import com.google.gson.*;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.XStream;

import java.io.*;
import java.net.*;
import java.util.*;

public class ConceptDetails
{

// Variable declaration
	private String code;
	private String label;
	private String preferredName;
	private String displayName;
	private Boolean isMainType;
	private Boolean isSubtype;
	private Boolean isDiseaseStage;
	private Boolean isDiseaseGrade;
	private Boolean isDisease;
	private Boolean isBiomarker;
	private Boolean isReferenceGene;
	private List mainMenuAncestors;
	private List definitions;
	private List synonyms;
	private List conceptStatus;
	private List semanticTypes;
	private List superconcepts;
	private List subconcepts;
	private List additionalProperties;

// Default constructor
	public ConceptDetails() {
	}

// Constructor
	public ConceptDetails(
		String code,
		String label,
		String preferredName,
		String displayName,
		Boolean isMainType,
		Boolean isSubtype,
		Boolean isDiseaseStage,
		Boolean isDiseaseGrade,
		Boolean isDisease,
		List mainMenuAncestors,
		List definitions,
		List synonyms,
		List conceptStatus,
		List semanticTypes,
		List superconcepts,
		List subconcepts,
		List additionalProperties) {

		this.code = code;
		this.label = label;
		this.preferredName = preferredName;
		this.displayName = displayName;
		this.isMainType = isMainType;
		this.isSubtype = isSubtype;
		this.isDiseaseStage = isDiseaseStage;
		this.isDiseaseGrade = isDiseaseGrade;
		this.isDisease = isDisease;
		this.mainMenuAncestors = mainMenuAncestors;
		this.definitions = definitions;
		this.synonyms = synonyms;
		this.conceptStatus = conceptStatus;
		this.semanticTypes = semanticTypes;
		this.superconcepts = superconcepts;
		this.subconcepts = subconcepts;
		this.additionalProperties = additionalProperties;
	}

    public static List vector2List(Vector v) {
		if (v == null) return null;
		return new ArrayList(v);
	}

// Set methods
	public void setCode(String code) {
		this.code = code;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public void setPreferredName(String preferredName) {
		this.preferredName = preferredName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public void setIsMainType(Boolean isMainType) {
		this.isMainType = isMainType;
	}

	public void setIsSubtype(Boolean isSubtype) {
		this.isSubtype = isSubtype;
	}

	public void setIsDiseaseStage(Boolean isDiseaseStage) {
		this.isDiseaseStage = isDiseaseStage;
	}

	public void setIsDiseaseGrade(Boolean isDiseaseGrade) {
		this.isDiseaseGrade = isDiseaseGrade;
	}

	public void setIsDisease(Boolean isDisease) {
		this.isDisease = isDisease;
	}

	public void setmainMenuAncestors(List mainMenuAncestors) {
		this.mainMenuAncestors = mainMenuAncestors;
	}

	public void setDefinitions(List definitions) {
		this.definitions = definitions;
	}

	public void setSynonyms(List synonyms) {
		this.synonyms = synonyms;
	}

	public void setConceptStatus(List conceptStatus) {
		this.conceptStatus = conceptStatus;
	}

	public void setSemanticTypes(List semanticTypes) {
		this.semanticTypes = semanticTypes;
	}

	public void setSuperconcepts(List superconcepts) {
		this.superconcepts = superconcepts;
	}

	public void setSubconcepts(List subconcepts) {
		this.subconcepts = subconcepts;
	}

	public void setAdditionalProperties(List additionalProperties) {
		this.additionalProperties = additionalProperties;
	}


// Get methods
	public String getCode() {
		return this.code;
	}

	public String getLabel() {
		return this.label;
	}

	public String getPreferredName() {
		return this.preferredName;
	}

	public String getDisplayName() {
		return this.displayName;
	}

	public Boolean getIsMainType() {
		return this.isMainType;
	}

	public Boolean getIsSubtype() {
		return this.isSubtype;
	}

	public Boolean getIsDiseaseStage() {
		return this.isDiseaseStage;
	}

	public Boolean getIsDiseaseGrade() {
		return this.isDiseaseGrade;
	}

	public Boolean getIsDisease() {
		return this.isDisease;
	}
	public List getmainMenuAncestors() {
		return this.mainMenuAncestors;
	}

	public List getDefinitions() {
		return this.definitions;
	}

	public List getSynonyms() {
		return this.synonyms;
	}

	public List getConceptStatus() {
		return this.conceptStatus;
	}

	public List getSemanticTypes() {
		return this.semanticTypes;
	}

	public List getSuperconcepts() {
		return this.superconcepts;
	}

	public List getSubconcepts() {
		return this.subconcepts;
	}

	public List getAdditionalProperties() {
		return this.additionalProperties;
	}

	public ConceptDetails(
		Vector label_vec,
		Vector property_vec,
		Vector property_qualifier_vec,
		Vector synonym_vec,
		Vector superclass_vec,
		Vector subclass_vec,
		List mainMenuAncestors,
	    Boolean isMainType,
	    Boolean isSubtype,
	    Boolean isDiseaseStage,
	    Boolean isDiseaseGrade,
	    Boolean isDisease,
	    Boolean isBiomarker,
	    Boolean isReferenceGene
		) {
		ParserUtils parser = new ParserUtils();
        HashMap prop_hmap = parser.parseProperties(property_vec);
        Vector code_vec = (Vector) prop_hmap.get("code");

		String code = null;
		if (code_vec != null) {
				code = (String) code_vec.elementAt(0);
		}
        String label = parser.parseLabel(label_vec);
        Vector displayNames = (Vector) prop_hmap.get("Display_Name");
        if (displayNames != null) {
        	displayName = (String) displayNames.elementAt(0);
		}
        Vector preferredName_vec = (Vector) prop_hmap.get("Preferred_Name");
        String preferredName = null;
        if (preferredName_vec != null) {
        	preferredName = (String) preferredName_vec.elementAt(0);
		}

        Vector displayName_vec = (Vector) prop_hmap.get("Display_Name");
        if (displayName_vec != null) {
        	displayName = (String) displayName_vec.elementAt(0);
		}

        List synonyms = parser.getSynonyms(synonym_vec);
		List list = new ParserUtils().getSynonyms(synonym_vec);

        Vector conceptStatus_vec = (Vector) prop_hmap.get("Concept_Status");
        Vector semanticType_vec = (Vector) prop_hmap.get("Semantic_Type");

        HashMap superclasses_hmap = parser.parseSuperclasses(superclass_vec);
        List superconcepts = new ArrayList();
        Iterator it = superclasses_hmap.keySet().iterator();
        while (it.hasNext()) {
			String cd = (String) it.next();
			Vector w = (Vector) superclasses_hmap.get(cd);
			String name = null;
			if (w != null) {
				name = (String) w.elementAt(0);
			}
			superconcepts.add(new Superconcept(cd, name));
		}

        HashMap subclasses_hmap = parser.parseSubclasses(subclass_vec);
        List subconcepts = new ArrayList();
        it = subclasses_hmap.keySet().iterator();
        while (it.hasNext()) {
			String cd = (String) it.next();
			Vector w = (Vector) subclasses_hmap.get(cd);
			String name = null;
			if (w != null) {
				name = (String) w.elementAt(0);
			}
			subconcepts.add(new Subconcept(cd, name));
		}
		this.code = code;
		this.label = label;
		this.preferredName = preferredName;
		this.displayName = displayName;
		this.synonyms =synonyms;

		this.conceptStatus = vector2List(conceptStatus_vec);
		this.semanticTypes = vector2List(semanticType_vec);
		this.superconcepts = superconcepts;
		this.subconcepts = subconcepts;

		List definitions = new ArrayList();
        Vector def_vec = parser.filterPropertyQualifiers(property_qualifier_vec, Constants.DEFINITION);//) { //type: FULL_SYN, DEFINITION, ALT_DEFINITION
        definitions = parser.getDefinitions(def_vec);
		Vector alt_def_vec = parser.filterPropertyQualifiers(property_qualifier_vec, Constants.ALT_DEFINITION);//) { //type: FULL_SYN, DEFINITION, ALT_DEFINITION
		List alt_definitions = parser.getDefinitions(alt_def_vec);//) { //type: FULL_SYN, DEFINITION, ALT_DEFINITION
		if (alt_definitions != null && alt_definitions.size() > 0) {
			for (int j=0; j<alt_definitions.size(); j++) {
				Definition def = (Definition) alt_definitions.get(j);
				definitions.add(def);
			}
	    }

		this.definitions = definitions;
		this.additionalProperties = parser.getAdditionalProperties(property_vec);
		new SortUtils().quickSort(additionalProperties);

        this.mainMenuAncestors = mainMenuAncestors;
	    this.isMainType = isMainType;
	    this.isSubtype = isSubtype;
	    this.isDiseaseStage = isDiseaseStage;
	    this.isDiseaseGrade	= isDiseaseGrade;
	    this.isDisease = isDisease;
	    this.isBiomarker = isBiomarker;
	    this.isReferenceGene = isReferenceGene;
	}

	public String toXML() {
		XStream xstream_xml = new XStream(new DomDriver());
		String xml = xstream_xml.toXML(this);
		xml = StringUtils.escapeDoubleQuotes(xml);
		xml = Constants.XML_DECLARATION + "\n" + xml;
		xml = StringUtils.removePackageNames(Constants.EVSRESTAPI_BEAN, xml);
        return xml;
	}

	public String toJson() {
		JsonParser parser = new JsonParser();
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
	}
}
