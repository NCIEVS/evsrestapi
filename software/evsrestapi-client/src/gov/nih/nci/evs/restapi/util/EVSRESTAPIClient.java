package gov.nih.nci.evs.restapi.util;

import gov.nih.nci.evs.restapi.model.*;

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

public class EVSRESTAPIClient {
	public static String ENDPOINT_PREFIX = "https://api-evsrest.nci.nih.gov";
	public static String ENDPOINT_CONCEPT = "/api/v1/concept/{terminology}/{code}";
	public static String[] TYPES = new String[]{"roles", "inverseRoles", "associations", "inverseAssociations", "parents"};
    public static HashMap EVSRESTAPI_URL_MAP = new HashMap();

    static {
		EVSRESTAPI_URL_MAP = new HashMap();
		EVSRESTAPI_URL_MAP.put("associations", "https://api-evsrest.nci.nih.gov/api/v1/concept/{terminology}/{code}/associations");
		EVSRESTAPI_URL_MAP.put("inverseAssociations", "https://api-evsrest.nci.nih.gov/api/v1/concept/{terminology}/{code}/inverseAssociations");
        EVSRESTAPI_URL_MAP.put("code", "https://api-evsrest.nci.nih.gov/api/v1/concept/{terminology}/{code}");
		EVSRESTAPI_URL_MAP.put("maps", "https://api-evsrest.nci.nih.gov/api/v1/concept/{terminology}/{code}/maps");
		EVSRESTAPI_URL_MAP.put("pathsToAncestor_ancestorCode", "https://api-evsrest.nci.nih.gov/api/v1/concept/{terminology}/{code}/pathsToAncestor/{ancestorCode}");
		EVSRESTAPI_URL_MAP.put("roles", "https://api-evsrest.nci.nih.gov/api/v1/concept/{terminology}/{code}/roles");
		EVSRESTAPI_URL_MAP.put("roots", "https://api-evsrest.nci.nih.gov/api/v1/concept/{terminology}/roots");
		EVSRESTAPI_URL_MAP.put("descendants", "https://api-evsrest.nci.nih.gov/api/v1/concept/{terminology}/{code}/descendants");
		EVSRESTAPI_URL_MAP.put("pathsToRoot", "https://api-evsrest.nci.nih.gov/api/v1/concept/{terminology}/{code}/pathsToRoot");
		EVSRESTAPI_URL_MAP.put("subtree", "https://api-evsrest.nci.nih.gov/api/v1/concept/{terminology}/{code}/subtree");
		EVSRESTAPI_URL_MAP.put("inverseAssociations", "https://api-evsrest.nci.nih.gov/api/v1/concept/{terminology}/{code}/inverseAssociations");
		EVSRESTAPI_URL_MAP.put("inverseRoles", "https://api-evsrest.nci.nih.gov/api/v1/concept/{terminology}/{code}/inverseRoles");
		EVSRESTAPI_URL_MAP.put("concept_terminology", "https://api-evsrest.nci.nih.gov/api/v1/concept/{terminology}");
		EVSRESTAPI_URL_MAP.put("children", "https://api-evsrest.nci.nih.gov/api/v1/concept/{terminology}/{code}/subtree/children");
		EVSRESTAPI_URL_MAP.put("disjointWith", "https://api-evsrest.nci.nih.gov/api/v1/concept/{terminology}/{code}/disjointWith");
		EVSRESTAPI_URL_MAP.put("pathsFromRoot", "https://api-evsrest.nci.nih.gov/api/v1/concept/{terminology}/{code}/pathsFromRoot");
		EVSRESTAPI_URL_MAP.put("parents", "https://api-evsrest.nci.nih.gov/api/v1/concept/{terminology}/{code}/parents");
        EVSRESTAPI_URL_MAP.put("concept", "https://api-evsrest.nci.nih.gov/api/v1/concept/{terminology}/{code}?include=full");


		EVSRESTAPI_URL_MAP.put("metadata_associations", "https://api-evsrest.nci.nih.gov/api/v1/metadata/{terminology}/associations");
		EVSRESTAPI_URL_MAP.put("metadata_terminologies", "https://api-evsrest.nci.nih.gov/api/v1/metadata/terminologies");
		EVSRESTAPI_URL_MAP.put("metadata_values", "https://api-evsrest.nci.nih.gov/api/v1/metadata/{terminology}/qualifier/{codeOrLabel}/values");
		EVSRESTAPI_URL_MAP.put("metadata_roles", "https://api-evsrest.nci.nih.gov/api/v1/metadata/{terminology}/roles");
		EVSRESTAPI_URL_MAP.put("metadata_role_codeOrLabel", "https://api-evsrest.nci.nih.gov/api/v1/metadata/{terminology}/role/{codeOrLabel}");
		EVSRESTAPI_URL_MAP.put("metadata_qualifiers", "https://api-evsrest.nci.nih.gov/api/v1/metadata/{terminology}/qualifiers");
		EVSRESTAPI_URL_MAP.put("metadata_definitionSources", "https://api-evsrest.nci.nih.gov/api/v1/metadata/{terminology}/definitionSources");
		EVSRESTAPI_URL_MAP.put("metadata_termTypes", "https://api-evsrest.nci.nih.gov/api/v1/metadata/{terminology}/termTypes");
		EVSRESTAPI_URL_MAP.put("metadata_synonymSources", "https://api-evsrest.nci.nih.gov/api/v1/metadata/{terminology}/synonymSources");
		EVSRESTAPI_URL_MAP.put("metadata_association_codeOrLabel", "https://api-evsrest.nci.nih.gov/api/v1/metadata/{terminology}/association/{codeOrLabel}");
		EVSRESTAPI_URL_MAP.put("metadata_property_codeOrLabel", "https://api-evsrest.nci.nih.gov/api/v1/metadata/{terminology}/property/{codeOrLabel}");
		EVSRESTAPI_URL_MAP.put("metadata_conceptStatuses", "https://api-evsrest.nci.nih.gov/api/v1/metadata/{terminology}/conceptStatuses");
		EVSRESTAPI_URL_MAP.put("metadata_properties", "https://api-evsrest.nci.nih.gov/api/v1/metadata/{terminology}/properties");
		EVSRESTAPI_URL_MAP.put("metadata_qualifier_codeOrLabel", "https://api-evsrest.nci.nih.gov/api/v1/metadata/{terminology}/qualifier/{codeOrLabel}");
	}


	public EVSRESTAPIClient() {

	}

	public static HashMap get_evsrestapi_endpoint_map(String filename) {
		HashMap hmap = new HashMap();
		String end_point_prefix = EVSRESTAPIClient.ENDPOINT_PREFIX;
		Vector v = Utils.readFile(filename);
		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			if (t.indexOf("/api/") != -1) {
				t = end_point_prefix + t;
				String t0 = t;
				if (t.endsWith("{codeOrLabel}")) {
					t = t.replace("/{codeOrLabel}", "_codeOrLabel");
   			    }
				int n = t.lastIndexOf("/");
				String type = t.substring(n+1, t.length());
				if (type.startsWith("{")) {
					type = type.substring(1, type.length());
				}
				if (type.endsWith("}")) {
					type = type.substring(0, type.length()-1);
				}
				hmap.put(type, t0);
			}
		}
		return hmap;
	}

	public static String getURL(String terminology, String code) {
		String t = (String) EVSRESTAPI_URL_MAP.get("concept");
		if (terminology != null) t = t.replace("{terminology}", terminology);
		if (code != null) t = t.replace("{code}", code);
		return t;
	}

	public static void dumpVector(String label, Vector v) {
		System.out.println(label);
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			System.out.println(t);
		}
	}

    public static String getJson(String url) {
		String json = null;
		Vector w = new Vector();
		try {
			URL urlObj = new URL(url);
			URLConnection urlConnection = urlObj.openConnection();
			Charset charset = Charset.forName("UTF8");
			InputStreamReader stream = new InputStreamReader(urlConnection.getInputStream(), charset);
			BufferedReader reader = new BufferedReader(stream);
			StringBuffer responseBuffer = new StringBuffer();
			String line = null;
			while ((line = reader.readLine()) != null) {
				w.add(line);
			}
			json = JSONParser.flatten(w);
			reader.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return json;
	}


	public static Vector get_evsrestapi_endpoints(String filename, String code) {
		String terminology = "ncit";
		return get_evsrestapi_endpoints(filename, terminology, code);
	}



	public static Vector get_evsrestapi_endpoints(String filename, String terminology, String code) {
		String end_point_prefix = ENDPOINT_PREFIX;
		Vector v = Utils.readFile(filename);
		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			if (t.indexOf("/api/") != -1) {
				t = end_point_prefix + t;
				t = t.replace("{terminology}", terminology);
				t = t.replace("{code}", code);
				w.add(t);
			}
		}
		return w;
	}

	public static Vector get_sample_jsons(String filename, String code) {
		List<String> list = Arrays.asList(TYPES);
		Vector end_points = get_evsrestapi_endpoints( filename, code);
		Vector v = new Vector();
		int k = 0;
		for (int i=0; i<end_points.size(); i++) {
			String t = (String) end_points.elementAt(i);
			int n = t.lastIndexOf("/");
			String type = t.substring(n+1, t.length());
			//if (list.contains(type)) {
				k++;
				System.out.println("(" + k + ") " + t);
				String json = getJson(t);
				v.add(json);
			//}
		}
        return v;
	}

     public static Object deserialize(String className, String json) throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper(); // can reuse, share globally
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		if (className.compareTo("Concept") == 0) {
			gov.nih.nci.evs.restapi.model.Concept concept = mapper.readValue(json, gov.nih.nci.evs.restapi.model.Concept.class);
	        return concept;
		} else if (className.compareTo("Association") == 0) {
			gov.nih.nci.evs.restapi.model.Association association = mapper.readValue(json, gov.nih.nci.evs.restapi.model.Association.class);
	        return association;
		} else if (className.compareTo("InverseAssociation") == 0) {
			gov.nih.nci.evs.restapi.model.InverseAssociation inverseAssociation = mapper.readValue(json, gov.nih.nci.evs.restapi.model.InverseAssociation.class);
	        return inverseAssociation;
		} else if (className.compareTo("Role") == 0) {
			gov.nih.nci.evs.restapi.model.Role role = mapper.readValue(json, gov.nih.nci.evs.restapi.model.Role.class);
	        return role;
		} else if (className.compareTo("InverseRole") == 0) {
			gov.nih.nci.evs.restapi.model.InverseRole inverseRole = mapper.readValue(json, gov.nih.nci.evs.restapi.model.InverseRole.class);
	        return inverseRole;
		} else if (className.compareTo("Superclass") == 0) {
			gov.nih.nci.evs.restapi.model.Superclass superclass = mapper.readValue(json, gov.nih.nci.evs.restapi.model.Superclass.class);
	        return superclass;
		} else if (className.compareTo("Subclass") == 0) {
			gov.nih.nci.evs.restapi.model.Subclass subclass = mapper.readValue(json, gov.nih.nci.evs.restapi.model.Subclass.class);
	        return subclass;
		} else if (className.compareTo("MapsTo") == 0) {
			gov.nih.nci.evs.restapi.model.MapsTo mapsTo = mapper.readValue(json, gov.nih.nci.evs.restapi.model.MapsTo.class);
	        return mapsTo;
		} else if (className.compareTo("ConceptDetails") == 0) {
			gov.nih.nci.evs.restapi.model.ConceptDetails superclass = mapper.readValue(json, gov.nih.nci.evs.restapi.model.ConceptDetails.class);
	        return superclass;

		} else if (className.compareTo("Subtree") == 0) {
			gov.nih.nci.evs.restapi.model.Subtree response = mapper.readValue(json, gov.nih.nci.evs.restapi.model.Subtree.class);
	        return response;
		} else if (className.compareTo("Tree") == 0) {
			if (!json.startsWith("{")) {
				json = "{\"subtrees\":" + json + "}";
			}
			gov.nih.nci.evs.restapi.model.Tree response = mapper.readValue(json, gov.nih.nci.evs.restapi.model.Tree.class);
	        return response;

		} else if (className.compareTo("Descendant") == 0) {
			if (!json.startsWith("{")) {
				json = "{\"descendants\":" + json + "}";
			}
			gov.nih.nci.evs.restapi.model.Descendant response = mapper.readValue(json, gov.nih.nci.evs.restapi.model.Descendant.class);
	        return response;

		} else if (className.compareTo("RESTResponse") == 0) {
			gov.nih.nci.evs.restapi.model.RESTResponse response = mapper.readValue(json, gov.nih.nci.evs.restapi.model.RESTResponse.class);
	        return response;

		}
		return null;
     }

     public static String appendJSON(String json, String name, String value) {
		 String s = json.substring(0, json.length()-1);
		 s = s + ",\"" + name + "\":" + value + "}";
		 return s;
	 }



	public static ValueSet getValueSet(String terminology, String code) {
		HashMap hmap = EVSRESTAPI_URL_MAP;
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		gov.nih.nci.evs.restapi.model.ConceptDetails conceptDetails = null;
        String url = getURL(terminology, code);
		String json = getJson(url);
		url = (String) hmap.get("inverseAssociations");
		url = getURL(url, terminology, code);
		String value = getJson(url);
		json = appendJSON(json, "inverseAssociations", value);

        ValueSet vs = null;
		try {
			conceptDetails = mapper.readValue(json, gov.nih.nci.evs.restapi.model.ConceptDetails.class);
			Vector codes = new Vector();
			List inverseAssociations = conceptDetails.getInverseAssociations();
			for (int i=0; i<inverseAssociations.size(); i++) {
				gov.nih.nci.evs.restapi.model.InverseAssociation inverseAssociation = (InverseAssociation) inverseAssociations.get(i);
				codes.add(inverseAssociation.getRelatedCode());
			}
			vs = new ValueSet(conceptDetails.getCode(),
			                  conceptDetails.getName(),
			                  conceptDetails.getTerminology(),
			                  conceptDetails.getVersion(),
			                  codes);

		} catch (Exception ex) {
			ex.printStackTrace();
		}
        return vs;
	}

	public static gov.nih.nci.evs.restapi.model.ConceptDetails getConceptDetails(String terminology, String code) {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		gov.nih.nci.evs.restapi.model.ConceptDetails conceptDetails = null;
		String t = (String) EVSRESTAPI_URL_MAP.get("concept");
		if (terminology != null) t = t.replace("{terminology}", terminology);
		String url = t;
		if (code != null) {
			url = t.replace("{code}", code);
		}
        String json = getJson(url);
		try {
			conceptDetails = (gov.nih.nci.evs.restapi.model.ConceptDetails) deserialize("ConceptDetails", json);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
        return conceptDetails;
	}

	public static String getConceptDetailsInJSON(String terminology, String code) {
		gov.nih.nci.evs.restapi.model.ConceptDetails cd = getConceptDetails(terminology, code);
		return flattenJSON(cd.toJson());
	}

	public static Vector getConceptDetailsInJSON(String terminology, Vector codes) {
		Vector w = new Vector();
		for (int i=0; i<codes.size(); i++) {
			String code = (String) codes.elementAt(i);
			String json = getConceptDetailsInJSON(terminology, code);
			w.add(json);
		}
		return w;
	}

	public static String wrapJSON(String code, String terminology, String json) {
		StringBuffer buf = new StringBuffer();
		buf.append("{\"code\":\"" + code + "\",\"terminology\":\"ncit\",\"list\":");
		buf.append(json).append("}");
		return buf.toString();
	}

    public static RESTResponse getRESTResponse(String code, String terminology, String type) {
		String url = (String) EVSRESTAPI_URL_MAP.get(type);
		if (terminology != null) url = url.replace("{terminology}", terminology);
		if (code != null) url = url.replace("{code}", code);
		String json = EVSRESTAPIClient.getJson(url);
		String json_new = wrapJSON(code, terminology, json);
		RESTResponse response = null;
		try {
			response = (RESTResponse) deserialize("RESTResponse", json_new);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return response;
	}

	public static String flattenJSON(String json) {
		String t = json;
		Vector u = StringUtils.parseData(t, "\n");
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<u.size(); i++) {
			String s = (String) u.elementAt(i);
			s = s.trim();
			buf.append(s);
		}
		return buf.toString();
	}

	public static Vector getJSONs(String filename) {
		return getJSONs(filename, "ncit");
	}

	public static Vector getJSONs(String filename, String terminology) {
		Vector failed_urls = new Vector();
		Vector w = new Vector();
		Vector lines = Utils.readFile(filename);
		int lcv = 0;
		long ms = System.currentTimeMillis();
		for (int i=0; i<lines.size(); i++) {
			lcv++;
			if (lcv/50*50 == lcv) {
				System.out.println("" + lcv + " out of " + lines.size() + " completed.");
			}
			String line = (String) lines.elementAt(i);
			Vector u = StringUtils.parseData(line);
			String code = (String) u.elementAt(0);
			try {
				String json = getConceptDetailsInJSON(terminology, code);
				w.add(json);
			} catch (Exception ex) {
				ex.printStackTrace();
				String url = getURL(terminology, code);
				failed_urls.add(url);
			}
		}
		if (failed_urls.size() > 0) {
			Utils.saveToFile("failed_urls.txt", failed_urls);
		}
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
		return w;
	}

	public static String getURL(String t, String terminology, String code) {
		if (terminology != null) t = t.replace("{terminology}", terminology);
		if (code != null) t = t.replace("{code}", code);
		return t;
	}

	public String code2Label(String code) {
		return code2Label("ncit", code);
	}

	public String code2Label(String terminology, String code) {
		String t = (String) EVSRESTAPI_URL_MAP.get("code");
		String url = getURL(t, terminology, code);
		String json = getJson(url);
		if (json == null) return null;
		try {
			gov.nih.nci.evs.restapi.model.Concept c = (gov.nih.nci.evs.restapi.model.Concept) deserialize("Concept", json);
			return c.getName();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public Vector extractHierarchyData(String parentCode, String parntLabel , gov.nih.nci.evs.restapi.model.TreeNode node) {
		if (node == null) return null;
		Vector w = new Vector();
		w.add(parntLabel + "|" + parentCode + "|" + node.getName() + "|" + node.getCode());
		List list = node.getChildren();
		if (list != null) {
			for (int k=0; k<list.size(); k++) {
				gov.nih.nci.evs.restapi.model.TreeNode childnode = (gov.nih.nci.evs.restapi.model.TreeNode) list.get(k);
				Vector v = extractHierarchyData(node.getCode(), node.getName(), childnode);
				w.addAll(v);
			}
		}
		return w;
	}

	public Vector extractHierarchyData(String rootCode, gov.nih.nci.evs.restapi.model.Descendant descendant) {
		if (descendant == null) return null;
		String rootLabel = code2Label(rootCode);
		Vector w = new Vector();
		List list = descendant.getDescendants();
		for (int k=0; k<list.size(); k++) {
			gov.nih.nci.evs.restapi.model.TreeNode node = (gov.nih.nci.evs.restapi.model.TreeNode) list.get(k);
			Vector v = extractHierarchyData(rootCode, rootLabel, node);
			w.addAll(v);
		}
		return w;
	}

	public Vector extractHierarchyData(String code) {
		return extractHierarchyData("ncit", code);
	}


	public Vector extractHierarchyData(String terminology, String code) {
		String t = (String) EVSRESTAPI_URL_MAP.get("descendants");
		String url = getURL(t, terminology, code);
		String json = getJson(url+ "?maxLevel=50");
		try {
			gov.nih.nci.evs.restapi.model.Descendant descendant = (gov.nih.nci.evs.restapi.model.Descendant) deserialize("Descendant", json);
            return extractHierarchyData(code, descendant);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

    public void printTree(String code) {
        printTree("ncit", code);
	}

    public void printTree(String terminology, String code) {
        Vector v = extractHierarchyData(terminology, code);
        HierarchyHelper hh = new HierarchyHelper(v);
        hh.printTree();
	}


	public static void main(String[] args) {
	    Vector v = null;
	    try {
            String terminology = "ncit";
            String code = "C3224";
            gov.nih.nci.evs.restapi.model.ConceptDetails c = getConceptDetails(terminology, code);
            String json = c.toJson();
            //System.out.println(c.toJson());
            String flattened_json = flattenJSON(json);
            System.out.println(flattened_json);
            gov.nih.nci.evs.restapi.model.ConceptDetails c2 = (gov.nih.nci.evs.restapi.model.ConceptDetails) deserialize("ConceptDetails", flattened_json);
            System.out.println(c2.toJson());

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
