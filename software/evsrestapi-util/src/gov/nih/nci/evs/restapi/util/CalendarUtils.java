package gov.nih.nci.evs.restapi.util;

import gov.nih.nci.evs.restapi.bean.*;
import gov.nih.nci.evs.restapi.common.*;

import java.io.*;
import java.text.*;
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
 * Copyright 2020 MSC. This software was developed in conjunction
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


public class CalendarUtils {

    public CalendarUtils() {

	}

	public static void test() {
		Calendar calendar = Calendar.getInstance();
		System.out.println(calendar.getActualMaximum(Calendar.WEEK_OF_MONTH));
	}

	public static int getNumberOfWeeksInMonth(String year_month) throws Exception {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM");
		Date date = format.parse(year_month);
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		int start = c.get(Calendar.WEEK_OF_MONTH);
		c.add(Calendar.MONTH, 1);
		c.add(Calendar.DATE, -1);
		int end = c.get(Calendar.WEEK_OF_MONTH);
		int num_weeks = end - start + 1;
		System.out.println(" # of weeks in " + format.format(c.getTime()) + ": " + (end - start + 1));
		return num_weeks;
	}

    public static int getReleaseWeek(char c) {
		int int_c = (int) c;
		return int_c - 96;
	}

	public static char getReleaseChar(int int_c) {
		char c = (char) (int_c + 96);
		return c;
	}

	public static String getReleaseVersion(int year, int month, int week) {
		String year_str = Integer.valueOf(year).toString().substring(2, 4);
		//System.out.println(year_str);
		String month_str = Integer.valueOf(month).toString();
		if (month_str.length() == 1) {
			month_str = "0" + month_str;
		}
		return year_str + "." + month_str + getReleaseChar(week);
	}

	public static String getLatestMonthlyHistoryFile() {
		String today = StringUtils.getToday();
		Vector u = StringUtils.parseData(today, '-');
		String month_str = (String) u.elementAt(0);
		String day_str = (String) u.elementAt(1);
		String year_str = (String) u.elementAt(2);
		if (month_str.startsWith("0")) {
			month_str = month_str.substring(1, month_str.length());
		}
		System.out.println(year_str + " " + month_str + " " + day_str);
		int year = Integer.parseInt(year_str);
		int month = Integer.parseInt(month_str) - 1;
		try {
			String year_month = year_str + "-" + month;
			System.out.println("year_month: " + year_month);
			int week = CalendarUtils.getNumberOfWeeksInMonth(year_month);
			String s = getReleaseVersion(year, month, week-1);
			return s;
		} catch (Exception ex) {
            ex.printStackTrace();
		}
		return null;
	}

	public static void main(String[] args) throws Exception {
		try {
			String year_month = "2020-08";
			int num_weeks = getNumberOfWeeksInMonth(year_month);

			System.out.println(" # of weeks in " + year_month + ": " + num_weeks);

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}