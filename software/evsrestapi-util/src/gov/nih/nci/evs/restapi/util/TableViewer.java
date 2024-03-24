package gov.nih.nci.evs.restapi.util;

import java.io.*;
import java.util.*;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

public class TableViewer extends JFrame
{
	public String datafile = null;
	public String[] columns = null;
	public String title = null;
	public char delim = '\t';

	public String[] getTableHeadings() {
	    Vector v = Utils.readFile(datafile);
	    int m = datafile.lastIndexOf(".");
	    title = datafile.substring(0, m);
	    String firstLine = (String) v.elementAt(0);
	    if (firstLine.indexOf("\t") == -1) {
			delim = '|';
		}
	    Vector u = StringUtils.parseData(firstLine, delim);
	    String[] headings = new String[u.size()];
	    for (int i=0; i<u.size(); i++) {
			String heading = (String) u.elementAt(i);
			headings[i] = heading;
		}
		return headings;
	}

	public Object[][] getTableData() {
		int num_cols = columns.length;
		Vector v = Utils.readFile(datafile);
		int num_rows = v.size()-1;
		Object[][] data = new Object[num_rows][num_cols];
		for (int i=1; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, delim);
			for (int j=0; j<u.size(); j++) {
				data[i-1][j] = u.elementAt(j);
			}
		}
		return data;
	}


    public TableViewer(String datafile) {
		this.datafile = datafile;
        columns = getTableHeadings();
        Object[][] data = getTableData();
         JTable table = new JTable(data, columns);
        this.add(new JScrollPane(table));
        this.setTitle(title);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.pack();
        this.setVisible(true);
    }

    public static void show(String datafile) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new TableViewer(datafile);
            }
        });
    }

    public static void main(String[] args) {
		String datafile = args[0];
		show(datafile);
    }
}