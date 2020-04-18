package gov.nih.nci.evs.restapi.util;

import gov.nih.nci.evs.restapi.bean.*;
import gov.nih.nci.evs.restapi.common.*;

import java.io.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.*;
import org.apache.commons.codec.binary.Base64;
import org.json.*;

/**
 * <!-- LICENSE_TEXT_START -->
 * Copyright Copyright 2020 MSC.. This software was developed in conjunction
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
 *      or MSC
 *   5. THIS SOFTWARE IS PROVIDED "AS IS," AND ANY EXPRESSED OR IMPLIED
 *      WARRANTIES, (INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *      OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE) ARE
 *      DISCLAIMED. IN NO EVENT SHALL THE NATIONAL CANCER INSTITUTE,
 *      MSC, OR THEIR AFFILIATES BE LIABLE FOR ANY DIRECT, INDIRECT,
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
 * Modification history:
 *     Initial implementation kim.ong@ngc.com
 *
 */


public class FormatHelper {

    public FormatHelper() {


    }

	public static boolean isInteger(Object object) {
		if(object instanceof Integer) {
			return true;
		} else {
			String string = object.toString();

			try {
				Integer.parseInt(string);
			} catch(Exception e) {
				return false;
			}
		}
		return true;
	}

    public static String escapeComma(String field) {
		char ch = ',';
		return escapeChar(field, ch);
	}


    public static String escapeChar(String field, char ch) {
		if (field == null) return "";
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<field.length(); i++) {
			char c = field.charAt(i);
			buf.append(c);
			if (c == ch) {
			    buf.append(c);
		    }
		}
		return buf.toString();
	}

	public static String appendDoubleQuote(String s) {
		/*
		if (isInteger(s)) return s;
		else {
			return "\"" + s + "\"";
		}
		*/
		return "\"" + s + "\"";
	}

    public static String toCSVLine(Vector fields) {
		if (fields == null) return null;
		if (fields.size() == 0) return "";
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<fields.size(); i++) {
			String field = (String) fields.elementAt(i);
			field = escapeComma(field);
			field = appendDoubleQuote(field);
			buf.append(field);
			if (i < fields.size()-1) {
				buf.append(",");
			}
		}
		return buf.toString();
	}

    public static String toDelimitedLine(Vector fields, char delim) {
		if (fields == null) return null;
		if (fields.size() == 0) return "";
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<fields.size(); i++) {
			String field = (String) fields.elementAt(i);
			buf.append(field);
			if (i < fields.size()-1) {
				buf.append(delim);
			}
		}
		return buf.toString();
	}

	public static void main(String[] args) {
		Vector fields = new Vector();
		fields.add("First field \",");
		fields.add("12345");
		fields.add("123a45");
		String retstr = toCSVLine(fields);
		System.out.println(retstr);

		String delimitedLine = toDelimitedLine(fields, '|');
		System.out.println(delimitedLine);


		//2-AMINO-5,8-DIMETHOXY-(1,2,4)TRIAZOLO(1,5-C)PYRIMIDINE	sys	0129526470	5,8-DIMETHOXY(1,2,4)TRIAZOLO(1,5-C)PYRIMIDIN-2-AMINE
		fields = new Vector();
		fields.add("2-AMINO-5,8-DIMETHOXY-(1,2,4)TRIAZOLO(1,5-C)PYRIMIDINE");
		fields.add("sys	0129526470");
		fields.add("5,8-DIMETHOXY(1,2,4)TRIAZOLO(1,5-C)PYRIMIDIN-2-AMINE");
		retstr = toCSVLine(fields);
		System.out.println(retstr);



	}
}
