package gov.nih.nci.evs.restapi.util;

import java.io.*;
import java.util.*;
import java.nio.file.*;
import java.text.*;

public class SetupUtils {

	public static String getToday() {
		//return getToday("MM-dd-yyyy");
		return getToday("MMddyyyy");
	}

	public static String getToday(String format) {
		java.util.Date date = Calendar.getInstance().getTime();
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		return sdf.format(date);
	}

	public static void copyDirectory(String sourceDirectoryLocation, String destinationDirectoryLocation)
	  throws IOException {
	    Files.walk(Paths.get(sourceDirectoryLocation))
	      .forEach(source -> {
		  Path destination = Paths.get(destinationDirectoryLocation, source.toString()
		    .substring(sourceDirectoryLocation.length()));
		  try {
		      Files.copy(source, destination);
		  } catch (IOException e) {
		      e.printStackTrace();
		  }
	      });
	}

	public static void main(String[] args) {
         try {
			 String sourceDirectoryLocation = "baseDir";
			 String today = getToday();
			 System.out.println(today);
			 String destinationDirectoryLocation = today;
			 copyDirectory(sourceDirectoryLocation, destinationDirectoryLocation);

		 } catch (Exception ex) {
			 ex.printStackTrace();
		 }
	}
}
