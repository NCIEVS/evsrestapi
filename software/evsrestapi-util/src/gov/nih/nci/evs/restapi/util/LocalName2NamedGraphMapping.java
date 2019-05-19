package gov.nih.nci.evs.restapi.util;

import gov.nih.nci.evs.restapi.util.*;
import gov.nih.nci.evs.restapi.bean.*;

import java.io.*;
import java.util.*;
import java.text.*;
import java.sql.*;
import java.net.*;

import org.apache.commons.lang.*;
import java.util.Map;
import java.util.Map.Entry;

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

 public class LocalName2NamedGraphMapping {
	 String serviceUrl = null;

	 public LocalName2NamedGraphMapping(String serviceUrl) {
		 this.serviceUrl = serviceUrl;
	 }

	 public HashMap getNameVersion2NamedGraphMap() {
	     return new OWLSPARQLUtils(serviceUrl).getNameVersion2NamedGraphMap();
	 }

	 public HashMap createSAB2NamedGraphMap() {
		 HashMap hmap = getNameVersion2NamedGraphMap();
		 HashMap localNames2NamedGraphMap = new HashMap();
		 Iterator it = hmap.keySet().iterator();
		 while (it.hasNext()) {
			 String key = (String) it.next();
			 Vector ng_vec = (Vector) hmap.get(key);
			 for (int k=0; k<1; k++) {
				 String ng = (String) ng_vec.elementAt(k);
				 Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(key, '|');
				 String localName = (String) u.elementAt(0);
				 localNames2NamedGraphMap.put(localName, ng);
				 if (localName.compareTo("NCI_Thesaurus") == 0) {
					 localNames2NamedGraphMap.put("NCI", ng);
				 } else if (localName.compareTo("MEDDRA") == 0) {
					 localNames2NamedGraphMap.put("MDR", ng);
				 } else if (localName.compareTo("SNOMEDCT") == 0) {
					 localNames2NamedGraphMap.put("SNOMEDCT_US", ng);
				 }
			 }
		 }
	     return localNames2NamedGraphMap;
	 }

	 public HashMap addSAB2NamedGraphMap(HashMap hmap, String key, String value) {
		 hmap.put(key, value);
		 return hmap;
	 }

	 public static void main(String[] args) {
		 String serviceUrl = args[0];
		 LocalName2NamedGraphMapping test = new LocalName2NamedGraphMapping(serviceUrl);
		 HashMap hmap = test.createSAB2NamedGraphMap();
		 gov.nih.nci.evs.restapi.util.StringUtils.dumpHashMap("NameVersion2NamedGraphMap", hmap);
	 }

 }