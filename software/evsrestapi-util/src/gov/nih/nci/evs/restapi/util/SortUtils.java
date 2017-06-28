package gov.nih.nci.evs.restapi.util;


import gov.nih.nci.evs.restapi.bean.*;
import java.util.*;


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


public class SortUtils {

    public static final int SORT_BY_NAME = 1;
    public static final int SORT_BY_CODE = 2;

    /**
     * Performs quick sort of a List by name.
     *
     * @param list an instance of List
     */
    public void quickSort(List list) {
        quickSort(list, SORT_BY_NAME);
    }

    /**
     * Performs quick sort of a List by a specified sort option.
     *
     * @param list an instance of List
     * @param sort_option, an integer; 1, if sort by name; 2: if sort by code
     */
    public void quickSort(List list, int sort_option) {
        if (list == null)
            return;
        if (list.size() <= 1)
            return;
        try {
            Collections.sort(list, new SortComparator(sort_option));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Performs quick sort of a Vector by a specified sort option.
     *
     * @param v an instance of Vector
     * @param sort_option, an integer; 1, if sort by name; 2: if sort by code
     */

    public Vector quickSort(Vector v, int sort_option) {
        if (v == null)
            return v;
        if (v.size() <= 1)
            return v;
        try {
            Collections.sort((List) v, new SortComparator(sort_option));
            return v;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Performs quick sort of a Vector by name.
     *
     * @param v an instance of Vector
     */

    public Vector quickSort(Vector v) {
        return quickSort(v, SORT_BY_NAME);
    }

    @SuppressWarnings("unchecked")
    public Enumeration<?> sort(Enumeration<?> enumeration) {
        if (enumeration == null)
            return enumeration;

        List keyList = Collections.list(enumeration);
        Collections.sort(keyList);
        enumeration = Collections.enumeration(keyList);
        return enumeration;
    }

	public Vector caseInsensitiveSort(Vector v) {
		if (v == null) return null;
		HashMap hmap = new HashMap();
		Vector keys = new Vector();
		Vector values = new Vector();
		for (int i=0; i<v.size(); i++) {
			String key = (String) v.elementAt(i);
			String key_lower_case = key.toLowerCase();
			keys.add(key_lower_case);
			hmap.put(key_lower_case, key);
		}
		keys = quickSort(keys);
		for (int i=0; i<keys.size(); i++) {
			String key = (String) keys.elementAt(i);
			String value = (String) hmap.get(key);
			values.add(value);
		}
		return values;
	}
}
