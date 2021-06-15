package gov.nih.nci.evs.restapi.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class HTTPPostRequest {

	private static final String USER_AGENT = "Mozilla/5.0";

    private String GET_URL = "https://www.accessdata.fda.gov/scripts/cder/ob/search_product.cfm";
    private String POST_URL = "https://www.accessdata.fda.gov/scripts/cder/ob/search_product.cfm";

    private String POST_PARAMS = "discontinued=RX,OTC,DISCN&drugname=";

    private int number_of_columns = 0;
    private Vector data_vec = new Vector();

    public HTTPPostRequest() {

	}

	public void setGET_URL(String GET_URL) {
		this.GET_URL = GET_URL;
	}

	public void setPOST_URL(String POST_URL) {
		this.POST_URL = POST_URL;
	}

	public void setPOST_PARAMS(String POST_PARAMS) {
		this.POST_PARAMS = POST_PARAMS;
	}

    private String extractTable(String line) {
		int n = line.indexOf("<table");
		line = line.substring(n, line.length());
		n = line.lastIndexOf("</table>");
		line = line.substring(0, n + "</table>".length());
		return line;
	}

    private Vector extractTableHeadings(String line) {
		Vector th_vec = new Vector();
		int n = line.indexOf("</th>");
		while (n != -1) {
			String t = line.substring(0, n);
			int m = t.lastIndexOf(">");
			String s = t.substring(m+1, n);
			if (s.length() > 0) {
				if (!th_vec.contains(s)) {
					th_vec.add(s);
				} else {
					break;
				}
			} else {
				th_vec.add(s);
			}

			line = line.substring(n+1, line.length());
			n = line.indexOf("</th>");
		}
		return th_vec;
	}

    private Vector extractTableData(String line) {
		Vector td_vec = new Vector();
		int n = line.indexOf("</td>");
		while (n != -1) {
			String t = line.substring(0, n);
			int m = t.lastIndexOf(">");
			String s = t.substring(m+1, n);
			td_vec.add(s);
			line = line.substring(n+1, line.length());
			n = line.indexOf("</td>");
		}
		return td_vec;
	}


	private void sendGET() throws IOException {
		URL obj = new URL(GET_URL);
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
				response.append(inputLine);
			}
			in.close();

			// print result
			System.out.println(response.toString());
		} else {
			System.out.println("GET request not worked");
		}
	}

	private String toHeadingLine(Vector v) {
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			buf.append(t);
			if (i < v.size()-1) {
				buf.append("\t");
			}
		}
		return buf.toString();
	}

	private Vector toDataLines(Vector v) {
		Vector dt_vec = new Vector();
		StringBuffer buf = new StringBuffer();
		int j = 0;
		for (int i=0; i<v.size(); i++) {
			j++;
			String t = (String) v.elementAt(i);
			buf.append(t);
			if (j < number_of_columns) {
				buf.append("\t");
			} else if (j == number_of_columns) {
				dt_vec.add(buf.toString());
				buf = new StringBuffer();
				j = 0;
			}
		}
		return dt_vec;
	}

	private Vector sendPOST(Vector drugnames) throws IOException {
		Vector w = new Vector();
		int j = 0;
		data_vec = new Vector();
        for (int i=0; i<drugnames.size(); i++) {
			try {
				String drugname = (String) drugnames.elementAt(i);
				j++;
				System.out.println("(" + j + ") " + drugname);
				URL obj = new URL(POST_URL);
				HttpURLConnection con = (HttpURLConnection) obj.openConnection();
				con.setRequestMethod("POST");
				con.setRequestProperty("User-Agent", USER_AGENT);

				con.setDoOutput(true);
				OutputStream os = con.getOutputStream();
				String params = POST_PARAMS + drugname;
				os.write(params.getBytes());
				os.flush();
				os.close();

				int responseCode = con.getResponseCode();
				if (responseCode == HttpURLConnection.HTTP_OK) {
					BufferedReader in = new BufferedReader(new InputStreamReader(
							con.getInputStream()));
					String inputLine;
					StringBuffer response = new StringBuffer();

					while ((inputLine = in.readLine()) != null) {
						response.append(inputLine);
					}
					in.close();
					String s = response.toString();
					s = s.trim();
					s = extractTable(s);
					w.add(drugname + "|" + s);
                    if (i == 0) {
						Vector th_vec = extractTableHeadings(s);
						number_of_columns = th_vec.size();
						data_vec.add(toHeadingLine(th_vec));
					}
                    Vector td_vec = extractTableData(s);
                    data_vec.addAll(toDataLines(td_vec));

				} else {
					System.out.println("WARNING: POST request failed " + drugname);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		return data_vec;
	}

	public static void main(String[] args) throws IOException {
		long ms = System.currentTimeMillis();
		HTTPPostRequest httpPostRequest = new HTTPPostRequest();
		String filename = args[0];
		Vector drugnames = Utils.readFile(filename);
		Vector w = httpPostRequest.sendPOST(drugnames);
		String textfile = "FDA_Approved_Drug_Data.txt";
		Utils.saveToFile(textfile, w);

		char delimiter = '\t';
		try {
			String csvfile = Text2CSV.toCSV(textfile, delimiter);
			System.out.println(csvfile + " generated.");
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}
}