package gov.nih.nci.evs.api.service;

import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

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
  public ConfigData getApplicationMetadata();
  public List<Terminology> getTerminologies();
  public List<Concept> getAssociations(final String terminology, 
      final Optional<String> include, final Optional<String> list);
  public Concept getAssociation(final String terminology,
      final String code, final Optional<String> include);
  public List<Concept> getRoles(final String terminology,
      final Optional<String> include, final Optional<String> list);
  public Concept getRole(final String terminology,
      final String code, final Optional<String> include);
  public List<Concept> getProperties(final String terminology,
      final Optional<String> include, final Optional<String> list);
  public Concept getProperty(final String terminology,
      final String code, final Optional<String> include);
  public List<String> getConceptStatuses(final String terminology);
  public List<String> getContributingSources(final String terminology);
  public List<String> getAxiomQualifiersList(final String terminology, final String code);
}
