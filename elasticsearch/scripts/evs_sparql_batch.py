import sys
import requests
from requests.auth import HTTPBasicAuth
import json


def run_sparql_query(endpoint, named_graph, query, in_clause):
    '''Generic method for running a SPARQL query.
    Checks the status code and returns results in JSON format.

    :param str endpoint: url for endpoint 
    :param str named_graph: named_graph
    :param str query: SPARQL query
    :rtype: JSON
    :returns: query results in JSON format

    '''

    query = query.replace("NAMED_GRAPH", named_graph)
    query = query.replace("IN_CLAUSE", in_clause)
    sparql_query = prefix + query
    #print(sparql_query)
    headers = {'Accept': 'application/sparql-results+json'}
    r = requests.post(endpoint,
                      headers=headers, data={"query": sparql_query},
                      auth=HTTPBasicAuth("admin", "admin"))

    if r.status_code != 200:
        sys.stderr.write("Problem Status Code: " + str(r.status_code) + "\n")
        return None

    return r.json()


prefix = '''
PREFIX :<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>
PREFIX base:<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl>
PREFIX owl:<http://www.w3.org/2002/07/owl#>
PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>
PREFIX dc:<http://purl.org/dc/elements/1.1/>
'''

all_concepts = '''
SELECT ?concept_code
{ GRAPH <NAMED_GRAPH> {
        ?concept a owl:Class .
        ?concept rdfs:label ?concept_label .
        ?concept :NHC0 ?concept_code
    }
}
'''

all_concepts_batch = '''
SELECT ?concept_code ?concept_label
{ GRAPH <NAMED_GRAPH> {
        ?concept a owl:Class .
        FILTER (?concept_code IN (IN_CLAUSE)) .
        ?concept rdfs:label ?concept_label .
        ?concept :NHC0 ?concept_code
    }
}
'''

all_properties_all_concepts = '''
SELECT ?concept_code ?property ?property_code ?property_label ?property_value
{ GRAPH <NAMED_GRAPH> {
        ?concept a owl:Class .
        ?concept :NHC0 ?concept_code .
        FILTER (?concept_code IN (IN_CLAUSE)) .
        ?concept ?property ?property_value .
        ?property a owl:AnnotationProperty .
        ?property rdfs:label ?property_label .
        OPTIONAL { ?property :NHC0 ?property_code }
    }
}
ORDER BY ?concept_code ?property_code
'''

all_axioms_all_concepts = '''
SELECT ?concept_code ?axiom ?axiomProperty ?axiomValue
{ GRAPH <NAMED_GRAPH> {
      ?concept a owl:Class .
      ?concept :NHC0 ?concept_code .
      FILTER (?concept_code IN (IN_CLAUSE)) .
      ?axiom a owl:Axiom .
      ?axiom owl:annotatedSource ?concept .
      ?axiom ?axiomProperty ?axiomValue
    }
}
ORDER BY ?axiom
'''

all_subclasses_all_concepts = '''
SELECT ?concept_code ?subclass ?subclass_label ?subclass_code
{ GRAPH <NAMED_GRAPH> 
{
    {
        {
            ?superclass a owl:Class .
            ?superclass :NHC0 ?concept_code  .
            FILTER (?concept_code IN (IN_CLAUSE)) .
            ?subclass rdfs:subClassOf ?superclass .
            ?subclass a owl:Class .
            ?subclass rdfs:label ?subclass_label .
            ?subclass :NHC0 ?subclass_code
        }
        FILTER (?superclass != ?subclass)
    }
    UNION
    {
        {
            ?superclass a owl:Class .
            ?superclass :NHC0 ?concept_code .
            FILTER (?concept_code IN (IN_CLAUSE)) .
            ?equiv_concept owl:intersectionOf ?list .
            ?list rdf:rest*/rdf:first ?superclass .
            ?subclass owl:equivalentClass ?equiv_concept .
            ?subclass a owl:Class .
            ?subclass rdfs:label ?subclass_label .
            ?subclass :NHC0 ?subclass_code
        }
        FILTER (?superclass != ?subclass)
    }
}
}
ORDER by ?concept_code ?subclass_label
'''

all_superclasses_all_concepts = '''
SELECT ?concept_code ?superclass ?superclass_label ?superclass_code
{ GRAPH <NAMED_GRAPH>
        {
          {
            {
              ?subclass a owl:Class .
              ?subclass :NHC0 ?concept_code .
              FILTER (?concept_code IN (IN_CLAUSE)) .
              ?subclass rdfs:subClassOf ?superclass .
              ?superclass a owl:Class .
              ?superclass rdfs:label ?superclass_label .
              ?superclass :NHC0 ?superclass_code
            }
            FILTER (?subclass != ?superclass)
          }
          UNION
          {
            {
              ?subclass a owl:Class .
              ?subclass :NHC0 ?concept_code .
              FILTER (?concept_code IN (IN_CLAUSE)) .
              ?equiv_concept owl:intersectionOf ?list .
              ?list rdf:rest*/rdf:first ?superclass .
              ?subclass owl:equivalentClass ?equiv_concept .
              ?superclass a owl:Class .
              ?superclass rdfs:label ?superclass_label .
              ?superclass :NHC0 ?superclass_code
            }
            FILTER (?subclass != ?superclass)
          }
        }
}
ORDER by ?concept_code ?superclass_label
'''

all_associations_all_concepts = '''
SELECT ?concept_code ?relationshipCode ?relationship ?relatedConceptCode ?relatedConceptLabel
{
    GRAPH <NAMED_GRAPH>
    {
        ?x a owl:Class .
        ?x :NHC0 ?concept_code .
        FILTER (?concept_code IN (IN_CLAUSE)) .
        ?y a owl:AnnotationProperty .
        ?x ?y ?z .
        ?z a owl:Class .
        ?z rdfs:label ?relatedConceptLabel .
        ?z :NHC0 ?relatedConceptCode .
        ?y rdfs:label ?relationship .
        ?y :NHC0 ?relationshipCode .
        ?y rdfs:range ?y_range
    }
    FILTER (str(?y_range)="http://www.w3.org/2001/XMLSchema#anyURI")
}
ORDER BY ?concept_code ?relationship ?relatedConceptLabel
'''

all_inverse_associations_all_concepts = '''
SELECT ?concept_code ?relationshipCode ?relatedConceptLabel ?relatedConceptCode ?relationship
{
    GRAPH <NAMED_GRAPH>
    {
        ?z a owl:Class .
        ?z rdfs:label ?z_label .
        ?z :NHC0 ?concept_code .
        FILTER (?concept_code IN (IN_CLAUSE)) .
        ?x a owl:Class .
        ?x rdfs:label ?relatedConceptLabel .
        ?x :NHC0 ?relatedConceptCode .
        ?y a owl:AnnotationProperty .
        ?x ?y ?z .
        ?y rdfs:label ?relationship .
        ?y :NHC0 ?relationshipCode .
        ?y rdfs:range ?y_range
    }
    FILTER (str(?y_range)="http://www.w3.org/2001/XMLSchema#anyURI")
}
ORDER BY ?concept_code ?relationship ?relatedConceptLabel
'''

all_disjoint_with_all_concepts = '''
SELECT ?concept_code ?relationship ?relatedConceptCode ?relatedConceptLabel
{
    GRAPH <NAMED_GRAPH>
    {
        ?x a owl:Class .
        ?x :NHC0 ?concept_code .
        FILTER (?concept_code IN (IN_CLAUSE)) .
        ?x owl:disjointWith ?concept .
        ?concept :NHC0 ?relatedConceptCode .
        ?concept rdfs:label ?relatedConceptLabel
        BIND ("disjointWith" as ?relationship)
    }
}
ORDER BY ?concept_code ?relatedConceptLabel
'''

all_roles_all_concepts = '''
SELECT distinct ?concept_code ?relationship ?relationshipCode ?relatedConceptLabel ?relatedConceptCode
{
    graph <NAMED_GRAPH>
    {
      {
        ?x a owl:Class .
        ?x :NHC0 ?concept_code .
        FILTER (?concept_code IN (IN_CLAUSE)) .
        ?x rdfs:label ?x_label .
        ?x rdfs:subClassOf ?z0 .
        ?z0 a owl:Class .
        ?z0 owl:intersectionOf ?list .
        ?list rdf:rest*/rdf:first ?z2 .
        ?z2 a owl:Restriction .
        ?z2 owl:onProperty ?p .
        ?p rdfs:label ?relationship .
        ?p :NHC0 ?relationshipCode .
        ?z2 owl:someValuesFrom ?y .
        ?y :NHC0 ?relatedConceptCode .
        ?y rdfs:label ?relatedConceptLabel
      }
      UNION
      {
        ?x a owl:Class .
        ?x :NHC0 ?concept_code .
        FILTER (?concept_code IN (IN_CLAUSE)) .
        ?x rdfs:subClassOf ?r .
        ?r a owl:Restriction .
        ?r owl:onProperty ?p .
        ?p rdfs:label ?relationship .
        ?p :NHC0 ?relationshipCode .
        ?r owl:someValuesFrom ?y .
        ?y a owl:Class .
        ?y rdfs:label ?relatedConceptLabel .
        ?y :NHC0 ?relatedConceptCode
      }
      UNION
      {
        ?x a owl:Class .
        ?x :NHC0 ?concept_code .
        FILTER (?concept_code IN (IN_CLAUSE)) .
        ?x owl:equivalentClass ?z .
        ?z a owl:Class .
        ?z owl:intersectionOf ?list .
        ?list rdf:rest*/rdf:first ?z2 .
        ?z2 a owl:Restriction .
        ?z2 owl:onProperty ?p .
        ?p rdfs:label ?relationship .
        ?p :NHC0 ?relationshipCode .
        ?z2 owl:someValuesFrom ?y .
        ?y :NHC0 ?relatedConceptCode .
        ?y rdfs:label ?relatedConceptLabel
      }
      UNION
      {
        ?x a owl:Class .
        ?x :NHC0 ?concept_code .
        FILTER (?concept_code IN (IN_CLAUSE)) .
        ?x owl:equivalentClass ?z1 .
        ?z1 a owl:Class .
        ?z1 owl:intersectionOf ?list1 .
        ?list1 rdf:rest*/rdf:first ?z2 .
        ?z2 owl:unionOf ?list2 .
        ?list2 rdf:rest*/rdf:first ?z3 .
        ?z3 owl:intersectionOf ?list3 .
        ?list3 rdf:rest*/rdf:first ?z4 .
        ?z4 a owl:Restriction .
        ?z4 owl:onProperty ?p .
        ?p rdfs:label ?relationship .
        ?p :NHC0 ?relationshipCode .
        ?z4 owl:someValuesFrom ?y .
        ?y :NHC0 ?relatedConceptCode .
        ?y rdfs:label ?relatedConceptLabel
      }
   }
}
ORDER BY ?concept_code ?relationship ?relatedConceptLabel
'''

all_inverse_roles_all_concepts = '''
SELECT distinct ?concept_code ?relationship ?relationshipCode ?relatedConceptLabel ?relatedConceptCode
{
    GRAPH <NAMED_GRAPH>
    {
      {
        ?x a owl:Class .
        ?x :NHC0 ?relatedConceptCode .
        ?x rdfs:label ?relatedConceptLabel .
        ?x rdfs:subClassOf ?z0 .
        ?z0 a owl:Class .
        ?z0 owl:intersectionOf ?list .
        ?list rdf:rest*/rdf:first ?z2 .
        ?z2 a owl:Restriction .
        ?z2 owl:onProperty ?p .
        ?p rdfs:label ?relationship .
        ?p :NHC0 ?relationshipCode .
        ?z2 owl:someValuesFrom ?y .
        ?y :NHC0 ?concept_code .
        FILTER (?concept_code IN (IN_CLAUSE)) .
        ?y rdfs:label ?y_label
      }
      UNION
      {
        ?x a owl:Class .
        ?x :NHC0 ?relatedConceptCode .
        ?x rdfs:label ?relatedConceptLabel .
        ?x rdfs:subClassOf ?r .
        ?r a owl:Restriction .
        ?r owl:onProperty ?p .
        ?p rdfs:label ?relationship .
        ?p :NHC0 ?relationshipCode .
        ?r owl:someValuesFrom ?y .
        ?y a owl:Class .
        ?y :NHC0 ?concept_code .
        FILTER (?concept_code IN (IN_CLAUSE))
      }
      UNION
      {
        ?x a owl:Class .
        ?x :NHC0 ?relatedConceptCode .
        ?x rdfs:label ?relatedConceptLabel .
        ?x owl:equivalentClass ?z .
        ?z a owl:Class .
        ?z owl:intersectionOf ?list .
        ?list rdf:rest*/rdf:first ?z2 .
        ?z2 a owl:Restriction .
        ?z2 owl:onProperty ?p .
        ?p rdfs:label ?relationship .
        ?p :NHC0 ?relationshipCode .
        ?z2 owl:someValuesFrom ?y .
        ?y a owl:Class .
        ?y :NHC0 ?concept_code .
        FILTER (?concept_code IN (IN_CLAUSE))
      }
      UNION
      {
        ?x a owl:Class .
        ?x :NHC0 ?relatedConceptCode .
        ?x rdfs:label ?relatedConceptLabel .
        ?x owl:equivalentClass ?z1 .
        ?z1 a owl:Class .
        ?z1 owl:intersectionOf ?list1 .
        ?list1 rdf:rest*/rdf:first ?z2 .
        ?z2 owl:unionOf ?list2 .
        ?list2 rdf:rest*/rdf:first ?z3 .
        ?z3 owl:intersectionOf ?list3 .
        ?list3 rdf:rest*/rdf:first ?z4 .
        ?z4 a owl:Restriction .
        ?z4 owl:onProperty ?p .
        ?p rdfs:label ?relationship .
        ?p :NHC0 ?relationshipCode .
        ?z4 owl:someValuesFrom ?y .
        ?y a owl:Class .
        ?y :NHC0 ?concept_code .
        FILTER (?concept_code IN (IN_CLAUSE))
      }
   }
}
ORDER BY ?concept_code ?relationship ?relatedConceptLabel
'''