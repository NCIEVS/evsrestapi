package gov.nih.nci.evs.restapi.util;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.net.*;
import java.util.*;

/**
 * <!-- LICENSE_TEXT_START -->
 * Copyright 2020, MSC. This software was developed in conjunction
 * with the National Cancer Institute, and so to the extent government
 * employees are co-authors, any rights in such works shall be subject
 * to Title 17 of the United States Code, section 105.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *   1. Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the disclaimer of Article 3,
 *      below. Redistributions in binary form must reproduce the above
 *      copyright notice, this list of conditions and the following
 *      disclaimer in the documentation and/or other materials provided
 *      with the distribution.
 *   2. The end-user documentation included with the redistribution,
 *      if any, must include the following acknowledgment:
 *      "This product includes software developed by MSC and the National
 *      Cancer Institute."   If no such end-user documentation is to be
 *      included, this acknowledgment shall appear in the software itself,
 *      wherever such third-party acknowledgments normally appear.
 *   3. The names "The National Cancer Institute", "NCI" and "MSC" must
 *      not be used to endorse or promote products derived from this software.
 *   4. This license does not authorize the incorporation of this software
 *      into any third party proprietary programs. This license does not
 *      authorize the recipient to use any trademarks owned by either NCI
 *      or NGIT
 *   5. THIS SOFTWARE IS PROVIDED "AS IS," AND ANY EXPRESSED OR IMPLIED
 *      WARRANTIES, (INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *      OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE) ARE
 *      DISCLAIMED. IN NO EVENT SHALL THE NATIONAL CANCER INSTITUTE,
 *      NGIT, OR THEIR AFFILIATES BE LIABLE FOR ANY DIRECT, INDIRECT,
 *      INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *      BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *      LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *      CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *      LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *      ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *      POSSIBILITY OF SUCH DAMAGE.
 * <!-- LICENSE_TEXT_END -->
 */

/**
 * @author EVS Team
 * @version 1.0
 *
 *          Modification history Initial implementation kim.ong@ngc.com
 *
 */


public class FTPUtils {
	static String NCIT_FTP_URL = "https://evs.nci.nih.gov/ftp1/NCI_Thesaurus/";
    static String currentWorkingDirectory;
    static {
		currentWorkingDirectory = System.getProperty("user.dir");
	}

	public static void download(String uri, String outputfile) {
		try (BufferedInputStream in = new BufferedInputStream(new URL(uri).openStream());
		  FileOutputStream fileOutputStream = new FileOutputStream(outputfile)) {
			byte dataBuffer[] = new byte[1024];
			int bytesRead;
			while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
				fileOutputStream.write(dataBuffer, 0, bytesRead);
			}
		} catch (IOException e) {
			// handle exception
		}
	}

    private static void unzip(String zipFilePath, String destDir) {
		System.out.println("zipFilePath: " + zipFilePath);
		System.out.println("destDir: " + destDir);

        File dir = new File(destDir);
        // create output directory if it doesn't exist
        if(!dir.exists()) dir.mkdirs();
        FileInputStream fis;
        //buffer for read and write data to file
        byte[] buffer = new byte[1024];
        try {
            fis = new FileInputStream(zipFilePath);
            ZipInputStream zis = new ZipInputStream(fis);
            ZipEntry ze = zis.getNextEntry();
            while(ze != null){
                String fileName = ze.getName();
                File newFile = new File(destDir + File.separator + fileName);
                System.out.println("Unzipping to "+newFile.getAbsolutePath());
                //create directories for sub directories in zip
                new File(newFile.getParent()).mkdirs();
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                   fos.write(buffer, 0, len);
                }
                fos.close();
                zis.closeEntry();
                ze = zis.getNextEntry();
            }
            //close last ZipEntry
            zis.closeEntry();
            zis.close();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void run(String NCIT_FTP_URL, String zipfile) {
		String currentWorkingDirectory = System.getProperty("user.dir");
		System.out.println("currentWorkingDirectory: " + currentWorkingDirectory);

        download(NCIT_FTP_URL + zipfile, zipfile);
        String zipFilePath = currentWorkingDirectory + "/" + zipfile;
        unzip(zipFilePath, currentWorkingDirectory);
	}

    public static boolean rename(String old_file, String new_file) {
		File oldfile = new File(old_file);
		File newfile = new File(new_file);

		if (!oldfile.exists()) {
			System.out.println("File " + old_file + " does not existis.");
			return false;
		}

		if (oldfile.renameTo(newfile)){
			return true;
		}
		return false;
	}

    public static String downloadNCItInferredOWL(String version) {
        //String zipfile = args[0];//"ThesaurusInf_20.05d.OWL.zip";
        String zipfile = "ThesaurusInf_" + version + ".OWL.zip";
        System.out.println("NCIT_FTP_URL: " + NCIT_FTP_URL);
        System.out.println("zipfile: " + zipfile);
        run(NCIT_FTP_URL, zipfile);
        int n = zipfile.indexOf("_");
        String old_file = zipfile.substring(0, n) + "erred.owl";
        int m = zipfile.indexOf("OWL");
        String new_file = zipfile.substring(0, m-1) + ".owl";
        System.out.println("oldfile: " + old_file);
        System.out.println("newfile: " + new_file);
        boolean bool = rename(old_file, new_file);
        if (bool) {
        	System.out.println("File rename successful? " + bool);
        	new File(zipfile).delete();
        	return new_file;
		}
        return null;
	}

    public static void main(String[] args) {
		long ms = System.currentTimeMillis();
		String version = args[0];
		String owlfile = downloadNCItInferredOWL(version);
		System.out.println("owlfile: " + owlfile);
		System.out.println("Total edit history run time (ms): " + (System.currentTimeMillis() - ms));
	}
}

