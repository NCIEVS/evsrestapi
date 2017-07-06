package gov.nih.nci.evs.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;



@Service
public class QueryBuilderServiceImpl implements QueryBuilderService {
	
	private static final Logger log = LoggerFactory.getLogger(QueryBuilderServiceImpl.class);
	
	public String contructPrefix(){
		
		String prefix = String.join(System.getProperty("line.separator"), 
			    "PREFIX :<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>",
		        "PREFIX base:<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>",
		        "PREFIX owl:<http://www.w3.org/2002/07/owl#>",
		        "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>",
		        "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>",
		        "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>"
		);

		
		
	    return prefix;
	}
	
	public String constructConceptLabelQuery(String conceptCode, String namedGraph){
		
		StringBuffer query = new StringBuffer();
		query.append("SELECT ?conceptLabel\n");
		query.append("{ GRAPH <" + namedGraph + ">");
		query.append("  { ?concept a owl:Class .\n");
		query.append("    ?concept :NHC0 "+ "\"" + conceptCode + "\" .\n");		
		query.append("    ?concept rdfs:label ?conceptLabel \n");
		query.append("  }\n");
		query.append("}\n");
		return query.toString();
		
	}
	
	
	public String constructPropertyQuery(String conceptCode, String namedGraph){
		
		StringBuffer query = new StringBuffer();
		query.append("SELECT ?property ?propertyCode  ?propertyLabel ?propertyValue\n");
		query.append("{ GRAPH <" + namedGraph + ">");
		query.append("  { ?concept a owl:Class .\n");
		query.append("    ?concept :NHC0 "+ "\"" + conceptCode + "\" .\n");
		query.append("    ?property a owl:AnnotationProperty .\n");
		query.append("    ?property rdfs:label ?propertyLabel .\n");
		query.append("    ?property :NHC0 ?propertyCode .\n");
		query.append("    ?concept ?property ?propertyValue .\n");
		query.append("    ?property rdfs:range ?y_range \n");
		query.append("  }\n");
		query.append("  FILTER  (str(?y_range)!=\"http://www.w3.org/2001/XMLSchema#anyURI\")\n");
		query.append("}\n");

		return query.toString();
	}

	public String constructAxiomQuery(String conceptCode, String namedGraph) {
		StringBuffer query = new StringBuffer();
        query.append("SELECT ?axiom ?axiomProperty  ?axiomValue\n");
		query.append("{ GRAPH <" + namedGraph + ">");
        query.append("    {\n");
        query.append("      ?axiom a owl:Axiom .\n");
        query.append("      ?axiom owl:annotatedSource :" + conceptCode + " .\n");
        query.append("      ?axiom ?axiomProperty ?axiomValue\n");
        query.append("    }\n");
		query.append("}\n");
        query.append("ORDER BY ?axiom\n");
		
		return query.toString();
	}

	public String constructSubconceptQuery(String conceptCode, String namedGraph) {
		StringBuffer query = new StringBuffer();
		query.append("SELECT ?subclass ?subclassLabel ?subclassCode\n");
		query.append("{ GRAPH <" + namedGraph + ">");
		query.append("		{\n");
		query.append("		  {\n");
		query.append("		    {\n");
		query.append("		      ?superclass a owl:Class .\n");
		query.append("		      ?superclass :NHC0 \"" + conceptCode + "\" .\n");
		query.append("		      ?subclass rdfs:subClassOf ?superclass .\n");
		query.append("		      ?subclass a owl:Class .\n");
		query.append("		      ?subclass rdfs:label ?subclassLabel .\n");
		query.append("		      ?subclass :NHC0 ?subclassCode\n");
		query.append("		    }\n");
		query.append("		    FILTER (?superclass != ?subclass)\n");
		query.append("		  }\n");
		query.append("		  UNION\n");
		query.append("		  {\n");
		query.append("		    {\n");
		query.append("		      ?superclass a owl:Class .\n");
		query.append("		      ?superclass :NHC0 \"" + conceptCode + "\" .\n");
		query.append("		      ?equiv_concept owl:intersectionOf ?list .\n");
		query.append("		      ?list rdf:rest*/rdf:first ?superclass .\n");
		query.append("		      ?subclass owl:equivalentClass ?equiv_concept .\n");
		query.append("		      ?subclass a owl:Class .\n");
		query.append("		      ?subclass rdfs:label ?subclassLabel .\n");
		query.append("		      ?subclass :NHC0 ?subclassCode\n");
		query.append("		    }\n");
		query.append("		    FILTER (?superclass != ?subclass)\n");
		query.append("		  }\n");
		query.append("		}\n");
		query.append("}\n");
		query.append("ORDER by ?subclassLabel\n");
		
		return query.toString();
	}

	public String constructSuperconceptQuery(String conceptCode, String namedGraph) {
		StringBuffer query = new StringBuffer();
		query.append("SELECT ?superclass ?superclassLabel ?superclassCode\n");
		query.append("{ GRAPH <" + namedGraph + ">");
		query.append("		{\n");
		query.append("		  {\n");
		query.append("		    {\n");
		query.append("		      ?subclass a owl:Class .\n");
		query.append("		      ?subclass :NHC0 \"" + conceptCode + "\" .\n");
		query.append("		      ?subclass rdfs:subClassOf ?superclass .\n");
		query.append("		      ?superclass a owl:Class .\n");
		query.append("		      ?superclass rdfs:label ?superclassLabel .\n");
		query.append("		      ?superclass :NHC0 ?superclassCode\n");
		query.append("		    }\n");
		query.append("		    FILTER (?subclass != ?superclass)\n");
		query.append("		  }\n");
		query.append("		  UNION\n");
		query.append("		  {\n");
		query.append("		    {\n");
		query.append("		      ?subclass a owl:Class .\n");
		query.append("		      ?subclass :NHC0 \"" + conceptCode + "\" .\n");
		query.append("		      ?equiv_concept owl:intersectionOf ?list .\n");
		query.append("		      ?list rdf:rest*/rdf:first ?superclass .\n");
		query.append("		      ?subclass owl:equivalentClass ?equiv_concept .\n");
		query.append("		      ?superclass a owl:Class .\n");
		query.append("		      ?superclass rdfs:label ?superclassLabel .\n");
		query.append("		      ?superclass :NHC0 ?superclassCode\n");
		query.append("		    }\n");
		query.append("		    FILTER (?subclass != ?superclass)\n");
		query.append("		  }\n");
		query.append("		}\n");
		query.append("}\n");
		query.append("ORDER by ?superclassLabel\n");

		return query.toString();
	}	

	public String constructDiseaseIsStageSourceCodesQuery(String namedGraph) {
		log.info("***In constructDiseaseIsStageSourceCodesQuery");
		StringBuffer query = new StringBuffer();
		query.append("SELECT distinct ?conceptLabel ?conceptCode\n");
		query.append("{ \n");
		query.append("    graph <" + namedGraph + ">\n");
		query.append("    {\n");
		query.append("	    {").append("\n");
		query.append("		?x a owl:Class .\n");
		query.append("		?x :NHC0 ?conceptCode .\n");
		query.append("		?x rdfs:label ?conceptLabel .\n");
		query.append("		?x rdfs:subClassOf ?z0 .\n");
		query.append("		?z0 a owl:Class .\n");
		query.append("		?z0 owl:intersectionOf ?list .\n");
		query.append("		?list rdf:rest*/rdf:first ?z2 .\n");
		query.append("		?z2 a owl:Restriction .\n");
		query.append("		?z2 owl:onProperty ?p .\n");
		query.append("		?p rdfs:label ?p_label .\n");
		query.append("		FILTER (str(?p_label)=\"Disease_Is_Stage\"^^xsd:string)\n");
		query.append("	   }\n");
		query.append("	   UNION \n");
		query.append("	   {\n");
		query.append("		?x a owl:Class .\n");
		query.append("		?x rdfs:label ?conceptLabel .\n");
		query.append("		?x :NHC0 ?conceptCode .\n");
		query.append("		?x rdfs:subClassOf ?r .\n");
		query.append("		?r a owl:Restriction .\n");
		query.append("		?r owl:onProperty ?p .\n");
		query.append("		?p rdfs:label ?p_label .\n");
		query.append("		FILTER (str(?p_label)=\"Disease_Is_Stage\"^^xsd:string)\n");
		query.append("	   }\n");
		query.append("	   UNION\n");
		query.append("	   {\n");
		query.append("		?x a owl:Class .\n");
		query.append("		?x rdfs:label ?conceptLabel .\n");
		query.append("		?x :NHC0 ?conceptCode .\n");
		query.append("		?x owl:equivalentClass ?z .\n");
		query.append("			?z owl:intersectionOf ?list .\n");
		query.append("			?list rdf:rest*/rdf:first ?z2 .\n");
		query.append("				?z2 a owl:Restriction .\n");
		query.append("				?z2 owl:onProperty ?p .\n");
		query.append("				?p rdfs:label ?p_label .\n");
		query.append("				FILTER (str(?p_label)=\"Disease_Is_Stage\"^^xsd:string)\n");
		query.append("	   }\n");
		query.append("	   UNION\n");
		query.append("	   {\n");
		query.append("		?x a owl:Class .\n");
		query.append("		?x rdfs:label ?conceptLabel .\n");
		query.append("		?x :NHC0 ?conceptCode .\n");
		query.append("		?x owl:equivalentClass ?z1 .\n");
		query.append("			?z1 owl:intersectionOf ?list1 .\n");
		query.append("			?list1 rdf:rest*/rdf:first ?z2 .\n");
		query.append("			     ?z2 owl:unionOf ?list2 .\n");
		query.append("			     ?list2 rdf:rest*/rdf:first ?z3 .\n");
		query.append("				 ?z3 owl:intersectionOf ?list3 .\n");
		query.append("				 ?list3 rdf:rest*/rdf:first ?z4 .\n");
		query.append("					?z4 a owl:Restriction .\n");
		query.append("					?z4 owl:onProperty ?p .\n");
		query.append("					?p rdfs:label ?p_label .\n");
		query.append("					FILTER (str(?p_label)=\"Disease_Is_Stage\"^^xsd:string)\n");
		query.append("	   }\n");
		query.append("   }\n");
		query.append("}\n");
		return query.toString();
	}
	
	public String constructAssociationsQuery(String conceptCode, String namedGraph) {
		StringBuffer query = new StringBuffer();
		query.append("SELECT ?relationship ?relatedConceptCode ?relatedConceptLabel\n");
		query.append("{\n");
		query.append("    GRAPH <" + namedGraph + ">\n");
		query.append("    {\n");
		query.append("	    ?x a owl:Class .\n");
		query.append("	    ?x :NHC0 \"" + conceptCode + "\"^^<http://www.w3.org/2001/XMLSchema#string> .\n");
		query.append("	    ?y a owl:AnnotationProperty .\n");
		query.append("	    ?x ?y ?z .\n");
		query.append("	    ?z a owl:Class .\n");
		query.append("	    ?z rdfs:label ?relatedConceptLabel .\n");
		query.append("	    ?z :NHC0 ?relatedConceptCode .\n");
		query.append("	    ?y rdfs:label ?relationship .\n");
		query.append("	    ?y :NHC0 ?y_code .\n");
		query.append("	    ?y rdfs:range ?y_range\n");
		query.append("    }\n");
		query.append("    FILTER (str(?y_range)=\"http://www.w3.org/2001/XMLSchema#anyURI\")\n");
		query.append("}\n");
		return query.toString();	
	}		

	public String constructInverseAssociationsQuery(String conceptCode, String namedGraph) {
		StringBuffer query = new StringBuffer();
		query.append("SELECT ?relatedConceptLabel ?relatedConceptCode ?relationship").append("\n");
		query.append("{").append("\n");
		query.append("    GRAPH <" + namedGraph + ">").append("\n");
		query.append("    {").append("\n");
		query.append("	    ?x a owl:Class .").append("\n");
		query.append("	    ?x rdfs:label ?relatedConceptLabel .").append("\n");
		query.append("	    ?x :NHC0 ?relatedConceptCode .").append("\n");
		query.append("	    ?y a owl:AnnotationProperty .").append("\n");
		query.append("	    ?x ?y ?z .").append("\n");
		query.append("	    ?z a owl:Class .").append("\n");
		query.append("	    ?z rdfs:label ?z_label .").append("\n");
		query.append("	    ?z :NHC0 \"" + conceptCode + "\"^^<http://www.w3.org/2001/XMLSchema#string> .").append("\n");
		query.append("	    ?y rdfs:label ?relationship .").append("\n");
		query.append("	    ?y :NHC0 ?y_code .").append("\n");
		query.append("	    ?y rdfs:range ?y_range").append("\n");
		query.append("    }").append("\n");
		query.append("    FILTER (str(?y_range)=\"http://www.w3.org/2001/XMLSchema#anyURI\")").append("\n");
		query.append("}").append("\n");
		return query.toString();		
	}		

	public String constructInverseRolesQuery(String conceptCode, String namedGraph) {
		StringBuffer query = new StringBuffer();
		query.append("SELECT distinct ?relatedConceptLabel ?relatedConceptCode ?relationship ").append("\n");
		query.append("{").append("\n");
		query.append("    GRAPH <" + namedGraph + ">").append("\n");
		query.append("    { ").append("\n");
		query.append("	    {").append("\n");
		query.append("		?x a owl:Class .").append("\n");
		query.append("		?x :NHC0 ?relatedConceptCode .").append("\n");
		query.append("		?x rdfs:label ?relatedConceptLabel .").append("\n");
		query.append("		?x rdfs:subClassOf ?z0 .").append("\n");
		query.append("		?z0 a owl:Class .").append("\n");
		query.append("		?z0 owl:intersectionOf ?list .").append("\n");
		query.append("		?list rdf:rest*/rdf:first ?z2 .").append("\n");
		query.append("		?z2 a owl:Restriction .").append("\n");
		query.append("		?z2 owl:onProperty ?p .").append("\n");
		query.append("		?p rdfs:label ?relationship .").append("\n");
		query.append("		?z2 owl:someValuesFrom ?y .").append("\n");
		query.append("		?y :NHC0 \"" + conceptCode + "\"^^<http://www.w3.org/2001/XMLSchema#string> .").append("\n");
		query.append("		?y rdfs:label ?y_label").append("\n");
		query.append("	   }").append("\n");
		query.append("	   UNION").append("\n");
		query.append("	   {").append("\n");
		query.append("		?x a owl:Class .").append("\n");
		query.append("		?x :NHC0 ?relatedConceptCode .").append("\n");
		query.append("		?x rdfs:label ?relatedConceptLabel .").append("\n");
		query.append("		?x rdfs:subClassOf ?r .").append("\n");
		query.append("		?r a owl:Restriction .").append("\n");
		query.append("		?r owl:onProperty ?p .").append("\n");
		query.append("		?p rdfs:label ?relationship .").append("\n");
		query.append("		?r owl:someValuesFrom ?y .").append("\n");
		query.append("		?y a owl:Class .").append("\n");
		query.append("		?y :NHC0 \"" + conceptCode + "\"^^<http://www.w3.org/2001/XMLSchema#string> ").append("\n");
		query.append("	   }	").append("\n");
		query.append("	   UNION").append("\n");
		query.append("	   {").append("\n");
		query.append("		?x a owl:Class .").append("\n");
		query.append("		?x :NHC0 ?relatedConceptCode .").append("\n");
		query.append("		?x rdfs:label ?relatedConceptLabel .").append("\n");
		query.append("		?x owl:equivalentClass ?z .").append("\n");
		query.append("			?z owl:intersectionOf ?list .").append("\n");
		query.append("			?list rdf:rest*/rdf:first ?z2 .").append("\n");
		query.append("				?z2 a owl:Restriction .").append("\n");
		query.append("				?z2 owl:onProperty ?p .").append("\n");
		query.append("				?p rdfs:label ?relationship .").append("\n");
		query.append("				?z2 owl:someValuesFrom ?y .").append("\n");
		query.append("				?y a owl:Class .").append("\n");
		query.append("				?y :NHC0 \"" + conceptCode + "\"^^<http://www.w3.org/2001/XMLSchema#string> ").append("\n");
		query.append("	   }").append("\n");
		query.append("	   UNION").append("\n");
		query.append("	   {").append("\n");
		query.append("		?x a owl:Class .").append("\n");
		query.append("		?x :NHC0 ?relatedConceptCode .").append("\n");
		query.append("		?x rdfs:label ?relatedConceptLabel .").append("\n");
		query.append("		?x owl:equivalentClass ?z1 .").append("\n");
		query.append("			?z1 owl:intersectionOf ?list1 .").append("\n");
		query.append("			?list1 rdf:rest*/rdf:first ?z2 .").append("\n");
		query.append("			     ?z2 owl:unionOf ?list2 .").append("\n");
		query.append("			     ?list2 rdf:rest*/rdf:first ?z3 .").append("\n");
		query.append("				 ?z3 owl:intersectionOf ?list3 .").append("\n");
		query.append("				 ?list3 rdf:rest*/rdf:first ?z4 .").append("\n");
		query.append("					?z4 a owl:Restriction .").append("\n");
		query.append("					?z4 owl:onProperty ?p .").append("\n");
		query.append("					?p rdfs:label ?relationship .").append("\n");
		query.append("					?z4 owl:someValuesFrom ?y .").append("\n");
		query.append("					?y a owl:Class .").append("\n");
		query.append("					?y :NHC0 \"" + conceptCode + "\"^^<http://www.w3.org/2001/XMLSchema#string> ").append("\n");
		query.append("	   }").append("\n");
		query.append("   }").append("\n");
		query.append("} ").append("\n");

		return query.toString();		
	}		

	public String constructRolesQuery(String conceptCode, String namedGraph) {
		StringBuffer query = new StringBuffer();
		query.append("SELECT distinct ?relationship ?relatedConceptLabel ?relatedConceptCode ").append("\n");
		query.append("{ ").append("\n");
		query.append("    graph <" + namedGraph + ">").append("\n");
		query.append("    {").append("\n");
		query.append("	    {").append("\n");
		query.append("		?x a owl:Class .").append("\n");
		query.append("		?x :NHC0 \"" + conceptCode + "\"^^<http://www.w3.org/2001/XMLSchema#string> .").append("\n");
		query.append("		?x rdfs:label ?x_label .").append("\n");
		query.append("		?x rdfs:subClassOf ?z0 .").append("\n");
		query.append("		?z0 a owl:Class .").append("\n");
		query.append("		?z0 owl:intersectionOf ?list .").append("\n");
		query.append("		?list rdf:rest*/rdf:first ?z2 .").append("\n");
		query.append("		?z2 a owl:Restriction .").append("\n");
		query.append("		?z2 owl:onProperty ?p .").append("\n");
		query.append("		?p rdfs:label ?relationship .").append("\n");
		query.append("		?z2 owl:someValuesFrom ?y .").append("\n");
		query.append("		?y :NHC0 ?relatedConceptCode .").append("\n");
		query.append("		?y rdfs:label ?relatedConceptLabel").append("\n");
		query.append("	   }").append("\n");
		query.append("	   UNION ").append("\n");
		query.append("	   {").append("\n");
		query.append("		?x a owl:Class .").append("\n");
		query.append("		?x :NHC0 \"" + conceptCode + "\"^^<http://www.w3.org/2001/XMLSchema#string> .").append("\n");
		query.append("		?x rdfs:subClassOf ?r .").append("\n");
		query.append("		?r a owl:Restriction .").append("\n");
		query.append("		?r owl:onProperty ?p .").append("\n");
		query.append("		?p rdfs:label ?relationship .").append("\n");
		query.append("		?r owl:someValuesFrom ?y .").append("\n");
		query.append("		?y a owl:Class .").append("\n");
		query.append("		?y rdfs:label ?relatedConceptLabel .").append("\n");
		query.append("		?y :NHC0 ?relatedConceptCode").append("\n");
		query.append("	   }	").append("\n");
		query.append("	   UNION").append("\n");
		query.append("	   {").append("\n");
		query.append("		?x a owl:Class .").append("\n");
		query.append("		?x :NHC0 \"" + conceptCode + "\"^^<http://www.w3.org/2001/XMLSchema#string> .").append("\n");
		query.append("		?x owl:equivalentClass ?z .").append("\n");
		query.append("			?z owl:intersectionOf ?list .").append("\n");
		query.append("			?list rdf:rest*/rdf:first ?z2 .").append("\n");
		query.append("				?z2 a owl:Restriction .").append("\n");
		query.append("				?z2 owl:onProperty ?p .").append("\n");
		query.append("				?p rdfs:label ?relationship .").append("\n");
		query.append("				?z2 owl:someValuesFrom ?y .").append("\n");
		query.append("				?y :NHC0 ?relatedConceptCode .").append("\n");
		query.append("				?y rdfs:label ?relatedConceptLabel").append("\n");
		query.append("	   }").append("\n");
		query.append("	   UNION").append("\n");
		query.append("	   {").append("\n");
		query.append("		?x a owl:Class .").append("\n");
		query.append("		?x :NHC0 \"" + conceptCode + "\"^^<http://www.w3.org/2001/XMLSchema#string> .").append("\n");
		query.append("		?x owl:equivalentClass ?z1 .").append("\n");
		query.append("			?z1 owl:intersectionOf ?list1 .").append("\n");
		query.append("			?list1 rdf:rest*/rdf:first ?z2 .").append("\n");
		query.append("			     ?z2 owl:unionOf ?list2 .").append("\n");
		query.append("			     ?list2 rdf:rest*/rdf:first ?z3 .").append("\n");
		query.append("				 ?z3 owl:intersectionOf ?list3 .").append("\n");
		query.append("				 ?list3 rdf:rest*/rdf:first ?z4 .").append("\n");
		query.append("					?z4 a owl:Restriction .").append("\n");
		query.append("					?z4 owl:onProperty ?p .").append("\n");
		query.append("					?p rdfs:label ?relationship .").append("\n");
		query.append("					?z4 owl:someValuesFrom ?y .").append("\n");
		query.append("					?y :NHC0 ?relatedConceptCode .").append("\n");
		query.append("					?y rdfs:label ?relatedConceptLabel").append("\n");
		query.append("	   }").append("\n");
		query.append("   }").append("\n");
		query.append("} ").append("\n");

		return query.toString();
	}

}
