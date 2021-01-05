package gov.nih.nci.evs.restapi.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.text.*;
import java.nio.file.*;

public class FileUtils
{
    public static void copyfile(String src_file, String target_file) {
    	InputStream inStream = null;
	    OutputStream outStream = null;
    	try{
    	    File afile =new File(src_file);
    	    File bfile =new File(target_file);
    	    inStream = new FileInputStream(afile);
    	    outStream = new FileOutputStream(bfile);
    	    byte[] buffer = new byte[1024];
    	    int length;
    	    while ((length = inStream.read(buffer)) > 0){
    	    	outStream.write(buffer, 0, length);
    	    }
    	    inStream.close();
    	    outStream.close();
    	    System.out.println("File copied from " + src_file + " to " + target_file);
    	}catch(IOException e){
    		e.printStackTrace();
    	}
    }

    public static boolean fileExists(String filename) {
		File f = new File(filename);
		if(f.exists() && !f.isDirectory()) {
            return true;
		}
		return false;
	}

    public static boolean directoryExists(String filename) {
		File f = new File(filename);
		if(f.exists()) {
            return true;
		}
		return false;
	}

	public static String getCurrentWorkingDirectory() {
		return System.getProperty("user.dir");
	}

	public static String getToday() {
		return getToday("MM-dd-yyyy");
	}

	public static String getToday(String format) {
		java.util.Date date = Calendar.getInstance().getTime();
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		return sdf.format(date);
	}

    public static boolean createDirectory(String pathname) {
		Path path = Paths.get(pathname);
		try {
			Files.createDirectory(path);
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

    public static void copyFile(String sourceDir, String targetDir, Vector filesToCopy) {
		for (int i=0; i<filesToCopy.size(); i++) {
			String filename = (String) filesToCopy.elementAt(i);
			String from = sourceDir + File.separator + filename;
			String to = targetDir + File.separator + filename;
			copyfile(from, to);
		}
	}

    public static Vector getFilesInDirectory(String dirpathname) {
        File f = new File(dirpathname);
        Vector v = new Vector();
        String[] pathnames = f.list();
        for (String pathname : pathnames) {
            v.add(pathname);
        }
        return v;
    }

    public static void copyAll(String sourceDir, String targetDir) {
		copyFile(sourceDir, targetDir, getFilesInDirectory(sourceDir));
	}

    public static void main1(String[] args) {
		String currentWirkingDir = getCurrentWorkingDirectory();
		System.out.println("getCurrentWorkingDirectory: " + currentWirkingDir);
		String today = getToday("MMddyy");
		System.out.println("getToday: " + today);
		boolean exists = directoryExists(today);
		System.out.println("directory exist? " + exists);
		String dirname = currentWirkingDir + File.separator + today + File.separator;
		if (!exists) {
			System.out.println(dirname);
    		boolean created = createDirectory(dirname);
    		System.out.println("directory created? " + created);
		}
		exists = directoryExists(dirname);
		System.out.println("directory exist? " + exists);
		Vector filesToCopy = new Vector();
		filesToCopy.add("cmd.exe");
		filesToCopy.add("run.bat");
		copyFile(currentWirkingDir, dirname, filesToCopy);
	}

	public static void main(String[] args) {
		String sourceDir = args[4];
		String targetDir = args[5];
		copyAll(sourceDir, targetDir);
	}
}
