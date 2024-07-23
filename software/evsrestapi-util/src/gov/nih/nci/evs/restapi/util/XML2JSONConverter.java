package gov.nih.nci.evs.restapi.util;

import java.io.*;
import java.util.*;
import org.json.*;

public class XML2JSONConverter {
public static String xml= "<?xml version=\"1.0\" ?><root><test attribute=\"text1\">javatpoint</test><test attribute=\"text2\">JTP</test></root>";


//ADaM Terminology.odm.html
    public static String loadXML(String filename) {
	    StringBuffer buf = new StringBuffer();
	    Vector v = Utils.readFile(filename);
	    for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			if (!t.startsWith("#")) {
			    buf.append(t);
				buf.append("\n");
		    }
		}
		String query = buf.toString();
		return query;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String xml = loadXML(args[0]);
		try {
			JSONObject json = XML.toJSONObject(xml);
			String jsonString = json.toString(4);
			System.out.println(jsonString);

		}catch (JSONException e) {
			// TODO: handle exception
			System.out.println(e.toString());
		}
	}
}

