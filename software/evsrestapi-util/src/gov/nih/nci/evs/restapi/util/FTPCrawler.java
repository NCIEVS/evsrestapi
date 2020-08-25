package gov.nih.nci.evs.restapi.util;

import java.io.*;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.*;
import java.util.*;
//import org.apache.commons.io.FilenameUtils;


/**
 * <!-- LICENSE_TEXT_START -->
 * Copyright 2008,2009 NGIT. This software was developed in conjunction
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
 *      "This product includes software developed by NGIT and the National
 *      Cancer Institute."   If no such end-user documentation is to be
 *      included, this acknowledgment shall appear in the software itself,
 *      wherever such third-party acknowledgments normally appear.
 *   3. The names "The National Cancer Institute", "NCI" and "NGIT" must
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


public class FTPCrawler {
   public static String NCIT_FTP_SITE = "https://evs.nci.nih.gov/ftp1/";
   public static String ABOUT_PAGE = "About.html";
   public static String MAPPINGS = "Mappings";
   public static String NCI_THESAURUS = "NCI_Thesaurus";
   public static String NCIT_MAPPINGS_SITE = NCIT_FTP_SITE + NCI_THESAURUS + "/" + MAPPINGS + "/";
   public static String NCIT_ABOUT_SITE = NCIT_MAPPINGS_SITE + ABOUT_PAGE;
   public static String ICDO3_MAPPIGNS_SITE = NCIT_MAPPINGS_SITE + "ICD-O-3_Mappings/";
   public static String ICDO3_ABOUT_SITE   = ICDO3_MAPPIGNS_SITE + "About.html";
   public static String[] MAPPING_SITES = new String[] {NCIT_MAPPINGS_SITE, NCIT_ABOUT_SITE, ICDO3_MAPPIGNS_SITE, ICDO3_ABOUT_SITE};

   public static HashMap lastUpdatedHashMap = null;

   public static Vector otherMappingData = null;

   public static Vector getOtherMappingData() {
	   return otherMappingData;
   }

   public static void dumpMappingSites() {
       for (int i=0; i<MAPPING_SITES.length; i++) {
		   String t = MAPPING_SITES[i];
		   System.out.println(t);
	   }
   }

   public static Vector get(String uri) {
      URL u = null;
      InputStream is = null;
      DataInputStream dis = null;
      String s;
      Vector v = new Vector();
      try {
         u = new URL(uri);
         if (u != null) {
			 is = u.openStream();
			 dis = new DataInputStream(new BufferedInputStream(is));
			 while ((s = dis.readLine()) != null) {
				v.add(s);
			 }
		 }

      } catch (MalformedURLException mue) {
		 System.out.println("(*) MalformedURLException: " + uri);
		 return v;
         //mue.printStackTrace();

      } catch (IOException ioe) {
		 System.out.println(uri);
         ioe.printStackTrace();

      } finally {
         try {
            if (is != null) is.close();
         } catch (IOException ioe) {
            ioe.printStackTrace();
         }
      }
      return v;
   }

//========================================================================================================================================

	public static boolean containsAboutHTML(Vector v) {
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			if (t.indexOf("About.html") != -1) {
				return true;
			}
		}
		return false;
	}

    public static Vector extractHrefLinks(Vector v) {
		String target = "href=";
		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			int n = t.indexOf(target);
			if (n != -1) {
				String s = t.substring(n+6, t.length());
				n = s.indexOf("\"");
				if (n != -1) {
					s = s.substring(0, n);
					if (s.indexOf("=") == -1) {
					    w.add(s);
					}
				}
			}
		}
		return w;
	}

	public static Vector excludeArchive(Vector w) {
		Vector v = new Vector();
		for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			if (line.indexOf("archive") == -1 && line.indexOf("Archive") == -1) {
				v.add(line);
			}
		}
		return v;
	}

	public static Vector searchForHrefLinks(Vector w) {
		Vector v = new Vector();
		for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			Vector u = new Vector();
			if (line.indexOf("\r\n") != -1) {
				u = StringUtils.parseData(line, "\r\n");
			} else {
				u.add(line);
			}
			for (int j=0; j<u.size(); j++) {
				String t = (String) u.elementAt(j);
				if (t.indexOf("href=") != -1) {
					v.add(line);
				}
			}
		}
		return excludeArchive(extractHrefLinks(v));
	}

	public static Vector filter_link_vec(Vector w) {
		Vector v = new Vector();
		for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			if (line.indexOf("About.html") == -1 && !line.endsWith("/")) {
				v.add(line);
			}
		}
		return v;
	}

	public static Vector run(String root_page_uri) {
        Stack stack = new Stack();
        stack.push(root_page_uri);
        Vector link_vec = new Vector();
        while (!stack.empty()) {
			String uri = (String) stack.pop();
			link_vec.add(uri);
			Vector v = get(uri);
			//boolean retval = containsAboutHTML(v);
			//System.out.println(uri + " contains about.html? " + retval);
			Vector w = searchForHrefLinks(v);
			for (int i=0; i<w.size(); i++) {
				String t = (String) w.elementAt(i);
				if (uri.endsWith("/") && !t.startsWith("/")) {
					String next_uri = uri + t;
					stack.push(next_uri);
				}
			}
		}
		return filter_link_vec(link_vec);
	}


    public static Vector listFileInfoAtFTPSite(String ftp_site) {
		Vector w = new Vector();
        try {
            URL url = new URL(ftp_site);
            URLConnection conn = url.openConnection();
            InputStream inputStream = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line = null;
            while ((line = reader.readLine()) != null) {
                w.add(line);
            }
            inputStream.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return w;
    }
}

