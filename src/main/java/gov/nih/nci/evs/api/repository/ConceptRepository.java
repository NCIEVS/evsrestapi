
package gov.nih.nci.evs.api.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import gov.nih.nci.evs.api.model.Concept;

/**
 * Repository for {@code Concept} entity
 * 
 * <p>
 * Spring data creates default index (if not present) on repository bootstrapping.
 *
 * @author Arun
 */
@Repository
public interface ConceptRepository extends CrudRepository<Concept, String> {
  // n/a - I don't think we use this and we should probably remove it.
}
