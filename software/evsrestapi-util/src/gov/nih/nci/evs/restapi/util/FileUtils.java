package gov.nih.nci.evs.restapi.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

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


    public static void main(String[] args) {
		Vector files = gov.nih.nci.evs.restapi.util.Utils.readFile(args[0]);
        for(int i=0; i<files.size(); i++) {
			String line = (String) files.elementAt(i);
			Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(line, '|');
			String src_file = (String) u.elementAt(0);
			String target_file = (String) u.elementAt(1);
			copyfile(src_file, target_file);
		}

	}
}
