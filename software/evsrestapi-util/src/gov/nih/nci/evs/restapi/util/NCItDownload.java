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


public class NCItDownload {

    public static String NCIt_UPLOAD_URL = "https://evs.nci.nih.gov/ftp1/upload/";
    public static String NCIT_ZIP_FILE = "ThesaurusInferred_forTS.zip";

    public static String generatePrescrubFilename(String serviceUrl, String username, String password) {
		MetadataUtils metadataUtils = new MetadataUtils(serviceUrl, username, password);
		String version = metadataUtils.getLatestVersion("NCI_Thesaurus");
		return "CTRPThesaurusInferred-" + version + ".owl-lvg.owl.zip";
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

    public static void unzip(String zipFilePath, String destDir) {
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
                //close this ZipEntry
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
        String[] pathnames = f.list();
        for (String pathname : pathnames) {
			System.out.println(pathname);
			v.add(pathname);
        }
        return v;
	}

    public static void download() {
		String currentWorkingDirectory = System.getProperty("user.dir");
        download(NCIt_UPLOAD_URL + NCIT_ZIP_FILE, NCIT_ZIP_FILE);
        String zipFilePath = currentWorkingDirectory + "/" + NCIT_ZIP_FILE;
        unzip(zipFilePath, currentWorkingDirectory);
	}

    public static void main(String[] args) {
		String serviceUrl = null;
		String named_graph = null;
		String username = null;
		String password = null;
		String URL = NCIt_UPLOAD_URL;
		String target = NCIT_ZIP_FILE;

		if (args.length > 0) {
			serviceUrl = args[0];
			named_graph = args[1];
			username = args[2];
			password = args[3];
			if (args.length == 5) {
				target = args[4];
			}
		}

	    try {
			System.out.println("Downloading " + target + " from " + URL);
			download(URL + target, target);
			System.out.println("Completed.");
		} catch (Exception ex) {
			ex.printStackTrace();
		}

        String currentWorkingDirectory = System.getProperty("user.dir");
		String zipFilePath = currentWorkingDirectory + "/" + target;
		unzip(zipFilePath, currentWorkingDirectory);
		Vector files = listFilesInDirectory();
		Utils.dumpVector("listFilesInDirectory", files);

	}
}

