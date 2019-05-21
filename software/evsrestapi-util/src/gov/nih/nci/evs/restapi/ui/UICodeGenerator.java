package gov.nih.nci.evs.restapi.ui;

import gov.nih.nci.evs.restapi.util.*;
import gov.nih.nci.evs.restapi.bean.*;


import java.io.*;
import java.util.*;
import java.text.*;
import java.sql.*;
import java.net.*;

import org.apache.commons.lang.*;
//import org.apache.log4j.*;
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

 public class UICodeGenerator {
	 public String application_name = "ncimbrowser";
	 public String bookmark_url = null;

	 public UICodeGenerator() {

	 }

	 public void set_application_name(String application_name) {
		 this.application_name = application_name;
	 }

	 public void set_bookmark_url(String bookmark_url) {
		 this.bookmark_url = bookmark_url;
	 }

	 public static String escapeDoubleQuotes(String inputStr) {
		 char doubleQ = '"';
		 StringBuffer buf = new StringBuffer();
		 for (int i=0;  i<inputStr.length(); i++) {
			 char c = inputStr.charAt(i);
			 if (c == doubleQ) {
				 buf.append("\\").append(doubleQ);
			 } else {
			     buf.append(c);
			 }
		 }
		 return buf.toString();
	 }

	 public static String vector2String(Vector v) {
		StringBuffer buf = new StringBuffer();
		String s = "StringBuffer buf = new StringBuffer();";
		s = s + "\n";
		buf.append(s);
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			line = escapeDoubleQuotes(line);
			String t = "buf.append(\"" + line + "\").append(\"\\n\");";
			buf.append(t).append("\n");
		}
		s = "return buf.toString();";
		s = s + "\n";
		buf.append(s);
		return buf.toString();
	 }

     public static Boolean isEven (Integer i) {
        return (i % 2) == 0;
     }


     public String generateSynonymTable(String code, Vector syn_vec) {
		 Vector sourcesWithHierarchy_vec = null;
		 return generateSynonymTable(code, syn_vec, sourcesWithHierarchy_vec);
	 }

     public String generateSynonymTable(String code, Vector syn_vec, Vector sourcesWithHierarchy_vec) {
		 Vector codingSchemes = null;
		 return generateSynonymTable(code, syn_vec, sourcesWithHierarchy_vec, codingSchemes);
	 }

     public String generateSynonymTable(String code, Vector syn_vec, Vector sourcesWithHierarchy_vec, Vector codingSchemes) {
		StringBuffer buf = new StringBuffer();
		buf.append("<table border=\"0\" width=\"708px\" role='presentation'>").append("\n");
		buf.append("<tr>").append("\n");
		buf.append("<td class=\"textsubtitle-blue\" align=\"left\">Synonym Details:<a name=\"SynonymsDetails\"></a></td>").append("\n");
		buf.append("</tr>").append("\n");
		buf.append("</table> ").append("\n");
		buf.append("").append("\n");
		buf.append("<table class=\"datatable_960\" border=\"0\" width=\"100%\">").append("\n");
		buf.append("<col width=\"530\">").append("\n");
		buf.append("<col width=\"120\">").append("\n");
		buf.append("<col width=\"80\">").append("\n");
		buf.append("<col width=\"200\">      ").append("\n");
		buf.append("<tr>").append("\n");
		buf.append("<th class=\"dataTableHeader\" scope=\"col\" align=\"left\">").append("\n");
		buf.append("Term").append("\n");
		buf.append("</th>").append("\n");
		buf.append("<th class=\"dataTableHeader\" scope=\"col\" align=\"left\">").append("\n");
		buf.append("<a href=\"/" + application_name + "/ConceptReport.jsp?dictionary=NCI Metathesaurus&code=" + code + "&type=synonym&sortBy=source#SynonymsDetails\">Source</a>").append("\n");
		buf.append("<a href=\"#\" onclick=\"javascript:window.open('/" + application_name + "/pages/source_help_info.jsf',").append("\n");
		buf.append("'_blank','top=100, left=100, height=740, width=680, status=no, menubar=no, resizable=yes, scrollbars=yes, toolbar=no, location=no, directories=no');\">").append("\n");
		buf.append("<img src=\"/" + application_name + "/images/help.gif\" alt=\"Source List\" title=\"Source List\" border=\"0\">").append("\n");
		buf.append("</a>").append("\n");
		buf.append("</th>").append("\n");
		buf.append("<th class=\"dataTableHeader\" scope=\"col\" align=\"left\">").append("\n");
		buf.append("<a href=\"/" + application_name + "/ConceptReport.jsp?dictionary=NCI Metathesaurus&code=" + code + "&type=synonym&sortBy=type#SynonymsDetails\">Type</a>").append("\n");
		buf.append("<a href=\"#\" onclick=\"javascript:window.open('/" + application_name + "/pages/term_type_help_info.jsf',").append("\n");
		buf.append("'_blank','top=100, left=100, height=740, width=680, status=no, menubar=no, resizable=yes, scrollbars=yes, toolbar=no, location=no, directories=no');\">").append("\n");
		buf.append("<img src=\"/" + application_name + "/images/help.gif\" alt=\"Term Type Definitions\" title=\"Term Type Definitions\" border=\"0\">").append("\n");
		buf.append("</a>").append("\n");
		buf.append("</th>").append("\n");
		buf.append("<th class=\"dataTableHeader\" scope=\"col\" align=\"left\">").append("\n");
		buf.append("<a href=\"/" + application_name + "/ConceptReport.jsp?dictionary=NCI Metathesaurus&code=" + code + "&type=synonym&sortBy=code#SynonymsDetails\">Code</a>").append("\n");
		buf.append("</th>").append("\n");
		buf.append("</tr>").append("\n");
if (syn_vec != null) {
		for (int i=0; i<syn_vec.size(); i++) {
			//String syn = (String) syn_vec.elementAt(i);
			//Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(syn, '|');
			String dataRowColor = "dataRowDark";
			if (!isEven(i).equals(Boolean.TRUE)) {
				dataRowColor = "dataRowLight";
			}
			buf.append("<tr class=\"" + dataRowColor + "\">").append("\n");
			buf.append("<td>").append("\n");
			buf.append("<div style=\"width: 530; word-wrap: break-word\">").append("\n");

			Synonym syn = (Synonym) syn_vec.elementAt(i);
	        String ncit_code = null;
			String term = syn.getTermName();
			String sab = syn.getTermSource();

			String type = syn.getTermGroup();
			String src_code = syn.getSourceCode();
			buf.append("" + term + "").append("\n");
			buf.append("</div>").append("\n");
			buf.append("</td>               ").append("\n");
			buf.append("<td class=\"dataCellText\">" + sab + "</td>").append("\n");
			buf.append("<td class=\"dataCellText\">" + type + "</td>").append("\n");
			buf.append("<td class=\"dataCellText\">").append("\n");

            if (codingSchemes != null && codingSchemes.contains(sab)) {
                buf.append(getCodeHyperlink(src_code, sab));
			} else {
				buf.append(src_code);
			}
			if (sourcesWithHierarchy_vec != null && sourcesWithHierarchy_vec.contains(sab)) {
				buf.append(" &nbsp;" + getViewInSourceHierarchyLink(code, sab));
			}
            buf.append("\n");
			buf.append("</td>").append("\n");
			buf.append("</tr>").append("\n");
		}
}
		buf.append("</table>").append("\n");
		return buf.toString();
	 }

     public String generateRelationshipTable(String table_name, Vector rel_vec) {
		StringBuffer buf = new StringBuffer();
		buf.append("<span class=\"textsubtitle-blue-small\">" + table_name + " Concepts:</span><a name=\"" + table_name + "\"></a>").append("\n");
		buf.append("<table class=\"datatable_960\">").append("\n");
		buf.append("<th class=\"dataTableHeader\" scope=\"col\" align=\"left\" width=\"15%\">Relationship").append("\n");
		buf.append("<a href=\"#\" onclick=\"javascript:window.open('/" + application_name + "/pages/rel_help_info.jsf',").append("\n");
		buf.append("'_blank','top=100, left=100, height=740, width=680, status=no, menubar=no, resizable=yes, scrollbars=yes, toolbar=no, location=no, directories=no');\">").append("\n");
		buf.append("<img src=\"/" + application_name + "/images/help.gif\" alt=\"Relationship Definitions\" title=\"Relationship Definitions\" border=\"0\">").append("\n");
		buf.append("</a>").append("\n");
		buf.append("</th>").append("\n");
		buf.append("<th class=\"dataTableHeader\" scope=\"col\" align=\"left\" width=\"25%\">").append("\n");
		buf.append("").append("\n");
		buf.append("<a href=\"/" + application_name + "/pages/concept_details.jsf?type=relationship&sort0=rela&sort1=source&sort2=source&sort3=source&sort4=source&sort5=source#Parent\">").append("\n");
		buf.append("Rel. Attribute").append("\n");
		buf.append("</a>").append("\n");
		buf.append("<a href=\"#\" onclick=\"javascript:window.open('/" + application_name + "/pages/rela_help_info.jsf',").append("\n");
		buf.append("'_blank','top=100, left=100, height=740, width=680, status=no, menubar=no, resizable=yes, scrollbars=yes, toolbar=no, location=no, directories=no');\">").append("\n");
		buf.append("<img src=\"/" + application_name + "/images/help.gif\" alt=\"Relationship Attr. Definitions\" title=\"Relationship Attr. Definitions\" border=\"0\">").append("\n");
		buf.append("</a>").append("\n");
		buf.append("</th>").append("\n");
		buf.append("<th class=\"dataTableHeader\" scope=\"col\" align=\"left\" width=\"45%\">").append("\n");
		buf.append("<a href=\"/" + application_name + "/pages/concept_details.jsf?type=relationship&sort0=name&sort1=source&sort2=source&sort3=source&sort4=source&sort5=source#Parent\">").append("\n");
		buf.append("Name").append("\n");
		buf.append("</a>").append("\n");
		buf.append("</th>").append("\n");
		buf.append("<th class=\"dataTableHeader\" scope=\"col\" align=\"left\" width=\"15%\">").append("\n");
		buf.append("<a href=\"/" + application_name + "/pages/concept_details.jsf?type=relationship&sort0=source&sort1=source&sort2=source&sort3=source&sort4=source&sort5=source#Parent\">").append("\n");
		buf.append("Rel. Source").append("\n");
		buf.append("</a>").append("\n");
		buf.append("<a href=\"#\" onclick=\"javascript:window.open('/" + application_name + "/pages/source_help_info.jsf',").append("\n");
		buf.append("'_blank','top=100, left=100, height=740, width=680, status=no, menubar=no, resizable=yes, scrollbars=yes, toolbar=no, location=no, directories=no');\">").append("\n");
		buf.append("<img src=\"/" + application_name + "/images/help.gif\" alt=\"Source List\" title=\"Source List\" border=\"0\">").append("\n");
		buf.append("</a>").append("\n");
		buf.append("</th>").append("\n");
		buf.append("    ").append("\n");

if (rel_vec != null) {
		for (int i=0; i<rel_vec.size(); i++) {
			String line = (String) rel_vec.elementAt(i);
			Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(line, '|');
			String dataRowColor = "dataRowDark";
			if (!isEven(i).equals(Boolean.TRUE)) {
				dataRowColor = "dataRowLight";
			}
			buf.append("<tr class=\"" + dataRowColor + "\">").append("\n");

			String rela = (String) u.elementAt(1);
			String name = (String) u.elementAt(4);
			String cui = (String) u.elementAt(2);
			String sab = (String) u.elementAt(6);

			buf.append("<td width=100>" + table_name + "</td>").append("\n");
			buf.append("<td width=180>" + rela + "</td>").append("\n");
			buf.append("<td>").append("\n");
			buf.append("<a href=\"/" + application_name + "/ConceptReport.jsp?dictionary=NCI Metathesaurus&code=" + cui + "\">").append("\n");
			buf.append("" + name + "").append("\n");
			buf.append("</a>").append("\n");
			buf.append("</td>").append("\n");
			buf.append("<td width=85>" + sab + "</td>").append("\n");
			buf.append("</tr>").append("\n");
		}
		buf.append("</table>").append("\n");
		buf.append("<p>").append("\n");
		buf.append("</p>").append("\n");
}
		return buf.toString();
	 }

     public String generateRelationshipTable(HashMap rel_hashmap) {
		StringBuffer buf = new StringBuffer();
		buf.append("<table border=\"0\" width=\"708px\" role='presentation'>").append("\n");
		buf.append("<tr>").append("\n");
		buf.append("<td class=\"textsubtitle-blue\" align=\"left\">").append("\n");
		buf.append("Relationships with other NCI Metathesaurus Concepts:").append("\n");
		buf.append("</td>").append("\n");
		buf.append("</tr>").append("\n");
		buf.append("</table>").append("\n");
		buf.append("").append("\n");
		buf.append("<table border=\"0\" width=\"690px\" bgcolor=\"#ffffff\" cellpadding=\"5\" cellspacing=\"0\" role='presentation'>").append("\n");
		buf.append("<tr>").append("\n");
		buf.append("<td class=\"subtab\" align=\"center\">").append("\n");
		buf.append("<b>").append("\n");
		buf.append("<a href=\"#Parent\">Parents</a> |").append("\n");
		buf.append("<a href=\"#Child\">Children</a> |").append("\n");
		buf.append("<a href=\"#Broader\">Broader</a> |").append("\n");
		buf.append("<a href=\"#Narrower\">Narrower</a> |").append("\n");
		buf.append("<a href=\"#Sibling\">Siblings</a> |").append("\n");
		buf.append("<a href=\"#Other\">Other</a>").append("\n");
		buf.append("</b>").append("\n");
		buf.append("</td>").append("\n");
		buf.append("</tr>").append("\n");
		buf.append("</table>").append("\n");
		buf.append("<p>").append("\n");
		buf.append("</p>").append("\n");

if (rel_hashmap != null) {
		String table_name = "Parent";
		String content = generateRelationshipTable(table_name, (Vector) rel_hashmap.get("PAR"));
		buf.append(content);
		table_name = "Child";
		content = generateRelationshipTable(table_name, (Vector) rel_hashmap.get("CHD"));
		buf.append(content);
		table_name = "Broader";
		content = generateRelationshipTable(table_name, (Vector) rel_hashmap.get("RB"));
		buf.append(content);
		table_name = "Narrower";
		content = generateRelationshipTable(table_name, (Vector) rel_hashmap.get("RN"));
		buf.append(content);
		table_name = "Sibling";
		content = generateRelationshipTable(table_name, (Vector) rel_hashmap.get("SIB"));
		buf.append(content);
		table_name = "Other";
		content = generateRelationshipTable(table_name, (Vector) rel_hashmap.get("OTH"));
		buf.append(content);
}
		return buf.toString();
	}

    public String generateSourceTable(String source, Vector source_vec) {
		StringBuffer buf = new StringBuffer();
		String str = generateSourceSynonymTable(source, source_vec);
		buf.append(str);

		buf.append("<span class=\"textsubtitle-blue-small\">Relationships</span><a name=\"Relationships\"></a>").append("\n");
		buf.append("<table class=\"datatable_960\" border=\"0\">").append("\n");
		buf.append("<tr>").append("\n");
		buf.append("<th class=\"dataTableHeader\" scope=\"col\" align=\"left\" valign=\"top\" nowrap>").append("\n");
		buf.append("Relationship").append("\n");
		buf.append("<a href=\"#\" onclick=\"javascript:window.open('/" + application_name + "/pages/rel_help_info.jsf',").append("\n");
		buf.append("'_blank','top=100, left=100, height=740, width=680, status=no, menubar=no, resizable=yes, scrollbars=yes, toolbar=no, location=no, directories=no');\">").append("\n");
		buf.append("<img src=\"/" + application_name + "/images/help.gif\" alt=\"Relationship Definitions\" title=\"Relationship Definitions\" border=\"0\">").append("\n");
		buf.append("</a>").append("\n");
		buf.append("</th>").append("\n");
		buf.append("<th class=\"dataTableHeader\" scope=\"col\" align=\"left\" valign=\"top\" nowrap>").append("\n");
		buf.append("<a href=\"/" + application_name + "/pages/concept_details.jsf?code=C0007581&type=sources&sortBy2=rel&sortBy=name&sab=NCI#Relationships\">Rel. Attribute</a>").append("\n");
		buf.append("<a href=\"#\" onclick=\"javascript:window.open('/" + application_name + "/pages/rela_help_info.jsf',").append("\n");
		buf.append("'_blank','top=100, left=100, height=740, width=680, status=no, menubar=no, resizable=yes, scrollbars=yes, toolbar=no, location=no, directories=no');\">").append("\n");
		buf.append("<img src=\"/" + application_name + "/images/help.gif\" alt=\"Relationship Attr. Definitions\" title=\"Relationship Attr. Definitions\" border=\"0\">").append("\n");
		buf.append("</a>").append("\n");
		buf.append("</th>").append("\n");
		buf.append("<th class=\"dataTableHeader\" scope=\"col\" align=\"left\">").append("\n");
		buf.append("<a href=\"/" + application_name + "/pages/concept_details.jsf?code=C0007581&type=sources&sortBy2=cui&sortBy=name&sab=NCI#Relationships\">CUI</a>").append("\n");
		buf.append("</th>").append("\n");
		buf.append("<th class=\"dataTableHeader\" scope=\"col\" align=\"left\">").append("\n");
		buf.append("<a href=\"/" + application_name + "/pages/concept_details.jsf?code=C0007581&type=sources&sortBy2=name&sortBy=name&sab=NCI#Relationships\">Term</a>").append("\n");
		buf.append("</th>").append("\n");
		buf.append("<th class=\"dataTableHeader\" scope=\"col\" align=\"left\" valign=\"top\">").append("\n");
		buf.append("Source").append("\n");
		buf.append("<a href=\"#\" onclick=\"javascript:window.open('/" + application_name + "/pages/source_help_info.jsf',").append("\n");
		buf.append("'_blank','top=100, left=100, height=740, width=680, status=no, menubar=no, resizable=yes, scrollbars=yes, toolbar=no, location=no, directories=no');\">").append("\n");
		buf.append("<img src=\"/" + application_name + "/images/help.gif\" alt=\"Source List\" title=\"Source List\" border=\"0\">").append("\n");
		buf.append("</a>").append("\n");
		buf.append("</th>").append("\n");
		buf.append("<th class=\"dataTableHeader\" scope=\"col\" align=\"left\" valign=\"top\">").append("\n");
		buf.append("<a href=\"/" + application_name + "/pages/concept_details.jsf?code=C0007581&type=sources&sortBy2=type&sortBy=name&sab=NCI#Relationships\">Type</a>").append("\n");
		buf.append("<a href=\"#\" onclick=\"javascript:window.open('/" + application_name + "/pages/term_type_help_info.jsf',").append("\n");
		buf.append("'_blank','top=100, left=100, height=740, width=680, status=no, menubar=no, resizable=yes, scrollbars=yes, toolbar=no, location=no, directories=no');\">").append("\n");
		buf.append("<img src=\"/" + application_name + "/images/help.gif\" alt=\"Term Type Definition\" title=\"Term Type Definition\" border=\"0\">").append("\n");
		buf.append("</a>").append("\n");
		buf.append("</th>").append("\n");
		buf.append("<th class=\"dataTableHeader\" scope=\"col\" align=\"left\" valign=\"top\">").append("\n");
		buf.append("<a href=\"/" + application_name + "/pages/concept_details.jsf?code=C0007581&type=sources&sortBy2=code&sortBy=name&sab=NCI#Relationships\">Code</a>").append("\n");
		buf.append("</th>").append("\n");
		buf.append("</tr>").append("\n");


if (source_vec != null) {

        int lcv = 0;
		for (int i=0; i<source_vec.size(); i++) {
			String line = (String) source_vec.elementAt(i);
			Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(line, '|');
			String rel = (String) u.elementAt(0);
			String rela = (String) u.elementAt(1);
			String cui = (String) u.elementAt(2);
            String term = (String) u.elementAt(3);
			String sab = (String) u.elementAt(4);
			String type = (String) u.elementAt(5);
			String code = (String) u.elementAt(6);
			if (sab.compareTo(source) == 0) {
				String dataRowColor = "dataRowDark";
				if (!isEven(lcv).equals(Boolean.TRUE)) {
					dataRowColor = "dataRowLight";
				}
				buf.append("<tr class=\"" + dataRowColor + "\">").append("\n");
				buf.append("<td class=\"dataCellText\" width=80>" + rel + "</td>").append("\n");
				buf.append("<td class=\"dataCellText\" width=230>" + rela + "</td>").append("\n");
				buf.append("<td class=\"dataCellText\" width=66>").append("\n");
				buf.append("<a href=\"/" + application_name + "/ConceptReport.jsp?dictionary=NCI Metathesaurus&code=C0007613\">").append("\n");
				buf.append("" + cui + "").append("\n");
				buf.append("</a>").append("\n");
				buf.append("</td>").append("\n");
				buf.append("<td class=\"dataCellText\" width=130><a href=\"/" + application_name + "/ConceptReport.jsp?dictionary=NCI Metathesaurus&code=C0007613\">" + term + "</a></td>").append("\n");
				buf.append("<td class=\"dataCellText\" width=60>" + sab + "</td>").append("\n");
				buf.append("<td class=\"dataCellText\" width=50>" + type + "</td>").append("\n");
				buf.append("<td class=\"dataCellText\" width=50><a href=\"/" + application_name + "/pages/concept_details.jsf?type=sources&cui=" + cui + "&sab=" + sab + "&sourcecode=" + code + "&checkmultiplicity=true\">" + code + "</a></td>").append("\n");
				buf.append("</tr>").append("\n");
				lcv++;
			}
		}
}
		buf.append("</table>").append("\n");
		buf.append("<p>").append("\n");
		buf.append("</p>").append("\n");

		return buf.toString();
	 }

     public String generateSourceSynonymTable(String source, Vector source_vec) {
		StringBuffer buf = new StringBuffer();
		buf.append("<span class=\"textsubtitle-blue-small\">Synonyms</span><a name=\"Synonyms\"></a>").append("\n");
		buf.append("<table class=\"datatable_960\" border=\"0\">").append("\n");
		buf.append("<tr>").append("\n");
		buf.append("<th class=\"dataTableHeader\" scope=\"col\" align=\"left\">").append("\n");
		buf.append("Term").append("\n");
		buf.append("</th>").append("\n");
		buf.append("<th class=\"dataTableHeader\" scope=\"col\" align=\"left\" valign=\"top\" nowrap>").append("\n");
		buf.append("Source").append("\n");
		buf.append("<a href=\"#\" onclick=\"javascript:window.open('/" + application_name + "/pages/source_help_info.jsf',").append("\n");
		buf.append("'_blank','top=100, left=100, height=740, width=680, status=no, menubar=no, resizable=yes, scrollbars=yes, toolbar=no, location=no, directories=no');\">").append("\n");
		buf.append("<img src=\"/" + application_name + "/images/help.gif\" alt=\"Source List\" title=\"Source List\" border=\"0\">").append("\n");
		buf.append("</a>").append("\n");
		buf.append("</th>").append("\n");
		buf.append("<th class=\"dataTableHeader\" scope=\"col\" align=\"left\" valign=\"top\" nowrap>").append("\n");
		buf.append("<a href=\"/" + application_name + "/pages/concept_details.jsf?code=C0007581&type=sources&sortBy=type&sortBy2=rel_type&sab=NCI#Synonyms\">Type</a>").append("\n");
		buf.append("<a href=\"#\" onclick=\"javascript:window.open('/" + application_name + "/pages/term_type_help_info.jsf',").append("\n");
		buf.append("'_blank','top=100, left=100, height=740, width=780, status=no, menubar=no, resizable=yes, scrollbars=yes, toolbar=no, location=no, directories=no');\">").append("\n");
		buf.append("<img src=\"/" + application_name + "/images/help.gif\" alt=\"Term Type Definition\" title=\"Term Type Definition\" border=\"0\">").append("\n");
		buf.append("</a>").append("\n");
		buf.append("</th>").append("\n");
		buf.append("<th class=\"dataTableHeader\" scope=\"col\" align=\"left\" valign=\"top\" nowrap>").append("\n");
		buf.append("<a href=\"/" + application_name + "/pages/concept_details.jsf?code=C0007581&type=sources&sortBy=code&sortBy2=rel_type&sab=NCI#Synonyms\">Code</a>").append("\n");
		buf.append("</th>").append("\n");
		buf.append("</tr>").append("\n");

if (source_vec != null) {
        int lcv = 0;
		for (int i=0; i<source_vec.size(); i++) {
			String line = (String) source_vec.elementAt(i);
			Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(line, '|');
			String rel = (String) u.elementAt(0);
			String rela = (String) u.elementAt(1);
			String cui = (String) u.elementAt(2);
            String term = (String) u.elementAt(3);
			String sab = (String) u.elementAt(4);
			String type = (String) u.elementAt(5);
			String code = (String) u.elementAt(6);
			if (sab.compareTo(source) == 0) {
				String dataRowColor = "dataRowDark";
				if (!isEven(lcv).equals(Boolean.TRUE)) {
					dataRowColor = "dataRowLight";
				}
				buf.append("<tr class=\"" + dataRowColor + "\">").append("\n");
				buf.append("<td class=\"dataCellText\">" + term + "</td>").append("\n");
				buf.append("<td class=\"dataCellText\" width=60>" + sab + "</td>").append("\n");
				buf.append("<td class=\"dataCellText\" width=50>" + type + "</td>").append("\n");
				buf.append("<td class=\"dataCellText\" width=50>" + code + "</td>").append("\n");
				buf.append("</tr>").append("\n");
				lcv++;
			}
		}
		buf.append("</table>").append("\n");
		buf.append("<p></p>").append("\n");
}
		return buf.toString();
	 }

     public String generatePropertyTable(String cui, HashMap property_hashmap) {
		StringBuffer buf = new StringBuffer();
		buf.append("<table border=\"0\" width=\"708px\" role='presentation'>").append("\n");
		buf.append("<tr>").append("\n");
		buf.append("<td class=\"textsubtitle-blue\" align=\"left\">").append("\n");
		buf.append("Terms & Properties").append("\n");
		buf.append("</td>").append("\n");
		buf.append("</tr>").append("\n");
		buf.append("</table>").append("\n");
		buf.append("<p><b>Concept Unique Identifier (CUI):&nbsp;</b>" + cui + "</p>").append("\n");
		buf.append("<p>").append("\n");

if(property_hashmap != null) {
		String ncit_code = null;
		Vector v = (Vector) property_hashmap.get("Synonym");
		int lcv = 0;
		for (int i=0; i<v.size(); i++) {
			Synonym syn = (Synonym) v.elementAt(i);
			String termname = syn.getTermName();
			String source = syn.getTermSource();
			if (source.compareTo("NCI") == 0) {
				ncit_code = syn.getSourceCode();
				break;
			}
		}
		buf.append("<b>NCI Thesaurus Code:&nbsp;</b>" + ncit_code + "&nbsp;").append("\n");
		buf.append("<a href=\"javascript:redirect_site('https://ncit.nci.nih.gov/ncitbrowser/pages/concept_details.jsf?dictionary=NCI%20Thesaurus&code=" + ncit_code + "')\">(see NCI Thesaurus info)</a>").append("\n");
		buf.append("</p><p>").append("\n");

		v = (Vector) property_hashmap.get("Property");
		for (int i=0; i<v.size(); i++) {
			Property prop = (Property) v.elementAt(i);
			String prop_name = prop.getName();
			if (prop_name.compareTo("Semantic_Type") == 0) {
				String semantic_type = prop.getValue();
				buf.append("<p><b>Semantic Type:&nbsp;</b>" + semantic_type + "</p>").append("\n");
			}
		}
		buf.append("</p>").append("\n");

		v = (Vector) property_hashmap.get("Definition");
		for (int i=0; i<v.size(); i++) {
			Definition def = (Definition) v.elementAt(i);
			String description = def.getDescription();
			String source = def.getSource();
			if (source.compareTo("NCI") == 0) {
				source = "NCIt";
			}
			buf.append("<p><b>" + source + " Definition:&nbsp;</b>" + description + "</p>").append("\n");
		}

		buf.append("<p>").append("\n");
		buf.append("<b>Synonyms &amp; Abbreviations:</b>").append("\n");
		buf.append("<a href=\"/" + application_name + "/pages/concept_details.jsf?dictionary=NCI Metathesaurus&code=C0007581&type=synonym\">(see Synonym Details)</a>").append("\n");
		buf.append("<table class=\"datatable_960\">").append("\n");
		v = (Vector) property_hashmap.get("Synonym");
		lcv = 0;
		HashSet syn_hset = new HashSet();
		int k = 0;
		for (int i=0; i<v.size(); i++) {
			Synonym syn = (Synonym) v.elementAt(i);
			String termname = syn.getTermName();
			if (!syn_hset.contains(termname)) {
				syn_hset.add(termname);
				String dataRowColor = "dataRowDark";
				if (!isEven(lcv).equals(Boolean.TRUE)) {
					dataRowColor = "dataRowLight";
				}
				buf.append("<tr class=\"" + dataRowColor + "\">").append("\n");
				buf.append("<td>" + termname + "</td>").append("\n");
				buf.append("</tr>").append("\n");
				lcv++;
			}
		}
		buf.append("</table>").append("\n");
		buf.append("</p>").append("\n");

		buf.append("<p>").append("\n");
		buf.append("<b>Other Properties:</b>").append("\n");
		buf.append("<a href=\"#\" onclick=\"javascript:window.open('/" + application_name + "/pages/property_help_info.jsf',").append("\n");
		buf.append("'_blank','top=100, left=100, height=740, width=680, status=no, menubar=no, resizable=yes, scrollbars=yes, toolbar=no, location=no, directories=no');\">").append("\n");
		buf.append("<img src=\"/" + application_name + "/images/help.gif\" alt=\"Property Definitions\" title=\"Property Definitions\" border=\"0\">").append("\n");
		buf.append("</a>").append("\n");
		buf.append("<table class=\"datatable_960\">").append("\n");
		buf.append("<th class=\"dataTableHeader\" scope=\"col\" align=\"left\" width=\"25%\">").append("\n");
		buf.append("Name").append("\n");
		buf.append("</th>").append("\n");
		buf.append("<th class=\"dataTableHeader\" scope=\"col\" align=\"left\" width=\"60%\">").append("\n");
		buf.append("Value").append("\n");
		buf.append("</th>").append("\n");
		buf.append("<th class=\"dataTableHeader\" scope=\"col\" align=\"left\" width=\"15%\">").append("\n");
		buf.append("Source").append("\n");
		buf.append("</th>").append("\n");

		v = (Vector) property_hashmap.get("Property");
		lcv = 0;
		for (int i=0; i<v.size(); i++) {
			Property prop = (Property) v.elementAt(i);
			String prop_name = prop.getName();
			String prop_value = prop.getValue();
			String prop_source = prop.getSource();
			String dataRowColor = "dataRowDark";
			if (!isEven(lcv).equals(Boolean.TRUE)) {
				dataRowColor = "dataRowLight";
			}
			buf.append("<tr class=\"" + dataRowColor + "\">").append("\n");
			buf.append("<td>" + prop_name + "</td>").append("\n");
			buf.append("<td>" + prop_value + "</td>").append("\n");
			buf.append("<td>" + prop_source + "</td>").append("\n");
			buf.append("</tr>").append("\n");
		}
}
		buf.append("</table>").append("\n");
		buf.append("</p>").append("\n");
		buf.append("<p>").append("\n");
		buf.append("<b>Additional Concept Data:&nbsp;</b> <i>(none)</i>").append("\n");
		buf.append("</p>").append("\n");
		buf.append("<p>").append("\n");
		buf.append("<b>URL to Bookmark</b>:").append("\n");
		//https://ncim65.nci.nih.gov/sparqlncim/ConceptReport.jsp?dictionary=NCI Metathesaurus&code=
		buf.append("<a href=javascript:bookmark('" + bookmark_url + cui + "')>").append("\n");
        buf.append(bookmark_url + cui).append("\n");
		buf.append("</a>").append("\n");
		return buf.toString();
	 }

	 public String generateSourceSelection(String code, String label, String sab, Vector sab_vec) {
		StringBuffer buf = new StringBuffer();
		buf.append("<a name=\"sources\"></a>").append("\n");
		buf.append("<table border=\"0\" width=\"708px\" role='presentation'>").append("\n");
		buf.append("<tr>").append("\n");
		buf.append("<td class=\"textsubtitle-blue\" align=\"left\">'" + label + "'&nbsp;By Source:&nbsp;" + sab + "</td>").append("\n");
		buf.append("</tr>").append("\n");
		buf.append("</table>").append("\n");
		buf.append("<div>      ").append("\n");
		buf.append("<table class=\"datatable_960\" border=\"0\">").append("\n");
		buf.append("<tr>").append("\n");
		buf.append("<td class=\"textbody\">").append("\n");
		buf.append("Select source: &nbsp;").append("\n");

		for (int i=0; i<sab_vec.size(); i++) {
			String source = (String) sab_vec.elementAt(i);
			buf.append("<a href=\"" + application_name + "/pages/concept_details.jsf?type=sources&dictionary=NCI Metathesaurus&code=" + code + "&sab=" + source + "\">").append("\n");
			buf.append(source).append("\n");
			buf.append("</a>&nbsp;").append("\n");
		}
		buf.append("</td>").append("\n");
		buf.append("</tr>").append("\n");
		buf.append("</table>");
		return buf.toString();
	}


	public String generatePaginationForm(int total_count, String matchText) {
        StringBuffer buf = new StringBuffer();
		buf.append("<form id=\"paginationForm\" name=\"paginationForm\" method=\"post\" action=\"/" + application_name + "/pages/search_results.jsf\" enctype=\"application/x-www-form-urlencoded\">").append("\n");
		buf.append("<input type=\"hidden\" name=\"paginationForm\" value=\"paginationForm\" />").append("\n");
		buf.append("<table role='presentation'>").append("\n");
		buf.append("<tr>").append("\n");
		buf.append("<td class=\"textbody\" align=left>").append("\n");
		buf.append("<b>Results 1-50 of&nbsp;" + total_count + "</b>").append("\n");
		buf.append("</td>").append("\n");
		buf.append("<td>").append("\n");
		buf.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;").append("\n");
		buf.append("</td>").append("\n");
		buf.append("<td class=\"textbody\" align=right>").append("\n");
		buf.append("").append("\n");
		buf.append("1&nbsp;").append("\n");
		buf.append("").append("\n");
		buf.append("<a href=\"/" + application_name + "/pages/search_results.jsf?matchText=" + matchText + "&page_number=2\">2</a>").append("\n");
		buf.append("&nbsp;").append("\n");
		buf.append("").append("\n");
		buf.append("&nbsp;").append("\n");
		buf.append("<i>").append("\n");
		buf.append("<a href=\"/" + application_name + "/pages/search_results.jsf?matchText=" + matchText + "&page_number=2\">Next</a>").append("\n");
		buf.append("</i>").append("\n");
		buf.append("").append("\n");
		buf.append("</td>").append("\n");
		buf.append("</tr>").append("\n");
		buf.append("<tr>").append("\n");
		buf.append("<td class=\"textbody\" align=left>").append("\n");
		buf.append("<label for=\"paginationForm:resultsPerPage\">Show</label>").append("\n");
		buf.append("<select id=\"paginationForm:resultsPerPage\" name=\"paginationForm:resultsPerPage\" size=\"1\" onchange=\"submit()\">	<option value=\"10\">10</option>").append("\n");
		buf.append("<option value=\"25\">25</option>").append("\n");
		buf.append("<option value=\"50\" selected=\"selected\">50</option>").append("\n");
		buf.append("<option value=\"75\">75</option>").append("\n");
		buf.append("<option value=\"100\">100</option>").append("\n");
		buf.append("<option value=\"250\">250</option>").append("\n");
		buf.append("<option value=\"500\">500</option>").append("\n");
		buf.append("</select>").append("\n");
		buf.append("&nbsp;results per page").append("\n");
		buf.append("</td>").append("\n");
		buf.append("<td>").append("\n");
		buf.append("&nbsp;&nbsp;").append("\n");
		buf.append("</td>").append("\n");
		buf.append("<td>").append("\n");
		buf.append("&nbsp;").append("\n");
		buf.append("</td>").append("\n");
		buf.append("</tr>").append("\n");
		buf.append("</table>").append("\n");
		return buf.toString();
	}

	public String generateSearchResultsTable(int total_count, String matchText, Vector search_results_vec) {
		StringBuffer buf = new StringBuffer();
		buf.append("<!-- Page content -->").append("\n");
		buf.append("<div class=\"pagecontent\">").append("\n");
		buf.append("<a name=\"evs-content\" id=\"evs-content\" tabindex=\"0\"></a>").append("\n");
		buf.append("<table width=\"700px\" role='presentation'>").append("\n");
		buf.append("<tr>").append("\n");

		buf.append("<table role='presentation'>").append("\n");
		buf.append("<tr>").append("\n");
		buf.append("<td class=\"texttitle-blue\">Result for:</td>").append("\n");
		buf.append("<td class=\"texttitle-gray\">cell aging</td>").append("\n");
		buf.append("</tr>").append("\n");
		buf.append("</table>").append("\n");

		buf.append("</tr>").append("\n");
		buf.append("<tr>").append("\n");
		buf.append("<td><hr></td>").append("\n");
		buf.append("</tr>").append("\n");

		buf.append("<tr>").append("\n");
		buf.append("<td>").append("\n");
		buf.append("<b>Results 1-50 of&nbsp;" + total_count + " for: " + matchText + "</b>").append("\n");
		buf.append("</td>").append("\n");
		buf.append("</tr>").append("\n");
		buf.append("<tr>").append("\n");
		buf.append("<td class=\"textbody\">").append("\n");

		buf.append("<table class=\"datatable_960\" summary=\"\" cellpadding=\"3\" cellspacing=\"0\" border=\"0\" width=\"100%\">").append("\n");
		buf.append("<th class=\"dataTableHeader\" scope=\"col\" align=\"left\">Concept</th>").append("\n");

		if (search_results_vec != null) {
			String line = (String) search_results_vec.elementAt(0);
			Vector u0 = gov.nih.nci.evs.restapi.util.StringUtils.parseData(line, '|');
			if (u0.size() > 2) {
				buf.append("<th class=\"dataTableHeader\" scope=\"col\" align=\"left\">Semantic Type</th>").append("\n");
			}
		}
		buf.append("</tr>").append("\n");
		if (search_results_vec != null) {
			int lcv = 0;
			for (int i=0; i<search_results_vec.size(); i++) {
				String line = (String) search_results_vec.elementAt(i);
				Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(line, '|');
				String cui = (String) u.elementAt(1);
				String label = (String) u.elementAt(0);
				String semantic_type = "";
				if (u.size() > 2) {
					semantic_type = (String) u.elementAt(2);
				}
				String dataRowColor = "dataRowDark";
				if (!isEven(lcv).equals(Boolean.TRUE)) {
					dataRowColor = "dataRowLight";
				}
				buf.append("<tr class=\"" + dataRowColor + "\">").append("\n");
				buf.append("<td class=\"dataCellText\" width=600>").append("\n");
				buf.append("<a href=\"/" + application_name + "/ConceptReport.jsp?dictionary=NCI Metathesaurus&code=" + cui + "\">" + label + "</a>").append("\n");
				buf.append("</td>").append("\n");
				if (u.size() > 2) {
					buf.append("<td class=\"dataCellText\" width=400>").append("\n");
					buf.append(semantic_type).append("\n");
					buf.append("</td>").append("\n");
			    }
				buf.append("</tr>").append("\n");
			}
		}
		buf.append("</table>").append("\n");
		buf.append("</td>").append("\n");
		buf.append("</tr>").append("\n");
		buf.append("</table>").append("\n");
		buf.append("").append("\n");
		return buf.toString();
	}

	public String generateAdvancedSearchForm(Vector source_vec) {
		StringBuffer buf = new StringBuffer();
		buf.append("<div class=\"pagecontent\">").append("\n");
		buf.append("<a name=\"evs-content\" id=\"evs-content\" tabindex=\"0\"></a>").append("\n");
		buf.append("<table role='presentation'>").append("\n");
		buf.append("<tr>").append("\n");
		buf.append("<td class=\"texttitle-blue\">Advanced Search</td>").append("\n");
		buf.append("</tr>").append("\n");
		buf.append("<tr class=\"textbody\"><td>").append("\n");
		buf.append("<form id=\"advancedSearchForm\" name=\"advancedSearchForm\" method=\"post\" action=\"/ncimbrowser/pages/advanced_search.jsf\" class=\"search-form\" enctype=\"application/x-www-form-urlencoded\">").append("\n");
		buf.append("<input type=\"hidden\" name=\"advancedSearchForm\" value=\"advancedSearchForm\" />").append("\n");
		buf.append("<table role='presentation'>").append("\n");
		buf.append("<tr><td>").append("\n");
		buf.append("<input CLASS=\"searchbox-input\" id=\"adv_matchText\" name=\"adv_matchText\" value=\"\" aria-label=\"adv_matchText\" onFocus=\"active=true\"").append("\n");
		buf.append("onBlur=\"active=false\"  onkeypress=\"return submitEnter('advancedSearchForm:adv_search',event)\"  />").append("\n");
		buf.append("<input id=\"advancedSearchForm:adv_search\" type=\"image\" src=\"/ncimbrowser/images/search.gif\" name=\"advancedSearchForm:adv_search\" accesskey=\"13\" alt=\"Search\" onclick=\"javascript:cursor_wait();\" />").append("\n");
		buf.append("</td></tr>").append("\n");
		buf.append("<tr><td>").append("\n");
		buf.append("<table border=\"0\" cellspacing=\"0\" cellpadding=\"0\" role='presentation'>").append("\n");
		buf.append("<tr valign=\"top\" align=\"left\"><td align=\"left\" class=\"textbody\">").append("\n");
		buf.append("<input type=\"radio\" name=\"adv_search_algorithm\" id=\"adv_search_algorithm3\" value=\"contains\" alt=\"Contains\" checked onclick=\"refresh_algorithm()\"; /><label for=\"adv_search_algorithm3\">Contains</label>").append("\n");
		buf.append("<input type=\"radio\" name=\"adv_search_algorithm\" id=\"adv_search_algorithm1\" value=\"exactMatch\" alt=\"Exact Match\"  onclick=\"refresh_algorithm()\"; /><label for=\"adv_search_algorithm1\">Exact Match&nbsp;</label>").append("\n");
		buf.append("<input type=\"radio\" name=\"adv_search_algorithm\" id=\"adv_search_algorithm2\" value=\"startsWith\" alt=\"Begins With\"  onclick=\"refresh_algorithm()\"; /><label for=\"adv_search_algorithm2\">Begins With&nbsp;</label>").append("\n");
		buf.append("<input type=\"radio\" name=\"adv_search_algorithm\" id=\"adv_search_algorithm4\" value=\"lucene\" alt=\"Lucene\"  tabindex=\"0\" onclick=\"refresh_algorithm()\"; ><label for=\"adv_search_algorithm4\">Lucene</label>").append("\n");
		buf.append("</td></tr>").append("\n");
		buf.append("</table>").append("\n");
		buf.append("</td></tr>").append("\n");
		buf.append("<tr><td>").append("\n");
		buf.append("<label id=\"advancedSearchForm:rel_search_source_Label\" for=\"advancedSearchForm:adv_search_source\" class=\"textbody\">").append("\n");
		buf.append("Source").append("\n");
		buf.append("<select id=\"adv_search_source\" name=\"adv_search_source\" size=\"1\">").append("\n");
		buf.append("<option value=\"ALL\" selected>ALL</option>").append("\n");
		for (int i=0; i<source_vec.size(); i++) {
			String source = (String) source_vec.elementAt(i);
		    buf.append("<option value=\"" + source + "\">" + source + "</option>").append("\n");
		}
		buf.append("</select>").append("\n");
		buf.append("</label>").append("\n");
		buf.append("</td></tr>").append("\n");
		buf.append("<tr><td>").append("\n");
		buf.append("&nbsp;&nbsp;").append("\n");
		buf.append("</td></tr>").append("\n");
		buf.append("<tr valign=\"top\" align=\"left\"><td align=\"left\" class=\"textbody\">").append("\n");
		buf.append("Concepts with this value in:").append("\n");
		buf.append("</td></tr>").append("\n");
		buf.append("<tr valign=\"top\" align=\"left\"><td align=\"left\" class=\"textbody\">").append("\n");
		buf.append("<input type=\"radio\" name=\"selectSearchOption\" id=\"selectSearchOption2\" value=\"Name\" alt=\"Name\" checked onclick=\"javascript:refresh()\" /><label for=\"selectSearchOption2\">Name&nbsp;</label>").append("\n");
		buf.append("<input type=\"radio\" name=\"selectSearchOption\" id=\"selectSearchOption1\" value=\"Code\" alt=\"Code\"  onclick=\"refresh_code()\" /><label for=\"selectSearchOption1\">Code&nbsp;</label>").append("\n");
		buf.append("<input type=\"radio\" name=\"selectSearchOption\" id=\"selectSearchOption3\" value=\"Property\" alt=\"Property\"  onclick=\"javascript:refresh()\" /><label for=\"selectSearchOption3\">Property&nbsp;</label>").append("\n");
		buf.append("<input type=\"radio\" name=\"selectSearchOption\" id=\"selectSearchOption4\" value=\"Relationship\" alt=\"Relationship\"  onclick=\"javascript:refresh()\" /><label for=\"selectSearchOption4\">Relationship</label>").append("\n");
		buf.append("</td></tr>").append("\n");
		buf.append("<tr><td>").append("\n");
		buf.append("<table role='presentation'>").append("\n");
		buf.append("<input type=\"hidden\" name=\"selectProperty\" id=\"selectProperty\" value=\"ALL\" />").append("\n");
		buf.append("<input type=\"hidden\" name=\"rel_search_association\" id=\"rel_search_association\" value=\"ALL\" />").append("\n");
		buf.append("<input type=\"hidden\" name=\"rel_search_rela\" id=\"rel_search_rela\" value=\"\" />").append("\n");
		buf.append("</table>").append("\n");
		buf.append("</td></tr>").append("\n");
		buf.append("</table>").append("\n");
		buf.append("<input type=\"hidden\" name=\"referer\" id=\"adv_search_referer\" value=\"https%3A%2F%2Fncim65.nci.nih.gov%2Fncimbrowser%2F\" />").append("\n");
		buf.append("<input type=\"hidden\" name=\"adv_search_type\" id=\"adv_search_type\" value=\"Name\" />").append("\n");
		buf.append("<input type=\"hidden\" name=\"javax.faces.ViewState\" />").append("\n");
		buf.append("</form>").append("\n");
		buf.append("</td></tr>").append("\n");
		buf.append("</table>").append("\n");
		return buf.toString();
	}


    public String getViewInSourceHierarchyLink(String cui, String sab) {
		StringBuffer buf = new StringBuffer();
		buf.append("<a class=\"icon_blue\" href=\"#\" onclick=\"javascript:window.open('/" + this.application_name + "/redirect?action=tree&dictionary=NCI Metathesaurus&code=" + cui + "&sab=" + sab + "&type=hierarchy',").append("\n");
		buf.append("'_blank','top=100, left=100, height=740, width=780, status=no, menubar=no, resizable=yes, scrollbars=yes, toolbar=no, location=no, directories=no');\">").append("\n");
		buf.append("<img src=\"/" + this.application_name + "/images/visualize.gif\" width=\"12px\" height=\"12px\" title=\"View In " + sab + " Hierarchy\" alt=\"View In " + sab + " Hierarchy\" border=\"0\"/>").append("\n");
		buf.append("</a>").append("\n");
		return buf.toString();
	}

    public String getCodeHyperlink(String code, String sab) {
		StringBuffer buf = new StringBuffer();
	    buf.append("			  <a href=\"#\" onclick=\"javascript:window.open('/" + this.application_name + "/redirect?action=ncitdetails&sab=" + sab + "&code=" + code + "',").append("\n");
	    buf.append("			  '_blank','top=100, left=100, height=740, width=780, status=no, menubar=no, resizable=yes, scrollbars=yes, toolbar=no, location=no, directories=no');\">").append("\n");
	    buf.append(code);
	    buf.append("			  </a>").append("\n");
		return buf.toString();
	}

	public static void main(String[] args) {

		 String filename = args[0];
		 Vector v = Utils.readFile(filename);
		 String t = vector2String(v);
		 Vector w = new Vector();
		 w.add(t);
		 Utils.saveToFile("test_" + filename, w);
         /*
		 Vector w = new Vector();
		 w.add(test());
		 Utils.saveToFile("test_" + filename, w);
		 */
		 String s = "Cell aging";
		 //s = encode(s);
		 //System.out.println(s);
	 }

 }