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

	public static String[] DATA_TYPES = new String[] {
		"synonyms",
		"definitions",
		"properties",
		"parents",
		"children",
		"associations",
		"inverseAssociations",
		"roles",
		"inverseRoles",
		"maps"
	};

	public EditHistoryUtils() {

	}

    public static int toHashCode(ConceptDetails cd) {
		return cd.toJson().hashCode();
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

    public static int getHashCode(List list) {
		int hashcode = 0;
		if (list == null || list.size() == 0) return hashcode;
		for (int i=0; i<list.size(); i++) {
			Object obj = list.get(i);
			if (obj instanceof Synonym) {
				Synonym cls = (Synonym) obj;
				if (cls.getName() != null) hashcode = hashcode + cls.getName().hashCode();
				if (cls.getType() != null) hashcode = hashcode + cls.getType().hashCode();
				if (cls.getTermGroup() != null) hashcode = hashcode + cls.getTermGroup().hashCode();
				if (cls.getSource() != null) hashcode = hashcode + cls.getSource().hashCode();
				if (cls.getSubSource() != null) hashcode = hashcode + cls.getSubSource().hashCode();
				if (cls.getCode() != null) hashcode = hashcode + cls.getCode().hashCode();
			} else if (obj instanceof Definition) {
				Definition cls = (Definition) obj;
				if (cls.getDefinition() != null) hashcode = hashcode + cls.getDefinition().hashCode();
				if (cls.getType() != null) hashcode = hashcode + cls.getType().hashCode();
				if (cls.getSource() != null) hashcode = hashcode + cls.getSource().hashCode();
			} else if (obj instanceof Property) {
				Property cls = (Property) obj;
				hashcode = hashcode + cls.getType().hashCode() + cls.getValue().hashCode();
			} else if (obj instanceof Subclass) {
				Subclass cls = (Subclass) obj;
				hashcode = hashcode + cls.getCode().hashCode() + cls.getName().hashCode();
			} else if (obj instanceof Superclass) {
				Superclass cls = (Superclass) obj;
				hashcode = hashcode + cls.getCode().hashCode() + cls.getName().hashCode();
			} else if (obj instanceof Association) {
				Association cls = (Association) obj;
				hashcode = hashcode + cls.getType().hashCode() + cls.getRelatedCode().hashCode() + cls.getRelatedName().hashCode();
			} else if (obj instanceof InverseAssociation) {
				InverseAssociation cls = (InverseAssociation) obj;
				hashcode = hashcode + cls.getType().hashCode() + cls.getRelatedCode().hashCode() + cls.getRelatedName().hashCode();
			} else if (obj instanceof Role) {
				Role cls = (Role) obj;
				hashcode = hashcode + cls.getType().hashCode() + cls.getRelatedCode().hashCode() + cls.getRelatedName().hashCode();
			} else if (obj instanceof InverseRole) {
				InverseRole cls = (InverseRole) obj;
				hashcode = hashcode + cls.getType().hashCode() + cls.getRelatedCode().hashCode() + cls.getRelatedName().hashCode();
			} else if (obj instanceof MapsTo) {
				MapsTo cls = (MapsTo) obj;
				hashcode = hashcode + cls.getType().hashCode()
				                    + cls.getTargetTermGroup().hashCode()
				                    + cls.getTargetCode().hashCode()
				                    + cls.getTargetTerminology().hashCode()
				                    + cls.getTargetTerminologyVersion().hashCode();
			}
		}
		return hashcode;
	}



    public static int getHashCode(ConceptDetails cd, String type) {
		int hashcode = 0;
		if (type.compareTo("synonyms") == 0) {
			 return getHashCode(cd.getSynonyms());
		} else if (type.compareTo("definitions") == 0) {
			 return getHashCode(cd.getDefinitions());
		} else if (type.compareTo("properties") == 0) {
			 return getHashCode(cd.getProperties());
		} else if (type.compareTo("parents") == 0) {
			 return getHashCode(cd.getParents());
		} else if (type.compareTo("children") == 0) {
			 return getHashCode(cd.getChildren());
		} else if (type.compareTo("associations") == 0) {
			 return getHashCode(cd.getAssociations());
		} else if (type.compareTo("inverseAssociations") == 0) {
			 return getHashCode(cd.getInverseAssociations());
		} else if (type.compareTo("Roles") == 0) {
			 return getHashCode(cd.getRoles());
		} else if (type.compareTo("inverseRoles") == 0) {
			 return getHashCode(cd.getInverseRoles());
		} else if (type.compareTo("maps") == 0) {
			 return getHashCode(cd.getMaps());
		}
		return hashcode;
	}

    public static boolean modified(ConceptDetails cd_1, ConceptDetails cd_2, String type) {
		int hashcode_1 = getHashCode(cd_1, type);
		int hashcode_2 = getHashCode(cd_2, type);
		if (hashcode_1 == hashcode_2) return false;
		return true;
	}


/////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static Vector getSuperclassValues(ConceptDetails cd) {
		Vector v = new Vector();
		String code = cd.getCode();
		List<Superclass> superclasses = cd.getParents();
		if (superclasses == null || superclasses.size() == 0) return v;
		for (int i=0; i<superclasses.size(); i++) {
			Superclass superclass = (Superclass) superclasses.get(i);
			v.add(code + "|" + cd.getName() + "|" + superclass.getCode() + "|" + superclass.getName());
		}
		return v;
	}

    public static Vector getSubclassValues(ConceptDetails cd) {
		Vector v = new Vector();
		String code = cd.getCode();
		List<Subclass> suberclasses = cd.getChildren();
		if (suberclasses == null || suberclasses.size() == 0) return v;
		for (int i=0; i<suberclasses.size(); i++) {
			Subclass subclass = (Subclass) suberclasses.get(i);
			v.add(code + "|" + cd.getName() + "|" + subclass.getCode() + "|" + subclass.getName());
		}
		return v;
	}

    public static Vector getMapsToValues(ConceptDetails cd) {
		Vector v = new Vector();
		String code = cd.getCode();
		List<MapsTo> maps = cd.getMaps();
		if (maps == null || maps.size() == 0) return v;
		for (int i=0; i<maps.size(); i++) {
			MapsTo map = (MapsTo) maps.get(i);
			v.add(code + "|" + cd.getName() + "|" + map.getType()
			                                + "|" + map.getTargetName()
			                                + "|" + map.getTargetTermGroup()
			                                + "|" + map.getTargetCode()
			                                + "|" + map.getTargetTerminology()
			                                + "|" + map.getTargetTerminologyVersion()
			                                );
		}
		return v;
	}

    public static Vector getPropertyValues(ConceptDetails cd) {
		Vector v = new Vector();
		String code = cd.getCode();
		List<Property> properties = cd.getProperties();
		if (properties == null || properties.size() == 0) return v;
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
		if (definitions == null || definitions.size() == 0) return v;
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
		if (roles == null || roles.size() == 0) return v;
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
		if (roles == null || roles.size() == 0) return v;
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
		if (roles == null || roles.size() == 0) return v;
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
		if (roles == null || roles.size() == 0) return v;
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
		if (synonyms == null || synonyms.size() == 0) return v;
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

    public static Vector getValues(ConceptDetails cd, String type) {
		if (type.compareTo("synonyms") == 0) {
			 return getFULLSYNValues(cd);
		} else if (type.compareTo("definitions") == 0) {
			 return getDefinitionValues(cd);
		} else if (type.compareTo("properties") == 0) {
			 return getPropertyValues(cd);
		} else if (type.compareTo("parents") == 0) {
			 return getSuperclassValues(cd);
		} else if (type.compareTo("children") == 0) {
			 return getSubclassValues(cd);
		} else if (type.compareTo("associations") == 0) {
			 return getAssociationValues(cd);
		} else if (type.compareTo("inverseAssociations") == 0) {
			 return getInverseAssociationValues(cd);
		} else if (type.compareTo("Roles") == 0) {
			 return getRoleValues(cd);
		} else if (type.compareTo("inverseRoles") == 0) {
			 return getInverseRoleValues(cd);
		} else if (type.compareTo("maps") == 0) {
			 return getMapsToValues(cd);
		}
		return null;
	}

    public static Vector compareValues(Vector vec_1, Vector vec_2, String type) {
		Vector edit_history = new Vector();
		String s = "";
		if (type.compareTo("synonyms") == 0) {
			s = "";
		} else if (type.compareTo("definitions") == 0) {
			s = "";
		} else if (type.compareTo("properties") == 0) {
			s = "property|";
		} else if (type.compareTo("parents") == 0) {
			s = "superclass|";
		} else if (type.compareTo("children") == 0) {
			s = "subclass|";
		} else if (type.compareTo("associations") == 0) {
			s = "association|";
		} else if (type.compareTo("inverseAssociations") == 0) {
			s = "inverseAssociation|";
		} else if (type.compareTo("roles") == 0) {
			s = "role|";
		} else if (type.compareTo("inverseRoles") == 0) {
			s = "inverseRole|";
		} else if (type.compareTo("maps") == 0) {
			s = "maps|";
		}
		Vector w1_clone = (Vector) vec_1.clone();
		Vector w2_clone = (Vector) vec_2.clone();
		w1_clone.removeAll(w2_clone);
		for (int i=0; i<w1_clone.size(); i++) {
			String t = (String) w1_clone.elementAt(i);
			edit_history.add("add|" + s + t);
		}
		w1_clone = (Vector) vec_2.clone();
		w2_clone = (Vector) vec_1.clone();
		w1_clone.removeAll(w2_clone);
		for (int i=0; i<w1_clone.size(); i++) {
			String t = (String) w1_clone.elementAt(i);
			edit_history.add("delete|" + s + t);
		}
		return edit_history;
	}

    public static Vector compare(ConceptDetails cd_1, ConceptDetails cd_2) {
		Vector edit_history = new Vector();
		//superclasses

		System.out.println(cd_1.getName() + " (" + cd_1.getCode() + ")");
		System.out.println(cd_2.getName() + " (" + cd_2.getCode() + ")");

        Vector vec_1 = null;
        Vector vec_2 = null;
		for (int i=0; i<DATA_TYPES.length; i++) {
			String type = (String) DATA_TYPES[i];
			boolean retval = modified(cd_1, cd_2, type);
			System.out.println("\t" + type + " modified? " + retval);

			if (retval) {
				vec_1 = getValues(cd_1, type);
				vec_2 = getValues(cd_2, type);
				Vector w = compareValues(vec_1, vec_2, type);
                edit_history.addAll(w);
			}
		}
        /*
		edit_history.addAll(getEditHistoryForRelatedConcepts(cd_1, cd_2, "role"));
		edit_history.addAll(getEditHistoryForRelatedConcepts(cd_1, cd_2, "inverseRole"));
		edit_history.addAll(getEditHistoryForRelatedConcepts(cd_1, cd_2, "association"));
		edit_history.addAll(getEditHistoryForRelatedConcepts(cd_1, cd_2, "inverseAssociation"));
		edit_history.addAll(getEditHistoryForRelatedConcepts(cd_1, cd_2, "maps"));
		*/

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
        } else if (type.compareTo("maps") == 0) {
			vec_1 = getMapsToValues(cd_1);
			vec_2 = getMapsToValues(cd_2);
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

		HashMap cd_map_1 = (HashMap) code2CDMap1.clone();// new HashMap();
		HashMap cd_map_2 = (HashMap) code2CDMap2.clone();

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


	public static Vector getEditHistory(String jsonfile_new, String jsonfile_old) {
	    Vector v = new Vector();
	    try {
	        Vector edit_history = compare(jsonfile_new, jsonfile_old);
	        List<EditAction> list = toEditActionList(edit_history);
	        for (int i=0; i<list.size(); i++) {
				EditAction ea = (EditAction) list.get(i);
				v.add(ea.toJson());
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return v;
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
