package gov.nih.nci.evs.restapi.util;

import gov.nih.nci.evs.restapi.client.bean.*;
import java.io.*;
import java.io.IOException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.*;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;


public class ObjectMappingUtils {

	static String BASE_URL = "https://api-evsrest.nci.nih.gov/api/v1/";

	public static String ASSOCIATIONS = "associations";
	public static String CHILDREN = "children";
	public static String DESCENDANTS = "descendants";
	public static String DISJOINTWITH = "disjointWith";
	public static String INVERSEASSOCIATIONS = "inverseAssociations";
	public static String INVERSEROLES = "inverseRoles";
	public static String MAPS = "maps";
	public static String PARENTS = "parents";
	public static String ROLES = "roles";

	public static String[] HIERARCHICAL_RELATIONSHIPS = new String[] {CHILDREN, PARENTS};

	public static String[] RELATIONSHIPS = new String[] {ASSOCIATIONS, DISJOINTWITH, INVERSEASSOCIATIONS,
	                                                    INVERSEROLES, MAPS, ROLES};

    public static void setBASE_URL(String url) {
		BASE_URL = url;
	}

	public static void run(String[] args) {
		String inputfile = args[0];
		run(inputfile);
	}

	public static Vector readFile(String filename)
	{
		Vector v = new Vector();
		try {
			BufferedReader in = new BufferedReader(
			   new InputStreamReader(
						  new FileInputStream(filename), "UTF8"));
			String str;
			while ((str = in.readLine()) != null) {
				v.add(str);
			}
            in.close();
		} catch (Exception ex) {
            ex.printStackTrace();
		}
		return v;
	}

	 public static String replaceFilename(String filename) {
	    return filename.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
	 }

	 public static void saveToFile(String outputfile, Vector v) {
		outputfile = replaceFilename(outputfile);
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(outputfile, "UTF-8");
			if (v != null && v.size() > 0) {
				for (int i=0; i<v.size(); i++) {
					String t = (String) v.elementAt(i);
					pw.println(t);
				}
		    }
		} catch (Exception ex) {

		} finally {
			try {
				pw.close();
				System.out.println("Output file " + outputfile + " generated.");
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	 }

	public static void saveToFile(PrintWriter pw, Vector v) {
		if (v != null && v.size() > 0) {
			for (int i=0; i<v.size(); i++) {
				String t = (String) v.elementAt(i);
				pw.println(t);
			}
		}
	}

    public static String getJsonFromFile(String filename) {
		Vector u = readFile(filename);
		return (String) u.elementAt(0);
	}

	private static void run(String inputfile) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			String jsonInString = getJsonFromFile(inputfile);
			SearchResult cls = mapper.readValue(jsonInString, SearchResult.class);
			System.out.println(cls.toJson());
			System.out.println("\n" + cls.toXML());

		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static SearchResult json2SearchResult(String jsonInString) {
		ObjectMapper mapper = new ObjectMapper();
		SearchResult cls = null;
		try {
			cls = mapper.readValue(jsonInString, SearchResult.class);
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return cls;
	}

	public static SearchResultDetails json2SearchResultDetails(String jsonInString) {
		ObjectMapper mapper = new ObjectMapper();
		SearchResultDetails cls = null;
		try {
			cls = mapper.readValue(jsonInString, SearchResultDetails.class);
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return cls;
	}


	public static ConceptDetails json2ConceptDetails(String jsonInString) {
		ObjectMapper mapper = new ObjectMapper();
		ConceptDetails cls = null;
		try {
			cls = mapper.readValue(jsonInString, ConceptDetails.class);
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return cls;
	}

	public static Concept[] json2ConceptList(String jsonInString) {
		ObjectMapper mapper = new ObjectMapper();
		Concept[] cls = null;
		try {
			cls = mapper.readValue(jsonInString, Concept[].class);
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return cls;
	}

	public static RelatedConcept[] json2RelatedConceptList(String jsonInString) {
		ObjectMapper mapper = new ObjectMapper();
		RelatedConcept[] cls = null;
		try {
			cls = mapper.readValue(jsonInString, RelatedConcept[].class);
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return cls;
	}

	public static ConceptDetails[] json2ConceptDetailsList(String jsonInString) {
		ObjectMapper mapper = new ObjectMapper();
		ConceptDetails[] cls = null;
		try {
			cls = mapper.readValue(jsonInString, ConceptDetails[].class);
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return cls;
	}

	public static HierarchicallyRelatedConcept[] json2HierarchicallyRelatedConceptList(String jsonInString) {
		ObjectMapper mapper = new ObjectMapper();
		HierarchicallyRelatedConcept[] cls = null;
		try {
			cls = mapper.readValue(jsonInString, HierarchicallyRelatedConcept[].class);
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return cls;
	}

	public static Node[] json2NodeList(String jsonInString) {
		ObjectMapper mapper = new ObjectMapper();
		Node[] cls = null;
		try {
			cls = mapper.readValue(jsonInString, Node[].class);
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return cls;
	}

	public static TreeNode[] json2TreeNodeList(String jsonInString) {
		ObjectMapper mapper = new ObjectMapper();
		TreeNode[] cls = null;
		try {
			cls = mapper.readValue(jsonInString, TreeNode[].class);
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return cls;
	}

	public static List json2PathList(String jsonInString) {
		ObjectMapper mapper = new ObjectMapper();
		List cls = null;
		try {
			cls = mapper.readValue(jsonInString, List.class);
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return cls;
	}

	public static Metadata[] json2MetadataList(String jsonInString) {
		ObjectMapper mapper = new ObjectMapper();
		Metadata[] cls = null;
		try {
			cls = mapper.readValue(jsonInString, Metadata[].class);
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return cls;
	}

    public gov.nih.nci.evs.restapi.client.bean.SearchResult searchTerm(String terminology, String term, int fromRecord, int pageSize, String algorithm) {
        String url = BASE_URL + "concept/" + terminology + "/search?fromRecord=" + fromRecord + "&include=minimal&pageSize=" + pageSize
           + "&term=" + term + "&type=" + algorithm;
		String json = EVSRESTAPIClient.getJson(url);
		gov.nih.nci.evs.restapi.client.bean.SearchResult sr = ObjectMappingUtils.json2SearchResult(json);
		return sr;
	}

    public gov.nih.nci.evs.restapi.client.bean.Concept[] getRoots(String terminology) {
	    String url = BASE_URL + "concept/" + terminology + "/roots?include=minimal";
	    String json = EVSRESTAPIClient.getJson(url);
	    gov.nih.nci.evs.restapi.client.bean.Concept[] list = ObjectMappingUtils.json2ConceptList(json);
	    return list;
	}

    public gov.nih.nci.evs.restapi.client.bean.ConceptDetails getConceptDetails(String terminology, String code) {
	    String url = BASE_URL + "concept/" + terminology + "/" + code + "?include=full";
	    String json = EVSRESTAPIClient.getJson(url);
	    gov.nih.nci.evs.restapi.client.bean.ConceptDetails cd = ObjectMappingUtils.json2ConceptDetails(json);
	    return cd;
	}

    public gov.nih.nci.evs.restapi.client.bean.SearchResultDetails searchProperty(String terminology, String term, String include, int fromRecord, int pageSize, String propertyName, String algorithm) {
        String url = BASE_URL + "concept/search?fromRecord=" + fromRecord + "&include=" + include + "&pageSize=" + pageSize + "&synonymType=" + propertyName + "&terminology=" + terminology + "&type=" + algorithm;
        if (term != null) {
			url = url + "&term=" + term;
		}
		String json = EVSRESTAPIClient.getJson(url);
		gov.nih.nci.evs.restapi.client.bean.SearchResultDetails sr = ObjectMappingUtils.json2SearchResultDetails(json);
		return sr;
	}


    public gov.nih.nci.evs.restapi.client.bean.RelatedConcept[] getRelatedConceptsList(String terminology, String code, String relationship) {
		String url = BASE_URL + "concept/" + terminology + "/" + code + "/" + relationship;
	    String json = EVSRESTAPIClient.getJson(url);
	    gov.nih.nci.evs.restapi.client.bean.RelatedConcept[] list = ObjectMappingUtils.json2RelatedConceptList(json);
	    return list;
	}

    public gov.nih.nci.evs.restapi.client.bean.HierarchicallyRelatedConcept[] getHierarchicallyRelatedConceptList(String terminology, String code, String relationship) {
		String url = BASE_URL + "concept/" + terminology + "/" + code + "/" + relationship;
	    String json = EVSRESTAPIClient.getJson(url);
	    gov.nih.nci.evs.restapi.client.bean.HierarchicallyRelatedConcept[] list = ObjectMappingUtils.json2HierarchicallyRelatedConceptList(json);
	    return list;
	}

    public gov.nih.nci.evs.restapi.client.bean.Node[] getDescendants(String terminology, String code) {
		String relationship = "descendants";
		String url = BASE_URL + "concept/" + terminology + "/" + code + "/" + relationship;
	    String json = EVSRESTAPIClient.getJson(url);
	    gov.nih.nci.evs.restapi.client.bean.Node[] list = ObjectMappingUtils.json2NodeList(json);
	    return list;
	}

    public gov.nih.nci.evs.restapi.client.bean.TreeNode[] getSubtree(String terminology, String code) {
		String relationship = "subtree";
		String url = BASE_URL + "concept/" + terminology + "/" + code + "/" + relationship;
	    String json = EVSRESTAPIClient.getJson(url);

	    gov.nih.nci.evs.restapi.client.bean.TreeNode[] list = ObjectMappingUtils.json2TreeNodeList(json);
	    return list;
	}

    public gov.nih.nci.evs.restapi.client.bean.Node[] getChildren(String terminology, String code) {
		String relationship = "children";
		String url = BASE_URL + "concept/" + terminology + "/" + code + "/" + relationship;
	    String json = EVSRESTAPIClient.getJson(url);
	    gov.nih.nci.evs.restapi.client.bean.Node[] list = ObjectMappingUtils.json2NodeList(json);
	    return list;
	}

	static boolean getPrimitive(Boolean value) {
			return Boolean.parseBoolean("" + value);
	}

    public static Vertex linkedHashMap2Vertex(LinkedHashMap hmap) {
		String code = (String) hmap.get("code");
		String name = (String) hmap.get("name");
		String terminology = (String) hmap.get("terminology");
		String version = (String) hmap.get("version");
		Integer level_obj = (Integer) hmap.get("level");
		int level = level_obj.intValue();

		Boolean leaf_obj = (Boolean) hmap.get("leaf");
		boolean leaf = getPrimitive(leaf_obj);

		return new Vertex(
			code,
			name,
			terminology,
			version,
			level,
			leaf);
	}

    public gov.nih.nci.evs.restapi.client.bean.Paths getPaths(String terminology, String code) {
		return getPaths(terminology, code, true);
	}

    public gov.nih.nci.evs.restapi.client.bean.Paths getPaths(String terminology, String code, boolean traverseUp) {
		String relationship = "pathsToRoot";
		if (!traverseUp) {
			relationship = "pathsFromRoot";
		}
		String url = BASE_URL + "concept/" + terminology + "/" + code + "/" + relationship + "?include=minimal";
	    String json = EVSRESTAPIClient.getJson(url);
        List list = ObjectMappingUtils.json2PathList(json);
        gov.nih.nci.evs.restapi.client.bean.Paths paths = new gov.nih.nci.evs.restapi.client.bean.Paths();
        List pathList = new ArrayList();
        for (int i=0; i<list.size(); i++) {
			List linkedHashMapList = (List) list.get(i);
			gov.nih.nci.evs.restapi.client.bean.Path path = new gov.nih.nci.evs.restapi.client.bean.Path();
			List vertexList = new ArrayList();
			for (int j=0; j<linkedHashMapList.size(); j++) {
				LinkedHashMap hmap = (LinkedHashMap) linkedHashMapList.get(j);
				Vertex vertex = linkedHashMap2Vertex(hmap);
				vertexList.add(vertex);
			}
			path.setPath(vertexList);
			pathList.add(path);
		}
		paths.setPaths(pathList);
		return paths;
	}

    public gov.nih.nci.evs.restapi.client.bean.Concept[] resolveSubset(String terminology, String code) {
		String relationship = "subsetMembers";
		String url = BASE_URL + "concept/" + terminology + "/" + relationship + "/" + code;
		String json = EVSRESTAPIClient.getJson(url);
        gov.nih.nci.evs.restapi.client.bean.Concept[] concepts = ObjectMappingUtils.json2ConceptList(json);
        return concepts;
	}

    public gov.nih.nci.evs.restapi.client.bean.Metadata[] getMetadata(String terminology, String type) {
		String url = BASE_URL + "metadata/" + terminology + "/" + type + "?include=minimal";
		String json = EVSRESTAPIClient.getJson(url);
        gov.nih.nci.evs.restapi.client.bean.Metadata[] list = ObjectMappingUtils.json2MetadataList(json);
        return list;
	}
}



