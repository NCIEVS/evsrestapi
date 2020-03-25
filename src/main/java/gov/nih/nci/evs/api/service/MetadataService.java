package gov.nih.nci.evs.api.service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.support.ConfigData;

/**
 * Service for metadata
 * 
 * @author Arun
 *
 */
public interface MetadataService {
  /**
   * Get application metadata
   * 
   * @return the config data
   * @throws IOException
   */
  ConfigData getApplicationMetadata() throws IOException;

  /**
   * Get application metadata for {@code dbType}}
   * 
   * @param dbType the db type (monthly, weekly etc)
   * @return the config data
   * @throws IOException
   */
  ConfigData getApplicationMetadata(String dbType) throws IOException;

  /**
   * Get list of terminologies
   * 
   * @return the list of terminologies
   * @throws IOException
   */
  List<Terminology> getTerminologies() throws IOException;

  /**
   * Get list of associations
   * 
   * @param terminology
   * @param include
   * @param list
   * @return the list of associations
   * @throws Exception
   */
  List<Concept> getAssociations(final String terminology, final Optional<String> include,
    final Optional<String> list) throws Exception;

  /**
   * Get association for the given code
   * 
   * @param terminology
   * @param code
   * @param include
   * @return the optional association - empty if association is not found
   * @throws Exception
   */
  Optional<Concept> getAssociation(final String terminology, final String code,
    final Optional<String> include) throws Exception;

  /**
   * Get roles
   * 
   * @param terminology
   * @param include
   * @param list
   * @return the list of roles
   * @throws Exception
   */
  List<Concept> getRoles(final String terminology, final Optional<String> include,
    final Optional<String> list) throws Exception;

  /**
   * Get role for the given code
   * 
   * @param terminology
   * @param code
   * @param include
   * @return the optional role - empty if role is not found
   * @throws Exception
   */
  Optional<Concept> getRole(final String terminology, final String code,
    final Optional<String> include) throws Exception;

  /**
   * Get properties
   * 
   * @param terminology
   * @param include
   * @param list
   * @return the list of properties
   * @throws Exception
   */
  List<Concept> getProperties(final String terminology, final Optional<String> include,
    boolean forDocumentation, final Optional<String> list) throws Exception;

  /**
   * Get property for the given code
   * 
   * @param terminology
   * @param code
   * @param include
   * @return the optional property - empty if property is not found
   * @throws Exception
   */
  Optional<Concept> getProperty(final String terminology, final String code,
    final Optional<String> include) throws Exception;

  /**
   * Get status list
   * 
   * @param terminology
   * @return the list of statuses for the given terminology
   * @throws Exception
   */
  Optional<List<String>> getConceptStatuses(final String terminology) throws Exception;

  /**
   * Get contributing sources
   * 
   * @param terminology
   * @return the contributing sources for the given terminology
   * @throws Exception
   */
  Optional<List<String>> getContributingSources(final String terminology) throws Exception;

  /**
   * Get axiom qualifiers
   * 
   * @param terminology
   * @param code
   * @return the list of axiom qualifiers
   * @throws Exception
   */
  Optional<List<String>> getAxiomQualifiersList(final String terminology, final String code)
    throws Exception;

  /**
   * Get term types
   * 
   * @param terminology
   * @return the list of term types
   */
  List<Concept> getTermTypes(final String terminology) throws Exception;
}
