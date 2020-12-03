package gov.nih.nci.evs.restapi.util;

import java.io.*;
import java.util.*;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.*;


public class ZipUtils {
    private static final int BUFFER_SIZE = 4096;

	public ZipUtils() {

	}

	public static void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
		if (fileToZip.isHidden()) {
			return;
		}
		if (fileToZip.isDirectory()) {
			if (fileName.endsWith(File.separator)) {
				zipOut.putNextEntry(new ZipEntry(fileName));
				zipOut.closeEntry();
			} else {
				zipOut.putNextEntry(new ZipEntry(fileName + File.separator));
				zipOut.closeEntry();
			}

			File[] children = fileToZip.listFiles();
			for (File childFile : children) {
				zipFile(childFile, fileName + File.separator + childFile.getName(), zipOut);
			}
		   return;
		}

		FileInputStream fis = new FileInputStream(fileToZip);
		ZipEntry zipEntry = new ZipEntry(fileName);

		zipOut.putNextEntry(zipEntry);
		byte[] bytes = new byte[1024];
		int length;
		while ((length = fis.read(bytes)) >= 0) {
			zipOut.write(bytes, 0, length);
		}
		fis.close();

	}

    public static Vector listFilesInDirectory() {
		String dirName = null;
		return listFilesInDirectory(dirName);
	}


    public static Vector listFilesInDirectory(String dirName) {
		Vector v = new Vector();
		if (dirName == null) {
			dirName = System.getProperty("user.dir");;
		}
        File f = new File(dirName);

		File filesList[] = f.listFiles();
		for(File file : filesList) {
			v.add(file.getAbsolutePath());
		}
        return v;
	}

	public static void zipFile(String rooDir, String zipfle) throws IOException {
		FileOutputStream fos = new FileOutputStream(zipfle);
		ZipOutputStream zipOut = new ZipOutputStream(fos);
		Vector v = listFilesInDirectory(rooDir);
		for (int i=0 ;i<v.size(); i++) {
			String subfoldername = (String) v.elementAt(i);
			File fileToZip = new File(subfoldername);
			zipFile(fileToZip, fileToZip.getName(), zipOut);
		}
		zipOut.close();
		fos.close();
		System.out.println(zipfle + " generated.");
	}

    public static void unzip(String zipFilePath, String destDirectory) throws IOException {
        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            destDir.mkdir();
        }
        ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
        ZipEntry entry = zipIn.getNextEntry();
        // iterates over entries in the zip file
        while (entry != null) {
            String filePath = destDirectory + File.separator + entry.getName();
            if (!entry.isDirectory()) {
                // if the entry is a file, extracts it
                extractFile(zipIn, filePath);
            } else {
                // if the entry is a directory, make the directory
                File dir = new File(filePath);
                dir.mkdirs();
            }
            zipIn.closeEntry();
            entry = zipIn.getNextEntry();
        }
        zipIn.close();
    }

    private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read = 0;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }

	public static void main(String[] args) throws IOException {
		/*
		String rootDir = args[0];;
		String zipfle = args[1];
		zipFile(rootDir, zipfle);
		*/

        String zipfle = args[0];
		String destDir = args[1];;
		unzip(zipfle, destDir);

	}

}