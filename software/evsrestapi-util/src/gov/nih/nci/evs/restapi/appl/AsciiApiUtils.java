package gov.nih.nci.evs.restapi.appl;

import gov.nih.nci.evs.restapi.util.*;

import java.io.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

import gov.nih.nlm.nls.lvg.Api.*;
import gov.nih.nlm.nls.lvg.Lib.*;

// This class uses ToAsciiApi to convert an input term to ASCII
public class AsciiApiUtils
{
	private ToAsciiApi api = null;

	public AsciiApiUtils() {
		Hashtable<String, String> properties = new Hashtable<String, String>();
		String currentDirectory = System.getProperty("user.dir");
		properties.put("LVG_DIR", currentDirectory + "/");
		api = new ToAsciiApi(properties);
	}

	public AsciiApiUtils(String currentDirectory) {
		Hashtable<String, String> properties = new Hashtable<String, String>();
		if (currentDirectory == null) {
			currentDirectory = System.getProperty("user.dir");
		}
		properties.put("LVG_DIR", currentDirectory + "/");
		api = new ToAsciiApi(properties);
	}


	public Vector readFromFile(String filename) {
		Vector v = new Vector();
		try {
			BufferedReader in = new BufferedReader(
			   new InputStreamReader(
				  new FileInputStream(filename)));
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

	public void writeToFile(String outputfile, String content) {
		try {
			File file = new File(outputfile);
			if (!file.exists()) {
				file.createNewFile();
			}
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(content);
			bw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String toAscii(String inTerm) {
		// mutate the inTerm
		String outTerm = api.Mutate(inTerm);
		return outTerm;
	}

	public String getFootNote() {
		return "<!-- Edited by LVG ToAsciiApi (version 2020) -->";
	}

    //scan non-ascii characters in a file
	public void scan(String inputfile, String outputfile) {
		try {
			File file = new File(outputfile);
			if (!file.exists()) {
				file.createNewFile();
			}
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);

			Vector lines = readFromFile(inputfile);
			for (int i=0; i<lines.size(); i++) {
				int j = i+1;
				String line = (String) lines.elementAt(i);
				String content = toAscii(line);
				String msg = null;
				if (line.compareTo(content) != 0) {
					msg = "WARNING: Line " + j + "  contains non--ascii characters: " + line;
					bw.write(msg);
					bw.write("\n");
				}
			}
			bw.write(getFootNote());
			bw.close();
			System.out.println("Output file " + outputfile + " generated.");

		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public void run(String inputfile, String outputfile) {
		try {
			File file = new File(outputfile);
			if (!file.exists()) {
				file.createNewFile();
			}
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);

			Vector lines = readFromFile(inputfile);
			for (int i=0; i<lines.size(); i++) {
				String line = (String) lines.elementAt(i);
				String content = toAscii(line);
				bw.write(content);
				bw.write("\n");
			}
			bw.write(getFootNote());
			bw.close();
			System.out.println("Output file " + outputfile + " generated.");

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main (String[] args)
	{
		if(args.length != 1)
		{
			System.err.println("Usage: java AsciiApiUtils <inputfile>");
			System.exit(1);
		}
		AsciiApiUtils utils = new AsciiApiUtils();
		String inputfile = args[0];
		//Vector v = readFile(inputfile);
		int n = inputfile.lastIndexOf(".");
		String outputfile = inputfile.substring(0, n) + "_" + StringUtils.getToday() + inputfile.substring(n, inputfile.length());
		String warning_file = "warning_" + outputfile;
        new AsciiApiUtils().scan(inputfile, warning_file);
		new AsciiApiUtils().run(inputfile, outputfile);
	}
}
