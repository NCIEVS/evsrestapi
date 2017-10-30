package gov.nih.nci.evs.api.service;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gov.nih.nci.evs.api.properties.StardogProperties;



@Service
public class QueryBuilderServiceImpl implements QueryBuilderService {
	
	private static final Logger log = LoggerFactory.getLogger(QueryBuilderServiceImpl.class);
	
	
	@Autowired
	StardogProperties stardogProperties;
	private String named_graph_id = ":NHC0";
	
    public void set_named_graph_id(String named_graph_id) {
		this.named_graph_id = named_graph_id;
	}
	
	public String contructPrefix(){
		String prefix = String.join(System.getProperty("line.separator"), 
			    "PREFIX :<http://ncicb.nci.nih.gov/xml/owl/EVS/" + stardogProperties.getOwlfileName() + "#>",
		        "PREFIX base:<http://ncicb.nci.nih.gov/xml/owl/EVS/" + stardogProperties.getOwlfileName() + ">",
		        "PREFIX owl:<http://www.w3.org/2002/07/owl#>",
		        "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>",
		        "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>",
		        "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>"
		);

		log.debug("prefix - " + prefix);
		
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
		
		log.debug("constructConceptLabelQuery - " + query.toString());
		
		return query.toString();
		
	}
	
	public String constructSearchQuery(String searchStr, String property, String limit, String namedGraph){
		StringBuffer query = new StringBuffer();
		
		if (property == null || property.equalsIgnoreCase("")){
			query.append("SELECT ?conceptCode ?conceptLabel ?conceptStatus ?preferredName ?propertyCode ?propertyLabel ?propertyValue ?score\n");
			query.append("{ GRAPH <" + namedGraph + ">");
			query.append("  { ?concept a owl:Class .\n");
			query.append("    ?concept :NHC0  ?conceptCode .\n");		
			query.append("    ?concept rdfs:label ?conceptLabel .\n");
			query.append("    ?concept ?property ?propertyValue .\n");
			query.append("    ?property :NHC0 ?propertyCode .\n");
			query.append("    ?property a owl:AnnotationProperty .\n");
			query.append("    ?property rdfs:label ?propertyLabel .\n");	
			query.append("    (?propertyValue ?score) <tag:stardog:api:property:textMatch> '"+ searchStr +"'.\n");
			query.append("    OPTIONAL { ?concept :P310 ?conceptStatus . }\n");
			query.append("    OPTIONAL { ?concept :P108 ?preferredName . }\n");
			query.append("  }\n");
			query.append("}\n");
			query.append("order by DESC(?score)");
		} else {
			
			StringBuffer propertyQuery = new StringBuffer();
			String[] properties = property.split(",");
			List propertiesList =  Arrays.asList(properties);
			int size = propertiesList.size();
			int count = 0;
			for (Object p : propertiesList){
				String propertyCode = ((String)p).trim();
				count++;
				
				propertyQuery = propertyQuery.append("   { ?property :NHC0 \"" +  propertyCode + "\" } \n");
				if (count < size){
					propertyQuery = propertyQuery.append("UNION \n");
				}
			}
			
			String proQuery = propertyQuery.toString();
			
			query.append("SELECT ?conceptCode ?conceptLabel ?conceptStatus ?preferredName ?propertyCode ?propertyLabel ?propertyValue ?score\n");
			query.append("{ GRAPH <" + namedGraph + ">");
			query.append("  { ?concept a owl:Class .\n");
			query.append("    ?concept :NHC0  ?conceptCode .\n");		
			query.append("    ?concept rdfs:label ?conceptLabel .\n");
			query.append("    ?concept ?property ?propertyValue .\n");
			query.append("    ?property :NHC0 ?propertyCode .\n");
			query.append("    ?property a owl:AnnotationProperty .\n");
			query.append("    ?property rdfs:label ?propertyLabel .\n");	
		
			if (proQuery != null && !proQuery.equalsIgnoreCase("")){
				query.append(proQuery);
			}
			query.append("    (?propertyValue ?score) <tag:stardog:api:property:textMatch> '"+ searchStr +"'.\n");
			query.append("    OPTIONAL { ?concept :P310 ?conceptStatus . }\n");
			query.append("    OPTIONAL { ?concept :P108 ?preferredName . }\n");
			query.append("  }\n");
			query.append("}\n");
			query.append("order by DESC(?score)");
			
		}
		
		if (limit != null && !limit.equals("")) {
			query.append("LIMIT " + limit + "\n");
		}
		
		log.info("constructSearchQuery - " + query.toString());
		
		return query.toString();
	}
	
	public String constructGetClassCountsQuery(String namedGraph) {		
		StringBuffer query = new StringBuffer();		
		query.append("SELECT ( count(?class) as ?count )\n");
		query.append("{ GRAPH <" + namedGraph + ">");
		query.append("  { ?class a owl:Class\n");
		query.append("  }\n");
		query.append("}\n");
		
		log.debug("constructGetClassCounts - " + query.toString());
		
		return query.toString();
	}
	
	
	public String constructPropertyQuery(String conceptCode, String namedGraph){
		StringBuffer query = new StringBuffer();
		query.append("SELECT ?property ?propertyCode  ?propertyLabel ?propertyValue\n");
		query.append("{ GRAPH <" + namedGraph + ">");
		query.append("  { ?concept a owl:Class .\n");
		query.append("    ?concept :NHC0 "+ "\"" + conceptCode + "\" .\n");
		query.append("    ?concept ?property ?propertyValue .\n");
		query.append("    ?property :NHC0 ?propertyCode .\n");
		query.append("    ?property a owl:AnnotationProperty .\n");
		query.append("    ?property rdfs:label ?propertyLabel .\n");
		query.append("  }\n");
		query.append("}\n");

		log.debug("constructPropertyQuery - " + query.toString());
		
		return query.toString();
	}

	/*
	 * This version of the query is slower in Stardog than the one above.
	 * So for now we will use the faster query and filter in the JAVA code.
	 * 	
	 */
	/*
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

		log.info("constructPropertyQuery - " + query.toString());
		
		return query.toString();
	}
	*/
	
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
		
        log.debug("constructAxiomQuery - " + query.toString());
        
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
		
		log.debug("constructSubconceptQuery - " + query.toString());
		
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
		
		log.debug("constructSuperconceptQuery - " + query.toString());

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
		
		
		log.debug("constructDiseaseIsStageSourceCodesQuery - " + query.toString());
		
		return query.toString();
	}
	
	
	public String constructDiseaseIsGradeSourceCodesQuery(String namedGraph) {
		log.info("***In constructDiseaseIsGradeSourceCodesQuery");
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
		query.append("		FILTER (str(?p_label)=\"Disease_Is_Grade\"^^xsd:string)\n");
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
		query.append("		FILTER (str(?p_label)=\"Disease_Is_Grade\"^^xsd:string)\n");
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
		query.append("				FILTER (str(?p_label)=\"Disease_Is_Grade\"^^xsd:string)\n");
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
		query.append("					FILTER (str(?p_label)=\"Disease_Is_Grade\"^^xsd:string)\n");
		query.append("	   }\n");
		query.append("   }\n");
		query.append("}\n");
		
		
		log.debug("constructDiseaseIsGradeSourceCodesQuery - " + query.toString());
		
		return query.toString();
	}

	public String constructHierarchyQuery(String namedGraph) {
		log.info("***In constructHierarchyQuery");
		StringBuffer query = new StringBuffer();
		query.append("SELECT ?parentCode ?parentLabel ?childCode ?childLabel\n");
		query.append("	{\n");
		query.append("    GRAPH <" + namedGraph + ">\n");
		query.append("	  {\n");
		query.append("	    {\n");
		query.append("	      {\n");
		query.append("	        ?child a owl:Class .\n");
		query.append("	        ?child rdfs:label ?childLabel .\n");
		query.append("	        ?child :NHC0 ?childCode .\n");
		query.append("	        ?child rdfs:subClassOf ?parent .\n");
		query.append("	        ?parent a owl:Class .\n");
		query.append("	        ?parent rdfs:label ?parentLabel .\n");
		query.append("	        ?parent :NHC0 ?parentCode\n");
		query.append("	      }\n");
		query.append("	      FILTER (?child != ?parent)\n");
		query.append("	    }\n");
		query.append("	    UNION\n");
		query.append("	    {\n");
		query.append("	      {\n");
		query.append("	        ?child a owl:Class .\n");
		query.append("	        ?child rdfs:label ?childLabel .\n");
		query.append("	        ?child :NHC0 ?childCode .\n");
		query.append("	        ?child owl:equivalentClass ?y .\n");
		query.append("	        ?y owl:intersectionOf ?list .\n");
		query.append("	        ?list rdf:rest*/rdf:first ?parent .\n");
		query.append("	        ?parent a owl:Class .\n");
		query.append("	        ?parent rdfs:label ?parentLabel .\n");
		query.append("	        ?parent :NHC0 ?parentCode\n");
		query.append("	      }\n");
		query.append("	      FILTER (?child != ?parent)\n");
		query.append("	    }\n");
		query.append("	  }\n");
		query.append("	}\n");
		
		log.debug("constructHierarchyQuery - " + query.toString());
		
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
		
		log.debug("constructAssociationsQuery - " + query.toString());
		
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
		
		log.debug("constructInverseAssociationsQuery - " + query.toString());
		
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
		
		log.debug("constructInverseRolesQuery - " + query.toString());

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
		
		log.debug("constructRolesQuery - " + query.toString());

		return query.toString();
	}
	
	public String constructConceptInSubsetQuery(String subsetCode, String namedGraph) {
		StringBuffer query = new StringBuffer();
		query.append("SELECT ?conceptLabel ?conceptCode").append("\n");
		query.append("{").append("\n");
		query.append("    GRAPH <" + namedGraph + ">").append("\n");
		query.append("    {").append("\n");
		query.append("	    ?x a owl:Class .").append("\n");
		query.append("	    ?x rdfs:label ?conceptLabel .").append("\n");
		query.append("      ?x " + named_graph_id + " ?conceptCode .").append("\n");
		query.append("      ?y a owl:AnnotationProperty .").append("\n");
		query.append("      ?x ?y ?z .").append("\n");
		query.append("      ?z " + named_graph_id + " \"" + subsetCode + "\"^^xsd:string .").append("\n");
		query.append("      ?y rdfs:label " + "\"" + "Concept_In_Subset" + "\"^^xsd:string ").append("\n");
		query.append("    }").append("\n");
		query.append("}").append("\n");

		log.debug("constructConceptInSubsetQuery - " + query.toString());
		return query.toString();
		
	}

}
