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

public class SPARQLUtilsClient {
    String serviceUrl = null;
    String sparql_endpoint = null;
    HashMap OWLSPARQLUtilsMap = null;
    HashMap codingSchemeNameAndVersion2NamedGraphMap = null;
    HashMap namedGraph2codingSchemeNameAndVersionMap = null;

    public SPARQLUtilsClient() {

    }

    public SPARQLUtilsClient(String serviceUrl) {
		this.serviceUrl = serviceUrl;
		initialize();
    }

	public void initialize() {
		String[] sparql_endpoints = new String[1];
		sparql_endpoint = serviceUrl + "?query=";
		sparql_endpoints[0] = sparql_endpoint;
        OWLSPARQLUtilsMap = OWLSPARQLUtilsExt.getCodingSchemeNameVersion2OWLSPARQLUtilsMap(sparql_endpoints);
        Vector v = OWLSPARQLUtilsExt.getSupportedCodingSchemesAndVersions(OWLSPARQLUtilsMap);
        //Utils.dumpVector("Supported coding schemes", v);
        codingSchemeNameAndVersion2NamedGraphMap = new HashMap();
        namedGraph2codingSchemeNameAndVersionMap = new HashMap();
        for (int k=0; k<v.size(); k++) {
			String key = (String) v.elementAt(k);
			gov.nih.nci.evs.restapi.util.OWLSPARQLUtils owlSPARQLUtils = (gov.nih.nci.evs.restapi.util.OWLSPARQLUtils) OWLSPARQLUtilsMap.get(key);
			if (owlSPARQLUtils != null) {
				namedGraph2codingSchemeNameAndVersionMap.put(owlSPARQLUtils.get_named_graph(), key);
				codingSchemeNameAndVersion2NamedGraphMap.put(key, owlSPARQLUtils.get_named_graph());
				/*
				//System.out.println(owlSPARQLUtils.get_named_graph() + " --> " + key);
				//System.out.println(key + " --> " + owlSPARQLUtils.get_named_graph());
				Vector w = owlSPARQLUtils.getTripleCount(owlSPARQLUtils.get_named_graph());
				String count = new ParserUtils().getValue((String) w.elementAt(0));
				System.out.println(key + " triple count: " + count);
				Vector named_graphs = owlSPARQLUtils.getNamedGraphs();
				Utils.dumpVector("named_graphs", named_graphs);
				*/
			}
		}
	}

	public Vector execute(String named_graph, String query_file) {
		String key = (String) codingSchemeNameAndVersion2NamedGraphMap.get(named_graph);
		gov.nih.nci.evs.restapi.util.OWLSPARQLUtils owlSPARQLUtils = (gov.nih.nci.evs.restapi.util.OWLSPARQLUtils) OWLSPARQLUtilsMap.get(key);
		HTTPUtils httpUtils = new HTTPUtils(sparql_endpoint, null, null);
		String query = httpUtils.loadQuery(query_file, false);
		//System.out.println(query);
	    return owlSPARQLUtils.execute(query_file);
	}

	public String getNamedGraphByCodingSchemeAndVersion(String codingScheme, String version) {
        return (String) codingSchemeNameAndVersion2NamedGraphMap.get(codingScheme + "|" + version);
	}

	public String getCodingSchemeAndVersionByNamedGraph(String namedGraph) {
        return (String) namedGraph2codingSchemeNameAndVersionMap.get(namedGraph);
	}

	public Vector getOntologyInfo(String named_graph) {
		String key = (String) namedGraph2codingSchemeNameAndVersionMap.get(named_graph);
		OWLSPARQLUtils owlSPARQLUtils = (OWLSPARQLUtils) OWLSPARQLUtilsMap.get(key);
		return owlSPARQLUtils.get_ontology_info(named_graph);
	}


	public Vector getNamedGraph() {
		OWLSPARQLUtils owlSPARQLUtils = new OWLSPARQLUtils(sparql_endpoint, null, null);
		return owlSPARQLUtils.getNamedGraph();
	}


	public Vector getOntologyInfo() {
		OWLSPARQLUtils owlSPARQLUtils = new OWLSPARQLUtils(sparql_endpoint, null, null);
		return owlSPARQLUtils.get_ontology_info();
	}


	public Vector getLabelByCode(String named_graph, String code) {
		String key = (String) namedGraph2codingSchemeNameAndVersionMap.get(named_graph);
		OWLSPARQLUtils owlSPARQLUtils = (OWLSPARQLUtils) OWLSPARQLUtilsMap.get(key);
		return owlSPARQLUtils.getLabelByCode(named_graph, code);
	}


	public Vector getInverseRolesByCode(String named_graph, String code) {
		String key = (String) namedGraph2codingSchemeNameAndVersionMap.get(named_graph);
		OWLSPARQLUtils owlSPARQLUtils = (OWLSPARQLUtils) OWLSPARQLUtilsMap.get(key);
		return owlSPARQLUtils.getInverseRolesByCode(named_graph, code);
	}


	public Vector getCodeAndLabel(String named_graph) {
		String key = (String) namedGraph2codingSchemeNameAndVersionMap.get(named_graph);
		OWLSPARQLUtils owlSPARQLUtils = (OWLSPARQLUtils) OWLSPARQLUtilsMap.get(key);
		return owlSPARQLUtils.getCodeAndLabel(named_graph);
	}


	public Vector getClassCounts() {
		OWLSPARQLUtils owlSPARQLUtils = new OWLSPARQLUtils(sparql_endpoint, null, null);
		return owlSPARQLUtils.getClassCounts();
	}


	public Vector getClassCount(String named_graph) {
		String key = (String) namedGraph2codingSchemeNameAndVersionMap.get(named_graph);
		OWLSPARQLUtils owlSPARQLUtils = (OWLSPARQLUtils) OWLSPARQLUtilsMap.get(key);
		return owlSPARQLUtils.getClassCount(named_graph);
	}


	public Vector getTripleCount(String named_graph) {
		String key = (String) namedGraph2codingSchemeNameAndVersionMap.get(named_graph);
		OWLSPARQLUtils owlSPARQLUtils = (OWLSPARQLUtils) OWLSPARQLUtilsMap.get(key);
		return owlSPARQLUtils.getTripleCount(named_graph);
	}


	public Vector getSuperclassesByCode(String named_graph, String code) {
		String key = (String) namedGraph2codingSchemeNameAndVersionMap.get(named_graph);
		OWLSPARQLUtils owlSPARQLUtils = (OWLSPARQLUtils) OWLSPARQLUtilsMap.get(key);
		return owlSPARQLUtils.getSuperclassesByCode(named_graph, code);
	}


	public Vector getSubclassesByCode(String named_graph, String code) {
		String key = (String) namedGraph2codingSchemeNameAndVersionMap.get(named_graph);
		OWLSPARQLUtils owlSPARQLUtils = (OWLSPARQLUtils) OWLSPARQLUtilsMap.get(key);
		return owlSPARQLUtils.getSubclassesByCode(named_graph, code);
	}


	public Vector getRolesByCode(String named_graph, String code) {
		String key = (String) namedGraph2codingSchemeNameAndVersionMap.get(named_graph);
		OWLSPARQLUtils owlSPARQLUtils = (OWLSPARQLUtils) OWLSPARQLUtilsMap.get(key);
		return owlSPARQLUtils.getRolesByCode(named_graph, code);
	}


	public Vector getPropertiesByCode(String named_graph, String code) {
		String key = (String) namedGraph2codingSchemeNameAndVersionMap.get(named_graph);
		OWLSPARQLUtils owlSPARQLUtils = (OWLSPARQLUtils) OWLSPARQLUtilsMap.get(key);
		Vector v = owlSPARQLUtils.getPropertiesByCode(named_graph, code, false);
		v = ParserUtils.formatOutput(v);
		return ParserUtils.excludePropertyType(v, "#A|#R");
	}


	public Vector getOntologyVersionInfo(String named_graph) {
		String key = (String) namedGraph2codingSchemeNameAndVersionMap.get(named_graph);
		OWLSPARQLUtils owlSPARQLUtils = (OWLSPARQLUtils) OWLSPARQLUtilsMap.get(key);
		return owlSPARQLUtils.getOntologyVersionInfo(named_graph);
	}


	public Vector getSynonyms(String named_graph, String code) {
		String key = (String) namedGraph2codingSchemeNameAndVersionMap.get(named_graph);
		OWLSPARQLUtils owlSPARQLUtils = (OWLSPARQLUtils) OWLSPARQLUtilsMap.get(key);
		return owlSPARQLUtils.getSynonyms(named_graph, code);
	}


	public Vector getAnnotationProperties(String named_graph) {
		String key = (String) namedGraph2codingSchemeNameAndVersionMap.get(named_graph);
		OWLSPARQLUtils owlSPARQLUtils = (OWLSPARQLUtils) OWLSPARQLUtilsMap.get(key);
		return owlSPARQLUtils.getAnnotationProperties(named_graph);
	}


	public Vector getObjectProperties(String named_graph) {
		String key = (String) namedGraph2codingSchemeNameAndVersionMap.get(named_graph);
		OWLSPARQLUtils owlSPARQLUtils = (OWLSPARQLUtils) OWLSPARQLUtilsMap.get(key);
		return owlSPARQLUtils.getObjectProperties(named_graph);
	}


	public Vector getPropertyQualifiersByCode(String named_graph, String code) {
		String key = (String) namedGraph2codingSchemeNameAndVersionMap.get(named_graph);
		OWLSPARQLUtils owlSPARQLUtils = (OWLSPARQLUtils) OWLSPARQLUtilsMap.get(key);
		return owlSPARQLUtils.getPropertyQualifiersByCode(named_graph, code);
	}


	public Vector getInboundRolesByCode(String named_graph, String code) {
		String key = (String) namedGraph2codingSchemeNameAndVersionMap.get(named_graph);
		OWLSPARQLUtils owlSPARQLUtils = (OWLSPARQLUtils) OWLSPARQLUtilsMap.get(key);
		return owlSPARQLUtils.getInboundRolesByCode(named_graph, code);
	}


	public Vector getOutboundRolesByCode(String named_graph, String code) {
		String key = (String) namedGraph2codingSchemeNameAndVersionMap.get(named_graph);
		OWLSPARQLUtils owlSPARQLUtils = (OWLSPARQLUtils) OWLSPARQLUtilsMap.get(key);
		return owlSPARQLUtils.getOutboundRolesByCode(named_graph, code);
	}


	public Vector getRoleRelationships(String named_graph) {
		String key = (String) namedGraph2codingSchemeNameAndVersionMap.get(named_graph);
		OWLSPARQLUtils owlSPARQLUtils = (OWLSPARQLUtils) OWLSPARQLUtilsMap.get(key);
		return owlSPARQLUtils.getRoleRelationships(named_graph);
	}


	public Vector getInverseAssociationsByCode(String named_graph, String code) {
		String key = (String) namedGraph2codingSchemeNameAndVersionMap.get(named_graph);
		OWLSPARQLUtils owlSPARQLUtils = (OWLSPARQLUtils) OWLSPARQLUtilsMap.get(key);
		return owlSPARQLUtils.getInverseAssociationsByCode(named_graph, code);
	}


	public Vector getAssociationsByCode(String named_graph, String code) {
		String key = (String) namedGraph2codingSchemeNameAndVersionMap.get(named_graph);
		OWLSPARQLUtils owlSPARQLUtils = (OWLSPARQLUtils) OWLSPARQLUtilsMap.get(key);
		return owlSPARQLUtils.getAssociationsByCode(named_graph, code);
	}


	public Vector getDiseaseIsStageSourceCodes(String named_graph) {
		String key = (String) namedGraph2codingSchemeNameAndVersionMap.get(named_graph);
		OWLSPARQLUtils owlSPARQLUtils = (OWLSPARQLUtils) OWLSPARQLUtilsMap.get(key);
		return owlSPARQLUtils.getDiseaseIsStageSourceCodes(named_graph);
	}


	public Vector getDiseaseIsGradeSourceCodes(String named_graph) {
		String key = (String) namedGraph2codingSchemeNameAndVersionMap.get(named_graph);
		OWLSPARQLUtils owlSPARQLUtils = (OWLSPARQLUtils) OWLSPARQLUtilsMap.get(key);
		return owlSPARQLUtils.getDiseaseIsGradeSourceCodes(named_graph);
	}


	public Vector getAssociationSourceCodes(String named_graph, String associationName) {
		String key = (String) namedGraph2codingSchemeNameAndVersionMap.get(named_graph);
		OWLSPARQLUtils owlSPARQLUtils = (OWLSPARQLUtils) OWLSPARQLUtilsMap.get(key);
		return owlSPARQLUtils.getAssociationSourceCodes(named_graph, associationName);
	}


	public Vector getDisjointWithByCode(String named_graph, String code) {
		String key = (String) namedGraph2codingSchemeNameAndVersionMap.get(named_graph);
		OWLSPARQLUtils owlSPARQLUtils = (OWLSPARQLUtils) OWLSPARQLUtilsMap.get(key);
		return owlSPARQLUtils.getDisjointWithByCode(named_graph, code);
	}


	public Vector getObjectPropertiesDomainRange(String named_graph) {
		String key = (String) namedGraph2codingSchemeNameAndVersionMap.get(named_graph);
		OWLSPARQLUtils owlSPARQLUtils = (OWLSPARQLUtils) OWLSPARQLUtilsMap.get(key);
		return owlSPARQLUtils.getObjectPropertiesDomainRange(named_graph);
	}


	public Vector getObjectValuedAnnotationProperties(String named_graph) {
		String key = (String) namedGraph2codingSchemeNameAndVersionMap.get(named_graph);
		OWLSPARQLUtils owlSPARQLUtils = (OWLSPARQLUtils) OWLSPARQLUtilsMap.get(key);
		return owlSPARQLUtils.getObjectValuedAnnotationProperties(named_graph);
	}


	public Vector getStringValuedAnnotationProperties(String named_graph) {
		String key = (String) namedGraph2codingSchemeNameAndVersionMap.get(named_graph);
		OWLSPARQLUtils owlSPARQLUtils = (OWLSPARQLUtils) OWLSPARQLUtilsMap.get(key);
		return owlSPARQLUtils.getStringValuedAnnotationProperties(named_graph);
	}


	public Vector getSupportedProperties(String named_graph) {
		String key = (String) namedGraph2codingSchemeNameAndVersionMap.get(named_graph);
		OWLSPARQLUtils owlSPARQLUtils = (OWLSPARQLUtils) OWLSPARQLUtilsMap.get(key);
		return owlSPARQLUtils.getSupportedProperties(named_graph);
	}


	public Vector getSupportedAssociations(String named_graph) {
		String key = (String) namedGraph2codingSchemeNameAndVersionMap.get(named_graph);
		OWLSPARQLUtils owlSPARQLUtils = (OWLSPARQLUtils) OWLSPARQLUtilsMap.get(key);
		return owlSPARQLUtils.getSupportedAssociations(named_graph);
	}


	public Vector getSupportedRoles(String named_graph) {
		String key = (String) namedGraph2codingSchemeNameAndVersionMap.get(named_graph);
		OWLSPARQLUtils owlSPARQLUtils = (OWLSPARQLUtils) OWLSPARQLUtilsMap.get(key);
		return owlSPARQLUtils.getSupportedRoles(named_graph);
	}


	public Vector getHierarchicalRelationships(String named_graph) {
		String key = (String) namedGraph2codingSchemeNameAndVersionMap.get(named_graph);
		OWLSPARQLUtils owlSPARQLUtils = (OWLSPARQLUtils) OWLSPARQLUtilsMap.get(key);
		return owlSPARQLUtils.getHierarchicalRelationships(named_graph);
	}


	public Vector getConceptsInSubset(String named_graph, String code) {
		String key = (String) namedGraph2codingSchemeNameAndVersionMap.get(named_graph);
		OWLSPARQLUtils owlSPARQLUtils = (OWLSPARQLUtils) OWLSPARQLUtilsMap.get(key);
		return owlSPARQLUtils.getConceptsInSubset(named_graph, code);
	}


	public Vector getAvailableOntologies() {
		OWLSPARQLUtils owlSPARQLUtils = new OWLSPARQLUtils(sparql_endpoint, null, null);
		return owlSPARQLUtils.getAvailableOntologies();
	}


	public Vector getOntologyVersion(String named_graph) {
		String key = (String) namedGraph2codingSchemeNameAndVersionMap.get(named_graph);
		OWLSPARQLUtils owlSPARQLUtils = (OWLSPARQLUtils) OWLSPARQLUtilsMap.get(key);
		return owlSPARQLUtils.getOntologyVersion(named_graph);
	}


	public Vector getOntology(String named_graph) {
		String key = (String) namedGraph2codingSchemeNameAndVersionMap.get(named_graph);
		OWLSPARQLUtils owlSPARQLUtils = (OWLSPARQLUtils) OWLSPARQLUtilsMap.get(key);
		return owlSPARQLUtils.getOntology(named_graph);
	}


	public Vector getNamedGraphs() {
		OWLSPARQLUtils owlSPARQLUtils = new OWLSPARQLUtils(sparql_endpoint, null, null);
		return owlSPARQLUtils.getNamedGraphs();
	}

	public Vector getTree(String named_graph, String code) {
		String key = (String) namedGraph2codingSchemeNameAndVersionMap.get(named_graph);
		OWLSPARQLUtils owlSPARQLUtils = (OWLSPARQLUtils) OWLSPARQLUtilsMap.get(key);
		return owlSPARQLUtils.getTree(named_graph, code);
	}


	public Vector getTree(String named_graph, String code, int maxLevel) {
		String key = (String) namedGraph2codingSchemeNameAndVersionMap.get(named_graph);
		OWLSPARQLUtils owlSPARQLUtils = (OWLSPARQLUtils) OWLSPARQLUtilsMap.get(key);
		return owlSPARQLUtils.getTree(named_graph, code, maxLevel);
	}


	public static void main(String[] args) {
		String serviceUrl = args[0];
		SPARQLUtilsClient testCodeGen = new SPARQLUtilsClient(serviceUrl);
		String codingScheme = "NCI_Thesaurus";
		String version = "17.07d";
		String namedGraph = testCodeGen.getNamedGraphByCodingSchemeAndVersion(codingScheme, version);
		String header_concept_code = "C54456";
		Vector codes = testCodeGen.getConceptsInSubset(namedGraph, header_concept_code);
		for (int i=0; i<codes.size(); i++) {
			String code = (String) codes.elementAt(i);
			System.out.println(code);
		}
	}
}
