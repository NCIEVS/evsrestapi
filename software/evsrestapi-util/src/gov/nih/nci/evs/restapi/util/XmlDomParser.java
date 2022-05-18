package gov.nih.nci.evs.restapi.util;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.File;


/**
 * <!-- LICENSE_TEXT_START -->
 * Copyright 2022 Guidehouse. This software was developed in conjunction
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
 *      "This product includes software developed by Guidehouse and the National
 *      Cancer Institute."   If no such end-user documentation is to be
 *      included, this acknowledgment shall appear in the software itself,
 *      wherever such third-party acknowledgments normally appear.
 *   3. The names "The National Cancer Institute", "NCI" and "Guidehouse" must
 *      not be used to endorse or promote products derived from this software.
 *   4. This license does not authorize the incorporation of this software
 *      into any third party proprietary programs. This license does not
 *      authorize the recipient to use any trademarks owned by either NCI
 *      or GUIDEHOUSE
 *   5. THIS SOFTWARE IS PROVIDED "AS IS," AND ANY EXPRESSED OR IMPLIED
 *      WARRANTIES, (INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *      OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE) ARE
 *      DISCLAIMED. IN NO EVENT SHALL THE NATIONAL CANCER INSTITUTE,
 *      GUIDEHOUSE, OR THEIR AFFILIATES BE LIABLE FOR ANY DIRECT, INDIRECT,
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

public class XmlDomParser {

	public static Node searchNode(NodeList nodeList, String nodeName) {
		if (nodeList.getLength() == 0) return null;
		for (int count = 0; count < nodeList.getLength(); count++) {
			Node tempNode = nodeList.item(count);
			if (tempNode.getNodeName().compareTo(nodeName) == 0) {
				return tempNode;
			} else {
				Node node = searchNode(tempNode.getChildNodes(), nodeName);
				if (node != null && node.getNodeName().compareTo(nodeName) == 0) {
					return node;
				}
			}
		}
		return null;
	}

	private static void printNote(NodeList nodeList) {
		for (int count = 0; count < nodeList.getLength(); count++) {
			Node tempNode = nodeList.item(count);
			// make sure it's element node.
			if (tempNode.getNodeType() == Node.ELEMENT_NODE) {
				// get node name and value
				System.out.println("\nNode Name=" + tempNode.getNodeName() + " [OPEN]");
				System.out.println("Node Value=" + tempNode.getTextContent());
				if (tempNode.hasAttributes()) {
					// get attributes names and values
					NamedNodeMap nodeMap = tempNode.getAttributes();
					for (int i = 0; i < nodeMap.getLength(); i++) {
						Node node = nodeMap.item(i);
						System.out.println("attr name: " + node.getNodeName());
						System.out.println("attr value: " + node.getNodeValue());
					}
				}
				if (tempNode.hasChildNodes()) {
					// loop again if has child nodes
					printNote(tempNode.getChildNodes());
				}
				System.out.println("Node Name=" + tempNode.getNodeName() + " [CLOSE]");
			}
		}
	}


	public static InputStream readXmlFileIntoInputStream(String fileName) {
		String currentDir = System.getProperty("user.dir");
		fileName = currentDir + File.separator + fileName;
		System.out.println(fileName);
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(new File(fileName));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return fis;
	}


	public static void main(String[] args) {
		String xmlfile = args[0];
		// Instantiate the Factory
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try (InputStream is = readXmlFileIntoInputStream(xmlfile)) {
			// parse XML file
			DocumentBuilder db = dbf.newDocumentBuilder();

			// read from a project's resources folder
			Document doc = db.parse(is);

			System.out.println("Root Element :" + doc.getDocumentElement().getNodeName());

            String nodeName = "met:concise_name";
            Node node = searchNode(doc.getChildNodes(), nodeName);
            if (node != null) {
				System.out.println(nodeName + ": " + node.getTextContent());
			} else {
				System.out.println(nodeName + ": NOT FOUND");
			}

            nodeName = "met:description";
            node = searchNode(doc.getChildNodes(), nodeName);
            if (node != null) {
				System.out.println(nodeName + ": " + node.getTextContent());
			} else {
				System.out.println(nodeName + ": NOT FOUND");
			}

            nodeName = "met:version_releaseDate";
            node = searchNode(doc.getChildNodes(), nodeName);
            if (node != null) {
				System.out.println(nodeName + ": " + node.getTextContent());
			} else {
				System.out.println(nodeName + ": NOT FOUND");
			}
            nodeName = "met:map_rank_applicable";
            node = searchNode(doc.getChildNodes(), nodeName);
            if (node != null) {
				System.out.println(nodeName + ": " + node.getTextContent());
			} else {
				System.out.println(nodeName + ": NOT FOUND");
			}

		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
		}
	}
}