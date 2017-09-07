package gov.nih.nci.evs.restapi.util;

import gov.nih.nci.evs.restapi.bean.*;

import java.io.*;
import java.util.*;
import java.net.*;

public class ExportUtils {
	private OWLSPARQLUtils owlSPARQLUtils;

// Default constructor
	public ExportUtils() {
	}

// Constructor
	public ExportUtils(OWLSPARQLUtils owlSPARQLUtils) {
		this.owlSPARQLUtils = owlSPARQLUtils;
	}

	public OWLSPARQLUtils getOwlSPARQLUtils() {
		return this.owlSPARQLUtils;
	}

    public RelatedConcepts buildRelatedConcepts(String named_graph, String code) {
		ParserUtils parser = new ParserUtils();
        Vector superclass_vec = owlSPARQLUtils.getSuperclassesByCode(named_graph, code);
        HashMap superclasses_hmap = parser.parseSuperclasses(superclass_vec);
        List superconcepts = new ArrayList();
        Iterator it = superclasses_hmap.keySet().iterator();
        while (it.hasNext()) {
			String cd = (String) it.next();
			Vector w = (Vector) superclasses_hmap.get(cd);
			String name = null;
			if (w != null) {
				name = (String) w.elementAt(0);
			}
			superconcepts.add(new Superconcept(cd, name));
		}

        Vector subclass_vec = owlSPARQLUtils.getSubclassesByCode(named_graph, code);
        HashMap subclasses_hmap = parser.parseSubclasses(subclass_vec);
        List subconcepts = new ArrayList();
        it = subclasses_hmap.keySet().iterator();
        while (it.hasNext()) {
			String cd = (String) it.next();
			Vector w = (Vector) subclasses_hmap.get(cd);
			String name = null;
			if (w != null) {
				name = (String) w.elementAt(0);
			}
			subconcepts.add(new Subconcept(cd, name));
		}

//		buf.append("SELECT distinct ?p_label ?y_label ?y_code ").append("\n");
        Vector roles_vec = owlSPARQLUtils.getRolesByCode(named_graph, code);
        roles_vec = parser.toDelimited(roles_vec, 3, '|');
        List roles = new ArrayList();
        for (int i=0; i<roles_vec.size(); i++) {
			String line = (String) roles_vec.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String rel = (String) u.elementAt(0);
			String concept_code = (String) u.elementAt(2);
			String concept_label = (String) u.elementAt(1);
			roles.add(new Role(rel, concept_code, concept_label));
		}

//		buf.append("SELECT distinct ?x_label ?x_code ?p_label ").append("\n");
        Vector inv_roles_vec = owlSPARQLUtils.getInverseRolesByCode(named_graph, code);
        inv_roles_vec = parser.toDelimited(inv_roles_vec, 3, '|');
        List inverseRoles = new ArrayList();
        for (int i=0; i<inv_roles_vec.size(); i++) {
			String line = (String) inv_roles_vec.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String rel = (String) u.elementAt(2);
			String concept_code = (String) u.elementAt(1);
			String concept_label = (String) u.elementAt(0);
			inverseRoles.add(new InverseRole(rel, concept_code, concept_label));
		}

//		buf.append("SELECT ?y_label ?z_label ?z_code").append("\n");
        Vector associations_vec = owlSPARQLUtils.getAssociationsByCode(named_graph, code);
        associations_vec = parser.toDelimited(associations_vec, 3, '|');
        List associations = new ArrayList();
        for (int i=0; i<associations_vec.size(); i++) {
			String line = (String) associations_vec.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String rel = (String) u.elementAt(0);
			String concept_code = (String) u.elementAt(2);
			String concept_label = (String) u.elementAt(1);
			associations.add(new Association(rel, concept_code, concept_label));
		}

//		buf.append("SELECT ?x_label ?x_code ?y_label").append("\n");
        Vector inv_associations_vec = owlSPARQLUtils.getInverseAssociationsByCode(named_graph, code);
        inv_associations_vec = parser.toDelimited(inv_associations_vec, 3, '|');
        List inverseAssoications = new ArrayList();
        for (int i=0; i<inv_associations_vec.size(); i++) {
			String line = (String) inv_associations_vec.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String rel = (String) u.elementAt(2);
			String concept_code = (String) u.elementAt(1);
			String concept_label = (String) u.elementAt(0);
			inverseAssoications.add(new InverseAssociation(rel, concept_code, concept_label));
		}
		return new RelatedConcepts(superconcepts, subconcepts, associations, inverseAssoications, roles, inverseRoles);
	}


    public ConceptDetails buildConceptDetails(String named_graph, String code,
        List mainMenuAncestors,
        Boolean isMainType,
        Boolean isSubtype,
        Boolean isDiseaseStage,
        Boolean isDiseaseGrade,
        Boolean isDisease
        ) {

		Vector label_vec = owlSPARQLUtils.getLabelByCode(named_graph, code);
		Vector property_vec = owlSPARQLUtils.getPropertiesByCode(named_graph, code);
		Vector property_qualifier_vec = owlSPARQLUtils.getPropertyQualifiersByCode(named_graph, code);
		Vector synonym_vec = owlSPARQLUtils.getSynonyms(named_graph, code);
		Vector superconcept_vec = owlSPARQLUtils.getSuperclassesByCode(named_graph, code);
		Vector subconcept_vec = owlSPARQLUtils.getSubclassesByCode(named_graph, code);


/*
	public ConceptDetails(
		Vector label_vec,
		Vector property_vec,
		Vector property_qualifier_vec,
		Vector synonym_vec,
		Vector superclass_vec,
		Vector subclass_vec,
		List mainMenuAncestors,
	    Boolean isMainType,
	    Boolean isSubtype,
	    Boolean isDiseaseStage,
	    Boolean isDiseaseGrade
*/

		return new ConceptDetails(
				label_vec,
				property_vec,
				property_qualifier_vec,
				synonym_vec,
				superconcept_vec,
		        subconcept_vec,
		        mainMenuAncestors,
		        isMainType,
		        isSubtype,
		        isDiseaseStage,
		        isDiseaseGrade,
		        isDisease
		        );
	}

    public Paths buildPaths(String named_graph, String code, int direction) {
        TreeBuilder treeBuilder = new TreeBuilder(owlSPARQLUtils);
		Vector u = treeBuilder.generateTreeData(code, direction);
		Vector v = new Vector();
		for (int i=0; i<u.size(); i++) {
			String t = (String) u.elementAt(i);
			if (t.indexOf("@") == -1) {
				v.add(t);
			}
		}
		PathFinder pathFinder = new PathFinder(v);
		Paths paths = pathFinder.findPaths();
		return paths;
	}

}
