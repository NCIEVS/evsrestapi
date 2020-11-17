//package gov.nih.nci.evs.restapi.util;

import gov.nih.nci.evs.restapi.model.*;
import gov.nih.nci.evs.restapi.util.*;

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
		EVSRESTAPI_URL_MAP.put("ancestorCode", "https://api-evsrest.nci.nih.gov/api/v1/concept/{terminology}/{code}/pathsToAncestor/{ancestorCode}");
		EVSRESTAPI_URL_MAP.put("roles", "https://api-evsrest.nci.nih.gov/api/v1/concept/{terminology}/{code}/roles");
		EVSRESTAPI_URL_MAP.put("roots", "https://api-evsrest.nci.nih.gov/api/v1/concept/{terminology}/roots");
		EVSRESTAPI_URL_MAP.put("descendants", "https://api-evsrest.nci.nih.gov/api/v1/concept/{terminology}/{code}/descendants");
		EVSRESTAPI_URL_MAP.put("pathsToRoot", "https://api-evsrest.nci.nih.gov/api/v1/concept/{terminology}/{code}/pathsToRoot");
		EVSRESTAPI_URL_MAP.put("subtree", "https://api-evsrest.nci.nih.gov/api/v1/concept/{terminology}/{code}/subtree");
		EVSRESTAPI_URL_MAP.put("inverseAssociations", "https://api-evsrest.nci.nih.gov/api/v1/concept/{terminology}/{code}/inverseAssociations");
		EVSRESTAPI_URL_MAP.put("inverseRoles", "https://api-evsrest.nci.nih.gov/api/v1/concept/{terminology}/{code}/inverseRoles");
		EVSRESTAPI_URL_MAP.put("terminology", "https://api-evsrest.nci.nih.gov/api/v1/concept/{terminology}");
		EVSRESTAPI_URL_MAP.put("children", "https://api-evsrest.nci.nih.gov/api/v1/concept/{terminology}/{code}/subtree/children");
		EVSRESTAPI_URL_MAP.put("disjointWith", "https://api-evsrest.nci.nih.gov/api/v1/concept/{terminology}/{code}/disjointWith");
		EVSRESTAPI_URL_MAP.put("pathsFromRoot", "https://api-evsrest.nci.nih.gov/api/v1/concept/{terminology}/{code}/pathsFromRoot");
		EVSRESTAPI_URL_MAP.put("parents", "https://api-evsrest.nci.nih.gov/api/v1/concept/{terminology}/{code}/parents");
        EVSRESTAPI_URL_MAP.put("concept", "https://api-evsrest.nci.nih.gov/api/v1/concept/{terminology}/{code}?include=full");
	}

	public EVSRESTAPIClient() {

	}

	public static HashMap get_evsrestapi_endpoint_map(String filename) {
		HashMap hmap = new HashMap();
		String end_point_prefix = ENDPOINT_PREFIX;
		Vector v = Utils.readFile(filename);
		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			if (t.indexOf("/api/") != -1) {
				t = end_point_prefix + t;
				int n = t.lastIndexOf("/");
				String type = t.substring(n+1, t.length());
				if (type.startsWith("{")) {
					type = type.substring(1, type.length());
				}
				if (type.endsWith("}")) {
					type = type.substring(0, type.length()-1);
				}
				hmap.put(type, t);
			}
		}
		return hmap;
	}

	public static String getURL(String terminology, String code) {
		String t = (String) EVSRESTAPI_URL_MAP.get("code");
		t = t.replace("{terminology}", terminology);
		t = t.replace("{code}", code);
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
			json = (String) w.elementAt(0);
			reader.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return json;
	}

	public static Vector get_evsrestapi_endpoints(String filename, String code) {
		String end_point_prefix = ENDPOINT_PREFIX;
		Vector v = Utils.readFile(filename);
		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			if (t.indexOf("/api/") != -1) {
				t = end_point_prefix + t;
				t = t.replace("{terminology}", "ncit");
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

	public static String getURL(String t, String terminology, String code) {
		t = t.replace("{terminology}", terminology);
		t = t.replace("{code}", code);
		return t;
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
				InverseAssociation inverseAssociation = (InverseAssociation) inverseAssociations.get(i);
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
		//HashMap hmap = EVSRESTAPI_URL_MAP;
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		gov.nih.nci.evs.restapi.model.ConceptDetails conceptDetails = null;
		String t = (String) EVSRESTAPI_URL_MAP.get("concept");
		t = t.replace("{terminology}", terminology);
		String url = t.replace("{code}", code);
        String json = getJson(url);
		/*

        String url = getURL(terminology, code);
		String json = getJson(url);

		url = (String) hmap.get("associations");
		url = getURL(url, terminology, code);
		String value = getJson(url);
		json = appendJSON(json, "associations", value);

		url = (String) hmap.get("inverseAssociations");
		url = getURL(url, terminology, code);
		value = getJson(url);
		json = appendJSON(json, "inverseAssociations", value);

		url = (String) hmap.get("roles");
		url = getURL(url, terminology, code);
		value = getJson(url);
		json = appendJSON(json, "roles", value);

		url = (String) hmap.get("inverseRoles");
		url = getURL(url, terminology, code);
		value = getJson(url);
		json = appendJSON(json, "inverseRoles", value);

		url = (String) hmap.get("parents");
		url = getURL(url, terminology, code);
		value = getJson(url);
		json = appendJSON(json, "superclasses", value);
		*/

		try {
			conceptDetails = (gov.nih.nci.evs.restapi.model.ConceptDetails) deserialize("ConceptDetails", json);
			//System.out.println(conceptDetails.toJson());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
        return conceptDetails;
	}

	public static String getConceptDetailsInJSON(String terminology, String code) {
		ConceptDetails cd = getConceptDetails(terminology, code);
		/*
		HashMap hmap = EVSRESTAPI_URL_MAP;
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		gov.nih.nci.evs.restapi.model.ConceptDetails conceptDetails = null;
        String url = getURL(terminology, code);
		String json = getJson(url);

		url = (String) hmap.get("associations");
		url = getURL(url, terminology, code);
		String value = getJson(url);
		json = appendJSON(json, "associations", value);

		url = (String) hmap.get("inverseAssociations");
		url = getURL(url, terminology, code);
		value = getJson(url);
		json = appendJSON(json, "inverseAssociations", value);

		url = (String) hmap.get("roles");
		url = getURL(url, terminology, code);
		value = getJson(url);
		json = appendJSON(json, "roles", value);

		url = (String) hmap.get("inverseRoles");
		url = getURL(url, terminology, code);
		value = getJson(url);
		json = appendJSON(json, "inverseRoles", value);

		url = (String) hmap.get("parents");
		url = getURL(url, terminology, code);
		value = getJson(url);
		json = appendJSON(json, "superclasses", value);
        */
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
		url = url.replace("{terminology}", terminology);
		url = url.replace("{code}", code);
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
            gov.nih.nci.evs.restapi.model.ConceptDetails c2 = (ConceptDetails) deserialize("ConceptDetails", flattened_json);
            System.out.println(c2.toJson());

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
