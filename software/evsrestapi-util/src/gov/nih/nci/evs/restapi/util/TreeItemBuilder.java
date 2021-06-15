package gov.nih.nci.evs.restapi.util;
import gov.nih.nci.evs.restapi.bean.*;

import java.io.*;
import java.util.*;


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


public class TreeItemBuilder {
	HierarchyHelper hh = null;
	String parent_child_file = null;

    public TreeItemBuilder(String parent_child_file) {
		this.parent_child_file = parent_child_file;
		Vector v = Utils.readFile(parent_child_file);
		this.hh = new HierarchyHelper(v, 1);
	}

	public TreeItem run() {
		TreeItem rootItem = new TreeItem("<Root>", "Root node");
		Vector child_codes = hh.getSubclassCodes("<Root>");
		for (int i=0; i<child_codes.size(); i++) {
			String code = (String) child_codes.elementAt(i);
			String label = hh.getLabel(code);
			TreeItem sub_item = createTreeItem(code, label);
			rootItem.addChild("inverse_is_a", sub_item);
			rootItem._expandable = true;
		}
		return rootItem;
	}

	public TreeItem createTreeItem(String code, String label) {
		TreeItem ti = new TreeItem(code, label);
		Vector child_codes = hh.getSubclassCodes(code);
		if (child_codes != null && child_codes.size() > 0) {
			for (int j=0; j<child_codes.size(); j++) {
				String child_code = (String) child_codes.elementAt(j);
				String child_label = hh.getLabel(child_code);
				TreeItem sub_item = createTreeItem(child_code, child_label);
				ti.addChild("inverse_is_a", sub_item);
				ti._expandable = true;
			}
		}
		return ti;
	}

	public static void main(String[] args) {
        long ms = System.currentTimeMillis();
        String title = args[0];
        String parent_child_file = args[1];
        TreeItemBuilder treeItemBuilder = new TreeItemBuilder(parent_child_file);

		PrintWriter pw = null;
		String outputfile = title + ".html";
		outputfile = outputfile.replace(" ", "_");
		try {
			pw = new PrintWriter(outputfile, "UTF-8");
			TreeItem ti = treeItemBuilder.run();
			boolean print_code_first = false;
            TreeItem.printTree(pw, ti, 0, print_code_first);

		} catch (Exception ex) {

		} finally {
			try {
				pw.close();
				System.out.println("Output file " + outputfile + " generated.");
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}
}
