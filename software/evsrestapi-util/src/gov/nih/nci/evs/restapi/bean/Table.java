package gov.nih.nci.evs.restapi.bean;
import gov.nih.nci.evs.restapi.util.*;

import java.io.*;
import java.util.*;
import java.net.*;

import com.google.gson.*;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.XStream;

public class Table
{

// Variable declaration
	private String label;
	private List<String> headings;
	private List<Row> rows;

// Default constructor
	public Table() {
	}

// Constructor
	public Table(
		String label,
		List<String> headings,
		List<Row> rows) {

		this.label = label;
		this.headings = headings;
		this.rows = rows;
	}


	public Table(
		String label,
		Vector heading_vec,
		Vector data_vec) {
		this.label = label;
		this.headings = new ArrayList();
		for (int i=0; i<heading_vec.size(); i++) {
			String heading = (String) heading_vec.elementAt(i);
		    this.headings.add(heading);
		}
		this.rows = new ArrayList();
		for (int i=0; i<data_vec.size(); i++) {
			String line = (String) data_vec.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			List list = new ArrayList();
			for (int j=0; j<u.size(); j++) {
				String cell_val = (String) u.elementAt(j);
				list.add(cell_val);
			}
			this.rows.add(new Row(list));
		}
	}


// Set methods
	public void setLabel(String label) {
		this.label = label;
	}

	public void setHeadings(List<String> headings) {
		this.headings = headings;
	}

	public void setRows(List<Row> rows) {
		this.rows = rows;
	}


// Get methods
	public String getLabel() {
		return this.label;
	}

	public List<String> getHeadings() {
		return this.headings;
	}

	public List<Row> getRows() {
		return this.rows;
	}

	public String toXML() {
		XStream xstream_xml = new XStream(new DomDriver());
		String xml = xstream_xml.toXML(this);
		xml = escapeDoubleQuotes(xml);
		StringBuffer buf = new StringBuffer();
		String XML_DECLARATION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		buf.append(XML_DECLARATION).append("\n").append(xml);
		xml = buf.toString();
		return xml;
	}

	public String toJson() {
		JsonParser parser = new JsonParser();
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
	}

	public String escapeDoubleQuotes(String inputStr) {
		char doubleQ = '"';
		StringBuffer buf = new StringBuffer();
		for (int i=0;  i<inputStr.length(); i++) {
			char c = inputStr.charAt(i);
			if (c == doubleQ) {
				buf.append(doubleQ).append(doubleQ);
			}
			buf.append(c);
		}
		return buf.toString();
	}


	public static String table2String(Table table) {
		StringBuffer buf = new StringBuffer();
		buf.append("<div>").append("\n");
		buf.append("<center>").append("\n");
		buf.append("<table>").append("\n");
		buf.append("<tr>").append("\n");
		List<String> headings = table.getHeadings();
		for (int i=0; i<headings.size(); i++) {
			String heading = headings.get(i);
			buf.append("<th>").append("\n");
			buf.append(heading).append("\n");
			buf.append("</th>").append("\n");
		}
		buf.append("<tr>").append("\n");
		List<Row> rows = table.getRows();
		for (int i=0; i<rows.size(); i++) {
			buf.append("<tr>").append("\n");
			Row row = rows.get(i);
			List cell_values = row.getrow();
			for (int j=0; j<cell_values.size(); j++) {
				String cell_value = (String) cell_values.get(j);
				buf.append("<td>").append("\n");
				buf.append(cell_value).append("\n");
				buf.append("</td>").append("\n");
			}
			buf.append("</tr>").append("\n");
		}
		buf.append("</table>").append("\n");
		buf.append("</div>").append("\n");
		return buf.toString();
	}


	public static Table construct_table(
		String label,
		Vector heading_vec,
		Vector data_vec) {
		List headings = new ArrayList();
		for (int i=0; i<heading_vec.size(); i++) {
			String heading = (String) heading_vec.elementAt(i);
			headings.add(heading);
		}
		List<Row> rows = new ArrayList();
		for (int i=0; i<data_vec.size(); i++) {
			List list = new ArrayList();
			String data = (String) data_vec.elementAt(i);
			Vector u = StringUtils.parseData(data, '|');
			List row_data = new ArrayList();
			for (int j=0; j<u.size(); j++) {
				String cell = (String) u.elementAt(j);
				row_data.add(cell);
			}
			Row row = new Row(row_data);
			rows.add(row);
		}
		return new Table(label, headings, rows);
	}
}
