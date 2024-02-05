package gov.nih.nci.evs.api.repository;

import gov.nih.nci.evs.api.model.Concept;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@code Concept} entity
 *
 * <p>Spring data creates default index (if not present) on repository bootstrapping
 *
 * @author Arun
 */
@Repository
public interface ConceptRepository extends CrudRepository<Concept, String> {}
