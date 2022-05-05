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

    public static Vector listFiles(String path) {
		Vector w = new Vector();
        File root = new File( path );
        File[] list = root.listFiles();
        if (list == null) return w;
        for (File f : list) {
            if (f.isDirectory()) {
                w.addAll(listFiles( f.getAbsolutePath()));
            } else {
                w.add(f.getAbsoluteFile());
            }
        }
        return w;
    }

	public static void copyDirectory(String sourceDirectoryLocation, String destinationDirectoryLocation)
		throws IOException {
		File target_dir = new File(destinationDirectoryLocation);

		Files.walk(Paths.get(sourceDirectoryLocation)).forEach(source -> {
		Path destination = Paths.get(destinationDirectoryLocation, source.toString().substring(sourceDirectoryLocation.length()));
		try {
			Files.copy(source, destination);
		} catch (IOException e) {
			e.printStackTrace();
		}
		});
	}

    public static void main(String[] args) {
		String currentWorkingDir = getCurrentWorkingDirectory();
		System.out.println("getCurrentWorkingDirectory: " + currentWorkingDir);
		String today = getToday("MMddyy");
		System.out.println("getToday: " + today);
		boolean exists = directoryExists(today);
		System.out.println("directory exist? " + exists);
		String dirname = currentWorkingDir + File.separator + today + File.separator;
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
		copyFile(currentWorkingDir, dirname, filesToCopy);
	}
}
