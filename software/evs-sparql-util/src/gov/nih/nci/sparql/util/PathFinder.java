package gov.nih.nci.sparql.util;


import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.XStream;
import gov.nih.nci.sparql.bean.*;
import java.io.*;
import java.util.*;
import org.json.*;


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


public class PathFinder {
	public static final String XML_DECLARATION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

// Variable declaration
	private HierarchyHelper hierarchyHelper;
	private String codingScheme;
	private String version;

    public PathFinder(Vector v) {
		this.hierarchyHelper = new HierarchyHelper(v);
		this.hierarchyHelper.findRootAndLeafNodes();
	}

// Constructor
	public PathFinder(
		HierarchyHelper hierarchyHelper,
		String codingScheme,
		String version) {

		this.hierarchyHelper = hierarchyHelper;
		this.codingScheme = codingScheme;
		this.version = version;
	}

// Set methods
	public void setHierarchyHelper(HierarchyHelper hierarchyHelper) {
		this.hierarchyHelper = hierarchyHelper;
	}

	public void setCodingScheme(String codingScheme) {
		this.codingScheme = codingScheme;
	}

	public void setVersion(String version) {
		this.version = version;
	}


// Get methods
	public HierarchyHelper getHierarchyHelper() {
		return this.hierarchyHelper;
	}

	public String getCodingScheme() {
		return this.codingScheme;
	}

	public String getVersion() {
		return this.version;
	}

    public Path createPath(String path) {
		Path p = new Path();
		List concepts = new ArrayList();
        Vector codes = StringUtils.parseData(path);
        for (int i=0; i<codes.size(); i++) {
			String code = (String) codes.elementAt(i);
			String name = hierarchyHelper.getLabel(code);
			Concept concept = new Concept(
				i,
				codingScheme,
				version,
				name,
				code);
			concepts.add(concept);
		}
		p.setDirection(TreeBuilder.TRAVERSE_UP);
		p.setConcepts(concepts);
		return p;
	}

	public Paths findPaths() {
		Paths paths = new Paths();
		Stack stack = new Stack();
		Vector roots = hierarchyHelper.getRoots();
		for (int i=0; i<roots.size(); i++) {
			String code = (String) roots.elementAt(i);
			stack.push(code);
		}
		while (!stack.isEmpty()) {
			String path = (String) stack.pop();
			Vector u = StringUtils.parseData(path);
			String last_code = (String) u.elementAt(u.size()-1);
			Vector subs = hierarchyHelper.getSubclassCodes(last_code);
			if (subs == null) {
				//System.out.println(path);
				paths.add(createPath(path));
			} else {
				for (int k=0; k<subs.size(); k++) {
					String s = (String) subs.elementAt(k);
					stack.push(path + "|" + s);
				}
			}
		}
        return paths;
	}

	public String marshalPaths(Paths paths) throws Exception {
		XStream xstream_xml = new XStream(new DomDriver());
        String xml = XML_DECLARATION + "\n" + xstream_xml.toXML(paths);
        return xml;
	}

	public void dumpPaths(Paths paths) {
		System.out.println(paths.getPathCount());
		List list = paths.getPaths();
		for (int k=0; k<list.size(); k++) {
			Path path = (Path) list.get(k);
			List conceptList = path.getConcepts();
			for (int k2=0; k2<conceptList.size(); k2++) {
				Concept c = (Concept) conceptList.get(k2);
				System.out.println("\ndistance: " + c.getIdx());
				System.out.println("scheme: " + c.getCodingScheme());
				System.out.println("version: " + c.getVersion());
				System.out.println("name: " + c.getLabel());
				System.out.println("code: " + c.getCode());
			}
		}
	}
}

