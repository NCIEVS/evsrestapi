package gov.nih.nci.evs.restapi.util;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.net.*;
import java.util.*;

import javax.net.ssl.*;
import java.security.cert.*;
import java.security.*;

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


//https://evs.nci.nih.gov/ftp1/upload/ThesaurusInferred_forTS.zip


public class NCItDownload {

    public static String NCIt_URI = "https://evs.nci.nih.gov/ftp1/upload/";
    public static String NCIT_ZIP_FILE = "ThesaurusInferred_forTS.zip";

	public static void download(String uri, String outputfile) {
		download(uri, new File(outputfile));
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
			//if (pathname.endsWith(".owl") && pathname.indexOf("READ ME") == -1) {
                System.out.println(pathname);
                v.add(pathname);
		    //}
        }
        return v;
	}

    public static void download() {
		String currentWorkingDirectory = System.getProperty("user.dir");
		try {
			download(NCIt_URI + NCIT_ZIP_FILE, NCIT_ZIP_FILE);
			String zipFilePath = currentWorkingDirectory + "/" + NCIT_ZIP_FILE;
			unzip(zipFilePath, currentWorkingDirectory);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	 public static void doTrustToCertificates() throws Exception {
        //Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
        TrustManager[] trustAllCerts = new TrustManager[]{
			new X509TrustManager() {
				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
					return;
				}

				public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {
					return;
				}
			}
        };

        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        HostnameVerifier hv = new HostnameVerifier() {
            public boolean verify(String urlHostName, SSLSession session) {
                if (!urlHostName.equalsIgnoreCase(session.getPeerHost())) {
                    System.out.println("Warning: URL host '" + urlHostName + "' is different to SSLSession host '" + session.getPeerHost() + "'.");
                }
                return true;
            }
        };
        HttpsURLConnection.setDefaultHostnameVerifier(hv);
    }

	public static void download(String download_url, File file) {
		try {
			byte[] buffer = new byte[1024];
			double TotalDownload = 0.00;
			int readbyte = 0;
			double percentOfDownload = 0.00;

			try {
				doTrustToCertificates();
			} catch (Exception ex) {
				ex.printStackTrace();
			}

			URL url = new URL(download_url);
			HttpURLConnection http = (HttpURLConnection)url.openConnection();
			double filesize = (double)http.getContentLengthLong();

			BufferedInputStream input = new BufferedInputStream(http.getInputStream());
			FileOutputStream ouputfile = new FileOutputStream(file);
			BufferedOutputStream bufferOut = new BufferedOutputStream(ouputfile, 1024);

			while((readbyte = input.read(buffer, 0, 1024)) >= 0) {
				bufferOut.write(buffer,0,readbyte);
				TotalDownload += readbyte;
				percentOfDownload = (TotalDownload*100)/filesize;
				String percent = String.format("%.2f", percentOfDownload);
				//System.out.println("Downloaded "+ percent + "%");
			}

			System.out.println("Download is complete.");
			bufferOut.close();
			input.close();
		}
		catch(IOException e){
			e.printStackTrace();
		}
	}

    public static void main(String[] args) {
		long ms = System.currentTimeMillis();
		String currentWorkingDirectory = System.getProperty("user.dir");

        if (args.length == 1) {
			NCIt_URI = args[0];
		}
		String url = NCIt_URI + NCIT_ZIP_FILE;
		System.out.println("NCIt_URI + NCIT_ZIP_FILE: " + url);
		System.out.println("NCIT_ZIP_FILE: " + NCIT_ZIP_FILE);

		try {
        	download(NCIt_URI + NCIT_ZIP_FILE, NCIT_ZIP_FILE);
			String zipFilePath = currentWorkingDirectory + "/" + NCIT_ZIP_FILE;
			unzip(zipFilePath, currentWorkingDirectory);

			Vector files = listFilesInDirectory();
			Utils.dumpVector("listFilesInDirectory", files);

		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}
}

