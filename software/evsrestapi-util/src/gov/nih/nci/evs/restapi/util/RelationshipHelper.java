package gov.nih.nci.evs.restapi.util;
import gov.nih.nci.evs.restapi.common.*;

import java.io.*;
import java.util.*;

public class RelationshipHelper {
	private String sparql_endpoint = null;
    private String database = "ncit";
    private OWLSPARQLUtils owlSPARQLUtils = null;
    private String prefixes = null;
    private String named_graph = null;
    private String version = null;
    private String serviceUrl = null;

    public static int SUPERCONCEPT_OPTION = 0;
    public static int SUBCONCEPT_OPTION = 1;
    public static int ROLE_OPTION = 2;
    public static int INVERSE_ROLE_OPTION = 3;
    public static int ASSOCIATION_OPTION = 4;
    public static int INVERSE_ASSOCIATION_OPTION = 5;

    public RelationshipHelper(String serviceUrl) {
		this.serviceUrl = serviceUrl;
		this.sparql_endpoint = serviceUrl + "?query=";
		this.owlSPARQLUtils = new OWLSPARQLUtils(sparql_endpoint);
		this.version = MetadataUtils.getLatestVersionOfCodingScheme(serviceUrl, Constants.NCI_THESAURUS);
		this.named_graph = MetadataUtils.getNamedGraphOfCodingScheme(serviceUrl, Constants.NCI_THESAURUS, this.version);
		this.owlSPARQLUtils.set_named_graph(this.named_graph);
		//System.out.println(this.version);
		//System.out.println(this.named_graph);
	}

	public OWLSPARQLUtils getOwlSPARQLUtils() {
		return this.owlSPARQLUtils;
	}

    public List createOptionList(boolean superconcept,
                                 boolean subconcept,
                                 boolean role,
                                 boolean inverse_role,
                                 boolean association,
                                 boolean inverse_association) {
		ArrayList list = new ArrayList();
		list.add(new Boolean(superconcept));
		list.add(new Boolean(subconcept));
		list.add(new Boolean(role));
		list.add(new Boolean(inverse_role));
		list.add(new Boolean(association));
		list.add(new Boolean(inverse_association));
		return list;
    }

    public List getDefaultOptionList() {
		ArrayList list = new ArrayList();
		for (int i=0; i<6; i++) {
			list.add(new Boolean(true));
		}
		return list;
	}

	public boolean checkOption(List options, int index) {
		Boolean bool = (Boolean) options.get(index);
		if (bool.equals(Boolean.TRUE)) return true;
		return false;
	}

	public void setSparqlService(String sparql_endpoint) {
		this.sparql_endpoint = sparql_endpoint;
	}


	public List sortList(List list) {
		if (list == null) return list;
        Vector v = new Vector();
		for (int i=0; i<list.size(); i++) {
			String t = (String) list.get(i);
			v.add(t);
		}
		v = new SortUtils().quickSort(v);
		list = new ArrayList();
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			list.add(t);
		}
        return list;
	}

	public ArrayList sortList(ArrayList list) {
		if (list == null) return list;
        Vector v = new Vector();
		for (int i=0; i<list.size(); i++) {
			String t = (String) list.get(i);
			v.add(t);
		}
		v = new SortUtils().quickSort(v);
		ArrayList new_list = new ArrayList();
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			new_list.add(t);
		}
        return new_list;
	}

    public HashMap getRelationshipHashMap(String code) {
		return getRelationshipHashMap(null, null, code, null, false);
	}

    public HashMap getRelationshipHashMap(String scheme, String version, String code) {
		return getRelationshipHashMap(scheme, version, code, null, false);
	}


    public HashMap getRelationshipHashMap(String scheme, String version, String code, String ns, boolean useNamespace) {
		return getRelationshipHashMap(scheme, version, code, ns, useNamespace, getDefaultOptionList());
	}

    // to be implemented
	public List getRelationshipData(HashMap relMap, String key, List relationship_list) {
		if (relMap == null) return null;
		List list = (ArrayList) relMap.get(key);
		if (list == null) return null;
		List a = new ArrayList();
		for (int i=0; i<list.size(); i++) {
			String t = (String) list.get(i);
			String rel_label = gov.nih.nci.evs.restapi.util.StringUtils.getFieldValue(t, 0);
			String name = gov.nih.nci.evs.restapi.util.StringUtils.getFieldValue(t, 1);
			String code = gov.nih.nci.evs.restapi.util.StringUtils.getFieldValue(t, 2);
			if (relationship_list.contains(rel_label)) {
				a.add(rel_label + "|" + name + "|" + code);
			}
		}
		return a;
	}


    public HashMap getRelationshipHashMap(String scheme, String version, String code, String ns, boolean useNamespace, List options) {
		if (options == null) {
			options = getDefaultOptionList();
		}

		String name = "<NO DESCRIPTION>";
		String retstr = owlSPARQLUtils.getEntityDescriptionByCode(named_graph, code);
		if (retstr != null) {
			name = retstr;
		}

		String entityCodeNamespace = ns;
		if (gov.nih.nci.evs.restapi.util.StringUtils.isNullOrBlank(ns)) {
			entityCodeNamespace = "";
		}

        ArrayList roleList = new ArrayList();
        ArrayList associationList = new ArrayList();

        ArrayList inverse_roleList = new ArrayList();
        ArrayList inverse_associationList = new ArrayList();

        ArrayList superconceptList = new ArrayList();
        ArrayList subconceptList = new ArrayList();

        HashMap map = new HashMap();

        Vector v = null;

        if (checkOption(options, SUPERCONCEPT_OPTION)) {
			v = owlSPARQLUtils.getSuperclassesByCode(code);
			superconceptList = new ArrayList(v);
			new SortUtils().quickSort(superconceptList);
			map.put(Constants.TYPE_SUPERCONCEPT, superconceptList);
		}

        if (checkOption(options, SUBCONCEPT_OPTION)) {
			v = owlSPARQLUtils.getSubclassesByCode(code);
			subconceptList = new ArrayList(v);
			new SortUtils().quickSort(subconceptList);
			map.put(Constants.TYPE_SUBCONCEPT, subconceptList);
		}

		if (checkOption(options, ROLE_OPTION)) {
			v = owlSPARQLUtils.getOutboundRolesByCode(code);
			roleList = new ArrayList(v);
			new SortUtils().quickSort(roleList);
			map.put(Constants.TYPE_ROLE, roleList);
		}

		if (checkOption(options, INVERSE_ROLE_OPTION)) {
			v = owlSPARQLUtils.getInboundRolesByCode(code);
			inverse_roleList = new ArrayList(v);
			new SortUtils().quickSort(inverse_roleList);
			map.put(Constants.TYPE_INVERSE_ROLE, inverse_roleList);
		}

		if (checkOption(options, ASSOCIATION_OPTION)) {
			v = owlSPARQLUtils.getAssociationsByCode(code);
			associationList = new ArrayList(v);
			new SortUtils().quickSort(associationList);
			map.put(Constants.TYPE_ASSOCIATION, associationList);
		}

		if (checkOption(options, INVERSE_ASSOCIATION_OPTION)) {
			v = owlSPARQLUtils.getInverseAssociationsByCode(code);
			inverse_associationList = new ArrayList(v);
			new SortUtils().quickSort(inverse_associationList);
			map.put(Constants.TYPE_INVERSE_ASSOCIATION, inverse_associationList);
		}
		return map;
	}


    public void output(String label, List list) {
		if (list == null) return;
		System.out.println(label);
		for (int i=0; i<list.size(); i++) {
			String t = (String) list.get(i);
			System.out.println(t);
		}
		System.out.println("\n");
	}

    public void dumpRelationshipHashmap(HashMap map) {
		List superconceptList = (List) map.get(Constants.TYPE_SUPERCONCEPT);
		output(Constants.TYPE_SUPERCONCEPT, superconceptList);
		List subconceptList = (List) map.get(Constants.TYPE_SUBCONCEPT);
		output(Constants.TYPE_SUBCONCEPT, subconceptList);
		List roleList = (List) map.get(Constants.TYPE_ROLE);
		output(Constants.TYPE_ROLE, roleList);
		List inverse_roleList = (List) map.get(Constants.TYPE_INVERSE_ROLE);
		output(Constants.TYPE_INVERSE_ROLE, inverse_roleList);
		List associationList = (List) map.get(Constants.TYPE_ASSOCIATION);
		output(Constants.TYPE_ASSOCIATION, associationList);
		List inverse_associationList = (List) map.get(Constants.TYPE_INVERSE_ASSOCIATION);
		output(Constants.TYPE_INVERSE_ASSOCIATION, inverse_associationList);
	}

	public String get_named_graph() {
		return this.named_graph;
	}

	public Vector getSuperclassesByCode(String code) {
		/*
		Vector v = owlSPARQLUtils.getSuperclassesByCode(named_graph, code);
		if (v == null) return null;
		if (v.size() == 0) return new Vector();
		v = new ParserUtils().getResponseValues(v);
		v = new SortUtils().quickSort(v);
		return v;
		*/
		return owlSPARQLUtils.getSuperclassesByCode(code);
	}

	public Vector getSubclassesByCode(String code) {
		/*
		Vector v = owlSPARQLUtils.getSubclassesByCode(named_graph, code);
		if (v == null) return null;
		if (v.size() == 0) return new Vector();
		v = new ParserUtils().getResponseValues(v);
		v = new SortUtils().quickSort(v);
		return v;
		*/
		return owlSPARQLUtils.getSubclassesByCode(code);
	}

	public Vector getOutboundRolesByCode(String code) {
		/*
		Vector v = owlSPARQLUtils.getOutboundRolesByCode(named_graph, code);
		if (v == null) return null;
		if (v.size() == 0) return new Vector();
		v = new ParserUtils().getResponseValues(v);
		v = new SortUtils().quickSort(v);
		return v;
		*/
		return owlSPARQLUtils.getOutboundRolesByCode(code);
	}

	public Vector getInboundRolesByCode(String code) {
		/*
		Vector v = owlSPARQLUtils.getInboundRolesByCode(named_graph, code);
		if (v == null) return null;
		if (v.size() == 0) return new Vector();
		v = new ParserUtils().getResponseValues(v);
		v = new SortUtils().quickSort(v);
		return v;
		*/
		return owlSPARQLUtils.getInboundRolesByCode(code);
	}

	public Vector getAssociationsByCode(String code) {
		/*
		Vector v = owlSPARQLUtils.getAssociationsByCode(named_graph, code);
		if (v == null) return null;
		if (v.size() == 0) return new Vector();
		StringUtils.dumpVector("getAssociationsByCode", v);
		v = new ParserUtils().getResponseValues(v);
		v = new SortUtils().quickSort(v);
		return v;
		*/
		return owlSPARQLUtils.getAssociationsByCode(code);
	}

	public Vector getInverseAssociationsByCode(String code) {
		/*
		Vector v = owlSPARQLUtils.getInverseAssociationsByCode(named_graph, code);
		if (v == null) return null;
		if (v.size() == 0) return new Vector();
		v = new ParserUtils().getResponseValues(v);
		v = new SortUtils().quickSort(v);
		return v;
		*/
		return owlSPARQLUtils.getInverseAssociationsByCode(code);
	}

	public void test(String code) {
		Vector v = getSuperclassesByCode(code);
		StringUtils.dumpVector("getSuperclassesByCode", v);
		v = getSubclassesByCode(code);
		StringUtils.dumpVector("getSubclassesByCode", v);
		v = getOutboundRolesByCode(code);
		StringUtils.dumpVector("getOutboundRolesByCode", v);
		v = getInboundRolesByCode(code);
        StringUtils.dumpVector("getInboundRolesByCode", v);
 		v = getAssociationsByCode(code);
        StringUtils.dumpVector("getAssociationsByCode", v);
 		v = getInverseAssociationsByCode(code);
        StringUtils.dumpVector("getInverseAssociationsByCode", v);
	}

    public static void main(String [] args) {
		/*
		String sparql_endpoint = "http://localhost:8080/jena-fuseki/ncit/query";
		RelationshipHelper relationshipHelper = new RelationshipHelper(sparql_endpoint);
		code = args[0];
		HashMap map = relationshipHelper.getRelationshipHashMap(code);
		relationshipHelper.dumpRelationshipHashmap(map);
		*/
		String serviceUrl = args[0];
		System.out.println(serviceUrl);
		String code = "C16395";
		RelationshipHelper relationshipHelper = new RelationshipHelper(serviceUrl);
		String label = relationshipHelper.getOwlSPARQLUtils().getEntityDescriptionByCode(relationshipHelper.get_named_graph(), code);
		System.out.println(label + " (" + code + ")");
		//relationshipHelper.test(code);

		HashMap map = relationshipHelper.getRelationshipHashMap(code);
		relationshipHelper.dumpRelationshipHashmap(map);


	}

}

