package gov.nih.nci.evs.restapi.util;
import gov.nih.nci.evs.restapi.model.*;

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

public class ValueSetQA {
	HTTPUtils httpUtils = null;
	String serviceUrl = null;
	String username = null;
	String password = null;
	String namedGraph = null;
	String prefixes = null;
	OWLSPARQLUtils owlSPARQLUtils = null;
	gov.nih.nci.evs.restapi.util.ValueSetConditionValidator validator = null;
	boolean checkOutBoundConceptInSubset = false;
	ValueSetReportGenerator generator = null;

	public ValueSetQA(String serviceUrl, String namedGraph, String username, String password) {
		this.serviceUrl = serviceUrl;
		this.namedGraph = namedGraph;
		this.username = username;
		this.password = password;
		this.httpUtils = new HTTPUtils(serviceUrl,username, password);

		this.owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, username, password);
		this.owlSPARQLUtils.set_named_graph(namedGraph);

	}

	public void setCheckOutBoundConceptInSubset(boolean bool) {
		checkOutBoundConceptInSubset = bool;
	}

	public void setValueSetConditionValidator(gov.nih.nci.evs.restapi.util.ValueSetConditionValidator validator) {
		this.validator = validator;
	}

    public String getQuery(String query_file) {
	    return owlSPARQLUtils.getQuery(query_file);
	}

	public Vector execute(String query) {
		return owlSPARQLUtils.execute(query);
	}

	public Vector getConceptsWithPropertyAndQualifiersMatching(String named_graph, String code, String propertyLabel,
	              Vector qualifierCodes, Vector qualifierValues) {
	    return owlSPARQLUtils.getConceptsWithPropertyAndQualifiersMatching(named_graph, code, propertyLabel,
	              qualifierCodes, qualifierValues);
	}

	public void run(Vector conditions) {
		gov.nih.nci.evs.restapi.util.ValueSetConditionValidator validator = new gov.nih.nci.evs.restapi.util.ValueSetConditionValidator(
			  this.serviceUrl, this.namedGraph,this. username, this.password);
        validator.setConditions(conditions);
		String headerConceptCode = validator.getHeaderConceptCode();
		if (headerConceptCode == null) {
			System.out.println("WARNING: Value set header concept not set -- program abort.");
			System.exit(1);
		}
		System.out.println("Instantiatng valueSetQA ...");
		setValueSetConditionValidator(validator);
        System.out.println("Instantiatng ValueSetReportGenerator ...");
	    generator = new ValueSetReportGenerator(serviceUrl, namedGraph, username, password, validator);
        generator.set_condition_data(validator.getConditionData());
        generator.set_conditions(validator.getConditions());
        generator.setCheckOutBoundConceptInSubset(this.checkOutBoundConceptInSubset);
        System.out.println("Generating QA report, please wait...");
        generator.generate();
	}

	public Vector getWarnings() {
		return generator.getWarnings();
	}

	public Vector getMissings() {
		return generator.getMissings();
	}

}
