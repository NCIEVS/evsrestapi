#!/usr/bin/env python

import sys
import os
import json
from pathlib import Path
from dotenv import load_dotenv

import evs_sparql as es
import evs_util as eu

env_path = Path('.')/'.env'
load_dotenv(".env")
SPARQL_ENDPOINT = os.environ.get("SPARQL_ENDPOINT")
NAMED_GRAPH = os.environ.get("NAMED_GRAPH")
CONCEPT_OUTPUT_DIR = os.environ.get('CONCEPT_OUTPUT_DIR')

if __name__ == '__main__':
    print("Starting bulkDownloadConcepts")
    concepts = eu.getAllConcepts(SPARQL_ENDPOINT, NAMED_GRAPH)
    print("Number of Concepts: " + str(len(concepts)))

    properties = eu.getAllProperties(SPARQL_ENDPOINT, NAMED_GRAPH)
    print("Number of Properties: " + str(len(properties)))

    eu.mergeConceptsAndProperties(concepts, properties)
    del properties

    axioms = eu.getAllAxioms(SPARQL_ENDPOINT, NAMED_GRAPH)
    print("Number of Axioms: " + str(len(axioms)))
    eu.mergeConceptsAndAxioms(concepts, axioms)
    del axioms

    subclasses = eu.getAllSubclasses(SPARQL_ENDPOINT, NAMED_GRAPH)
    print("Number of subclasses: " + str(len(subclasses)))
    eu.mergeConceptsAndSubclasses(concepts, subclasses)
    del subclasses

    superclasses = eu.getAllSuperclasses(SPARQL_ENDPOINT, NAMED_GRAPH)
    print("Number of superclasses: " + str(len(superclasses)))
    eu.mergeConceptsAndSuperclasses(concepts, superclasses)
    del superclasses

    associations = eu.getAllAssociations(SPARQL_ENDPOINT, NAMED_GRAPH)
    print("Number of associations: " + str(len(associations)))
    eu.mergeConceptsAndAssociations(concepts, associations)
    del associations

    inverse_associations = eu.getAllInverseAssociations(SPARQL_ENDPOINT, NAMED_GRAPH)
    print("Number of inverse associations: " + str(len(inverse_associations)))
    eu.mergeConceptsAndInverseAssociations(concepts, inverse_associations)
    del inverse_associations

    roles = eu.getAllRoles(SPARQL_ENDPOINT, NAMED_GRAPH)
    print("Number of roles: " + str(len(roles)))
    eu.mergeConceptsAndRoles(concepts, roles)
    del roles

    inverse_roles = eu.getAllInverseRoles(SPARQL_ENDPOINT, NAMED_GRAPH)
    print("Number of inverse_roles: " + str(len(inverse_roles)))
    eu.mergeConceptsAndInverseRoles(concepts, inverse_roles)
    del inverse_roles

    disjoint_withs = eu.getAllDisjointWith(SPARQL_ENDPOINT, NAMED_GRAPH)
    print("Number of disjoint_withs: " + str(len(disjoint_withs)))
    eu.mergeConceptsAndDisjointWith(concepts, disjoint_withs)
    del disjoint_withs

    for code, concept in concepts.items():
        eu.addAdditionalProperties(concept)
        del concept['properties']
        eu.addFullSynonyms(concept)
        eu.addDefinitions(concept)
        eu.addAltDefinitions(concept)
        eu.addMapsTo(concept)
        eu.addGO_Annotation(concept)
        del concept['axioms']
        eu.addSubclasses(concept)
        del concept['subclasses']
        eu.addSuperclasses(concept)
        del concept['superclasses']
        eu.addAssociations(concept)
        del concept['associations']
        eu.addInverseAssociations(concept)
        del concept['inverse_associations']
        eu.addRoles(concept)
        del concept['roles']
        eu.addInverseRoles(concept)
        del concept['inverse_roles']
        eu.addDisjointWith(concept)
        del concept['disjoint_with']

    for code, concept in concepts.items():
        with open(CONCEPT_OUTPUT_DIR + concept['Code'] + ".json", "w") as output_file:
            print(json.dumps(concept, indent=2, sort_keys=True), file=output_file)
            output_file.close()
