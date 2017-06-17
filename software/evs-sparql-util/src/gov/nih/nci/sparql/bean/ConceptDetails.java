package gov.nih.nci.sparql.bean;


import com.google.gson.*;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.XStream;
import gov.nih.nci.sparql.common.*;
import gov.nih.nci.sparql.util.*;
import java.io.*;
import java.net.*;
import java.util.*;


/**
 * <!-- LICENSE_TEXT_START -->
 * Copyright 2008-2017 NGIS. This software was developed in conjunction
 * with the National Cancer Institute, and so to the extent government
 * employees are co-authors, any rights in such works shall be subject
 * to Title 17 of the United States Code, section 105.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *   1. Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the disclaimer of Article 3,
 *      below. Redistributions in binary form must reproduce the above
 *      copyright notice, this list of conditions and the following
 *      disclaimer in the documentation and/or other materials provided
 *      with the distribution.
 *   2. The end-user documentation included with the redistribution,
 *      if any, must include the following acknowledgment:
 *      "This product includes software developed by NGIS and the National
 *      Cancer Institute."   If no such end-user documentation is to be
 *      included, this acknowledgment shall appear in the software itself,
 *      wherever such third-party acknowledgments normally appear.
 *   3. The names "The National Cancer Institute", "NCI" and "NGIS" must
 *      not be used to endorse or promote products derived from this software.
 *   4. This license does not authorize the incorporation of this software
 *      into any third party proprietary programs. This license does not
 *      authorize the recipient to use any trademarks owned by either NCI
 *      or NGIS
 *   5. THIS SOFTWARE IS PROVIDED "AS IS," AND ANY EXPRESSED OR IMPLIED
 *      WARRANTIES, (INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *      OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE) ARE
 *      DISCLAIMED. IN NO EVENT SHALL THE NATIONAL CANCER INSTITUTE,
 *      NGIS, OR THEIR AFFILIATES BE LIABLE FOR ANY DIRECT, INDIRECT,
 *      INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *      BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *      LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *      CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *      LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *      ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *      POSSIBILITY OF SUCH DAMAGE.
 * <!-- LICENSE_TEXT_END -->
 */

/**
 * @author EVS Team
 * @version 1.0
 *
 * Modification history:
 *     Initial implementation kim.ong@ngc.com
 *
 */


public class ConceptDetails
{
	public static final String XML_DECLARATION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

// Variable declaration
	private String code;
	private String label;
	private String displayName;
	private List synonyms;
	private List conceptStatus;
	private String neoplasticStatus;
	private List semanticTypes;
	private List superconcepts;
	private List subconcepts;

// Default constructor
	public ConceptDetails() {
	}

// Constructor
	public ConceptDetails(
		String code,
		String label,
		String displayName,
		List synonyms,
		List conceptStatus,
		String neoplasticStatus,
		List semanticTypes,
		List superconcepts,
		List subconcepts) {

		this.code = code;
		this.label = label;
		this.displayName = displayName;
		this.synonyms = synonyms;
		this.conceptStatus = conceptStatus;
		this.neoplasticStatus = neoplasticStatus;
		this.semanticTypes = semanticTypes;
		this.superconcepts = superconcepts;
		this.subconcepts = subconcepts;
	}

    public static List vector2List(Vector v) {
		if (v == null) return null;
		return new ArrayList(v);
	}

	public ConceptDetails(
		Vector label_vec,
		Vector property_vec,
		Vector synonym_vec,
		Vector superclass_vec,
		Vector subclass_vec) {

		ParserUtils parser = new ParserUtils();

        HashMap prop_hmap = parser.parseProperties(property_vec);
        Vector code_vec = (Vector) prop_hmap.get("code");
        String code = (String) code_vec.elementAt(0);
        String label = parser.parseLabel(label_vec);
        //String displayName = (String) prop_hmap.get("Display_Name");
        Vector displayName_vec = (Vector) prop_hmap.get("Display_Name");
        String displayName = null;
        if (displayName_vec != null) {
        	displayName = (String) displayName_vec.elementAt(0);
		}
        List synonyms = parser.getSynonyms(synonym_vec);
        Vector conceptStatus_vec = (Vector) prop_hmap.get("Concept_Status");

        Vector neoplasticStatus_vec = (Vector) prop_hmap.get("Neoplastic_Status");
        String neoplasticStatus = null;
        if (neoplasticStatus_vec != null) {
        	neoplasticStatus = (String) neoplasticStatus_vec.elementAt(0);
		}

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
		this.displayName = displayName;
		this.synonyms =synonyms;

		this.conceptStatus = vector2List(conceptStatus_vec);
		this.neoplasticStatus = neoplasticStatus;
		this.semanticTypes = vector2List(semanticType_vec);
		this.superconcepts = superconcepts;
		this.subconcepts = subconcepts;
	}

// Set methods
	public void setCode(String code) {
		this.code = code;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public void setSynonyms(List synonyms) {
		this.synonyms = synonyms;
	}

	public void setConceptStatus(List conceptStatus) {
		this.conceptStatus = conceptStatus;
	}

	public void setNeoplasticStatus(String neoplasticStatus) {
		this.neoplasticStatus = neoplasticStatus;
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


// Get methods
	public String getCode() {
		return this.code;
	}

	public String getLabel() {
		return this.label;
	}

	public String getDisplayName() {
		return this.displayName;
	}

	public List getSynonyms() {
		return this.synonyms;
	}

	public List getConceptStatus() {
		return this.conceptStatus;
	}

	public String getNeoplasticStatus() {
		return this.neoplasticStatus;
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


	public String toXML() {
		XStream xstream_xml = new XStream(new DomDriver());
		String xml = xstream_xml.toXML(this);
		xml = StringUtils.escapeDoubleQuotes(xml);
		xml = XML_DECLARATION + "\n" + xml;
		xml = StringUtils.removePackageNames("gov.nih.nci.sparql.bean", xml);
        return xml;
	}

	public String toJson() {
		//String xml = toXML();
		//return new XMLJSONConverter().xml2JSON(xml);

		JsonParser parser = new JsonParser();
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);

		//return new Gson().toJson(this);
	}

    public static void main(String[] args) {
		Vector label_vec = Utils.readFile("C3173_label.txt");
		Vector property_vec = Utils.readFile("C3173_properties.txt");
		Vector synonym_vec = Utils.readFile("C3173_synonyms.txt");
		Vector superclass_vec = Utils.readFile("C3173_superclasses.txt");
		Vector subclass_vec = Utils.readFile("C3173_subclasses.txt");

	    ConceptDetails cd = new ConceptDetails(
			label_vec,
			property_vec,
			synonym_vec,
			superclass_vec,
			subclass_vec);
		System.out.println(cd.toXML());
	}
}
