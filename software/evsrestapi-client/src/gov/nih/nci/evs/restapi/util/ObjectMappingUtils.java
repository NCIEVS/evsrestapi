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
}



