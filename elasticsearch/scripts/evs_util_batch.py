import evs_sparql_batch as es
import json

IGNORE_PROPERTIES = {
    "code": 1,
    "label": 1,
    "DEFINITION": 1,
    "ALT_DEFINITION": 1,
    "FULL_SYN": 1,
    "Maps_To": 1,
    "GO_Annotation": 1
}

def getAllConcepts(sparql_endpoint, named_graph):
    query = es.all_concepts
    obj = es.run_sparql_query(sparql_endpoint, named_graph, query, "")

    if obj is None:
        print("No Result Returned - Problem with Query")
        return None
    else:  
        concepts = []
        for result in obj['results']['bindings']:
            concepts.append(result['concept_code']['value'])

    return concepts

def getBulkConcepts(sparql_endpoint, named_graph, in_clause):
    query = es.all_concepts_batch
    obj = es.run_sparql_query(sparql_endpoint, named_graph, query, in_clause)

    if obj is None:
        print("No Result Returned - Problem with Query")
        return None
    else:
        concepts = {}
        for result in obj['results']['bindings']:
            concept = {}
            concept['Code'] = result['concept_code']['value']
            concept['Label'] = result['concept_label']['value']
            concept['properties'] = []
            concept['axioms'] = []
            concept['subclasses'] = []
            concept['superclasses'] = []
            concept['associations'] = []
            concept['inverse_associations'] = []
            concept['roles'] = []
            concept['inverse_roles'] = []
            concept['disjoint_with'] = []
            #r.execute_command('JSON.SET', concept['Code'], '.', json.dumps(concept))
            #rj.jsonset(concept['Code'], rejson.Path.rootPath(), concept)
            concepts[concept['Code']] = concept

    return concepts


def getAllProperties(sparql_endpoint, named_graph, in_clause):
    query = es.all_properties_all_concepts
    obj = es.run_sparql_query(sparql_endpoint, named_graph, query, in_clause)

    if obj is None:
        print("No Result Returned - Problem with Query")
        return None
    else:
        properties = []
        for result in obj['results']['bindings']:
            property = {}
            property['concept_code'] = result['concept_code']['value']
            if "property_code" in result:
                property['property_code'] = result['property_code']['value']
            else:
                property['property_code'] = ""
            property['property_label'] = result['property_label']['value']
            property['property_value'] = result['property_value']['value']
            properties.append(property)
    return properties


def getAllPropertiesFast(sparql_endpoint, named_graph, concepts, in_clause):
    query = es.all_properties_all_concepts
    obj = es.run_sparql_query(sparql_endpoint, named_graph, query, in_clause)

    if obj is None:
        print("No Result Returned - Problem with Query")
        return None
    else:
        for result in obj['results']['bindings']:
            property = {}
            property['concept_code'] = result['concept_code']['value']
            if property['concept_code'] in concepts:
                if "property_code" in result:
                    property['property_code'] = result['property_code']['value']
                property['property_label'] = result['property_label']['value']
                property['property_value'] = result['property_value']['value']
                concepts[property['concept_code']]['properties'].append(property)


def getAllAxioms(sparql_endpoint, named_graph, in_clause):
    query = es.all_axioms_all_concepts
    obj = es.run_sparql_query(sparql_endpoint, named_graph, query, in_clause)

    if obj is None:
        print("No Result Returned - Problem with Query")
        return None
    else:
        sw = False
        oldAxiom = ""
        axiom = {}
        axioms = []
        for result in obj['results']['bindings']:
            currentAxiom = result['axiom']['value']
            if sw and currentAxiom != oldAxiom:
                axioms.append(axiom)
                axiom = {}

            sw = True
            oldAxiom = currentAxiom
            axiom['concept_code'] = result['concept_code']['value']

            property = result['axiomProperty']['value']
            if "#" in property:
                property = property.split("#")[1]
            if ":" in property:
                property = property.split(":")[1]

            value = result['axiomValue']['value']
            if "#" in value:
                value = value.split("#")[1]
            
            axiom[property] = value

        axioms.append(axiom)

    return axioms


def getAllSubclasses(sparql_endpoint, named_graph, in_clause):
    query = es.all_subclasses_all_concepts
    obj = es.run_sparql_query(sparql_endpoint, named_graph, query, in_clause)

    if obj is None:
        print("No Result Returned - Problem with Query")
        return None
    else:
        subclasses = []
        for result in obj['results']['bindings']:
            subclass = {}
            subclass['concept_code'] = result['concept_code']['value']
            subclass['subclass_label'] = result['subclass_label']['value']
            subclass['subclass_code'] = result['subclass_code']['value']
            subclasses.append(subclass)
    return subclasses


def getAllSuperclasses(sparql_endpoint, named_graph, in_clause):
    query = es.all_superclasses_all_concepts
    obj = es.run_sparql_query(sparql_endpoint, named_graph, query, in_clause)

    if obj is None:
        print("No Result Returned - Problem with Query")
        return None
    else:
        superclasses = []
        for result in obj['results']['bindings']:
            superclass = {}
            superclass['concept_code'] = result['concept_code']['value']
            superclass['superclass_label'] = result['superclass_label']['value']
            superclass['superclass_code'] = result['superclass_code']['value']
            superclasses.append(superclass)
    return superclasses


def getAllAssociations(sparql_endpoint, named_graph, in_clause):
    query = es.all_associations_all_concepts
    obj = es.run_sparql_query(sparql_endpoint, named_graph, query, in_clause)

    if obj is None:
        print("No Result Returned - Problem with Query")
        return None
    else:
        associations = []
        for result in obj['results']['bindings']:
            association = {}
            association['concept_code'] = result['concept_code']['value']
            association['relationship'] = result['relationship']['value']
            association['relationshipCode'] = result['relationshipCode']['value']
            association['relatedConceptCode'] = result['relatedConceptCode']['value']
            association['relatedConceptLabel'] = result['relatedConceptLabel']['value']
            associations.append(association)
    return associations


def getAllInverseAssociations(sparql_endpoint, named_graph, in_clause):
    query = es.all_inverse_associations_all_concepts
    obj = es.run_sparql_query(sparql_endpoint, named_graph, query, in_clause)

    if obj is None:
        print("No Result Returned - Problem with Query")
        return None
    else:
        associations = []
        for result in obj['results']['bindings']:
            association = {}
            association['concept_code'] = result['concept_code']['value']
            association['relationship'] = result['relationship']['value']
            association['relationshipCode'] = result['relationshipCode']['value']
            association['relatedConceptCode'] = result['relatedConceptCode']['value']
            association['relatedConceptLabel'] = result['relatedConceptLabel']['value']
            associations.append(association)
    return associations


def getAllRoles(sparql_endpoint, named_graph, in_clause):
    query = es.all_roles_all_concepts
    obj = es.run_sparql_query(sparql_endpoint, named_graph, query, in_clause)

    if obj is None:
        print("No Result Returned - Problem with Query")
        return None
    else:
        roles = []
        for result in obj['results']['bindings']:
            role = {}
            role['concept_code'] = result['concept_code']['value']
            role['relationship'] = result['relationship']['value']
            role['relationshipCode'] = result['relationshipCode']['value']
            role['relatedConceptCode'] = result['relatedConceptCode']['value']
            role['relatedConceptLabel'] = result['relatedConceptLabel']['value']
            roles.append(role)
    return roles


def getAllInverseRoles(sparql_endpoint, named_graph, in_clause):
    query = es.all_inverse_roles_all_concepts
    obj = es.run_sparql_query(sparql_endpoint, named_graph, query, in_clause)

    if obj is None:
        print("No Result Returned - Problem with Query")
        return None
    else:
        roles = []
        for result in obj['results']['bindings']:
            role = {}
            role['concept_code'] = result['concept_code']['value']
            role['relationship'] = result['relationship']['value']
            role['relationshipCode'] = result['relationshipCode']['value']
            role['relatedConceptCode'] = result['relatedConceptCode']['value']
            role['relatedConceptLabel'] = result['relatedConceptLabel']['value']
            roles.append(role)
    return roles


def getAllDisjointWith(sparql_endpoint, named_graph, in_clause):
    query = es.all_disjoint_with_all_concepts
    obj = es.run_sparql_query(sparql_endpoint, named_graph, query, in_clause)

    if obj is None:
        print("No Result Returned - Problem with Query")
        return None
    else:
        disjoints = []
        for result in obj['results']['bindings']:
            disjoint = {}
            disjoint['concept_code'] = result['concept_code']['value']
            disjoint['relationship'] = result['relationship']['value']
            disjoint['relatedConceptCode'] = result['relatedConceptCode']['value']
            disjoint['relatedConceptLabel'] = result['relatedConceptLabel']['value']
            disjoints.append(disjoint)
    return disjoints


def mergeConceptsAndProperties(concepts, properties):
    for property in properties:
        if property['concept_code'] in concepts:
            concepts[property['concept_code']]['properties'].append(property)


def mergeConceptsAndAxioms(concepts, axioms):
    for axiom in axioms:
        if axiom['concept_code'] in concepts:
                concepts[axiom['concept_code']]['axioms'].append(axiom)


def mergeConceptsAndSubclasses(concepts, subclasses):
    for subclass in subclasses:
        if subclass['concept_code'] in concepts:
                concepts[subclass['concept_code']]['subclasses'].append(subclass)


def mergeConceptsAndSuperclasses(concepts, superclasses):
    for superclass in superclasses:
        if superclass['concept_code'] in concepts:
                concepts[superclass['concept_code']]['superclasses'].append(superclass)


def mergeConceptsAndAssociations(concepts, associations):
    for association in associations:
        if association['concept_code'] in concepts:
                concepts[association['concept_code']]['associations'].append(association)


def mergeConceptsAndInverseAssociations(concepts, associations):
    for association in associations:
        if association['concept_code'] in concepts:
                concepts[association['concept_code']]['inverse_associations'].append(association)


def mergeConceptsAndRoles(concepts, roles):
    for role in roles:
        if role['concept_code'] in concepts:
                concepts[role['concept_code']]['roles'].append(role)


def mergeConceptsAndInverseRoles(concepts, roles):
    for role in roles:
        if role['concept_code'] in concepts:
                concepts[role['concept_code']]['inverse_roles'].append(role)


def mergeConceptsAndDisjointWith(concepts, disjoints):
    for disjoint in disjoints:
        if disjoint['concept_code'] in concepts:
                concepts[disjoint['concept_code']]['disjoint_with'].append(disjoint)


def addAdditionalProperties(concept):
    for property in concept['properties']:
        if property['property_label'] not in IGNORE_PROPERTIES:
            if not property['property_code'].startswith('A'):
                if property['property_label'] not in concept:
                    concept[property['property_label']] = []
                    concept[property['property_label']].append(property['property_value'])
                else:
                    concept[property['property_label']].append(property['property_value'])

    if "Preferred_Name" in concept:
        concept['Preferred_Name'] = concept['Preferred_Name'][0]


def addSubclasses(concept):
    subclasses = []
    for sub in concept['subclasses']:
        subclass = {}
        subclass['code'] = sub['subclass_code']
        subclass['label'] = sub['subclass_label']
        subclasses.append(subclass)

    if len(subclasses) > 0:
        concept['Subconcept'] = subclasses


def addSuperclasses(concept):
    superclasses = []
    for sup in concept['superclasses']:
        superclass = {}
        superclass['code'] = sup['superclass_code']
        superclass['label'] = sup['superclass_label']
        superclasses.append(superclass)

    if len(superclasses) > 0:
        concept['Superconcept'] = superclasses


def addAssociations(concept):
    associations = []
    for assoc in concept['associations']:
        association = {}
        association['relationship'] = assoc['relationship']
        association['relationshipCode'] = assoc['relationshipCode']
        association['relatedConceptCode'] = assoc['relatedConceptCode']
        association['relatedConceptLabel'] = assoc['relatedConceptLabel']
        associations.append(association)

    if len(associations) > 0:
        concept['Association'] = associations


def addInverseAssociations(concept):
    associations = []
    for assoc in concept['inverse_associations']:
        association = {}
        association['relationship'] = assoc['relationship']
        association['relationshipCode'] = assoc['relationshipCode']
        association['relatedConceptCode'] = assoc['relatedConceptCode']
        association['relatedConceptLabel'] = assoc['relatedConceptLabel']
        associations.append(association)

    if len(associations) > 0:
        concept['InverseAssociation'] = associations


def addRoles(concept):
    roles = []
    for r in concept['roles']:
        role = {}
        role['relationship'] = r['relationship']
        role['relationshipCode'] = r['relationshipCode']
        role['relatedConceptCode'] = r['relatedConceptCode']
        role['relatedConceptLabel'] = r['relatedConceptLabel']
        roles.append(role)

    if len(roles) > 0:
        concept['Role'] = roles


def addInverseRoles(concept):
    roles = []
    for r in concept['inverse_roles']:
        role = {}
        role['relationship'] = r['relationship']
        role['relationshipCode'] = r['relationshipCode']
        role['relatedConceptCode'] = r['relatedConceptCode']
        role['relatedConceptLabel'] = r['relatedConceptLabel']
        roles.append(role)

    if len(roles) > 0:
        concept['InverseRole'] = roles


def addDisjointWith(concept):
    disjoints = []
    for d in concept['disjoint_with']:
        disjoint = {}
        disjoint['relationship'] = d['relationship']
        disjoint['relatedConceptCode'] = d['relatedConceptCode']
        disjoint['relatedConceptLabel'] = d['relatedConceptLabel']
        disjoints.append(disjoint)

    if len(disjoints) > 0:
        concept['DisjointWith'] = disjoints


def addDefinitions(concept):
    definitions = []
    for axiom in concept['axioms']:
        if axiom['annotatedProperty'] == 'P97':
            definition = {}
            definition['definition'] = axiom['annotatedTarget']
            definition['def-source'] = axiom['P378']
            if 'P381' in axiom:
                definition['attr'] = axiom['P381']
            definitions.append(definition)

    if len(definitions) > 0:
        concept['DEFINITION'] = definitions


def addAltDefinitions(concept):
    definitions = []
    for axiom in concept['axioms']:
        if axiom['annotatedProperty'] == 'P325':
            definition = {}
            definition['definition'] = axiom['annotatedTarget']
            definition['def-source'] = axiom['P378']
            if 'P381' in axiom:
                definition['attr'] = axiom['P381']
            definitions.append(definition)

    if len(definitions) > 0:
        concept['ALT_DEFINITION'] = definitions


def addFullSynonyms(concept):
    synonyms = []
    for axiom in concept['axioms']:
        if axiom['annotatedProperty'] == 'P90':
            synonym = {}
            synonym['term-name'] = axiom['annotatedTarget']
            synonym['term-group'] = axiom['P383']
            synonym['term-source'] = axiom['P384']
            if 'P385' in axiom:
                synonym['source-code'] = axiom['P385']
            if 'P386' in axiom:
                synonym['subsource-name'] = axiom['P386']
            synonyms.append(synonym)

    if len(synonyms) > 0:
        concept['FULL_SYN'] = synonyms


def addMapsTo(concept):
    maps = []
    for axiom in concept['axioms']:
        if axiom['annotatedProperty'] == 'P375':
            mapTo = {}
            mapTo['annotatedTarget'] = axiom['annotatedTarget']
            if 'P393' in axiom:
                mapTo['Relationship_to_Target'] = axiom['P393']
            if 'P394' in axiom:
                mapTo['Target_Term_Type'] = axiom['P394']
            if 'P395' in axiom:
                mapTo['Target_Code'] = axiom['P395']
            if 'P396' in axiom:
                mapTo['Target_Terminology'] = axiom['P396']
            maps.append(mapTo)

    if len(maps) > 0:
        concept['Maps_To'] = maps


def addGO_Annotation(concept):
    gos = []
    for axiom in concept['axioms']:
        if axiom['annotatedProperty'] == 'P211':
            go = {}
            go['go-term'] = axiom['annotatedTarget']
            if 'P387' in axiom:
                go['go-id'] = axiom['P387']
            if 'P389' in axiom:
                go['go-evi'] = axiom['P389']
            if 'P390' in axiom:
                go['go-source'] = axiom['P390']
            if 'P391' in axiom:
                go['source-date'] = axiom['P391']
            gos.append(go)

    if len(gos) > 0:
        concept['GO_Annotation'] = gos

