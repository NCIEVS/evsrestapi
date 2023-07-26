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

public class AxiomRetrieval {
    String owlfile = null;
    OWLScanner owlscanner = null;
    Vector owl_vec = null;

    HashMap synonymMap = null;
    HashMap sourceDefinitionMap = null;
    HashMap definitionMap = null;
    HashMap mapsToMap = null;

    String source = null;

    public AxiomRetrieval(String owlfile) {
		this.owlfile = owlfile;
		this.source = null;
		initialize();
    }

    public AxiomRetrieval(String owlfile, String source) {
		this.owlfile = owlfile;
		this.source = source;
		initialize();
    }

    public HashMap getSynonymMap() {
		return this.synonymMap;
	}

    public HashMap getSourceDefinitionMap() {
		return this.sourceDefinitionMap;
	}

    public HashMap getDefinitionMap() {
		return this.definitionMap;
	}

    public HashMap getMapsToMap() {
		return this.mapsToMap;
	}

	public void initialize() {
		owlscanner = new OWLScanner(owlfile);
		owl_vec = owlscanner.get_owl_vec();
		String propertyCode = "P325";
		Vector defs = owlscanner.extractAxiomData(propertyCode);
        sourceDefinitionMap = new HashMap();
        for (int i=0; i<defs.size(); i++) {
			String line = (String) defs.elementAt(i);
            if (source != null) {
				if (line.indexOf("P378$" + source) != -1) {
					Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(line, '|');
					String code = (String) u.elementAt(1);
					String alt_def = (String) u.elementAt(3);
					alt_def = decodeSpecialChar(alt_def);
					String source_str = (String) u.elementAt(4);
					Vector u2 = parseData(source_str, '$');
					String src = (String) u2.elementAt(1);
					if (src.compareTo(source) == 0) {
						sourceDefinitionMap.put(code, alt_def);
					}
				}
			} else {
				Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(line, '|');
				String code = (String) u.elementAt(1);
				String alt_def = (String) u.elementAt(3);
				alt_def = decodeSpecialChar(alt_def);
				sourceDefinitionMap.put(code, alt_def);
			}
		}
		propertyCode = "P97";
		defs = owlscanner.extractAxiomData(propertyCode);
        definitionMap = new HashMap();
        for (int i=0; i<defs.size(); i++) {
			String line = (String) defs.elementAt(i);
			if (line.indexOf("P378$NCI") != -1) {
				Vector u = parseData(line, '|');
				String code = (String) u.elementAt(1);
				String def = (String) u.elementAt(3);
				def = decodeSpecialChar(def);
				String source_str = (String) u.elementAt(4);
				Vector u2 = parseData(source_str, '$');
				String src = (String) u2.elementAt(1);
				if (src.compareTo("NCI") == 0) {
					definitionMap.put(code, def);
				}
			}
		}

		propertyCode = "P90";
		Vector syns = owlscanner.extractAxiomData(propertyCode);
		gov.nih.nci.evs.restapi.util.Utils.saveToFile("axiom_data.txt", syns);

		synonymMap = new HashMap();
		for (int i=0; i<syns.size(); i++) {
			String line = (String) syns.elementAt(i);
			Vector u = parseData(line, '|');
			String code = (String) u.elementAt(1);
			Synonym syn = null;

			//Arsenic Trioxide/Tretinoin Regimen|C198431|P90|All-trans Retinoic Acid|Arsenic Trioxide|P383$SY|P384$NCI
			try {
			    syn = string2Synonym(line);
			} catch (Exception ex) {
				ex.printStackTrace();
				System.out.println(line);
			}
			String termName = (String) u.elementAt(3);
			termName = decodeSpecialChar(termName);
			syn.setTermName(termName);
			Vector w = new Vector();
			if (synonymMap.containsKey(code)) {
				w = (Vector) synonymMap.get(code);
			}
			w.add(syn);
			synonymMap.put(code, w);
		}

		propertyCode = "P375";
		Vector map_vec = owlscanner.extractAxiomData(propertyCode);
		gov.nih.nci.evs.restapi.util.Utils.saveToFile("map_data.txt", syns);
		mapsToMap = new HashMap();
		for (int i=0; i<map_vec.size(); i++) {
			String line = (String) map_vec.elementAt(i);
			Vector u = parseData(line, '|');
			String code = (String) u.elementAt(1);
			MapToEntry entry = null;
			try {
			    entry = string2MapToEntry(line);
			} catch (Exception ex) {
				ex.printStackTrace();
				System.out.println(line);
			}
			String targetTerm = (String) u.elementAt(3);
			targetTerm = decodeSpecialChar(targetTerm);
			entry.setTargetTerm(targetTerm);
			Vector w = new Vector();
			if (mapsToMap.containsKey(code)) {
				w = (Vector) mapsToMap.get(code);
			}
			w.add(entry);
			mapsToMap.put(code, w);
		}
		owl_vec.clear();
	}

	public static String decodeSpecialChar(String line) {
		line = line.replace("&apos;", "'");
		line = line.replace("&amp;", "&");
		line = line.replace("&lt;", "<");
		line = line.replace("&gt;", ">");
		line = line.replace("&quot;", "\"");
		return line;
	}

    public static Synonym string2Synonym(String line) {
		Vector u = parseData(line, '|');
		String label = (String) u.elementAt(0);
		String code = (String) u.elementAt(1);
		String termName = (String) u.elementAt(3);
		String termGroup = null;
		String termSource = null;
		String sourceCode = null;
		String subSourceName = null;
		String subSourceCode = null;
		String qualifierCode = null;
        String qualifierName = null;
		if (u.size() > 4) {
			for (int i=4; i<u.size(); i++) {
				String str = (String) u.elementAt(i);
				Vector v = parseData(str, '$');
				qualifierCode = (String) v.elementAt(0);
				qualifierName = (String) v.elementAt(1);
				if (qualifierCode.compareTo("P383") == 0) {
					termGroup = qualifierName;
				} else if (qualifierCode.compareTo("P384") == 0) {
					termSource = qualifierName;
				} else if (qualifierCode.compareTo("P385") == 0) {
					sourceCode = qualifierName;
				} else if (qualifierCode.compareTo("P386") == 0) {
					subSourceName = qualifierName;
				}
			}
		}
		Synonym syn = new Synonym(
			code,
			label,
			termName,
			termGroup,
			termSource,
			sourceCode,
			subSourceName,
			subSourceCode);

		return syn;
	}

    public static MapToEntry string2MapToEntry(String line) {
		Vector u = parseData(line, '|');
		String preferredName = (String) u.elementAt(0);
		String code = (String) u.elementAt(1);
		String targetTerm = (String) u.elementAt(3);
		String targetCode = null;
		String relationshipToTarget = null;
		String targetTermType = null;
		String targetTerminology = null;
		String targetTerminologyVersion = null;

		if (u.size() > 4) {
			for (int i=4; i<u.size(); i++) {
				String str = (String) u.elementAt(i);
				Vector v = parseData(str, '$');
				String qualifierCode = (String) v.elementAt(0);
				String qualifierName = (String) v.elementAt(1);
				if (qualifierCode.compareTo("P393") == 0) {
					relationshipToTarget = qualifierName;
				} else if (qualifierCode.compareTo("P394") == 0) {
					targetTermType = qualifierName;
				} else if (qualifierCode.compareTo("P395") == 0) {
					targetCode = qualifierName;
				} else if (qualifierCode.compareTo("P396") == 0) {
					targetTerminology = qualifierName;
				} else if (qualifierCode.compareTo("P397") == 0) {
					targetTerminologyVersion = qualifierName;
				}
			}
		}

	    MapToEntry entry = new MapToEntry(
			code,
			preferredName,
			relationshipToTarget,
			targetCode,
			targetTerm,
			targetTermType,
			targetTerminology,
			targetTerminologyVersion);

		return entry;
	}

    public static boolean isMatched(String expectedValue, String value) {
		if (expectedValue == null) return true;
		if (expectedValue.compareTo("null") == 0) return true;
		if (value == null) return false;
		if (expectedValue.compareTo(value) == 0) return true;
		return false;
	}
	public String getMatchedSynonyms(String code, String source, String type, String subsource) {
		Vector w = new Vector();
		Vector syn_vec = (Vector) synonymMap.get(code);
		if (syn_vec == null || syn_vec.size() == 0) return "";
		for (int i=0; i<syn_vec.size(); i++) {
			Synonym syn = (Synonym) syn_vec.elementAt(i);
			boolean matched = true;
			if (!isMatched(source, syn.getTermSource())) {
				matched = false;
			}
			if (!isMatched(type, syn.getTermGroup())) {
				matched = false;
			}
			if (!isMatched(subsource, syn.getSubSourceName())) {
				matched = false;
			}
			if (matched) {
				String termName = syn.getTermName();
				if (!w.contains(termName)) {
					w.add(termName);
				}
			}
		}
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<w.size(); i++) {
			String t = (String) w.elementAt(i);
			buf.append(t).append(" || ");
		}
		String s = buf.toString();
		if (s.length() > 0) {
			s = s.substring(0, s.length()-4);
		}
		return s;
	}


    public String getDefinition(String code) {
		if (definitionMap.containsKey(code)) {
			return (String) definitionMap.get(code);
		}
		return "";
	}

    public String getSourceDefinition(String code) {
		if (sourceDefinitionMap.containsKey(code)) {
			return (String) sourceDefinitionMap.get(code);
		}
		return "";
	}

    public static Vector parseData(String line, char delimiter) {
		if(line == null) return null;
		Vector w = new Vector();
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<line.length(); i++) {
			char c = line.charAt(i);
			if (c == delimiter) {
				w.add(buf.toString());
				buf = new StringBuffer();
			} else {
				buf.append(c);
			}
		}
		w.add(buf.toString());
		return w;
    }

    public String getIndentation(int level) {
		String s = "";
		for (int i=0; i<level; i++) {
			s = s + "\t";
		}
		return s;
	}

	public Vector getSynonyms(String code, String source, String type, String subsource) {
		Vector w = new Vector();
		Vector syn_vec = (Vector) synonymMap.get(code);
		for (int i=0; i<syn_vec.size(); i++) {
			Synonym syn = (Synonym) syn_vec.elementAt(i);
			boolean matched = true;
			if (!isMatched(source, syn.getTermSource())) {
				matched = false;
			}
			if (!isMatched(type, syn.getTermGroup())) {
				matched = false;
			}
			if (!isMatched(subsource, syn.getSubSourceName())) {
				matched = false;
			}
			if (matched) {
				String termName = syn.getTermName();
				if (!w.contains(termName)) {
					w.add(syn);
				}
			}
		}
		return w;
	}

    public String findMatchedSubsourceNames(String code, String source, String type) {
		StringBuffer buf = new StringBuffer();
		Vector w = (Vector) synonymMap.get(code);
		for (int i=0; i<w.size(); i++) {
			Synonym syn = (Synonym) w.elementAt(i);
			boolean matched = true;
			if (syn.getTermSource() != null && syn.getTermSource().compareTo(source) == 0 &&
			    syn.getTermGroup() != null && syn.getTermGroup().compareTo(type) == 0) {
			    if (syn.getSubSourceName() != null) {
				    buf.append(syn.getSubSourceName()).append("|");
				}
			}
		}
		String t = buf.toString();
		if (t.length() > 0) {
			t = t.substring(0, t.length()-1);
		}
		return t;
	}

    public String findMatchedSourcecodes(String code, String source, String type) {
		StringBuffer buf = new StringBuffer();
		Vector w = (Vector) synonymMap.get(code);
		for (int i=0; i<w.size(); i++) {
			Synonym syn = (Synonym) w.elementAt(i);
			boolean matched = true;
			if (syn.getTermSource() != null && syn.getTermSource().compareTo(source) == 0 &&
			    syn.getTermGroup() != null && syn.getTermGroup().compareTo(type) == 0) {
			    if (syn.getSourceCode() != null) {
				    buf.append(syn.getSourceCode()).append("|");
				}
			}
		}
		String t = buf.toString();
		if (t.length() > 0) {
			t = t.substring(0, t.length()-1);
		}
		return t;
	}

	public Vector extractAxiomData(String propertyCode) {
		return owlscanner.extractAxiomData(propertyCode);
	}

	public void test(String code) {
		Vector w = (Vector) mapsToMap.get(code);
		for (int i=0; i<w.size(); i++) {
			MapToEntry entry = (MapToEntry) w.elementAt(i);
			System.out.println(entry.toJson());
		}
	}

	public static void main(String[] args) {
		long ms = System.currentTimeMillis();
		String owlfile = args[0];
		System.out.println("Initialization in progress ...");
		AxiomRetrieval axiomRetrieval = new AxiomRetrieval(owlfile);
		System.out.println("Initialization completed.");
		System.out.println("Total initialization time (ms): " + (System.currentTimeMillis() - ms));

	}
}

