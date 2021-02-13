package gov.nih.nci.evs.restapi.util;
import java.io.*;
import java.util.*;
import java.util.stream.*;

import java.text.*;
import java.nio.file.*;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class Text2CSV {

	public static String toCSV(String textfile) {
		return toCSV(".", textfile, '\t');
	}

	public static String toCSV(String textfile, char delimiter) {
		return toCSV(".", textfile, delimiter);
	}

	public static String toCSV(String dir, String textfile, char delimiter) {
		Path path = Paths.get(dir);
		int n = textfile.lastIndexOf(".");
		String delim = "" + delimiter;
		String csvfile = textfile.substring(0, n) + ".csv";

		final Path txt = path.resolve(textfile);
		final Path csv = path.resolve(csvfile);
		try (
			final Stream<String> lines = Files.lines(txt);
			final PrintWriter pw = new PrintWriter(Files.newBufferedWriter(csv, StandardOpenOption.CREATE_NEW))) {
				lines.map((line) -> line.split(delim)).
				map((line) -> Stream.of(line).collect(Collectors.joining(","))).
				forEach(pw::println);
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
		return csvfile;
	}

	public static void main(String[] args) throws Exception {
		//String dir = "output";
		String dir = ".";
		String textfile = "NCIt-ChEBI_Mapping.txt";
		char delimiter = '\t';
		try {
			String csvfile = toCSV(dir, textfile, delimiter);
			System.out.println(csvfile + " generated.");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}

