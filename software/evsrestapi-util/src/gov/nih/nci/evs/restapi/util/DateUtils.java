package gov.nih.nci.evs.restapi.util;

import java.io.*;
import java.net.URI;
import java.text.*;
import java.util.*;
import java.time.DayOfWeek;
import java.time.YearMonth;

import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;

import java.time.format.TextStyle;
import java.text.DateFormatSymbols;

/**
 * <!-- LICENSE_TEXT_START -->
 * Copyright 2008-2017 NGIS. This software was developed in conjunction
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
 *      "This product includes software developed by NGIS and the National
 *      Cancer Institute."   If no such end-user documentation is to be
 *      included, this acknowledgment shall appear in the software itself,
 *      wherever such third-party acknowledgments normally appear.
 *   3. The names "The National Cancer Institute", "NCI" and "NGIS" must
 *      not be used to endorse or promote products derived from this software.
 *   4. This license does not authorize the incorporation of this software
 *      into any third party proprietary programs. This license does not
 *      authorize the recipient to use any trademarks owned by either NCI
 *      or NGIS
 *   5. THIS SOFTWARE IS PROVIDED "AS IS," AND ANY EXPRESSED OR IMPLIED
 *      WARRANTIES, (INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *      OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE) ARE
 *      DISCLAIMED. IN NO EVENT SHALL THE NATIONAL CANCER INSTITUTE,
 *      NGIS, OR THEIR AFFILIATES BE LIABLE FOR ANY DIRECT, INDIRECT,
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


public class DateUtils {
	static String ALPHABETICS = "abcdefg";
	static String NCI_THESAURUS_URI = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus";

/*
	public static String getNCItMonthlyVersion() {
		String t = getToday(); //2011-11
		t = t.substring(2, t.length());
		t = t.replace("-", ".");
		int n = getNumberOfWeeks();
		char c = ALPHABETICS.charAt(n-1);
		return t + c;
	}
*/

	public static String getToday() {
		return getToday("yyyy-MM");
	}

	public static String getToday(String format) {
		java.util.Date date = Calendar.getInstance().getTime();
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		return sdf.format(date);
	}

	public static int getNumberOfWeeks() {
		String m = getToday();
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM");
		try {
			Date date = format.parse(m);
			Calendar c = Calendar.getInstance();
			c.setTime(date);

			int start = c.get(Calendar.WEEK_OF_MONTH);

			c.add(Calendar.MONTH, 1);
			c.add(Calendar.DATE, -1);
			int end = c.get(Calendar.WEEK_OF_MONTH);
			return end - start + 1;
		} catch (Exception ex) {
			return -1;
		}
	}

	public static int getDayNumberNew(LocalDate date) {
		DayOfWeek day = date.getDayOfWeek();
		return day.getValue();
	}

    public static LocalDate getLocalDate() {
		//Current Date
		LocalDate today = LocalDate.now();
		System.out.println("Current Date="+today);
		return today;
	}


    public static LocalDate getLocalDate(int year, int month, int day) { // month: Month.JANUARY
		//Creating LocalDate by providing input arguments
		LocalDate local_date = LocalDate.of(year, month, day);
		return local_date;
    }

    public static int getDayNumber(LocalDate date) {
		DayOfWeek day = date.getDayOfWeek();
		printDayOfWeek(day);
		return day.getValue();
	}

    public static void printDayOfWeek(DayOfWeek dow) {
		Locale locale = Locale.getDefault();
		System.out.println(dow.getDisplayName(TextStyle.FULL, locale));
		System.out.println(dow.getDisplayName(TextStyle.NARROW, locale));
		System.out.println(dow.getDisplayName(TextStyle.SHORT, locale));
	}



    public static int getDaysInMonth(int year, int month) {
		YearMonth yearMonthObject = YearMonth.of(year, month);
		int daysInMonth = yearMonthObject.lengthOfMonth();
		return daysInMonth;
	}

	public static int getNumberOfMondaysInAMonth(int year, int month) {
		String weekDay = "Monday";
		return getNumberOfWeekDaysInAMonth(year, month, weekDay);
	}

	public static int getNumberOfWeekDaysInAMonth(int year, int month, String weekDay) {
		int m = 0;
		Locale locale = Locale.getDefault();
		int num_days = getDaysInMonth(year, month);
		for (int day=1; day<=num_days; day++) {
			LocalDate date = getLocalDate(year, month, day);
			DayOfWeek dow = date.getDayOfWeek();
			String fullName = dow.getDisplayName(TextStyle.FULL, locale);
			if (fullName.compareTo(weekDay) == 0) {
				m++;
			}
		}
		return m;
	}

	public static String getNCItMonthlyVersion(int year, int month) {
		int n = getNumberOfMondaysInAMonth(year, month);
		char c = ALPHABETICS.charAt(n-1);
		StringBuffer buf = new StringBuffer();
		String year_str = "" + year;
		String t = year_str.substring(2, 4);
		buf.append(t).append(".");
		if (month < 10) {
			buf.append("0").append("" + month);
			buf.append(c);
		} else {
			buf.append("" + month);
			buf.append(c);
		}
		return buf.toString();
	}

	public static String getNCItMonthlyVersion() {
		int currentYear = getCurrentYear();
		int currentMonth = getCurrentMonth();
		return getNCItMonthlyVersion(currentYear, currentMonth);
	}

    public static String getNCIThesaurusGraphName(int year, int month) {
		 String version = getNCItMonthlyVersion(year, month);
		 return NCI_THESAURUS_URI + version + ".owl";
	}

    public static String getNCIThesaurusGraphName() {
		int currentYear = getCurrentYear();
		int currentMonth = getCurrentMonth();
		String version = getNCItMonthlyVersion(currentYear, currentMonth);
		return NCI_THESAURUS_URI + version + ".owl";
	}

	public static int getCurrentMonth() {
		 return Calendar.getInstance().get(Calendar.MONTH) + 1;
	}

	public static int getCurrentYear() {
		 return Calendar.getInstance().get(Calendar.YEAR);
	}

	public static Vector getNCItReleaseSchedule(int year) {
		Vector w = new Vector();
		w.add("Year|Month|Version|Graph Name|Release Data");
		for (int i=1; i<=12; i++) {
			String version = getNCItMonthlyVersion(year, i);
			String graphName = getNCIThesaurusGraphName(year, i);
			w.add("" + year + "|" + getMonthString(i) + "|" + version + "|" + graphName + "|" + getMonthlyNCItReleaseDate(year, i));
		}
		return w;
	}

	public static int getLastWeekdayOfAMonth(int year, int month, String weekDay) {
		int m = 0;
		Locale locale = Locale.getDefault();
		int num_days = getDaysInMonth(year, month);
		for (int day=1; day<=num_days; day++) {
			int j = num_days-day+1;
			LocalDate date = getLocalDate(year, month, j);
			DayOfWeek dow = date.getDayOfWeek();
			String fullName = dow.getDisplayName(TextStyle.FULL, locale);
			if (fullName.compareTo(weekDay) == 0) {
				return j;
			}
		}
		return 0;
	}

	public static String getMonthlyNCItReleaseDate(int year, int month) {
		int n = getLastWeekdayOfAMonth(year, month, "Monday");
		StringBuffer buf = new StringBuffer();
		if (month < 10) {
			buf.append("0");
		}
        buf.append(month).append("/");
		if (n < 10) {
			buf.append("0");
		}
        buf.append(n).append("/");
        buf.append(year);
        return buf.toString();
	}

    public static String getMonthString(int month) {
		return new DateFormatSymbols().getMonths()[month-1];
	}

	public static HashMap getReleaseScheduleHashMap(int year) {
		Vector w = getNCItReleaseSchedule(year);
		HashMap hmap = new HashMap();
		for (int i=1; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			hmap.put((String) u.elementAt(0) + "|" + (String) u.elementAt(1), line);
		}
		return hmap;
	}

	public static void main(String[] args) throws Exception {
		int currentYear = getCurrentYear();
		int currentMonth = getCurrentMonth();
        Vector w = getNCItReleaseSchedule(currentYear);
        Utils.dumpVector("NCIt Release Schedule (Year " + currentYear + ")", w);

	}

}