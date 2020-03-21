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
  ConfigData getApplicationMetadata() throws IOException;
  ConfigData getApplicationMetadata(String dbType) throws IOException;
  List<Terminology> getTerminologies() throws IOException;
  List<Concept> getAssociations(final String terminology, 
      final Optional<String> include, final Optional<String> list) throws Exception;
  Optional<Concept> getAssociation(final String terminology,
      final String code, final Optional<String> include) throws Exception;
  List<Concept> getRoles(final String terminology,
      final Optional<String> include, final Optional<String> list);
  Concept getRole(final String terminology,
      final String code, final Optional<String> include);
  List<Concept> getProperties(final String terminology,
      final Optional<String> include, final Optional<String> list) throws Exception;
  Concept getProperty(final String terminology,
      final String code, final Optional<String> include);
  List<String> getConceptStatuses(final String terminology);
  List<String> getContributingSources(final String terminology);
  List<String> getAxiomQualifiersList(final String terminology, final String code);
}
