package gov.nih.nci.evs.restapi.util;
import gov.nih.nci.evs.restapi.common.*;
import gov.nih.nci.evs.restapi.bean.*;
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

public class RelationshipUtils {
    String named_graph = null;

    OWLSPARQLUtils owlSPARQLUtils = null;
    Vector rel_vec = null;
    HashMap roleMap = null;
    HashMap inverseRoleMap = null;
    Vector disease_is_stage_sources = null;

	public RelationshipUtils(OWLSPARQLUtils owlSPARQLUtils, String named_graph) {
		this.owlSPARQLUtils = owlSPARQLUtils;
		this.named_graph = named_graph;
		initialize();
	}

	public RelationshipUtils(Vector rel_vec) {
		this.rel_vec = rel_vec;
		initialize();
	}

	public RelationshipUtils(String named_graph, Vector rel_vec) {
		this.rel_vec = rel_vec;
		this.named_graph = named_graph;
		initialize();
	}

	public void set_named_graph(String named_graph) {
		this.named_graph = named_graph;
	}



	public void initialize() {
		long ms = System.currentTimeMillis();
		if (rel_vec == null) {
			if (named_graph == null) {
				System.out.println("Please specified a named_space.");
				return;
			}
		    Vector v = owlSPARQLUtils.getRoleRelationships(named_graph);
            rel_vec = new ParserUtils().toDelimited(v, 5, '|');
	    }
		roleMap = new HashMap();
		inverseRoleMap = new HashMap();
		disease_is_stage_sources = new Vector();

        for (int i=0; i<rel_vec.size(); i++) {
			String line = (String) rel_vec.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String name = (String) u.elementAt(2);
			String sourceCode = (String) u.elementAt(1);
			String sourceLabel = (String) u.elementAt(0);
			String targetCode = (String) u.elementAt(4);
			String targetLabel = (String) u.elementAt(3);
			Role role = new Role(name, targetCode, targetLabel);
			InverseRole inverse_role = new InverseRole(name, sourceCode, sourceLabel);
			Vector w = new Vector();
			if (roleMap.containsKey(sourceCode)) {
				w = (Vector) roleMap.get(sourceCode);
			}
			w.add(role);
			roleMap.put(sourceCode, w);

			w = new Vector();
			if (inverseRoleMap.containsKey(targetCode)) {
				w = (Vector) inverseRoleMap.get(targetCode);
			}
			w.add(inverse_role);
			inverseRoleMap.put(sourceCode, w);

			if (name.compareTo(Constants.DISEASE_IS_STAGE) == 0) {
				if (!disease_is_stage_sources.contains(sourceCode)) {
					disease_is_stage_sources.add(sourceCode);
				}
			}
		}
		System.out.println("Total RelationshipUtils initialization run time (ms): " + (System.currentTimeMillis() - ms));
	}

	public Vector getRoles(String code) {
		return (Vector) roleMap.get(code);
	}

	public Vector getInverseRoles(String code) {
		return (Vector) inverseRoleMap.get(code);
	}

	public boolean isStage(String code) {
		if (disease_is_stage_sources.contains(code)) return true;
		return false;
	}
/*
x_label|literal|Stage I Differentiated Thyroid Gland Carcinoma Under 45 Years
x_code|literal|C101539
p_label|literal|Disease_Is_Stage
y_label|literal|Stage I
y_code|literal|C27966

Stage I Differentiated Thyroid Gland Carcinoma Under 45 Years|C101539|Disease_Is_Stage|Stage I|C27966
*/
	public static void main(String[] args) {
		String datafile = args[0];
		Vector v = Utils.readFile(datafile);
        Vector rel_vec = new ParserUtils().toDelimited(v, 5, '|');
        rel_vec = new SortUtils().quickSort(rel_vec);
        String named_graph = "http://NCIt";
        RelationshipUtils util = new RelationshipUtils(named_graph, rel_vec);
        String code = "C101539";
        boolean isStage = util.isStage(code);
        System.out.println("Is stage(\"" + code + "\"): " + isStage);

        System.out.println("Roles: ");
        Vector w = util.getRoles(code);
        for (int i=0; i<w.size(); i++) {
			Role r = (Role) w.elementAt(i);
			//System.out.println("(" + r.getRelationship() + ") --> [" + r.getRelatedConceptLabel() + " (" + r.getRelatedConceptCode() + ")]");
			System.out.println(r.toString());
		}

		System.out.println("\n\nInverse Roles: ");
        w = util.getInverseRoles(code);
        for (int i=0; i<w.size(); i++) {
			InverseRole r = (InverseRole) w.elementAt(i);
			//System.out.println("[" + r.getRelatedConceptLabel() + " (" + r.getRelatedConceptCode() + ")] --> (" + r.getRelationship() + ")");
			System.out.println(r.toString());
		}
	}
}