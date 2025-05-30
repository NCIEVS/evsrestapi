package gov.nih.nci.evs.api.service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import gov.nih.nci.evs.api.model.AssociationEntryResultList;
import gov.nih.nci.evs.api.model.Audit;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptMinimal;
import gov.nih.nci.evs.api.model.HierarchyNode;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Mapping;
import gov.nih.nci.evs.api.model.Paths;
import gov.nih.nci.evs.api.model.SearchCriteria;
import gov.nih.nci.evs.api.model.StatisticsEntry;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.support.es.IndexMetadata;
import gov.nih.nci.evs.api.util.HierarchyUtils;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The Opensearch/OpenSearch DB query service.
 *
 * @author Arun
 */
public interface OpensearchQueryService {

  /**
   * Check if concept exists.
   *
   * @param code the code of the concept to check
   * @param terminology the terminology
   * @return boolean indicating if code exists or not
   */
  boolean checkConceptExists(String code, Terminology terminology);

  /**
   * Returns concept.
   *
   * @param code the code of the concept
   * @param terminology the terminology
   * @param ip the include param
   * @return the optional of concept for the code
   */
  Optional<Concept> getConcept(String code, Terminology terminology, IncludeParam ip);

  /**
   * Returns list of concepts.
   *
   * @param codes the list of concept codes
   * @param terminology the terminology
   * @param ip the include param
   * @return the list of concepts found
   */
  List<Concept> getConcepts(Collection<String> codes, Terminology terminology, IncludeParam ip);

  /**
   * Returns concepts mapped by code.
   *
   * @param codes the list of concept codes
   * @param terminology the terminology
   * @param ip the include param
   * @return concepts mapped by code
   */
  Map<String, Concept> getConceptsAsMap(
      Collection<String> codes, Terminology terminology, IncludeParam ip);

  /**
   * Returns child concepts.
   *
   * @param code the code of the parent concept
   * @param terminology the terminology
   * @return the list of child concepts
   */
  List<Concept> getSubclasses(String code, Terminology terminology);

  /**
   * Returns descendant concepts.
   *
   * @param code the code of the parent concept
   * @param terminology the terminology
   * @return the list of descendant concepts
   */
  List<Concept> getDescendants(String code, Terminology terminology);

  /**
   * Returns parent concepts.
   *
   * @param code the code of the child concept
   * @param terminology the terminology
   * @return the list of parent concepts
   */
  List<Concept> getSuperclasses(String code, Terminology terminology);

  /**
   * Returns the label of the concept.
   *
   * @param code the code of the concept
   * @param terminology the terminology
   * @return the optional of code label
   */
  Optional<String> getLabel(String code, Terminology terminology);

  /**
   * Returns the root nodes.
   *
   * @param terminology the terminology
   * @param ip the ip
   * @return the list of root nodes
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  List<Concept> getRootNodes(Terminology terminology, IncludeParam ip)
      throws JsonParseException, JsonMappingException, IOException;

  /**
   * see superclass *.
   *
   * @param terminology the terminology
   * @return the root nodes
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  List<HierarchyNode> getRootNodesHierarchy(Terminology terminology)
      throws JsonParseException, JsonMappingException, IOException;

  /**
   * Returns the child nodes.
   *
   * @param parent the parent code
   * @param maxLevel the max level upto which child nodes to be fetched
   * @param terminology the terminology
   * @return the list of child nodes
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  List<HierarchyNode> getChildNodes(String parent, int maxLevel, Terminology terminology)
      throws JsonParseException, JsonMappingException, IOException;

  /**
   * Returns all child codes (obtained recursively).
   *
   * @param code the parent code
   * @param terminology the terminology
   * @return the list of child codes
   */
  List<String> getAllChildNodes(String code, Terminology terminology);

  /**
   * Returns the path to root.
   *
   * @param code the code
   * @param terminology the terminology
   * @return the path to root
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  Paths getPathsToRoot(String code, Terminology terminology)
      throws JsonParseException, JsonMappingException, IOException;

  /**
   * Returns the path to parent.
   *
   * @param code the code
   * @param parentCode the parent code
   * @param terminology the terminology
   * @return the path to parent
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  Paths getPathsToParent(String code, String parentCode, Terminology terminology)
      throws JsonParseException, JsonMappingException, IOException;

  /**
   * Get concepts count for the given terminology.
   *
   * @param terminology the terminology
   * @return the concepts count
   */
  long getCount(Terminology terminology);

  /**
   * Get {@link IndexMetadata} objects.
   *
   * @param completedOnly boolean indicating to fetch metadata for complete indexes only
   * @return the list of {@link IndexMetadata} objects
   */
  List<IndexMetadata> getIndexMetadata(boolean completedOnly);

  /**
   * Get hiearchy roots for a given terminology.
   *
   * @param terminology the terminology
   * @return the hierarchy roots
   * @throws JsonMappingException the json mapping exception
   * @throws JsonProcessingException the json processing exception
   */
  public Optional<HierarchyUtils> getHierarchyRoots(Terminology terminology)
      throws JsonMappingException, JsonProcessingException;

  /**
   * Returns qualifiers.
   *
   * @param terminology the terminology
   * @param ip the include param
   * @return the list of qualifier concepts
   * @throws JsonMappingException the json mapping exception
   * @throws JsonProcessingException the json processing exception
   */
  List<Concept> getQualifiers(Terminology terminology, IncludeParam ip)
      throws JsonMappingException, JsonProcessingException;

  /**
   * Returns the qualifier values.
   *
   * @param terminology the terminology
   * @return the qualifier values
   * @throws JsonMappingException the json mapping exception
   * @throws JsonProcessingException the json processing exception
   * @throws Exception
   */
  Map<String, Set<String>> getQualifierValues(Terminology terminology) throws Exception;

  /**
   * Get qualifier.
   *
   * @param code the code
   * @param terminology the terminology
   * @param ip the include param
   * @return the qualifier
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  Optional<Concept> getQualifier(String code, Terminology terminology, IncludeParam ip)
      throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns properties.
   *
   * @param terminology the terminology
   * @param ip the include param
   * @return the list of property concepts
   * @throws JsonMappingException the json mapping exception
   * @throws JsonProcessingException the json processing exception
   */
  List<Concept> getProperties(Terminology terminology, IncludeParam ip)
      throws JsonMappingException, JsonProcessingException;

  /**
   * Get property.
   *
   * @param code the code
   * @param terminology the terminology
   * @param ip the include param
   * @return the property
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  Optional<Concept> getProperty(String code, Terminology terminology, IncludeParam ip)
      throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns associations.
   *
   * @param terminology the terminology
   * @param ip the include param
   * @return the list of association concepts
   * @throws JsonMappingException the json mapping exception
   * @throws JsonProcessingException the json processing exception
   */
  List<Concept> getAssociations(Terminology terminology, IncludeParam ip)
      throws JsonMappingException, JsonProcessingException;

  /**
   * Get association.
   *
   * @param code the code
   * @param terminology the terminology
   * @param ip the include param
   * @return the association
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  Optional<Concept> getAssociation(String code, Terminology terminology, IncludeParam ip)
      throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns roles.
   *
   * @param terminology the terminology
   * @param ip the include param
   * @return the list of role concepts
   * @throws JsonMappingException the json mapping exception
   * @throws JsonProcessingException the json processing exception
   */
  List<Concept> getRoles(Terminology terminology, IncludeParam ip)
      throws JsonMappingException, JsonProcessingException;

  /**
   * Get role.
   *
   * @param code the code
   * @param terminology the terminology
   * @param ip the include param
   * @return the role
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  Optional<Concept> getRole(String code, Terminology terminology, IncludeParam ip)
      throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns the synonym types.
   *
   * @param terminology the terminology
   * @param ip the ip
   * @return the synonym types
   * @throws JsonMappingException the json mapping exception
   * @throws JsonProcessingException the json processing exception
   */
  List<Concept> getSynonymTypes(Terminology terminology, IncludeParam ip)
      throws JsonMappingException, JsonProcessingException;

  /**
   * Returns the synonym type.
   *
   * @param code the code
   * @param terminology the terminology
   * @param ip the ip
   * @return the synonym type
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  Optional<Concept> getSynonymType(String code, Terminology terminology, IncludeParam ip)
      throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns the definition types.
   *
   * @param terminology the terminology
   * @param ip the ip
   * @return the definition types
   * @throws JsonMappingException the json mapping exception
   * @throws JsonProcessingException the json processing exception
   */
  List<Concept> getDefinitionTypes(Terminology terminology, IncludeParam ip)
      throws JsonMappingException, JsonProcessingException;

  /**
   * Returns the definition type.
   *
   * @param code the code
   * @param terminology the terminology
   * @param ip the ip
   * @return the definition type
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  Optional<Concept> getDefinitionType(String code, Terminology terminology, IncludeParam ip)
      throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns synonym sources.
   *
   * @param terminology the terminology
   * @return the list of synonym source concepts
   * @throws ClassNotFoundException the class not found exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  List<ConceptMinimal> getSynonymSources(Terminology terminology)
      throws ClassNotFoundException, IOException;

  /**
   * Returns contributing sources.
   *
   * @param terminology the terminology
   * @return the list of contributing source concepts
   * @throws ClassNotFoundException the class not found exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  List<ConceptMinimal> getContributingSources(Terminology terminology)
      throws ClassNotFoundException, IOException;

  /**
   * Returns subsets.
   *
   * @param terminology the terminology
   * @return the list of subsets
   * @throws JsonProcessingException
   * @throws JsonMappingException
   * @throws ClassNotFoundException the class not found exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  List<Concept> getSubsets(Terminology term, IncludeParam ip)
      throws JsonMappingException, JsonProcessingException;

  /*
   * returns a list of association entries
   *
   * @param terminology the terminology
   *
   * @param ip the ip
   *
   * @param fromRecord the starting record for the search
   *
   * @param pageSize the size of pages in returned result
   *
   * @return the association entry list
   *
   * @throws Exception Signals that an exception has occurred.
   */
  AssociationEntryResultList getAssociationEntries(
      String terminology, String label, int fromRecord, int pageSize) throws Exception;

  /**
   * Returns mapsets.
   *
   * @return the list of mapsets
   * @throws JsonProcessingException
   * @throws JsonMappingException
   * @throws ClassNotFoundException the class not found exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  List<Concept> getMapsets(IncludeParam ip) throws Exception;

  /**
   * Returns mapset by code filter.
   *
   * @param code the code to filter
   * @return the list of mapsets
   * @throws JsonProcessingException
   * @throws JsonMappingException
   * @throws ClassNotFoundException the class not found exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  List<Concept> getMapset(String code, IncludeParam ip) throws Exception;

  /**
   * see superclass *.
   *
   * @param terminology the terminology
   * @param source the source
   * @return the source stats
   * @throws JsonMappingException the json mapping exception
   * @throws JsonProcessingException the json processing exception
   */
  Map<String, List<StatisticsEntry>> getSourceStats(Terminology terminology, String source)
      throws JsonMappingException, JsonProcessingException;

  /**
   * Returns mappings by code filter.
   *
   * @param code the code to filter
   * @return the list of mappings
   * @throws JsonProcessingException
   * @throws JsonMappingException
   * @throws ClassNotFoundException the class not found exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  List<Mapping> getMapsetMappings(String code) throws Exception;

  /**
   * Returns audits by terminology filter
   *
   * @param terminology the terminology to filter by
   * @return the list of audit records matching the terminology
   * @throws Exception if retrieval fails
   */
  List<Audit> getAuditsByTerminology(String terminology) throws Exception;

  /**
   * Returns audit records filtered by type
   *
   * @param type the type to filter by
   * @return the list of audit records matching the type
   * @throws Exception if retrieval fails
   */
  List<Audit> getAuditsByType(String type) throws Exception;

  /**
   * Returns all audit records.
   *
   * @param searchCriteria the search criteria
   * @return the list of audit records
   * @throws Exception if retrieval fails
   */
  List<Audit> getAllAudits(SearchCriteria searchCriteria) throws Exception;
}
