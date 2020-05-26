package gov.nih.nci.evs.api.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import gov.nih.nci.evs.api.model.Association;
import gov.nih.nci.evs.api.model.Axiom;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.DisjointWith;
import gov.nih.nci.evs.api.model.Property;
import gov.nih.nci.evs.api.model.Role;

/**
 * 
 * 
 * @author Arun
 *
 */
public class ElasticLoadUtils {

  private static final List<String> IGNORE_PROPERTIES = new ArrayList<>();
  
  static {
    IGNORE_PROPERTIES.add("code");
    IGNORE_PROPERTIES.add("label");
    IGNORE_PROPERTIES.add("DEFINITION");
    IGNORE_PROPERTIES.add("ALT_DEFINITION");
    IGNORE_PROPERTIES.add("FULL_SYN");
    IGNORE_PROPERTIES.add("Maps_To");
    IGNORE_PROPERTIES.add("GO_Annotation");
  }
  
  /**

  def mergeConceptsAndProperties(concepts, properties):
      for property in properties:
          if property['concept_code'] in concepts:
              concepts[property['concept_code']]['properties'].append(property)
  */
  
  public static void mergeConceptsAndProperties(List<Concept> concepts, Map<String, List<Property>> propertiesByConcept) {
    for(Concept concept: concepts) {
      if (propertiesByConcept.containsKey(concept.getCode())) {
        concept.getProperties().addAll(propertiesByConcept.get(concept.getCode()));
      }
    }
  }
  
  /*
  
  def mergeConceptsAndAxioms(concepts, axioms):
      for axiom in axioms:
          if axiom['concept_code'] in concepts:
                  concepts[axiom['concept_code']]['axioms'].append(axiom)
  */
  
  public static void mergeConceptsAndAxioms(List<Concept> concepts, Map<String, List<Axiom>> axiomsByConcept) {
    for(Concept concept: concepts) {
      if (axiomsByConcept.containsKey(concept.getCode())) {
        concept.getSynonyms().addAll(EVSUtils.getSynonyms(axiomsByConcept.get(concept.getCode())));
      }
    }
  }
  
  /*
  
  def mergeConceptsAndSubclasses(concepts, subclasses):
      for subclass in subclasses:
          if subclass['concept_code'] in concepts:
                  concepts[subclass['concept_code']]['subclasses'].append(subclass)
  */
  
  public static void mergeConceptsAndChildren(List<Concept> concepts, Map<String, List<Concept>> childrenByConcept) {
    for(Concept concept: concepts) {
      if (childrenByConcept.containsKey(concept.getCode())) {
        concept.getChildren().addAll(childrenByConcept.get(concept.getCode()));
      }
    }
  }
  
  /*
  def mergeConceptsAndSuperclasses(concepts, superclasses):
      for superclass in superclasses:
          if superclass['concept_code'] in concepts:
                  concepts[superclass['concept_code']]['superclasses'].append(superclass)
  */
  
  public static void mergeConceptsAndSuperclasses(List<Concept> concepts, Map<String, List<Concept>> parentsByConcept) {
    for(Concept concept: concepts) {
      if (parentsByConcept.containsKey(concept.getCode())) {
        concept.getParents().addAll(parentsByConcept.get(concept.getCode()));
      }
    }    
  }
  
  /*
  def mergeConceptsAndAssociations(concepts, associations):
      for association in associations:
          if association['concept_code'] in concepts:
                  concepts[association['concept_code']]['associations'].append(association)
  */
  
  public static void mergeConceptsAndAssociations(List<Concept> concepts, Map<String, List<Association>> associationsByConcept) {
    for(Concept concept: concepts) {
      if (associationsByConcept.containsKey(concept.getCode())) {
        concept.getAssociations().addAll(associationsByConcept.get(concept.getCode()));
      }
    }    
  } 
  
  /*
  def mergeConceptsAndInverseAssociations(concepts, associations):
      for association in associations:
          if association['concept_code'] in concepts:
                  concepts[association['concept_code']]['inverse_associations'].append(association)
  */
  
  public static void mergeConceptsAndInverseAssociations(List<Concept> concepts, Map<String, List<Association>> invAssociationsByConcept) {
    for(Concept concept: concepts) {
      if (invAssociationsByConcept.containsKey(concept.getCode())) {
        concept.getInverseAssociations().addAll(invAssociationsByConcept.get(concept.getCode()));
      }
    }    
  }
  
  /*
  def mergeConceptsAndRoles(concepts, roles):
      for role in roles:
          if role['concept_code'] in concepts:
                  concepts[role['concept_code']]['roles'].append(role)
  */
  
  public static void mergeConceptsAndRoles(List<Concept> concepts, Map<String, List<Role>> rolesByConcept) {
    for(Concept concept: concepts) {
      if (rolesByConcept.containsKey(concept.getCode())) {
        concept.getRoles().addAll(rolesByConcept.get(concept.getCode()));
      }
    }    
  }  
  
  /*
  def mergeConceptsAndInverseRoles(concepts, roles):
      for role in roles:
          if role['concept_code'] in concepts:
                  concepts[role['concept_code']]['inverse_roles'].append(role)
  */
  
  public static void mergeConceptsAndInverseRoles(List<Concept> concepts, Map<String, List<Role>> inverseRolesByConcept) {
    for(Concept concept: concepts) {
      if (inverseRolesByConcept.containsKey(concept.getCode())) {
        concept.getInverseRoles().addAll(inverseRolesByConcept.get(concept.getCode()));
      }
    }    
  }
  
  /*
  def mergeConceptsAndDisjointWith(concepts, disjoints):
      for disjoint in disjoints:
          if disjoint['concept_code'] in concepts:
                  concepts[disjoint['concept_code']]['disjoint_with'].append(disjoint)
  */
  
  public static void mergeConceptsAndDisjointWith(List<Concept> concepts, Map<String, List<DisjointWith>> disjointWithByConcept) {
    for(Concept concept: concepts) {
      if (disjointWithByConcept.containsKey(concept.getCode())) {
        concept.getDisjointWith().addAll(disjointWithByConcept.get(concept.getCode()));
      }
    }    
  }
  
  /*
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
  */
  
  public static void addAdditionalProperties(Concept concept) {
    //TODO:
  }
  
  /*
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

  **/
}
