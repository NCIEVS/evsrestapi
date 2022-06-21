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
 *     Initial implementation kim.ong@nih.gov
 *
 */


public class LogicalExpressionGenerator {
	HashMap rangeHashMap = null;
	LogicalExpression le = null;

    public LogicalExpressionGenerator(String serviceUrl, String named_graph, String username, String password) {
		le = new LogicalExpression(serviceUrl, named_graph, username, password);
        rangeHashMap = le.getRangeHashMap(named_graph);
    }

    public String getLogicalExpression(String named_graph, String code) {
		HashMap range2IdsMap = new HashMap();
        Vector paths = new Vector();
        paths.add("E|I|O|C");
        paths.add("E|I|O|R");
        paths.add("E|I|O|U|O|R");
        paths.add("E|I|O|U|O|I|O|R");
        HashMap hmap = le.getLogicalExpressionData(named_graph, code, paths);

        StringBuffer buf = new StringBuffer();
        Vector parents = (Vector) hmap.get("E|I|O|C");
        Vector parent_vec = new Vector();
        if (parents != null && parents.size() > 0) {
			buf.append("Parent").append("\n");
			for (int i=0; i<parents.size(); i++) {
				String line = (String) parents.elementAt(i);
				Vector u = StringUtils.parseData(line, '|');
				parent_vec.add("\t" + (String) u.elementAt(u.size()-1) + " (" + (String) u.elementAt(u.size()-2) + ")");
			}
		}
		parent_vec = new SortUtils().quickSort(parent_vec);
		for (int i=0; i<parent_vec.size(); i++) {
			String line = (String) parent_vec.elementAt(i);
			buf.append(line).append("\n");
		}

        HashMap role_hmap = new HashMap();
        String path = "E|I|O|R";
        Vector restrictions = (Vector) hmap.get("E|I|O|R");

        Vector simple_role_groups = (Vector) hmap.get("E|I|O|U|O|R");
        HashMap simple_role_group_hmap = le.sortRestrictions(path, simple_role_groups);
        Vector multiple_role_groups = (Vector) hmap.get("E|I|O|U|O|I|O|R");

        HashMap role_group_hmap = new HashMap();

        if (multiple_role_groups != null) {
			role_group_hmap = le.sortRestrictions(path, multiple_role_groups);
			if (role_group_hmap.keySet().size() > 0) {
				Iterator it = role_group_hmap.keySet().iterator();
				while (it.hasNext()) {
					String key = (String) it.next();
					Vector w = (Vector) role_group_hmap.get(key);
					for (int j=0; j<w.size(); j++) {
						String line = (String) restrictions.elementAt(j);
						Vector u = StringUtils.parseData(line, '|');
						String label = (String) u.elementAt(u.size()-1);
						String role_code = (String) u.elementAt(u.size()-2);
						String role = (String) u.elementAt(u.size()-3);
						String roleName = (String) u.elementAt(u.size()-4);
						String range = (String) rangeHashMap.get(role);
						Vector id_vec = new Vector();
						if (range2IdsMap.containsKey(range)) {
							id_vec = (Vector) range2IdsMap.get(range);
						}
						id_vec.add(key);
						range2IdsMap.put(range, id_vec);
					}
				}
			}
		}

//////////////////////////////////////////////////////////////////////////////////////////////

		Vector key_vec = le.getRangeLabels(named_graph);
		for (int lcv=0; lcv<key_vec.size(); lcv++) {
			String key = (String) key_vec.elementAt(lcv);
			Vector id_vec = (Vector) range2IdsMap.get(key);

			int role_group_knt = 0;
			Vector role_group_vec = new Vector();

			Vector restriction_vec = new Vector();
			Vector simple_role_group_vec = new Vector();
			int simple_role_group_knt = 0;



			for (int i=0; i<restrictions.size(); i++) {
				String line = (String) restrictions.elementAt(i);
				Vector u1 = StringUtils.parseData(line, '|');
				String target_1_label = (String) u1.elementAt(u1.size()-1);
				String target_1_code = (String) u1.elementAt(u1.size()-2);
				String role_1_code = (String) u1.elementAt(u1.size()-3);
				String role_1_label = (String) u1.elementAt(u1.size()-4);
				String range = (String) rangeHashMap.get(role_1_code);
				if (range.compareTo(key) == 0) {
					restriction_vec.add("\t" + role_1_label +"\t" + (String) u1.elementAt(u1.size()-1) + " (" + (String) u1.elementAt(u1.size()-2) + ")");
				}
			}

			Iterator it_simple_role_group_hmap = simple_role_group_hmap.keySet().iterator();
			while (it_simple_role_group_hmap.hasNext()) {
				String key2 = (String) it_simple_role_group_hmap.next();
				Vector w = (Vector) simple_role_group_hmap.get(key2);

				for (int k=0; k<w.size(); k++) {
					String line = (String) w.elementAt(k);
					Vector u1 = StringUtils.parseData(line, '|');
					String target_1_label = (String) u1.elementAt(u1.size()-1);
					String target_1_code = (String) u1.elementAt(u1.size()-2);
					String role_1_code = (String) u1.elementAt(u1.size()-3);
					String role_1_label = (String) u1.elementAt(u1.size()-4);
					String range = (String) rangeHashMap.get(role_1_code);
					if (range.compareTo(key) == 0) {
						simple_role_group_vec.add("\t" + role_1_label +"\t" + (String) u1.elementAt(u1.size()-1) + " (" + (String) u1.elementAt(u1.size()-2) + ")");
					}
					simple_role_group_knt++;
				}
			}

            if (id_vec != null && id_vec.size() > 0) {
				for (int i=0; i<id_vec.size(); i++) {
					String id = (String) id_vec.elementAt(i);

					if (role_group_hmap.containsKey(id)) {
						Vector values = (Vector) role_group_hmap.get(id);
						String line = (String) values.elementAt(0);
						Vector u1 = StringUtils.parseData(line, '|');
						String target_1_label = (String) u1.elementAt(u1.size()-1);
						String target_1_code = (String) u1.elementAt(u1.size()-2);
						String role_1_code = (String) u1.elementAt(u1.size()-3);
						String role_1_label = (String) u1.elementAt(u1.size()-4);
						String range_1 = (String) rangeHashMap.get(role_1_code);

						line = (String) values.elementAt(1);
						Vector u2 = StringUtils.parseData(line, '|');
						String target_2_label = (String) u2.elementAt(u2.size()-1);
						String target_2_code = (String) u2.elementAt(u2.size()-2);
						String role_2_code = (String) u2.elementAt(u2.size()-3);
						String role_2_label = (String) u2.elementAt(u2.size()-4);
						String range_2 = (String) rangeHashMap.get(role_2_code);

						if (range_1.compareTo(key) == 0 && range_2.compareTo(key) == 0) {
							role_group_vec.add("\t" + role_1_label +"\t" + (String) u1.elementAt(u1.size()-1) + " (" + (String) u1.elementAt(u1.size()-2) + ")");
							role_group_vec.add("\t" + role_2_label +"\t" + (String) u2.elementAt(u2.size()-1) + " (" + (String) u2.elementAt(u2.size()-2) + ")");
							role_group_knt++;
						}
					}
				}
			}

			if (restriction_vec.size() > 0 || simple_role_group_knt > 0 || role_group_knt > 0) {
                StringBuffer buf2 = new StringBuffer();
				restriction_vec = new SortUtils().quickSort(restriction_vec);
				for (int k=0; k<restriction_vec.size(); k++) {
					buf2.append("\n");
					String line = (String) restriction_vec.elementAt(k);
					buf2.append(line);
				}

				if (simple_role_group_knt > 0) {
					it_simple_role_group_hmap = simple_role_group_hmap.keySet().iterator();
					Vector w0 = new Vector();
					while (it_simple_role_group_hmap.hasNext()) {
						String key2 = (String) it_simple_role_group_hmap.next();
						Vector w = (Vector) simple_role_group_hmap.get(key2);
						for (int j=0; j<w.size(); j++) {
							String line = (String) w.elementAt(j);
							Vector u = StringUtils.parseData(line, '|');
							String label = (String) u.elementAt(u.size()-1);
							String role_code = (String) u.elementAt(u.size()-2);
							String role = (String) u.elementAt(u.size()-3);
							String roleName = (String) u.elementAt(u.size()-4);
							String range = (String) rangeHashMap.get(role);
							if (range.compareTo(key)== 0) {
								w0.add("\t" + roleName +"\t" + label + " (" + role_code + ")");
							}
						}
					}
					if (w0.size() > 0) {
						buf2.append("\n\t").append("Role Group(s)").append("\n");
						for (int k=0; k<w0.size(); k++) {
							buf2.append((String) w0.elementAt(k)).append("\n");
							if (k<w0.size() - 1) {
								buf2.append("\t").append("or").append("\n");
							}
						}
					}
				}

				if (role_group_knt > 0) {
					buf2.append("\n\n");
					if (role_group_knt > 0) {
						buf2.append("\n\t").append("Role Group(s)").append("\n");
						for (int k=0; k<role_group_vec.size()/2; k++) {
							buf2.append((String) role_group_vec.elementAt(k)).append("\n");
							buf2.append((String) role_group_vec.elementAt(k+1)).append("\n");
							if (k<role_group_vec.size()/2 - 1) {
								buf2.append("\n\t").append("or").append("\n");
							}
						}
					}
				}
				if (buf2.length() > 0) {
					buf.append("\n").append(key);
				}
				buf.append(buf2);
				if (buf2.length() > 0) {
					buf.append("\n");
				}
			}
		}
		return buf.toString();
	}

	public static void main(String[] args) {
		long ms = System.currentTimeMillis();
		String serviceUrl = args[0];
		String named_graph = args[1];
		String username = args[2];
		String password = args[3];
		String code = args[4];
        LogicalExpressionGenerator test = new LogicalExpressionGenerator(serviceUrl, named_graph, username, password);
        String expression = test.getLogicalExpression(named_graph, code);
        System.out.println("\n\n" + expression);
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}
}