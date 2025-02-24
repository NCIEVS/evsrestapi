package gov.nih.nci.evs.api.service;

import gov.nih.nci.evs.api.model.AssociationEntryResultList;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptMinimal;
import gov.nih.nci.evs.api.model.StatisticsEntry;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for metadata.
 *
 * @author Arun
 */
public interface MetadataService {

  /**
   * Get list of associations.
   *
   * @param terminology the terminology
   * @param include the include
   * @param list the list
   * @return the list of associations
   * @throws Exception the exception
   */
  List<Concept> getAssociations(
      final String terminology, final Optional<String> include, final Optional<String> list)
      throws Exception;

  /**
   * Get association for the given code.
   *
   * @param terminology the terminology
   * @param code the code
   * @param include the include
   * @return the optional association - empty if association is not found
   * @throws Exception the exception
   */
  Optional<Concept> getAssociation(
      final String terminology, final String code, final Optional<String> include) throws Exception;

  /**
   * Get roles.
   *
   * @param terminology the terminology
   * @param include the include
   * @param list the list
   * @return the list of roles
   * @throws Exception the exception
   */
  List<Concept> getRoles(
      final String terminology, final Optional<String> include, final Optional<String> list)
      throws Exception;

  /**
   * Get role for the given code.
   *
   * @param terminology the terminology
   * @param code the code
   * @param include the include
   * @return the optional role - empty if role is not found
   * @throws Exception the exception
   */
  Optional<Concept> getRole(
      final String terminology, final String code, final Optional<String> include) throws Exception;

  /**
   * Get properties.
   *
   * @param terminology the terminology
   * @param include the include
   * @param list the list
   * @return the list of properties
   * @throws Exception the exception
   */
  List<Concept> getProperties(
      final String terminology, final Optional<String> include, final Optional<String> list)
      throws Exception;

  /**
   * Returns the qualifiers.
   *
   * @param terminology the terminology
   * @param include the include
   * @param list the list
   * @return the qualifiers
   * @throws Exception the exception
   */
  List<Concept> getQualifiers(
      final String terminology, final Optional<String> include, final Optional<String> list)
      throws Exception;

  /**
   * Returns the qualifier.
   *
   * @param terminology the terminology
   * @param code the code
   * @param include the include
   * @return the qualifier
   * @throws Exception the exception
   */
  Optional<Concept> getQualifier(
      final String terminology, final String code, final Optional<String> include) throws Exception;

  /**
   * Get property for the given code.
   *
   * @param terminology the terminology
   * @param code the code
   * @param include the include
   * @return the optional property - empty if property is not found
   * @throws Exception the exception
   */
  Optional<Concept> getProperty(
      final String terminology, final String code, final Optional<String> include) throws Exception;

  /**
   * Get status list.
   *
   * @param terminology the terminology
   * @return the list of statuses for the given terminology
   * @throws Exception the exception
   */
  Optional<List<String>> getConceptStatuses(final String terminology) throws Exception;

  /**
   * Returns the definition sources.
   *
   * @param terminology the terminology
   * @return the definition sources
   * @throws Exception the exception
   */
  List<ConceptMinimal> getDefinitionSources(final String terminology) throws Exception;

  /**
   * Returns the synonym sources.
   *
   * @param terminology the terminology
   * @return the synonym sources
   * @throws Exception the exception
   */
  List<ConceptMinimal> getSynonymSources(final String terminology) throws Exception;

  /**
   * Get axiom qualifiers.
   *
   * @param terminology the terminology
   * @param code the code
   * @return the list of axiom qualifiers
   * @throws Exception the exception
   */
  Optional<List<String>> getQualifierValues(final String terminology, final String code)
      throws Exception;

  /**
   * Get term types.
   *
   * @param terminology the terminology
   * @return the list of term types
   * @throws Exception the exception
   */
  List<ConceptMinimal> getTermTypes(final String terminology) throws Exception;

  /**
   * Returns the synonym types.
   *
   * @param terminology the terminology
   * @param include the include
   * @param list the list
   * @return the synonym types
   * @throws Exception the exception
   */
  List<Concept> getSynonymTypes(
      final String terminology, final Optional<String> include, final Optional<String> list)
      throws Exception;

  /**
   * Returns the synonym type.
   *
   * @param terminology the terminology
   * @param code the code
   * @param include the include
   * @return the synonym type
   * @throws Exception the exception
   */
  Optional<Concept> getSynonymType(
      final String terminology, final String code, final Optional<String> include) throws Exception;

  /**
   * Returns the definition types.
   *
   * @param terminology the terminology
   * @param include the include
   * @param list the list
   * @return the definition types
   * @throws Exception the exception
   */
  List<Concept> getDefinitionTypes(
      final String terminology, final Optional<String> include, final Optional<String> list)
      throws Exception;

  /**
   * Returns the definition type.
   *
   * @param terminology the terminology
   * @param code the code
   * @param include the include
   * @return the definition type
   * @throws Exception the exception
   */
  Optional<Concept> getDefinitionType(
      final String terminology, final String code, final Optional<String> include) throws Exception;

  /**
   * Returns the subsets.
   *
   * @param terminology the terminology
   * @param include the include
   * @param list the list
   * @return the properties
   * @throws Exception the exception
   */
  List<Concept> getSubsets(String terminology, Optional<String> include, Optional<String> list)
      throws Exception;

  /**
   * Get subset for the given code.
   *
   * @param terminology the terminology
   * @param code the code
   * @param include the include
   * @return the optional subset - empty if subset is not found
   * @throws Exception the exception
   */
  Optional<Concept> getSubset(String terminology, String code, Optional<String> include)
      throws Exception;

  /**
   * Returns the association entries.
   *
   * @param terminology the terminology
   * @param code the code
   * @param orElse the starting record
   * @param orElse2 the page size of the results list
   * @return the association entry list
   * @throws Exception the exception
   */
  AssociationEntryResultList getAssociationEntries(
      String terminology, String label, Integer fromRecord, Integer pageSize) throws Exception;

  /**
   * Returns the welcome text.
   *
   * @param terminology the terminology
   * @return the welcome text
   * @throws Exception the exception
   */
  String getWelcomeText(String terminology) throws Exception;

  /**
   * Returns the source stats.
   *
   * @param terminology the terminology
   * @param source the source
   * @return the properties
   * @throws Exception the exception
   */
  Map<String, List<StatisticsEntry>> getSourceStats(String terminology, String source)
      throws Exception;
}
