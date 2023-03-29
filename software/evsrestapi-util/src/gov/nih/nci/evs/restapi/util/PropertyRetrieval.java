package gov.nih.nci.evs.restapi.util;
import gov.nih.nci.evs.restapi.bean.*;

import java.io.*;
import java.util.*;

/**
 * <!-- LICENSE_TEXT_START -->
 * Copyright 2011, MSC. This software was developed in conjunction
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
 *      "This product includes software developed by MSC and the National
 *      Cancer Institute."   If no such end-user documentation is to be
 *      included, this acknowledgment shall appear in the software itself,
 *      wherever such third-party acknowledgments normally appear.
 *   3. The names "The National Cancer Institute", "NCI" and "MSC" must
 *      not be used to endorse or promote products derived from this software.
 *   4. This license does not authorize the incorporation of this software
 *      into any third party proprietary programs. This license does not
 *      authorize the recipient to use any trademarks owned by either NCI
 *      or MSC.
 *   5. THIS SOFTWARE IS PROVIDED "AS IS," AND ANY EXPRESSED OR IMPLIED
 *      WARRANTIES, (INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *      OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE) ARE
 *      DISCLAIMED. IN NO EVENT SHALL THE NATIONAL CANCER INSTITUTE,
 *      MSC, OR THEIR AFFILIATES BE LIABLE FOR ANY DIRECT, INDIRECT,
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
 *     Initial implementation ongki@nih.gov
 *
 */

public class PropertyRetrieval {
    String owlfile = null;
    OWLScanner owlscanner = null;
    Vector owl_vec = null;
    HashSet retired_concepts = null;

    HashMap propertyMap = null;
    Vector properties = null;
    Vector annotationProperties = null;
    HashMap annotationPropertyCode2LabelMap = null;

    Vector objectProperties = null;
    HashMap objectPropertyCode2LabelMap = null;

    public PropertyRetrieval(String owlfile) {
		this.owlfile = owlfile;
    }

    public PropertyRetrieval(String owlfile, Vector properties) {
		this.owlfile = owlfile;
		this.properties = properties;
		initialize();
    }

    public HashMap getPropertyMap() {
		return this.propertyMap;
	}

    public void setProeprties(Vector properties){
		this.properties = properties;
	}

	public Vector getAnnotationProperties() {
		return this.annotationProperties;
	}

	public String getAnnotationPropertyLabel(String propertyCode) {
		return (String) annotationPropertyCode2LabelMap.get(propertyCode);
	}

	public String getObjectPropertyLabel(String propertyCode) {
		return (String) objectPropertyCode2LabelMap.get(propertyCode);
	}

	public void initialize() {
		if (properties == null) {
			System.out.println("No property is specified -- program aborts.");
			return;
		}

		owlscanner = new OWLScanner(owlfile);

		annotationProperties = owlscanner.extractAnnotationProperties(owlscanner.get_owl_vec());
		annotationPropertyCode2LabelMap = new HashMap();
		for (int i=0; i<annotationProperties.size(); i++) {
			String line = (String) annotationProperties.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			annotationPropertyCode2LabelMap.put((String) u.elementAt(0),(String) u.elementAt(1));
		}

		objectProperties = owlscanner.extractObjectProperties(owlscanner.get_owl_vec());
		objectPropertyCode2LabelMap = new HashMap();
		for (int i=0; i<objectProperties.size(); i++) {
			String line = (String) objectProperties.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			objectPropertyCode2LabelMap.put((String) u.elementAt(0),(String) u.elementAt(1));
		}

		retired_concepts = new HashSet();
		owl_vec = owlscanner.get_owl_vec();
		propertyMap = new HashMap();
		for (int i=0; i<properties.size(); i++) {
			String propertyCode = (String) properties.elementAt(i);
			HashMap hmap = new HashMap();
			Vector w = null;
			if (propertyCode.startsWith("A")) {
				w = owlscanner.extractAssociations(owl_vec, propertyCode);
			} else if (propertyCode.startsWith("P")) {
				w = owlscanner.extractProperties(owl_vec, propertyCode);
			} else if (propertyCode.startsWith("R")) {
				w = owlscanner.extractOWLRestrictions(owl_vec, propertyCode);
			}

			if (w != null) {
				for (int j=0; j<w.size(); j++) {
					String line = (String) w.elementAt(j);
					Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(line, '|');
					String code = (String) u.elementAt(0);
					String value = AxiomRetrieval.decodeSpecialChar((String) u.elementAt(2));
					Vector w1 = new Vector();
					if (hmap.containsKey(code)) {
						w1 = (Vector) hmap.get(code);
					}
					if (!w1.contains(value)) {
						w1.add(value);
					}
					hmap.put(code, w1);
				}
				propertyMap.put(propertyCode, hmap);
			}
		}

		Vector w3 = owlscanner.extractProperties(owl_vec, "P310");
        for (int i=0; i<w3.size(); i++) {
			String line = (String) w3.elementAt(i);
			Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(line, '|');
			String code = (String) u.elementAt(0);
			String status = (String) u.elementAt(2);
			if (status.compareTo("Retired_Concept") == 0) {
				retired_concepts.add(code);
			}
		}
		owl_vec.clear();
	}

	public HashSet get_retired_concepts() {
		return this.retired_concepts;
	}

	public HashMap getPropertyHashMap(String propertyCode) {
		if (!propertyMap.containsKey(propertyCode)) {
			return null;
		}
		HashMap hmap = (HashMap) propertyMap.get(propertyCode);
		return hmap;
	}

	public Vector getPropertyValues(String propertyCode, String code) {
		if (!propertyMap.containsKey(propertyCode)) {
			return null;
		}
		HashMap hmap = (HashMap) propertyMap.get(propertyCode);
		if (!hmap.containsKey(code)) {
			return null;
		}
		return (Vector) hmap.get(code);
	}

	public String toDelimited(Vector v, char delim) {
		if (v == null) return null;
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			buf.append(t).append(delim);
		}
		String s = buf.toString();
		return s.substring(0, s.length()-1);
	}

	public void exportPropertyData(String propertyCode) {
		if (!propertyMap.containsKey(propertyCode)) return;
		HashMap hmap = (HashMap) propertyMap.get(propertyCode);
		Vector w0 = new Vector();
		Iterator it = hmap.keySet().iterator();
		while (it.hasNext()) {
			String code = (String) it.next();
			Vector w = (Vector) hmap.get(code);
			for (int i=0; i<w.size(); i++) {
				String value = (String) w.elementAt(i);
				w0.add(code + "|" + propertyCode + "|" + value);
			}
		}
		Utils.saveToFile(propertyCode + ".txt", w0);
	}

	public static void main(String[] args) {
		long ms = System.currentTimeMillis();
		String owlfile = args[0];

		Vector properties = new Vector();
		System.out.println("Initialization in progress ...");
		properties.add("P108");
		properties.add("A5");
		properties.add("R108");

		PropertyRetrieval propertyRetrieval = new PropertyRetrieval(owlfile, properties);
		System.out.println("Initialization completed.");

		HashMap propertyMap = propertyRetrieval.getPropertyMap();

		Iterator it = propertyMap.keySet().iterator();
		while (it.hasNext()) {
			String propertyCode = (String) it.next();
			String propertyLabel = null;
			HashMap hmap = (HashMap) propertyMap.get(propertyCode);
			if (propertyCode.startsWith("R")) {
				propertyLabel = propertyRetrieval.getObjectPropertyLabel(propertyCode);
			} else {
				propertyLabel = propertyRetrieval.getAnnotationPropertyLabel(propertyCode);
			}
		    System.out.println(propertyLabel + " (" + propertyCode + "): " + hmap.keySet().size());
		}

		propertyRetrieval.exportPropertyData("R108");

		System.out.println("Total initialization time (ms): " + (System.currentTimeMillis() - ms));

	}
}

