package gov.nih.nci.evs.restapi.util;

import gov.nih.nci.evs.restapi.model.*;
import gov.nih.nci.evs.restapi.bean.EditAction;

import java.io.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.*;
import java.nio.charset.Charset;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;

public class EditHistoryUtils {
	public static String DEFAULT_TERMINOLOGY = "ncit";
	public EditHistoryUtils() {

	}

    public static int toHashCode(ConceptDetails cd) {
		if (cd == null) return -1;
		int hashcode = 0;
		hashcode = hashcode + cd.getCode().hashCode()
		                    + cd.getName().hashCode()
		                    + cd.getTerminology().hashCode()
		                    + cd.getVersion().hashCode();

        if (cd.getSynonyms() != null) {
			hashcode = hashcode + getSynonymHashCode(cd.getSynonyms());
		}
        if (cd.getProperties() != null) {
			hashcode = hashcode + getPropertyHashCode(cd.getProperties());
		}
        if (cd.getRoles() != null) {
			hashcode = hashcode + getRoleHashCode(cd.getRoles());
		}
        if (cd.getInverseRoles() != null) {
			hashcode = hashcode + getInverseRoleHashCode(cd.getInverseRoles());
		}
        if (cd.getAssociations() != null) {
			hashcode = hashcode + getAssociationHashCode(cd.getAssociations());
		}
        if (cd.getInverseAssociations() != null) {
			hashcode = hashcode + getInverseAssociationHashCode(cd.getInverseAssociations());
		}
        if (cd.getDefinitions() != null) {
			hashcode = hashcode + getDefinitionHashCode(cd.getDefinitions());
		}
        if (cd.getSuperclasses() != null) {
			hashcode = hashcode + getSuperclassHashCode(cd.getSuperclasses());
		}
	    return hashcode;
	}

	public static Vector getValueSetInJSON(ValueSet vs) {
		Vector w = new Vector();
		List<String> codes = vs.getCodes();
		for (int i=0; i<codes.size(); i++) {
			String code = (String) codes.get(i);
			w.add(EVSRESTAPIClient.getConceptDetailsInJSON(DEFAULT_TERMINOLOGY, code));
		}
		return w;
	}

    public static int getDefinitionHashCode(List<Definition> definitions) {
		int hashcode = 0;
		if (definitions == null || definitions.size() == 0) return 0;
		for (int i=0; i<definitions.size(); i++) {
			Definition prop = (Definition) definitions.get(i);
			if (prop.getType() != null) {
				hashcode = hashcode + prop.getType().hashCode();
			}
			if (prop.getDefinition() != null) {
				hashcode = hashcode + prop.getDefinition().hashCode();
			}
			if (prop.getSource() != null) {
				hashcode = hashcode + prop.getSource().hashCode();
			}
		}
		return hashcode;
	}

    public static int getSuperclassHashCode(List<Superclass> superclasses) {
		int hashcode = 0;
		if (superclasses == null || superclasses.size() == 0) return 0;
		for (int i=0; i<superclasses.size(); i++) {
			Superclass prop = (Superclass) superclasses.get(i);
			if (prop.getCode() != null) {
				hashcode = hashcode + prop.getCode().hashCode();
			}
			if (prop.getName() != null) {
				hashcode = hashcode + prop.getName().hashCode();
			}
		}
		return hashcode;
	}

    public static int getSynonymHashCode(List<Synonym> synonyms) {
		int hashcode = 0;
		if (synonyms == null || synonyms.size() == 0) return 0;
		for (int i=0; i<synonyms.size(); i++) {
			Synonym syn = (Synonym) synonyms.get(i);
			if (syn.getName() != null) {
				hashcode = hashcode + syn.getName().hashCode();
			}
			if (syn.getType() != null) {
				hashcode = hashcode + syn.getType().hashCode();
			}
			if (syn.getCode() != null) {
				hashcode = hashcode + syn.getCode().hashCode();
			}
			if (syn.getTermGroup() != null) {
				hashcode = hashcode + syn.getTermGroup().hashCode();
			}
			if (syn.getTermGroup() != null) {
				hashcode = hashcode + syn.getTermGroup().hashCode();
			}
			if (syn.getSource() != null) {
				hashcode = hashcode + syn.getSource().hashCode();
			}
			if (syn.getSubSource() != null) {
				hashcode = hashcode + syn.getSubSource().hashCode();
			}
		}
		return hashcode;
	}

    public static int getRoleHashCode(List<Role> roles) {
		int hashcode = 0;
		if (roles == null || roles.size() == 0) return 0;
		for (int i=0; i<roles.size(); i++) {
			Role prop = (Role) roles.get(i);
			if (prop.getType() != null) {
				hashcode = hashcode + prop.getType().hashCode();
			}
			if (prop.getRelatedCode() != null) {
				hashcode = hashcode + prop.getRelatedCode().hashCode();
			}
		}
		return hashcode;
	}

    public static int getInverseRoleHashCode(List<InverseRole> inverseroles) {
		int hashcode = 0;
		if (inverseroles == null || inverseroles.size() == 0) return 0;
		for (int i=0; i<inverseroles.size(); i++) {
			InverseRole prop = (InverseRole) inverseroles.get(i);
			if (prop.getType() != null) {
				hashcode = hashcode + prop.getType().hashCode();
			}
			if (prop.getRelatedCode() != null) {
				hashcode = hashcode + prop.getRelatedCode().hashCode();
			}
		}
		return hashcode;
	}

    public static int getAssociationHashCode(List<Association> associations) {
		int hashcode = 0;
		if (associations == null || associations.size() == 0) return 0;
		for (int i=0; i<associations.size(); i++) {
			Association prop = (Association) associations.get(i);
			if (prop.getType() != null) {
				hashcode = hashcode + prop.getType().hashCode();
			}
			if (prop.getRelatedCode() != null) {
				hashcode = hashcode + prop.getRelatedCode().hashCode();
			}
		}
		return hashcode;
	}

    public static int getInverseAssociationHashCode(List<InverseAssociation> inverseassociation) {
		int hashcode = 0;
		if (inverseassociation == null || inverseassociation.size() == 0) return 0;
		for (int i=0; i<inverseassociation.size(); i++) {
			InverseAssociation prop = (InverseAssociation) inverseassociation.get(i);
			if (prop.getType() != null) {
				hashcode = hashcode + prop.getType().hashCode();
			}
			if (prop.getRelatedCode() != null) {
				hashcode = hashcode + prop.getRelatedCode().hashCode();
			}
		}
		return hashcode;
	}

/////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static Vector getSuperclassValues(ConceptDetails cd) {
		Vector v = new Vector();
		String code = cd.getCode();
		List<Superclass> superclasses = cd.getSuperclasses();
		if (superclasses == null || superclasses.size() == 0) return null;
		for (int i=0; i<superclasses.size(); i++) {
			Superclass superclass = (Superclass) superclasses.get(i);
			v.add(code + "|" + cd.getName() + "|" + superclass.getCode() + "|" + superclass.getName());
		}
		return v;
	}

    public static Vector getPropertyValues(ConceptDetails cd) {
		Vector v = new Vector();
		String code = cd.getCode();
		List<Property> properties = cd.getProperties();
		if (properties == null || properties.size() == 0) return null;
		for (int i=0; i<properties.size(); i++) {
			Property prop = (Property) properties.get(i);
			v.add(code + "|" + cd.getName() + "|" + prop.getType() + "|" + prop.getValue());
		}
		return v;
	}

    public static Vector getDefinitionValues(ConceptDetails cd) {
		Vector v = new Vector();
		String code = cd.getCode();
		List<Definition> definitions = cd.getDefinitions();
		if (definitions == null || definitions.size() == 0) return null;
		for (int i=0; i<definitions.size(); i++) {
			Definition def = (Definition) definitions.get(i);
			String label = "DEFINITION";
			if (def.getSource().compareTo("NCI") != 0) {
				label = "ALT_DEFINITION";
			}
			v.add(label + "|" + code + "|" + cd.getName() + "|" + def.getDefinition() + "|source|" + def.getSource());
		}
		return v;
	}

    public static Vector getRoleValues(ConceptDetails cd) {
		Vector v = new Vector();
		String code = cd.getCode();
		List<Role> roles = cd.getRoles();
		if (roles == null || roles.size() == 0) return null;
		for (int i=0; i<roles.size(); i++) {
			Role role = (Role) roles.get(i);
			v.add(code + "|" + cd.getName() + "|" + role.getType() + "|" + role.getRelatedCode());
		}
		return v;
	}

    public static Vector getInverseRoleValues(ConceptDetails cd) {
		Vector v = new Vector();
		String code = cd.getCode();
		List<InverseRole> roles = cd.getInverseRoles();
		if (roles == null || roles.size() == 0) return null;
		for (int i=0; i<roles.size(); i++) {
			InverseRole role = (InverseRole) roles.get(i);
			v.add(code + "|" + cd.getName() + "|" + role.getType() + "|" + role.getRelatedCode());
		}
		return v;
	}

    public static Vector getAssociationValues(ConceptDetails cd) {
		Vector v = new Vector();
		String code = cd.getCode();
		List<Association> roles = cd.getAssociations();
		if (roles == null || roles.size() == 0) return null;
		for (int i=0; i<roles.size(); i++) {
			Association role = (Association) roles.get(i);
			v.add(code + "|" + cd.getName() + "|" + role.getType() + "|" + role.getRelatedCode());
		}
		return v;
	}

    public static Vector getInverseAssociationValues(ConceptDetails cd) {
		Vector v = new Vector();
		String code = cd.getCode();
		List<InverseAssociation> roles = cd.getInverseAssociations();
		if (roles == null || roles.size() == 0) return null;
		for (int i=0; i<roles.size(); i++) {
			InverseAssociation role = (InverseAssociation) roles.get(i);
			v.add(code + "|" + cd.getName() + "|" + role.getType() + "|" + role.getRelatedCode());
		}
		return v;
	}

    public static Vector getFULLSYNValues(ConceptDetails cd) {
		Vector v = new Vector();
		String code = cd.getCode();
		List<Synonym> synonyms = cd.getSynonyms();
		if (synonyms == null || synonyms.size() == 0) return null;
		for (int i=0; i<synonyms.size(); i++) {
			Synonym syn = (Synonym) synonyms.get(i);
			if (syn.getType().compareTo("FULL_SYN") == 0) {
				v.add(syn.getType() + "|" + code + "|" + cd.getName()
				   + "|term-name|" + syn.getName()
				   + "|Term Type|" + syn.getTermGroup()
				   + "|Term Source|" + syn.getSource()
				   + "|Subsource|" + syn.getSubSource()
				   + "|source-code|" + syn.getCode());
			} else {
				v.add("property|" + code + "|" + cd.getName() + "|" + syn.getType() + "|" + syn.getName());
			}
		}
		return v;
	}

    public static int getPropertyHashCode(List<Property> properties) {
		int hashcode = 0;
		if (properties == null || properties.size() == 0) return 0;
		for (int i=0; i<properties.size(); i++) {
			Property prop = (Property) properties.get(i);
			if (prop.getType() != null) {
				hashcode = hashcode + prop.getType().hashCode();
			}
			if (prop.getValue() != null) {
				hashcode = hashcode + prop.getValue().hashCode();
			}
		}
		return hashcode;
	}

    public static Vector compare(ConceptDetails cd_1, ConceptDetails cd_2) {
		Vector edit_history = new Vector();
		//superclasses
        Vector vec_1 = getSuperclassValues(cd_1);
        Vector vec_2 = getSuperclassValues(cd_2);
		Vector w1_clone = (Vector) vec_1.clone();
		Vector w2_clone = (Vector) vec_2.clone();
		w1_clone.removeAll(w2_clone);
		for (int i=0; i<w1_clone.size(); i++) {
			String t = (String) w1_clone.elementAt(i);
			edit_history.add("add|superclass|" + t);
		}
		w1_clone = (Vector) vec_2.clone();
		w2_clone = (Vector) vec_1.clone();
		w1_clone.removeAll(w2_clone);
		for (int i=0; i<w1_clone.size(); i++) {
			String t = (String) w1_clone.elementAt(i);
			edit_history.add("delete|superclass|" + t);
		}

        //property
        vec_1 = getPropertyValues(cd_1);
        vec_2 = getPropertyValues(cd_2);
		w1_clone = (Vector) vec_1.clone();
		w2_clone = (Vector) vec_2.clone();
		w1_clone.removeAll(w2_clone);
		for (int i=0; i<w1_clone.size(); i++) {
			String t = (String) w1_clone.elementAt(i);
			edit_history.add("add|property|" + t);
		}
		w1_clone = (Vector) vec_2.clone();
		w2_clone = (Vector) vec_1.clone();
		w1_clone.removeAll(w2_clone);
		for (int i=0; i<w1_clone.size(); i++) {
			String t = (String) w1_clone.elementAt(i);
			edit_history.add("delete|property|" + t);
		}

		// Definition
        vec_1 = getDefinitionValues(cd_1);
        vec_2 = getDefinitionValues(cd_2);
		w1_clone = (Vector) vec_1.clone();
		w2_clone = (Vector) vec_2.clone();
		w1_clone.removeAll(w2_clone);
		for (int i=0; i<w1_clone.size(); i++) {
			String t = (String) w1_clone.elementAt(i);
			edit_history.add("add|" + t);
		}
		w1_clone = (Vector) vec_2.clone();
		w2_clone = (Vector) vec_1.clone();
		w1_clone.removeAll(w2_clone);
		for (int i=0; i<w1_clone.size(); i++) {
			String t = (String) w1_clone.elementAt(i);
			edit_history.add("delete|" + t);
		}

        //FULL_SYN
        vec_1 = getFULLSYNValues(cd_1);
        vec_2 = getFULLSYNValues(cd_2);
		w1_clone = (Vector) vec_1.clone();
		w2_clone = (Vector) vec_2.clone();
		w1_clone.removeAll(w2_clone);
		for (int i=0; i<w1_clone.size(); i++) {
			String t = (String) w1_clone.elementAt(i);
			edit_history.add("add|" + t);
		}
		w1_clone = (Vector) vec_2.clone();
		w2_clone = (Vector) vec_1.clone();
		w1_clone.removeAll(w2_clone);
		for (int i=0; i<w1_clone.size(); i++) {
			String t = (String) w1_clone.elementAt(i);
			edit_history.add("delete|" + t);
		}

		edit_history.addAll(getEditHistoryForRelatedConcepts(cd_1, cd_2, "role"));
		edit_history.addAll(getEditHistoryForRelatedConcepts(cd_1, cd_2, "inverseRole"));
		edit_history.addAll(getEditHistoryForRelatedConcepts(cd_1, cd_2, "association"));
		edit_history.addAll(getEditHistoryForRelatedConcepts(cd_1, cd_2, "inverseAssociation"));
		return edit_history;
	}

    public static Vector getEditHistoryForRelatedConcepts(ConceptDetails cd_1,
                                                          ConceptDetails cd_2,
                                                          String type) {
        Vector edit_history = new Vector();
        Vector vec_1 = new Vector();
        Vector vec_2 = new Vector();
        if (type.compareTo("role") == 0) {
			vec_1 = getRoleValues(cd_1);
			vec_2 = getRoleValues(cd_2);
        } else if (type.compareTo("inverseRole") == 0) {
			vec_1 = getInverseRoleValues(cd_1);
			vec_2 = getInverseRoleValues(cd_2);
        } else if (type.compareTo("association") == 0) {
			vec_1 = getAssociationValues(cd_1);
			vec_2 = getAssociationValues(cd_2);
        } else if (type.compareTo("inverseAssociation") == 0) {
			vec_1 = getInverseAssociationValues(cd_1);
			vec_2 = getInverseAssociationValues(cd_2);
		}
		Vector w1_clone = null;
		Vector w2_clone = null;
        //role

        if (vec_1 == null) {
			w1_clone = new Vector();
		} else {
			w1_clone = (Vector) vec_1.clone();
		}
        if (vec_2 == null) {
			w2_clone = new Vector();
		} else {
			w2_clone = (Vector) vec_2.clone();
		}
		w1_clone.removeAll(w2_clone);
		for (int i=0; i<w1_clone.size(); i++) {
			String t = (String) w1_clone.elementAt(i);
			edit_history.add("add|"+ type + "|" + t);
		}

		if (vec_2 == null) {
			w1_clone = new Vector();
		} else {
			w1_clone = (Vector) vec_2.clone();
		}
		if (vec_1 == null) {
			w2_clone = new Vector();
		} else {
			w2_clone = (Vector) vec_1.clone();
		}
		w1_clone.removeAll(w2_clone);
		for (int i=0; i<w1_clone.size(); i++) {
			String t = (String) w1_clone.elementAt(i);
			edit_history.add("delete|"+ type + "|" + t);
		}
		return edit_history;
	}

    public static Vector compare(String jsonfilenew, String jsonfileold) {
        Vector v10 = Utils.readFile(jsonfilenew);
        Vector v20 = Utils.readFile(jsonfileold);
        Vector v1 = new Vector();
        for (int i=0; i<v10.size(); i++) {
			String t = (String) v10.elementAt(i);
			t = t.trim();
			if (t.length() > 0) {
				v1.add(t);
			}
		}
        Vector v2 = new Vector();
        for (int i=0; i<v20.size(); i++) {
			String t = (String) v20.elementAt(i);
			t = t.trim();
			if (t.length() > 0) {
				v2.add(t);
			}
		}
        Vector<ConceptDetails> cd1 = new Vector();
        Vector<ConceptDetails> cd2 = new Vector();
        Vector<String> w1 = new Vector();
        Vector<String> w2 = new Vector();
        Vector<Integer> hashcd_1 = new Vector();
        Vector<Integer> hashcd_2 = new Vector();
        HashMap<String, ConceptDetails> code2CDMap1 = new HashMap();
        HashMap<String, ConceptDetails> code2CDMap2 = new HashMap();

        HashMap<String, String> code2Label1 = new HashMap();
        HashMap<String, String> code2Label2 = new HashMap();

        Vector edit_history = new Vector();

        for (int i=0; i<v1.size(); i++) {
			String json = (String) v1.elementAt(i);
			ConceptDetails cd = null;
			try {
			    cd = (ConceptDetails) EVSRESTAPIClient.deserialize("ConceptDetails", json);
			    if (cd == null) {
					System.out.println(json);
				}

			    cd1.add(cd);
			    w1.add(cd.getCode());
			    code2CDMap1.put(cd.getCode(), cd);
			    code2Label1.put(cd.getCode(), cd.getName());
			} catch (Exception ex) {
				System.out.println(json);
				ex.printStackTrace();
			}
		}
        for (int i=0; i<v2.size(); i++) {
			String json = (String) v2.elementAt(i);
			ConceptDetails cd = null;
			try {
			    cd = (ConceptDetails) EVSRESTAPIClient.deserialize("ConceptDetails", json);
			    cd2.add(cd);
			    w2.add(cd.getCode());
			    code2CDMap2.put(cd.getCode(), cd);
			    code2Label2.put(cd.getCode(), cd.getName());
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		Vector w1_clone = (Vector) w1.clone();
		Vector w2_clone = (Vector) w2.clone();
		w1_clone.removeAll(w2_clone);
		for (int i=0; i<w1_clone.size(); i++) {
			String code = (String) w1_clone.elementAt(i);
			edit_history.add("add|concept|" + code + "|" + (String) code2Label1.get(code));
		}

		w1_clone = (Vector) w2.clone();
		w2_clone = (Vector) w1.clone();
		w1_clone.removeAll(w2_clone);
		for (int i=0; i<w1_clone.size(); i++) {
			String code = (String) w1_clone.elementAt(i);
			edit_history.add("delete|concept|" + code + "|" + (String) code2Label2.get(code));
		}

        HashMap cd_map_1 = new HashMap();
        for (int i=0; i<cd1.size(); i++) {
			ConceptDetails cd = (ConceptDetails) cd1.elementAt(i);
			cd_map_1.put(cd.getCode(), cd);
		}
        HashMap cd_map_2 = new HashMap();
        for (int i=0; i<cd2.size(); i++) {
			ConceptDetails cd = (ConceptDetails) cd2.elementAt(i);
			cd_map_2.put(cd.getCode(), cd);
		}
		Iterator it = cd_map_1.keySet().iterator();
		Vector w = new Vector();
		while (it.hasNext()) {
			String code = (String) it.next();
			if (cd_map_2.containsKey(code)) {
				ConceptDetails cd_1 = (ConceptDetails) cd_map_1.get(code);
				ConceptDetails cd_2 = (ConceptDetails) cd_map_2.get(code);
				int hashcode_1 = toHashCode(cd_1);
				int hashcode_2 = toHashCode(cd_2);
				if (hashcode_1 != hashcode_2) {
					w.add(code);
				}
			}
		}
		for (int i=0; i<w.size(); i++) {
			String code = (String) w.elementAt(i);
			edit_history.add("modify|concept|" + code + "|" + (String) code2Label2.get(code));
		}
		for (int i=0; i<w.size(); i++) {
			String code = (String) w.elementAt(i);
			ConceptDetails cd_1 = (ConceptDetails) code2CDMap1.get(code);
			ConceptDetails cd_2 = (ConceptDetails) code2CDMap2.get(code);
			edit_history.addAll(compare(cd_1, cd_2));
		}
        return edit_history;
    }

    public static EditAction toEditAction(String line) {
		EditAction ea = null;
		Vector u = StringUtils.parseData(line);
		String action = (String) u.elementAt(0);
		String target = (String) u.elementAt(1);
		if (target.compareTo("concept") == 0) {
			ea = new EditAction((String) u.elementAt(3),
			                    (String) u.elementAt(2),
			                    (String) u.elementAt(0),
			                    (String) u.elementAt(1),
			                    null);

		} else if (target.compareTo("property") == 0) {
			ea = new EditAction((String) u.elementAt(3),
			                    (String) u.elementAt(2),
			                    (String) u.elementAt(0),
			                    target,
			                    (String) u.elementAt(4)+"|"+(String) u.elementAt(5));
		} else if (target.compareTo("association") == 0 ||
		           target.compareTo("inverseAssociation") == 0 ||
		           target.compareTo("role") == 0 ||
		           target.compareTo("inverseRole") == 0) {
			ea = new EditAction((String) u.elementAt(3),
			                    (String) u.elementAt(2),
			                    (String) u.elementAt(0),
			                    target,
			                    (String) u.elementAt(4)+"|"+(String) u.elementAt(5));
		} else {
			StringBuffer buf = new StringBuffer();
			for (int i=4; i<u.size(); i++) {
				buf.append((String) u.elementAt(i));
				if (i < u.size()-1) {
					buf.append("|");
				}
			}
			String value = buf.toString();
			ea = new EditAction((String) u.elementAt(3),
			                    (String) u.elementAt(2),
			                    (String) u.elementAt(0),
			                    target,
			                    value);

	    }
		return ea;
	}

    public static List<EditAction> loadEditHistory(String filename) {
		Vector lines = Utils.readFile(filename);
		return toEditActionList(lines);
	}

    public static List<EditAction> toEditActionList(Vector lines) {
		List<EditAction> list = new ArrayList();
		for (int i=0; i<lines.size(); i++) {
			String line = (String) lines.elementAt(i);
			EditAction ea = toEditAction(line);
			list.add(ea);
    	}
		return list;
	}

	public static void main(String[] args) {
	    Vector v = null;
	    try {
	        String jsonfile_new = args[0];
	        String jsonfile_old = args[1];
	        Vector edit_history = compare(jsonfile_new, jsonfile_old);
	        /*
	        System.out.println("saving edit history...");
	        Utils.saveToFile("edit_history.txt", edit_history);
	        List<EditAction> list = loadEditHistory("edit_history.txt");
	        */
	        List<EditAction> list = toEditActionList(edit_history);
	        for (int i=0; i<list.size(); i++) {
				EditAction ea = (EditAction) list.get(i);
				System.out.println(ea.toJson());
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
