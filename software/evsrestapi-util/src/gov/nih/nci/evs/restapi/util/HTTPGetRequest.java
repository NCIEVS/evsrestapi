package gov.nih.nci.evs.restapi.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class HTTPGetRequest {

	private static final String USER_AGENT = "Mozilla/5.0";
	private static String ALPHABETIC = "abcdefghijklmnopqrstuvwxyz";

    private String GET_URL = "https://druginfo.nlm.nih.gov/drugportal/drug/names/";

    private int number_of_columns = 0;
    private Vector data_vec = new Vector();

    public HTTPGetRequest() {

	}

	public void setGET_URL(String GET_URL) {
		this.GET_URL = GET_URL;
	}

	public Vector extractRowData(Vector w) {
		Vector v = new Vector();
		for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			line = line.trim();
			if (line.startsWith("<tr><td align=\"left\"><a href=")) {
				line = line.replace("/drugportal", "https://druginfo.nlm.nih.gov/drugportal");
				v.add(line);
			}
		}
		return v;
	}

	public Vector sendGET(String url_str) throws IOException {
		Vector w = new Vector();
		URL obj = new URL(url_str);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setRequestMethod("GET");
		con.setRequestProperty("User-Agent", USER_AGENT);
		int responseCode = con.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK) { // success
			BufferedReader in = new BufferedReader(new InputStreamReader(
					con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				w.add(inputLine);
			}
			in.close();
		} else {
			System.out.println("GET request failed.");
		}
		return w;
	}


	public Vector sendGET(char c) throws IOException {
		Vector w = new Vector();
		URL obj = new URL(GET_URL + c);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setRequestMethod("GET");
		con.setRequestProperty("User-Agent", USER_AGENT);
		int responseCode = con.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK) { // success
			BufferedReader in = new BufferedReader(new InputStreamReader(
					con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				w.add(inputLine);
			}
			in.close();
		} else {
			System.out.println("GET request failed.");
		}
		return w;
	}

	public String getAlphabeticListing() {
		StringBuffer buf = new StringBuffer();
	    for (int i=0; i<ALPHABETIC.length(); i++) {
			char ch = ALPHABETIC.charAt(i);
			String t = "" + ch;
			String t_upper = t.toUpperCase();
			//&nbsp;
			//https://druginfo.nlm.nih.gov/drugportal/drug/names
			buf.append("<a href=\"https://druginfo.nlm.nih.gov/drugportal/drug/names/" + t + "\">" + t_upper + "</a> &nbsp;&nbsp;");
		}
		return buf.toString();
	}

    public void generate() {
        Vector w = new Vector();
        w.add("<html>");
        w.add("<head>");
        w.add("</head>");
        w.add("<body style=\"background-color:LightGray;\">");
        w.add("<h2>");
        w.add("<center>FDA Approved Drugs</center>");
        w.add("<h4>");
        w.add("<center>(Last Updated on " + StringUtils.getToday() + ")</center>");
        w.add("<center>");
        w.add("<p></p>");
        w.add(getAlphabeticListing());
        w.add("<p></p>");
        w.add("<table border=\"0\">");
 	    w.add("<tr><th scope=\"col\">Drug Names</th></tr>");
	    for (int i=0; i<ALPHABETIC.length(); i++) {
			char ch = ALPHABETIC.charAt(i);
			try {
				Vector v = sendGET(ch);
				v = extractRowData(v);
				w.addAll(v);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		w.add("</table>");
		w.add("</center>");
		w.add("</body>");
        w.add("</html>");

        Utils.saveToFile("FDAApprovedDrugs.html", w);
	}

	public static void main(String[] args) throws IOException {
		long ms = System.currentTimeMillis();
		HTTPGetRequest httpGetRequest = new HTTPGetRequest();
        httpGetRequest.generate();
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}
}
